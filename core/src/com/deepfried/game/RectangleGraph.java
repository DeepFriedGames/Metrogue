package com.deepfried.game;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.Arrays;

public class RectangleGraph implements IndexedGraph<Rectangle> {
    public final Rectangle[] rectangles;
    final Array<Connection<Rectangle>>[] connections;

    public RectangleGraph(Rectangle[] rectangles) {
        this.rectangles = rectangles;
        this.connections = setConnections(rectangles);
    }

    public Rectangle getRectangle(float x, float y) {
        for(Rectangle r : rectangles)
            if(r.contains(x, y))
                return r;

        return null;
    }

    private Array<Connection<Rectangle>>[] setConnections(Rectangle[] rectangles) {
        Array<Connection<Rectangle>>[] connections = new Array[rectangles.length];
        for (int f = 0; f < rectangles.length; f++) {
            Rectangle a = rectangles[f];
            if(connections[f] == null)
                connections[f] = new Array<>();

            for (int t = f + 1; t < rectangles.length; t++) {
                Rectangle b = rectangles[t];
                if(connections[t] == null)
                    connections[t] = new Array<>();

                boolean found = false;

                // ensure the rooms overlap but connected edge is not a corner
                if (a.x <= b.x + b.width && a.y <= b.y + b.height && a.x + a.width >= b.x && a.y + a.height >= b.y)
                    found = (a.x == b.x + b.width || b.x == a.x + a.width) ^ (a.y == b.y + b.height || b.y == a.y + a.height);

                if (found) {
                    connections[f].add(new RectangleConnection(f, t));
                    connections[t].add(new RectangleConnection(t, f));
                }
            }
        }
        return connections;
    }

    @Override
    public int getIndex(Rectangle node) {
        return Arrays.binarySearch(rectangles, node);
    }

    @Override
    public int getNodeCount() {
        return rectangles.length;
    }

    @Override
    public Array<Connection<Rectangle>> getConnections(Rectangle fromNode) {
        return connections[getIndex(fromNode)];
    }

    public class RectangleConnection implements Connection<Rectangle> {
        int from, to;

        public RectangleConnection(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public float getCost() {
            return 1;
        }

        @Override
        public Rectangle getFromNode() {
            return rectangles[from];
        }

        @Override
        public Rectangle getToNode() {
            return rectangles[to];
        }
    }
}
