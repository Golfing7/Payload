package com.jonahseguin.payload.database.redis.packets.channel;

import com.jonahseguin.payload.database.redis.RedisAccess;
import com.jonahseguin.payload.database.redis.packets.ChannelInboundHandler;

public final class SimpleChannel extends Channel{
    public SimpleChannel(RedisAccess access, String channelName, ChannelInboundHandler handler) {
        super(channelName, handler);
    }

    @Override
    public void message(String s, String s2) {

    }

    @Override
    public void message(String s, String k1, String s2) {

    }

    @Override
    public void subscribed(String s, long l) {

    }

    @Override
    public void psubscribed(String s, long l) {

    }

    @Override
    public void unsubscribed(String s, long l) {

    }

    @Override
    public void punsubscribed(String s, long l) {

    }
}
