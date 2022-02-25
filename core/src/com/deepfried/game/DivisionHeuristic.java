package com.deepfried.game;

import com.badlogic.gdx.math.Rectangle;

public interface DivisionHeuristic {
    boolean needsDivided(Rectangle room);

    boolean needsVerticalDivision(Rectangle room);
}
