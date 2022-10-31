package com.jonahseguin.payload.database.redis.packets.channel;

import com.jonahseguin.payload.database.redis.RedisAccess;
import com.jonahseguin.payload.database.redis.packets.ChannelInboundHandler;
import com.jonahseguin.payload.database.redis.packets.PacketMethod;
import com.jonahseguin.payload.database.redis.packets.PayloadPacket;
import com.jonahseguin.payload.database.redis.packets.type.PacketLoadException;
import io.lettuce.core.pubsub.RedisPubSubListener;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.UUID;

public abstract class Channel implements RedisPubSubListener<String, String> {
    private final String channelName;

    private final Int2ObjectMap<Class<? extends PayloadPacket>> packetIDMap;
    private final IdentityHashMap<Class<? extends PayloadPacket>, Integer> innerPacket2Id;

    private final Int2ObjectMap<MethodHandle> handlerMethods;

    private final ChannelInboundHandler handler;

    private final RedisAccess access;

    public Channel(RedisAccess access, String channelName, ChannelInboundHandler handler) {
        this.access = access;
        this.channelName = channelName;
        this.handler = handler;
        this.packetIDMap = new Int2ObjectOpenHashMap<>();
        this.innerPacket2Id = new IdentityHashMap<>();
        this.registerPacketTypes();
        this.handlerMethods = new Int2ObjectOpenHashMap<>();
        this.loadMethodHandles(handler);
    }

    protected abstract void registerPacketTypes();

    private int packetID;

    protected final void addPacketType(Class<? extends PayloadPacket> clazz) {
        innerPacket2Id.put(clazz, packetID);
        packetIDMap.put(packetID++, clazz);
    }

    public String getChannelName() {
        return channelName;
    }

    public void send(PayloadPacket packet) {
        Optional<? extends Player> first = Bukkit.getOnlinePlayers().stream().findFirst();

        if (first.isEmpty())
            throw new PacketLoadException("Failed to send packet! No players online!");

        send(first.get(), packet);
    }

    public void send(Player player, PayloadPacket packet) {
        access.sendOnPacketChannel(this, packet);
    }

    public void receiveFailure(int packetID, UUID receiver, UUID sender, long packetUID, String... data) {
        Class<? extends PayloadPacket> type = packetIDMap.get(packetID);

        try {
            Constructor<? extends PayloadPacket> constructor = receiver == null ?
                    type.getConstructor(UUID.class, String[].class) :
                    type.getConstructor(UUID.class, UUID.class, String[].class);

            PayloadPacket packet = receiver == null ? constructor.newInstance(sender, data) : constructor.newInstance(sender, receiver, data);

            packet.setPacketUID(packetUID);

            handler.onPacketFailure(packet);
        } catch (NoSuchMethodException exc) {
            Bukkit.getLogger().info("Malformed packet: %s! Requires constructor!".formatted(type.getName()));
            exc.printStackTrace();
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            Bukkit.getLogger().info("Failed to create packet!");
            e.printStackTrace();
        } catch (Throwable throwable) {
            Bukkit.getLogger().info("Method failed to invoke!");
            throwable.printStackTrace();
        }
    }

    public void receive(int packetID, UUID receiver, UUID sender, long packetUID, String... data) {
        Class<? extends PayloadPacket> type = packetIDMap.get(packetID);

        try {
            Constructor<? extends PayloadPacket> constructor = receiver == null ?
                    type.getConstructor(UUID.class, String[].class) :
                    type.getConstructor(UUID.class, UUID.class, String[].class);

            PayloadPacket packet = receiver == null ? constructor.newInstance(sender, data) : constructor.newInstance(sender, receiver, data);

            packet.setPacketUID(packetUID);

            if (packetUID == -1L) {
                handler.onPacketFailure(packet);
                return;
            }

            MethodHandle methodHandle = handlerMethods.get(innerPacket2Id.get(type));

            if (methodHandle == null)
                return;

            methodHandle.invoke(handler, packet);
        } catch (NoSuchMethodException exc) {
            Bukkit.getLogger().info("Malformed packet: %s! Requires constructor!".formatted(type.getName()));
            exc.printStackTrace();
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            Bukkit.getLogger().info("Failed to create packet!");
            e.printStackTrace();
        } catch (Throwable throwable) {
            Bukkit.getLogger().info("Method failed to invoke!");
            throwable.printStackTrace();
        }
    }

    private void loadMethodHandles(ChannelInboundHandler handler) {
        for (Method method : handler.getClass().getDeclaredMethods()) {
            if (method.isBridge() || method.isSynthetic() || method.isVarArgs())
                continue;

            if ((method.getModifiers() & Modifier.STATIC) != 0)
                continue;

            if (method.getParameterCount() != 1)
                continue;

            Class<?> parameterType = method.getParameterTypes()[0];
            if (!PayloadPacket.class.isAssignableFrom(parameterType))
                continue;

            if (!method.isAnnotationPresent(PacketMethod.class))
                continue;

            try {
                method.setAccessible(true);

                this.handlerMethods.put(innerPacket2Id.get(parameterType), MethodHandles.lookup().unreflect(method));
            } catch (IllegalAccessException exc) {
                Bukkit.getLogger().severe("Error while loading channel!");
                exc.printStackTrace();
            }
        }
    }
}
