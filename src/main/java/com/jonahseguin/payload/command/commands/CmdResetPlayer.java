package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;

public class CmdResetPlayer implements PayloadCommand {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void execute(CmdArgs args) {
        int cachesReset = 0;
        String playerName = args.arg(0);
        for (Cache<?, ?> cache : PayloadCache.getAllCaches()) {
            if (!(cache instanceof ProfileCache<?>)) {
                continue;
            }

            ProfileCache profileCache = (ProfileCache) cache;
            var profile = profileCache.get(playerName);
            if (profile.isEmpty())
                continue;

            cachesReset++;
            profileCache.delete(profile.get());
        }

        args.msg("&aReset &e{0} &acache objects for player &e{1}&a.", cachesReset, playerName);
    }

    @Override
    public String name() {
        return "resetprofile";
    }

    @Override
    public String[] aliases() {
        return new String[0];
    }

    @Override
    public String desc() {
        return "Resets all profile data linked with the username";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.ADMIN;
    }

    @Override
    public String usage() {
        return "<username>";
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
