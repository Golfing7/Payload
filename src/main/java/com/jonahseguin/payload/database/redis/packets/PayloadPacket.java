package com.jonahseguin.payload.database.redis.packets;

import com.google.common.collect.Lists;
import com.jonahseguin.payload.database.redis.packets.type.PacketTypeAdapter;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public abstract class PayloadPacket {
    public static final AtomicLong PACKET_COUNT = new AtomicLong(0L);
    @Getter
    private final UUID sender, receiver;
    @Getter @Setter
    private transient long packetUID;

    public PayloadPacket(UUID sender, UUID receiver, String... args)
    {
        this.sender = sender;
        this.receiver = receiver;

        this.packetUID = PACKET_COUNT.getAndIncrement();

        try{
            loadFields(args);
        }catch(IllegalAccessException exc){
            exc.printStackTrace();
        }
    }

    public String[] encode() throws IllegalAccessException {
        List<String> encoded = Lists.newArrayList();
        Class<?> clazz = this.getClass();
        while(PayloadPacket.class.isAssignableFrom(clazz)){
            for(Field field : clazz.getDeclaredFields()){
                if((field.getModifiers() & Modifier.TRANSIENT) != 0 ||
                        (field.getModifiers() & Modifier.FINAL) != 0 ||
                        (field.getModifiers() & Modifier.STATIC) != 0)
                    continue;

                for(PacketTypeAdapter adapter : PacketTypeAdapter.values()){
                    if(adapter.typeEquals(field.getType())){
                        field.setAccessible(true);

                        encoded.add(adapter.toPacketString(field.get(this)));
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }
        return encoded.toArray(new String[0]);
    }

    private void loadFields(String... types) throws IllegalAccessException {
        if(types.length == 0)
            return;

        int parseIndex = 0;

        Class<?> clazz = this.getClass();

        while(PayloadPacket.class.isAssignableFrom(clazz)){
            for(Field field : clazz.getDeclaredFields()){
                if((field.getModifiers() & Modifier.TRANSIENT) != 0 ||
                        (field.getModifiers() & Modifier.FINAL) != 0 ||
                        (field.getModifiers() & Modifier.STATIC) != 0)
                    continue;

                for(PacketTypeAdapter adapter : PacketTypeAdapter.values()){
                    if(adapter.typeEquals(field.getType())){
                        field.setAccessible(true);

                        field.set(this, adapter.transform(types[parseIndex++]));
                    }
                }
            }

            clazz = clazz.getSuperclass();
        }
    }
}
