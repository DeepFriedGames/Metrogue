package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class GravityComponent implements Component {
    //accelerations in tiles per frame per second
    public Vector2 terminal = new Vector2(0, -5.01325f);
    public Vector2 acceleration = new Vector2(0, -0.109375f);
}
