package com.deepfried.component;

import com.badlogic.ashley.core.Component;

public class CollisionComponent implements Component {
    public static final byte NONE = 0b00000000, TILE = 0b00000001, SOLID = 0b00000010, SEMISOLID = 0b00000100,
            ENEMY = 0b00001000, ENEMY_PROJECTILE = 0b00010000, PLAYER = 0b00100000,
            PLAYER_PROJECTILE = 0b01000000, OTHER = 0b1000000;
    public byte mask, flags, ignore = NONE; //mask contains the bits that this component will collide with and flags contains the bits that this component triggers in others, stored in Tiled as strings

    public CollisionComponent(String mask, String flags) {
        this.mask = Byte.parseByte(mask, 2);
        this.flags = Byte.parseByte(flags, 2);
    }
}
