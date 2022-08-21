package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.deepfried.component.GravityComponent;
import com.deepfried.component.ShapeComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.VelocityComponent;
import com.deepfried.utility.Segment;

public class TileSystem extends EntitySystem {
    private static final Vector2 VECTOR = new Vector2();
    private static final float MARGIN = 1/256f;
    private static final float[] SLOPE_SCALING = new float[]{1f, 0.85f, 0.39f};

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<ShapeComponent> shapes = ComponentMapper.getFor(ShapeComponent.class);
    private final ComponentMapper<GravityComponent> gravities = ComponentMapper.getFor(GravityComponent.class);

    private ImmutableArray<Entity> entities;
    public final TiledMap map;
    public final TiledMapTileLayer tileLayer;
    public float x, y;

    public TileSystem(TiledMap map) {
        this.map = map;
        this.tileLayer = (TiledMapTileLayer) map.getLayers().get("Collision Layer");
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(PositionComponent.class, VelocityComponent.class, ShapeComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        for(Entity entity : entities) {
            PositionComponent position = positions.get(entity);
            Rectangle rectangle = shapes.get(entity).getRectangle();
            Vector2 v = VECTOR.set(velocities.get(entity));
            GravityComponent g = gravities.get(entity);
            Rectangle box = Rectangle.tmp.set(rectangle).setPosition(position);

            float entityLeft = box.getX();
            float entityRight = entityLeft + box.getWidth();
            float entityBottom = box.getY();
            float entityTop = entityBottom + box.getHeight();
            float entityCenterX = entityLeft + box.getWidth() / 2f;
            float x = v.x > 0 ? entityRight + MARGIN : entityLeft;
            float y = v.y > 0 ? entityTop + MARGIN : entityBottom;
            float dx = v.x; //the amount to move the entity,
            float dy = v.y; //the amount to move the entity,
            float[] sensors, vertices;

            Segment bottomSlope = null, topSlope = null;
            TiledMapTileLayer.Cell bottomCell = getCell(entityCenterX, entityBottom);
            TiledMapTileLayer.Cell topCell = getCell(entityCenterX, entityTop);
            if(bottomCell != null && !isInsideUp(bottomCell)) bottomSlope = getSlope(entityCenterX, entityBottom);
            if(topCell != null && isInsideUp(topCell)) topSlope = getSlope(entityCenterX, entityTop);

            sensors = getSensors(entityBottom, entityTop, tileLayer.getTileHeight()); //x-axis sensors
            for(float sensorY : sensors) {
                if((sensorY == entityBottom && bottomSlope != null)
                        || (sensorY == entityTop && topSlope != null))
                    continue;
                Polygon polygon = getPolygon(x + dx, sensorY);
                if(polygon == null) continue;
                vertices = polygon.getTransformedVertices();
                for(int p = 0; p < vertices.length; p += 2) {
                    Segment edge = new Segment(vertices[p], vertices[p + 1],
                            vertices[(p + 2) % vertices.length], vertices[(p + 3) % vertices.length]);
                    if(edge.minY != edge.maxY && edge.minX != edge.maxX) continue; //polygon edge is a slope
                    if(edge.minY < entityTop && edge.maxY > sensorY
                            && entityRight + dx > edge.minX && entityLeft + dx < edge.maxX) {
                        if(v.x > 0) dx = Math.min(dx, edge.minX - x);
                        if(v.x < 0) dx = Math.max(dx, edge.maxX - x);
                    }
                }
            }

			if(bottomSlope != null) {
				float scale = SLOPE_SCALING[0];
				if(bottomSlope.m >= 1) scale = SLOPE_SCALING[1];
				if(bottomSlope.m >= 2) scale = SLOPE_SCALING[2];
				dx *= scale;
				float distanceToEdge = bottomSlope.getY(entityCenterX + dx) - entityBottom;
				dy = Math.max(dy, distanceToEdge);
			} else if(topSlope != null) {
                float scale = SLOPE_SCALING[0];
                if(topSlope.m >= 1) scale = SLOPE_SCALING[1];
                if(topSlope.m >= 2) scale = SLOPE_SCALING[2];
				dx *= scale;
                float distanceToEdge = topSlope.getY(entityCenterX + dx) - entityTop;
                dy = Math.min(dy, distanceToEdge);
			}

            Segment underSlope = null;
			if(dy <= 0) underSlope = getSlope(entityCenterX + dx, entityBottom + dy);

            sensors = getSensors(entityLeft + dx, entityRight + dx, tileLayer.getTileWidth()); //y-axis sensors
            if(bottomSlope != null || topSlope != null || underSlope != null)
                sensors = new float[]{entityCenterX + dx};
            for (float sensorX : sensors) {
                Polygon polygon = getPolygon(sensorX, y + dy);
                if(polygon == null) continue;
                vertices = polygon.getTransformedVertices();
                for (int p = 0; p < vertices.length; p += 2) {
                    Segment edge = new Segment(vertices[p], vertices[p + 1],
                            vertices[(p + 2) % vertices.length], vertices[(p + 3) % vertices.length]);
                    if (edge.xa == edge.xb) continue; //polygon edge parallel to y-axis
                    if (edge.ya != edge.yb) continue; //polygon edge is a slope
                    //ya == yb and the edge is perpendicular to y-axis
                    if (entityBottom + dy < edge.ya && edge.ya < entityTop + dy
                            && edge.minX < entityRight && entityLeft < edge.maxX) {
                        //the edge in question overlaps the range of where the entity is going to be
                        float distanceToEdge = edge.ya - y;
                        if (v.y > 0) dy = Math.min(dy, distanceToEdge);
                        if (v.y < 0) dy = Math.max(dy, distanceToEdge);
                    }
                }
            }



            if(underSlope != null) {
                float slopeY = underSlope.getY(entityCenterX + dx);
                float distanceToEdge = slopeY - entityBottom;
                TiledMapTileLayer.Cell cell = getCell(entityCenterX + dx, slopeY);
                if(g.grounded && cell != null && !isInsideUp(cell)) dy = distanceToEdge;
            }

            velocities.get(entity).set(dx, dy);
        }
    }
	
	public boolean isInsideUp(TiledMapTileLayer.Cell cell) {
		if(cell.getRotation() == TiledMapTileLayer.Cell.ROTATE_0) return cell.getFlipVertically();
		if(cell.getRotation() == TiledMapTileLayer.Cell.ROTATE_270) return cell.getFlipHorizontally();
		if(cell.getRotation() == TiledMapTileLayer.Cell.ROTATE_90) return !cell.getFlipHorizontally();
		return false;
	}

    Segment getSlope(float x, float y) {
        TiledMapTileLayer.Cell cell = getCell(x, y);
        if(cell == null) return null;
        Polygon polygon = getPolygon(x, y);
        if(polygon == null) return null;
        float[] vertices = polygon.getTransformedVertices();
        for (int p = 0; p < vertices.length; p += 2) {
            Segment edge = new Segment(vertices[p], vertices[p + 1],
                    vertices[(p + 2) % vertices.length], vertices[(p + 3) % vertices.length]);
            if (edge.xa != edge.xb && edge.ya != edge.yb) {
                //polygon edge is a slope
                if(edge.minX <= x && x <= edge.maxX && edge.minY <= y && y <= edge.maxY)
                    return edge;
            }
        }
        return null;
    }

    float[] getSensors(float start, float end, float interval) {
		//returns array of float values from start to end with a constant difference of "interval"
        float width = end - start;
        int size = MathUtils.ceil(width / interval) + 1;
        float[] sensors = new float[size];
        for(int s = 0; s < size; s++) {
            sensors[s] = Math.min(start + s * interval, end);
        }
        return sensors;
    }

    private Polygon getPolygon(float x, float y) {
        TiledMapTileLayer.Cell cell = getCell(x, y);
        if(cell == null) return null;
        TiledMapTile tile = cell.getTile();
        Array<PolygonMapObject> mapObjects = tile.getObjects().getByType(PolygonMapObject.class);
        if(mapObjects.size < 1) return null;
        PolygonMapObject mapObject = mapObjects.first();
        if(mapObject == null) return null;
        Polygon polygon = mapObject.getPolygon();
        if(polygon == null) return null;
        float centerX = mapObject.getProperties().get("x", Float.class); //relative center x-value of the polygon
        float centerY = mapObject.getProperties().get("y", Float.class); //relative center y-value of the polygon
        float tileX = MathUtils.floor(x / tileLayer.getTileWidth()) * tileLayer.getTileWidth(); //world x-value of the tile
        float tileY = MathUtils.floor(y / tileLayer.getTileHeight()) * tileLayer.getTileHeight(); //world y-value of the tile
        polygon.setPosition(tileX + centerX, tileY + centerY); //world position of the polygon
        float scaleX = cell.getFlipHorizontally() ? -1 : 1;
        float scaleY = cell.getFlipVertically() ? -1 : 1;
        polygon.setScale(scaleX, scaleY);
        switch(cell.getRotation()) {
            case TiledMapTileLayer.Cell.ROTATE_0:
                polygon.setRotation(0f);
                break;
            case TiledMapTileLayer.Cell.ROTATE_90:
                polygon.setRotation(90f);
                break;
            case TiledMapTileLayer.Cell.ROTATE_180:
                polygon.setRotation(180f);
                break;
            case TiledMapTileLayer.Cell.ROTATE_270:
                polygon.setRotation(270f);
                break;

        }
        return polygon;
    }

    private int getTileX(float x) {
        // returns the x index of a x value
        int tx = MathUtils.floor(x / tileLayer.getTileWidth());
        tx -= this.x;

        return tx;
    }

    private int getTileY(float y) {
        // returns the y index of a y value
        int ty = MathUtils.floor(y / tileLayer.getTileHeight());
        ty -= this.y;

        return ty;
    }

    TiledMapTileLayer.Cell getCell(float x, float y) {
        return tileLayer.getCell(getTileX(x), getTileY(y));
    }

    public boolean isSolid(float x, float y) {
        Polygon polygon = getPolygon(x, y);
        if(polygon == null) return false;
        return polygon.contains(x, y);
    }

}
