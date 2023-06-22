/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.mode.profile.listener.ProfileListener;
import com.jonahseguin.payload.mode.profile.network.NetworkProfile;
import com.jonahseguin.payload.mode.profile.util.MsgBuilder;
import com.jonahseguin.payload.server.ServerPublisher;
import com.jonahseguin.payload.server.ServerService;
import dev.morphia.annotations.*;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang3.Validate;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

// The implementing class of this abstract class must add an @Entity annotation (from MongoDB) with a collection name!
@Getter
@Setter
@Indexes({
        @Index(fields = @Field("username")),
        @Index(fields = @Field("uniqueId"))
})
public abstract class PayloadProfile implements Payload<UUID> {

    protected transient final ProfileCache cache;

    @Id
    protected ObjectId objectId = new ObjectId();
    @Indexed
    protected String username;
    @Indexed
    protected String uniqueId;
    protected String loginIp = null; // IP the profile logged in with
    protected String payloadId; // The ID of the Payload instance that currently holds this profile
    protected transient UUID uuid = null;
    protected transient long cachedTimestamp = System.currentTimeMillis();
    protected transient long lastInteractionTimestamp = System.currentTimeMillis();
    protected transient long lastSaveTimestamp = 0;
    protected transient boolean saveFailed = false; // If the player's profile failed to auto-save/save on shutdown,
    // This will be set to true, and we will notify the player once their
    // Profile has been saved successfully
    protected transient String loadingSource = null;
    protected transient Player player = null;
    protected transient long handshakeStartTimestamp = 0; // the time when a handshake starts (when another server requests that we save this profile)
    protected transient boolean handshakeLogin = false; // If the incoming handshake is from the player logging in to a different server.

    @Inject
    public PayloadProfile(ProfileCache cache) {
        this.cache = cache;
        this.payloadId = cache.getApi().getPayloadID();
    }

    public PayloadProfile(ProfileCache cache, String username, UUID uniqueId, String loginIp) {
        this(cache);
        this.username = username;
        this.uuid = uniqueId;
        this.uniqueId = uniqueId.toString();
        this.loginIp = loginIp;
    }

    public PayloadProfile() {
        this.cache = null;
    }

    @PostLoad
    private void onPostPayloadLoad() {
        this.uuid = UUID.fromString(this.uniqueId);
    }

    @Override
    public void onReceiveUpdate() {

    }

    @Override
    public boolean hasValidHandshake() {
        if (handshakeStartTimestamp > 0) {
            long ago = System.currentTimeMillis() - handshakeStartTimestamp;
            long seconds = ago / 1000;
            return seconds <= cache.getSettings().getHandshakeTimeoutSeconds() + 1;
        }
        return false;
    }

    @Nonnull
    @Override
    public ProfileCache getCache() {
        return cache;
    }

    public UUID getUUID() {
        if (this.uuid == null) {
            this.uuid = UUID.fromString(this.uniqueId);
        }
        return this.uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
        this.uniqueId = uuid.toString();
    }

    public UUID getUniqueId() {
        return this.getUUID();
    }

    public void setUniqueId(UUID uuid) {
        this.setUUID(uuid);
    }

    public final void initializePlayer(Player player) {
        Validate.notNull(player, "Player cannot be null for initializePlayer");
        this.player = player;
        this.init();
    }

    public final void uninitializePlayer() {
        this.uninit();
        this.player = null;
    }

    protected abstract void init();

    protected abstract void uninit();

    public boolean isOnlineInstancing() {
        if (this.isOnline()) {
            return true;
        }

        if (this.getCache().getMode() != PayloadMode.NETWORK_NODE) {
            return false;
        }

        Optional<NetworkProfile> networked = getNetworked();
        return networked.isPresent() && networked.get().isOnline();
    }

    public Optional<NetworkProfile> getNetworked() {
        return this.cache.getNetworked(this.getUniqueId());
    }

    public boolean isOnline() {
        if (this.player == null) {
            Player player = Bukkit.getPlayer(this.getUUID());
            if (player != null && player.isOnline()) {
                this.player = player;
                return true;
            }
        }
        return this.player != null && this.player.isOnline();
    }

    public String getCurrentIP() {
        if (player != null && player.isOnline()) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return this.getLoginIp();
    }

    @Override
    public void interact() {
        this.lastInteractionTimestamp = System.currentTimeMillis();
    }

    @Override
    public UUID getIdentifier() {
        return this.getUniqueId();
    }

    @SuppressWarnings("deprecation")
    public void sendMessage(String msg) {
        Player player = getPlayer();
        if (player != null) {
            player.sendMessage(msg);
            return;
        }

        Document data = new Document();
        data.put("action", ProfileListener.ProfileAction.MESSAGE_PLAYER.name());
        data.put("chat-type", ChatMessageType.CHAT.name());
        data.put("message", msg);
        data.put("isComponent", false);

        ServerService serverService = this.getCache().getDatabase().getServerService();
        ServerPublisher publisher = serverService.getPublisher();
        publisher.publishPlayerEvent(getUUID(), true, data);
    }

    @SuppressWarnings("deprecation")
    public void sendMessage(Component component) {
        Player player = getPlayer();
        if (player != null) {
            player.sendMessage(component);
            return;
        }

        Document data = new Document();
        data.put("action", ProfileListener.ProfileAction.MESSAGE_PLAYER.name());
        data.put("chat-type", ChatMessageType.CHAT.name());
        data.put("message", GsonComponentSerializer.gson().serialize(component));
        data.put("isComponent", true);

        ServerService serverService = this.getCache().getDatabase().getServerService();
        ServerPublisher publisher = serverService.getPublisher();
        publisher.publishPlayerEvent(getUUID(), true, data);
    }

    /**
     * Sends an action bar component to this player, wherever they are located.
     *
     * @param component the component.
     */
    @SuppressWarnings("deprecation")
    public void sendActionBar(Component component) {
        Player player = getPlayer();
        if(player != null) {
            player.sendActionBar(component);
            return;
        }

        Document data = new Document();
        data.put("action", ProfileListener.ProfileAction.MESSAGE_PLAYER.name());
        data.put("chat-type", ChatMessageType.ACTION_BAR.name());
        data.put("message", GsonComponentSerializer.gson().serialize(component));
        data.put("isComponent", true);

        ServerService serverService = this.getCache().getDatabase().getServerService();
        ServerPublisher publisher = serverService.getPublisher();
        publisher.publishPlayerEvent(getUUID(), true, data);
    }

    /**
     * Sends a sound to this player at the player's location if they're online.
     *
     * @param sound the sound to play.
     * @param volume the volume.
     * @param pitch the pitch.
     */
    public void sendSound(Sound sound, float volume, float pitch) {
        this.sendSound(sound, volume, pitch, null);
    }

    /**
     * Sends a sound to the given player. If the soundLocation is null, it will use the player's location.
     *
     * @param sound the sound.
     * @param volume the volume of the sound.
     * @param pitch the pitch of the sound.
     * @param soundLocation the location of the sound.
     */
    public void sendSound(Sound sound, float volume, float pitch, Location soundLocation) {
        Player player = getPlayer();
        if(player != null) {
            player.playSound(soundLocation == null ? player.getLocation() : soundLocation, sound, volume, pitch);
            return;
        }

        Document data = new Document();
        data.put("action", ProfileListener.ProfileAction.PLAY_SOUND.name());
        data.put("sound-type", sound.name());
        data.put("sound-volume", volume);
        data.put("sound-pitch", pitch);

        if(soundLocation != null) {
            data.put("sound-location.x", soundLocation.getX());
            data.put("sound-location.y", soundLocation.getY());
            data.put("sound-location.z", soundLocation.getZ());
        }

        ServerService serverService = this.getCache().getDatabase().getServerService();
        ServerPublisher publisher = serverService.getPublisher();
        publisher.publishPlayerEvent(getUUID(), true, data);
    }

    /**
     * Sends the given title to the player.
     *
     * @param title the title.
     */
    public void sendTitle(Title title) {
        Player player = getPlayer();
        if(player != null) {
            player.showTitle(title);
            return;
        }

        Document data = new Document();
        data.put("action", ProfileListener.ProfileAction.SHOW_TITLE.name());
        data.put("title", GsonComponentSerializer.gson().serialize(title.title()));
        data.put("subtitle", GsonComponentSerializer.gson().serialize(title.subtitle()));
        Title.Times times = title.times();
        if(times != null) {
            data.put("times.fade-in", times.fadeIn().toMillis());
            data.put("times.fade-out", times.fadeOut().toMillis());
            data.put("times.stay", times.stay().toMillis());
        }

        ServerService serverService = this.getCache().getDatabase().getServerService();
        ServerPublisher publisher = serverService.getPublisher();
        publisher.publishPlayerEvent(getUUID(), true, data);
    }

    public boolean isInitialized() {
        return this.player != null;
    }

    @Nonnull
    @Override
    public String identifierFieldName() {
        return "uniqueId";
    }

    @Override
    public long cachedTimestamp() {
        return this.cachedTimestamp;
    }

    public String getName() {
        return this.getUsername();
    }

    @Deprecated
    public void msg(String msg) {
        this.sendMessage(msg);
    }

    @Deprecated
    public void msg(String msg, Object... args) {
        this.msg(PayloadPlugin.format(msg, args));
    }

    @Deprecated
    public void msg(BaseComponent component) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(component);
        }
    }

    @Deprecated
    public void msg(BaseComponent[] components) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(components);
        }
    }

    @Deprecated
    public void msgBuilder(String msg, MsgBuilder builder) {
        this.msg(builder.build(new ComponentBuilder(msg)));
    }

    @Deprecated
    public ComponentBuilder msgBuilder(String msg) {
        return new ComponentBuilder(msg);
    }

    @Override
    public String getPayloadServer() {
        return this.payloadId;
    }

    @Override
    public void setPayloadServer(String payloadID) {
        this.payloadId = payloadID;
    }

    @Override
    public boolean save() {
        return this.cache.save(this);
    }

    @Override
    public int hashCode() {
        int result = 1;
        Object $objectId = this.getObjectId();
        result = result * 59 + ($objectId == null ? 43 : $objectId.hashCode());
        Object $uniqueId = this.getUniqueId();
        result = result * 59 + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        return result;
    }

    private boolean canEqual(Object other) {
        return other instanceof PayloadProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PayloadProfile)) {
            return false;
        } else {
            PayloadProfile other = (PayloadProfile) o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                if (this.objectId != null && other.objectId != null) {
                    if (this.uniqueId != null && other.uniqueId != null) {
                        return this.objectId.equals(other.objectId)
                                && this.uniqueId.equalsIgnoreCase(other.uniqueId);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

}
