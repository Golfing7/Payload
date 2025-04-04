/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.inject.Injector;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.base.lang.PLangService;
import com.jonahseguin.payload.base.settings.CacheSettings;
import com.jonahseguin.payload.base.store.PayloadRemoteStore;
import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadController;
import com.jonahseguin.payload.base.type.PayloadInstantiator;
import com.jonahseguin.payload.database.DatabaseDependent;
import com.jonahseguin.payload.database.DatabaseService;
import dev.morphia.query.filters.Filter;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface Cache<K, X extends Payload<K>> extends Service, DatabaseDependent {

    boolean pushUpdate(@Nonnull X payload);

    boolean pushUpdate(@Nonnull X payload, boolean forceLoad);

    Collection<X> getWhere(@Nonnull Filter... filters);

    Optional<X> get(@Nonnull K key);

    /**
     * Gets the element without caching it at the end if this is an object cache.
     * @return the optional of the element.
     */
    Optional<X> getNoCache(@Nonnull K key);

    Optional<X> getFromCache(@Nonnull K key);

    Optional<X> getFromDatabase(@Nonnull K key);

    boolean save(@Nonnull X payload);

    /**
     * Saves the given payload and updates it in the payload.
     *
     * @param payload the payload to save and update.
     * @return true if it worked.
     */
    boolean saveAndUpdate(@Nonnull X payload);

    /**
     * Saves the element without caching it if this is an object cache.
     * @return If the save is successful.
     */
    boolean saveNoCache(@Nonnull X key);

    void saveAsync(@Nonnull X payload);

    void cache(@Nonnull X payload);

    void uncache(@Nonnull K key);

    void uncache(@Nonnull X payload);

    void delete(@Nonnull K key);

    void delete(@Nonnull X payload);

    void deleteAll();

    boolean isCached(@Nonnull K key);

    void cacheAll();

    long deleteInvalidCaches();

    @Nonnull
    Collection<X> getAll();

    @Nonnull
    Collection<X> getCached();

    @Nonnull
    ErrorService getErrorService();

    void setErrorService(@Nonnull ErrorService errorService);

    @Nonnull
    CacheSettings getSettings();

    int saveAll();

    @Nonnull
    PayloadStore<K, X> getLocalStore();

    @Nonnull
    PayloadRemoteStore<K, X> getDatabaseStore();

    @Nonnull
    String getName();

    @Nonnull
    String getServerSpecificName();

    @Nonnull
    Plugin getPlugin();

    @Nonnull
    PayloadMode getMode();

    void setMode(@Nonnull PayloadMode mode);

    void setInstantiator(@Nonnull PayloadInstantiator<K, X> instantiator);

    String keyToString(@Nonnull K key);

    K keyFromString(@Nonnull String key);

    void addDepend(@Nonnull Cache cache);

    boolean isDependentOn(@Nonnull Cache cache);

    @Nonnull
    PayloadAPI getApi();

    void runAsync(@Nonnull Runnable runnable);

    void runAsyncImmediately(@Nonnull Runnable runnable);

    @Nonnull
    PLangService getLang();

    boolean isDebug();

    void setDebug(boolean debug);

    void alert(@Nonnull PayloadPermission required, @Nonnull String msg);

    void updatePayloadID();

    int cachedObjectCount();

    /**
     * Gets the total amount of objects in this cache.
     *
     * @return the amount of objects.
     */
    long cacheSize();

    @Nonnull
    PayloadController<X> controller(@Nonnull K key);

    DatabaseService getDatabase();

    X create();

    Injector getInjector();

}

