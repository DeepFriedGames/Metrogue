package com.deepfried.game;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Stack;

public class AreaGenerator {
    final Area area = new Area();
    public RectangleGraph rGraph;
    public CartesianGraph sGraph;
    public final ArrayList<KeyItem> keyItemList = new ArrayList<>();

    private Rectangle[] subdivide(Rectangle rectangle, DivisionHeuristic heuristic) {
        //SUBDIVISION METHOD
        //results in a more uneven distributed layout
        //start by dividing the whole map into 2 smaller rooms
        Array<Rectangle> array = new Array<>();
        Rectangle divide = new Rectangle(rectangle);
        array.add(divide);

        int i = 0; //index to divide
        while(i < array.size) {
            divide = array.get(i);
            if(heuristic.needsDivided(divide)) {
                int width = (int) divide.getWidth();
                int height = (int) divide.getHeight();

                if (heuristic.needsVerticalDivision(divide)
                        && width > 1) {
                    //divides the room with a vertical line
                    int w = MathUtils.random(1, width - 1);
                    if(MathUtils.randomBoolean()) {
                        array.add(new Rectangle(divide.getX() + width - w, divide.getY(), w, divide.getHeight()));
                        divide.setWidth(width - w);
                    } else {
                        array.add(new Rectangle(divide.getX(), divide.getY(), w, divide.getHeight()));
                        divide.set(divide.getX() + w, divide.getY(), width - w, divide.getHeight());
                    }
                } else if(height > 1) {
                    //divides the room with a horizontal line
                    int h = MathUtils.random(1, height - 1);
                    if(MathUtils.randomBoolean()) {
                        array.add(new Rectangle(divide.getX(), divide.getY() + height - h, divide.getWidth(), h));
                        divide.setHeight(height - h);
                    } else {
                        array.add(new Rectangle(divide.getX(), divide.getY(), divide.getWidth(), h));
                        divide.set(divide.getX(), divide.getY() + h, divide.getWidth(), height - h);
                    }
                }
            } else {
                //repeat for each new room until room condition is met
                i++;
            }
        }

        return array.toArray(Rectangle.class);
    }

    public Area generate() {
//        MathUtils.random.setSeed(365583L);

        Rectangle bounds = new Rectangle(-0.5f, -0.5f, Area.W + 1, Area.H + 1);
        DivisionHeuristic heuristic = new DivisionHeuristic() {
            @Override
            public boolean needsDivided(Rectangle rectangle) {
                float p = 0.15f * MathUtils.log2(rectangle.area());
                return MathUtils.random() < p;
            }

            @Override
            public boolean needsVerticalDivision(Rectangle rectangle) {
                return MathUtils.random() < 0.1f * MathUtils.log2(rectangle.getAspectRatio()) + 0.5f;
            }
        };
        //Subdivide the initial rectangle into smaller rectangles
        rGraph = new RectangleGraph(subdivide(bounds, heuristic));

        //This sector graph represents all connections in the area
        sGraph = makeCartesianGraph();

        //NODE 0: choose a random entrance at extreme (top, bottom, left, right)
        int x = -1, y = -1;
		
        switch(MathUtils.random(0, 3)) {
            case 0:
                x = 0;
                y = MathUtils.random(0, Area.H - 1);
                break;
            case 1:
                x = Area.W - 1;
                y = MathUtils.random(0, Area.H - 1);
                break;
            case 2:
                x = MathUtils.random(0, Area.W - 1);
                y = 0;
                break;
            case 3:
                x = MathUtils.random(0, Area.W - 1);
                y = Area.H - 1;
                break;
        }
        int entrance = getIndex(x, y);
        //NODE N: choose a random exit at the extreme opposite start (bottom, top, right, left)

        x = Math.abs(x - Area.W + 1);
        y = Math.abs(y - Area.H + 1);
        int exit = getIndex(x, y);
        //NODES 2 through N-1: choose random places to put key items
        int numOfKeyItems = keyItemList.size();
        Integer[] keyItemNodes = new Integer[numOfKeyItems];
        int k = 0;
        while(k < numOfKeyItems) {
            x = MathUtils.random(1, Area.W - 2);
            y = MathUtils.random(1, Area.H - 2);
            int key = getIndex(x, y);

            boolean unique = true;
            for(int other : keyItemNodes) {
                if (key == other) {
                    unique = false;
                    break;
                }
            }

            if(unique) {
                keyItemNodes[k] = key;
                k++;
            }
        }

        Array<Integer> indices = new Array<>();
        indices.add(entrance);
        indices.addAll(keyItemNodes);
        indices.add(exit); //key items + exit + entrance

        //this graph represents the key nodes and their connections
        CartesianGraph pGraph = createKeyPathsGraph(indices);

        //top sort the graph
        //visit() method in the top sort creates the Sector
        ArrayList<Sector> sectors = topSort(pGraph);

        //add features here
		for(int sector=0; sector < sectors.size(); sector++) {
			Sector s = sectors.get(sector);
			s.order = sector;
			
			if(sector == 0) {
				//first visit is the start
				s.addFeature(Sector.Feature.START);
			} else {
				for(int k=0; k < numOfKeyItems; k++) {
					if(s.index == keyItemNodes[k]) {
						s.addFeature(Sector.Feature.getItemFeature(keyItemList.get(k)));							
					} else if(s.order > keyItemOrder[k] && !keyHasObstacle[k] && random.nextBoolean()) {
						s.addFeature(Sector.Feature.getItemObstacle(keyItemList.get(k)));
					}
				}
			}
			
		}
        //after all tier 0 items are added, add an area transition
            //these can have features that require tier 0 items which come before it
            //either in the top sort or areas
        //then add tier 1 items, add an area transition
            //these have features that require tier 0 items
            //these may have features that require tier 1 items which come before it
            //either in the top sort or areas
        //then add tier 2 items
            //these have features that require tier 1 items
            //these may have features that require tier 2 items which come before it
            //either in the top sort or areas


        //this still doesn't account for optional items
        //maybe they can be placed and blocked behind items based on index?
        return area;
    }

    private CartesianGraph createKeyPathsGraph(Array<Integer> indices) {
        int numOfKeyNodes = indices.size;

        CartesianGraph pGraph = new CartesianGraph();

        for(int a = 0; a < numOfKeyNodes; a ++) {
            int b = (a + 1) % numOfKeyNodes;
            Integer[] start = sGraph.nodes.get(indices.get(a)),
                    end = sGraph.nodes.get(indices.get(b));

            GraphPath<Integer[]> path = new DefaultGraphPath<>();
            IndexedAStarPathFinder<Integer[]> pathFinder = new IndexedAStarPathFinder<>(sGraph);
            pathFinder.searchNodePath(start, end, sGraph.heuristic, path);

            for(int f = 0; f < path.getCount(); f ++) {
                Integer[] from = path.get(f);
                if(!pGraph.nodes.contains(from)) {
                    pGraph.nodes.add(from);
                    pGraph.connections.add(new Connection[4]);
                }

                Array<Connection<Integer[]>> connections = sGraph.getConnections(from);
                for(int t = f - 1; t <= f + 1; t += 2) {
                    if(0 <= t && t < path.getCount()) {
                        Integer[] to = path.get(t);
                        //cycle through the sGraph connections
                        //find the one that matches
                        //place it in pGraph connections
                        for(int c = 0; c < connections.size; c ++) {
                            Connection<Integer[]> connection = connections.get(c);
                            if(connection.getToNode() == to) {
                                pGraph.connections.get(pGraph.getIndex(from))[c] = connection;
                            }
                        }
                    }
                }
            }

        }
        return pGraph;
    }

    private int getIndex(int x, int y) {
        return x + y * Area.W;
    }

    private CartesianGraph makeCartesianGraph() {
        CartesianGraph sGraph = new CartesianGraph();

        for(int y = 0; y < Area.H; y ++)
            for(int x = 0; x < Area.W; x ++)
                sGraph.nodes.add(new Integer[]{x, y});

        for(int f = 0; f < sGraph.getNodeCount(); f ++) {
            Integer[] from = sGraph.nodes.get(f);
            Connection<Integer[]>[] array = new Connection[4];
            int[] neighbors = new int[]{f + 1, f + Area.W, f - 1, f - Area.W};
            for (int i = 0; i < 4; i++) {
                int n = neighbors[i];
                if (0 <= n && n < sGraph.getNodeCount()) {
                    Integer[] to = sGraph.nodes.get(n);
                    PointConnection connection = new PointConnection(from, to);
                    for (Rectangle r : rGraph.rectangles)
                        if (r.contains(from[0], from[1]) &&
                                r.contains(to[0], to[1])) {
                            connection.cost = 1;
                            break;
                        }

                    array[i] = connection;
                }

                sGraph.connections.add(array);
            }
        }
        return sGraph;
    }

    public ArrayList<Sector> topSort(CartesianGraph graph) {
        ArrayList<Sector> sort = new ArrayList<>(graph.getNodeCount());
        Stack<Integer> stack = new Stack<>();
        boolean[] isVisited = new boolean[graph.getNodeCount()];
        stack.push(0);
		boolean isLeaf = false;
        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (!isVisited[current]) {
                isVisited[current] = true;
                
                visit(current, sort, isLeaf);

				isLeaf = true;
                for (Connection<Integer[]> connection : graph.connections.get(current)) {
                    int dest = graph.getIndex(connection.getToNode());
                    if (!isVisited[dest]) {
                        stack.push(dest);
						isLeaf = false;
					}
                }
            }
        }
        return sort;
    }

    private void visit(int i, ArrayList<Sector> list, boolean isLeaf) {
		Sector s = createSector(i);
		if(isLeaf) 
			s.addFeature(Sector.Feature.ITEM);
		
        list.add(s);
    }

    private Sector createSector(int i) {
        Sector sector = new Sector();
		sector.index = i;
        sector.mapX = sGraph.nodes.get(i)[0];
        sector.mapY = sGraph.nodes.get(i)[1];
        area.sectorMap[sector.mapX][sector.mapY] = sector;

        return sector;
    }

    private class PointConnection implements Connection<Integer[]> {
        public float cost = 10;
        final Integer[] to, from;

        public PointConnection(Integer[] from, Integer[] to) {
            this.to = to;
            this.from = from;
        }

        @Override
        public float getCost() {
            return cost;
        }

        @Override
        public Integer[] getFromNode() {
            return from;
        }

        @Override
        public Integer[] getToNode() {
            return to;
        }
    }
}
