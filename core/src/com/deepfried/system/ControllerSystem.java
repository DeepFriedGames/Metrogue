package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.deepfried.component.CollisionComponent;
import com.deepfried.component.ControllerComponent;
import com.deepfried.component.GravityComponent;
import com.deepfried.component.PlayerComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.ShapeComponent;
import com.deepfried.component.VelocityComponent;

public class ControllerSystem extends EntitySystem {
	private static final float WALL_GRAB_MARGIN = 45/16f;
    private static final float TURNING_ACCELERATION = 0.5f;
    private static final float JUMP_DECELERATION = 1/3f;
    private ImmutableArray<Entity> entities;
    private final Array<Integer> pressed = new Array<>();

    private final ComponentMapper<ControllerComponent> controllers = ComponentMapper.getFor(ControllerComponent.class);
    private final ComponentMapper<GravityComponent> gravities = ComponentMapper.getFor(GravityComponent.class);
    private final ComponentMapper<ShapeComponent> shapes = ComponentMapper.getFor(ShapeComponent.class);
    private final ComponentMapper<PlayerComponent> players = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<CollisionComponent> collisions = ComponentMapper.getFor(CollisionComponent.class);

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(ControllerComponent.class,
                GravityComponent.class, ShapeComponent.class, PlayerComponent.class,
                PositionComponent.class, VelocityComponent.class).get());
    }

    public void update(float deltaTime) {
        for (Entity entity : entities) {
            handleInputs(entity);
        }
    }

    private void handleInputs(Entity entity) {
        ControllerComponent codes = controllers.get(entity);
        PlayerComponent player = players.get(entity);
        PositionComponent p = positions.get(entity);
        Rectangle box = shapes.get(entity).getRectangle();

        TileSystem tileSystem = getEngine().getSystem(TileSystem.class);
        CollisionSystem collisionSystem = getEngine().getSystem(CollisionSystem.class);
        float right = p.x + box.width;
        float left = p.x;
        float centerY = p.y + box.height / 2;
        boolean wallRight = tileSystem.isSolid(right + WALL_GRAB_MARGIN, centerY)
                || collisionSystem.isPathObstructed(right, centerY, right + WALL_GRAB_MARGIN, centerY);
        boolean wallLeft = tileSystem.isSolid(left - WALL_GRAB_MARGIN, centerY)
                || collisionSystem.isPathObstructed(left, centerY, left - WALL_GRAB_MARGIN, centerY);


        if(player.next != null) {
            if (player.stateChangeProgress >= 1) {
                player.current = player.next;
                player.next = null;
            } else {
                player.stateChangeProgress += 0.5f;
            }
        }

        checkFalling(entity);

        if(player.current == MovementState.WALL_GRABBING)
            if(!wallLeft && !wallRight)
                setState(entity, MovementState.FALLING);

		if(player.current == MovementState.SLIDING) {
			if(decelerate(entity))
				setState(entity, MovementState.CRAWLING);
				
        } else if(isButtonPressed(entity, codes.left) ^ isButtonPressed(entity, codes.right)) {
            accelerate(entity);
            if(isButtonPressed(entity, codes.left))
                updateDirection(entity, PlayerComponent.LEFT);
            if(isButtonPressed(entity, codes.right))
                updateDirection(entity, PlayerComponent.RIGHT);

			switch(player.current) {
				case STANDING:
				case WALKING:
				case RUNNING:
				case CRAWLING:
					setState(entity, isButtonPressed(entity, codes.run) ? MovementState.RUNNING : MovementState.WALKING);
					break;
				case JUMPING:
				case SPINNING:
				case FALLING:
					if((isButtonPressed(entity, codes.left) && wallLeft)
					 || (isButtonPressed(entity, codes.right) && wallRight))
						setState(entity, MovementState.WALL_GRABBING);
					break;
				case WALL_GRABBING:
				    if(player.next == null) {
                        if ((isButtonPressed(entity, codes.left) && wallLeft)
                                || (isButtonPressed(entity, codes.right) && wallRight))
                            slowDecent(entity);
                    }
					break;
			}
        } else {
			boolean stopped = decelerate(entity);
			switch(player.current) {
				case RUNNING:
				case WALKING:
					if(stopped) setState(entity, MovementState.STANDING);
					break;
				case WALL_GRABBING:
					if(!isButtonPressed(entity, codes.left) && !isButtonPressed(entity, codes.right))
						setState(entity, MovementState.FALLING);
					break;
			}
        }

        if(isButtonPressed(entity, codes.up) ^ isButtonPressed(entity, codes.down)){
			if(isButtonPressed(entity, codes.up)) {
			    if(player.current == MovementState.CRAWLING)
			        setState(entity, MovementState.STANDING);
			    else
                    setAim(entity, PlayerComponent.AIM_UP);
            }
			if(isButtonPressed(entity, codes.down)) {
			    collisions.get(entity).ignore |= CollisionComponent.SEMISOLID;
			    if(player.current == MovementState.STANDING)
			        setState(entity, MovementState.CRAWLING);
                else
                    setAim(entity, PlayerComponent.AIM_DOWN);
            }
        } else if(isButtonPressed(entity, codes.aimLock)){
            setAim(entity, PlayerComponent.AIM_FREE);
        } else {
			//TODO make conditions for when the player sheathes weapon
			setAim(entity, PlayerComponent.WEAPON_OUT);
		}
		
        if(isButtonJustPressed(entity, codes.slide)) {
            switch (player.current) {
                case WALKING:
                case RUNNING:
                    setState(entity, MovementState.SLIDING);
                    break;
                case STANDING:
                    setState(entity, MovementState.CRAWLING);
                    break;
                case SLIDING:
                    setState(entity, isButtonPressed(entity, codes.run) ? MovementState.RUNNING : MovementState.WALKING);
                    break;
                case CRAWLING:
                    setState(entity, MovementState.STANDING);
                    break;
            }
        }

        if(isButtonPressed(entity, codes.run)){
            if(player.current == MovementState.WALKING)
                setState(entity, MovementState.RUNNING);
        } else {
            if(player.current == MovementState.RUNNING)
                setState(entity, MovementState.WALKING);
        }
		
        if(isButtonJustPressed(entity, codes.jump)) {
            jump(entity);

			switch(player.current) {
				case STANDING:
					setState(entity, MovementState.JUMPING);
					break;
                case WALL_GRABBING:
                    if(wallLeft)
                        velocities.get(entity).x = player.current.max_velocity;
                    if(wallRight)
                        velocities.get(entity).x = -player.current.max_velocity;
				case WALKING:
				case RUNNING:
					setState(entity, MovementState.SPINNING);
					break;				
			}
        }
        if(!isButtonPressed(entity, codes.jump)) {
            endJump(entity);
        }

        if(isButtonJustPressed(entity, codes.attack)) {
            attack(entity);
			if(player.aim == PlayerComponent.RESTING)
					setAim(entity, PlayerComponent.WEAPON_OUT);
			if(player.current == MovementState.SPINNING || player.current == MovementState.WALL_GRABBING)
					setState(entity, MovementState.JUMPING);
        }

        if(codes.controller != null)
            queryPressed(codes.controller);

//        debugPrint(entity);
    }

    private void queryPressed(Controller controller) {
        for(int code = controller.getMinButtonIndex(); code < controller.getMaxButtonIndex(); code++){
            if(controller.getButton(code))
                if(!pressed.contains(code, false)) pressed.add(code);
            else
                pressed.removeValue(code, false);
        }
    }

    private void slowDecent(Entity entity) {
        VelocityComponent velocity = velocities.get(entity);

        if(velocity.y < -1/2f)
            velocity.y = -1/2f;

    }

    private boolean isButtonJustPressed(Entity entity, int code) {
        if(controllers.get(entity).controller != null) {
            Controller controller = controllers.get(entity).controller;
            if(!pressed.contains(code, false))
                return controller.getButton(code);
        }
        return Gdx.input.isKeyJustPressed(code);
    }

    private boolean isButtonPressed(Entity entity, int code) {
        if(controllers.get(entity).controller != null) {
            Controller controller = controllers.get(entity).controller;
            return controller.getButton(code);
        }
        return Gdx.input.isKeyPressed(code);
    }

    private void debugPrint(Entity entity) {
//        System.out.println("State progress: " + player.stateChangeProgress);
        System.out.println("Current State: " + players.get(entity).current);
        System.out.println("Next State: " + players.get(entity).next);
        System.out.println("Direction: " + players.get(entity).direction);
        System.out.println("velocity.y: " + velocities.get(entity).y);
//        System.out.println("Aim: " + player.aim);
    }

    private void checkFalling(Entity entity) {
        GravityComponent gravity = gravities.get(entity);
        PlayerComponent player = players.get(entity);

        switch(player.current) {
            case WALL_GRABBING:
            case JUMPING:
            case SPINNING:
            case FALLING:
                if(gravity.grounded)
                    setState(entity, MovementState.STANDING);
                break;
            default:
                if(!gravity.grounded)
                    setState(entity, MovementState.FALLING);
                break;
        }
    }

    private void jump(Entity entity) {
        PlayerComponent player = players.get(entity);
        if(player.current.jump_velocity == 0) return;

        VelocityComponent velocity = velocities.get(entity);

		velocity.y = player.current.jump_velocity;
    }

    private void endJump(Entity entity) {
        VelocityComponent velocity = velocities.get(entity);
        if(velocity.y > 0) velocity.y -= JUMP_DECELERATION;
    }

    private void attack(Entity entity) {
        //TODO this
    }

    private void setAim(Entity entity, int aim) {
        PlayerComponent player = players.get(entity);
        player.aim = aim;
    }

    private void setState(Entity entity, MovementState state) {
        PlayerComponent player = players.get(entity);

        if(player.next != null) return;
		if(player.current == state) return;

		if(!setHeight(entity, state)) {
			//setting the new height is unsuccessful
			switch(player.current) {
				case SLIDING:
				case JUMPING:
				case FALLING:
				case CRAWLING:
					return;
				case SPINNING:
				case WALL_GRABBING:
					player.next = MovementState.CRAWLING;
					setHeight(entity, player.next);
					return;
			}
		}

        player.next = state;
		player.stateChangeProgress = 0;
    }
	
	private boolean setHeight(Entity entity, MovementState next) {
        Rectangle rectangle = shapes.get(entity).getRectangle();
        Rectangle current = Rectangle.tmp.set(rectangle).setPosition(positions.get(entity));

        boolean solidAbove = false, solidBelow = false;
        TileSystem tileSystem = getEngine().getSystem(TileSystem.class);
        CollisionSystem collisionSystem = getEngine().getSystem(CollisionSystem.class);

        float[] sensorsX = tileSystem.getSensors(current.x, current.x + current.width, tileSystem.tileLayer.getTileWidth());

        //from bottom to new height
        float startY = current.y + current.height;
        float endY = current.y + next.height;
        float[] sensorsY = tileSystem.getSensors(startY, endY, tileSystem.tileLayer.getTileHeight());
        loopX:
        for(float sensorX : sensorsX) {
            if(collisionSystem.isPathObstructed(sensorX, startY, sensorX, endY)) {
                solidAbove = true;
                break;
            }
            for (float sensorY : sensorsY)
                if (tileSystem.isSolid(sensorX, sensorY)) {
                    //try to adjust height from bottom second
                    solidAbove = true;
                    break loopX;
                }
        }

        //from top to new bottom
        startY = current.y + current.height - next.height;
        endY = current.y;
        sensorsY = tileSystem.getSensors(startY, endY, tileSystem.tileLayer.getTileHeight());
        loopX:
        for(float sensorX: sensorsX) {
            if(collisionSystem.isPathObstructed(sensorX, startY, sensorX, endY)) {
                solidBelow = true;
                break;
            }
            for (float sensorY : sensorsY)
                if (tileSystem.isSolid(sensorX, sensorY)) {
                    solidBelow = true;
                    break loopX;
                }
        }
        switch (next) {
            case STANDING:
            case WALKING:
            case RUNNING:
            case SLIDING:
            case CRAWLING:
            case FALLING:
                if(next.height <= current.height || !solidAbove) {
                    rectangle.setHeight(next.height);
                    return true;
                }
                if(!solidBelow) {
                    positions.get(entity).y = current.y + current.height - next.height;
                    rectangle.setHeight(next.height);
                    return true;
                }
                break;
            case JUMPING:
            case SPINNING:
            case WALL_GRABBING:
                if(next.height <= current.height || !solidBelow) {
                    positions.get(entity).y = current.y + current.height - next.height;
                    rectangle.setHeight(next.height);
                    return true;
                }
                if(!solidAbove) {
                    rectangle.setHeight(next.height);
                    return true;
                }
                break;
        }
		
		return false;
	}

    private void updateDirection(Entity entity, float direction) {
        PlayerComponent player = players.get(entity);
        if(MathUtils.isEqual(player.direction, direction, 0.001f))
            player.direction = direction;
        else
            player.direction = MathUtils.lerp(player.direction, direction, TURNING_ACCELERATION);
    }

    private boolean decelerate(Entity entity) {
        PlayerComponent player = players.get(entity);
        VelocityComponent velocity = velocities.get(entity);

        if(player.direction > 0)
            if (velocity.x <= 0) velocity.x = 0;
            else velocity.x -= player.current.deceleration;
        if(player.direction < 0)
            if (velocity.x >= 0) velocity.x = 0;
            else velocity.x += player.current.deceleration;

        return velocity.x == 0;

    }

    private void accelerate(Entity entity) {
        PlayerComponent player = players.get(entity);
        VelocityComponent velocity = velocities.get(entity);

        if(player.direction > 0 && velocity.x < player.current.max_velocity)
            velocity.x += player.current.acceleration;
        if(player.direction < 0 && velocity.x > -player.current.max_velocity)
            velocity.x -= player.current.acceleration;

    }

    public enum MovementState {
        STANDING(0, 3/16f, 0f, 62/16f, 27f),
        WALKING(3/16f, 3/16f, 20/16f, 62/16f, 27f),
        RUNNING(4/16f, 2/16f, 31/16f, 62/16f, 27f),
        SLIDING(0, 1/32f, 31/16f, 36/16f, 10f),
        CRAWLING(2/16f, 9/16f, 17/16f, 0, 10f),
        JUMPING(8/16f, 3/16f, 16/16f, 0, 20f),
        FALLING(8/16f, 3/16f, 16/16f, 0, 20f),
        SPINNING(3/16f, 0, 25/16f, 0, 12f),
        WALL_GRABBING(1/32f, 0, 25/16f, 50/16f, 18f);

        public final float acceleration;
        public final float deceleration;
        public final float max_velocity;
        public final float jump_velocity;
		public final float height;

        MovementState(float acceleration, float deceleration, float max_velocity, float jump_velocity, float height) {
            this.acceleration = acceleration;
            this.deceleration = deceleration;
            this.max_velocity = max_velocity;
            this.jump_velocity = jump_velocity;
			this.height = height;
        }
    }
}
