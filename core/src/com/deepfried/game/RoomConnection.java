package com.deepfried.game;

import com.badlogic.gdx.ai.pfa.Connection;

public class RoomConnection implements Connection<Room> {
    public static final short LEFT = 0b00, RIGHT = 0b01, DOWN = 0b10, UP = 0b11;
    public final Room from;
    public final Room to;
    public float cost;
    public float x, y;
    public short direction = 0;
    boolean active = false;

    public RoomConnection(Room from, Room to) {
        this.from = from;
        this.to = to;
        if(from.y == to.y + to.height || to.y == from.y + from.height)
            direction += 0b10;
        if(to.y == from.y + from.height || to.x == from.x + from.width)
            direction += 0b01;

        if(direction == UP || direction == DOWN)
            cost *= 3;

        cost = from.area();
    }

    @Override
    public float getCost() {
        return cost;
    }

    @Override
    public Room getFromNode() {
        return from;
    }

    @Override
    public Room getToNode() {
        return to;
    }

}
