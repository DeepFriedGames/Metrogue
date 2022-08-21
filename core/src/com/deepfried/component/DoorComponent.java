package com.deepfried.component;

import com.badlogic.ashley.core.Component;

public class DoorComponent implements Component {
    private final String toMapName;
    public float outX, outY;

    public DoorComponent(String toMapName) {
        this.toMapName = toMapName;
    }

    public String getToMapName() {
        return toMapName;
    }
}
