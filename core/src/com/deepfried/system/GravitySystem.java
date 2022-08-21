package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.deepfried.component.GravityComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.ShapeComponent;
import com.deepfried.component.VelocityComponent;

public class GravitySystem extends EntitySystem {
    //accelerations in tiles per frame per second
    public static final Vector2 TERMINAL = new Vector2(0, -85/16f);
    public static final Vector2 ACCELERATION = new Vector2(0, -34/256f);
    public static final Vector2 VECTOR = new Vector2();
    private static final float GROUND_CHECK_MARGIN = 45/16f;
    ImmutableArray<Entity> entities;
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<GravityComponent> gravities = ComponentMapper.getFor(GravityComponent.class);
    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<ShapeComponent> shapes = ComponentMapper.getFor(ShapeComponent.class);

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(GravityComponent.class, VelocityComponent.class).get());

    }

    @Override
    public void removedFromEngine(Engine engine) {

    }

    @Override
    public void update(float deltaTime) {
        for(Entity entity : entities) {
            VelocityComponent velocity = velocities.get(entity);
            GravityComponent gravity = gravities.get(entity);
            Vector2 acceleration = VECTOR.set(ACCELERATION);

            velocity.add(acceleration);

			//halts acceleration once TERMINAL velocity is achieved
            if ((0 < TERMINAL.y && TERMINAL.y < velocity.y) ||
                    (velocity.y < TERMINAL.y && TERMINAL.y < 0))
                velocity.y = TERMINAL.y;

            if ((0 < TERMINAL.x && TERMINAL.x < velocity.x) ||
                    (velocity.x < TERMINAL.x && TERMINAL.x < 0))
                velocity.x = TERMINAL.x;

            //check if the entity is grounded
            gravity.grounded = false;
            TileSystem tileSystem = getEngine().getSystem(TileSystem.class);
            CollisionSystem collisionSystem = getEngine().getSystem(CollisionSystem.class);
            PositionComponent position = positions.get(entity);
            Rectangle box = shapes.get(entity).getRectangle();
            if(Math.abs(ACCELERATION.y) > 0) {
                float y = ACCELERATION.y > 0 ? position.y + box.getHeight() : position.y;
                float margin = Math.copySign(GROUND_CHECK_MARGIN, ACCELERATION.y);
                float[] sensorsX = tileSystem.getSensors(position.x, position.x + box.getWidth(), tileSystem.tileLayer.getTileWidth());
                for(float sensorX : sensorsX) {
                    if(collisionSystem.isPathObstructed(sensorX, y, sensorX, y + velocity.y)) {
                        gravity.grounded = true;
                        break;
                    }
                    if(tileSystem.isSolid(sensorX, y + margin)) {
                        gravity.grounded = true;
                        break;
                    }
                }
            }
        }
    }
}
