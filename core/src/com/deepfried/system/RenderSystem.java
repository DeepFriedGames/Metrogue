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
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.deepfried.component.CameraFollowComponent;
import com.deepfried.component.ColorComponent;
import com.deepfried.component.DebugShapeComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.ShapeComponent;

public class RenderSystem extends EntitySystem {
    private static final Vector2 VECTOR = new Vector2();
    private static final float MAX_SPEED = 1/4f;
    private final OrthogonalTiledMapRenderer mapRenderer;
    ShapeRenderer shapeRenderer;
    public OrthographicCamera camera;
    public FitViewport viewport;
    private ImmutableArray<Entity> entities;
    public Color clearColor = Color.BLACK;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<ShapeComponent> shapes = ComponentMapper.getFor(ShapeComponent.class);
    private final ComponentMapper<ColorComponent> colors = ComponentMapper.getFor(ColorComponent.class);
    private final ComponentMapper<CameraFollowComponent> follow = ComponentMapper.getFor(CameraFollowComponent.class);

    public RenderSystem(OrthogonalTiledMapRenderer renderer) {
        this.mapRenderer = renderer;
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(PositionComponent.class).one(ShapeComponent.class, DebugShapeComponent.class).get());
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(256, 144, camera);
    }

    @Override
    public void update(float remainderTime) {
        ScreenUtils.clear(clearColor);
        camera.update();

        mapRenderer.setView(camera);
        mapRenderer.render();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.begin();
        for(Entity entity: entities) {
            if(follow.has(entity)) {
                Rectangle rect = Rectangle.tmp.set(shapes.get(entity).getRectangle()).setPosition(positions.get(entity));
                Vector2 point = VECTOR.set(rect.x + follow.get(entity).x,
                        rect.y + rect.height - follow.get(entity).y);

                //constrain target position to edges of sectors
                MapProperties properties = mapRenderer.getMap().getProperties();
                int mapWidth = properties.get("width", Integer.class); //in tiles
                int mapHeight = properties.get("height", Integer.class); //in tiles
                float cameraHalfWidth = camera.viewportWidth * camera.zoom / 2;
                float cameraHalfHeight = camera.viewportHeight * camera.zoom / 2;
                TiledMapTileLayer tileLayer = mapRenderer.getMap().getLayers().getByType(TiledMapTileLayer.class).first();
                int tileWidth = tileLayer.getTileWidth(); //pixels per tile
                int tileHeight = tileLayer.getTileHeight();
                float newX = Math.max(cameraHalfWidth, Math.min(point.x, mapWidth * tileWidth - cameraHalfWidth));
                float newY = Math.max(cameraHalfHeight, Math.min(point.y, mapHeight * tileHeight - cameraHalfHeight));
                camera.position.x = MathUtils.lerp(camera.position.x, newX, MAX_SPEED);
                camera.position.y = MathUtils.lerp(camera.position.y, newY, MAX_SPEED);

            }

            if (colors.has(entity)) shapeRenderer.setColor(colors.get(entity).color);
            else shapeRenderer.setColor(Color.WHITE);
            if(shapes.has(entity)) {
                if(shapes.get(entity).getRectangle() != null) {
                    Rectangle box = Rectangle.tmp.set(shapes.get(entity).getRectangle()).setPosition(positions.get(entity));
                    shapeRenderer.rect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
                }
                if(shapes.get(entity).getPolyline() != null) {
                    float[] vertices = shapes.get(entity).getPolyline().getTransformedVertices().clone();

                    shapeRenderer.polyline(vertices);
                }
            }
            if(positions.has(entity)) {
                PositionComponent position = positions.get(entity);
                shapeRenderer.setColor(Color.RED);
                shapeRenderer.point(position.x, position.y, 0);
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