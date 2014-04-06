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
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Application state to translate the player's avatars on a constant-velocity
 * trajectory until they reach the end of a specified navigation arc.
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
    // *************************************************************************
    // fields
    /**
     * attaching application: set by initialize()
     */
    private Application application;
    /**
     * active arc: set by activate()
     */
    private NavArc activeArc;
    /**
     * player model instance: set by constructor (not null)
     */
    final private PlayerModel player;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled move state for the specified player.
     *
     * @param player player model instance (not null)
     */
    MoveState(PlayerModel player) {
        assert player != null;
        this.player = player;
        setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Enable this state with the specified active arc.
     *
     * @param arc arc to activate (not null)
     */
    void activate(NavArc arc) {
        assert arc != null;
        logger.log(Level.INFO, "arc={0}", arc);
        player.startMove();

        activeArc = arc;
        setEnabled(true);
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
        assert isEnabled();
        super.update(elapsedTime);

        Vector3f location = player.getLocation();
        NavVertex destinationVertex = activeArc.getToVertex();
        Vector3f destination = destinationVertex.getLocation();
        Vector3f offset = destination.subtract(location);
        float distance = offset.length();
        if (distance <= epsilon) {
            player.setLocation(destination);
            movementComplete(destinationVertex);
            return;
        }
        /*
         * Compute the player's new location.
         */
        float maxDistance = elapsedTime * player.getMoveSpeed();
        if (distance > maxDistance) {
            offset.multLocal(maxDistance / distance);
        }
        Vector3f newLocation = location.add(offset);
        player.setLocation(newLocation);
    }
    // *************************************************************************
    // private methods

    /**
     * The current movement is complete.
     *
     * @param destinationVertex (not null)
     */
    private void movementComplete(NavVertex destinationVertex) {
        assert destinationVertex != null;

        setEnabled(false);
        /*
         * Check whether the player has reached a goal.
         */
        if (player.isGoal(destinationVertex)) {
            int moveCount = player.getMoveCount();
            if (moveCount == 1) {
                System.out.printf("You traversed the maze in one move!%n");
            } else {
                System.out.printf("You traversed the maze in %d moves.%n",
                        moveCount);
            }
            application.stop();
            return;
        }

        int numArcs = destinationVertex.getNumArcs();
        boolean autoTurn = numArcs <= maxArcsForAutoTurn;
        AppStateManager stateManager = application.getStateManager();
        if (autoTurn) {
            /*
             * Turn to whichever arc requires the least rotation.
             */
            Vector3f direction = player.getDirection();
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
    }
}