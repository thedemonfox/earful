
package com.transcendcode.earful;

public enum SleepState
{
    OFF(1),
    WAITING(2),
    HUSHING(3),
    SLEEP(4);
    private final int id;

    SleepState(int id)
    {
        this.id = id;
    }

    public int getValue()
    {
        return id;
    }

    public static boolean isOn(SleepState num)
    {
        if (num == SleepState.OFF)
            return false;

        return true;
    }
}
