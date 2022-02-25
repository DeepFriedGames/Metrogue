package com.deepfried.game;

import com.badlogic.gdx.utils.Array;

public class Area {
    final static int W = 56, H = 24;
    final public Array<Room> rooms = new Array<>();
    final public Sector[][] sectorMap = new Sector[W][H];

}
