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

public class ItemStackCodec implements Codec<ItemStack> {
    @Override
    public ItemStack decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return ItemStack.deserializeBytes(bsonReader.readBinaryData().getData());
    }

    @Override
    public void encode(BsonWriter bsonWriter, ItemStack itemStack, EncoderContext encoderContext) {
        bsonWriter.writeBinaryData(new BsonBinary(itemStack.serializeAsBytes()));
    }

    @Override
    public Class<ItemStack> getEncoderClass() {
        return ItemStack.class;
    }
}
