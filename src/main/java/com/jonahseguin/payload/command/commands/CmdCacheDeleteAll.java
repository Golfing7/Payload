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

            for(Cache<?, ?> cache : PayloadCache.getAllCaches()){
                deleted += cache.getAll().size();

                cache.deleteAll();
            }

            args.msg("&aDeleted &e{0} &accaches!", deleted);
        }

        StringBuilder randomStringBuilder = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            randomStringBuilder.append('a' + ThreadLocalRandom.current().nextInt(25));
        }

        verificationMap.put(builtSenderID, randomStringBuilder.toString());

        Bukkit.getScheduler().runTaskLater(api.getPlugin(), () -> verificationMap.remove(builtSenderID, randomStringBuilder.toString()), 100);

        args.msg("&cType &a\"/payload deletecache {0}\" &cto delete all caches.", randomStringBuilder.toString());
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
