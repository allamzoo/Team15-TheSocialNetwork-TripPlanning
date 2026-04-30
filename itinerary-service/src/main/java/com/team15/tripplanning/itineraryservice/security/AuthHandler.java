package com.team15.tripplanning.itineraryservice.security;

public abstract class AuthHandler {

    protected AuthHandler next;

    public void setNext(AuthHandler next) {
        this.next = next;
    }

    public abstract boolean handle(AuthContext ctx);
}
