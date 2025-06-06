/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadCallback;
import com.jonahseguin.payload.base.store.PayloadRemoteStore;
import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.base.type.PayloadInstantiator;
import com.jonahseguin.payload.base.uuid.UUIDService;
import com.jonahseguin.payload.mode.profile.handshake.ProfileHandshakeService;
import com.jonahseguin.payload.mode.profile.listener.ProfileListener;
import com.jonahseguin.payload.mode.profile.network.NetworkProfile;
import com.jonahseguin.payload.mode.profile.network.NetworkService;
import com.jonahseguin.payload.mode.profile.network.RedisNetworkService;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import com.jonahseguin.payload.mode.profile.store.ProfileStoreLocal;
import com.jonahseguin.payload.mode.profile.store.ProfileStoreMongo;
import com.jonahseguin.payload.mode.profile.update.ProfileUpdater;
import com.jonahseguin.payload.server.PayloadServer;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Singleton
public class PayloadProfileCache<X extends PayloadProfile> extends PayloadCache<UUID, X> implements ProfileCache<X> {

    private final ProfileCacheSettings settings = new ProfileCacheSettings();
    private final ConcurrentMap<UUID, PayloadProfileController<X>> controllers = new ConcurrentHashMap<>();
    private final ProfileStoreLocal<X> localStore = new ProfileStoreLocal<>(this);
    private final ProfileStoreMongo<X> mongoStore = new ProfileStoreMongo<>(this);
    @Inject
    private UUIDService uuidService;
    private NetworkService<X> networkService = null;
    private ProfileHandshakeService<X> handshakeService = null;
    private ProfileUpdater<X> profileUpdater = null;

    public PayloadProfileCache(Injector injector, PayloadInstantiator<UUID, X> instantiator, String name, Class<X> payload) {
        super(injector, instantiator, name, UUID.class, payload);
        this.setupModule();
    }

    public PayloadProfileCache(Injector injector, String name, Class<X> payload) {
        super(injector, name, UUID.class, payload);
        this.instantiator = (inj) -> instantiateByReflection();
        this.setupModule();
    }

    @Override
    protected void setupModule() {
        super.injectMe();
        super.setupModule();
        injector.injectMembers(this);
        networkService = new RedisNetworkService<>(this);
        handshakeService = new ProfileHandshakeService<>(this, database);
        profileUpdater = new ProfileUpdater<>(this, database);
    }

    @Override
    protected boolean initialize() {
        boolean success = true;
        if (!localStore.start()) {
            success = false;
            errorService.capture("Failed to start Local store for cache: " + name);
        }
        if (!mongoStore.start()) {
            success = false;
            errorService.capture("Failed to start MongoDB store for cache: " + name);
        }

        if (mode.equals(PayloadMode.NETWORK_NODE)) {
            if (!handshakeService.start()) {
                success = false;
                errorService.capture("Failed to start Profile Handshake Service (Network Node mode) for cache: " + name);
            }
            if (!networkService.start()) {
                success = false;
                errorService.capture("Failed to start Network Service (Network Node mode) for cache: " + name);
            }
            if (!profileUpdater.start()) {
                success = false;
                errorService.capture("Failed to start Profile Updater (Network Node mode) for cache: " + name);
            }
        }
        return success;
    }

    @Override
    protected boolean terminate() {
        boolean success = true;
        AtomicInteger failedSaves = new AtomicInteger(0);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            getFromCache(player).ifPresent(payload -> {
                if (settings.isSetOfflineOnShutdown()) {
                    getNetworked(payload).ifPresent(networkProfile -> {
                        networkProfile.markUnloaded(false);
                        networkService.save(networkProfile);
                    });
                }
                if (!save(payload)) {
                    failedSaves.getAndIncrement();
                }
            });
        }
        if (failedSaves.get() > 0) {
            errorService.capture(failedSaves + " objects failed to save during shutdown");
        }
        controllers.clear();
        if (!localStore.shutdown()) {
            success = false;
        }
        if (!mongoStore.shutdown()) {
            success = false;
        }
        if (mode.equals(PayloadMode.NETWORK_NODE)) {
            if (handshakeService.isRunning()) {
                if (!handshakeService.shutdown()) {
                    success = false;
                    errorService.capture("Failed to shutdown Profile Handshake Service (Network Node mode) for cache: " + name);
                }
            }
            if (networkService.isRunning()) {
                if (!networkService.shutdown()) {
                    success = false;
                    errorService.capture("Failed to shutdown Network Service (Network Node mode) for cache: " + name);
                }
            }
            if (profileUpdater.isRunning()) {
                if (!profileUpdater.shutdown()) {
                    success = false;
                    errorService.capture("Failed to shutdown Profile Updater (Network Node mode) for cache: " + name);
                }
            }
        }
        return success;
    }

    @Override
    public void prepareUpdate(@Nonnull X payload, @Nonnull PayloadCallback<X> callback) {
        Preconditions.checkNotNull(payload, "Payload cannot be null for prepareUpdate");
        Preconditions.checkNotNull(callback, "Callback cannot be null for prepareUpdate");
        if (mode.equals(PayloadMode.NETWORK_NODE)) {
            NetworkProfile networkProfile = getNetworked(payload).orElse(null);
            if (networkProfile != null) {
                if (networkProfile.isOnline()) {
                    if (!networkProfile.isOnlineThisServer()) {
                        PayloadServer server = database.getServerService().get(networkProfile.getLastSeenServer()).orElse(null);
                        if (server != null && server.isOnline()) {
                            if (!profileUpdater.requestSave(payload, server.getName(), callback)) {
                                errorService.capture("Failed to requestSave for prepareUpdate for Profile: " + payload.getName());
                            }
                        } else {
                            callback.callback(payload);
                            // Their last seen server isn't accurate / the server is offline
                        }
                    } else {
                        callback.callback(payload);
                        // They're online THIS server, meaning we already have the most up-to-date Profile instance
                    }
                } else {
                    callback.callback(payload);
                    // They aren't online at all, meaning we already have the most up-to-date Profile instance
                    // (since they are saved on logout/shutdown)
                }
            } else {
                // Fail hard.  They should have a NetworkProfile
                errorService.capture("Couldn't get a NetworkProfile (null) during prepareUpdate for Profile: " + payload.getName());
                callback.callback(null);
            }
        } else {
            // Fail nicely, so that end user doesn't have to check the cache's mode.  Instead just callback in STANDALONE
            // Since their version will be the most updated anyways.
            callback.callback(payload);
        }
    }

    @Override
    public void prepareUpdateAsync(@Nonnull X payload, @Nonnull PayloadCallback<X> callback) {
        runAsyncImmediately(() -> prepareUpdate(payload, callback));
    }

    @Override
    public Optional<NetworkProfile> getNetworked(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return networkService.get(key);
    }

    @Override
    public Optional<NetworkProfile> getNetworked(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        return networkService.get(payload);
    }

    @Override
    public void uncache(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        getLocalStore().remove(key);
    }

    @Override
    public void uncache(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        getLocalStore().remove(payload);
    }

    @Override
    public void deleteAll() {
        getLocalStore().clear();
        getDatabaseStore().clear();
    }

    @Nonnull
    @Override
    public Collection<X> getCached() {
        return localStore.getAll();
    }

    @Nonnull
    @Override
    public PayloadRemoteStore<UUID, X> getDatabaseStore() {
        return mongoStore;
    }

    @Override
    public Optional<X> get(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        Preconditions.checkState(username.length() > 1, "Username length must be > 1");
        Player player = plugin.getServer().getPlayerExact(username);
        if (player != null && player.isOnline()) {
            return get(player);
        }
        UUID uuid = uuidService.get(username).orElse(null);
        if (uuid != null) {
            if (isCached(uuid)) {
                return getFromCache(uuid);
            }
        }
        return mongoStore.getByUsername(username);
    }

    @Override
    public boolean isCached(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        Player player = plugin.getServer().getPlayerExact(username);
        if (player != null && player.isOnline()) {
            return isCached(player);
        }
        return uuidService.get(username).filter(this::isCached).isPresent();
    }

    @Override
    public void cache(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        Optional<X> o = getLocalStore().get(payload.getUniqueId());
        if (o.isPresent()) {
            updatePayloadFromNewer(o.get(), payload);
        } else {
            getLocalStore().save(payload);
        }
    }

    @Override
    public Optional<X> get(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return get(player.getUniqueId());
    }

    @Override
    public boolean isCached(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return isCached(player.getUniqueId());
    }

    @Override
    public Optional<X> get(@Nonnull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);
        PayloadProfileController<X> controller = this.controller(uniqueId);
        controller.setLogin(false);
        return controller.cache();
    }

    @Override
    public Optional<X> getNoCache(@NotNull UUID key) {
        return get(key);
    }

    @Nonnull
    @Override
    public Set<X> getOnline() {
        return this.localStore.getLocalCache().values().stream()
                .filter(PayloadProfile::isOnline)
                .collect(Collectors.toSet());
    }

    @NotNull
    @Override
    public Collection<X> getOnlineInstances() {
        return this.networkService.getOnline().stream()
                .map(networkProfile -> {
                    try {
                        UUID identifier = networkProfile.getIdentifier();
                        if (identifier == null) {
                            return null;
                        }

                        Optional<X> optional = this.get(identifier);
                        return optional.orElse(null);
                    } catch (Exception ignore) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Collection<String> getOnlineInstancesNames() {
        return this.networkService.getOnline().stream()
                .map(NetworkProfile::getLastKnownName).collect(Collectors.toList());
    }

    @Override
    public Collection<UUID> getOnlineInstancesKeys() {
        return this.networkService.getOnline().stream()
                .map(NetworkProfile::getUuidID).collect(Collectors.toList());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(@NotNull UUID playerUUID, @NotNull Component text) {
        Preconditions.checkNotNull(playerUUID);
        Preconditions.checkNotNull(text);

        Document data = new Document();
        data.put("action", ProfileListener.ProfileAction.MESSAGE_PLAYER.name());
        data.put("chat-type", ChatMessageType.CHAT.name());
        data.put("message", GsonComponentSerializer.gson().serialize(text));
        data.put("isComponent", true);

        getServerService().getPublisher().publishPlayerEvent(playerUUID, true, data);
    }

    @Override
    public void broadcast(@NotNull Component text) {
        Preconditions.checkNotNull(text);

        Document data = new Document();
        data.put("action", ProfileListener.ServerAction.BROADCAST.name());
        data.put("message", GsonComponentSerializer.gson().serialize(text));

        getServerService().getPublisher().publishServerEvent(null, data);
    }

    @Override
    public void broadcastActionbar(@NotNull Component text) {
        Preconditions.checkNotNull(text);

        Document data = new Document();
        data.put("action", ProfileListener.ServerAction.BROADCAST_ACTIONBAR.name());
        data.put("message", GsonComponentSerializer.gson().serialize(text));

        getServerService().getPublisher().publishServerEvent(null, data);
    }

    @Override
    public void broadcastSound(@NotNull Sound sound, float volume, float pitch) {
        Preconditions.checkNotNull(sound);

        Document data = new Document();
        data.put("action", ProfileListener.ServerAction.BROADCAST_SOUND.name());
        data.put("sound-type", sound.getKey().toString());
        data.put("sound-volume", volume);
        data.put("sound-pitch", pitch);

        getServerService().getPublisher().publishServerEvent(null, data);
    }

    @Override
    public void broadcastTitle(@NotNull Title title) {
        Preconditions.checkNotNull(title);

        Document data = new Document();
        data.put("action", ProfileListener.ServerAction.BROADCAST_TITLE.name());
        data.put("title", GsonComponentSerializer.gson().serialize(title.title()));
        data.put("subtitle", GsonComponentSerializer.gson().serialize(title.subtitle()));
        Title.Times times = title.times();
        if(times != null) {
            data.put("times.fade-in", times.fadeIn().toMillis());
            data.put("times.fade-out", times.fadeOut().toMillis());
            data.put("times.stay", times.stay().toMillis());
        }

        getServerService().getPublisher().publishServerEvent(null, data);
    }

    @Override
    public boolean isCached(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return this.localStore.has(key);
    }

    @Override
    public Optional<X> getFromCache(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return this.localStore.get(key);
    }

    @Override
    public Optional<X> getFromDatabase(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return mongoStore.get(key);
    }

    @Override
    public Optional<X> getFromCache(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        UUID uuid = null;
        Player player = plugin.getServer().getPlayerExact(username);
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            Optional<UUID> o = uuidService.get(username);
            if (o.isPresent()) {
                uuid = o.get();
            }
        }
        if (uuid != null) {
            return getFromCache(uuid);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<X> getFromCache(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return getFromCache(player.getUniqueId());
    }

    @Override
    public Optional<X> getFromDatabase(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        return mongoStore.getByUsername(username);
    }

    @Override
    public Optional<X> getFromDatabase(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return mongoStore.get(player.getUniqueId());
    }

    @Override
    public void delete(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        if (mode == PayloadMode.NETWORK_NODE) {
            try {
                this.get(key).ifPresent(payload -> this.getUpdater().pushDelete(payload));
            } catch (Exception ignored) {
            }
        }

        controllers.remove(key);
        localStore.remove(key);
        mongoStore.remove(key);
    }

    @Override
    public void cacheAll() {
        this.getAll().forEach(this::cache);
    }

    @Override
    public long deleteInvalidCaches() {
        return getDatabaseStore().deleteInvalids();
    }

    @Override
    public UUID keyFromString(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        return UUID.fromString(key);
    }

    @Override
    public String keyToString(@Nonnull UUID key) {
        return key.toString();
    }

    @Nonnull
    public Set<X> getAll() {
        final Set<X> all = this.localStore.getAll().stream().filter(PayloadProfile::isOnline).collect(Collectors.toSet());
        Collection<X> allOnMongo = this.mongoStore.getAll();
        for(X payload : allOnMongo) {
            if(all.stream().noneMatch(thing -> thing.getUniqueId().equals(payload.getUniqueId())))
                all.add(payload);
        }
        return all;
    }

    @Nonnull
    @Override
    public PayloadProfileController<X> controller(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return controllers.computeIfAbsent(key, s -> new PayloadProfileController<>(this, s));
    }

    @Override
    public void saveAsync(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        this.cache(payload);
        this.runAsyncImmediately(() -> this.save(payload));
    }

    @Override
    public boolean save(@Nonnull X payload) {
        Preconditions.checkNotNull(payload, "Cannot save a null Payload");
        cache(payload);
        if (mode.equals(PayloadMode.NETWORK_NODE)) {
            Optional<NetworkProfile> onp = networkService.get(payload);
            if (onp.isPresent()) {
                NetworkProfile np = onp.get();
                if (saveMongo(payload)) {
                    np.markSaved();
                    if (networkService.save(np)) {
                        return true;
                    } else {
                        errorService.capture("Failed to save profile " + payload.getName() + ": Couldn't save network profile (but saved normal profile)");
                        return false;
                    }
                } else {
                    errorService.capture("Failed to save profile " + payload.getName() + ": Failed to save to database (via saveMongo())");
                    return false;
                }
            } else {
                errorService.capture("Failed to save profile " + payload.getName() + ": Network Profile doesn't exist (should have been created)");
                return false;
            }
        } else {
            return saveMongo(payload);
        }
    }

    @Override
    public boolean saveAndUpdate(@NotNull X payload) {
        if(!this.save(payload))
            return false;

        this.pushUpdate(payload, true);
        return true;
    }

    @Override
    public boolean saveNoCache(@NotNull X payload) {
        return save(payload);
    }

    private boolean saveMongo(@Nonnull X payload) {
        Preconditions.checkNotNull(payload, "Cannot save a null Payload (saveMongo)");
        boolean mongo = mongoStore.save(payload);
        if (mongo) {
            payload.setSaveFailed(false);
            payload.setLastSaveTimestamp(System.currentTimeMillis());
            payload.interact();
        } else {
            payload.setSaveFailed(true);
        }
        return mongo;
    }

    @Override
    public int saveAll() {
        int failures = 0;
        for (Player p : this.getPlugin().getServer().getOnlinePlayers()) {
            Optional<X> o = this.get(p);
            if (o.isPresent()) {
                X payload = o.get();
                payload.interact();

                try {
                    if (!this.save(payload)) {
                        failures++;
                    }
                } catch (Throwable exc) {
                    Bukkit.getLogger().info(String.format("[%s] - Encountered an error while saving cache %s!", payloadPlugin.getName(), payload.getIdentifier()));
                    exc.printStackTrace();
                }
            } else {
                failures++;
            }
        }
        return failures;
    }

    public PayloadProfileController<X> getController(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        return this.controllers.get(uuid);
    }

    public void removeController(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        this.controllers.remove(uuid);
    }

    @Override
    public boolean requireRedis() {
        return true;
    }

    @Override
    public boolean requireMongoDb() {
        return true;
    }

    @Nonnull
    @Override
    public ProfileCacheSettings getSettings() {
        return this.settings;
    }

    @Override
    public void updatePayloadID() {
        for (X x : this.getCached()) {
            x.setPayloadId(api.getPayloadID());
            getNetworked(x).ifPresent(np -> {
                if (np.isOnlineThisServer()) {
                    np.setLastSeenServer(serverService.getThisServer().getName());
                    getNetworkService().save(np);
                }
            });
        }
    }

    @Override
    public NetworkProfile createNetworked() {
        return injector.getInstance(NetworkProfile.class);
    }
}
