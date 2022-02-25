package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.deepfried.component.GravityComponent;
import com.deepfried.component.HitboxComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.TileComponent;
import com.deepfried.component.VelocityComponent;
import com.deepfried.game.Room;
import com.deepfried.game.TileID;
import com.sun.tools.javac.util.Pair;

import static com.deepfried.game.Room.PPT;
import static com.deepfried.game.Room.TPS;

public class TileSystem extends EntitySystem {
    private static final Vector2 VECTOR = new Vector2(), VECTOR_2 = new Vector2();
    private static final float MARGIN = 1/256f;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<HitboxComponent> hitboxes = ComponentMapper.getFor(HitboxComponent.class);
    private final ComponentMapper<TileComponent> tiles = ComponentMapper.getFor(TileComponent.class);
    private final ComponentMapper<GravityComponent> gravities = ComponentMapper.getFor(GravityComponent.class);

    private ImmutableArray<Entity> entities;
    public final Entity[][] tileMap;
    public float x, y;

    public TileSystem(Room room) {
        this.tileMap = new Entity[MathUtils.round(room.width * TPS)][Math.round(room.height * TPS)];
        this.x = room.x * TPS;
        this.y = room.y * TPS;
    }

    public Entity getTile(int x, int y) {
        if(0 <= x && x < tileMap.length && 0 <= y && y < tileMap[x].length)
            return tileMap[x][y];
        else
            return null;
    }

    public Entity getTile(float x, float y) {
        int tx = MathUtils.floor(x / PPT); //tile's x index
        int ty = MathUtils.floor(y / PPT); //tile's y index
        tx -= this.x;
        ty -= this.y;

        return getTile(tx, ty);
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(PositionComponent.class, VelocityComponent.class, HitboxComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        for (Entity entity : entities) {
            PositionComponent p = positions.get(entity);
            VelocityComponent v = velocities.get(entity);
            Rectangle box = Rectangle.tmp.set(hitboxes.get(entity)).setPosition(p);
            Vector2 center = box.getCenter(VECTOR_2);
            GravityComponent g = null;
            if(gravities.has(entity)) g = gravities.get(entity);

            boolean inSlope = false;
            Entity tile = getTile(center.x, p.y);
            if(tile != null) {
                p.tile = tiles.get(tile);
                inSlope = p.tile.id == TileID.SLOPE || p.tile.id == TileID.HALF_SLOPE;
            }

            float newX = -1;
            float x0 = p.previous.x;
            float x1 = p.x;
            if(x0 < x1) {
                x0 += box.getWidth();
                x1 += box.getWidth();
            }

            p.grounded = false;
            //do horizontal collision checks all the way up the height of the entity's hitbox
            //round up the bottom and the height to the nearest tile
            float h = box.getHeight();
            float ceil = MathUtils.ceil(h / PPT);
            for(int i = inSlope ? 1 : 0; i <= ceil; i++) {
                float y = p.previous.y + Math.min(h, i * PPT);
                float tx = horizontalCheck(x0, x1, y);
                if(Math.abs(x0 - tx) < Math.abs(x0 - newX))
                    newX = tx;

                if(g != null && g.acceleration.x != 0 &&
                        Math.copySign(1, g.acceleration.x) == Math.copySign(1, v.x) &&
                        isSolid(p.x + g.acceleration.x, y))
                    p.grounded = true;
            }

            if (newX >= 0) {
                p.x = newX - (x0 < x1 ? box.getWidth() : 0);
                v.x = 0;
            }

            float newY = -1;
            float y0 = p.previous.y;
            float y1 = p.y;
            if(y0 < y1) {
                y0 += box.getHeight();
                y1 += box.getHeight();
            }

            float w = box.getWidth();
            ceil = MathUtils.ceil(w / PPT);
            for(int i = 0; i <= ceil; i++) {
                float x = inSlope ? center.x : p.x + Math.min(w, i * PPT);
                Pair<TileID, Float> collision = verticalChecks(x, center.x, y0, y1);
                float ty = collision.snd;
                if(collision.fst == TileID.SLOPE || collision.fst == TileID.HALF_SLOPE) {
                    newY = ty;
                } else if(Math.abs(y0 - ty) < Math.abs(y0 - newY))
                    newY = ty;

                if(g != null && g.acceleration.y != 0 &&
                        Math.copySign(1, g.acceleration.y) == Math.copySign(1, v.y) &&
                        isSolid(x, p.y + Math.copySign(2, g.acceleration.y)))
                    p.grounded = true;
            }

            if (newY >= 0) {
                p.y = newY - (y0 < y1 ? box.getHeight() : 0);
                v.y = 0;
            }
        }
    }

    private Pair<TileID, Float> verticalChecks(float x, float centerX, float y0, float y1) {
        float ty = -1;

        for(float y : new float[]{y0, y1}) {
            Entity test = getTile(x, y);
            if (test != null) {
                TileComponent tile = tiles.get(test);
                Rectangle box = Rectangle.tmp2.set(hitboxes.get(test).setPosition(positions.get(test)));
                float top = box.getY() + box.getHeight();
                switch (tile.id) {
                    case FULL:
                        if (y < box.getCenter(VECTOR).y) ty = box.getY() - MARGIN;
                        else ty = top + MARGIN;
                        break;
                    case HALF_FULL:
                        if(box.y + box.height / 2 <= y && y <= top)
                            ty = top + MARGIN;
                        if(box.y <= y && y < box.y + box.height / 2)
                            ty = box.getY() - MARGIN;
                        break;
                    case PLATFORM:
                        if(y1 <= top && top <= y0)
                            ty = top + MARGIN;
                        break;
                    case HALF_SLOPE:
                    case SLOPE:
                        float xs = box.getX() + tile.id.x0,
                                ys = box.getY() + tile.id.y0,
                                m = (tile.flipX ^ tile.flipY ? -1 : 1) * tile.id.m;

                        if(box.getX() <= centerX && centerX < box.getX() + box.getWidth()) {
                            ty = m * (centerX - xs) + ys;
                            if(y1 > ty + 5 && !tile.flipY)
                                ty = -1;
                            if(y1 < ty && tile.flipY)
                                ty = -1;
                        }
                        break;
                }
                if(ty >= 0)
                    return new Pair<>(tile.id, ty);
            }
        }
        return new Pair<>(null, ty);
    }

    private float horizontalCheck(float x0, float x1, float y) {
        float tx = -1;

        Entity test = getTileExcludeX(x1, y);
        if (test != null) {
            TileComponent tile = tiles.get(test);
            Rectangle box = Rectangle.tmp2.set(hitboxes.get(test).setPosition(positions.get(test)));
            switch (tile.id) {
                case SLOPE:
                case HALF_SLOPE:
                    if(box.contains(x1, y)) {
                        if(tile.flipX) {
                            if (x0 <= box.getX())
                                tx = box.getX() - MARGIN;
                        } else {
                            if (x0 >= box.getX() + box.getWidth())
                                tx = box.getX() + box.getWidth();
                        }
                    }
                    break;
                case HALF_FULL:
                    if(box.contains(x1, y)) {
                        if (x0 <= box.getX())
                            tx = box.getX() - MARGIN;
                        if (x0 >= box.getX() + box.getWidth())
                            tx = box.getX() + box.getWidth();
                    }
                    break;
                case FULL:
                    if (x0 >= box.getX() + box.getWidth())
                        tx = box.getX() + box.getWidth();
                    if (x0 <= box.getX())
                        tx = box.getX() - MARGIN;
                    break;
            }
            if(tx >= 0) return tx;

        }
        return tx;
    }

    private Entity getTileExcludeX(float x, float y) {
        Entity tile = getTile(x, y);

        if(tile != null) {
            Rectangle box = Rectangle.tmp2.set(hitboxes.get(tile).setPosition(positions.get(tile)));
            if (x > box.x && x < box.x + box.width && y >= box.y && y <= box.y + box.height)
                return tile;
        }
        return null;
    }

    public boolean isSolid(float x, float y) {
        Entity entity = getTile(x, y);
        if(entity == null) return false;

        TileComponent tile = tiles.get(entity);
        HitboxComponent box = hitboxes.get(entity);
        switch (tile.id) {
            case FULL:
                return true;
            case HALF_FULL:
            case PLATFORM:
                return box.contains(x, y);
            case HALF_SLOPE:
            case SLOPE:
                float xs = box.getX() + tile.id.x0,
                        ys = box.getY() + tile.id.y0,
                        m = (tile.flipX ^ tile.flipY ? -1 : 1) * tile.id.m;

                float boundY = m * (x - xs) + ys;
                return tile.flipY ^ y <= boundY;
        }
        return false;
    }
}
