package com.team15.tripplanning.bookingservice.security.handler;

import com.team15.tripplanning.bookingservice.security.AuthContext;
import com.team15.tripplanning.bookingservice.security.AuthHandler;

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
