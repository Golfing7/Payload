/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.handshake;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ProfileHandshakePacket {

    private static final String KEY_SENDER_SERVER = "senderServer";
    private static final String KEY_TARGET_SERVER = "targetServer";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_LOGIN = "login";

    private String senderServer;
    private final UUID uuid;
    private String targetServer;
    private boolean playerLogin;

    @Nullable
    public static ProfileHandshakePacket fromJSON(@Nonnull String json) {
        Preconditions.checkNotNull(json, "JSON cannot be null for ProfileHandshakePacket");
        Document document = Document.parse(json);
        String uuidString = document.getString(KEY_UUID);
        String targetServer = document.getString(KEY_TARGET_SERVER);
        String senderServer = document.getString(KEY_SENDER_SERVER);
        boolean login = document.getBoolean(KEY_LOGIN);

        if (uuidString != null && targetServer != null && senderServer != null) {
            return new ProfileHandshakePacket(senderServer, UUID.fromString(uuidString), targetServer, login);
        }
        return null;
    }

    public Document toDocument() {
        Document document = new Document();
        document.append(KEY_TARGET_SERVER, targetServer);
        document.append(KEY_UUID, uuid.toString());
        document.append(KEY_SENDER_SERVER, senderServer);
        document.append(KEY_LOGIN, playerLogin);
        return document;
    }

}
