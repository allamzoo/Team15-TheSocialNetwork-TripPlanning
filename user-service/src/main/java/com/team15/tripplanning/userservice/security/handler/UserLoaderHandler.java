package com.team15.tripplanning.userservice.security.handler;

import com.team15.tripplanning.userservice.repository.UserRepository;
import com.team15.tripplanning.userservice.security.AuthContext;
import com.team15.tripplanning.userservice.security.AuthHandler;

public class UserLoaderHandler extends AuthHandler {

    private final UserRepository userRepository;

    public UserLoaderHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean handle(AuthContext ctx) {
        boolean exists = userRepository.existsByEmail(ctx.getUserEmail());
        if (!exists) {
            ctx.getResponse().setStatus(401);
            return false;
        }
        return next == null || next.handle(ctx);
    }
}
