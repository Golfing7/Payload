package com.jonahseguin.payload.database.redis.packets;

public interface PacketHandler {

    void receive(PayloadPacket packet);
}
