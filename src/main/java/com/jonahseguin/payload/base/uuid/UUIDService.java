/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.uuid;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadPlugin;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class UUIDService {

    private final PayloadPlugin payloadPlugin;
    private final BiMap<UUID, String> cache = Maps.synchronizedBiMap(HashBiMap.create());

    @Inject
    public UUIDService(@Nonnull PayloadPlugin payloadPlugin) {
        Preconditions.checkNotNull(payloadPlugin);
        this.payloadPlugin = payloadPlugin;
    }

    public void save(@Nonnull UUID uuid, @Nonnull String name) {
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(name);
        //Use force put in case we've already cached it and are simply updating.
        cache.forcePut(uuid, name.toLowerCase());
    }

    public Optional<UUID> get(@Nonnull String name) {
        Preconditions.checkNotNull(name);
        return Optional.ofNullable(cache.inverse().get(name.toLowerCase()));
    }

    public Optional<String> get(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        return Optional.ofNullable(cache.get(uuid));
    }

    public boolean isCached(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        return cache.containsKey(uuid);
    }

    public boolean isCached(@Nonnull String name) {
        Preconditions.checkNotNull(name);
        return cache.inverse().containsKey(name);
    }

    public Optional<String> getNameFromOfflinePlayer(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        OfflinePlayer offlinePlayer = payloadPlugin.getServer().getOfflinePlayer(uuid);
        if (offlinePlayer != null) {
            return Optional.ofNullable(offlinePlayer.getName());
        }
        return Optional.empty();
    }

}
