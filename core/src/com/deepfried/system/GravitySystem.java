package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.deepfried.component.GravityComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.VelocityComponent;
import com.deepfried.game.TileID;

public class GravitySystem extends EntitySystem {
    public static final Vector2 VECTOR = new Vector2();
    ImmutableArray<Entity> entities;
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<GravityComponent> gravities = ComponentMapper.getFor(GravityComponent.class);
    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);

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
            VelocityComponent d = velocities.get(entity);
            GravityComponent g = gravities.get(entity);
            PositionComponent p = positions.get(entity);
            Vector2 a = VECTOR.set(g.acceleration);

            d.add(a);

            Vector2 terminal = VECTOR.set(g.terminal);
            if ((0 < terminal.y && terminal.y < d.y) ||
                    (d.y < terminal.y && terminal.y < 0))
                d.y = terminal.y;

            if ((0 < terminal.x && terminal.x < d.x) ||
                    (d.x < terminal.x && terminal.x < 0))
                d.x = terminal.x;

        }
    }
}
