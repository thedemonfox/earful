package com.transcendcode.earful;

public enum PlayState {
    STOPPED(1),
    PLAYING(2),
    PAUSED(3),
    NOBOOK(4);
    private final int id;

    PlayState(int id)
    {
        this.id = id;
    }

    public int getValue()
    {
        return id;
    }
}