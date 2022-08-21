package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

public class PositionComponent extends Vector2 implements Component {
    public PositionComponent(float x, float y) {
        super(x, y);
    }

}