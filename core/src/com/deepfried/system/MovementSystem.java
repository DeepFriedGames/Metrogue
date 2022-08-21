package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.VelocityComponent;

public class MovementSystem  extends EntitySystem {
    private static final Vector2 VECTOR = new Vector2();
    private ImmutableArray<Entity> entities;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(PositionComponent.class, VelocityComponent.class).get());
    }

    public void update(float deltaTime) {
        for (Entity entity : entities) {
            PositionComponent position = positions.get(entity);
            Vector2 v = VECTOR.set(velocities.get(entity));

            double x = position.x + v.x;
            double y = position.y + v.y;
            position.set((float) x, (float) y);
        }
    }
}
