package com.deepfried.game;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class WorldGenerator implements Runnable {
    private static final int ITEMS_PER_AREA = 6;
    private static final int NUMBER_OF_AREAS = 4;
    public final World world;
    public boolean done = false;
    private static final Heuristic<Room> heuristic = new Heuristic<Room>() {
        @Override
        public float estimate(Room node, Room end) {
            float x1 = Math.min(node.x + node.width, end.x + end.width);
            float x2 = Math.max(node.x, end.x);
            float y1 = Math.min(node.y + node.width, end.y + end.width);
            float y2 = Math.max(node.y, end.y);
            return Math.abs(x2 - x1) + Math.abs(y2 - y1);
        }
    };
    private final float width = 56, height = 56;

    public WorldGenerator() {
        this.world = new World();
    }

    @Override
    public void run() {
        MathUtils.random.setSeed(365583);

        Array<KeyItem> items = new Array<>();
        items.addAll(KeyItem.values());

        while(world.areas.size < NUMBER_OF_AREAS) {
            AreaGenerator areaGenerator = new AreaGenerator();
            while (areaGenerator.keyItemList.size() < ITEMS_PER_AREA && items.size > 0) {
                KeyItem i = items.get(MathUtils.random(items.size));
                areaGenerator.keyItemList.add(i);
                items.removeValue(i, true);
            }
            world.areas.add(areaGenerator.generate());
        }

        /*final WorldArea areaA = buildWorldAreaA();
        final WorldArea areaB = buildWorldAreaB(areaA);
        final WorldArea areaC = buildWorldAreaC(areaB);
        world.areas.addAll(areaA, areaB, areaC);
        //create Rooms
        for(WorldArea area : world.areas) {
            removeUnusedRooms(area);
            generateRooms(area);
        }*/

        done = true;
    }

    private void removeUnusedRooms(WorldArea area) {
        for(Room[] pointlessRegion : area.pointlessRegions)
            for(Room unusedRoom : pointlessRegion)
                if(area.rooms.contains(unusedRoom, true)) {
                    area.rooms.removeValue(unusedRoom, true);
                }
    }

    private void findUnitsClosestToPath(final WorldArea area, int n) {
        IndexedAStarPathFinder<Room> pathFinder = new IndexedAStarPathFinder<>(area);
        final HashMap<GraphPath<Connection<Room>>, Float> paths = new HashMap<>();

        for(Room unit : area.unitRooms) {
            Room closest = area.keyRooms.first();
            float d1 = heuristic.estimate(closest, unit);

            for(GraphPath<Connection<Room>> path : area.paths) {
                for(Connection<Room> connection : path) {
                    Room r = connection.getFromNode();
                    float d2 = heuristic.estimate(r, unit);

                    if(d2 <= d1) {
                        closest = r;
                        d1 = heuristic.estimate(closest, unit);
                    }
                }
            }
            GraphPath<Connection<Room>> pathToClosest = new RoomPath();

            if(pathFinder.searchConnectionPath(unit, closest, heuristic, pathToClosest)) {
                paths.put(pathToClosest, d1);
            }
        }

        Array<GraphPath<Connection<Room>>> sortedPaths = new Array<>();
        sortedPaths.addAll(paths.keySet().toArray(new GraphPath[0]));

        sortedPaths.sort(new Comparator<GraphPath<Connection<Room>>>() {
            @Override
            public int compare(GraphPath<Connection<Room>> k1, GraphPath<Connection<Room>> k2) {
                return MathUtils.round(paths.get(k1) - paths.get(k2));
            }
        });

        for(int count = 0; count < n; count++) {
            area.paths.add(sortedPaths.get(count));
        }

    }
    //TODO non-robot unicorns

    private void sortRooms(WorldArea area) {
        area.topSort(0);
    }

    private void findKeyPaths(final WorldArea area) {
        IndexedAStarPathFinder<Room> pathFinder = new IndexedAStarPathFinder<>(area);
        area.paths.clear();
        //finds a path from each key room to each other key room
        //when a connection is used its cost is set to 0 to incentivise back-tracking
        for(int f = 0; f < area.keyRooms.size; f ++) {
            Room from = area.keyRooms.get(f);
            for (int t = f + 1; t < area.keyRooms.size; t++) {
                GraphPath<Connection<Room>> path = new RoomPath();
                Room to = area.keyRooms.get(t);
                if (pathFinder.searchConnectionPath(from, to, heuristic, path)) {
                    area.paths.add(path);
                    for (Connection<Room> connection : path) {
                        RoomConnection roomConnection = (RoomConnection) connection;

                        roomConnection.cost *= 0;
                        roomConnection.active = true;

                        for (RoomConnection toConnection : connection.getToNode().connections)
                            if (toConnection.to.equals(connection.getFromNode())) {
                                toConnection.cost *= 0;
                                toConnection.active = true;
                            }
                    }

                }
            }
        }
    }

    private void setConnections(WorldArea area) {
        for (int f = 0; f < area.rooms.size; f++) {
            for (int t = f + 1; t < area.rooms.size; t++) {
                Room a = area.rooms.get(f);
                Room b = area.rooms.get(t);
                boolean found = false;

                // ensure the rooms overlap but connected edge is not a corner
                if (a.x <= b.x + b.width && a.y <= b.y + b.height && a.x + a.width >= b.x && a.y + a.height >= b.y)
                    found = (a.x == b.x + b.width || b.x == a.x + a.width) ^ (a.y == b.y + b.height || b.y == a.y + a.height);

                if (found) {
                    RoomConnection connectionA = new RoomConnection(a, b);
                    if(area.keyRooms.contains(b, true))
                        connectionA.cost *= 10;
                    a.connections.add(connectionA);

                    RoomConnection connectionB = new RoomConnection(b, a);
                    if(area.keyRooms.contains(a, true))
                        connectionB.cost *= 10;
                    b.connections.add(connectionB);

                    int minX = MathUtils.round(Math.max(a.x, b.x));
                    int maxX = MathUtils.round(Math.min(a.x + a.width, b.x + b.width)) - 1;
                    int minY = MathUtils.round(Math.max(a.y, b.y));

                    float x, y;

                    switch (connectionA.direction) {
                        case RoomConnection.LEFT:
                            x = a.x;
                            y = minY;
                            break;
                        case RoomConnection.RIGHT:
                            x = b.x;
                            y = minY;
                            break;
                        case RoomConnection.DOWN:
                            y = a.y;
                            x = MathUtils.random(minX, maxX);
                            break;
                        case RoomConnection.UP:
                            y = b.y;
                            x = MathUtils.random(minX, maxX);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + connectionA.direction);
                    }

                    connectionA.x = x;
                    connectionA.y = y;
                    connectionB.x = x;
                    connectionB.y = y;
                }
            }
        }
    }

    private void establishPointlessRegions(WorldArea area) {
        Array<Room> pointlessRegion = new Array<>();

        //find the pointless regions
        for (Room room : area.rooms) {
            //check if it is pointless and unclaimed
            boolean fresh = true;
            for(Room[] region : area.pointlessRegions)
                for(Room test : region)
                    if (test.equals(room)) {
                        fresh = false;
                        break;
                    }

            boolean connectionsInactive = true;
            for(RoomConnection connection : room.connections)
                if(connection.active) {
                    connectionsInactive = false;
                    break;
                }

            if(connectionsInactive && fresh) {
                //create an array
                pointlessRegion.clear();
                pointlessRegion.add(room);

                //start with the first item in the array
                for(int p=0; p<pointlessRegion.size; p++) {
                    //check if its connected nodes are pointless add to the array
                    Room check = pointlessRegion.get(p);
                    for(RoomConnection connection : check.connections) {
                        Room to = connection.getToNode();
                        boolean pointless = true;
                        for (RoomConnection toConnection : to.connections) {
                            if (toConnection.active) {
                                pointless = false;
                                break;
                            }
                        }

                        if (pointless && !pointlessRegion.contains(to, true))
                            pointlessRegion.add(to);
                        //repeat until the end of the array
                    }
                }
                area.pointlessRegions.add(pointlessRegion.toArray(Room.class));
            }
        }

        //sort the regions by size
        Comparator<Room[]> comparator = new Comparator<Room[]>() {
            @Override
            public int compare(Room[] a1, Room[] a2) {
                float area1 = 0, area2 = 0;
                for (Room r : a1)
                    area1 += r.area();
                for (Room r : a2)
                    area2 += r.area();

                return MathUtils.round(area2 - area1);
            }
        };
        area.pointlessRegions.sort(comparator);
    }

    private void generateRooms(WorldArea area) {
        for(Room room : area.rooms)
            new RoomGenerator(room).run();
    }

    /*private WorldArea buildWorldAreaA() {
        final WorldArea areaA = new WorldArea();
        areaA.bounds.setSize(width, MathUtils.random(5 * height / 16, 6 * height / 16));
        areaA.bounds.setPosition(0, height - areaA.bounds.height);
        areaA.heuristic = new DivisionHeuristic() {
            @Override
            public boolean needsDivided(Room rectangle) {
                if(areaA.keyRooms.size < 1)
                    if(rectangle.getY() == height - rectangle.height && rectangle.getWidth() > 3 && rectangle.getHeight() > 3 && rectangle.area() < 50) {
                        //Need a large open landing site at the top
                        areaA.keyRooms.add(rectangle);
                        return false;
                    }
                if(rectangle.area() == 1) {
                    //Identify 1x1 rooms landing site side of area D
                    areaA.unitRooms.add(rectangle);
                    return false;
                }
                float p = 0.15f * MathUtils.log2(rectangle.area());
                return MathUtils.random() < p || areaA.unitRooms.size < 4;
            }

            @Override
            public boolean needsVerticalDivision(Room rectangle) {
                return MathUtils.random() < 0.1f * MathUtils.log2(rectangle.getAspectRatio()) + 0.5f;
            }
        };
        //Subdivide Area A
        while(areaA.keyRooms.size < 1) {
            areaA.reset();
            areaA.rooms.addAll(subdivide(new Room(areaA.bounds), areaA.heuristic));
        }

        Room landing = areaA.keyRooms.first();
        Room lowest = areaA.unitRooms.get(0),
                closest = areaA.unitRooms.get(1);
        for(Room unit : areaA.unitRooms) {
            //elevator 1 is the lowest relative to the landing
            float dx = Math.abs(landing.x - unit.x);
            float dy = Math.abs(landing.y - unit.y);
            if(lowest.y == unit.y) {
                if (dx < Math.abs(landing.x - lowest.x)) {
                    lowest = unit;
                    continue;
                }
            } else if (Math.abs(landing.y - lowest.y) < dy) {
                lowest = unit;
                continue;
            }

            if (dx + dy < Math.abs(landing.x - closest.x) + Math.abs(landing.y - closest.y)) {
                closest = unit;
            }
        }
        areaA.keyRooms.add(lowest);
        areaA.keyRooms.add(closest);
        areaA.unitRooms.removeValue(lowest, true);
        areaA.unitRooms.removeValue(closest, true);
        areaA.keyRooms.add(areaA.unitRooms.get(MathUtils.random(areaA.unitRooms.size - 1)));

        setConnections(areaA);
        findKeyPaths(areaA);
        findUnitsClosestToPath(areaA, 4);
        establishPointlessRegions(areaA);

        return areaA;
    }


    private WorldArea buildWorldAreaB(WorldArea areaA) {
        final WorldArea areaB = new WorldArea();
        areaB.bounds.setSize(width, MathUtils.random(7 * height / 16, 8 * height / 16));
        areaB.bounds.setPosition(0, areaA.bounds.y - areaB.bounds.height);
        final Room elevator1 = areaA.keyRooms.get(1);
        final Room elevator2 = areaA.keyRooms.get(2);
        final Room elevator3 = areaA.keyRooms.get(3);
        final Array<Room> potentialBossRooms = new Array<>();
        areaB.heuristic = new DivisionHeuristic() {
            @Override
            public boolean needsDivided(Room rectangle) {
                if(areaB.keyRooms.size < 1) {
                    if (rectangle.y < elevator1.y - 10 && rectangle.x == elevator1.x) {
                        if (rectangle.height == 1) {
                            areaB.keyRooms.add(rectangle);
                            return false;
                        } else
                            return true;
                    }
                } else if (areaB.keyRooms.size < 2) {
                    if (rectangle.y + rectangle.height == areaB.bounds.y + areaB.bounds.height && rectangle.x == elevator2.x) {
                        if (rectangle.width == 1) {
                            areaB.keyRooms.add(rectangle);
                            return false;
                        } else
                            return true;
                    }
                } else if (areaB.keyRooms.size < 3) {
                    if (rectangle.x == elevator3.x) {
                        if (rectangle.width == 1) {
                            areaB.keyRooms.add(rectangle);
                            return false;
                        } else
                            return true;
                    }
                }
                if (rectangle.width == 2 || rectangle.height == 2) {
                    if(rectangle.width == 2 && rectangle.height == 2) {
                        potentialBossRooms.add(rectangle);
                        return false;
                    } else
                        return true;
                }
                if(rectangle.area() == 1) {
                    areaB.unitRooms.add(rectangle);
                    return false;
                }
                return rectangle.area() > 24 || MathUtils.randomBoolean(.14f) || areaB.unitRooms.size < 2;
            }

            @Override
            public boolean needsVerticalDivision(Room rectangle) {
                return MathUtils.random() < 0.1f * MathUtils.log2(rectangle.getAspectRatio()) + 0.5f;
            }
        };

        while(areaB.keyRooms.size < 3 ||
                potentialBossRooms.size < 1 ||
                areaB.unitRooms.size < 2) {
            areaB.reset();
            potentialBossRooms.clear();
            areaB.rooms.addAll(subdivide(new Room(areaB.bounds), areaB.heuristic));
        }

        //The lowest unit is the elevator to area C
        Room e1 = areaB.keyRooms.first();
        Room lowest = areaB.unitRooms.first();
        for(Room unit : areaB.unitRooms) {
            //elevator 5 is the lowest
            if(lowest.y == unit.y) {
                if (Math.abs(e1.x - lowest.x) < Math.abs(e1.x - unit.x))
                    lowest = unit;
            } else if (lowest.y < unit.y)
                lowest = unit;

        }
        areaB.keyRooms.add(lowest);

        //The farthest from elevator 2 is the boss room
        Room e2 = areaB.keyRooms.get(1);
        Room bossRoom = potentialBossRooms.first();
        for(Room r : potentialBossRooms) {
            if(Math.abs(e2.x - bossRoom.x) + Math.abs(e2.y - bossRoom.y) < Math.abs(e2.x - r.x) + Math.abs(e2.y - r.y))
                bossRoom = r;
        }
        areaB.keyRooms.add(bossRoom);

        setConnections(areaB);
        findKeyPaths(areaB);
        findUnitsClosestToPath(areaB, 8);
        establishPointlessRegions(areaB);

        return areaB;
    }

    private WorldArea buildWorldAreaC(WorldArea areaB) {
        final WorldArea areaC = new WorldArea();
        areaC.bounds.setPosition(0, 0);
        areaC.bounds.setSize(width, areaB.bounds.y);
        final Room elevator1 = areaB.keyRooms.get(3);
        areaC.heuristic = new DivisionHeuristic() {
            @Override
            public boolean needsDivided(Room room) {
                if(areaC.keyRooms.size < 1) {
                    if (room.x == elevator1.x && room.y + room.height == areaC.bounds.y + areaC.bounds.height) {
                        if (room.width == 1) {
                            areaC.keyRooms.add(room);
                            return false;
                        } else
                            return true;
                    }
                }
                if(room.area() == 1) {
                    areaC.unitRooms.add(room);
                    return false;
                }
                return room.area() > 10 || MathUtils.randomBoolean(0.30f);
            }

            @Override
            public boolean needsVerticalDivision(Room room) {
                return MathUtils.randomBoolean(0.48f);
            }
        };

        while(areaC.keyRooms.size < 1 || areaC.unitRooms.size < 1) {
            areaC.reset();
            areaC.rooms.addAll(subdivide(new Room(areaC.bounds), areaC.heuristic));
        }

        float halfheight = areaC.bounds.y + areaC.bounds.height / 2;
        Room halfway = areaC.unitRooms.first();
        for(Room unit : areaC.unitRooms)
            if(Math.abs(halfheight - unit.y) < Math.abs(halfheight - halfway.y))
                halfway = unit;

        areaC.keyRooms.add(halfway);

        setConnections(areaC);
        findKeyPaths(areaC);
        findUnitsClosestToPath(areaC, 4);
        establishPointlessRegions(areaC);

        return areaC;
    }

    private WorldArea buildWorldAreaD(WorldArea areaA) {
        final WorldArea areaD = new WorldArea();
        areaD.heuristic = new DivisionHeuristic() {
            @Override
            public boolean needsDivided(Room room) {
                return room.height > 1;
            }

            @Override
            public boolean needsVerticalDivision(Room room) {
                return MathUtils.randomBoolean(.3f);
            }
        };
        areaD.bounds.setSize(12);

        //remove the areas that overlap area D
        Array<Room> toRemove = new Array<>();
        for(Room r : areaA.rooms) {
            float rMaxX = r.x + r.width;
            float dMaxX = areaD.bounds.x + areaD.bounds.width;
            if(areaD.bounds.contains(r) ||
                    r.x >= areaD.bounds.x && r.y >= areaD.bounds.y && rMaxX <= dMaxX && r.y + r.height <= areaD.bounds.y + areaD.bounds.height)
                toRemove.add(r);
            else if(areaD.bounds.overlaps(r)) {
                //trims the rectangle down
                if( r.y < areaD.bounds.y + areaD.bounds.height &&
                        areaD.bounds.x <= r.x &&
                        rMaxX <= dMaxX) {
                    //contains bottom edge
                    //trim the bottom edge
                    float y2 = r.y + r.height;
                    r.setY(areaD.bounds.y + areaD.bounds.height);
                    r.setHeight(y2 - r.y);
                } else if(areaD.bounds.x < r.x && r.x < dMaxX) {
                    //contains left edge
                    //trims the left side
                    r.setX(dMaxX);
                    r.setWidth(rMaxX - r.x);
                } else if (areaD.bounds.x < rMaxX && rMaxX < dMaxX) {
                    //contains right edge
                    //trims the right side
                    r.setWidth(areaD.bounds.x - r.x);
                } else {
                    //doesn't contain an edge
                    //splits into two rectangles
                    Room rect = new Room(r);
                    rect.setX(dMaxX).setWidth(rMaxX - rect.x);
                    areaA.rooms.add(rect);
                    r.setWidth(areaD.bounds.x - r.x);

                }
            }
        }
        areaA.rooms.removeAll(toRemove, true);

        //subdivide Area D
        Room mainShaft = new Room(MathUtils.random(areaD.bounds.x + 1, areaD.bounds.x + areaD.bounds.width - 2), 1, 1, areaD.bounds.height - 1);
        Room attic = new Room(areaD.bounds.x, areaD.bounds.y, areaD.bounds.width, 1);
        //first into a 1xH shaft
        Room left = new Room(areaD.bounds.x, mainShaft.y, mainShaft.x - areaD.bounds.x, mainShaft.height);
        Room right = new Room(mainShaft.x + mainShaft.width, mainShaft.y, areaD.bounds.x + areaD.bounds.width - (mainShaft.x + mainShaft.width), mainShaft.height);
        areaD.rooms.add(mainShaft, attic, left, right);
        areaD.keyRooms.add(mainShaft);
        areaD.keyRooms.add(attic);
        areaD.keyRooms.add(left);
        areaD.keyRooms.add(right);
        //then into Wx1 halls or 1x1
        areaD.rooms.addAll(subdivide(left, areaD.heuristic));
        areaD.rooms.addAll(subdivide(right, areaD.heuristic));

        return areaD;
    }

    private WorldArea buildWorldAreaE(WorldArea areaB) {
        final WorldArea areaE = new WorldArea();
        areaE.heuristic = new DivisionHeuristic() {
            @Override
            public boolean needsDivided(Room room) {
                return false;
            }

            @Override
            public boolean needsVerticalDivision(Room room) {
                return false;
            }
        };
        return areaE;
    }

    private WorldArea buildWorldAreaF(WorldArea areaC) {
        final WorldArea areaF = new WorldArea();
        areaF.heuristic = new DivisionHeuristic() {
            @Override
            public boolean needsDivided(Room room) {
                return false;
            }

            @Override
            public boolean needsVerticalDivision(Room room) {
                return false;
            }
        };
        return areaF;
    }*/

    private Array<Room> subdivide(Room room, DivisionHeuristic heuristic) {
        //SUBDIVISION METHOD
        //results in a more uneven distributed layout
        //start by dividing the whole map into 2 smaller rooms
        Array<Room> rooms = new Array<>();
        Room divide = new Room(room);
        rooms.add(divide);

        int i = 0; //index to divide
        while(i < rooms.size) {
            divide = rooms.get(i);
            if(heuristic.needsDivided(divide)) {
                int width = (int) divide.getWidth();
                int height = (int) divide.getHeight();

                if (heuristic.needsVerticalDivision(divide)
                        && width > 1) {
                    //divides the room with a vertical line
                    int w = MathUtils.random(1, width - 1);
                    if(MathUtils.randomBoolean()) {
                        rooms.add(new Room(divide.getX() + width - w, divide.getY(), w, divide.getHeight()));
                        divide.setWidth(width - w);
                    } else {
                        rooms.add(new Room(divide.getX(), divide.getY(), w, divide.getHeight()));
                        divide.set(divide.getX() + w, divide.getY(), width - w, divide.getHeight());
                    }
                } else if(height > 1) {
                    //divides the room with a horizontal line
                    int h = MathUtils.random(1, height - 1);
                    if(MathUtils.randomBoolean()) {
                        rooms.add(new Room(divide.getX(), divide.getY() + height - h, divide.getWidth(), h));
                        divide.setHeight(height - h);
                    } else {
                        rooms.add(new Room(divide.getX(), divide.getY(), divide.getWidth(), h));
                        divide.set(divide.getX(), divide.getY() + h, divide.getWidth(), height - h);
                    }
                }
            } else {
                //repeat for each new room until room condition is met
                i++;
            }
        }

        return rooms;
    }

    private static class RoomPath implements GraphPath<Connection<Room>> {
        final Array<Connection<Room>> connections = new Array<>();

        @Override
        public int getCount() {
            return connections.size;
        }

        @Override
        public Connection<Room> get(int index) {
            return connections.get(index);
        }

        @Override
        public void add(Connection<Room> connection) {
            connections.add(connection);
        }

        @Override
        public void clear() {
            connections.clear();
        }

        @Override
        public void reverse() {
            connections.reverse();
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<Connection<Room>> iterator() {
            return connections.iterator();
        }

    }
}
