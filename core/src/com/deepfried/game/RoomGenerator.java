package com.deepfried.game;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.deepfried.component.ColorComponent;
import com.deepfried.component.DebugShapeComponent;
import com.deepfried.component.DoorComponent;
import com.deepfried.component.HitboxComponent;
import com.deepfried.component.PositionComponent;
import com.deepfried.component.TileComponent;
import com.deepfried.system.TileSystem;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import static com.deepfried.game.Room.PPT;

public class RoomGenerator implements Runnable, IndexedGraph<RoomGenerator.RoomNode>, Heuristic<RoomGenerator.RoomNode> {
    private static final Vector2 VECTOR = new Vector2();
    private final Random random = new Random();
    private final Array<RoomNode> nodes = new Array<>();
    private final Array<NodeGraphPath> paths = new Array<>();
    private final Array<RoomNode> keyNodes = new Array<>();
    Room room;
    int width, height;
    int[][] bin;

    public RoomGenerator(Room room) {
        this.room = room;
        this.width = MathUtils.round(room.width * 4 + 1);
        this.height = MathUtils.round(room.height * 4 + 1);
        bin = new int[width * 4][height * 4];
    }

    @Override
    public void run() {
        createNodes(SectorPattern.STAR);
        connectNodes();
        findShortestPath();
        findFarthestPath();
        removeUnusedNodes();
        populateTileMap();
    }

    private void populateTileMap() {
        for(int[] col : bin)
            Arrays.fill(col, 0);

        int p = MathUtils.random(4);
        for(RoomNode node : nodes) {
            p += MathUtils.random(-1, 1);

            if(p < 0) p = 0;
            if(p > 4) p = 4;

            boolean bottom = false, top = false, left = false, right = false; //if a neighbor has solidity

            for(Connection<RoomNode> connection : node.connections) {
                RoomNode to = connection.getToNode();
                if(node.solidity + to.solidity > 0.5) {
                    if(to.x == node.x && to.y < node.y) bottom = true;
                    if (to.x == node.x && to.y > node.y) top = true;
                    if (to.y == node.y && to.x < node.x) left = true;
                    if (to.y == node.y && to.x > node.x) right = true;
                }
            }

            if(node.solidity == 0.5) {
                if(p == 4) node.solidity = 1;
                if(p == 0) node.solidity = 0;

                int xMin, xMax, x0, dx,
                        yMin, yMax, y0, dy;
                x0 = left ? 0 : 3;
                dx = left ? 1 : -1;
                xMin = left ? 0 : 4 - p;
                xMax = right ? 4 : p;

                y0 = bottom ? 0 : 3;
                dy = bottom ? 1 : -1;
                yMin = bottom ? 0 : 4 - p;
                yMax = top ? 4 : p;

                for (int y = y0; yMin <= y && y < yMax; y += dy) {
                    int x = x0;
                    while (xMin <= x && x < xMax) {
                        int tx = node.x + x,
                                ty = node.y + y;
                        bin[tx][ty] = 1;
                        x += dx;
                    }
                    if (top ^ bottom)
                        if (!left) yMax--;
                    if (!right) yMin++;
                }
            } else if(node.solidity == 1) {
                for(int x = 0; x < 4; x++) {
                    int[] column = bin[node.x + x];
                    for(int y = 0; y < 4; y++)
                        column[node.y + y] = 1;
                }
            } else if(node.solidity == 0) {
                if(!top && !bottom) {
                    if(left ^ right)
                        for(int x = left ? 0 : 4 - p; x < (left ? p : 4); x ++)
                            bin[node.x + x][node.y + 3] = 1;
                    else {
                        for (int i = 0; i < p; i++) {
                            bin[random.nextInt(4)][random.nextInt(4)] = 1;
                        }
                    }
                }
            }

        }
        debugPrint();

        createEntities();
    }

    private void createEntities() {
        room.tileSystem = new TileSystem(room);
        for(int x = 2; x < bin.length - 2; x ++)
            for(int y = 2; y < bin[x].length - 2; y ++) {

                TileComponent tile = getTileComponent(x, y);
                HitboxComponent hitbox;
                DebugShapeComponent shape;
                Vector2 position = VECTOR.set(room.getTileX() + x - 2, room.getTileY() + y - 2).scl(PPT);

                if(tile == null) continue;

                switch (tile.id) {
                    case HALF_FULL:
                        hitbox = new HitboxComponent(PPT, PPT / 2f);
                        shape = new DebugShapeComponent(0, 0, PPT, 0, PPT, PPT / 2f, 0, PPT / 2f);
                        break;
                    case FULL:
                        hitbox = new HitboxComponent(PPT, PPT);
                        shape = new DebugShapeComponent(0, 0, PPT, 0, PPT, PPT, 0, PPT);
                        break;
                    case HALF_SLOPE:
                        hitbox = new HitboxComponent(PPT, PPT / 2f);
                        shape = new DebugShapeComponent(0, tile.flipY ? PPT / 2f : 0,
                                PPT, tile.flipY ? PPT / 2f : 0,
                                tile.flipX ? 0 : PPT, tile.flipY ? 0 : PPT / 2f);
                        if (tile.flipY ^ bin[x][y] == 1)
                            position.add(0, PPT / 2f);
                        break;
                    case SLOPE:
                        tile = new TileComponent(TileID.SLOPE, tile.flipX, tile.flipY);
                        hitbox = new HitboxComponent(PPT, PPT);
                        shape = new DebugShapeComponent(0, tile.flipY ? PPT : 0,
                                PPT, tile.flipY ? PPT : 0,
                                tile.flipX ? 0 : PPT, tile.flipY ? 0 : PPT);
                        break;
                    case PLATFORM:
                        tile = new TileComponent(TileID.PLATFORM, false, false);
                        hitbox = new HitboxComponent(PPT, PPT / 4f);
                        shape = new DebugShapeComponent(0, 0, PPT, 0, PPT, PPT / 4f, 0, PPT / 4f);
                        position.add(0, 3 / 4f * PPT);
                        break;
                    default:
                        hitbox = new HitboxComponent(0, 0);
                        shape = null;
                }

                Entity entity = new Entity().add(tile).add(new ColorComponent(Color.BLACK))
                        .add(hitbox).add(new PositionComponent(position));
                entity.add(shape);

                room.tileSystem.tileMap[x - 2][y - 2] = entity;
                room.entities.add(entity);

            }

        //add the door entities
        for(RoomConnection connection : room.connections)
            if(connection.active) {
                Vector2 position = new Vector2();
                Entity door = new Entity()
                        .add(new ColorComponent(Color.BLUE))
                        .add(new DoorComponent(connection));
                if((connection.direction & 0b10) == 0) {
                    position.set(connection.x * Room.TPS - 1, connection.y * Room.TPS + 6);
                    position.scl(PPT).add(1, 0);
                    door.add(new HitboxComponent(2 * PPT - 2, 4 * PPT))
                            .add(new PositionComponent(position));
                } else {
                    position.set(connection.x * Room.TPS + 6, connection.y * Room.TPS - 1);
                    position.scl(PPT).add(0, 1);
                    door.add(new HitboxComponent(4 * PPT, 2 * PPT - 2))
                            .add(new PositionComponent(position));
                }
                room.entities.add(door);
            }
    }

    private TileComponent getTileComponent(int x, int y) {
        //each tile is determined by neighboring bits in the bin
        //678
        //345
        //012
        //each neighbor is 2^n
        boolean flipX = false;
        boolean flipY = false;
        TileID id = null;
        short b = 0;
        b += bin[x-1][y+1] * 0b100000000;
        b += bin[x][y+1] * 0b10000000;
        b += bin[x+1][y+1] * 0b1000000;
        b += bin[x-1][y] * 0b100000;
        b += bin[x][y] * 0b10000;
        b += bin[x+1][y] * 0b1000;
        b += bin[x-1][y-1] * 0b100;
        b += bin[x][y-1] * 0b10;
        b += bin[x + 1][y - 1];

        //'a' checks the four edge bits
        int a = b & 0b010111010;

        switch (a) {
            case 0b010111010:
            case 0b000111010:
            case 0b010011010:
            case 0b010110010:
            case 0b010111000:
            case 0b010010010:
            case 0b000010010:
            case 0b010010000:
                if((b & 0b000111111) == 0b000010111)
                    id = TileID.HALF_FULL;
                else
                    id = TileID.FULL;
                break;
            case 0b000011010:
            case 0b000110010:
            case 0b010011000:
            case 0b010110000:
            case 0b000001010:
            case 0b000100010:
            case 0b010001000:
            case 0b010100000:
                flipX = (b & 0b100000) >> 5 == 1;
                flipY = (b & 0b10000000) >> 7 == 1;
                if ((b & 0b100111011) == 0b000110011 ||
                        (b & 0b001111110) == 0b000011110 ||
                        (b & 0b011111100) == 0b011110000 ||
                        (b & 0b110111001) == 0b110011000 ||
                        (b & 0b100110110) == 0b000100110 ||
                        (b & 0b001011011) == 0b000001011 ||
                        (b & 0b110110100) == 0b110100000 ||
                        (b & 0b011011001) == 0b011001000) {
                    id = TileID.HALF_SLOPE;
                    if(bin[x][y] == 0) {
                        TileComponent neighbor = getTileComponent(flipX ? x - 1 : x + 1, y);
                        assert neighbor != null;
                        if (neighbor.id == TileID.FULL)
                            id = TileID.SLOPE;
                    }
                } else if((b & 0b100110010) == 0b000100010 ||
                        (b & 0b001011010) == 0b000001010 ||
                        (b & 0b010110100) == 0b010100000 ||
                        (b & 0b010011001) == 0b010001000)
                    id = TileID.SLOPE;
                else if((b & 0b10000) >> 4 == 1)
                    id = TileID.FULL;

                break;
            case 0b000111000:
            case 0b000110000:
            case 0b000011000:
            case 0b000010000:
                if((b & 0b111010111) == 0b000010000)
                    id = TileID.PLATFORM;
                else
                    id = TileID.FULL;

                break;
        }
        if(id == null) return null;
        return new TileComponent(id, flipX, flipY);
    }

    private int[][] smooth(int[][] set) {
        int[][] out = set.clone();

        float sum, count;
        for(int x = 0; x < set.length; x++)
            for(int y = 0; y < set[x].length; y++) {
                sum = 0;
                count = 0;
                for(int u = x - 1; u <= x + 1; u ++)
                    if(0 <= u && u < set.length) {
                        int[] col = set[u];
                        for (int v = y - 1; v <= y + 1; v++)
                            if (0 <= v && v < col.length) {
                                sum += col[y];
                                count++;
                            }
                    }
                out[x][y] = Math.round(sum / count);
            }

        return out;
    }

    private void debugPrint() {
        for (int y = bin[0].length - 1; y >= 0; y--) {
            for (int[] column : bin) {
                int n = column[y];
                if (n == 1) System.out.print('X');
                else if (n == 0) System.out.print(' ');
                else System.out.print(n);
            }
            System.out.println();
        }
        System.out.println();
    }

    private void createNodes(SectorPattern sector) {
        float[][] pattern = sector.pattern;

        for(int x = 0; x < width; x ++)
            for (int y = 0; y < height; y ++) {
                float solidity = pattern[x % 4][y % 4];
                if(x == 0 || y == 0 || x == width - 1 || y == height - 1) solidity = 1;
                nodes.add(new RoomNode(4 * x, 4 * y, solidity));
            }

        //certain nodes are saved such as all of the connections to other rooms and hidden items
        //connections with other rooms
        for(RoomConnection connection : room.connections)
            if(connection.active) {
                int x = MathUtils.round(connection.x - room.x) * 4;
                int y = MathUtils.round(connection.y - room.y) * 4;
                switch (connection.direction) {
                    case RoomConnection.DOWN:
                    case RoomConnection.UP:
                        x += 2;
                        break;
                    case RoomConnection.LEFT:
                    case RoomConnection.RIGHT:
                        y += 2;
                        break;
                }

                RoomNode doorNode = nodes.get(x * height + y);
                doorNode.solidity = 0;
                doorNode.pointless = false;
                keyNodes.add(doorNode);
            }
    }

    private void connectNodes() {
        for(int i=0; i<nodes.size; i++) {
            int[] neighbors = new int[]{i - height, i - 1, i + 1, i + height};
            RoomNode s = nodes.get(i);
            for (int test : neighbors)
                if(0 < test && test < nodes.size) {
                    RoomNode t = nodes.get(test);

                    if(s.x == t.x ^ s.y == t.y)
                        s.connections.add(new NodeConnection(s, t));
                }
        }
    }

    private void findShortestPath() {
        IndexedAStarPathFinder<RoomNode> pathFinder = new IndexedAStarPathFinder<>(this);

        //connect the existing key nodes aka the entrances
        for(int s=0; s<keyNodes.size; s++) {
            int t = s + 1;
            RoomNode startNode = keyNodes.get(s);
            while(t < keyNodes.size) {
                NodeGraphPath path = new NodeGraphPath();
                RoomNode endNode = keyNodes.get(t);
                pathFinder.searchNodePath(startNode, endNode, this, path);
                paths.add(path);
                t++;
            }
        }
    }

    private Array<RoomNode[]> computePointlessAreas() {
        Array<RoomNode[]> areas = new Array<>();

        Array<RoomNode> pointlessArea = new Array<>();

        for (RoomNode node : nodes) {
            //check if it is pointless and unclaimed
            boolean nu = true;
            for(RoomNode[] area : areas)
                for(RoomNode test : area)
                    if (test.equals(node)) {
                        nu = false;
                        break;
                    }

            if(node.pointless && nu) {
                //create an array
                pointlessArea.clear();
                pointlessArea.add(node);

                //start with the first item in the array
                for(int p=0; p<pointlessArea.size; p++) {
                    //check if its connected nodes are pointless add to the array
                    RoomNode pNode = pointlessArea.get(p);
                    for(Connection<RoomNode> connection : pNode.connections) {
                        RoomNode to = connection.getToNode();
                        if(to.solidity < 1 && to.pointless && !pointlessArea.contains(to, true))
                            pointlessArea.add(to);
                        //repeat until the end of the array
                    }
                }
                areas.add(pointlessArea.toArray(RoomNode.class));
            }
        }
        return areas;
    }

    private void findFarthestPath() {
        Array<RoomNode[]> pointlessAreas = computePointlessAreas();
        IndexedAStarPathFinder<RoomNode> pathFinder = new IndexedAStarPathFinder<>(this);
        //hidden items
        //choose a random node on the main paths
        if(keyNodes.size > 0) {
            RoomNode start = keyNodes.get(random.nextInt(keyNodes.size));

            RoomNode[] largest = pointlessAreas.first();
            for (RoomNode[] test : pointlessAreas)
                if (largest.length < test.length) largest = test;

            //find the node farthest from that starting node
            RoomNode farthest = start;
            for (RoomNode test : largest) {
                if (test.solidity < 1 && estimate(start, farthest) < estimate(start, test))
                    farthest = test;
            }

            NodeGraphPath farthestPath = new NodeGraphPath();
            pathFinder.searchNodePath(start, farthest, this, farthestPath);
            paths.add(farthestPath);
            keyNodes.add(farthest);
        }

    }

    private void removeUnusedNodes() {
        for(RoomNode node : nodes)
            if(node.pointless && node.solidity < 0.5) node.solidity = 1;
    }

    @Override
    public float estimate(RoomNode node, RoomNode endNode) {
        return Math.abs(node.x - endNode.x) + Math.abs(node.y - endNode.y); //returns the shortest Manhattan distance
    }

    @Override
    public int getIndex(RoomNode node) {
        return nodes.indexOf(node, true);
    }

    @Override
    public int getNodeCount() {
        return nodes.size;
    }

    @Override
    public Array<Connection<RoomNode>> getConnections(RoomNode fromNode) {
        return fromNode.connections;
    }

    public static class RoomNode {
        //represents a 4x4 space
        Array<Connection<RoomNode>> connections = new Array<>();
        float solidity;
        int x, y;
        boolean pointless;

        public RoomNode(int x, int y, float solidity) {
            this.x = x;
            this.y = y;
            this.solidity = solidity;
            this.pointless = true;
        }
    }

    public class NodeGraphPath implements GraphPath<RoomNode> {
        Array<RoomNode> array = new Array<>();

        @Override
        public int getCount() {
            return array.size;
        }

        @Override
        public RoomNode get(int index) {
            return this.array.get(index);
        }

        @Override
        public void add(RoomNode node) {
            node.pointless = false;
            if(node.solidity > 0) node.solidity = 0;
            for(Connection<RoomNode> connection : node.connections) {
                RoomNode to = connection.getToNode();
                if(to.pointless && to.x > 0 && to.y > 0 && to.x < room.getWidth() * 16 && to.y < room.getHeight() * 16) {
                    to.solidity = 0.5f;
                }
            }
            array.add(node);
        }

        @Override
        public void clear() {
            array.clear();
        }

        @Override
        public void reverse() {
            array.reverse();
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<RoomNode> iterator() {
            return array.iterator();
        }
    }

    public static class NodeConnection implements Connection<RoomNode> {
        RoomNode from, to;

        public NodeConnection(RoomNode from, RoomNode to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public float getCost() {
            return 5 * (to.solidity + from.solidity);
        }

        @Override
        public RoomNode getFromNode() {
            return from;
        }

        @Override
        public RoomNode getToNode() {
            return to;
        }
    }

    private enum SectorPattern {
        OPEN(new float[][]{ {0, 0, 0, 0},
                            {0, 0, 0, 0},
                            {0, 0, 0, 0},
                            {0, 0, 0, 0}}),
        ELEVATOR(new float[][]{ {1, 1, 0, 1},
                            {1, 0, 0, 0},
                            {0, 0, 0, 0},
                            {1, 1, 0, 1}}),
        STAR(new float[][]{ {1, 1, 0, 1},
                            {1, 0, 0, 0},
                            {0, 0, 0, 0},
                            {1, 0, 0, 0}});

        float[][] pattern;

        SectorPattern(float[][] pattern) {
            this.pattern = pattern;
        }
    }
}
