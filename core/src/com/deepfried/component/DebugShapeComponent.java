package com.deepfried.component;

import com.badlogic.ashley.core.Component;

public class DebugShapeComponent implements Component {
    public float[] vertices;

    public DebugShapeComponent(float... vertices) {
        this.vertices = vertices;
    }
}
