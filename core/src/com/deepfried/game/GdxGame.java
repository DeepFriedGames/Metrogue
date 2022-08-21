package com.deepfried.game;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.deepfried.screen.GameScreen;
import com.deepfried.screen.LoadingScreen;

public class GdxGame extends Game {
	public final AssetManager assetManager = new AssetManager();
	private final Engine engine = new Engine();
	private final Array<Component> playerComponents = new Array<>();
	private String currentMapFileName = "landing.tmx";

    final float dt = 1/60f;

	double currentTime = TimeUtils.nanosToMillis(TimeUtils.nanoTime()) / 1000d;
	double accumulator = 0.0;

	@Override
	public void create () {
		setScreen(new LoadingScreen(assetManager));
		assetManager.load("vagabond.atlas", TextureAtlas.class);
		assetManager.setLoader(TiledMap.class, new TmxMapLoader());
		assetManager.load(currentMapFileName, TiledMap.class);
	}

	@Override
	public void resize(int width, int height) {
		getScreen().resize(width, height);
	}

	@Override
	public void render() {
//		double newTime = TimeUtils.nanosToMillis(TimeUtils.nanoTime()) / 1000d;
//		double frameTime = newTime - currentTime;
//		currentTime = newTime;
//
//		accumulator += frameTime;
//
//		while ( accumulator >= dt )	{
			if(assetManager.update()) {
				if (getScreen().getClass() != GameScreen.class)
					setScreen(new GameScreen(assetManager.get(currentMapFileName, TiledMap.class)));

				engine.update(Gdx.graphics.getDeltaTime());
			}
			getScreen().render(Gdx.graphics.getDeltaTime());
//			accumulator -= dt;
//		}
	}

	@Override
	public void dispose () {
		getScreen().dispose();
		assetManager.dispose();
	}

	public Engine getEngine() {
		return engine;
	}

	public void loadMap(String mapFileName) {
		getScreen().dispose();
		setScreen(new LoadingScreen(assetManager));
		assetManager.unload(currentMapFileName);
		this.currentMapFileName = mapFileName;
		if(!assetManager.contains(mapFileName)) {
			assetManager.setLoader(TiledMap.class, new TmxMapLoader());
			assetManager.load(mapFileName, TiledMap.class);

		}
	}

	public void setPlayerComponents(Iterable<Component> components) {
		playerComponents.clear();
		for(Component component : components)
			playerComponents.add(component);
	}

	public void addComponentsTo(Entity entity) {
		if(playerComponents.size > 0) entity.removeAll();
		for(Component component : playerComponents)
			entity.add(component);
	}
}
