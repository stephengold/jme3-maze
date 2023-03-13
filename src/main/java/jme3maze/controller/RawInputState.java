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
package jme3maze.controller;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector2f;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3maze.items.Item;
import jme3utilities.navigation.NavVertex;

/**
 * Game app state to handle raw input (such as mouse buttons) in explore mode.
 * <p>
 * Enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RawInputState
        extends GameAppState
        implements RawInputListener {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            RawInputState.class.getName());
    // *************************************************************************
    // fields
    
    /**
     * track status of middle mouse button
     */
    private boolean middleMouseButtonPressed = false;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, enabled RawInputState.
     */
    public RawInputState() {
        super(true);
    }    
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the middle mouse button is pressed.
     *
     * @return true if pressed, false if released
     */
    public boolean isMiddleButtonPressed() {
        return middleMouseButtonPressed;
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
        inputManager.addRawInputListener(this);
    }
    // *************************************************************************
    // RawInputListener methods

    /**
     * Callback before each batch of input.
     */
    @Override
    public void beginInput() {
    }

    /**
     * Callback after each batch of input.
     */
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
     * Callback for keyboard events.
     *
     * @param event (not null)
     */
    @Override
    public void onKeyEvent(KeyInputEvent event) {
    }

    /**
     * Callback for mouse button events.
     *
     * @param event (not null)
     */
    @Override
    public void onMouseButtonEvent(MouseButtonEvent event) {
        if (event.getButtonIndex() == MouseInput.BUTTON_LEFT
                && event.isPressed()) {
            float x = event.getX();
            float y = event.getY();
            Vector2f click2d = new Vector2f(x, y);
            /*
             * If the cursor's hotspot was over a vertex in the map view, then
             * make that vertex be the player's goal.
             */
            NavVertex vertex = mapViewState.findVertex(click2d);
            if (vertex != null) {
                playerState.setGoal(vertex);
                return;
            }
            /*
             * If the cursor's hotspot was over an item in the main view, then
             * use that item.
             */
            Item item = mainViewState.findItem(click2d);
            if (item != null) {
                item.use(true);
            }

        } else if (event.getButtonIndex() == MouseInput.BUTTON_MIDDLE) {
            this.middleMouseButtonPressed = event.isPressed();
        }
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
     * Callback for touch screen events.
     *
     * @param event (not null)
     */
    @Override
    public void onTouchEvent(TouchEvent event) {
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Clean up this state on detach.
     */
    @Override
    public void cleanup() {
        /*
         * Unregister this event listener.
         */
        inputManager.removeRawInputListener(this);

        super.cleanup();
    }

    /**
     * Update this state.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);
        /*
         * Check whether the mouse cursor is over a free item and update the
         * input state accordingly.
         */
        Vector2f click2d = inputManager.getCursorPosition();
        Item item = mainViewState.findItem(click2d);
        boolean freeFlag = true;
        inputState.setMouseItem(item, freeFlag);
    }
}