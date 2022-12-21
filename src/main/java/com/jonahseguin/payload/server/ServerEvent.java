/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

public enum ServerEvent {

    JOIN("payload-server-join"),
    PING("payload-server-ping"),
    PING_REPLY("payload-server-ping-reply"),
    QUIT("payload-server-quit"),
    UPDATE_NAME("payload-server-update-name"),
    SERVER_EVENT("payload-server-event"),
    PLAYER_EVENT("payload-player-event");

    private final String event;

    ServerEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public static ServerEvent fromChannel(String msg) {
        for (ServerEvent e : values()) {
            if (e.event.equalsIgnoreCase(msg)) {
                return e;
            }
        }
        return null;
    }

}
