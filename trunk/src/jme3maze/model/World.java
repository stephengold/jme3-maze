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
import java.util.Random;
import java.util.logging.Logger;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * The complete abstract game data (MVC model) for the Maze Game, including a
 * player, a maze, and a set of free items.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class World {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(World.class.getName());
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
     * free items
     */
    final private FreeItems freeItems = new FreeItems();
    /**
     * maze model instance: set by constructor
     */
    private GridGraph maze;
    /**
     * player's starting point: set by constructor
     */
    private NavArc playerStartArc;
    /**
     * player model instance: set by constructor
     */
    private Player player;
    /**
     * pseudo-random number generator: set by constructor
     */
    final private Random generator;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a world using the default seed.
     */
    public World() {
        this(defaultSeed);
    }

    /**
     * Instantiate a world using the specified seed.
     */
    World(long seed) {
        generator = new Random(seed);
        initializeMaze();
        initializePlayer();
        initializeItems();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the free items.
     *
     * @return pre-existing instance
     */
    public FreeItems getFreeItems() {
        assert freeItems != null;
        return freeItems;
    }

    /**
     * Access the maze.
     *
     * @return pre-existing instance
     */
    public GridGraph getMaze() {
        assert maze != null;
        return maze;
    }

    /**
     * Access the player.
     *
     * @return pre-existing instance
     */
    public Player getPlayer() {
        assert player != null;
        return player;
    }
    // *************************************************************************
    // private methods

    /**
     * Add items to the world. Assumes that the player has been initialized.
     */
    private void initializeItems() {
        /*
         * Place the McGuffin (game-ending goal) at the vertex
         * furthest from the starting point.
         */
        NavVertex startVertex = playerStartArc.getFromVertex();
        NavVertex goalVertex = maze.findFurthest(startVertex);
        Item mcGuffin = new Item("McGuffin");
        freeItems.add(mcGuffin, goalVertex);
    }

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

    /**
     * Initialize the player. Assumes that the maze has been initialized.
     */
    private void initializePlayer() {
        /*
         * Choose a random starting arc.
         */
        playerStartArc = maze.randomArc(generator);
        /*
         * Create the player at the start of the chosen arc.
         */
        player = new Player(playerStartArc);
    }
}