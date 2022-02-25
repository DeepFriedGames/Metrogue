package com.deepfried.game;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.deepfried.system.TileSystem;

public class Room extends Rectangle {
    public static final int TPS = 16; //16 tiles per sector
    public static final int PPT = 16; //16 pixels per tile
    public final Array<Entity> entities = new Array<>();
    public final Array<RoomConnection> connections = new Array<>();
    public TileSystem tileSystem;
    public boolean visited = false;

    /*TODO rooms need a PURPOSE (or DESTINATION?) which has a node position inside the room.
        like an obstacle, an item, a boss, an elevator, another RoomConnection
        when the room is generated the RoomConnection should lead to its PURPOSE
     */

    public Room(Rectangle rectangle) {
        super(rectangle);
    }

    public Room(float x, float y, float w, float h) {
        super(x, y, w, h);
    }

    public float getTileX() {
        return x * TPS;
    }

    public float getTileY() {
        return y * TPS;
    }

    public void addTo(Engine engine) {
        for(Entity e : entities)
            engine.addEntity(e);
        engine.addSystem(tileSystem);
    }

    public void removeFrom(Engine engine) {
        for(Entity e : entities)
            engine.removeEntity(e);
        engine.removeSystem(tileSystem);
    }
}
