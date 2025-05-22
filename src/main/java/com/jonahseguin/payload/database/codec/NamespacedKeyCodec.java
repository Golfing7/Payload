package com.jonahseguin.payload.database.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bukkit.NamespacedKey;

/**
 * A codec for Bukkit {@link NamespacedKey}.
 */
public class NamespacedKeyCodec implements Codec<NamespacedKey> {
    @Override
    public NamespacedKey decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return NamespacedKey.fromString(bsonReader.readString());
    }

    @Override
    public void encode(BsonWriter bsonWriter, NamespacedKey namespacedKey, EncoderContext encoderContext) {
        bsonWriter.writeString(namespacedKey.asString());
    }

    @Override
    public Class<NamespacedKey> getEncoderClass() {
        return NamespacedKey.class;
    }
}
