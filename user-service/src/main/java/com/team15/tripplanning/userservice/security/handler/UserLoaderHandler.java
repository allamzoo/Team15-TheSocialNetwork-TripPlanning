package com.team15.tripplanning.userservice.security.handler;

import com.team15.tripplanning.userservice.model.Status;
import com.team15.tripplanning.userservice.model.User;
import com.team15.tripplanning.userservice.repository.UserRepository;
import com.team15.tripplanning.userservice.security.AuthContext;
import com.team15.tripplanning.userservice.security.AuthHandler;
import java.util.Optional;


public class UserLoaderHandler extends AuthHandler {

    private final UserRepository userRepository;

    public UserLoaderHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean handle(AuthContext ctx) {
        Optional<User> userOpt = userRepository.findByEmail(ctx.getUserEmail());
        if (userOpt.isEmpty()) {
            ctx.getResponse().setStatus(401);
            return false;
        }
        User user = userOpt.get();
        if (user.getStatus() != Status.ACTIVE) {
            ctx.getResponse().setStatus(401);
            return false;
        }
        return next == null || next.handle(ctx);
    }
}
