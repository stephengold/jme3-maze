/*
 Copyright (c) 2014, Stephen Gold
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

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.Noise;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavGraph;
import jme3utilities.navigation.NavVertex;

/**
 * A level of the maze, consisting of a rectangular grid of uniformly-spaced
 * vertices embedded in the overall navigation graph.
 *
 * @author Stephen Gold <sgold@sonic.net>
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
            NavGraph graph, Random generator, String name,
            NavVertex entryStartVertex, Vector3f entryEndLocation) {
        assert numRows > 1 : numRows;
        assert numColumns > 1 : numColumns;
        assert graph != null;
        assert generator != null;
        assert name != null;

        this.generator = generator;
        this.graph = graph;
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
            Vector3f entryStartLocation = entryStartVertex.getLocation();
            Vector3f entryOffset =
                    entryStartLocation.subtract(entryEndLocation);
            entryOffset.y = 0f;
            NavVertex entryEndVertex = findVertex(entryEndLocation);
            NavArc arc = entryEndVertex.findLeastTurn(entryOffset);
            graph.removePair(arc);
            numArcs -= 2;
        }
        /**
         * Prune the remaining arcs until a minimum spanning tree is obtained.
         */
        int numPairs = numVertices - 1;
        pruneTo(numPairs);

        if (entryEndLocation != null) {
            Vector3f entryStartLocation = entryStartVertex.getLocation();
            Vector3f entryOffset =
                    entryEndLocation.subtract(entryStartLocation);
            float entryLength = entryOffset.length();
            Vector3f entryDirection = entryOffset.normalize();
            NavVertex entryEndVertex = findVertex(entryEndLocation);
            graph.addArc(entryStartVertex, entryEndVertex, entryLength,
                    entryDirection);
            Vector3f entryReverseDirection = entryDirection.negate();
            graph.addArc(entryEndVertex, entryStartVertex, entryLength,
                    entryReverseDirection);
            numArcs += 2;
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find the next vertex along the specified line of sight.
     *
     * @param fromVertex (member)
     * @param rowIncrement (-1, 0, or +1)
     * @param columnIncrement (-1, 0, or +1)
     * @return pre-existing instance (or null if none)
     */
    public NavVertex findNextLineOfSight(NavVertex fromVertex,
            int rowIncrement, int columnIncrement) {
        validateMember(fromVertex);

        int row = findRow(fromVertex) + rowIncrement;
        int column = findColumn(fromVertex) + columnIncrement;
        NavVertex toVertex = getVertex(row, column);
        if (toVertex != null && fromVertex.hasArcTo(toVertex)) {
            return toVertex;
        }

        return null;
    }

    /**
     * Find a vertex by its location.
     *
     * @param location (world coordinates, not null)
     * @return pre-existing instance (or null if not found)
     */
    final public NavVertex findVertex(Vector3f location) {
        Validate.nonNull(location, "location");

        int row = findRow(location);
        int column = findColumn(location);
        NavVertex result = grid[row][column];

        return result;
    }

    /**
     * Read the number of columns of vertices on the Z-axis of this level.
     *
     * @return count (&gt;1)
     */
    public int getColumns() {
        int result = grid[0].length;
        assert result > 1 : result;
        return result;
    }

    /**
     * Read the world Y-coordinate for this level's floor.
     *
     * @return Y-coordinate value
     */
    public float getFloorY() {
        float result = grid[0][0].getLocation().getY();
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
     * Read the number of rows of vertices on the X-axis of this level.
     *
     * @return count (&gt;1)
     */
    public int getRows() {
        int result = grid.length;
        assert result > 1 : result;
        return result;
    }

    /**
     * Access the vertex in the specified row and column.
     *
     * @param row
     * @param column
     * @return pre-existing member (or null if invalid index)
     */
    public NavVertex getVertex(int row, int column) {
        if (row < 0 || row >= getRows()) {
            return null;
        }
        if (column < 0 || column >= getColumns()) {
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
        int numRows = getRows();
        int numColumns = getColumns();
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
     * Select a random arc from this level.
     *
     * @param generator generator of uniform random values (not null)
     * @return pre-existing member
     */
    public NavArc randomArc(Random generator) {
        Validate.nonNull(generator, "generator");

        List<NavArc> allArcs = new ArrayList<>(30);

        int numRows = getRows();
        int numColumns = getColumns();
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numColumns; column++) {
                NavVertex vertex = grid[row][column];
                NavArc[] vertexArcs = vertex.getArcs();
                List<NavArc> list = Arrays.asList(vertexArcs);
                allArcs.addAll(list);
            }
        }

        NavArc result = (NavArc) Noise.pick(allArcs, generator);
        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Create a new arc and add it to this level.
     *
     * @param startVertex member
     * @param endVertex member
     */
    private void addArc(NavVertex startVertex, NavVertex endVertex) {
        assert startVertex != null;
        assert endVertex != null;

        Vector3f startPosition = startVertex.getLocation();
        Vector3f offset = endVertex.getLocation();
        offset.subtractLocal(startPosition);
        float pathLength = offset.length();
        Vector3f direction = offset.divide(pathLength);
        graph.addArc(startVertex, endVertex, pathLength, direction);
        numArcs++;
    }

    /**
     * Add arcs to connect all neighboring vertices.
     */
    private void addArcs() {
        int numRows = getRows();
        int numColumns = getColumns();
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

        int numRows = getRows();
        int numColumns = getColumns();
        float vertexSpacing = WorldState.getVertexSpacing();
        for (int row = 0; row < numRows; row++) {
            float x = vertexSpacing * (row - numRows / 2);
            for (int column = 0; column < numColumns; column++) {
                float z = vertexSpacing * (column - numColumns / 2);
                Vector3f position = new Vector3f(x, yValue, z);
                String description =
                        String.format("%s(%d,%d)", namePrefix, row, column);
                NavVertex newVertex = graph.addVertex(description, position);
                assert findRow(newVertex) == row : row;
                assert findColumn(newVertex) == column : column;
                grid[row][column] = newVertex;
                numVertices++;
            }
        }
    }

    /**
     * Test whether this level contains the specified vertex.
     *
     * @param vertex vertex to be tested (or null)
     */
    private boolean contains(NavVertex vertex) {
        if (vertex == null) {
            return false;
        }
        int row = findRow(vertex);
        int column = findColumn(vertex);
        boolean result = grid[row][column] == vertex;

        return result;
    }

    /**
     * Find the column index of the specified vertex.
     *
     * @param vertex (member)
     * @return value between 0 and numColumns-1, inclusive
     */
    private int findColumn(NavVertex vertex) {
        graph.validateMember(vertex);

        Vector3f location = vertex.getLocation();
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

        int numColumns = getColumns();
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
        graph.validateMember(vertex);

        Vector3f location = vertex.getLocation();
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

        int numRows = getRows();
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
            NavArc arc = graph.randomArc(generator);
            if (graph.isConnectedWithout(arc)) {
                graph.removePair(arc);
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