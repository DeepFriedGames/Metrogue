package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Shape2D;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.deepfried.component.CollapsibleComponent;
import com.deepfried.component.CollisionComponent;
import com.deepfried.component.ControllerComponent;
import com.deepfried.component.DoorComponent;
import com.deepfried.component.JumpThroughComponent;
import com.deepfried.component.OneWayComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.ShapeComponent;
import com.deepfried.component.VelocityComponent;
import com.deepfried.game.GdxGame;
import com.deepfried.utility.Segment;

public class CollisionSystem extends EntitySystem {
	private ImmutableArray<Entity> entities;

	private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
	private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);
	private final ComponentMapper<ShapeComponent> shapes = ComponentMapper.getFor(ShapeComponent.class);
	private final ComponentMapper<DoorComponent> doors = ComponentMapper.getFor(DoorComponent.class);
	private final ComponentMapper<CollisionComponent> collisions = ComponentMapper.getFor(CollisionComponent.class);
	private final ComponentMapper<ControllerComponent> controllers = ComponentMapper.getFor(ControllerComponent.class);
	private final ComponentMapper<CollapsibleComponent> collapsibles = ComponentMapper.getFor(CollapsibleComponent.class);
	private final ComponentMapper<JumpThroughComponent> jumpThroughs = ComponentMapper.getFor(JumpThroughComponent.class);
	private final ComponentMapper<OneWayComponent> oneWays = ComponentMapper.getFor(OneWayComponent.class);

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(Family.all(PositionComponent.class, CollisionComponent.class).get());

	}

	@Override
	public void update(float deltaTime) {
		//for every entityA, only certain components care about collisions
		for(int e = 0; e < entities.size(); e ++) {
			Entity entityA = entities.get(e);
			byte maskA = collisions.get(entityA).mask;
			byte flagA = collisions.get(entityA).flags;
			byte ignoreA = collisions.get(entityA).ignore;
			for(int o = e + 1; o < entities.size(); o ++) {
				Entity entityB = entities.get(o);
				byte maskB = collisions.get(entityB).mask;
				byte flagB = collisions.get(entityB).flags;
				byte ignoreB = collisions.get(entityB).ignore;
				if((maskA & flagB & ~ignoreB) > 0)
					collision(entityA, entityB);
				if((maskB & flagA & ~ignoreA) > 0)
					collision(entityB, entityA);
			}

			//once all checking is done, reset the ignore bits for next frame
			collisions.get(entityA).ignore = CollisionComponent.NONE;
		}
	}

	private void collision(Entity entityA, Entity entityB) {
		if(controllers.has(entityA) && doors.has(entityB)) {
			if(!shapesOverlap(entityA, entityB)) return;

			DoorComponent door = doors.get(entityB);
			//set the current room
			GdxGame app = (GdxGame) Gdx.app.getApplicationListener();
			float dy = positions.get(entityA).y - positions.get(entityB).y;
			positions.get(entityA).set(door.outX, door.outY + dy);
			app.setPlayerComponents(entityA.getComponents());
			app.loadMap(door.getToMapName());

		}
		if(oneWays.has(entityA)) {
			OneWayComponent oneWay = oneWays.get(entityA);
			Vector2 v = velocities.get(entityB);
			Rectangle box = Rectangle.tmp.set(shapes.get(entityB).getRectangle()).setPosition(positions.get(entityB));
			float bottom = box.getY();
			float left = box.getX();
			float top = bottom + box.getHeight();
			float right = left + box.getWidth();

			Polyline polyline = shapes.get(entityA).getPolyline();
			float[] vertices = polyline.getTransformedVertices();
			float x1 = vertices[0], y1 = vertices[1], x2 = vertices[2], y2 = vertices[3];

			if(x1 == x2 && (top > Math.min(y1, y2) && bottom < Math.max(y1, y2)))
				if (oneWay.greater) {
					if (right <= x1 && x1 < right + v.x)
						v.x = x1 - right;
				} else if (left + v.x < x1 && x1 <= left)
					v.x = x1 - left;

			if (y1 == y2 && (right > Math.min(x1, x2) && left < Math.max(x1, x2)))
				if (oneWay.greater) {
					if (top <= y1 && y1 < top + v.y)
						v.y = y1 - top;
				} else if (bottom + v.y < y1 && y1 <= bottom)
					v.y = y1 - bottom;
		}
		if(jumpThroughs.has(entityA)) {
			Vector2 v = velocities.get(entityB);
			Rectangle box = Rectangle.tmp.set(shapes.get(entityB).getRectangle()).setPosition(positions.get(entityB));
			float bottom = box.getY();

			Polyline polyline = shapes.get(entityA).getPolyline();
			float[] vertices = polyline.getTransformedVertices();
			float centerX = positions.get(entityB).x + box.getWidth() / 2;
			float x1 = vertices[0], y1 = vertices[1], x2 = vertices[2], y2 = vertices[3];

			if(centerX < Math.min(x1, x2) || centerX > Math.max(x1, x2)) return;

			float m = x2 == x1 ? Float.MAX_VALUE : (y2 - y1) / (x2 - x1);
			float y = m * (centerX - x1) + y1;
			y = Math.min(Math.max(y1, y2), Math.max(Math.min(y1, y2), y));

			if (bottom + v.y < y && y <= bottom) v.y = y - bottom;
		}
		if(collapsibles.has(entityA)) {
			CollapsibleComponent collapsible = collapsibles.get(entityA);
			Rectangle box = Rectangle.tmp.set(shapes.get(entityA).getRectangle()).setPosition(positions.get(entityA));
			float bottom = box.getY();
			float left = box.getX();
			float top = bottom + box.getHeight();
			float right = left + box.getWidth();

			Vector2 v = velocities.get(entityB);
			Rectangle otherBox = Rectangle.tmp2.set(shapes.get(entityB).getRectangle()).setPosition(positions.get(entityB));
			float otherBottom = otherBox.getY();
			float otherLeft = otherBox.getX();
			float otherTop = otherBottom + otherBox.getHeight();
			float otherRight = otherLeft + otherBox.getWidth();

			if(otherBottom + v.y < top && top <= otherBottom)
				collapsible.corporeal = false;

			if(otherTop <= bottom && bottom < otherTop + v.y)
				v.y = bottom - otherTop;
			if(otherLeft + v.x < right && right <= otherLeft)
				v.x = right - otherLeft;
			if(otherRight <= left && left < otherRight + v.x)
				v.x = left - otherRight;
		}
	}

	private boolean shapesOverlap(Entity entityA, Entity entityB) {
		if(!shapes.has(entityA)) {
			if(!shapes.has(entityB))
				return positions.get(entityA).equals(positions.get(entityB));
			else
				return shapes.get(entityB).getShape().contains(positions.get(entityA));
		}
		if(!shapes.has(entityB))
			return shapes.get(entityA).getShape().contains(positions.get(entityB));

		Shape2D shapeA = shapes.get(entityA).getShape();
		Shape2D shapeB = shapes.get(entityB).getShape();
		if(shapeA instanceof Polygon || shapeA instanceof Rectangle || shapeA instanceof Polyline) {
			Segment[] entitySegments = getSegments(entityA);
			if (shapeB instanceof Polygon || shapeB instanceof Rectangle || shapeB instanceof Polyline) {
				Segment[] otherSegments = getSegments(entityA);
				for (Segment segmentA : entitySegments)
					for (Segment segmentB : otherSegments)
						if (segmentA.intersects(segmentB))
							return true;
			}
		}
		else if(shapeA instanceof Circle) {
			Circle circle = (Circle) shapeA;
			if (shapeB instanceof Polygon || shapeB instanceof Rectangle || shapeB instanceof Polyline) {
				Segment[] otherSegments = getSegments(entityA);
				for(Segment segmentB : otherSegments) {
					if(circle.contains(segmentB.xa, segmentB.ya) || circle.contains(segmentB.xb, segmentB.yb))
						return true;
					float rise = segmentB.yb - segmentB.ya;
					float run = segmentB.xb - segmentB.xa;
					float theta = MathUtils.atan(-run / rise);
					float sine = MathUtils.sin(theta);
					float cosine = MathUtils.cos(theta);
					float x1 = circle.x - circle.radius * cosine;
					float y1 = circle.y - circle.radius * sine;
					float x2 = circle.x + circle.radius * cosine;
					float y2 = circle.y + circle.radius * sine;
					Segment diameter = new Segment(x1, y1, x2, y2);
					if(diameter.intersects(segmentB))
						return true;
				}
			}
			else if(shapeB instanceof Circle) {
				Circle circleB = (Circle) shapeB;
					return circle.overlaps(circleB);
			}
		}
		return false;
	}

	public boolean isPathObstructed(float xa, float ya, float xb, float yb) {
		//returns whether an entityA in the alwaysChecking array obstructs the path from (xa, ya) to (xb, yb)
		for(Entity obstruction : entities) {
			if(oneWays.has(obstruction)) {
				Polyline polyline = shapes.get(obstruction).getPolyline();
				float[] vertices = polyline.getTransformedVertices();
				float x1 = vertices[0], y1 = vertices[1], x2 = vertices[2], y2 = vertices[3];

				OneWayComponent oneWay = oneWays.get(obstruction);
				if(x1 == x2 && (Math.min(y1, y2) < Math.max(ya, yb) && Math.min(ya, yb) < Math.max(y1, y2)))
					if (oneWay.greater) {
						if (xa <= x1 && x1 < xb)
							return true;
					} else if (xb < x1 && x1 <= xa)
						return true;

				if (y1 == y2 && (Math.min(x1, x2) < Math.max(xa, xb) && Math.min(xa, xb) < Math.max(x1, x2)))
					if (oneWay.greater) {
						if (ya <= y1 && y1 < yb)
							return true;
					} else if (yb < y1 && y1 <= ya)
						return true;

			}
			if(jumpThroughs.has(obstruction)) {
				if(yb > ya) continue;
				Polyline polyline = shapes.get(obstruction).getPolyline();
				float[] vertices = polyline.getTransformedVertices();
				float x1 = vertices[0], y1 = vertices[1], x2 = vertices[2], y2 = vertices[3];
				if(Math.max(xa, xb) < Math.min(x1, x2) || Math.max(x1, x2) < Math.min(xa, xb)) continue; //domains have no intersection
				if(Math.max(ya, yb) < Math.min(y1, y2) || Math.max(y1, y2) < Math.min(ya, yb)) continue; //ranges have no intersection

				float m1 = x2 == x1 ? Float.MAX_VALUE : (y2 - y1) / (x2 - x1);
				float m2 = xa == xb ? Float.MAX_VALUE : (yb - ya) / (xb - xa);
				float b1 = y1 - m1 * x1;
				float b2 = ya - m2 * xa;
				if(m1 == m2) {
					if (b1 == b2) return true;
					else continue; //edges are parallel
				}

				float xi = (b2 - b1) / (m1 - m2);
				if(xa == xb) xi = xa;
				if(x1 == x2) xi = x1;
				float yi = m1 * xi + b1;
				if(yi <= ya) return true;

			}
			if(collapsibles.has(obstruction)) {
				Rectangle box = Rectangle.tmp.set(shapes.get(obstruction).getRectangle()).setPosition(positions.get(obstruction));
				CollapsibleComponent collapsible = collapsibles.get(obstruction);
				if(!collapsible.corporeal) continue;

				if(box.contains(xb, yb)) return true;

			}
			CollisionComponent collider = collisions.get(obstruction);
			if((collider.mask & CollisionComponent.SOLID) > 0) {
				if(shapes.has(obstruction)) {
					return shapes.get(obstruction).getShape().contains(xa, ya)
							|| shapes.get(obstruction).getShape().contains(xb, yb);
				}
			}
		}
		return false;
	}

	private Segment[] getSegments(Entity entity) {
		Array<Segment> segments = new Array<>();
		Shape2D shape = shapes.get(entity).getShape();
		if(shape instanceof Polyline) {
			Polyline line = (Polyline) shape;
			float[] vertices = line.getTransformedVertices();
			segments.add(new Segment(vertices[0], vertices[1], vertices[2], vertices[3]));
		}
		if(shape instanceof Polygon) {
			Polygon polygon = (Polygon) shape;
			float[] vertices = polygon.getTransformedVertices();
			for (int p = 0; p < vertices.length; p += 2)
				segments.add(new Segment(vertices[p], vertices[p + 1], vertices[(p + 2) % vertices.length], vertices[(p + 3) % vertices.length]));
		}
		if(shape instanceof Rectangle) {
			Rectangle rectangle = (Rectangle) shape;
			float x1 = rectangle.getX(), x2 = rectangle.getX() + rectangle.getWidth();
			float y1 = rectangle.getY(), y2 = rectangle.getY() + rectangle.getHeight();

			segments.addAll(new Segment(x1, y1, x1, y2), new Segment(x1, y2, x2, y2), new Segment(x2, y2, x2, y1), new Segment(x2, y1, x1, y1));
		}
		return segments.toArray(Segment.class);

	}
}