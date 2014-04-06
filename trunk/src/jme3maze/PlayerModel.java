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
package jme3maze;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.math.MyVector3f;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Model class which represents a player of the maze game.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class PlayerModel {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(MazeGame.class.getName());
    /**
     * world "up" direction
     */
    final private static Vector3f upDirection = Vector3f.UNIT_Y;
    // *************************************************************************
    // fields
    /**
     * rate of movement in world units per second (&gt;0)
     */
    private float moveSpeed = 50f;
    /**
     * rate of rotation in radians per second (&gt;0)
     */
    private float turnRate = 5f;
    /*
     * number of moves made since the start of the game (&ge;0)
     */
    private int moveCount = 0;
    /**
     * objectives
     */
    private NavVertex[] goals;
    /**
     * orientation of the avatars in world coordinates
     */
    final private Quaternion orientation = new Quaternion();
    /**
     * direction the avatars are facing and moving (unit vector in world
     * coordinates)
     */
    final private Vector3f direction = Vector3f.UNIT_X;
    /**
     * location of the avatars (world coordinates)
     */
    final private Vector3f location = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a player.
     *
     * @param startArc (not null)
     * @param goals (not null, not empty, unaffected)
     */
    PlayerModel(NavArc startArc, NavVertex[] goals) {
        assert goals != null;
        /*
         * Save a copy of the goals.
         */
        setGoals(goals);
        /*
         * Initialize avatar positions.
         */
        NavVertex startVertex = startArc.getFromVertex();
        Vector3f startLocation = startVertex.getLocation();
        setLocation(startLocation);

        Vector3f startDirection = startArc.getStartDirection();
        setDirection(startDirection);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy this player's direction.
     *
     * @return a new unit vector in world coordinates
     */
    Vector3f getDirection() {
        assert direction.isUnitVector() : direction;
        return direction.clone();
    }

    /**
     * Copy this player's goals.
     *
     * @return a new array
     */
    NavVertex[] getGoals() {
        return goals.clone();
    }

    /**
     * Copy this player's location.
     *
     * @return a new vector in world coordinates
     */
    Vector3f getLocation() {
        return location.clone();
    }

    /**
     * Read the move count.
     *
     * @return number of moves this player has made (&ge;0)
     */
    int getMoveCount() {
        assert moveCount >= 0 : moveCount;
        return moveCount;
    }

    /**
     * Read this player's maximum move rate.
     *
     * @return speed in world units per second (&gt;0)
     */
    float getMoveSpeed() {
        assert moveSpeed > 0f : moveSpeed;
        return moveSpeed;
    }

    /**
     * Read this player's orientation in world coordinates.
     *
     * @return new instance
     */
    Quaternion getOrientation() {
        assert orientation != null;
        return orientation.clone();
    }

    /**
     * Read this player's maximum turn rate.
     *
     * @return rate in radians per second (&gt;0)
     */
    float getTurnRate() {
        return turnRate;
    }

    /**
     * Test whether the specified vertex is a goal of this player.
     *
     * @param vertex (not null)
     * @return true if it's a goal, false if it isn't
     */
    boolean isGoal(NavVertex vertex) {
        assert vertex != null;

        for (NavVertex goal : goals) {
            if (goal == vertex) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rotate this player by a specified angle around a specified axis.
     *
     * @param angle rotation angle (in radians)
     * @param axis (not null, not zero, unaffected)
     */
    void rotate(float angle, Vector3f axis) {
        assert axis != null;
        assert !MyVector3f.isZeroLength(axis);

        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(angle, axis);
        Vector3f newDirection = rotation.mult(direction);
        setDirection(newDirection);
    }

    /**
     * Alter this player's direction.
     *
     * @param newDirection world coordinates (not null, not zero, unaffected)
     */
    final void setDirection(Vector3f newDirection) {
        assert newDirection != null;
        assert !MyVector3f.isZeroLength(newDirection);

        Vector3f norm = newDirection.normalize();
        direction.set(norm);
        orientation.lookAt(direction, upDirection);
    }

    /**
     * Alter this player's goals.
     *
     * @param newGoals new goals (not null, not empty, unaffected, all elements
     * not null)
     */
    final void setGoals(NavVertex[] newGoals) {
        assert newGoals != null;
        assert newGoals.length > 0 : newGoals.length;
        for (NavVertex goal : newGoals) {
            assert goal != null;
        }

        goals = newGoals.clone();
    }

    /**
     * Alter this player's location.
     *
     * @param newLocation world coordinates (not null, unaffected)
     */
    final void setLocation(Vector3f newLocation) {
        assert newLocation != null;

        location.set(newLocation);
    }

    /**
     * Alter this player's movement rate.
     *
     * @param newSpeed (in world units per second, &gt;0)
     */
    void setMoveSpeed(float newSpeed) {
        assert newSpeed > 0f : newSpeed;
        moveSpeed = newSpeed;
    }

    /**
     * Alter this player's turning rate.
     *
     * @param newRate (in radians per second, &gt;0)
     */
    void setTurnRate(float newRate) {
        assert newRate > 0f : newRate;
        turnRate = newRate;
    }

    /**
     * Record the start of a new move.
     */
    void startMove() {
        moveCount++;
    }
}