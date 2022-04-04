/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

//Currently unused.
public class CmdCacheDelete implements PayloadCommand {

    private final PayloadAPI api;

    @Inject
    public CmdCacheDelete(PayloadAPI api) {
        this.api = api;
    }

    @Override
    public void execute(CmdArgs args) {
        String cacheName = args.joinArgs();
        Cache cache = api.getCache(cacheName);
        if (cache == null) {
            args.msg("&cA cache with the name '{0}' does not exist.  Type /payload caches for a list of caches.", cacheName);
            return;
        }


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
