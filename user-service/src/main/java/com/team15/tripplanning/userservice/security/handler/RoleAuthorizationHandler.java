package com.team15.tripplanning.userservice.security.handler;

import com.team15.tripplanning.userservice.security.AuthContext;
import com.team15.tripplanning.userservice.security.AuthHandler;

public class RoleAuthorizationHandler extends AuthHandler {

    private final String requiredRole;

    public RoleAuthorizationHandler(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public boolean handle(AuthContext ctx) {
        if (!ctx.getRole().equals(requiredRole)) {
            ctx.getResponse().setStatus(403);
            return false;
        }
        return next == null || next.handle(ctx);
    }
}
