/*
 Copyright (c) 2014-2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3maze.model;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.noise.Noise;
import jme3utilities.math.polygon.SimplePolygon3f;
import jme3utilities.math.spline.LinearSpline3f;
import jme3utilities.math.spline.Spline3f;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavGraph;
import jme3utilities.navigation.NavVertex;

/**
 * A level of the maze, consisting of a rectangular grid of uniformly-spaced
 * vertices embedded in the overall navigation graph.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MazeLevel {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MazeLevel.class.getName());
    // *************************************************************************
    // fields
    /**
     * navigation graph in which the vertices are embedded
     */
    final private NavGraph graph;
    /**
     * number of navigation arcs in this level
     */
    private int numArcs = 0;
    /**
     * number of navigation vertices in this level
     */
    private int numVertices = 0;
    /**
     * rectangular array of vertices: set by constructor
     */
    final private NavVertex[][] grid;
    /**
     * generator for randomization (not null): set by constructor
     */
    final private Random generator;
    /**
     * travel path for each arc (not null, set by constructor)
     */
    final private Map<NavArc, Spline3f> travelPaths;
    /**
     * name for this level (not null): set by constructor
     */
    final private String name;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a level with the specified dimensions etcetera.
     *
     * @param yValue y-coordinate of the floor (in world coordinates)
     * @param numRows number of rows of vertices on the X-axis (&gt;1)
     * @param numColumns number of columns of vertices on the Z-axis (&gt;1)
     * @param graph navigation graph to contain the vertices (not null)
     * @param generator number generator for randomization (not null)
     * @param name name for this level (not null)
     * @param entryStartVertex vertex to connect to the entry point (or null for
     * none)
     * @param entryEndLocation location for the entry point (or null for none)
     */
    MazeLevel(float yValue, int numRows, int numColumns,
            NavGraph graph, Map<NavArc, Spline3f> travelPaths,
            Random generator, String name,
            NavVertex entryStartVertex, Vector3f entryEndLocation) {
        assert numRows > 1 : numRows;
        assert numColumns > 1 : numColumns;
        assert graph != null;
        assert generator != null;
        assert name != null;

        this.generator = generator;
        this.graph = graph;
        this.travelPaths = travelPaths;
        this.name = name;
        /**
         * Create a rectangular grid of vertices in which neighbors are
         * connected by arcs.
         */
        grid = new NavVertex[numRows][numColumns];
        addVertices(yValue, name);
        addArcs();

        if (entryEndLocation != null) {
            /*
             * Remove the arc-pair which would interfere with entry from above.
             */
            Vector3f entryStartLocation = entryStartVertex.copyLocation();
            Vector3f entryOffset =
                    entryStartLocation.subtract(entryEndLocation);
            NavVertex entryEndVertex = findGridVertex(entryEndLocation);
            NavArc arc = entryEndVertex.findOutgoing(entryOffset, 0.75);
            NavArc reverse = arc.findReverse();
            graph.remove(arc);
            graph.remove(reverse);
            numArcs -= 2;
        }
        /**
         * Prune the remaining arcs until a minimum spanning tree is obtained.
         * In this way, all vertices are connected without any loops.
         */
        int numPairs = numVertices - 1;
        pruneTo(numPairs);

        if (entryEndLocation != null) {
            /*
             * Add an entry ramp down from (and up to) the previous level.
             */
            NavVertex endVertex = findGridVertex(entryEndLocation);
            graph.addArcPair(entryStartVertex, endVertex, 1f);
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether a particular vertex is part of this level.
     *
     * @param vertex vertex to test (or null)
     * @return true if it's a member, otherwise false
     */
    public boolean contains(NavVertex vertex) {
        if (vertex == null) {
            return false;
        }
        Vector3f location = vertex.copyLocation();
        if (FastMath.abs(location.y - getFloorY()) < 0.1f) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find the vertex nearest to the specified location.
     *
     * @param location (world coordinates, not null)
     * @return pre-existing instance (or null if not found)
     */
    final public NavVertex findGridVertex(Vector3f location) {
        Validate.nonNull(location, "location");

        int row = findRow(location);
        int column = findColumn(location);
        NavVertex result = grid[row][column];

        return result;
    }

    /**
     * List all grid arcs on this level.
     *
     * @return new list of grid arcs
     */
    public List<NavArc> getArcs() {
        int numRows = numGridRows();
        int numColumns = numGridColumns();
        List<NavArc> result = new ArrayList<>(4 * numRows * numColumns);

        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numColumns; column++) {
                NavVertex vertex = grid[row][column];
                if (vertex != null) {
                    NavArc[] out = vertex.copyOutgoing();
                    for (NavArc arc : out) {
                        assert !result.contains(arc);
                        boolean success = result.add(arc);
                        assert success : vertex;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Read the world Y-coordinate for this level's floor.
     *
     * @return Y-coordinate value
     */
    public float getFloorY() {
        float result = grid[0][0].copyLocation().getY();
        return result;
    }

    /**
     * Read the name of this level.
     *
     * @return name (not null)
     */
    public String getName() {
        assert name != null;
        return name;
    }

    /**
     * Access the vertex in the specified row and column.
     *
     * @param row which row of the grid
     * @param column which column of the grid
     * @return pre-existing member (or null if invalid index)
     */
    public NavVertex getGridVertex(int row, int column) {
        if (row < 0 || row >= numGridRows()) {
            return null;
        }
        if (column < 0 || column >= numGridColumns()) {
            return null;
        }

        NavVertex result = grid[row][column];
        return result;
    }

    /**
     * Enumerate all vertices in this level.
     *
     * @return new collection of members
     */
    public Collection<NavVertex> getVertices() {
        List<NavVertex> result = new ArrayList<>(30);
        int numRows = numGridRows();
        int numColumns = numGridColumns();
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numColumns; column++) {
                NavVertex vertex = grid[row][column];
                boolean success = result.add(vertex);
                assert success : vertex;
            }
        }

        return result;
    }

    /**
     * Read the number of columns of vertices on the Z-axis of this level.
     *
     * @return count (&gt;1)
     */
    public int numGridColumns() {
        int result = grid[0].length;
        assert result > 1 : result;
        return result;
    }

    /**
     * Read the number of rows of vertices on the X-axis of this level.
     *
     * @return count (&gt;1)
     */
    public int numGridRows() {
        int result = grid.length;
        assert result > 1 : result;
        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Create a new straight arc and add it to this level.
     *
     * @param startVertex member
     * @param endVertex member
     */
    private void addArc(NavVertex startVertex, NavVertex endVertex) {
        assert startVertex != null;
        assert endVertex != null;

        NavArc newArc = graph.addArc(startVertex, endVertex, 1f);
        Vector3f[] joints = new Vector3f[2];
        joints[0] = startVertex.copyLocation();
        joints[1] = endVertex.copyLocation();
        Spline3f travelPath = new LinearSpline3f(joints);
        travelPaths.put(newArc, travelPath);
        numArcs++;
    }

    /**
     * Add straight arcs to connect all neighboring vertices.
     */
    private void addArcs() {
        int numRows = numGridRows();
        int numColumns = numGridColumns();
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numColumns; column++) {
                NavVertex vertex = grid[row][column];
                /*
                 * Each vertex has two-to-four neighbors.
                 */
                if (row + 1 < numRows) {
                    NavVertex north = grid[row + 1][column];
                    addArc(vertex, north);
                }
                if (column + 1 < numColumns) {
                    NavVertex east = grid[row][column + 1];
                    addArc(vertex, east);
                }
                if (row - 1 >= 0) {
                    NavVertex south = grid[row - 1][column];
                    addArc(vertex, south);
                }
                if (column - 1 >= 0) {
                    NavVertex west = grid[row][column - 1];
                    addArc(vertex, west);
                }
            }
        }
    }

    /**
     * Fill this level with vertices (but no arcs).
     *
     * @param yValue y-coordinate for this level (in world coordinates)
     * @param namePrefix (not null)
     */
    private void addVertices(float yValue, String namePrefix) {
        assert namePrefix != null;

        int numRows = numGridRows();
        int numColumns = numGridColumns();
        float vertexSpacing = WorldState.getVertexSpacing();
        for (int row = 0; row < numRows; row++) {
            float x = vertexSpacing * (row - numRows / 2);
            for (int column = 0; column < numColumns; column++) {
                float z = vertexSpacing * (column - numColumns / 2);
                Vector3f position = new Vector3f(x, yValue, z);
                float halfWidth = WorldState.getCorridorWidth() / 2;
                Vector3f[] corners = new Vector3f[4];
                corners[0] = position.add(-halfWidth, 0f, -halfWidth);
                corners[1] = position.add(-halfWidth, 0f, halfWidth);
                corners[2] = position.add(halfWidth, 0f, halfWidth);
                corners[3] = position.add(halfWidth, 0f, -halfWidth);
                SimplePolygon3f locus = new SimplePolygon3f(corners, 0.1f);

                String vertexName = String.format(
                        "%s(%d,%d)", namePrefix, row, column);
                NavVertex newVertex = graph.addVertex(vertexName, locus);

                grid[row][column] = newVertex;
                numVertices++;
                assert findRow(newVertex) == row : row;
                assert findColumn(newVertex) == column : column;
            }
        }
    }

    /**
     * Find the column index of the specified vertex.
     *
     * @param vertex (member)
     * @return value between 0 and numColumns-1, inclusive
     */
    private int findColumn(NavVertex vertex) {
        validateMember(vertex);

        Vector3f location = vertex.copyLocation();
        int column = findColumn(location);

        return column;
    }

    /**
     * Find the column index of the specified location.
     *
     * @param location in world coordinates (not null)
     * @return value between 0 and numColumns-1, inclusive
     */
    private int findColumn(Vector3f location) {
        assert location != null;

        int numColumns = numGridColumns();
        float vertexSpacing = WorldState.getVertexSpacing();
        int column = Math.round(location.z / vertexSpacing) + numColumns / 2;

        if (column < 0) {
            return 0;
        } else if (column >= numColumns) {
            return numColumns - 1;
        }
        return column;
    }

    /**
     * Find the row index of the specified vertex.
     *
     * @param vertex (member)
     * @return value between 0 and numRows-1, inclusive
     */
    private int findRow(NavVertex vertex) {
        validateMember(vertex);

        Vector3f location = vertex.copyLocation();
        int row = findRow(location);

        return row;
    }

    /**
     * Find the row index of the specified location.
     *
     * @param location in world coordinates (not null)
     * @return value between 0 and numRows-1, inclusive
     */
    private int findRow(Vector3f location) {
        assert location != null;

        int numRows = numGridRows();
        float vertexSpacing = WorldState.getVertexSpacing();
        int row = Math.round(location.x / vertexSpacing) + numRows / 2;

        if (row < 0) {
            return 0;
        } else if (row >= numRows) {
            return numRows - 1;
        }
        return row;
    }

    /**
     * Remove random arc-pairs until the specified number of pairs remain.
     *
     * @param numPairs pair-count goal (&ge;0)
     */
    private void pruneTo(int numPairs) {
        assert numPairs >= 0 : numPairs;

        while (numArcs > 2 * numPairs) {
            NavArc allArcs[] = graph.copyArcs();
            NavArc arc = (NavArc) Noise.pick(allArcs, generator);
            if (graph.isConnectedWithout(arc)) {
                NavArc reverse = arc.findReverse();
                graph.remove(arc);
                graph.remove(reverse);
                numArcs -= 2;
            }
        }
    }

    /**
     * Verify that a vertex belongs to this level.
     *
     * @param vertex vertex to be validated (or null)
     */
    private void validateMember(NavVertex vertex) {
        if (!contains(vertex)) {
            logger.log(Level.SEVERE, "vertex={0}", vertex);
            throw new IllegalArgumentException(
                    "vertex should be on this level");
        }
    }
}