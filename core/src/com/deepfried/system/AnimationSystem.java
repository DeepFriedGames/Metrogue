package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.deepfried.component.AnimationComponent;
import com.deepfried.component.ShapeComponent;
import com.deepfried.component.PositionComponent;

public class AnimationSystem extends EntitySystem {

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<AnimationComponent> animations = ComponentMapper.getFor(AnimationComponent.class);
    private final ComponentMapper<ShapeComponent> shapes = ComponentMapper.getFor(ShapeComponent.class);

    private ImmutableArray<Entity> entities;

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(PositionComponent.class, AnimationComponent.class, ShapeComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        for(Entity entity : entities) {

        }
    }
}
