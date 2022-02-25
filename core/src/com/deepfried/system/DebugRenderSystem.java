package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.deepfried.component.CameraFollowComponent;
import com.deepfried.component.ColorComponent;
import com.deepfried.component.DebugShapeComponent;
import com.deepfried.component.HitboxComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.game.Room;
import com.deepfried.screen.DebugScreen;

public class DebugRenderSystem extends EntitySystem {
    private static final Vector2 VECTOR = new Vector2();
    ShapeRenderer shapeRenderer;
    public OrthographicCamera camera;
    public FitViewport viewport;
    private ImmutableArray<Entity> entities;
    public Color clearColor = Color.SKY;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<HitboxComponent> hitboxes = ComponentMapper.getFor(HitboxComponent.class);
    private final ComponentMapper<DebugShapeComponent> shapes = ComponentMapper.getFor(DebugShapeComponent.class);
    private final ComponentMapper<ColorComponent> colors = ComponentMapper.getFor(ColorComponent.class);
    private final ComponentMapper<CameraFollowComponent> follow = ComponentMapper.getFor(CameraFollowComponent.class);

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(PositionComponent.class).one(HitboxComponent.class, DebugShapeComponent.class).get());
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(256, 192, camera);
    }

    @Override
    public void update(float remainderTime) {
        ScreenUtils.clear(clearColor);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.begin();
        for(Entity entity: entities) {
            PositionComponent p = positions.get(entity);
            float x = p.x == p.previous.x ? p.x : MathUtils.lerp(p.x, p.previous.x, remainderTime);
            float y = p.y == p.previous.y ? p.y : MathUtils.lerp(p.y, p.previous.y, remainderTime);
            Rectangle rect = Rectangle.tmp.set(hitboxes.get(entity)).setPosition(x, y);
            if(follow.has(entity)) {
                Vector2 center = rect.getCenter(VECTOR);

                //constrain target position to edges of sectors
                Room room = ((DebugScreen) ((Game) Gdx.app.getApplicationListener()).getScreen()).room;
                float w = camera.viewportWidth * camera.zoom / 2;
                float h = camera.viewportHeight * camera.zoom / 2;
                camera.position.x = Math.max(room.x * 256 + w, Math.min(center.x, (room.x + room.width) * 256 - w));
                camera.position.y = Math.max(room.y * 256 + h, Math.min(rect.y + 16, (room.y + room.height) * 256 - h));

            }

            if (colors.has(entity)) shapeRenderer.setColor(colors.get(entity).color);
            else shapeRenderer.setColor(Color.WHITE);
            if(shapes.has(entity)) {
                float[] verts = shapes.get(entity).vertices.clone();
                for(int i = 0; i < verts.length; i++)
                    verts[i] += (i % 2 == 0) ? x : y;

                shapeRenderer.polygon(verts);
            } else if(hitboxes.has(entity)) {
                shapeRenderer.rect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                shapeRenderer.setColor(Color.RED);
                shapeRenderer.point(x, y, 0);
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