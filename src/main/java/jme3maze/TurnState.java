/*
 Copyright (c) 2014-2023 Stephen Gold

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3maze;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.math.ReadXZ;
import jme3utilities.math.VectorXZ;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Game app state to rotate the player at a constant angular rate to a specified
 * direction.
 * <p>
 * Disabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TurnState extends GameAppState {
    // *************************************************************************
    // constants

    /**
     * tolerance for detecting completion of turn
     */
    final private static float epsilon = 1e-3f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TurnState.class.getName());
    // *************************************************************************
    // fields

    /**
     * final direction of turn: set by activate()
     */
    private ReadXZ finalDirection;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled turn state.
     */
    TurnState() {
        super(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Enable this state with the specified final direction.
     *
     * @param finalDirection player's direction when the turn is complete
     * (length&gt;0)
     */
    public void activate(ReadXZ finalDirection) {
        VectorXZ.validateNonZero(finalDirection, "direction");

        logger.log(Level.INFO, "finalDirection={0}", finalDirection);

        ReadXZ norm = finalDirection.normalize();
        this.finalDirection = norm;
        setEnabled(true);
    }
    // *************************************************************************
    // GameAppState methods

    /**
     * Initialize this state prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application attaching application (not null)
     */
    @Override
    public void initialize(
            AppStateManager stateManager, Application application) {
        super.initialize(stateManager, application);

        // Activate the player's current arc.
        NavArc arc = playerState.getArc();
        VectorXZ horizontalDirection = arc.horizontalOffset();
        activate(horizontalDirection);
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Update this state.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        ReadXZ direction = playerState.getDirection();
        float directionError = direction.directionError(finalDirection);
        if (FastMath.abs(directionError) < epsilon) {
            turnComplete();
            return;
        }
        ReadXZ conjugate = direction.mirrorZ();
        ReadXZ rotation = finalDirection.mult(conjugate);
        float maxTurnAngle = elapsedTime * playerState.getMaxTurnRate();
        if (maxTurnAngle > FastMath.PI) {
            maxTurnAngle = FastMath.PI;
        }
        ReadXZ limitedRotation = rotation.clampDirection(maxTurnAngle);
        playerState.rotate(limitedRotation);
    }
    // *************************************************************************
    // private methods

    /**
     * The current rotation is complete. Switch to the input appstate.
     */
    private void turnComplete() {
        setEnabled(false);

        NavVertex vertex = playerState.getVertex();
        NavArc arc = vertex.findOutgoing(finalDirection, 1.0 - epsilon);
        if (arc != null) {
            playerState.setArc(arc);
        } else {
            playerState.setDirection(finalDirection);
        }
        inputState.setEnabled(true);
    }
}
