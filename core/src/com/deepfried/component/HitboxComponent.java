package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class HitboxComponent extends Rectangle implements Component {

    public HitboxComponent(float w, float h) {
        super(0, 0, w, h);
    }

}
