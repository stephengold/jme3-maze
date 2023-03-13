/*
 Copyright (c) 2014-2023, Stephen Gold
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

import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3maze.items.Item;
import jme3utilities.math.ReadXZ;
import jme3utilities.math.VectorXZ;
import jme3utilities.math.spline.Spline3f;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Game app state to translate the player along the current navigation arc.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MoveState extends GameAppState {
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
    final private static Logger logger = Logger.getLogger(
            MoveState.class.getName());
    // *************************************************************************
    // fields

    /**
     * distance traveled from start of arc
     */
    private float distanceTraveled = 0f;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled move state. After instantiation, the 1st method
     * invoked should be initialize().
     */
    MoveState() {
        super(false);
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Enable or disable this state.
     *
     * @param newStatus true to enable, false to disable
     */
    @Override
    final public void setEnabled(boolean newStatus) {
        if (newStatus && !isEnabled()) {
            playerState.incrementMoveCount();
            this.distanceTraveled = 0f;
        }

        super.setEnabled(newStatus);
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

        NavArc arc = playerState.getArc();
        Spline3f path = worldState.getPath(arc);
        float arcLength = path.totalLength();
        float distanceRemaining = arcLength - distanceTraveled;
        if (distanceRemaining <= epsilon) {
            movementComplete(arc);
            return;
        }

        // Update the player's location on the arc.
        float step = elapsedTime * playerState.getMaxMoveSpeed();
        if (step > distanceRemaining) {
            step = distanceRemaining;
        }
        this.distanceTraveled += step;
        Vector3f newLocation = path.interpolate(distanceTraveled);
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

        // Encounter any free items at the destination.
        Item[] items = freeItemsState.getItems(destinationVertex);
        for (Item item : items) {
            item.encounter();
        }

        int numArcs = destinationVertex.numOutgoing();
        boolean autoTurn = numArcs <= maxArcsForAutoTurn;
        if (autoTurn) {
            // Turn to the arc which requires the least rotation.
            ReadXZ direction = playerState.getDirection();
            NavArc nextArc = destinationVertex.findOutgoing(direction, -2.0);
            VectorXZ horizontalDirection = nextArc.horizontalOffset();
            turnState.activate(horizontalDirection);

        } else {
            // Activate the input state and await player input.
            inputState.setEnabled(true);
        }
    }
}
