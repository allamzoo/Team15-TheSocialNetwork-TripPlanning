package com.team15.tripplanning.bookingservice.security.handler;

import com.team15.tripplanning.bookingservice.security.AuthContext;
import com.team15.tripplanning.bookingservice.security.AuthHandler;
import com.team15.tripplanning.bookingservice.security.JwtService;

public class SignatureValidationHandler extends AuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean handle(AuthContext ctx) {
        if (!jwtService.isTokenValid(ctx.getToken())) {
            ctx.getResponse().setStatus(401);
            return false;
        }
        ctx.setUserEmail(jwtService.extractEmail(ctx.getToken()));
        ctx.setUserId(jwtService.extractUserId(ctx.getToken()));
        ctx.setRole(jwtService.extractRole(ctx.getToken()));
        return next == null || next.handle(ctx);
    }
}
