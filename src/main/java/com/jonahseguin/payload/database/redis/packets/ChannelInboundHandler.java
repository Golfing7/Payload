package com.jonahseguin.payload.database.redis.packets;

public interface ChannelInboundHandler {
    void onPacketFailure(PayloadPacket packet);
}
