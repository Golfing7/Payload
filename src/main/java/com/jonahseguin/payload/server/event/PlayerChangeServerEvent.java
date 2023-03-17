package com.jonahseguin.payload.server.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is switching servers on the same network.
 */
public class PlayerChangeServerEvent extends PlayerQuitEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PlayerChangeServerEvent(@NotNull PlayerQuitEvent event) {
        super(event.getPlayer(), event.quitMessage(), event.getReason());
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
