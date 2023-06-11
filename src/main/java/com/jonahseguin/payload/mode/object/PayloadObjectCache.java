/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadInstantiator;
import com.jonahseguin.payload.mode.object.settings.ObjectCacheSettings;
import com.jonahseguin.payload.mode.object.store.ObjectStoreLocal;
import com.jonahseguin.payload.mode.object.store.ObjectStoreMongo;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Singleton
public class PayloadObjectCache<X extends PayloadObject> extends PayloadCache<String, X> implements ObjectCache<X> {

    private final ObjectCacheSettings settings = new ObjectCacheSettings();
    private final ConcurrentMap<String, PayloadObjectController<X>> controllers = new ConcurrentHashMap<>();
    private final ObjectStoreLocal<X> localStore = new ObjectStoreLocal<>(this);
    private final ObjectStoreMongo<X> mongoStore = new ObjectStoreMongo<>(this);
    protected String identifierFieldName;

    public PayloadObjectCache(Injector injector, PayloadInstantiator<String, X> instantiator, String name, Class<X> payload) {
        super(injector, instantiator, name, String.class, payload);
        setupModule();

        this.findIDFieldName();
    }

    /**
     * A package-private constructor that allows future assignment of the instantiator field.
     *
     * @param injector the injector.
     * @param name the name.
     * @param payload the payload class.
     */
    PayloadObjectCache(Injector injector, String name, Class<X> payload) {
        super(injector, name, String.class, payload);
        setupModule();

        this.findIDFieldName();
    }

    private void findIDFieldName(){
        try{
            PayloadObject object = payloadClass.getConstructor().newInstance();

            identifierFieldName = object.identifierFieldName();
        }catch (InvocationTargetException | InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            try {
                // Try again with a different constructor.
                PayloadObject object = payloadClass.getConstructor(ObjectCache.class).newInstance(this);

                identifierFieldName = object.identifierFieldName();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException ex) {
                Bukkit.getLogger().warning("Unable to find identifier field name for object cache: " + name + "!");
                e.printStackTrace();

                identifierFieldName = "identifier";
            }
        }
    }

    @Override
    protected void setupModule() {
        super.injectMe();
        super.setupModule();
        injector.injectMembers(this);
    }

    @Override
    protected boolean initialize() {
        boolean success = true;
        if (settings.isUseMongo()) {
            if (!mongoStore.start()) {
                success = false;
                errorService.capture("Failed to start MongoDB store");
            }
        }
        return success;
    }

    @Override
    protected boolean terminate() {
        boolean success = true;
        AtomicInteger failedSaves = new AtomicInteger(0);
        getCached().forEach(payload -> {
            if (!save(payload)) {
                failedSaves.getAndIncrement();
            }
        });
        if (failedSaves.get() > 0) {
            errorService.capture(failedSaves + " objects failed to save during shutdown");
            success = false;
        }

        controllers.clear();
        if (!localStore.shutdown()) {
            success = false;
        }
        if (!mongoStore.shutdown()) {
            success = false;
        }

        return success;
    }

    @Nonnull
    @Override
    public PayloadObjectController<X> controller(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        return controllers.computeIfAbsent(key, s -> new PayloadObjectController<>(this, s));
    }

    @Nonnull
    @Override
    public PayloadStore<String, X> getDatabaseStore() {
        return mongoStore;
    }

    @Override
    public String keyToString(@Nonnull String key) {
        return key;
    }

    @Nonnull
    @Override
    public ObjectCacheSettings getSettings() {
        return settings;
    }

    @Override
    public String keyFromString(@Nonnull String key) {
        return key;
    }

    @Override
    public boolean saveAndUpdate(@NotNull X payload) {
        if(!save(payload))
            return false;
        this.pushUpdate(payload, true);
        return true;
    }

    @Override
    public void deleteAll() {
        getLocalStore().clear();
        getDatabaseStore().clear();
    }

    @Override
    public long deleteInvalidCaches() {
        return getDatabaseStore().deleteInvalids();
    }

    @Override
    public void cacheAll() {
        getAll().forEach(this::cache);
    }

    @Nonnull
    @Override
    public Set<X> getAll() {
        //Store the values from their key to themselves.
        Map<String, X> values = Maps.newHashMap();

        localStore.getAll().forEach(value -> values.put(value.getIdentifier(), value));
        if (settings.isUseMongo()) {
            mongoStore.getAll().forEach(value -> {
                values.putIfAbsent(value.getIdentifier(), value);
            });
        }
        return Sets.newHashSet(values.values());
    }

    @Override
    public int saveAll() {
        AtomicInteger failures = new AtomicInteger();
        for (X object : localStore.getAll()) {
            try{
                if (!save(object)) {
                    failures.getAndIncrement();
                }
            }catch(Throwable exc){
                Bukkit.getLogger().info(String.format("[%s] - Encountered an error while saving cache %s!", payloadPlugin.getName(), object.getIdentifier()));
                exc.printStackTrace();
            }
        }
        return failures.get();
    }

    @Override
    public boolean requireRedis() {
        return settings.isUseRedis();
    }

    @Override
    public boolean requireMongoDb() {
        return settings.isUseMongo();
    }

    @Nonnull
    @Override
    public Collection<X> getCached() {
        return localStore.getLocalCache().values();
    }

    @Override
    public void updatePayloadID() {
        getCached().forEach(o -> o.setPayloadId(api.getPayloadID()));
    }
}
