package com.deepfried.game;

public enum MovementType {
    WALKING(0.1875f, -0.5f, 2.75f, 42, 30),
    MORPH(0.75f, -3.25f, 3.25f, 14, 14),
    JUMP(0.1875f, -0.5f, 1.25f, 38, 23),
    SPIN_JUMP(0.1875f, 0, 1.375f, 26, 26),
    LONG_JUMP(0.375f, 0, 3.25f, 24, 24),
    FALLING(0.1875f, -0.5f, 1.375f, 38, 23),
    TURNAROUND(0.375f, -0.375f, 0.375f, 42, 30),
    SLIDE(0, -0.0625f, 3.25f, 14, 14);

    public final float acceleration;
    public final float max_momentum;
    public final float deceleration;
    public final float height;
    public final float low_height;

    MovementType(float acceleration, float deceleration, float max_momentum, float height, float low_height) {
        this.acceleration = acceleration;
        this.max_momentum = max_momentum;
        this.deceleration = deceleration;
        this.height = height;
        this.low_height = low_height;
    }
}
