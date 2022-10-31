package com.jonahseguin.payload.database.redis;

import com.jonahseguin.payload.database.redis.packets.PayloadPacket;
import com.jonahseguin.payload.database.redis.packets.channel.Channel;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.pubsub.RedisPubSubListener;

import java.util.concurrent.Future;

/**
 * Opens access to useful redis methods. Specifically ones useful in transferring data between instances, for plugins.
 */
public interface RedisAccess {
    /**
     * Maps a string key to a specific value, regardless of what is previously set in the database.
     * @param key the key to set.
     * @param value the value to map to.
     * @return a future of the state of the operation, true if successful, false if not.
     */
    Future<Boolean> setString(String key, String value);
    /**
     * Maps a string key to a specific value, if the key was already present, this operation does nothing.
     * @param key the key to set.
     * @param value the value to map to.
     * @return a future of the state of the operation, true if the key was set, false if it already existed.
     */
    Future<Boolean> setIfMissing(String key, String value);

    /**
     * Remove the key from the redis database.
     * @param key the key to remove.
     */
    void removeKey(String key);

    /**
     * Registers a given channel to the redis pub sub.
     * @param channel the channel to register.
     */
    void registerPacketChannel(Channel channel);

    /**
     * Unregisters a given channel from the redis pubsub.
     * @param channel the channel to unregister.
     */
    void unregisterPacketChannel(Channel channel);

    void sendOnPacketChannel(Channel channel, PayloadPacket packet);
}
