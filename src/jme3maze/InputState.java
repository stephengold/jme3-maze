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
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3maze.model.Player;
import jme3utilities.Validate;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * The application state for getting player input, enabled when the player is
 * stationary. Input is ignored while the player is turning. Input during player
 * moves is processed when the state is re-enabled, in other words, when the
 * player reaches the destination vertex.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class InputState
        extends AbstractAppState
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * tolerance for comparing direction vectors
     */
    final private static float epsilon = 1e-4f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(InputState.class.getName());
    /**
     * action string for following the current arc
     */
    final private static String advanceActionString = "advance";
    /**
     * action string for turning one arc to the left
     */
    final private static String leftActionString = "left";
    /**
     * action string for turning one arc to the right
     */
    final private static String rightActionString = "right";
    /**
     * the avatar's left direction in local coordinates
     */
    final private static Vector3f leftDirection = Vector3f.UNIT_X;
    /**
     * the avatar's right direction in local coordinates
     */
    final private static Vector3f rightDirection = new Vector3f(-1f, 0f, 0f);
    // *************************************************************************
    // fields
    /**
     * attaching application: set by initialize()
     */
    private MazeGame application = null;
    /**
     * most recent action string
     */
    private String lastActionString = "";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled input state.
     *
     * @param player player model instance (not null)
     */
    InputState() {
        setEnabled(false);
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Clean up this state on detach.
     */
    @Override
    public void cleanup() {
        super.cleanup();
        InputManager inputManager = application.getInputManager();
        /*
         * Unmap action strings.
         */
        if (inputManager.hasMapping(advanceActionString)) {
            inputManager.deleteMapping(advanceActionString);
        }
        if (inputManager.hasMapping(leftActionString)) {
            inputManager.deleteMapping(leftActionString);
        }
        if (inputManager.hasMapping(rightActionString)) {
            inputManager.deleteMapping(rightActionString);
        }
        /*
         * Unregister this event listener.
         */
        inputManager.removeListener(this);
    }

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
        this.application = (MazeGame) application;
        InputManager inputManager = application.getInputManager();
        /*
         * Map keys to action strings.
         */
        KeyTrigger aTrigger = new KeyTrigger(KeyInput.KEY_A);
        KeyTrigger leftTrigger = new KeyTrigger(KeyInput.KEY_LEFT);
        inputManager.addMapping(leftActionString, aTrigger, leftTrigger);

        KeyTrigger sTrigger = new KeyTrigger(KeyInput.KEY_S);
        KeyTrigger upTrigger = new KeyTrigger(KeyInput.KEY_UP);
        KeyTrigger wTrigger = new KeyTrigger(KeyInput.KEY_W);
        inputManager.addMapping(advanceActionString,
                sTrigger, upTrigger, wTrigger);

        KeyTrigger dTrigger = new KeyTrigger(KeyInput.KEY_D);
        KeyTrigger rightTrigger = new KeyTrigger(KeyInput.KEY_RIGHT);
        inputManager.addMapping(rightActionString, dTrigger, rightTrigger);
        /*
         * Register listeners for the action strings.
         */
        inputManager.addListener(this, advanceActionString);
        inputManager.addListener(this, leftActionString);
        inputManager.addListener(this, rightActionString);
    }

    /**
     * Update this state by processing the last action string.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        Validate.nonNegative(elapsedTime, "interval");
        assert isEnabled();
        super.update(elapsedTime);

        Player player = application.getWorld().getPlayer();
        Quaternion orientation = player.getOrientation();
        Vector3f direction;
        /*
         * Process the most recent action string.
         */
        switch (lastActionString) {
            case advanceActionString:
                NavVertex vertex = player.getVertex();
                direction = player.getDirection();
                NavArc arc = vertex.findLeastTurn(direction);
                Vector3f arcDirection = arc.getStartDirection();
                float dot = direction.dot(arcDirection);
                if (1f - dot < epsilon) {
                    /*
                     * The avatar's direction closely matches an arc, so
                     * follow that arc.
                     */
                    goMove(arc);
                }
                break;

            case leftActionString:
                /*
                 * Turn the player 90 degrees to the left;
                 */
                direction = orientation.mult(leftDirection);
                goTurn(direction);
                break;

            case rightActionString:
                /*
                 * Turn the player 90 degrees to the right;
                 */
                direction = orientation.mult(rightDirection);
                goTurn(direction);
                break;

            default:
                break;
        }

        lastActionString = "";
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Record an action from the keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        Validate.nonNull(actionString, "action string");
        /*
         * Ignore actions which are not ongoing.
         */
        if (!ongoing) {
            return;
        }
        /*
         * Ignore actions requested while the avatar is turning.
         */
        AppStateManager stateManager = application.getStateManager();
        TurnState turnState = stateManager.getState(TurnState.class);
        if (turnState.isEnabled()) {
            return;
        }
        lastActionString = actionString;
    }
    // *************************************************************************
    // private methods

    /**
     * Activate the move state for a specified arc.
     *
     * @param arc not null
     */
    private void goMove(NavArc arc) {
        assert arc != null;

        setEnabled(false);

        application.getWorld().getPlayer().setArc(arc);

        AppStateManager stateManager = application.getStateManager();
        MoveState moveState = stateManager.getState(MoveState.class);
        moveState.setEnabled(true);
    }

    /**
     * Activate the turn state for a specified direction.
     *
     * @param newDirection unit vector (unaffected)
     */
    private void goTurn(Vector3f newDirection) {
        assert newDirection != null;
        assert newDirection.isUnitVector() : newDirection;

        setEnabled(false);

        AppStateManager stateManager = application.getStateManager();
        TurnState turnState = stateManager.getState(TurnState.class);
        turnState.activate(newDirection);
    }
}