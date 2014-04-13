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
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.Noise;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavGraph;
import jme3utilities.navigation.NavVertex;

/**
 * A rectangular grid of uniformly-spaced vertices embedded in a navigation
 * graph.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class GridGraph {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(GridGraph.class.getName());
    // *************************************************************************
    // fields
    /**
     * navigation graph in which the vertices are contained
     */
    final private NavGraph graph;
    /**
     * spacing between vertices in the X and Z directions: set by constructor
     * (in world units, &gt;0)
     */
    final private float vertexSpacing;
    /**
     * number of arcs in the grid
     */
    int numArcs = 0;
    /**
     * number of vertices in the grid
     */
    int numVertices = 0;
    /**
     * rectangular array of vertices in the grid: set by constructor
     */
    final private NavVertex[][] grid;
    /**
     * generator for randomization: set by constructor (not null)
     */
    final private Random generator;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a grid with the specified dimensions etc.
     *
     * @param vertexSpacing spacing between rows and columns in the X-Z plane
     * (in world units, &gt;0)
     * @param yValue y-coordinate of the grid (in world coordinates)
     * @param gridRows number of rows on the X-axis of the grid (&gt;1)
     * @param gridColumns number of columns on the Z-axis of the grid (&gt;1)
     * @param graph navigation graph to contain the vertices (not null)
     * @param generator number generator for randomization (not null)
     * @param name name for this grid (not null)
     */
    GridGraph(float vertexSpacing, float yValue, int gridRows, int gridColumns,
            NavGraph graph, Random generator, String name) {
        assert vertexSpacing > 0f : vertexSpacing;
        assert gridRows > 1 : gridRows;
        assert gridColumns > 1 : gridColumns;
        assert graph != null;
        assert generator != null;
        assert name != null;

        this.generator = generator;
        this.vertexSpacing = vertexSpacing;
        this.graph = graph;
        /**
         * Create a rectangular grid of vertices in which neighbors are
         * connected by arcs.
         */
        grid = new NavVertex[gridRows][gridColumns];
        addVertices(yValue, name);
        addArcs();
        /**
         * Prune arcs until a minimum spanning tree is obtained.
         */
        int numPairs = numVertices - 1;
        if (numPairs <= 0) {
            return;
        }
        pruneTo(numPairs);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find the column index of the specified vertex.
     *
     * @param vertex (member)
     * @return value between 0 and numColumns-1, inclusive
     */
    public int findColumn(NavVertex vertex) {
        graph.validateMember(vertex);

        Vector3f location = vertex.getLocation();
        int gridColumns = getColumns();
        int column = Math.round(location.z / vertexSpacing) + gridColumns / 2;

        if (column < 0) {
            return 0;
        } else if (column >= gridColumns) {
            return gridColumns - 1;
        }
        return column;
    }

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
        graph.validateMember(fromVertex);

        int row = findRow(fromVertex) + rowIncrement;
        int column = findColumn(fromVertex) + columnIncrement;
        NavVertex toVertex = getVertex(row, column);
        if (toVertex != null && fromVertex.hasArcTo(toVertex)) {
            return toVertex;
        }

        return null;
    }

    /**
     * Find the row index of the specified vertex.
     *
     * @param vertex (member)
     * @return value between 0 and numRows-1, inclusive
     */
    public int findRow(NavVertex vertex) {
        graph.validateMember(vertex);

        Vector3f location = vertex.getLocation();
        int gridRows = getRows();
        int row = Math.round(location.x / vertexSpacing) + gridRows / 2;

        if (row < 0) {
            return 0;
        } else if (row >= gridRows) {
            return gridRows - 1;
        }
        return row;
    }

    /**
     * Read the number of columns on the Z-axis of the grid.
     *
     * @return count (&gt;1)
     */
    public int getColumns() {
        int result = grid[0].length;
        assert result > 1 : result;
        return result;
    }

    /**
     * Read the number of rows on the X-axis of the grid.
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
     * @return pre-existing instance (or null if invalid index)
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
     * Read the spacing between vertices in the X and Z directions.
     *
     * @return distance (in world units, &gt;0)
     */
    public float getVertexSpacing() {
        return vertexSpacing;
    }

    /**
     * Select a random arc from this grid.
     *
     * @param generator generator of uniform random values (not null)
     * @return pre-existing instance (member)
     */
    public NavArc randomArc(Random generator) {
        Validate.nonNull(generator, "generator");

        List<NavArc> allArcs = new ArrayList<>();

        int gridRows = getRows();
        int gridColumns = getColumns();
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
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
     * Create a new arc and add it to the grid.
     *
     * @param startVertex not null
     * @param endVertex not null
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
    }

    /**
     * Add arcs to connect all neighboring vertices.
     */
    private void addArcs() {
        int gridRows = getRows();
        int gridColumns = getColumns();
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
                NavVertex vertex = grid[row][column];
                /*
                 * Each vertex has two-to-four neighbors.
                 */
                if (row + 1 < gridRows) {
                    NavVertex north = grid[row + 1][column];
                    addArc(vertex, north);
                    numArcs++;
                }
                if (column + 1 < gridColumns) {
                    NavVertex east = grid[row][column + 1];
                    addArc(vertex, east);
                    numArcs++;
                }
                if (row - 1 >= 0) {
                    NavVertex south = grid[row - 1][column];
                    addArc(vertex, south);
                    numArcs++;
                }
                if (column - 1 >= 0) {
                    NavVertex west = grid[row][column - 1];
                    addArc(vertex, west);
                    numArcs++;
                }
            }
        }
    }

    /**
     * Initialize the grid with vertices.
     *
     * @param yValue y-coordinate of the grid (in world coordinates)
     */
    private void addVertices(float yValue, String namePrefix) {
        int gridRows = getRows();
        int gridColumns = getColumns();
        for (int row = 0; row < gridRows; row++) {
            float x = vertexSpacing * (row - gridRows / 2);
            for (int column = 0; column < gridColumns; column++) {
                float z = vertexSpacing * (column - gridColumns / 2);
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
}