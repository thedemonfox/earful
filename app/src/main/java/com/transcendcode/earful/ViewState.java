
package com.transcendcode.earful;

public enum ViewState {
    LIBRARY(1),
    PLAYBACK(2);
    private final int id;

    ViewState(int id)
    {
        this.id = id;
    }

    public int getValue()
    {
        return id;
    }
}
