/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.server.event.PayloadPlayerEvent;
import com.jonahseguin.payload.server.event.PayloadServerEvent;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
@Singleton
public class PayloadServerService implements Runnable, ServerService {

    public static final long ASSUME_OFFLINE_SECONDS = 60;
    public static final long PING_FREQUENCY_SECONDS = 30;

    private final PayloadAPI api;
    private final String name;
    private final PayloadPlugin payloadPlugin;
    private final DatabaseService database;
    private final PayloadServer thisServer;
    private final ErrorService error;
    private final ConcurrentMap<String, PayloadServer> servers = new ConcurrentHashMap<>();
    private ServerPublisher publisher = null;
    private BukkitTask pingTask = null;
    private RedisPubSubReactiveCommands<String, String> reactive = null;
    private boolean running = false;

    @Inject
    public PayloadServerService(PayloadAPI api, DatabaseService database, PayloadPlugin payloadPlugin, @Database ErrorService error, @Database String name) {
        this.api = api;
        this.name = name;
        this.database = database;
        this.payloadPlugin = payloadPlugin;
        this.error = error;
        this.thisServer = new PayloadServer(payloadPlugin.getLocal().getPayloadID(), System.currentTimeMillis(), true);
        this.servers.put(this.thisServer.getName().toLowerCase(), this.thisServer);
    }

    @Override
    public boolean start() {
        try {
            this.publisher = new ServerPublisher(this);

            boolean sub = subscribe();

            this.publisher.publishJoin();
            this.pingTask = payloadPlugin.getServer().getScheduler().runTaskTimerAsynchronously(payloadPlugin, this, 0L, (PING_FREQUENCY_SECONDS * 20));
            running = true;
            return sub;
        } catch (Exception ex) {
            error.capture(ex, "Error starting Server Service for database: " + name);
            return false;
        }
    }

    private boolean subscribe() {
        try {
            StatefulRedisPubSubConnection<String, String> connection = database.getRedisPubSub();
            reactive = connection.reactive();

            List<String> eventList = Arrays.stream(ServerEvent.values())
                    .map(ServerEvent::getEvent).map(database::generatePrefixedChannelName).toList();
            reactive.subscribe(eventList.toArray(String[]::new)).subscribe();

            reactive.observeChannels()
                    .filter(pm -> !pm.getMessage().equalsIgnoreCase(database.getServerService().getThisServer().getName()))
                    .filter(pm -> eventList.stream().anyMatch(channel -> channel.equalsIgnoreCase(pm.getChannel())))
                    .doOnNext(patternMessage -> {
                        //Don't do any events when
                        if(!PayloadPlugin.getPlugin().isEnabled())
                            return;

                        ServerEvent event = ServerEvent.fromChannel(patternMessage.getChannel().substring(patternMessage.getChannel().indexOf("#") + 1)); // Remove DB num
                        if (event != null) {
                            if (event.equals(ServerEvent.JOIN)) {
                                handleJoin(patternMessage.getMessage());
                            } else if (event.equals(ServerEvent.QUIT)) {
                                handleQuit(patternMessage.getMessage());
                            } else if (event.equals(ServerEvent.PING)) {
                                handlePing(patternMessage.getMessage());
                            } else if (event.equals(ServerEvent.UPDATE_NAME)) {
                                Document data = Document.parse(patternMessage.getMessage());
                                String oldName = data.getString("old");
                                String newName = data.getString("new");
                                handleUpdateName(oldName, newName);
                            } else if (event.equals(ServerEvent.PLAYER_EVENT)) {
                                Document data = Document.parse(patternMessage.getMessage());
                                handlePlayerEvent(data);
                            } else if (event.equals(ServerEvent.SERVER_EVENT)) {
                                Document data = Document.parse(patternMessage.getMessage());
                                handleServerEvent(data);
                            }else if(event.equals(ServerEvent.PING_REPLY)) {
                                Document data = Document.parse(patternMessage.getMessage());
                                String serverDest = data.getString("server");
                                String sender = data.getString("sender");

                                if(!serverDest.equals(this.getThisServer().getName())) {
                                    return;
                                }

                                handlePingReply(sender);
                            }
                        }
                    }).subscribe();

            return true;
        } catch (Exception ex) {
            database.getErrorService().capture(ex, "Error subscribing in Payload Server Service");
            return false;
        }
    }

    private Pair<Boolean, UUID> isUUID(String rawUUID) {
        try {
            UUID uuid = UUID.fromString(rawUUID);
            return Pair.of(true, uuid);
        } catch (Exception ex) {
            return Pair.of(false, null);
        }
    }

    private void handlePlayerEvent(Document data) {
        UUID uuid = data.get("uuid", UUID.class);
        boolean mustBeOnline = data.getBoolean("mustBeOnline", true);
        Player player = Bukkit.getPlayer(uuid);
        if (mustBeOnline && player == null) {
            return;
        }

        data.remove("uuid");
        data.remove("mustBeOnline");

        PayloadPlayerEvent payloadPlayerEvent = new PayloadPlayerEvent(uuid, mustBeOnline, player, data);
        Bukkit.getPluginManager().callEvent(payloadPlayerEvent);
    }

    private void handleServerEvent(Document data) {
        String destinationServer = data.containsKey("destination-server") ? data.getString("destination-server") : null;
        if(destinationServer != null && !this.getThisServer().getName().equals(destinationServer))
            return;

        data.remove("destination-server");

        PayloadServerEvent serverEvent = new PayloadServerEvent(destinationServer, data);
        Bukkit.getPluginManager().callEvent(serverEvent);
    }

    @Override
    @Nonnull
    public PayloadServer register(@Nonnull String name, boolean online) {
        Preconditions.checkNotNull(name);
        PayloadServer server = new PayloadServer(name, System.currentTimeMillis(), online);
        this.servers.put(name.toLowerCase(), server);
        return server;
    }

    @Override
    public boolean has(@Nonnull String name) {
        Preconditions.checkNotNull(name);
        return this.servers.containsKey(name.toLowerCase());
    }

    @Override
    public Optional<PayloadServer> get(@Nonnull String name) {
        Preconditions.checkNotNull(name);
        return Optional.ofNullable(this.servers.get(name.toLowerCase()));
    }

    void handlePing(@Nonnull String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setLastPing(System.currentTimeMillis());
        } else {
            this.handleJoin(serverName);
        }

        //Reply to the sender letting them know we're online
        this.publisher.publishPingReply(serverName);
    }

    /**
     * Used as a way for another server to reply to this one. It will be sent as a 'reply' from a ping packet.
     *
     * @param serverName the name of the server that we initially pinged
     */
    void handlePingReply(@Nonnull String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setLastPing(System.currentTimeMillis());
        } else {
            this.handleJoin(serverName);
        }
    }

    void handleJoin(@Nonnull String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            PayloadServer payloadServer = this.servers.get(serverName.toLowerCase());
            payloadServer.setOnline(true);
            //We must also update the ping time as the server may have just come BACK online.
            payloadServer.setLastPing(System.currentTimeMillis());
        } else {
            this.register(serverName, true);
        }
    }

    void handleQuit(String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setOnline(false);
        }
    }

    void handleUpdateName(String oldName, String serverName) {
        if (this.servers.containsKey(oldName.toLowerCase())) {
            PayloadServer server = this.servers.get(oldName.toLowerCase());
            server.setName(serverName);
            this.servers.remove(oldName.toLowerCase());
            this.servers.put(serverName.toLowerCase(), server);
        }
    }

    private void doPing() {
        this.publisher.publishPing();
    }

    @Override
    public boolean shutdown() {
        try {
            if (this.pingTask != null) {
                this.pingTask.cancel();
            }
            if (reactive != null) {
                List<String> eventList = Arrays.stream(ServerEvent.values())
                        .map(ServerEvent::getEvent).toList();
                reactive.unsubscribe(eventList.stream().map(database::generatePrefixedChannelName).toArray(String[]::new));
            }

            this.publisher.publishQuit(); // Sync.
            this.publisher = null;
            running = false;
            return true;
        } catch (Exception ex) {
            error.capture("Error shutting down Server Service for database: " + name);
            return false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public Collection<PayloadServer> getServers() {
        return servers.values();
    }

    @Override
    public void run() {
        this.doPing();
        this.thisServer.setLastPing(System.currentTimeMillis());
        this.thisServer.setOnline(true);
        this.servers.forEach((name, server) -> {
            if (!name.equalsIgnoreCase(this.thisServer.getName())) {
                if (server.isOnline()) {
                    long pingExpiredAt = System.currentTimeMillis() - (PayloadServerService.ASSUME_OFFLINE_SECONDS * 1000);
                    if (server.getLastPing() <= pingExpiredAt) {
                        // Assume they're offline
                        server.setOnline(false);

                        Bukkit.getLogger().info("PAYLOAD -- Server %s timed out with pings!".formatted(server.getName()));
                    }
                }
            }
        });
    }
}
