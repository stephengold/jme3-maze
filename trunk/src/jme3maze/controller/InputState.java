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
package jme3maze.controller;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import java.util.Collection;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3maze.items.Item;
import jme3utilities.Validate;
import jme3utilities.math.VectorXZ;
import jme3utilities.navigation.NavArc;
import tonegod.gui.controls.buttons.Button;
import tonegod.gui.controls.buttons.ButtonAdapter;
import tonegod.gui.controls.menuing.Menu;
import tonegod.gui.core.Element;
import tonegod.gui.core.Screen;
import tonegod.gui.core.utils.UIDUtil;

/**
 * Game app state to process action strings, enabled when the player is
 * stationary. Action strings received during player moves are processed when
 * the state gets re-enabled, in other words, when the player reaches the
 * destination vertex.
 * <p>
 * Disabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class InputState
        extends GameAppState {
    // *************************************************************************
    // constants

    /**
     * size of inventory icons (as a fraction of the screen's height)
     */
    final private static float inventoryIconSize = 0.15f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(InputState.class.getName());
    /**
     * action string for following the current arc
     */
    final public static String advanceActionString = "advance";
    /**
     * action string for turning one arc to the left
     */
    final public static String leftActionString = "left";
    /**
     * action string for resetting the cameras
     */
    final public static String resetActionString = "reset";
    /**
     * action string for turning one arc to the right
     */
    final public static String rightActionString = "right";
    /**
     * action string for using the left-hand item
     */
    final public static String useLeftHandItemActionString =
            "use leftHandItem";
    /**
     * action string for using the right-hand item
     */
    final public static String useRightHandItemActionString =
            "use rightHandItem";
    // *************************************************************************
    // fields
    /**
     * GUI element for the advance button
     */
    private Element advanceElement = null;
    /**
     * GUI element for the left hand's inventory
     */
    private Element leftHandElement = null;
    /**
     * GUI element for the right hand's inventory
     */
    private Element rightHandElement = null;
    /**
     * GUI screen: set by initialize()
     */
    private Screen guiScreen;
    /**
     * name of the most recent action
     */
    private String lastActionString = "";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled input state.
     */
    public InputState() {
        setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Select a use for the specified item from a popup menu.
     *
     * @param item (not null)
     * @param freeFlag true if item is free, false if it's in an inventory
     */
    public void selectUse(final Item item, boolean freeFlag) {
        Vector2f cursorPosition = inputManager.getCursorPosition();
        Vector2f upperLeft = cursorPosition.clone();
        boolean scrollable = false;
        Menu popupMenu = new Menu(guiScreen, upperLeft, scrollable) {
            @Override
            public void onMenuItemClicked(int index, Object value,
                    boolean isToggled) {
                String description = (String) value;
                item.use(description);
            }
        };
        guiScreen.addElement(popupMenu);

        Collection<String> uses = item.findUses(freeFlag);
        for (String use : uses) {
            String caption = use;
            Object value = use;
            Menu subMenu = null;
            popupMenu.addMenuItem(caption, value, subMenu);
        }

        Menu caller = null;
        popupMenu.showMenu(caller, cursorPosition.x, cursorPosition.y);
    }

    /**
     * Accept an action to be performed on the next update.
     *
     * @param actionString textual description of the action (not null)
     */
    public void setAction(String actionString) {
        Validate.nonNull(actionString, "action string");
        lastActionString = actionString;
    }

    /**
     * Alter which item is in the player's left hand.
     *
     * @param newItem may be null
     */
    public void setLeftHandItem(Item newItem) {
        if (leftHandElement != null) {
            leftHandElement.setToolTipText(null); // TODO
            guiScreen.removeElement(leftHandElement);
        }
        if (newItem != null) {
            Vector2f lowerLeft = descale(0.05f, 0.95f);
            float size = inventoryIconSize * guiScreen.getHeight();
            Vector2f dimensions = new Vector2f(size, size);
            Vector2f offset = new Vector2f(0f, -1f).multLocal(dimensions);
            Vector2f upperLeft = lowerLeft.add(offset);
            String assetPath = newItem.visualizeInventory();
            boolean freeFlag = false;
            String tipText = newItem.describeUse(freeFlag);
            leftHandElement = addActionButton(upperLeft, dimensions,
                    useLeftHandItemActionString, assetPath, tipText);
        }
    }

    /**
     * Alter which item is under the mouse pointer.
     *
     * @param newItem may be null
     * @param freeFlag if newItem != null, true &rarr; free item, false &rarr;
     * inventory item
     */
    public void setMouseItem(Item newItem, boolean freeFlag) {
        /*
         * Update the GUI's "forced" tool tip.
         */
        if (newItem == null) {
            guiScreen.releaseForcedToolTip();
        } else {
            String text = newItem.describeUse(freeFlag);
            guiScreen.setForcedToolTip(text);
        }
    }

    /**
     * Alter which item is in the player's right hand.
     *
     * @param newItem may be null
     */
    public void setRightHandItem(Item newItem) {
        if (rightHandElement != null) {
            rightHandElement.setToolTipText(null); // TODO
            guiScreen.removeElement(rightHandElement);
        }
        if (newItem != null) {
            Vector2f lowerRight = descale(0.95f, 0.95f);
            float size = inventoryIconSize * guiScreen.getHeight();
            Vector2f dimensions = new Vector2f(size, size);
            Vector2f offset = new Vector2f(-1f, -1f).multLocal(dimensions);
            Vector2f upperLeft = lowerRight.add(offset);
            String assetPath = newItem.visualizeInventory();
            boolean freeFlag = false;
            String tipText = newItem.describeUse(freeFlag);
            rightHandElement = addActionButton(upperLeft, dimensions,
                    useRightHandItemActionString, assetPath, tipText);
        }
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Enable or disable this state.
     *
     * @param newState true to enable, false to disable
     */
    @Override
    final public void setEnabled(boolean newState) {
        if (newState && !isEnabled()) {
            NavArc advanceArc = playerState.advanceArc();
            boolean canAdvance = advanceArc != null;
            if (canAdvance) { // TODO
                advanceElement.setToolTipText("advance");
            } else {
                advanceElement.setToolTipText(null);
            }
            advanceElement.setIsVisible(canAdvance);
        }

        super.setEnabled(newState);
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
         * Initialize the GUI.
         */
        guiScreen = new Screen(application);
        guiScreen.setUseToolTips(true);
        guiNode.addControl(guiScreen);
        initializeGuiButtons();
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Update this state by processing the last action string.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        NavArc advanceArc = playerState.advanceArc();
        VectorXZ direction = playerState.getDirection();
        /*
         * Process the most recent action string.
         */
        switch (lastActionString) {
            case advanceActionString:
                /*
                 * Attempt to advance the player along a forward arc.
                 */
                if (advanceArc != null) {
                    goMove(advanceArc);
                }
                break;

            case leftActionString:
                /*
                 * Turn the player to the left (90 degrees CCW).
                 */
                direction = direction.rotate(-FastMath.HALF_PI);
                goTurn(direction);
                break;

            case resetActionString:
                /*
                 * Reset the cameras.
                 */
                analogInputState.resetCameras();
                break;

            case rightActionString:
                /*
                 * Turn the player to the right (90 degrees CW).
                 */
                direction = direction.rotate(FastMath.HALF_PI);
                goTurn(direction);
                break;

            case useLeftHandItemActionString:
                /*
                 * Attempt to use the item in the player's left hand.
                 */
                playerState.useLeftHandItem();
                break;

            case useRightHandItemActionString:
                /*
                 * Attempt to use the item in the player's right hand.
                 */
                playerState.useRightHandItem();
                break;

            default:
                NavArc nextArc = playerState.seekGoal();
                if (nextArc == null) {
                    break;
                } else if (nextArc == advanceArc) {
                    goMove(advanceArc);
                } else {
                    direction = nextArc.getHorizontalDirection();
                    goTurn(direction);
                }
        }

        lastActionString = "";
    }
    // *************************************************************************
    // private methods

    /**
     * Add a new unpadded GUI button which generates actions.
     *
     * @param upperLeft screen coordinates of the button's upper left corner (in
     * pixels, not null)
     * @param size length and width of the button (in pixels, not null)
     * @param actionString action string to generate on a click (not null)
     * @param iconAssetPath asset path to icon texture (not null)
     * @param describeUse action description for the tool tip (or null for none)
     * @return new instance
     */
    private Button addActionButton(Vector2f upperLeft, Vector2f size,
            final String actionString, String iconAssetPath, String tipText) {
        assert upperLeft != null;
        assert size != null;
        assert actionString != null;
        assert iconAssetPath != null;

        String buttonUID = UIDUtil.getUID();
        Vector4f padding = new Vector4f(0f, 0f, 0f, 0f);
        Button button = new ButtonAdapter(guiScreen, buttonUID, upperLeft,
                size, padding, iconAssetPath) {
            @Override
            public void onButtonMouseLeftUp(MouseButtonEvent e, boolean t) {
                setAction(actionString);
            }
        };

        guiScreen.addElement(button);
        button.setToolTipText(tipText);

        return button;
    }

    /**
     * Convert fractional coordinates to GUI screen coordinates.
     *
     * @param xFraction fraction of the screen width
     * @param yFraction fraction of the screen height
     * @return new instance
     */
    private Vector2f descale(float xFraction, float yFraction) {
        float x = xFraction * guiScreen.getWidth();
        float y = yFraction * guiScreen.getHeight();
        Vector2f result = new Vector2f(x, y);

        return result;
    }

    /**
     * Activate the move state for a specified arc.
     *
     * @param arc not null
     */
    private void goMove(NavArc arc) {
        assert arc != null;

        setEnabled(false);
        playerState.setArc(arc);
        moveState.setEnabled(true);
    }

    /**
     * Activate the turn state for a specified direction.
     *
     * @param newDirection (length=1)
     */
    private void goTurn(VectorXZ newDirection) {
        assert newDirection != null;
        assert newDirection.isUnitVector() : newDirection;

        setEnabled(false);
        turnState.activate(newDirection);
    }

    /**
     * Create GUI buttons and map them to actions.
     */
    private void initializeGuiButtons() {
        Vector2f size = descale(0.15f, 0.15f);
        Vector2f offset = new Vector2f(0.5f, 1f).multLocal(size);
        Vector2f bottomCenter = descale(0.5f, 0.95f);
        Vector2f upperLeft = bottomCenter.subtract(offset);
        String actionString = advanceActionString;
        String iconAssetPath = "Textures/buttons/advance.png";
        String tipText = "advance";
        advanceElement = addActionButton(upperLeft, size, actionString,
                iconAssetPath, tipText);

        size = descale(0.1f, 0.1f);
        offset = new Vector2f(0.5f, 1f).multLocal(size);
        bottomCenter = descale(0.35f, 0.95f);
        upperLeft = bottomCenter.subtract(offset);
        iconAssetPath = "Textures/buttons/left.png";
        actionString = leftActionString;
        tipText = "turn left";
        addActionButton(upperLeft, size, actionString, iconAssetPath, tipText);

        bottomCenter = descale(0.65f, 0.95f);
        upperLeft = bottomCenter.subtract(offset);
        iconAssetPath = "Textures/buttons/right.png";
        actionString = rightActionString;
        tipText = "turn right";
        addActionButton(upperLeft, size, actionString, iconAssetPath, tipText);
    }
}
