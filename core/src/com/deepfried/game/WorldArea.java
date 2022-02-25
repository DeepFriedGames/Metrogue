package com.deepfried.game;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class WorldArea implements IndexedGraph<Room> {
    static final Array<Connection<Room>> TMP = new Array<>();
    public final Rectangle bounds = new Rectangle();
    public final Array<Room> rooms = new Array<>();
    public final Array<Room> unitRooms = new Array<>();
    public final Array<Room> keyRooms = new Array<>(true, 6);
    public final Array<GraphPath<Connection<Room>>> paths = new Array<>();
    public final Array<Room[]> pointlessRegions = new Array<>();
    public DivisionHeuristic heuristic;

    public void reset() {
        rooms.clear();
        unitRooms.clear();
        keyRooms.clear();
        paths.clear();
        pointlessRegions.clear();
    }

    @Override
    public int getIndex(Room node) {
        return rooms.indexOf(node, true);
    }

    @Override
    public int getNodeCount() {
        return rooms.size;
    }

    @Override
    public Array<Connection<Room>> getConnections(Room fromNode) {
        TMP.clear();
        TMP.addAll(fromNode.connections.toArray(Connection.class));
        return TMP;
    }

    public void topSort(int at) {
        Room room = rooms.get(at);
        if (room.visited) return;
        room.visited = true;
        rooms.removeIndex(at);
        rooms.insert(0, room);

        for(Connection<Room> connection : room.connections) {
            Room next = connection.getToNode();
            topSort(rooms.indexOf(next, true));
        }

        rooms.reverse();
    }
}
