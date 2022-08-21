package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.deepfried.system.ControllerSystem;

public class PlayerComponent implements Component {
    public static final int RESTING = 0, WEAPON_OUT = 1, AIM_UP = 2, AIM_DOWN = 3, AIM_FREE = 4;
    public static final float OUT = 0, RIGHT = 1, LEFT = -1;

    public ControllerSystem.MovementState current = ControllerSystem.MovementState.FALLING, next;
    public int aim;
    public float direction;
    public float dash;
    public float stateChangeProgress;
}
