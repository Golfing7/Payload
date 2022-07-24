/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CmdCacheDeleteAll implements PayloadCommand {

    private final PayloadAPI api;
    private final Map<String, String> verificationMap;

    @Inject
    public CmdCacheDeleteAll(PayloadAPI api) {
        this.api = api;

        this.verificationMap = Maps.newHashMap();
    }

    @Override
    public void execute(CmdArgs args) {
        String builtSenderID = (args.isPlayer() ? args.getPlayer().getUniqueId().toString() : "CONSOLE");

        if(args.length() == 1 && verificationMap.containsKey(builtSenderID)){
            String verificationCode = args.arg(0);

            String goodCode = verificationMap.remove(builtSenderID);

            if(!verificationCode.equals(goodCode)){
                args.msg("&cThat code doesn't match! Try again!");
                return;
            }

            long deleted = 0;

            List<String> toIgnore = api.getPlugin().getConfig().getStringList("deleteall-ignore-caches");

            for(Cache<?, ?> cache : PayloadCache.getAllCaches()){
                if(toIgnore.contains(cache.getName()))
                    continue;

                deleted += cache.getAll().size();

                cache.deleteAll();
            }

            args.msg("&aDeleted &e{0} &acaches!", deleted);
            return;
        }

        StringBuilder randomStringBuilder = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            randomStringBuilder.append((char) ('a' + ThreadLocalRandom.current().nextInt(25)));
        }

        verificationMap.put(builtSenderID, randomStringBuilder.toString());

        Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> verificationMap.remove(builtSenderID, randomStringBuilder.toString()), 200);

        args.msg("&cType &a\"/payload deleteall {0}\" &cto delete all caches.", randomStringBuilder.toString());
    }

    @Override
    public String name() {
        return "deleteall";
    }

    @Override
    public String[] aliases() {
        return new String[0];
    }

    @Override
    public String desc() {
        return "Deletes all caches.";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.ADMIN;
    }

    @Override
    public String usage() {
        return "";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public int minArgs() {
        return 0;
    }
}
