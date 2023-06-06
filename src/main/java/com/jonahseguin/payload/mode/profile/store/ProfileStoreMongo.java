/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.store;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.store.PayloadRemoteStore;
import com.jonahseguin.payload.base.type.PayloadQueryModifier;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileCache;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.morphia.query.CriteriaContainer;
import dev.morphia.query.Query;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProfileStoreMongo<X extends PayloadProfile> extends ProfileCacheStore<X> implements PayloadRemoteStore<UUID, X> {

    private final Set<PayloadQueryModifier<X>> queryModifiers = new HashSet<>();
    private boolean running = false;

    public ProfileStoreMongo(PayloadProfileCache<X> cache) {
        super(cache);
    }

    @Override
    public Optional<X> get(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        try {
            Query<X> q = getQuery(key);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> o = stream.findFirst();
            o.ifPresent(x -> x.setLoadingSource(layerName()));
            return o;
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error getting Profile from MongoDB Layer: " + key.toString());
            return Optional.empty();
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error getting Profile from MongoDB Layer: " + key.toString());
            return Optional.empty();
        }
    }

    @Override
    public boolean has(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        try {
            return getQuery(uuid).find().toList().stream().findAny().isPresent();
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error check if Profile exists in MongoDB Layer: " + uuid.toString());
            return false;
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error checking if Profile exists in MongoDB Layer: " + uuid.toString());
            return false;
        }
    }

    @Override
    public void remove(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        try {
            Query<X> q = getQuery(key);
            cache.getDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error removing Profile from MongoDB Layer: " + key.toString());
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error removing Profile from MongoDB Layer: " + key.toString());
        }
    }

    public Optional<X> getByUsername(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        try {
            Query<X> q = getQueryForUsername(username);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            X x = xp.orElse(null);
            if (x != null) {
                x.interact();
                x.setLoadingSource(layerName());
            }
            return xp;
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error getting Profile from MongoDB Layer: " + username);
            return Optional.empty();
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error getting Profile from MongoDB Layer: " + username);
            return Optional.empty();
        }
    }


    public boolean save(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        payload.interact();
        try {
            cache.getDatabase().getDatastore().save(payload);
            return true;
        } catch (MongoException ex) {
            ex.printStackTrace();
            getCache().getErrorService().capture(ex, "MongoDB error saving Profile to MongoDB Layer: " + payload.getUsername());
            return false;
        } catch (Exception expected) {
            expected.printStackTrace();
            getCache().getErrorService().capture(expected, "Error saving Profile to MongoDB Layer: " + payload.getUsername());
            return false;
        }
    }

    @Override
    public boolean has(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        return has(payload.getUniqueId());
    }

    @Override
    public void remove(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        remove(payload.getUniqueId());
    }


    @Override
    public int cleanup() {
        return 0;
    }

    @Override
    @Nonnull
    public Collection<X> getAll() {
        return createQuery().find().toList();
    }

    @Override
    public long clear() {
        // For safety reasons...
        if(!PayloadPlugin.DEVELOPMENT_MODE)
            throw new UnsupportedOperationException("Not supported when not in development mode!");

        try {
            MongoDatabase database = cache.getDatabase().getMongoClient().getDatabase(cache.getDatabase().getName());

            MongoCollection<?> collection = database.getCollection(cache.getEntityName());

            long l = collection.countDocuments();

            collection.deleteMany(new Document());

            return l;
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error removing Profile from MongoDB Layer (Removing All)");
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error removing Profile from MongoDB Layer (Removing All)");
        }
        return -1;
    }

    @Override
    public long deleteInvalids() {
        try {
            MongoDatabase database = cache.getDatabase().getMongoClient().getDatabase(cache.getDatabase().getName());

            MongoCollection<Document> collection = database.getCollection(cache.getEntityName());

            List<Document> found = Lists.newArrayList();

            for(Document document : collection.find()){
                found.add(document);
            }

            long removed = 0L;

            for(Document document : found){
                String identifier = document.getString("uniqueId");

                if(identifier == null)
                    continue;

                UUID uuid = UUID.fromString(identifier);

                try{
                    Query<X> q = getQuery(uuid);
                    q.find().toList();
                }catch(Throwable thr){
                    collection.deleteOne(document);
                    removed++;
                }
            }

            return removed;
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error removing Object from MongoDB Layer (Removing Invalids)");
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error removing Object from MongoDB Layer (Removing Invalids)");
        }
        return 0;
    }

    @Override
    public long size() {
        return cache.getDatabase().getDatastore().find(cache.getPayloadClass()).count();
    }

    @Override
    public boolean start() {
        boolean success = true;
        if (!cache.getDatabase().isRunning()) {
            cache.getErrorService().capture("Error initializing MongoDB Profile Layer: Payload Database is not connected");
            success = false;
        }
        if (cache.getSettings().isServerSpecific()) {
            addCriteriaModifier(query -> query.field("payloadId").equalIgnoreCase(cache.getApi().getPayloadID()));
        }
        running = true;
        return success;
    }

    @Override
    public boolean shutdown() {
        running = false;
        // Do nothing here.  MongoDB object closing will be handled when the cache shuts down.
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Nonnull
    @Override
    public String layerName() {
        return "Profile MongoDB";
    }

    public void addCriteriaModifier(PayloadQueryModifier<X> modifier) {
        queryModifiers.add(modifier);
    }

    public void removeCriteriaModifier(PayloadQueryModifier<X> modifier) {
        queryModifiers.remove(modifier);
    }

    public void applyQueryModifiers(Query<X> query) {
        for (PayloadQueryModifier<X> modifier : queryModifiers) {
            modifier.apply(query);
        }
    }

    public Query<X> createQuery() {
        Query<X> q = cache.getDatabase().getDatastore().createQuery(cache.getPayloadClass());
        applyQueryModifiers(q);
        return q;
    }

    @Override
    public int deleteWhere(@NotNull CriteriaContainer criteria) {
        Query<X> newQuery = createQuery();
        newQuery.and(criteria);
        var result = this.cache.getDatabase().getDatastore().delete(newQuery);
        return result.getN();
    }

    public Query<X> getQuery(UUID uniqueId) {
        Query<X> q = createQuery();
        q.criteria("uniqueId").equalIgnoreCase(uniqueId.toString());
        return q;
    }

    public Query<X> getQueryForUsername(String username) {
        Query<X> q = createQuery();
        q.criteria("username").equalIgnoreCase(username);
        return q;
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

    @NotNull
    @Override
    public Collection<X> queryPayloads(Query<X> q) {
        Preconditions.checkNotNull(q);
        try {
            Stream<X> stream = q.find().toList().stream().filter(o -> {
                o.setLoadingSource(layerName());
                return true;
            });
            return stream.toList();
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error getting Profiles from MongoDB Layer");
            return Collections.emptyList();
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error getting Profile from MongoDB Layer");
            return Collections.emptyList();
        }
    }

    @NotNull
    @Override
    public Collection<X> queryPayloads(CriteriaContainer criteriaContainer) {
        Query<X> newQuery = createQuery();
        newQuery.and(criteriaContainer);
        return newQuery.find().toList();
    }

}
