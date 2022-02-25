package com.deepfried.game;

public enum TileID {
    FULL(0, 0, 0, 1f),
    HALF_FULL(0, 0, 0, 1f),
    PLATFORM(0, 0, 0, 1f),
    SLOPE(8, 8, 1, 0.5f),
    HALF_SLOPE(8, 4, 0.5f, 0.75f);

    TileID(float x0, float y0, float m, float scl) {
        this.x0 = x0;
        this.y0 = y0;
        this.m = m;
        this.scl = scl;
    }

    public float x0, y0, m, scl;
}
