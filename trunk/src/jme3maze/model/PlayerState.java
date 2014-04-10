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
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;
import jme3maze.view.MainViewState;
import jme3maze.view.MapViewState;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * App state to manage the player in the Maze Game. The player has a position in
 * the world and an inventory of items.
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class PlayerState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(PlayerState.class.getName());
    // *************************************************************************
    // fields
    /**
     * state manager: set by initialize()
     */
    private AppStateManager stateManager;
    /**
     * items collected but not yet consumed or dropped
     */
    private ArrayList<Item> inventory = new ArrayList<>();
    /**
     * rate of movement in world units per second (&gt;0)
     */
    private float maxMoveSpeed = 50f;
    /**
     * rate of rotation in radians per second (&gt;0)
     */
    private float maxTurnRate = 5f;
    /*
     * number of moves made since the start of the game (&ge;0)
     */
    private int moveCount = 0;
    /**
     * arc which this player is on: set by constructor
     */
    private NavArc arc;
    /**
     * vertex which this player is at: set by constructor
     */
    private NavVertex vertex;
    /**
     * orientation in world coordinates
     */
    final private Quaternion orientation = new Quaternion();
    /**
     * direction this player facing and moving (unit vector in world
     * coordinates)
     */
    final private Vector3f direction = new Vector3f(1f, 0f, 0f);
    /**
     * location (world coordinates)
     */
    final private Vector3f location = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Access this player's current navigation arc.
     *
     * @return pre-existing instance
     */
    public NavArc getArc() {
        assert arc != null;
        return arc;
    }

    /**
     * Copy this player's direction.
     *
     * @return a new unit vector in world coordinates
     */
    public Vector3f getDirection() {
        assert direction.isUnitVector() : direction;
        return direction.clone();
    }

    /**
     * Copy this player's location.
     *
     * @return new vector in world coordinates
     */
    public Vector3f getLocation() {
        return location.clone();
    }

    /**
     * Read this player's maximum move rate.
     *
     * @return speed in world units per second (&gt;0)
     */
    public float getMaxMoveSpeed() {
        assert maxMoveSpeed > 0f : maxMoveSpeed;
        return maxMoveSpeed;
    }

    /**
     * Read this player's maximum turn rate.
     *
     * @return rate in radians per second (&gt;0)
     */
    public float getMaxTurnRate() {
        assert maxTurnRate > 0f : maxTurnRate;
        return maxTurnRate;
    }

    /**
     * Read this player's move count.
     *
     * @return number of moves initiated (&ge;0)
     */
    public int getMoveCount() {
        assert moveCount >= 0 : moveCount;
        return moveCount;
    }

    /**
     * Read this player's orientation in world coordinates.
     *
     * @return new instance
     */
    public Quaternion getOrientation() {
        assert orientation != null;
        return orientation.clone();
    }

    /**
     * Access this player's current navigation vertex, if any.
     *
     * @return pre-existing instance (or null if not at a vertex)
     */
    public NavVertex getVertex() {
        return vertex;
    }

    /**
     * Record the initiation of a move.
     */
    public void incrementMoveCount() {
        moveCount++;
    }

    /**
     * Rotate this player by a specified angle around a specified axis.
     *
     * @param angle rotation angle (in radians)
     * @param axis (not null, not zero, unaffected)
     */
    public void rotate(float angle, Vector3f axis) {
        Validate.nonNull(axis, "axis");
        if (MyVector3f.isZeroLength(axis)) {
            throw new IllegalArgumentException("axis has zero length");
        }

        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(angle, axis);
        Vector3f newDirection = rotation.mult(direction);
        setDirection(newDirection);
    }

    /**
     * Alter this player's arc.
     *
     * @param newArc (not null)
     */
    final public void setArc(NavArc newArc) {
        Validate.nonNull(newArc, "arc");
        arc = newArc;

        NavVertex fromVertex = arc.getFromVertex();
        setVertex(fromVertex);

        Vector3f startDirection = arc.getStartDirection();
        setDirection(startDirection);
    }

    /**
     * Alter this player's direction.
     *
     * @param newDirection world coordinates (not null, not zero, unaffected)
     */
    public void setDirection(Vector3f newDirection) {
        Validate.nonNull(newDirection, "direction");
        if (MyVector3f.isZeroLength(newDirection)) {
            throw new IllegalArgumentException("direction has zero length");
        }

        Vector3f norm = newDirection.normalize();
        direction.set(norm);
        orientation.lookAt(direction, WorldState.upDirection);
        /*
         * Visualize the rotation.
         */
        MainViewState mainViewState =
                stateManager.getState(MainViewState.class);
        mainViewState.setPlayerOrientation(orientation);
        MapViewState mapViewState = stateManager.getState(MapViewState.class);
        if (mapViewState != null) {
            mapViewState.setPlayerOrientation(orientation);
        }
    }

    /**
     * Alter this player's location.
     *
     * @param newLocation world coordinates (not null, unaffected)
     */
    public void setLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "location");
        location.set(newLocation);
        /*
         * Visualize the translation.
         */
        MainViewState mainViewState =
                stateManager.getState(MainViewState.class);
        mainViewState.setPlayerLocation(location);
        MapViewState mapViewState = stateManager.getState(MapViewState.class);
        if (mapViewState != null) {
            mapViewState.setPlayerLocation(location);
        }
    }

    /**
     * Alter this player's maximum movement rate.
     *
     * @param newSpeed (in world units per second, &gt;0)
     */
    public void setMaxMoveSpeed(float newSpeed) {
        Validate.positive(newSpeed, "speed");
        maxMoveSpeed = newSpeed;
    }

    /**
     * Alter this player's maximum turning rate.
     *
     * @param newRate (in radians per second, &gt;0)
     */
    public void setMaxTurnRate(float newRate) {
        Validate.positive(newRate, "rate");
        maxTurnRate = newRate;
    }

    /**
     * Alter this player's vertex.
     *
     * @param newVertex (or null if not at a vertex)
     */
    public void setVertex(NavVertex newVertex) {
        vertex = newVertex;
        if (vertex != null) {
            Vector3f newLocation = vertex.getLocation();
            setLocation(newLocation);
        }
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

        this.stateManager = stateManager;
        /*
         * Choose a random starting arc.
         */
        WorldState worldState = stateManager.getState(WorldState.class);
        GridGraph maze = worldState.getMaze();
        Random generator = worldState.getGenerator();
        NavArc playerStartArc = maze.randomArc(generator);
        setArc(playerStartArc);
    }
}