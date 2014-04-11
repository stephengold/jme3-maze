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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.model.PlayerState;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * App state to rotate the player at a constant angular rate to a specified
 * direction.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class TurnState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * tolerance for detecting completion of turn
     */
    final private static float epsilon = 1e-5f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(TurnState.class.getName());
    // *************************************************************************
    // fields
    /**
     * app state for getting player input: set by initialize()
     */
    private InputState inputState;
    /**
     * app state to manage the player: set by initialize()
     */
    private PlayerState playerState;
    /**
     * final direction of turn: set by activate()
     */
    private Vector3f finalDirection;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled turn state.
     */
    TurnState() {
        setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Enable this state with the specified final direction.
     *
     * @param finalDirection player's direction when the turn is complete
     * (length=1, unaffected)
     */
    void activate(Vector3f finalDirection) {
        assert finalDirection != null;
        assert finalDirection.isUnitVector() : finalDirection;
        logger.log(Level.INFO, "finalDirection={0}", finalDirection);

        this.finalDirection = finalDirection.clone();
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

        inputState = stateManager.getState(InputState.class);
        playerState = stateManager.getState(PlayerState.class);
        /*
         * Activate the "turn" app state.
         */
        Vector3f direction = playerState.getArc().getStartDirection();
        activate(direction);
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

        Vector3f direction = playerState.getDirection();
        float dot = finalDirection.dot(direction);
        if (1f - dot < epsilon) {
            turnComplete();
            return;
        }

        Vector3f axis = direction.cross(finalDirection);
        if (MyVector3f.isZeroLength(axis)) {
            axis = Vector3f.UNIT_Y;
        } else {
            axis.normalizeLocal();
        }
        float angle = FastMath.acos(dot); // positive angle
        float maxTurnAngle = elapsedTime * playerState.getMaxTurnRate();
        if (angle > maxTurnAngle) {
            angle = maxTurnAngle;
        }
        assert angle > 0f : angle;
        logger.log(Level.INFO, "turnAngle={0}", angle);
        playerState.rotate(angle, axis);
    }
    // *************************************************************************
    // private methods

    /**
     * The current turn is complete.
     */
    private void turnComplete() {
        setEnabled(false);

        NavVertex vertex = playerState.getVertex();
        NavArc arc = vertex.findLeastTurn(finalDirection);
        Vector3f arcDirection = arc.getStartDirection();
        float dot = finalDirection.dot(arcDirection);
        if (1f - dot < epsilon) {
            playerState.setArc(arc);
        } else {
            playerState.setDirection(finalDirection);
        }
        inputState.setEnabled(true);
    }
}