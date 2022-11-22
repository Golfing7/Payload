package com.jonahseguin.payload.server.event;

import lombok.Getter;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PayloadServerEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    @Getter
    private final String destinationServer;
    private final Document data;

    public PayloadServerEvent(String destinationServer, Document data) {
        super(!Bukkit.isPrimaryThread());
        this.destinationServer = destinationServer;
        this.data = data;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Document getData() {
        return data;
    }
}
