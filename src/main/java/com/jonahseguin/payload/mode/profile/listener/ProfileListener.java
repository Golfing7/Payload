/*
 * Copyright (c) 2020 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.listener;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileCache;
import com.jonahseguin.payload.mode.profile.PayloadProfileController;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.event.PayloadProfileLogoutEvent;
import com.jonahseguin.payload.mode.profile.event.PayloadProfileSwitchServersEvent;
import com.jonahseguin.payload.server.event.PayloadPlayerEvent;
import com.jonahseguin.payload.server.event.PlayerChangeServerEvent;
import com.jonahseguin.payload.server.event.PlayerLeaveNetworkEvent;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ProfileListener implements Listener {

    /**
     * An enumeration of all valid profile actions.
     */
    @AllArgsConstructor
    public enum ProfileAction {
        @SuppressWarnings("deprecation")
        MESSAGE_PLAYER((player, data) -> {
            String rawMessage = data.getString("message");
            if (rawMessage == null) {
                return;
            }

            ChatMessageType messageType;
            try{
                messageType = ChatMessageType.valueOf(data.getString("chat-type"));
            }catch(IllegalArgumentException exc) {
                Bukkit.getLogger().warning("[%s] - Chat message type %s not defined!".formatted(PayloadPlugin.getPlugin().getName(), data.getString("chat-type")));
                return;
            }

            //Check if we're receiving a component or not.
            boolean isComponent = data.getBoolean("isComponent", false);
            if (isComponent) {
                Component component = GsonComponentSerializer.gson().deserialize(rawMessage);
                switch (messageType) {
                    case CHAT, SYSTEM -> player.sendMessage(component);
                    case ACTION_BAR -> player.sendActionBar(component);
                }
                return;
            }

            switch (messageType) {
                case CHAT, SYSTEM -> player.sendMessage(rawMessage);
                case ACTION_BAR -> player.sendActionBar(rawMessage);
            }
        }),
        PLAY_SOUND((player, data) -> {
            Sound sound;
            try{
                sound = Sound.valueOf(data.getString("sound-type"));
            }catch(IllegalArgumentException exc) {
                Bukkit.getLogger().warning("[%s] - Sound type %s not defined!".formatted(PayloadPlugin.getPlugin().getName(), data.getString("sound-type")));
                return;
            }

            float volume = data.getDouble("sound-volume").floatValue();
            float pitch = data.getDouble("sound-pitch").floatValue();

            //Get the sound location.
            Location location;
            if(data.containsKey("sound-location.x")) {
                location = new Location(player.getWorld(),
                        data.getDouble("sound-location.x"),
                        data.getDouble("sound-location.y"),
                        data.getDouble("sound-location.z"));
            } else {
                location = player.getLocation();
            }

            player.playSound(location, sound, volume, pitch);
        }),
        SHOW_TITLE((player, data) -> {
            Component title = GsonComponentSerializer.gson().deserialize(data.getString("title"));
            Component subtitle = GsonComponentSerializer.gson().deserialize(data.getString("subtitle"));

            //Build the title from the given information.
            Title realTitle;
            if(data.containsKey("times")) {
                Duration fadeIn = Duration.ofMillis(data.getLong("times.fade-in"));
                Duration fadeOut = Duration.ofMillis(data.getLong("times.fade-out"));
                Duration stay = Duration.ofMillis(data.getLong("times.stay"));

                realTitle = Title.title(title, subtitle, Title.Times.of(fadeIn, stay, fadeOut));
            } else {
                realTitle = Title.title(title, subtitle);
            }

            player.showTitle(realTitle);
        }),
        ;

        /**
         * The handler for the document.
         */
        final BiConsumer<Player, Document> documentConsumer;
    }

    private final PayloadAPI api;

    @Inject
    public ProfileListener(PayloadAPI api) {
        this.api = api;
    }

    @EventHandler
    public void onPayloadPlayerEvent(PayloadPlayerEvent event) {
        Document data = event.getData();
        if (data == null || !data.containsKey("action")) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ProfileAction action;
        try{
            action = ProfileAction.valueOf(data.getString("action"));
        }catch(IllegalArgumentException exc) {
            Bukkit.getLogger().warning("[%s] - Action %s not defined as a ProfileAction!".formatted(api.getPlugin().getName(), data.getString("action")));
            return;
        }

        action.documentConsumer.accept(player, data);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingStart(AsyncPlayerPreLoginEvent event) {
        //It's possible the plugin receives this event while the server is "disabling". If the server IS turning off, we don't need to call this event.
        if (!api.getPlugin().isEnabled())
            return;

        final String username = event.getName();
        final UUID uniqueId = event.getUniqueId();
        final String ip = event.getAddress().getHostAddress();

        List<Cache> sortedCaches = api.getSortedCachesByDepends();

        sortedCaches.forEach(c -> {
            if (c instanceof ProfileCache) {
                ProfileCache cache = (ProfileCache) c;
                PayloadProfileController controller = cache.controller(uniqueId);
                controller.login(username, ip);
                controller.cache();

                if (controller.isDenyJoin()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, controller.getJoinDenyReason());
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingInit(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        api.getSortedCachesByDepends().forEach(c -> {
            if (c instanceof PayloadProfileCache) {
                PayloadProfileCache cache = (PayloadProfileCache) c;
                PayloadProfileController controller = cache.controller(player.getUniqueId());
                if (controller != null) {
                    cache.getErrorService().debug("Initializing player " + player.getName() + " for cache " + cache.getName());
                    controller.initializeOnJoin(player);
                }
                else {
                    cache.getErrorService().capture("Could not initialize player " + player.getName() + " for cache " + cache.getName() + " (controller is null)");
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProfileQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        boolean ranEvent = false;
        for(Cache c : api.getSortedCachesByDepends()) {
            if (c instanceof PayloadProfileCache) {
                PayloadProfileCache cache = (PayloadProfileCache) c;
                if (cache.getMode().equals(PayloadMode.STANDALONE)) {
                    // save on quit in standalone mode
                    Optional<PayloadProfile> o = cache.getFromCache(player.getUniqueId());
                    if (o.isPresent()) {
                        PayloadProfile profile = o.get();

                        PayloadProfileLogoutEvent payloadEvent = new PayloadProfileLogoutEvent(profile);
                        cache.getPlugin().getServer().getPluginManager().callEvent(payloadEvent);

                        profile.uninitializePlayer();
                        cache.saveAsync(profile);
                        cache.removeController(profile.getUniqueId());

                        if(ranEvent)
                            continue;

                        //Run the event.
                        PlayerLeaveNetworkEvent networkEvent = new PlayerLeaveNetworkEvent(event);
                        networkEvent.callEvent();
                        event.quitMessage(networkEvent.quitMessage());
                        ranEvent = true;
                    }
                } else if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
                    Optional<PayloadProfile> o = cache.getFromCache(player.getUniqueId());
                    if (o.isPresent()) {
                        PayloadProfile profile = o.get();
                        if (!profile.hasValidHandshake()) {
                            PayloadProfileLogoutEvent payloadEvent = new PayloadProfileLogoutEvent(profile);
                            cache.getPlugin().getServer().getPluginManager().callEvent(payloadEvent);

                            profile.uninitializePlayer();

                            // Not switching servers (no incoming handshake) -- we can assume they are actually
                            // Logging out, and not switching servers
                            cache.runAsyncImmediately(() -> {
                                cache.save(profile);
                                cache.controller(event.getPlayer().getUniqueId()).uncache(profile, false);
                                cache.removeController(player.getUniqueId());
                                cache.getErrorService().debug("Saving player " + player.getName() + " on logout (not switching servers)");
                            });

                            if(ranEvent)
                                continue;

                            //Run the event.
                            PlayerLeaveNetworkEvent networkEvent = new PlayerLeaveNetworkEvent(event);
                            networkEvent.callEvent();
                            event.quitMessage(networkEvent.quitMessage());
                            ranEvent = true;
                        } else {
                            PayloadProfileSwitchServersEvent payloadEvent = new PayloadProfileSwitchServersEvent(profile);
                            cache.getPlugin().getServer().getPluginManager().callEvent(payloadEvent);

                            profile.uninitializePlayer();

                            cache.controller(event.getPlayer().getUniqueId()).uncache(profile, true);
                            cache.getErrorService().debug("Not saving player " + player.getName() + " on quit (is switching servers)");

                            if(ranEvent)
                                continue;

                            //Run the event.
                            PlayerChangeServerEvent networkEvent = new PlayerChangeServerEvent(event);
                            networkEvent.callEvent();
                            event.quitMessage(networkEvent.quitMessage());
                            ranEvent = true;
                        }
                    } else {
                        // This shouldn't happen
                        cache.getErrorService().debug("Profile null during logout for Payload '" + player.getName() + "': could not set online=false");
                    }
                }
            }
        }
    }

}
