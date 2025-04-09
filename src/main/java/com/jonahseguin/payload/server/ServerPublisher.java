/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import com.google.common.base.Preconditions;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.UUID;

public class ServerPublisher {

    private final PayloadServerService payloadServerService;

    public ServerPublisher(PayloadServerService serverService) {
        this.payloadServerService = serverService;
    }

    public void publishPlayerEvent(UUID playerUUID, boolean mustBeOnline, Document data) {
        data.put("uuid", playerUUID);
        data.put("mustBeOnline", mustBeOnline);
        payloadServerService.getDatabase().getErrorService().debug("Payload Server Service: Publishing Player Event to " + playerUUID.toString());
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                payloadServerService.getDatabase().getRedis().async().publish(payloadServerService.getDatabase().generatePrefixedChannelName(ServerEvent.PLAYER_EVENT.getEvent()), data.toJson());
            } catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing PLAYER_EVENT event");
            }
        });

    }

    public void publishServerEvent(String destinationServer, Document data) {
        if(destinationServer != null)
            data.put("destination-server", destinationServer);

        payloadServerService.getDatabase().getErrorService().debug("Payload Server Service: Publishing Server Event to " + destinationServer);
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                payloadServerService.getDatabase().getRedis().async().publish(payloadServerService.getDatabase().generatePrefixedChannelName(ServerEvent.SERVER_EVENT.getEvent()), data.toJson());
            }catch(Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing SERVER_EVENT event");
            }
        });
    }

    /**
     * Used as a reply to a ping packet.
     */
    public void publishPingReply(String destination) {
        //Putting it into a document is necessary as the server destination will ignore it if we do not.
        Document pingReply = new Document();
        pingReply.put("server", destination);
        pingReply.put("sender", payloadServerService.getThisServer().getName());

        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try{
                payloadServerService.getDatabase().getRedis().async().publish(payloadServerService.getDatabase().generatePrefixedChannelName(ServerEvent.PING_REPLY.getEvent()), pingReply.toJson());
            }catch(Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing PING_REPLY event");
            }
        });
    }

    public void publishPing() {
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                payloadServerService.getDatabase().getRedis().async().publish(payloadServerService.getDatabase().generatePrefixedChannelName(ServerEvent.PING.getEvent()), payloadServerService.getThisServer().getName());
            } catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing PING event");
            }
        });
    }

    public void publishJoin() {
        Preconditions.checkNotNull(payloadServerService, "Server service");
        Preconditions.checkNotNull(payloadServerService.getDatabase(), "Database");
        Preconditions.checkNotNull(payloadServerService.getDatabase().getRedis(), "Redis");
        Preconditions.checkNotNull(payloadServerService.getDatabase().getRedis().async(), "Redis async");
        Preconditions.checkNotNull(ServerEvent.JOIN.getEvent(), "Join event");
        Preconditions.checkNotNull(payloadServerService.getThisServer(), "thisServer");
        Preconditions.checkNotNull(payloadServerService.getThisServer().getName(), "thisServer#name");
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                payloadServerService.getDatabase().getRedis().async().publish(payloadServerService.getDatabase().generatePrefixedChannelName(ServerEvent.JOIN.getEvent()), payloadServerService.getThisServer().getName());
            } catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing JOIN event");
            }
        });
    }

    public void publishQuit() {
        //We must run this sync.
        payloadServerService.getDatabase().getRedis().async().publish(payloadServerService.getDatabase().generatePrefixedChannelName(ServerEvent.QUIT.getEvent()), payloadServerService.getThisServer().getName());
    }

    public void publishUpdateName(String oldName, String newName) {
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                Document data = new Document();
                data.append("old", oldName);
                data.append("new", newName);
                payloadServerService.getDatabase().getRedis().async().publish(payloadServerService.getDatabase().generatePrefixedChannelName(ServerEvent.UPDATE_NAME.getEvent()), data.toJson());
            } catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing UPDATE_NAME event");
            }
        });
    }

}
