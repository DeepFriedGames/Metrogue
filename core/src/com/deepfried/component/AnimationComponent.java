package com.deepfried.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

public class AnimationComponent implements Component {
    TextureAtlas atlas;
    public AnimationComponent(TextureAtlas atlas) {
        this.atlas = atlas;
    }


}
