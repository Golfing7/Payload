package com.jonahseguin.payload.database.redis.packets;

import java.util.UUID;

public record UUIDResponse(UUID response, String name){
    public boolean isSuccessful(){return response != null;}
}
