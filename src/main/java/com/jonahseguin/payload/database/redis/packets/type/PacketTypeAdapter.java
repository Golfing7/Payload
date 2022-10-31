package com.jonahseguin.payload.database.redis.packets.type;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public enum PacketTypeAdapter {
    STRING((str) -> str, String.class),
    BYTE(Byte::parseByte, Byte.class, byte.class),
    SHORT(Short::parseShort, Byte.class, byte.class),
    INT(Integer::parseInt, Integer.class, int.class),
    LONG(Long::parseLong, Long.class, long.class),
    CHAR((str) -> str.charAt(0), Character.class, char.class),
    BOOLEAN(Boolean::parseBoolean, Boolean.class, boolean.class),
    FLOAT(Float::parseFloat, Float.class, float.class),
    DOUBLE(Double::parseDouble, Double.class, double.class),
    UUID(java.util.UUID::fromString, java.util.UUID.class),
    ITEM((str) -> {
        try {
            return CraftItemStack.asBukkitCopy(ItemStack.a(MojangsonParser.parse(str)));
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        throw new PacketLoadException("Couldn't parse %s as item!".formatted(str));
    }, (item) -> {
        org.bukkit.inventory.ItemStack stack = (org.bukkit.inventory.ItemStack) item;

        return CraftItemStack.asNMSCopy(stack).save(new NBTTagCompound()).asString();
    }, org.bukkit.inventory.ItemStack.class, CraftItemStack.class),
    SINGLE_DEPTH_LIST((str) -> {
        String[] split = str.split("\0");

        List list = Lists.newArrayList();

        if(split[0].length() <= 1)
            return list;

        for(String first : split){
            String[] sub = first.split("\n");
            try {
                Class<?> clazz = Class.forName(sub[0]);

                String value = sub[1];

                PacketTypeAdapter adapter = getFromClass(clazz);

                Object obj = adapter.transform(value);

                list.add(obj);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return list;
    }, (obj) -> {
        StringBuilder builder = new StringBuilder();

        List list = (List) obj;
        for(int i = 0; i < list.size(); i++){
            Object object = list.get(i);

            PacketTypeAdapter adapter = getFromClass(object.getClass());

            builder.append(object.getClass().getName());
            builder.append('\n');
            builder.append(adapter.toPacketString(object));
            if(i + 1 != list.size())
                builder.append('\0');
        }
        return builder.toString();
    }, List.class, ArrayList.class),
    ;

    public static PacketTypeAdapter getFromClass(Class<?> clazz){
        for (PacketTypeAdapter adapter : values()){
            if(adapter.typeEquals(clazz))
                return adapter;
        }
        return null;
    }

    private Class<?>[] typeClasses;
    private Function<String, ?> transformer;
    private Function<Object, String> toString;

    PacketTypeAdapter(Function<String, ?> transformer, Class<?>... typeClasses){
        this.transformer = transformer;
        this.toString = Object::toString;
        this.typeClasses = typeClasses;
    }

    PacketTypeAdapter(Function<String, ?> transformer, Function<Object, String> toString, Class<?>... typeClasses){
        this.transformer = transformer;
        this.toString = toString;
        this.typeClasses = typeClasses;
    }


    public String toPacketString(Object obj){
        return toString.apply(obj);
    }

    public boolean typeEquals(Class<?> clazz){
        for(Class c : typeClasses)
        {
            if(clazz == c){
                return true;
            }
        }
        return false;
    }

    public Object transform(String str){
        return transformer.apply(str);
    }
}
