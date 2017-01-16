/*
 Copyright (c) 2014-2017, Stephen Gold
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
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3maze.items.Item;
import jme3maze.items.Torch;
import jme3utilities.Validate;
import jme3utilities.math.VectorXZ;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Game app state to manage the player in the Maze Game. The player has a
 * position in the world and an inventory of items.
 * <p>
 * Enabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class PlayerState
        extends GameAppState {
    // *************************************************************************
    // constants

    /**
     * tolerance for comparing horizontal direction vectors
     */
    final private static float epsilon = 1e-4f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(PlayerState.class.getName());
    // *************************************************************************
    // fields
    /**
     * rate of movement in world units per second (&gt;0)
     */
    private float maxMoveSpeed = 25f;
    /**
     * rate of rotation in radians per second (&gt;0)
     */
    private float maxTurnRate = 2f;
    /*
     * which level of the maze this player is on (&ge;0)
     */
    private int mazeLevelIndex = 0;
    /*
     * number of moves made since the start of the game (&ge;0)
     */
    private int moveCount = 0;
    /**
     * item in the player's left hand
     */
    private Item leftHandItem = null;
    /**
     * item in the player's right hand
     */
    private Item rightHandItem = null;
    /**
     * arc which this player is on: set by constructor
     */
    private NavArc arc;
    /**
     * vertex which this player is trying to reach: set by setGoal()
     */
    private NavVertex goal;
    /**
     * vertex which this player is at: set by constructor
     */
    private NavVertex vertex;
    /**
     * orientation in world coordinates
     */
    final private Quaternion orientation = new Quaternion();
    /**
     * direction this player facing (in world coordinates, length=1)
     */
    private VectorXZ direction = VectorXZ.north;
    /**
     * location (world coordinates)
     */
    final private Vector3f location = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Find an arc which closely matches the player's location and direction.
     *
     * @return pre-existing instance, or null of no match
     */
    public NavArc advanceArc() {
        NavArc bestMatch = vertex.findLeastTurn(direction);
        VectorXZ horizontalDirection = bestMatch.getHorizontalDirection();
        float dot = direction.dot(horizontalDirection);
        if (1f - dot < epsilon) {
            return bestMatch;
        }
        return null;
    }

    /**
     * Remove the specified item from this player's inventory.
     *
     * @param item item to remove (not null)
     * @return true if successful, otherwise false
     */
    public boolean discard(Item item) {
        Validate.nonNull(item, "item");

        if (item == leftHandItem) {
            setLeftHandItem(null);
            return true;
        } else if (item == rightHandItem) {
            setRightHandItem(null);
            return true;
        }
        /*
         * item not found
         */
        return false;
    }

    /**
     * Find a particular class of item in this player's inventory.
     *
     * @param <T>
     * @param itemClass item class to search for
     * @return item found, otherwise null
     */
    public <T extends Item> T findItem(Class<T> itemClass) {
        if (leftHandItem != null
                && itemClass.isAssignableFrom(leftHandItem.getClass())) {
            @SuppressWarnings("unchecked")
            T result = (T) leftHandItem;
            return result;
        } else if (rightHandItem != null
                && itemClass.isAssignableFrom(rightHandItem.getClass())) {
            @SuppressWarnings("unchecked")
            T result = (T) rightHandItem;
            return result;
        }
        return null;
    }

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
     * Read this player's direction.
     *
     * @return unit vector in world coordinates
     */
    public VectorXZ getDirection() {
        assert direction != null;
        assert direction.isUnitVector() : direction;
        return direction;
    }

    /**
     * Read the item in this player's left hand.
     *
     * @return pre-existing instance or null if none
     */
    public Item getLeftHandItem() {
        return leftHandItem;
    }

    /**
     * Read the item in this player's right hand.
     *
     * @return pre-existing instance or null if none
     */
    public Item getRightHandItem() {
        return rightHandItem;
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
     * Access the maze level this player is on.
     *
     * @return pre-existing instance
     */
    public MazeLevel getMazeLevel() {
        MazeLevel level = worldState.getLevel(mazeLevelIndex);
        return level;
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
     * Test whether the player's left hand is empty.
     *
     * @return true if empty, false otherwise
     */
    public boolean isLeftHandEmpty() {
        boolean result = leftHandItem == null;
        return result;
    }

    /**
     * Test whether the player's right hand is empty.
     *
     * @return true if empty, false otherwise
     */
    public boolean isRightHandEmpty() {
        boolean result = rightHandItem == null;
        return result;
    }

    /**
     * Remove an item from the player's inventory.
     *
     * @param item item to remove (not null)
     * @return true if successful, false if item is not found
     */
    public boolean remove(Item item) {
        Validate.nonNull(item, "item");

        if (item == leftHandItem) {
            setLeftHandItem(null);
            return true;

        } else if (item == rightHandItem) {
            setRightHandItem(null);
            return true;
        }

        return false;
    }

    /**
     * Rotate this player around the +Y axis.
     *
     * @param rotation new direction for the current X-axis (length=1)
     */
    public void rotate(VectorXZ rotation) {
        Validate.nonNull(rotation, "rotation");
        if (!rotation.isUnitVector()) {
            logger.log(Level.SEVERE, "rotation={0}", rotation);
            throw new IllegalArgumentException(
                    "rotation should have length=1");
        }

        VectorXZ newDirection = direction.rotate(rotation);
        setDirection(newDirection);
    }

    /**
     * Find the shortest path from this player's location to their goal.
     *
     * @return 1st arc in the shortest path to the goal (or null if goal is
     * achieved or unreachable)
     */
    public NavArc seekGoal() {
        if (vertex == null || goal == null) {
            return null;
        }
        if (vertex == goal) {
            goal = null;
            return null;
        }
        NavArc result = worldState.getGraph().seek(vertex, goal);

        return result;
    }

    /**
     * Alter this player's current arc.
     *
     * @param newArc (not null)
     */
    final public void setArc(NavArc newArc) {
        Validate.nonNull(newArc, "arc");

        arc = newArc;
        NavVertex fromVertex = arc.getFromVertex();
        setVertex(fromVertex);

        VectorXZ horizontalDirection = arc.getHorizontalDirection();
        setDirection(horizontalDirection);
        /*
         * Update the map (if enabled and readable).
         */
        mapViewState.addMazeLineOfSight(vertex, direction);
    }

    /**
     * Alter this player's direction.
     *
     * @param newDirection world coordinates (not null, positive length)
     */
    public void setDirection(VectorXZ newDirection) {
        Validate.nonNull(newDirection, "direction");
        if (newDirection.isZeroLength()) {
            throw new IllegalArgumentException(
                    "direction should have positive length");
        }

        direction = newDirection.normalize();
        orientation.set(direction.toQuaternion());
        updateTorchLocation();
        /*
         * Visualize the rotation.
         */
        mainViewState.setPlayerOrientation(orientation);
        if (mapViewState.isEnabled()) {
            mapViewState.setPlayerOrientation(orientation);
        }
    }

    /**
     * Alter this player's goal.
     *
     * @param newGoal may be null for none
     */
    public void setGoal(NavVertex newGoal) {
        this.goal = newGoal;
    }

    /**
     * Alter what this player holds in their left hand.
     *
     * @param item may be null for none
     */
    public void setLeftHandItem(Item item) {
        if (item != null) {
            assert item != leftHandItem : item;
            assert item != rightHandItem : item;
        }

        leftHandItem = item;
        /*
         * Update the controller.
         */
        inputState.setLeftHandItem(item);
    }

    /**
     * Alter this player's location.
     *
     * @param newLocation world coordinates (not null, unaffected)
     */
    public void setLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "location");

        location.set(newLocation);
        updateTorchLocation();
        /*
         * Visualize the translation.
         */
        mainViewState.setPlayerLocation(location);
        if (mapViewState.isEnabled()) {
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
     * Alter what this player holds in their right hand.
     *
     * @param item may be null for none
     */
    public void setRightHandItem(Item item) {
        if (item != null) {
            assert item != leftHandItem : item;
            assert item != rightHandItem : item;
        }

        rightHandItem = item;
        /*
         * Update the controller.
         */
        inputState.setRightHandItem(item);
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

            mazeLevelIndex = WorldState.levelIndex(vertex);
            /*
             * Update the map (if enabled).
             */
            mapViewState.addMazeLineOfSight(vertex, direction);
        }
    }

    /**
     * Add the specified item to this player's inventory, in the left hand if
     * possible.
     *
     * @param item (not null)
     * @return true if successful, otherwise false
     */
    public boolean takeFavorLeftHand(Item item) {
        Validate.nonNull(item, "item");

        if (isLeftHandEmpty()) {
            setLeftHandItem(item);
            return true;
        } else if (isRightHandEmpty()) {
            setRightHandItem(item);
            return true;
        }
        return false;
    }

    /**
     * Add the specified item to this player's inventory, in the right hand if
     * possible.
     *
     * @param item (not null)
     * @return true if successful, otherwise false
     */
    public boolean takeFavorRightHand(Item item) {
        Validate.nonNull(item, "item");

        if (isRightHandEmpty()) {
            setRightHandItem(item);
            return true;
        } else if (isLeftHandEmpty()) {
            setLeftHandItem(item);
            return true;
        }
        return false;
    }

    /**
     * Update the location of the hand-held torch, if any.
     */
    public void updateTorchLocation() {
        Vector3f localOffset = null;
        if (leftHandItem instanceof Torch) {
            localOffset = new Vector3f(-2f, 7f, -3f);
        }
        if (rightHandItem instanceof Torch) {
            localOffset = new Vector3f(2f, 7f, -3f);
        }
        if (localOffset == null) {
            return;
        }

        Vector3f worldOffset = orientation.mult(localOffset);
        Vector3f torchLocation = location.add(worldOffset);
        worldState.setTorchLocation(torchLocation);
    }

    /**
     * Use the left-hand item.
     */
    public void useLeftHandItem() {
        if (!isLeftHandEmpty()) {
            leftHandItem.use(false);
        }
    }

    /**
     * Use the right-hand item.
     */
    public void useRightHandItem() {
        if (!isRightHandEmpty()) {
            rightHandItem.use(false);
        }
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
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);
        /*
         * Get the player's start arc.
         */
        NavArc playerStartArc = worldState.getStartArc();
        setArc(playerStartArc);
    }
}
