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
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * Game appstate to handle analog input in explore mode.
 * <p>
 * Enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AnalogInputState
        extends GameAppState
        implements AnalogListener {
    // *************************************************************************
    // constants

    /**
     * limit for main camera panning (unknown units)
     */
    final private static float maxPan = 0.5f;
    /**
     * limit for main camera tilting (unknown units)
     */
    final private static float maxTilt = 1f;
    /**
     * limit for main camera zooming in (logarithmic units)
     */
    final private static float maxMainZoomIn = 15f;
    /**
     * limit for main camera zooming out (logarithmic units)
     */
    final private static float maxMainZoomOut = 5f;
    /**
     * limit for map camera zooming (logarithmic units)
     */
    final private static float maxMapZoom = 5f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AnalogInputState.class.getName());
    /**
     * analog event when the mouse pointer moves down
     */
    final private static String moveDownEvent = "move down";
    /**
     * analog event when the mouse pointer moves left
     */
    final private static String moveLeftEvent = "move left";
    /**
     * analog event when the mouse pointer moves right
     */
    final private static String moveRightEvent = "move right";
    /**
     * analog event when the mouse pointer moves up
     */
    final private static String moveUpEvent = "move up";
    /**
     * analog event when the mouse wheel turns away
     */
    final private static String wheelAwayEvent = "wheel away";
    /**
     * analog event when the mouse wheel turns towards
     */
    final private static String wheelTowardsEvent = "wheel towards";
    // *************************************************************************
    // fields

    /**
     * accumulated amount of main camera zoom (outward from default)
     */
    private float mainZoomOut = 0f;
    /**
     * accumulated amount of map camera zoom (outward from default)
     */
    private float mapZoomOut = 0f;
    /**
     * accumulated amount of main camera pan (left of forward)
     */
    private float panLeft = 0f;
    /**
     * accumulated amount of main camera tilt (upward from horizontal)
     */
    private float tiltUp = 0f;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, enabled AnalogInputState.
     */
    public AnalogInputState() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Reset the cameras back to their default state.
     */
    public void resetCameras() {
        this.mainZoomOut = 0f;
        this.mapZoomOut = 0f;
        this.panLeft = 0f;
        this.tiltUp = 0f;

        updateMainCamera();
        updateMapCamera();
    }
    // *************************************************************************
    // AnalogListener methods

    /**
     * Process an analog event.
     *
     * @param eventString textual description of the analog event (not null)
     * @param amount amount of the event (in unknown units, &ge;0)
     * @param ignored elapsed since the previous update (in seconds, &ge;0)
     */
    @Override
    public void onAnalog(String eventString, float amount, float ignored) {
        Validate.nonNegative(amount, "amount");
        logger.log(Level.INFO, "Received analog event {0} with amount={1}",
                new Object[]{MyString.quote(eventString), amount});

        switch (eventString) {
            case moveDownEvent:
                if (rawInputState.isMiddleButtonPressed()) {
                    tiltUp(+amount);
                }
                return;
            case moveLeftEvent:
                if (rawInputState.isMiddleButtonPressed()) {
                    panLeft(-amount);
                }
                return;
            case moveRightEvent:
                if (rawInputState.isMiddleButtonPressed()) {
                    panLeft(+amount);
                }
                return;
            case moveUpEvent:
                if (rawInputState.isMiddleButtonPressed()) {
                    tiltUp(-amount);
                }
                return;
            case wheelAwayEvent:
                zoomOut(-amount);
                return;
            case wheelTowardsEvent:
                zoomOut(+amount);
                return;
            default:
        }
        logger.log(Level.WARNING, "Analog event {0} was not handled.",
                MyString.quote(eventString));
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

        // Moving the mouse pointer downward triggers an event.
        boolean moveDown = true;
        MouseAxisTrigger moveDownTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_Y, moveDown);
        inputManager.addMapping(moveDownEvent, moveDownTrigger);
        inputManager.addListener(this, moveDownEvent);

        // Moving the mouse pointer left triggers an event.
        boolean moveLeft = true;
        MouseAxisTrigger moveLeftTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_X, moveLeft);
        inputManager.addMapping(moveLeftEvent, moveLeftTrigger);
        inputManager.addListener(this, moveLeftEvent);

        // Moving the mouse pointer right triggers an event.
        boolean moveRight = false;
        MouseAxisTrigger moveRightTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_X, moveRight);
        inputManager.addMapping(moveRightEvent, moveRightTrigger);
        inputManager.addListener(this, moveRightEvent);

        // Moving the mouse pointer up triggers an event.
        boolean moveUp = false;
        MouseAxisTrigger moveUpTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_Y, moveUp);
        inputManager.addMapping(moveUpEvent, moveUpTrigger);
        inputManager.addListener(this, moveUpEvent);

        // Turning the mouse wheel away triggers an event.
        boolean wheelAway = true;
        MouseAxisTrigger wheelAwayTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_WHEEL, wheelAway);
        inputManager.addMapping(wheelAwayEvent, wheelAwayTrigger);
        inputManager.addListener(this, wheelAwayEvent);

        // Turning the mouse wheel toward you triggers an event.
        boolean wheelTowards = false;
        MouseAxisTrigger wheelTowardsTrigger = new MouseAxisTrigger(
                MouseInput.AXIS_WHEEL, wheelTowards);
        inputManager.addMapping(wheelTowardsEvent, wheelTowardsTrigger);
        inputManager.addListener(this, wheelTowardsEvent);

        updateMainCamera();
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Clean up this state on detach.
     */
    @Override
    public void cleanup() {
        // Unregister this event listener.
        inputManager.removeListener(this);

        super.cleanup();
    }
    // *************************************************************************
    // private methods

    /**
     * Pan the main camera.
     *
     * @param amount positive &rarr; left, negative &rarr; right
     */
    private void panLeft(float amount) {
        logger.log(Level.INFO, "{0}", amount);

        this.panLeft += amount;
        this.panLeft = MyMath.clamp(panLeft, maxPan);
        updateMainCamera();
    }

    /**
     * Tilt the main camera.
     *
     * @param amount positive &rarr; up, negative &rarr; down
     */
    private void tiltUp(float amount) {
        logger.log(Level.INFO, "{0}", amount);

        this.tiltUp += amount;
        this.tiltUp = MyMath.clamp(tiltUp, maxTilt);
        updateMainCamera();
    }

    /**
     * Update the main camera's look direction and field of view.
     */
    private void updateMainCamera() {
        float altitude = tiltUp; // radians
        float azimuth = -panLeft; // radians
        Vector3f direction = MyVector3f.fromAltAz(altitude, azimuth);
        mainViewState.setLookDirection(direction);

        float yTangent = FastMath.exp(-0.5f + 0.1f * mainZoomOut);
        Camera mainCamera = mainViewState.getSlot().getCamera();
        MyCamera.setYTangent(mainCamera, yTangent);
    }

    /**
     * Update the map camera's field of view.
     */
    private void updateMapCamera() {
        if (!mapViewState.isEnabled()) {
            return;
        }

        float diameter = 6f * FastMath.exp(0.1f * mapZoomOut); // grid units
        mapViewState.setViewDiameter(diameter);
    }

    /**
     * Zoom a camera.
     *
     * @param amount positive &rarr; out, negative &rarr; (logarithmic units)
     */
    private void zoomOut(float amount) {
        logger.log(Level.INFO, "{0}", amount);

        Vector2f cursorLocation = inputManager.getCursorPosition();
        if (mapViewState.isInside(cursorLocation)) {
            this.mapZoomOut += amount;
            this.mapZoomOut = MyMath.clamp(mapZoomOut, maxMapZoom);
            updateMapCamera();

        } else if (mainViewState.isInside(cursorLocation)) {
            this.mainZoomOut += amount;
            this.mainZoomOut = FastMath.clamp(
                    mainZoomOut, -maxMainZoomIn, maxMainZoomOut);
            updateMainCamera();
        }
    }
}
