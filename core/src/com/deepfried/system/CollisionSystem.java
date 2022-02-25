package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.deepfried.component.ControllerComponent;
import com.deepfried.component.DoorComponent;
import com.deepfried.component.HitboxComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.game.RoomConnection;
import com.deepfried.screen.DebugScreen;

public class CollisionSystem extends EntitySystem {
    ImmutableArray<Entity> entities;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<HitboxComponent> hitboxes = ComponentMapper.getFor(HitboxComponent.class);
    private final ComponentMapper<DoorComponent> doors = ComponentMapper.getFor(DoorComponent.class);
    private final ComponentMapper<ControllerComponent> controllers = ComponentMapper.getFor(ControllerComponent.class);

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(PositionComponent.class, HitboxComponent.class)
                .one(DoorComponent.class, ControllerComponent.class).get());

    }

    @Override
    public void update(float deltaTime) {
        //for every entity, only certain components care about collisions
        for(int e = 0; e < entities.size(); e ++) {
            Entity entity = entities.get(e);
            for(int o = e + 1; o < entities.size(); o ++) {
                Entity other = entities.get(o);

                if(overlap(entity, other)) {
                    collide(entity, other);
                    collide(other, entity);
                }
            }
        }
    }

    private void collide(Entity entity, Entity other) {
        if(controllers.has(entity) && doors.has(other)) {
            PositionComponent p1 = positions.get(entity);
            HitboxComponent h1 = hitboxes.get(entity);
            DoorComponent door = doors.get(other);
            PositionComponent p2 = positions.get(other);
            HitboxComponent h2 = hitboxes.get(other);

            //set the current room
            ((DebugScreen) ((Game) Gdx.app.getApplicationListener()).getScreen()).setRoom(door.connection.to);
            //move the entity
            switch (door.connection.direction) {
                case RoomConnection.LEFT:
                    p1.x = p2.x - h1.getWidth() - 17;
                    break;
                case RoomConnection.DOWN:
                    p1.y = p2.y - h1.getHeight() - 17;
                    break;
                case RoomConnection.UP:
                    p1.y = p2.y + h2.getHeight() + 17;
                    break;
                case RoomConnection.RIGHT:
                    p1.x = p2.x + h2.getWidth() + 17;
                    break;

            }
        }
    }

    private boolean overlap(Entity entityA, Entity entityB) {
        PositionComponent positionA = positions.get(entityA);
        Rectangle boxA = Rectangle.tmp.set(hitboxes.get(entityA)).setPosition(positionA);
        PositionComponent positionB = positions.get(entityB);
        Rectangle boxB = Rectangle.tmp2.set(hitboxes.get(entityB)).setPosition(positionB);
        return boxA.overlaps(boxB);
    }
}
