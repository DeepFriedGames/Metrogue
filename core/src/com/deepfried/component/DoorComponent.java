package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.deepfried.game.RoomConnection;

public class DoorComponent implements Component {
    public RoomConnection connection;

    public DoorComponent(RoomConnection connection) {
        this.connection = connection;
    }
}
