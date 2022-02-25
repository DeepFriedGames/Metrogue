package com.deepfried.screen;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.deepfried.component.CameraFollowComponent;
import com.deepfried.game.Area;
import com.deepfried.system.CollisionSystem;
import com.deepfried.component.ColorComponent;
import com.deepfried.component.ControllerComponent;
import com.deepfried.system.ControllerSystem;
import com.deepfried.system.DebugRenderSystem;
import com.deepfried.component.GravityComponent;
import com.deepfried.system.GravitySystem;
import com.deepfried.component.HitboxComponent;
import com.deepfried.system.MovementSystem;
import com.deepfried.component.PositionComponent;
import com.deepfried.game.Room;
import com.deepfried.component.VelocityComponent;
import com.deepfried.game.World;

public class DebugScreen implements Screen {
    Engine engine;
    DebugRenderSystem renderSystem;
    public Room room;
    Entity player;

    final float dt = 1/60f;
    double accumulator = 0;
    public World world;
    public Area area;

    public DebugScreen(World world) {
        this.world = world;
        this.area = this.world.areas.first();
//        this.room = area.paths.first().get(0).getFromNode();
    }

    @Override
    public void show() {
        engine = new Engine();
        renderSystem = new DebugRenderSystem();
        player = new Entity()
                .add(new PositionComponent((room.getTileX() + 8) * 16, (room.getTileY() + 8) * 16))
                .add(new ColorComponent(Color.ORANGE))
                .add(new HitboxComponent(10, 42))
                .add(new VelocityComponent())
                .add(new GravityComponent())
                .add(new ControllerComponent())
                .add(new CameraFollowComponent());
        engine.addEntity(player);
        engine.addSystem(new GravitySystem());
        engine.addSystem(new ControllerSystem());
        engine.addSystem(new CollisionSystem());
        engine.addSystem(new MovementSystem());
        room.addTo(engine);
        engine.addSystem(renderSystem);

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

        engine.getSystem(DebugRenderSystem.class).update(1 - alpha);
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

    }

    @Override
    public void dispose() {

    }

    public void setRoom(Room to) {
        room.removeFrom(engine);
        this.room = to;
        room.addTo(engine);
    }
}
