package com.jonahseguin.payload.database.redis.packets;

import java.util.UUID;

public abstract class RedisPacket extends PayloadPacket {
    public RedisPacket(UUID sender, String... args)
    {
        super(sender, null, args);
    }

    public UUID getPlayer(){
        return getSender() != null ? getSender() : getReceiver();
    }
}
