package com.team15.tripplanning.shared.auth;

public abstract class AuthHandler {

    protected AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    public abstract boolean handle(AuthContext ctx);
}
