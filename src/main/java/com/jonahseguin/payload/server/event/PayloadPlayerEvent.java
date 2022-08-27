package com.jonahseguin.payload.server.event;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PayloadPlayerEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final UUID uuid;
    private final boolean mustBeOnline;
    private final Player player;
    private final Document data;

    public PayloadPlayerEvent(UUID uuid, boolean mustBeOnline, Player player, Document data) {
        super(!Bukkit.isPrimaryThread());
        this.uuid = uuid;
        this.mustBeOnline = mustBeOnline;
        this.player = player;
        this.data = data;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isMustBeOnline() {
        return mustBeOnline;
    }

    public Player getPlayer() {
        return player;
    }

    public Document getData() {
        return data;
    }
}
