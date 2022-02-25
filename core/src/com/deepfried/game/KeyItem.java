package com.deepfried.game;

import com.badlogic.gdx.utils.Array;

public enum KeyItem {
    CHARGE_BEAM(0),
    ICE_BEAM(0),
    WAVE_BEAM(0),
    MORPH_BALL(0),
    MORPH_BOMBS(0),
    FIRST_MISSILE(0),
    X_RAY(0),
    SPAZER_BEAM(1),
    VARIA_SUIT(1),
    HI_JUMP_BOOTS(1),
    SPEED_BOOTS(1),
    FIRST_SUPERS(1),
    GRAPPLE(1),
    PLASMA_BEAM(2),
    GRAVITY_SUIT(2),
    SPRING_BALL(2),
    SCREW_ATTACK(2),
    SPACE_JUMP(2),
    FIRST_POWER_BOMBS(2);

    private final int tier;

    KeyItem(int tier) {
        this.tier = tier;
    }

	public static Array<KeyItem> getTierList(int tier) {
		Array<KeyItem> array = new Array<>();
		for(KeyItem i : values()) {
			if(i.tier == tier)
				array.add(i);
		}
		return array;
	}
}
