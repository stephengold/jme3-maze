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
import java.util.logging.Logger;
import jme3maze.items.Item;
import jme3maze.model.FreeItemsState;
import jme3maze.model.PlayerState;
import jme3utilities.Validate;
import jme3utilities.math.VectorXZ;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * App state to translate the player along the current navigation arc.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class MoveState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * tolerance for detecting completion of a move (in world units)
     */
    final private static float epsilon = 1e-3f;
    /**
     * maximum number of arcs per vertex for which the player turns
     * automatically after a move; 0 &rarr; disable automatic turning, &ge;1
     * &rarr; turn 180 at each cul-de-sac, &ge;2 &rarr; follow turns in
     * corridors
     */
    final private static int maxArcsForAutoTurn = 0;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MoveState.class.getName());
    // *************************************************************************
    // fields
    /**
     * app state manager: set by initialize()
     */
    private AppStateManager stateManager;
    /**
     *
     */
    private float distanceTravelled = 0f;
    /**
     * app state to manage free items: set by initialize()
     */
    private FreeItemsState freeItemsState;
    /**
     * app state to manage the player: set by initialize()
     */
    private PlayerState playerState;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled move state. After instantiation, the first method
     * invoked should be initialize().
     */
    MoveState() {
        setEnabled(false);
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
        freeItemsState = stateManager.getState(FreeItemsState.class);
        playerState = stateManager.getState(PlayerState.class);
    }

    /**
     * Enable or disable this state.
     *
     * @param newState true to enable, false to disable
     */
    @Override
    final public void setEnabled(boolean newState) {
        if (newState && !isEnabled()) {
            playerState.incrementMoveCount();
            distanceTravelled = 0f;
        }

        super.setEnabled(newState);
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

        NavArc arc = playerState.getArc();
        float arcLength = arc.getPathLength();
        float distanceRemaining = arcLength - distanceTravelled;
        if (distanceRemaining <= epsilon) {
            movementComplete(arc);
            return;
        }
        /*
         * Update the player's location on the arc.
         */
        float step = elapsedTime * playerState.getMaxMoveSpeed();
        if (step > distanceRemaining) {
            step = distanceRemaining;
        }
        distanceTravelled += step;
        Vector3f newLocation = arc.pathLocation(distanceTravelled);
        playerState.setLocation(newLocation);
        playerState.setVertex(null);
    }
    // *************************************************************************
    // private methods

    /**
     * The current move is complete.
     *
     * @param arc (not null)
     */
    private void movementComplete(NavArc arc) {
        setEnabled(false);
        NavVertex destinationVertex = arc.getToVertex();
        playerState.setVertex(destinationVertex);
        /*
         * Encounter any free items at the destination.
         */
        Item[] items = freeItemsState.getItems(destinationVertex);
        for (Item item : items) {
            item.encounter();
        }

        int numArcs = destinationVertex.getNumArcs();
        boolean autoTurn = numArcs <= maxArcsForAutoTurn;
        if (autoTurn) {
            /*
             * Turn to the arc which requires the least rotation.
             */
            VectorXZ direction = playerState.getDirection();
            NavArc nextArc = destinationVertex.findLeastTurn(direction);
            VectorXZ horizontalDirection = nextArc.getHorizontalDirection();
            TurnState turnState = stateManager.getState(TurnState.class);
            turnState.activate(horizontalDirection);

        } else {
            /*
             * Activate the input state and await user input.
             */
            InputState inputState = stateManager.getState(InputState.class);
            inputState.setEnabled(true);
        }
    }
}