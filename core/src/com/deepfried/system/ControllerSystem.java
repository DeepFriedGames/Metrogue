package com.deepfried.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.MathUtils;
import com.deepfried.component.ControllerComponent;
import com.deepfried.component.HitboxComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.VelocityComponent;
import com.deepfried.game.MovementType;
import com.deepfried.screen.DebugScreen;
import com.deepfried.screen.MapScreen;

import static com.deepfried.game.Room.PPT;

public class ControllerSystem extends EntitySystem {
    private final static float MAX_DASH = 2f;
    private final static float DASH_ACCELERATION = 0.0625f;
    private ImmutableArray<Entity> entities;
    private Screen previousScreen, currentScreen;

    private final ComponentMapper<ControllerComponent> controllers = ComponentMapper.getFor(ControllerComponent.class);
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<HitboxComponent> hitboxes = ComponentMapper.getFor(HitboxComponent.class);

    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(ControllerComponent.class, VelocityComponent.class).get());
        currentScreen = ((Game) Gdx.app.getApplicationListener()).getScreen();
    }

    public void update(float deltaTime) {
        for (Entity entity : entities) {

            processInputs(entity);
            updateState(entity);
            setVelocity(entity);
        }
    }

    private void processInputs(Entity entity) {
        VelocityComponent velocity = velocities.get(entity);
        ControllerComponent controller = controllers.get(entity);

        if(velocity.x == 0 &&
                controller.movement == MovementType.SPIN_JUMP) {
            controller.momentum = 0;
            controller.dash = 0;
        }

        if(Gdx.input.isKeyPressed(controller.left) ^ Gdx.input.isKeyPressed(controller.right)) {
            switch (controller.movement) {

                case WALKING:
                    dash(controller);
                case MORPH:
                case JUMP:
                case SPIN_JUMP:
                case FALLING:
                    direction(controller);
                case LONG_JUMP:
                    momentum(controller);
                    break;

                case TURNAROUND:
                    direction(controller);
                case SLIDE:
                    decelerate(controller);
                    break;
            }
        } else {
            decelerate(controller);

        }

        if(/*position.grounded && */controller.movement != MovementType.MORPH && Gdx.input.isKeyJustPressed(controller.jump)) {
            switch (controller.movement) {
                case TURNAROUND:
                    velocity.y = 5.046875f;
                    break;
                case SPIN_JUMP:
                    if(!wallGrabbing(entity))
                        break;
                case WALKING:
                    velocity.y = 4.765625f;
                    break;
                case SLIDE:
                    velocity.y = 2.3828125f;
                    break;
                default:
                    break;
            }
        }
        if(!Gdx.input.isKeyPressed(controller.jump) && velocity.y > 0)
            velocity.y = 0;

        if(Gdx.input.isKeyJustPressed(controller.pause)) {
            if ((((Game) Gdx.app.getApplicationListener()).getScreen().getClass()).equals(MapScreen.class)) {
                ((Game) Gdx.app.getApplicationListener()).setScreen(previousScreen);

            }
            if((((Game) Gdx.app.getApplicationListener()).getScreen().getClass()).equals(DebugScreen.class)){
                DebugScreen screen = ((DebugScreen) ((Game) Gdx.app.getApplicationListener()).getScreen());
                previousScreen = screen;
                ((Game) Gdx.app.getApplicationListener()).setScreen(new MapScreen(screen.world.areas.first()));

            }
        }

    }

    private void updateState(Entity entity) {
        VelocityComponent velocity = velocities.get(entity);
        ControllerComponent controller = controllers.get(entity);
        PositionComponent position = positions.get(entity);

        MovementType newMovement = null;

        if(controller.low) {
            if (Gdx.input.isKeyPressed(controller.right) && controller.horizontal_positive
                    || Gdx.input.isKeyPressed(controller.left) && !controller.horizontal_positive
                    || Gdx.input.isKeyJustPressed(controller.up))
                setLow(entity, false);
            if (Gdx.input.isKeyJustPressed(controller.down)) {
                newMovement = MovementType.MORPH;
                setLow(entity, false);
            }
            if (Gdx.input.isKeyJustPressed(controller.jump)) {
                positions.get(entity).y += 12;
                newMovement = MovementType.JUMP;
                setLow(entity, false);
            }
        } else if(Gdx.input.isKeyJustPressed(controller.down))
            setLow(entity, true);

        switch (controller.movement) {
            case WALKING:
                if((Gdx.input.isKeyJustPressed(controller.left) && controller.horizontal_positive) ||
                        (Gdx.input.isKeyJustPressed(controller.right) && !controller.horizontal_positive))
                    newMovement = MovementType.TURNAROUND;

                if(!position.grounded)
                    newMovement = MovementType.FALLING;

                if(Gdx.input.isKeyJustPressed(controller.jump)) {
                    if(velocity.x == 0)
                        newMovement = MovementType.JUMP;
                    if(Math.abs(velocity.x) > 0)
                        newMovement = MovementType.SPIN_JUMP;
                }

                if(controller.dash > MAX_DASH - DASH_ACCELERATION && Gdx.input.isKeyPressed(controller.run) && Gdx.input.isKeyJustPressed(controller.down))
                    newMovement = MovementType.SLIDE;

                break;
            case TURNAROUND:
                if(controller.momentum == 0)
                    newMovement = MovementType.WALKING;

                if(Gdx.input.isKeyPressed(controller.jump)) {
                    positions.get(entity).y += 8;
                    newMovement = MovementType.SPIN_JUMP;
                }
                break;
            case MORPH:
                if(Gdx.input.isKeyJustPressed(controller.up) || Gdx.input.isKeyJustPressed(controller.jump)) {
                    if(position.grounded) {
                        newMovement = MovementType.WALKING;
                        setLow(entity, true);
                    } else {
                        newMovement = MovementType.FALLING;
                    }
                }
                break;
            case SPIN_JUMP:
                int[] keys = new int[]{controller.down, controller.shoot, controller.up, controller.shoulderL, controller.shoulderR};
                for(int key : keys) {
                    if(Gdx.input.isKeyJustPressed(key)) {
                        newMovement = MovementType.FALLING;
                        break;
                    }
                }
            case LONG_JUMP:
            case JUMP:
            case FALLING:
                if(position.grounded) {
                    newMovement = MovementType.WALKING;
                    setLow(entity, false);
                }
                break;
            case SLIDE:
                if(!position.grounded)
                    newMovement = MovementType.FALLING;

                else if(controller.momentum == 0)
                    newMovement = MovementType.MORPH;

                else if(Gdx.input.isKeyJustPressed(controller.jump))
                    newMovement = MovementType.LONG_JUMP;
                break;
        }
        if(newMovement != null && setHeight(entity, newMovement)) {
            controller.movement = newMovement;
            System.out.println(controller.movement + ", low: " + controller.low);
        }
    }

    private void setVelocity(Entity entity) {
        VelocityComponent velocity = velocities.get(entity);
        ControllerComponent cont = controllers.get(entity);
        if(cont.horizontal_positive)
            velocity.x = cont.momentum + cont.dash;
        else
            velocity.x = -cont.momentum - cont.dash;
    }

    private boolean wallGrabbing(Entity entity) {
        int margin = 8;
        PositionComponent p = positions.get(entity);
        HitboxComponent box = hitboxes.get(entity);
        ControllerComponent controller = controllers.get(entity);

        if(currentScreen.getClass() != DebugScreen.class) return false;

        TileSystem tileSystem = getEngine().getSystem(TileSystem.class);

        float h = box.getHeight();
        int ceil = MathUtils.ceil(h / PPT);
        for(int i = 0; i <= ceil; i++) {
            float y = p.y + Math.min(h, i * PPT);
            if((tileSystem.isSolid(p.x - margin, y) && controller.horizontal_positive) ||
                    (tileSystem.isSolid(p.x + box.width + margin, y) && !controller.horizontal_positive))
                return true;

        }
        return false;
    }

    private void momentum(ControllerComponent controller) {
        if(controller.momentum < controller.movement.max_momentum)
            controller.momentum += controller.movement.acceleration;
        if(controller.momentum > controller.movement.max_momentum)
            controller.momentum = controller.movement.max_momentum;
    }

    private void dash(ControllerComponent controller) {
        if(Gdx.input.isKeyPressed(controller.run) && controller.dash < MAX_DASH) {
                    controller.dash += DASH_ACCELERATION;
        }
    }

    private void direction(ControllerComponent controller) {
        if(((Gdx.input.isKeyPressed(controller.left) && controller.horizontal_positive) ||
                (Gdx.input.isKeyPressed(controller.right) && !controller.horizontal_positive))) {
            controller.momentum = 0;
            controller.dash = 0;
            controller.horizontal_positive = !controller.horizontal_positive;
        }
    }

    private void decelerate(ControllerComponent controller) {
        controller.dash = 0;
        controller.momentum += controller.movement.deceleration;

        if(controller.momentum < 0)
            controller.momentum = 0;
    }

    private void setLow(Entity entity, boolean low) {
        controllers.get(entity).low = low;
        setHeight(entity, controllers.get(entity).movement);
    }

    private boolean setHeight(Entity entity, MovementType move) {
        PositionComponent p = positions.get(entity);
        HitboxComponent box = hitboxes.get(entity);
        ControllerComponent controller = controllers.get(entity);

        if(currentScreen.getClass() != DebugScreen.class) return false;

        TileSystem tileSystem = getEngine().getSystem(TileSystem.class);

        float w = box.getWidth();
        int ceil = MathUtils.ceil(w / PPT);
        for(int i = 0; i <= ceil; i++) {
            float x = p.x + Math.min(w, i * PPT);
            //try the movement's height change
            if(controller.low || tileSystem.isSolid(x, p.y + move.height)) {
                //try getting low
                if(tileSystem.isSolid(x, p.y + move.low_height)) {
                    return false;
                }
                controller.low = true;
            }
        }
        box.setHeight(controller.low ? move.low_height : move.height);
        return true;
    }
}
