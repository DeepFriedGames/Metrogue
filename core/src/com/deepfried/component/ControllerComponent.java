package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Input;
import com.deepfried.game.MovementType;

public class ControllerComponent implements Component {
    public int up = Input.Keys.UP;
    public int down = Input.Keys.DOWN;
    public int left = Input.Keys.LEFT;
    public int right = Input.Keys.RIGHT;
    public int jump = Input.Keys.X;
    public int run = Input.Keys.Z;
    public int shoot = Input.Keys.S;
    public int deselect = Input.Keys.A;
    public int shoulderR = Input.Keys.C;
    public int shoulderL = Input.Keys.D;
    public int pause = Input.Keys.ENTER;
    public int select = Input.Keys.SHIFT_LEFT;

    public MovementType movement = MovementType.WALKING;
    public boolean horizontal_positive = true, low = false;
    public float dash, momentum;
}
