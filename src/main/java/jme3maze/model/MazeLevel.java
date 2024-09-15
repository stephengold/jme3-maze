/*
 Copyright (c) 2014-2023, Stephen Gold

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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.noise.Generator;
import jme3utilities.math.polygon.SimplePolygon3f;
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
    final private static Logger logger = Logger.getLogger(
            MazeLevel.class.getName());
    // *************************************************************************
    // fields

    /**
     * Y-coordinate of the floor (in world coordinates, set by constructor)
     */
    final private float floorY;
    /**
     * tolerance for polygons (in world units, &gt;0, set by constructor)
     */
    final private float tolerance;
    /**
     * navigation graph in which the vertices are embedded (set by constructor)
     */
    final private NavGraph graph;
    /**
     * number of grid arcs in this level (&ge;0)
     */
    private int numGridArcs = 0;
    /**
     * number of grid vertices in this level (&ge;0)
     */
    private int numGridVertices = 0;
    /**
     * rectangular array of vertices (set by constructor)
     */
    final private NavVertex[][] grid;
    /**
     * generator for randomization (not null, set by constructor)
     */
    final private Generator generator;
    /**
     * name for this level (not null): set by constructor
     */
    final private String name;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a level with the specified dimensions etcetera.
     *
     * @param floorY Y-coordinate of the floor (in world coordinates)
     * @param numRows number of rows of vertices on the X-axis (&gt;1)
     * @param numColumns number of columns of vertices on the Z-axis (&gt;1)
     * @param graph navigation graph to contain the vertices (not null)
     * @param generator number generator for randomization (not null)
     * @param name name for this level (not null)
     * @param entryStartVertex vertex to connect to the entry point (or null for
     * none)
     * @param entryEndLocation location for the entry point (or null for none)
     * @param tolerance tolerance for polygons (in world units, &gt;0)
     */
    MazeLevel(float floorY, int numRows, int numColumns, NavGraph graph,
            Generator generator, String name, NavVertex entryStartVertex,
            Vector3f entryEndLocation, float tolerance) {
        assert numRows > 1 : numRows;
        assert numColumns > 1 : numColumns;
        assert graph != null;
        assert generator != null;
        assert name != null;
        assert tolerance > 0f : tolerance;

        this.floorY = floorY;
        this.generator = generator;
        this.graph = graph;
        this.name = name;
        this.tolerance = tolerance;

        assert graph.isConnected();
        assert graph.isReversible();
        /*
         * Create a rectangular grid of vertices in which neighbors are
         * connected by arcs.
         */
        grid = new NavVertex[numRows][numColumns];
        addGridVertices(floorY, name);
        assert numGridVertices == numRows * numColumns : numGridVertices;
        addGridArcs();
        assert numGridArcs == 4 * numGridVertices
                - 2 * (numRows + numColumns) : numGridArcs;
        assert graph.isReversible();

        if (entryEndLocation != null) {
            // Remove the arc-pair which would interfere with entry from above.
            Vector3f underLocation = entryStartVertex.copyLocation();
            underLocation.y = floorY;
            NavVertex underVertex = findGridVertex(underLocation);
            NavVertex endVertex = findGridVertex(entryEndLocation);
            NavArc arc = underVertex.findIncoming(endVertex);
            removeGridPair(arc);
        }
        assert graph.isReversible();
        /*
         * Prune the remaining arc-pairs until a minimum spanning tree is
         * obtained. In this way, all vertices are connected without any loops.
         */
        int numPairs = numGridVertices - 1;
        pruneGridTo(numPairs);

        if (entryEndLocation != null) {
            // Add an entry ramp down from (and up to) the previous level.
            NavVertex endVertex = findGridVertex(entryEndLocation);
            graph.addArcPair(entryStartVertex, endVertex, 1f);
        }
        assert graph.isConnected();
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
        if (FastMath.abs(location.y - floorY) < tolerance) {
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
     * Read the Y-coordinate of this level's floor.
     *
     * @return Y-coordinate value (in world coordinates)
     */
    public float getFloorY() {
        return floorY;
    }

    /**
     * Access the grid vertex in the specified row and column.
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
     * Test whether the specified arc is part of this level's grid.
     *
     * @param arc input (may be null)
     * @return true if grid arc, otherwise false
     */
    public boolean gridContains(NavArc arc) {
        if (arc == null) {
            return false;
        } else if (!MazeLevel.this.gridContains(arc.getFromVertex())) {
            return false;
        } else if (!MazeLevel.this.gridContains(arc.getToVertex())) {
            return false;
        }
        return true;
    }

    /**
     * Test whether the specified vertex is part of this level's grid.
     *
     * @param vertex input (may be null)
     * @return true if grid vertex, otherwise false
     */
    public boolean gridContains(NavVertex vertex) {
        if (vertex == null) {
            return false;
        }
        int row = findRow(vertex);
        if (row == -1) {
            return false;
        }
        int column = findColumn(vertex);
        if (column == -1) {
            return false;
        }
        if (grid[row][column] == vertex) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * List all grid arcs on this level.
     *
     * @return new list of grid arcs
     */
    public List<NavArc> listGridArcs() {
        int numRows = numGridRows();
        int numColumns = numGridColumns();
        List<NavArc> result = new ArrayList<>(4 * numRows * numColumns);

        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numColumns; column++) {
                NavVertex vertex = grid[row][column];
                assert gridContains(vertex);
                if (vertex != null) {
                    NavArc[] out = vertex.copyOutgoing();
                    for (NavArc arc : out) {
                        assert gridContains(arc);
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
     * Enumerate all grid vertices in this level.
     *
     * @return new list of grid vertices
     */
    public List<NavVertex> listGridVertices() {
        int numRows = numGridRows();
        int numColumns = numGridColumns();
        List<NavVertex> result = new ArrayList<>(numRows * numColumns);
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numColumns; column++) {
                NavVertex vertex = grid[row][column];
                if (vertex != null) {
                    assert gridContains(vertex);
                    assert !result.contains(vertex);

                    boolean success = result.add(vertex);
                    assert success : vertex;
                }
            }
        }

        return result;
    }

    /**
     * Calculate the number of columns of vertices on the Z-axis of this level.
     *
     * @return count (&gt;1)
     */
    public int numGridColumns() {
        int result = grid[0].length;
        assert result > 1 : result;
        return result;
    }

    /**
     * Calculate the number of rows of vertices on the X-axis of this level.
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
     * Create a new arc and add it to this level.
     *
     * @param startVertex member
     * @param endVertex member
     */
    private void addArc(NavVertex startVertex, NavVertex endVertex) {
        assert startVertex != null;
        assert endVertex != null;

        graph.addArc(startVertex, endVertex, 1f);
        numGridArcs++;
    }

    /**
     * Add arcs to connect all neighboring vertices.
     */
    private void addGridArcs() {
        int numRows = numGridRows();
        int numColumns = numGridColumns();
        for (int row = 0; row < numRows; row++) {
            for (int column = 0; column < numColumns; column++) {
                NavVertex vertex = grid[row][column];

                // Each vertex has two-to-four neighbors.
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
     * Fill this level with grid vertices (but no arcs).
     *
     * @param yValue y-coordinate for this level (in world coordinates)
     * @param namePrefix (not null)
     */
    private void addGridVertices(float yValue, String namePrefix) {
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
                SimplePolygon3f locus = new SimplePolygon3f(corners, tolerance);

                String vertexName = String.format(
                        "%s(%d,%d)", namePrefix, row, column);
                NavVertex newVertex = graph.addVertex(
                        vertexName, locus, position);

                grid[row][column] = newVertex;
                numGridVertices++;
                assert findRow(newVertex) == row : row;
                assert findColumn(newVertex) == column : column;
            }
        }
    }

    /**
     * Find the column index of the specified vertex.
     *
     * @param vertex (member)
     * @return value between 0 and numGridColumns-1 inclusive or -1 if not on
     * grid
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
     * @return value between 0 and numGridColumns-1 inclusive
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
     * Remove random arc-pairs from the grid until only the specified number of
     * pairs remain.
     *
     * @param numPairs pair-count goal (&ge;0)
     */
    private void pruneGridTo(int numPairs) {
        assert numPairs >= 0 : numPairs;

        List<NavArc> untried = listGridArcs();
        assert numGridArcs == untried.size();

        while (numGridArcs > 2 * numPairs) {
            assert !untried.isEmpty();

            NavArc arc = (NavArc) generator.pick(untried);
            NavArc reverse = arc.findReverse();

            if (graph.isConnectedWithout(arc)) {
                removeGridPair(arc);
            }

            boolean success = untried.remove(arc);
            assert success;
            success = untried.remove(reverse);
            assert success;
        }
    }

    /**
     * Remove a specified arc-pair from the grid.
     *
     * @param arc one arc of the pair (not null, reversible)
     */
    private void removeGridPair(NavArc arc) {
        assert arc != null;
        assert gridContains(arc) : arc;

        NavArc reverse = arc.findReverse();
        assert reverse != null;

        graph.remove(arc);
        graph.remove(reverse);
        this.numGridArcs -= 2;
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
