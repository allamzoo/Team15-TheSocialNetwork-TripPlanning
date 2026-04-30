package com.team15.tripplanning.destinationservice.security.handler;

import com.team15.tripplanning.destinationservice.security.AuthContext;
import com.team15.tripplanning.destinationservice.security.AuthHandler;

public class TokenExtractionHandler extends AuthHandler {

    @Override
    public boolean handle(AuthContext ctx) {
        String header = ctx.getRequest().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            ctx.getResponse().setStatus(401);
            return false;
        }
        ctx.setToken(header.substring(7));
        return next == null || next.handle(ctx);
    }
}
