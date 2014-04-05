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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Application state to translate the player's avatars on a constant-velocity
 * trajectory until they reach a specified vertex.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class MoveState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * tolerance for detecting completion of move (in world units)
     */
    final private static float epsilon = 1e-3f;
    /**
     * maximum number of arcs per vertex for which the avatar turns
     * automatically
     */
    final private static int maxArcsForAutoTurn = 2;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MoveState.class.getName());
    /**
     * the avatar's forward direction in local coordinates
     */
    final private static Vector3f forwardDirection = Vector3f.UNIT_X;
    // *************************************************************************
    // fields
    /**
     * attaching application: set by initialize()
     */
    private Application application;
    /**
     * translation rate of the avatar: set by constructor (in world units per
     * second, &gt;0)
     */
    private float moveSpeed;
    /*
     * number of moves the player has made since the start of the game (&ge;0)
     */
    private int moveCount = 0;
    /**
     * active arc: set by activate()
     */
    private NavArc activeArc;
    /**
     * the player's goals: set by constructor
     */
    private NavVertex[] goals;
    /**
     * the player's avatars: set by constructor (not null, not empty, all
     * elements not null)
     */
    final private Spatial[] avatars;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled move state with a specified avatar, movement rate,
     * and goal.
     *
     * @param avatars the player's avatars (not null, not empty, not altered,
     * all elements not null)
     * @param moveSpeed movement rate of the avatars (in world units per second,
     * &gt;0)
     * @param goals the player's goals (not null, not empty, not altered, all
     * elements not null)
     */
    MoveState(Spatial[] avatars, float moveSpeed, NavVertex[] goals) {
        assert avatars != null;
        assert avatars.length > 0 : avatars.length;
        for (Spatial avatar : avatars) {
            assert avatar != null;
        }
        assert moveSpeed > 0f : moveSpeed;
        assert goals != null;
        assert goals.length > 0 : goals.length;
        for (NavVertex vertex : goals) {
            assert vertex != null;
        }

        this.avatars = avatars.clone();
        this.moveSpeed = moveSpeed;
        this.goals = goals.clone();

        setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Enable this state with the specified the active arc.
     *
     * @param arc arc to activate (not null)
     */
    void activate(NavArc arc) {
        assert arc != null;
        logger.log(Level.INFO, "arc={0}", arc);
        moveCount++;

        activeArc = arc;
        setEnabled(true);
    }

    /**
     * Read the move count.
     *
     * @return number of moves the player has made (&ge;0)
     */
    int getMoveCount() {
        return moveCount;
    }

    /**
     * Alter the player's goals.
     *
     * @param newGoals the player's new goals (not null, not empty, not altered,
     * all elements not null)
     */
    void setGoals(NavVertex[] newGoals) {
        assert newGoals != null;
        goals = newGoals.clone();
    }

    /**
     * Alter the movement rate.
     *
     * @param newSpeed (in world units per second, &gt;0)
     */
    void setSpeed(float newSpeed) {
        assert newSpeed > 0f : newSpeed;
        moveSpeed = newSpeed;
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this state prior to its first update.
     *
     * @param stateManager (not null)
     * @param application (not null)
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

        this.application = application;
    }

    /**
     * Update this state.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        Validate.nonNegative(elapsedTime, "interval");
        super.update(elapsedTime);

        NavVertex destinationVertex = activeArc.getToVertex();
        Vector3f destination = destinationVertex.getLocation();
        Vector3f location = avatars[0].getWorldTranslation();
        Vector3f offset = destination.subtract(location);
        float distance = offset.length();
        if (distance < epsilon) {
            completion(destinationVertex);
            return;
        }
        /*
         * Compute the player's new location.
         */
        float maxDistance = elapsedTime * moveSpeed;
        if (distance > maxDistance) {
            offset.multLocal(maxDistance / distance);
        }
        Vector3f newLocation = location.add(offset);
        /*
         * Translate each avatar to the new location.
         */
        for (Spatial avatar : avatars) {
            MySpatial.setWorldLocation(avatar, newLocation);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * The current move is complete.
     *
     * @param destinationVertex (not null)
     */
    private void completion(NavVertex destinationVertex) {
        assert destinationVertex != null;
        /*
         * Check whether the player has reached a goal.
         */
        for (NavVertex goalVertex : goals) {
            if (destinationVertex == goalVertex) {
                if (moveCount == 1) {
                    System.out.printf("You traversed the maze in one move!%n");
                } else {
                    System.out.printf("You traversed the maze in %d moves.%n",
                            moveCount);
                }
                setEnabled(false);
                application.stop();
                return;
            }
        }

        int numArcs = destinationVertex.getNumArcs();
        boolean autoTurn = numArcs <= maxArcsForAutoTurn;
        AppStateManager stateManager = application.getStateManager();
        if (autoTurn) {
            /*
             * Turn to whichever arc requires the least rotation.
             */
            Quaternion rotation = avatars[0].getWorldRotation();
            Vector3f direction = rotation.mult(forwardDirection);
            NavArc nextArc = destinationVertex.findLeastTurn(direction);
            direction = nextArc.getStartDirection();
            TurnState turnState = stateManager.getState(TurnState.class);
            turnState.activate(destinationVertex, direction);
        } else {
            /*
             * Activate the input state to wait for user input.
             */
            InputState inputState = stateManager.getState(InputState.class);
            inputState.activate(destinationVertex);
        }
        setEnabled(false);
    }
}