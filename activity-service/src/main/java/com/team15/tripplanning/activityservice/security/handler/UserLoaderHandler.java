package com.team15.tripplanning.activityservice.security.handler;

import com.team15.tripplanning.activityservice.security.AuthContext;
import com.team15.tripplanning.activityservice.security.AuthHandler;

public class UserLoaderHandler extends AuthHandler {

    @Override
    public boolean handle(AuthContext ctx) {
        if (ctx.getUserEmail() == null || ctx.getUserEmail().isBlank()) {
            ctx.getResponse().setStatus(401);
            return false;
        }
        return next == null || next.handle(ctx);
    }
}
