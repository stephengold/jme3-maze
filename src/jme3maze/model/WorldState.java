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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.Collection;
import java.util.Random;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavGraph;
import jme3utilities.navigation.NavVertex;

/**
 * App state to manage the maze in the Maze Game.
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class WorldState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * spacing between levels (in world units)
     */
    final private static float levelSpacing = 20f;
    /**
     * spacing between adjacent vertices in a level (in world units)
     */
    final private static float vertexSpacing = 30f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(WorldState.class.getName());
    /**
     * default seed for pseudo-random number generator
     */
    final private static long defaultSeed = 13498675L;
    /**
     * "up" direction in world coordinates
     */
    final public static Vector3f upDirection = Vector3f.UNIT_Y;
    // *************************************************************************
    // fields
    /**
     * levels of the maze: set by initialize()
     */
    private GridGraph[] levels;
    /**
     * topology of the complete maze
     */
    final private NavGraph graph = new NavGraph();
    /**
     * pseudo-random number generator: set by constructor
     */
    final private Random generator;
    /**
     * starting point for player
     */
    private NavArc startArc;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a world using the default seed.
     */
    public WorldState() {
        this(defaultSeed);
    }

    /**
     * Instantiate a world using the specified seed.
     */
    WorldState(long seed) {
        generator = new Random(seed);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find the level index of the specified vertex.
     *
     * @param vertex (not null)
     * @return
     */
    public int findLevelIndex(NavVertex vertex) {
        Validate.nonNull(vertex, "vertex");

        Vector3f location = vertex.getLocation();
        int levelIndex = Math.round(-location.y / levelSpacing);

        return levelIndex;
    }

    /**
     * Access the number generator.
     *
     * @return pre-existing instance
     */
    public Random getGenerator() {
        assert generator != null;
        return generator;
    }

    /**
     * Access a maze level specified by its index.
     *
     * @param levelIndex index of the level (&ge;0)
     * @return pre-existing instance
     */
    public GridGraph getLevel(int levelIndex) {
        Validate.nonNegative(levelIndex, "level");
        GridGraph level = levels[levelIndex];

        assert level != null;
        return level;
    }

    /**
     * Access the maze topology.
     *
     * @return pre-existing instance
     */
    public NavGraph getGraph() {
        assert graph != null;
        return graph;
    }

    /**
     * Read the vertical interval between maze levels.
     *
     * @return distance (in world units, &gt;0)
     */
    public float getLevelSpacing() {
        return levelSpacing;
    }

    /**
     * Determine the number of levels in the maze.
     *
     * @return count (&ge;0)
     */
    public int getNumLevels() {
        int result = levels.length;
        return result;
    }

    /**
     * Access the start arc for this world.
     *
     * @return pre-existing instance
     */
    public NavArc getStartArc() {
        assert startArc != null;
        return startArc;
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
     * Convert the specified direction to a quaternion.
     *
     * @param direction (not null, not zero)
     * @return new instance
     */
    public static Quaternion toOrientation(Vector3f direction) {
        Validate.nonNull(direction, "direction");

        Quaternion result = new Quaternion();
        result.lookAt(direction, upDirection);
        return result;
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this state prior to its first update.
     *
     * @param stateManager (not null)
     * @param application attaching application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isInitialized()) {
            throw new IllegalStateException("already initialized");
        }
        Validate.nonNull(application, "application");
        Validate.nonNull(stateManager, "state manager");
        super.initialize(stateManager, application);

        initializeMaze();
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the maze.
     */
    private void initializeMaze() {
        int numLevels = 2;
        levels = new GridGraph[numLevels];
        NavVertex entryStartVertex = null;
        Vector3f entryEndLocation = null;

        for (int levelIndex = 0; levelIndex < numLevels; levelIndex++) {
            float baseY = -levelIndex * levelSpacing; // world coordinate
            int numRows = 5 + 2 * levelIndex; // 5, 7, 9, ...
            int numColumns = numRows;
            String levelName = String.format("L%d", levelIndex);
            GridGraph level = new GridGraph(vertexSpacing, baseY, numRows,
                    numColumns, graph, generator, levelName, entryStartVertex,
                    entryEndLocation);
            levels[levelIndex] = level;

            NavVertex entryEndVertex;
            if (entryEndLocation == null) {
                /*
                 * top level (level 0)
                 */
                startArc = level.randomArc(generator);
                entryEndVertex = startArc.getFromVertex();
            } else {
                entryEndVertex = level.findVertex(entryEndLocation);
            }
            /*
             * Put the level's exit as far as possible from the entrance.
             */
            Collection<NavVertex> vertices = level.getVertices();
            entryStartVertex = graph.findFurthest(entryEndVertex, vertices);
            NavArc[] arcs = entryStartVertex.getArcs();
            assert arcs.length == 1 : arcs.length;
            NavArc arc = arcs[0];
            Vector3f arcDirection = arc.getStartDirection();
            Vector3f entryStartLocation = entryStartVertex.getLocation();
            entryEndLocation = entryStartLocation.subtract(arcDirection);
            entryEndLocation.y -= vertexSpacing;
        }
    }
}