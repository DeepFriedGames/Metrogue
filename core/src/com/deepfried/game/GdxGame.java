package com.deepfried.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.deepfried.screen.LoadingScreen;

public class GdxGame extends Game {
	
	@Override
	public void create () {
		setScreen(new LoadingScreen());
	}

	@Override
	public void resize(int width, int height) {
		getScreen().resize(width, height);
	}

	@Override
	public void render() {
		getScreen().render(Gdx.graphics.getDeltaTime());
	}

	@Override
	public void dispose () {
		getScreen().dispose();
	}	
}
