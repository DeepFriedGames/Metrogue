package com.deepfried.screen;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.deepfried.component.AnimationComponent;
import com.deepfried.component.CameraFollowComponent;
import com.deepfried.component.CollisionComponent;
import com.deepfried.component.ColorComponent;
import com.deepfried.component.ControllerComponent;
import com.deepfried.component.DoorComponent;
import com.deepfried.component.GravityComponent;
import com.deepfried.component.JumpThroughComponent;
import com.deepfried.component.OneWayComponent;
import com.deepfried.component.PlayerComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.ShapeComponent;
import com.deepfried.component.VelocityComponent;
import com.deepfried.game.GdxGame;
import com.deepfried.system.CollisionSystem;
import com.deepfried.system.ControllerSystem;
import com.deepfried.system.GravitySystem;
import com.deepfried.system.MovementSystem;
import com.deepfried.system.RenderSystem;
import com.deepfried.system.TileSystem;

public class GameScreen implements Screen {
    private final EntitySystem gravitySystem = new GravitySystem(),
            controllerSystem = new ControllerSystem(),
            collisionSystem = new CollisionSystem(),
            movementSystem = new MovementSystem();
    private final RenderSystem renderSystem;
    private final TileSystem tileSystem;
    private final Array<Entity> entities = new Array<>();

    public GameScreen(TiledMap map) {
        this.tileSystem = new TileSystem(map);
        this.renderSystem = new RenderSystem(new OrthogonalTiledMapRenderer(map));
        convertObjectsToEntities(map.getLayers().get("Entity Layer").getObjects());
    }

    @Override
    public void show() {
        Engine engine = ((GdxGame) Gdx.app.getApplicationListener()).getEngine();
        engine.addSystem(gravitySystem);
        engine.addSystem(controllerSystem);
        engine.addSystem(tileSystem);
        engine.addSystem(collisionSystem);
        engine.addSystem(movementSystem);
        engine.addSystem(renderSystem);
        for(Entity entity : entities)
            engine.addEntity(entity);
    }

    @Override
    public void render(float delta) {
    }

    @Override
    public void resize(int width, int height) {
        renderSystem.resize(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        Engine engine = gravitySystem.getEngine();
        engine.removeAllEntities();
        engine.removeSystem(gravitySystem);
        engine.removeSystem(controllerSystem);
        engine.removeSystem(tileSystem);
        engine.removeSystem(movementSystem);
        engine.removeSystem(collisionSystem);
        engine.removeSystem(renderSystem);

    }

    @Override
    public void dispose() {
    }

    public void convertObjectsToEntities(MapObjects mapObjects) {
        for (MapObject object : mapObjects) {
            MapProperties properties = object.getProperties();

            Entity entity = new Entity();
            if(properties.containsKey("x") && properties.containsKey("y")) {
                float x = properties.get("x", Float.class);
                float y = properties.get("y", Float.class);
                entity.add(new PositionComponent(x, y));
            }
            if(object.getClass() == RectangleMapObject.class) {
                float width = properties.get("width", Float.class);
                float height = properties.get("height", Float.class);
                entity.add(new ShapeComponent(new Rectangle(0, 0, width, height)));
            }
            if(object.getClass() == PolylineMapObject.class) {
                PolylineMapObject polylineObject = (PolylineMapObject) object;
                Polyline polyline = new Polyline(polylineObject.getPolyline().getVertices());
                polyline.setPosition(properties.get("x", Float.class), properties.get("y", Float.class));
                entity.add(new ShapeComponent(polyline));
            }
            if (properties.containsKey("CameraFollow")) {
                CameraFollowComponent cameraFollow = new CameraFollowComponent();
                String string = properties.get("CameraFollow", String.class);
                String[] split = string.split(",");
                float followX = Float.parseFloat(split[0]);
                float followY = Float.parseFloat(split[1]);
                cameraFollow.set(followX, followY);
                entity.add(cameraFollow);
            }
            if (properties.containsKey("Color"))
                entity.add(new ColorComponent(properties.get("Color", Color.class)));
            if (properties.containsKey("Controller")) {
                ControllerComponent controllerComponent = new ControllerComponent();
                Controller current = Controllers.getCurrent();
                if(current != null) {
                    controllerComponent.controller = current;
                    controllerComponent.up = current.getMapping().buttonDpadUp;
                    controllerComponent.down = current.getMapping().buttonDpadDown;
                    controllerComponent.left = current.getMapping().buttonDpadLeft;
                    controllerComponent.right = current.getMapping().buttonDpadRight;
                    controllerComponent.jump = current.getMapping().buttonB;
                    controllerComponent.attack = current.getMapping().buttonY;
                    controllerComponent.slide = current.getMapping().buttonX;
                    controllerComponent.run = current.getMapping().buttonA;
                    controllerComponent.aimLock = current.getMapping().buttonL2;
                    controllerComponent.pause = current.getMapping().buttonStart;
                    controllerComponent.select = current.getMapping().buttonBack;
                }
                entity.add(controllerComponent);
            }
            if (properties.containsKey("Gravity"))
                entity.add(new GravityComponent());
            if (properties.containsKey("Velocity"))
                entity.add(new VelocityComponent());
            if (properties.containsKey("Animation")) {
                String fileName = properties.get("Animation", String.class);
                TextureAtlas atlas = ((GdxGame) Gdx.app.getApplicationListener()).assetManager.get(fileName, TextureAtlas.class);
                AnimationComponent animation = new AnimationComponent(atlas);
            }
            if(properties.containsKey("Collision")) {
                String[] bytes = properties.get("Collision", String.class).split(",");
                entity.add(new CollisionComponent(bytes[0], bytes[1]));
            }
            switch (object.getName()) {
                case "Player":
                    ((GdxGame) Gdx.app.getApplicationListener()).addComponentsTo(entity);
                    entity.add(new PlayerComponent());
                    break;
                case "Door":
                    DoorComponent doorComponent = new DoorComponent(properties.get("toMap", String.class));
                    doorComponent.outX = properties.get("outX", Float.class);
                    doorComponent.outY = properties.get("outY", Float.class);
                    entity.add(doorComponent);
                    break;
                case "One Way":
                    OneWayComponent oneWay = new OneWayComponent();
                    oneWay.greater = properties.get("Greater", Boolean.class);
                    entity.add(oneWay);
                    break;
                case "Jump Through":
                    JumpThroughComponent jumpThrough = new JumpThroughComponent();
                    entity.add(jumpThrough);
                    break;
            }
            entities.add(entity);

        }
    }
}
