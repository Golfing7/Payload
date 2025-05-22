package com.jonahseguin.payload.helper;

import dev.morphia.annotations.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Stores a bukkit location in a manner that makes world lookups possible.
 */
@Entity @Data @AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PayloadLocation {
    private String world;
    private double x, y, z;
    private float yaw, pitch;

    public World getWorld() {
        return world != null ? Bukkit.getWorld(world) : null;
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public static PayloadLocation wrap(Location location) {
        return new PayloadLocation(location.getWorld() == null ? null : location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
}
