/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CmdCacheDelete implements PayloadCommand {

    private final PayloadAPI api;
    private final Map<String, String> verificationMap;

    @Inject
    public CmdCacheDelete(PayloadAPI api) {
        this.api = api;

        this.verificationMap = Maps.newHashMap();
    }

    @Override
    public void execute(CmdArgs args) {
        String cacheName = args.getArgs()[0];
        Cache cache = api.getCache(cacheName);
        if (cache == null) {
            args.msg("&cA cache with the name '{0}' does not exist.  Type /payload caches for a list of caches.", cacheName);
            return;
        }

        String builtSenderID = (args.isPlayer() ? args.getPlayer().getUniqueId().toString() : "CONSOLE") + "_" + cache.getName();

        if(args.length() == 2 && verificationMap.containsKey(builtSenderID)){
            String verificationCode = args.arg(0);

            String goodCode = verificationMap.remove(builtSenderID);

            if(!verificationCode.equals(goodCode)){
                args.msg("&cThat code doesn't match! Try again!");
                return;
            }

            long deleted = cache.getAll().size();

            cache.deleteAll();

            args.msg("&aDeleted &e{0} &accaches from &e{1} &astore!", deleted, cache.getName());
        }

        StringBuilder randomStringBuilder = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            randomStringBuilder.append((char) ('a' + ThreadLocalRandom.current().nextInt(25)));
        }

        verificationMap.put(builtSenderID, randomStringBuilder.toString());

        Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> verificationMap.remove(builtSenderID, randomStringBuilder.toString()), 100);

        args.msg("&cType &a\"/payload deletecache {0} {1}\" &cto delete the cache.", cache.getName(), randomStringBuilder.toString());
    }

    @Override
    public String name() {
        return "deletecache";
    }

    @Override
    public String[] aliases() {
        return new String[]{"dc", "dcache"};
    }

    @Override
    public String desc() {
        return "Deletes all cached things in a cache.";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.ADMIN;
    }

    @Override
    public String usage() {
        return "<cache name>";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public int minArgs() {
        return 1;
    }
}
