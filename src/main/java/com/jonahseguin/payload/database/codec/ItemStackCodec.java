package com.jonahseguin.payload.database.codec;

import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("rawtypes")
public class ItemStackCodec implements Codec {
    private final Class<?> clazz;

    public ItemStackCodec(boolean craft) {
        try {
            this.clazz = craft ? ItemStack.class : Class.forName(Bukkit.getServer().getClass().getPackageName() + ".inventory.CraftItemStack");
        } catch (ClassNotFoundException exc) {
            throw new RuntimeException("Could not find ItemStack class", exc);
        }
    }

    @Override
    public ItemStack decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return ItemStack.deserializeBytes(bsonReader.readBinaryData().getData());
    }

    @Override
    public void encode(BsonWriter bsonWriter, Object itemStack, EncoderContext encoderContext) {
        bsonWriter.writeBinaryData(new BsonBinary(((ItemStack) itemStack).serializeAsBytes()));
    }

    @Override
    public Class getEncoderClass() {
        return clazz;
    }
}
