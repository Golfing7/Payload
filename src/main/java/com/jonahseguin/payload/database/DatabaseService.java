/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.server.ServerService;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import javax.annotation.Nonnull;
import java.util.Set;

public interface DatabaseService extends Service {

    MongoClient getMongoClient();

    Morphia getMorphia();

    Datastore getDatastore();

    MongoDatabase getDatabase();

    RedisClient getRedisClient();

    /**
     * Generates a database number prefixed channel name from the given unformatted name.
     *
     * @param unformatted the unformatted string.
     * @return the prefixed channel name.
     * @see <a href="https://redis.io/docs/manual/pubsub/#database--scoping">Redis Database Scoping</a>
     */
    String generatePrefixedChannelName(String unformatted);

    StatefulRedisConnection<String, String> getRedis();

    StatefulRedisPubSubConnection<String, String> getRedisPubSub();

    ServerService getServerService();

    boolean isConnected();

    boolean canFunction(@Nonnull DatabaseDependent dependent);

    String getName();

    void hook(DatabaseDependent dependent);

    Set<DatabaseDependent> getHooks();

    ErrorService getErrorService();

    DatabaseState getState();


}
