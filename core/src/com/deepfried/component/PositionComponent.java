package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class PositionComponent extends Vector2 implements Component {
    public boolean grounded;
    public Vector2 previous;
    public TileComponent tile;

    public PositionComponent() {
        super();
        this.previous = new Vector2();
    }

    public PositionComponent(Vector2 v) {
        super(v);
        this.previous = new Vector2(v);
    }

    public PositionComponent(float x, float y) {
        super(x, y);
        this.previous = new Vector2(x, y);
    }
}