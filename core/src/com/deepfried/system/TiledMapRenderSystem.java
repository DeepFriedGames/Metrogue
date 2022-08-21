package com.deepfried.system;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;

public class TiledMapRenderSystem extends EntitySystem implements MapRenderer {
    private OrthographicCamera camera;
    private Color clearColor = Color.BLACK;

    @Override
    public void update(float deltaTime) {
        ScreenUtils.clear(clearColor);
        camera.update();
        render();
    }

    @Override
    public void setView(OrthographicCamera camera) {
        this.camera = camera;
    }

    @Override
    public void setView(Matrix4 projectionMatrix, float viewboundsX, float viewboundsY, float viewboundsWidth, float viewboundsHeight) {
        this.camera.view.set(projectionMatrix);
        this.camera.view.setToOrtho2D(viewboundsX, viewboundsY, viewboundsWidth, viewboundsHeight);
    }

    @Override
    public void render() {

    }

    @Override
    public void render(int[] layers) {

    }
}
