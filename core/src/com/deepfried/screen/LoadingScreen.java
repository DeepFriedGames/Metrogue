package com.deepfried.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.deepfried.game.WorldGenerator;

public class LoadingScreen implements Screen {
    WorldGenerator worldGenerator;
    Thread worldGenThread;

    @Override
    public void show() {
        worldGenerator = new WorldGenerator();
        worldGenThread = new Thread(worldGenerator);
        worldGenThread.start();

    }

    @Override
    public void render(float delta) {
        if(worldGenerator.done) {
//            ((Game) Gdx.app.getApplicationListener()).setScreen(new DebugScreen(worldGenerator.world));

            ((Game) Gdx.app.getApplicationListener()).setScreen(new MapScreen(worldGenerator.world.areas.first()));
        }

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
