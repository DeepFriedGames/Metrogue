package com.deepfried.screen;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.deepfried.component.ColorComponent;
import com.deepfried.game.Area;
import com.deepfried.system.DebugMapRenderSystem;
import com.deepfried.component.HitboxComponent;
import com.deepfried.component.PositionComponent;

public class MapScreen implements Screen {
    final Engine engine;
    final DebugMapRenderSystem renderSystem;

    public MapScreen(Area area) {
        this.engine = new Engine();
        this.renderSystem = new DebugMapRenderSystem();

        Array<Entity> entities = new Array<>();
        entities.addAll(getMapEntities(area));

        for(Entity e : entities)
            this.engine.addEntity(e);


        renderSystem.clearColor = Color.BLACK;
    }

    public Entity[] getMapEntities(Area area) {
        Array<Entity> entities = new Array<>();

//        if(area.pointlessRegions.size > 0) {
//            for (Room r : area.pointlessRegions.first()) {
//                if (!area.keyRooms.contains(r, true))
//                    entities.add(getEntity(r));
//            }
//        }

//        entities.add(getEntity(area.bounds).add(new ColorComponent(Color.GRAY)));

//        for (GraphPath<Connection<Room>> path : area.paths) {
//            for(Connection<Room> connection : path) {
//                if (!area.keyRooms.contains(connection.getFromNode(), true))
//                    entities.add(getEntity(connection.getFromNode()).add(new ColorComponent(Color.CYAN)));
//
//            }
//        }

       /* for(Rectangle r : area.rGraph.rectangles) {
            entities.add(getEntity(r).add(new ColorComponent(Color.WHITE)));
        }

        for(int i = 0; i < area.sGraph.getNodeCount(); i ++) {
            int x = area.sGraph.getNodeX(i),
                    y = area.sGraph.getNodeY(i);
            Entity e = new Entity();
            e.add(new PositionComponent(x - 0.1f, y - 0.1f));
            e.add(new ColorComponent(Color.BLUE));
            e.add(new HitboxComponent(0.2f, 0.2f));
            entities.add(e);
        }*/

        return entities.toArray(Entity.class);
    }

    private Entity getEntity(Rectangle rectangle) {
        //Color the Room entity for debugging
        return new Entity().add(new PositionComponent(rectangle.x, rectangle.y))
                .add(new HitboxComponent(rectangle.width, rectangle.height));
    }

    @Override
    public void show() {
        engine.addSystem(renderSystem);
    }

    @Override
    public void render(float delta) {
        engine.update(delta);
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
}
