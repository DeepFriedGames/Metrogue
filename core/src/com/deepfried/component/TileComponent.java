package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.deepfried.game.TileID;

public class TileComponent implements Component {
    public TileID id;
    public boolean flipX, flipY;

    public TileComponent(TileID id, boolean flipX, boolean flipY) {
        this.id = id;
        this.flipX = flipX;
        this.flipY = flipY;
    }
}
