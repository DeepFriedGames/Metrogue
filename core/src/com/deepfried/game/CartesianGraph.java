package com.deepfried.game;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;

public class CartesianGraph implements IndexedGraph<Integer[]> {
    public final ArrayList<Integer[]> nodes = new ArrayList<>();
    public final ArrayList<Connection<Integer[]>[]> connections = new ArrayList<>();
    public Heuristic<Integer[]> heuristic = new Heuristic<Integer[]>() {
        @Override
        public float estimate(Integer[] a, Integer[] b) {
            return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
        }
    };

    @Override
    public int getIndex(Integer[] node) {
        return nodes.indexOf(node);
    }

    @Override
    public int getNodeCount() {
        return nodes.size();
    }

    @Override
    public Array<Connection<Integer[]>> getConnections(Integer[] from) {
        Array<Connection<Integer[]>> array = new Array<>(4);
        array.addAll(connections.get(getIndex(from)));
        return array;
    }

    public int getNodeX(int i) {
        return nodes.get(i)[0];
    }

    public int getNodeY(int i) {
        return nodes.get(i)[1];
    }
}
