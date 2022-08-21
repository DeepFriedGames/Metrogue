package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.deepfried.component.ColorComponent;
import com.deepfried.component.DebugShapeComponent;
import com.deepfried.component.ShapeComponent;
import com.deepfried.component.PositionComponent;

public class DebugMapRenderSystem extends EntitySystem {
    ShapeRenderer shapeRenderer;
    public OrthographicCamera camera;
    public FitViewport viewport;
    private ImmutableArray<Entity> entities;
    public Color clearColor = Color.SKY;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<ShapeComponent> shapes = ComponentMapper.getFor(ShapeComponent.class);
    private final ComponentMapper<DebugShapeComponent> debugShapes = ComponentMapper.getFor(DebugShapeComponent.class);
    private final ComponentMapper<ColorComponent> colors = ComponentMapper.getFor(ColorComponent.class);

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(PositionComponent.class).one(ShapeComponent.class, DebugShapeComponent.class).get());
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(60, 60, camera);
        camera.position.set(29, 29, 0);
    }

    @Override
    public void update(float deltaTime) {
        ScreenUtils.clear(clearColor);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.begin();
        for(Entity entity: entities) {
            PositionComponent p = positions.get(entity);

            if (colors.has(entity))
                shapeRenderer.setColor(colors.get(entity).color);
            else
                shapeRenderer.setColor(Color.DARK_GRAY);
            if(debugShapes.has(entity)) {
                float[] verts = debugShapes.get(entity).vertices.clone();
                for(int i = 0; i < verts.length; i++)
                    verts[i] += (i % 2 == 0) ? p.x : p.y;

                shapeRenderer.polygon(verts);
            } else if(shapes.has(entity)) {
                Rectangle rect = Rectangle.tmp.set(shapes.get(entity).getRectangle()).setPosition(p);
                shapeRenderer.rect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
            }
        }
        shapeRenderer.end();
    }

    @Override
    public void removedFromEngine(Engine engine) {
        shapeRenderer.dispose();
    }

    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}