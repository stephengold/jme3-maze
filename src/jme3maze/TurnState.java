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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.navigation.NavNode;

/**
 * App state for camera rotation to a specified navigation arc.
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
     * attaching application: set by initialize()
     */
    private Application application = null;
    /**
     * turn rate of the camera node: set by constructor (in radians per second,
     * &gt;0)
     */
    private float turnRate;
    /**
     * active node: set by activate()
     */
    private NavNode activeNode = null;
    /**
     * the player's avatar: set by constructor (not null)
     */
    final private Spatial cameraNode;
    /**
     * final direction of turn: set by activate()
     */
    private Vector3f finalDirection = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled turn state with a specified camera node and turn
     * rate.
     *
     * @param cameraNode the player's avatar: set by constructor (not null)
     * @param turnRate turn rate of the camera node (in radians per second,
     * &gt;0)
     */
    TurnState(Node cameraNode, float turnRate) {
        assert cameraNode != null;
        assert turnRate > 0f : turnRate;

        this.cameraNode = cameraNode;
        this.turnRate = turnRate;
        setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Enable this state with the specified the navigation node and final
     * direction.
     *
     * @param node which navigation node (not null)
     * @param finalDirection direction when the turn is complete (not null)
     */
    void activate(NavNode node, Vector3f finalDirection) {
        assert node != null;
        assert finalDirection != null;
        assert finalDirection.isUnitVector() : finalDirection;
        logger.log(Level.INFO, "node={0}, direction={1}",
                new Object[]{node, finalDirection});

        activeNode = node;
        this.finalDirection = finalDirection.clone();
        setEnabled(true);
    }

    /**
     * Alter the turn rate.
     *
     * @param newRate (in radians per second, &gt;0)
     */
    void setSpeed(float newRate) {
        assert newRate > 0f : newRate;
        turnRate = newRate;
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
        this.application = application;
    }

    /**
     * Update this state.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        Validate.nonNegative(elapsedTime, "elapsed time");

        Quaternion orientation = MySpatial.getWorldOrientation(cameraNode);
        Vector3f direction = orientation.mult(Vector3f.UNIT_X);
        float dot = finalDirection.dot(direction);
        if (1f - dot < epsilon) {
            goInput();
            return;
        }

        Vector3f turnAxis = direction.cross(finalDirection);
        float length = turnAxis.length();
        if (length == 0f) {
            turnAxis = Vector3f.UNIT_Y;
        } else {
            turnAxis.normalizeLocal();
        }
        float turnAngle = FastMath.acos(dot); // positive angle
        float maxTurnAngle = elapsedTime * turnRate;
        if (turnAngle > maxTurnAngle) {
            turnAngle = maxTurnAngle;
        }
        logger.log(Level.INFO, "turnAngle={0}", turnAngle);

        Quaternion rotation = new Quaternion();
        rotation.fromAngleNormalAxis(turnAngle, turnAxis);
        orientation = rotation.mult(orientation);
        MySpatial.setWorldOrientation(cameraNode, orientation);
    }
    // *************************************************************************
    // private methods

    /**
     * The turn is complete; activate the input state.
     */
    private void goInput() {
        AppStateManager stateManager = application.getStateManager();
        InputState inputState = stateManager.getState(InputState.class);
        inputState.activate(activeNode);
        setEnabled(false);
        activeNode = null;
    }
}