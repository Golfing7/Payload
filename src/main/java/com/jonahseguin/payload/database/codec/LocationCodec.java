package com.jonahseguin.payload.database.codec;

import dev.morphia.aggregation.codecs.ExpressionHelper;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationCodec implements Codec<Location> {
    @Override
    public Location decode(BsonReader bsonReader, DecoderContext decoderContext) {
        bsonReader.readStartDocument();
        double x = bsonReader.readDouble("x");
        double y = bsonReader.readDouble("y");
        double z = bsonReader.readDouble("z");
        String world = bsonReader.readString("world");
        float yaw = (float) bsonReader.readDouble("yaw");
        float pitch = (float) bsonReader.readDouble("pitch");
        bsonReader.readEndDocument();
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    @Override
    public void encode(BsonWriter bsonWriter, Location location, EncoderContext encoderContext) {
        ExpressionHelper.document(bsonWriter, () -> {
            bsonWriter.writeDouble("x", location.getX());
            bsonWriter.writeDouble("y", location.getY());
            bsonWriter.writeDouble("z", location.getZ());
            bsonWriter.writeName("world");
            if (location.getWorld() != null) {
                bsonWriter.writeString(location.getWorld().getName());
            } else {
                bsonWriter.writeNull();
            }
            bsonWriter.writeDouble("yaw", location.getYaw());
            bsonWriter.writeDouble("pitch", location.getPitch());
        });
    }

    @Override
    public Class<Location> getEncoderClass() {
        return Location.class;
    }
}
