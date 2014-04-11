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
import java.util.Random;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * App state to manage mazes in the Maze Game.
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
     * maze topology: set by initialize()
     */
    private GridGraph maze;
    /**
     * pseudo-random number generator: set by constructor
     */
    final private Random generator;
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
     * Access the number generator.
     *
     * @return pre-existing instance
     */
    public Random getGenerator() {
        assert generator != null;
        return generator;
    }

    /**
     * Access the maze topology.
     *
     * @return pre-existing instance
     */
    public GridGraph getMaze() {
        assert maze != null;
        return maze;
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
        int mazeColumns = 12;
        int mazeRows = 12;
        float vertexSpacing = 20f; // world units
        float baseY = 0f; // world coordinate
        maze = new GridGraph(mazeRows, mazeColumns, vertexSpacing,
                generator, baseY);
    }
}