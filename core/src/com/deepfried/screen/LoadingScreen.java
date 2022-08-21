package com.deepfried.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;

public class LoadingScreen implements Screen {
    private final AssetManager assetManager;

    public LoadingScreen(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
//        System.out.println(assetManager.getProgress());
        ScreenUtils.clear(Color.BLACK);
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
}
