/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database.redis;

import io.lettuce.core.RedisURI;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;

@Data
public class PayloadRedis {

    private final String address;
    private final int port;
    private final boolean auth;
    private final String password;
    private final boolean ssl;
    private final String uri;
    private final int database;

    public static PayloadRedis fromConfig(ConfigurationSection section) {
        String address = section.getString("address");
        System.out.println(section.getCurrentPath() + ", " + section.getString("address") + ", " + address);
        int port = section.getInt("port");

        ConfigurationSection authSection = section.getConfigurationSection("auth");
        boolean auth = authSection.getBoolean("enabled", false);
        String password = authSection.getString("password");
        boolean ssl = authSection.getBoolean("ssl", false);

        int database = section.getInt("database", 0);

        String uri = section.getString("uri", null); // Default uri to null
        // The connection URI, if provided, will completely overwrite all other properties.

        if (uri != null) {
            if (uri.equalsIgnoreCase("null") || uri.equalsIgnoreCase("") || uri.length() < 1) {
                uri = null;
            }
        }

        return new PayloadRedis(address, port, auth, password, ssl, uri, database);
    }

    public RedisURI getRedisURI() {
        if (useURI()) {
            return RedisURI.create(this.uri);
        } else {
            System.out.println(address + ":" + port);
            RedisURI.Builder builder = RedisURI.builder()
                    .withHost(address)
                    .withPort(port)
                    .withSsl(ssl)
                    .withDatabase(database);
            if (auth) {
                builder.withPassword(password);
            }
            System.out.println(builder.build().toURI().toString());
            return builder.build();
        }
    }

    public boolean useURI() {
        return this.uri != null && uri.length() > 0 && !uri.equalsIgnoreCase("null");
    }

}
