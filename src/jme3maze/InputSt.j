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
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import java.util.Collection;
import java.util.logging.Logger;
import jme3maze.items.Item;
import jme3maze.view.MainViewState;
import jme3maze.view.MapViewState;
import jme3utilities.Validate;
import jme3utilities.math.VectorXZ;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;
import tonegod.gui.controls.buttons.Button;
import tonegod.gui.controls.buttons.ButtonAdapter;
import tonegod.gui.controls.menuing.Menu;
import tonegod.gui.core.Element;
import tonegod.gui.core.Screen;
import tonegod.gui.core.utils.UIDUtil;

/**
 * App state for getting player input, enabled when the player is stationary.
 * Input is ignored while the player is turning. Input during player moves is
 * processed when the state gets re-enabled, in other words, when the player
 * reaches the destination vertex.
 * <p>
 * Disabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class InputState
        extends GameAppState
        implements ActionListener, RawInputListener {
    // *************************************************************************
    // constants

    /**
     * tolerance for comparing direction vectors
     */
    final private static float epsilon = 1e-4f;
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
     * action string for using the left-hand item
     */
    final private static String useLeftHandItemActionString =
            "use leftHandItem";
    /**
     * action string for using the right-hand item
     */
    final private static String useRightHandItemActionString =
            "use rightHandItem";
    // *************************************************************************
    // fields
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
     *
     * @param player player model instance (not null)
     */
    InputState() {
        setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Select use for the specified item from a popup menu.
     *
     * @param item (not null)
     */
    public void selectUse(final Item item) {
        Vector2f cursorPosition = inputManager.getCursorPosition();
        Vector2f upperLeft = cursorPosition.clone();
        boolean scrollable = false;
        Menu popupMenu = new Menu(guiScreen, upperLeft, scrollable) {
            @Override
            public void onMenuItemClicked(int index, Object value, boolean isToggled) {
                String description = (String) value;
                item.use(description);
            }
        };
        guiScreen.addElement(popupMenu);

        Collection<String> uses = item.findUses(false);
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
     * Alter what item is in the player's left hand.
     *
     * @param newItem may be null
     */
    public void setLeftHandItem(Item newItem) {
        if (leftHandElement != null) {
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
     * Alter what item is in the player's right hand.
     *
     * @param newItem may be null
     */
    public void setRightHandItem(Item newItem) {
        if (rightHandElement != null) {
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
     * Clean up this state on detach.
     */
    @Override
    public void cleanup() {
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

        super.cleanup();
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
        super.initialize(stateManager, application);

        initializeHotkeys();
        /*
         * Initialize the GUI.
         */
        guiScreen = new Screen(application);
        guiScreen.setUseToolTips(true);
        guiNode.addControl(guiScreen);
        initializeGuiButtons();
    }

    /**
     * Update this state by processing the last action string.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        VectorXZ direction = playerState.getDirection();
        /*
         * Process the most recent action string.
         */
        switch (lastActionString) {
            case advanceActionString:
                /*
                 * Pick the most promising arc.
                 */
                NavVertex vertex = playerState.getVertex();
                NavArc arc = vertex.findLeastTurn(direction);
                VectorXZ horizontalDirection = arc.getHorizontalDirection();
                float dot = direction.dot(horizontalDirection);
                if (1f - dot < epsilon) {
                    /*
                     * The player's direction closely matches the
                     * horizontal direction of an arc, so follow that arc.
                     */
                    goMove(arc);
                }
                break;

            case leftActionString:
                /*
                 * Turn the player to the left (90 degrees CCW).
                 */
                direction = direction.rotate(-FastMath.HALF_PI);
                goTurn(direction);
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
         * Ignore actions requested while the player is turning.
         */
        if (turnState.isEnabled()) {
            return;
        }

        lastActionString = actionString;
    }
    // *************************************************************************
    // RawInputListener methods

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    /**
     * Callback for joystick motion events.
     *
     * @param event (not null)
     */
    @Override
    public void onJoyAxisEvent(JoyAxisEvent event) {
    }

    /**
     * Callback for joystick button events.
     *
     * @param event (not null)
     */
    @Override
    public void onJoyButtonEvent(JoyButtonEvent event) {
    }

    /**
     * Callback for mouse motion events.
     *
     * @param event (not null)
     */
    @Override
    public void onMouseMotionEvent(MouseMotionEvent event) {
    }

    /**
     * Callback for mouse button events.
     *
     * @param event (not null)
     */
    @Override
    public void onMouseButtonEvent(MouseButtonEvent event) {
        if (event.getButtonIndex() == 0 && event.isPressed()) {
            Vector2f click2d = inputManager.getCursorPosition();
            MapViewState mapView = stateManager.getState(MapViewState.class);
            NavVertex vertex = null;//mapView.findVertex(click2d);
            if (vertex != null) {
            }
            MainViewState mainView = stateManager.getState(MainViewState.class);
            //Item item = mainView.findItem(click2d);
        }
    }

    /**
     * Callback for keyboard events.
     *
     * @param event (not null)
     */
    @Override
    public void onKeyEvent(KeyInputEvent event) {
    }

    /**
     * Callback for touch screen events.
     *
     * @param event (not null)
     */
    @Override
    public void onTouchEvent(TouchEvent event) {
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
                onAction(actionString, true, 0f);
            }
        };

        guiScreen.addElement(button);
        if (tipText != null) {
            String toolTipText = String.format("click to %s", tipText);
            button.setToolTipText(toolTipText);
        }

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
        addActionButton(upperLeft, size, actionString, iconAssetPath, tipText);

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

    /**
     * Map hotkeys to actions.
     */
    private void initializeHotkeys() {
        /*
         * Map hotkeys to action strings.
         */
        KeyTrigger aTrigger = new KeyTrigger(KeyInput.KEY_A);
        KeyTrigger leftTrigger = new KeyTrigger(KeyInput.KEY_LEFT);
        inputManager.addMapping(leftActionString, aTrigger, leftTrigger);

        KeyTrigger sTrigger = new KeyTrigger(KeyInput.KEY_S);
        KeyTrigger upTrigger = new KeyTrigger(KeyInput.KEY_UP);
        KeyTrigger wTrigger = new KeyTrigger(KeyInput.KEY_W);
        inputManager.addMapping(advanceActionString, sTrigger, upTrigger,
                wTrigger);

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
}