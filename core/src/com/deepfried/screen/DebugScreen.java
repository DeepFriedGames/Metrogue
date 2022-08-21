package com.deepfried.screen;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.deepfried.system.CollisionSystem;
import com.deepfried.system.ControllerSystem;
import com.deepfried.system.DebugRenderSystem;
import com.deepfried.system.GravitySystem;
import com.deepfried.system.MovementSystem;

public class DebugScreen implements Screen {
    private final Engine engine = new Engine();
    private final OrthogonalTiledMapRenderer renderer;
    private final OrthographicCamera camera = new OrthographicCamera(256, 192);
//    public Room room;

    final float dt = 1/60f;
    double accumulator = 0;

//    public DebugScreen(World world) {
//        this.area = this.world.areas.first();
//        this.room = area.rooms.get(0);  //TODO generate rooms
//        this.renderSystem = new DebugRenderSystem();
//        this.room.addTo(engine);
//        this.engine.addSystem(renderSystem);
//    }

    public DebugScreen(TiledMap map) {
        this.renderer = new OrthogonalTiledMapRenderer(map);


    }

    @Override
    public void show() {
        engine.addSystem(new GravitySystem());
        engine.addSystem(new ControllerSystem());
        engine.addSystem(new CollisionSystem());
        engine.addSystem(new MovementSystem());

    }

    @Override
    public void render(float delta) {
        accumulator += delta;

        while(accumulator >= dt) {
            for(EntitySystem system : engine.getSystems()) {
                if(system.getClass() != DebugRenderSystem.class)
                    system.update(dt);
            }
            accumulator -= dt;
        }
        //do the render
        float alpha = (float) (accumulator / dt);

        camera.update();
        renderer.setView(camera);
        renderer.render();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }

//    public void setRoom(Room to) {
//        room.removeFrom(engine);
//        this.room = to;
//        room.addTo(engine);
//    }
}
