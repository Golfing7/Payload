/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadCallback;
import com.jonahseguin.payload.mode.profile.network.NetworkProfile;
import com.jonahseguin.payload.mode.profile.network.NetworkService;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ProfileCache<X extends PayloadProfile> extends Cache<UUID, X> {

    void prepareUpdate(@Nonnull X payload, @Nonnull PayloadCallback<X> callback);

    void prepareUpdateAsync(@Nonnull X payload, @Nonnull PayloadCallback<X> callback);

    Optional<NetworkProfile> getNetworked(@Nonnull UUID key);

    Optional<NetworkProfile> getNetworked(@Nonnull X payload);

    Optional<X> get(@Nonnull String username);

    Optional<X> get(@Nonnull Player player);

    Optional<X> getFromCache(@Nonnull String username);

    Optional<X> getFromCache(@Nonnull Player player);

    Optional<X> getFromDatabase(@Nonnull String username);

    Optional<X> getFromDatabase(@Nonnull Player player);

    @Nonnull
    Collection<X> getOnline();

    @Nonnull
    Collection<X> getOnlineInstances();

    Collection<String> getOnlineInstancesNames();

    Collection<UUID> getOnlineInstancesKeys();

    /**
     * Attempts to send a message to the given player. Does not require a handshake.
     *
     * @param playerUUID the player UUID.
     * @param text the message to send.
     */
    void sendMessage(@Nonnull UUID playerUUID, @Nonnull Component text);

    /**
     * Broadcasts a message to all players of this cache.
     *
     * @param text the message to send.
     */
    void broadcast(@Nonnull Component text);

    /**
     * Broadcasts a title to all online players of this cache.
     *
     * @param title the title.
     */
    void broadcastTitle(@Nonnull Title title);

    /**
     * Broadcasts a sound to all online players of this cache.
     *
     * @param sound the sound.
     * @param volume the volume of the sound.
     * @param pitch the pitch of the sound.
     */
    void broadcastSound(@Nonnull Sound sound, float volume, float pitch);

    /**
     * Broadcasts an action bar to all online players of this cache.
     *
     * @param text the action bar text.
     */
    void broadcastActionbar(@Nonnull Component text);

    boolean isCached(@Nonnull String username);

    boolean isCached(@Nonnull Player player);

    @Nonnull
    ProfileCacheSettings getSettings();

    @Override
    @Nonnull
    PayloadProfileController<X> controller(@Nonnull UUID key);

    NetworkService<X> getNetworkService();

    NetworkProfile createNetworked();

}
