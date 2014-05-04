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
package jme3maze.view;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.List;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3utilities.Validate;

/**
 * Game app state to manage the default view.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class BigViewState
        extends GameAppState
        implements View {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(BigViewState.class.getName());
    // *************************************************************************
    // View methods

    /**
     * Access this view's camera.
     *
     * @return pre-existing instance
     */
    @Override
    public Camera getCamera() {
        Camera result = cam;
        assert result != null;
        return result;
    }

    /**
     * Access this view's root node.
     *
     * @return pre-existing instance (or null if none)
     */
    @Override
    public Node getRootNode() {
        List<Spatial> scenes = viewPort.getScenes();
        if (scenes.isEmpty()) {
            return null;
        }
        Spatial spatial = scenes.get(0);
        return (Node) spatial;
    }

    /**
     * Test whether the specified screen coordinates are inside this view.
     *
     * @param screenLocation screen coordinates (in pixels, measured from the
     * lower left, not null, unaffected)
     * @return true if location is within this view, otherwise false
     */
    @Override
    public boolean isInside(Vector2f screenLocation) {
        Validate.nonNull(screenLocation, "screen location");

        if (insetViewState.isInside(screenLocation)) {
            return false;
        }
        /*
         * Scale coordinates to fractions of the viewport dimensions.
         */
        float xFraction = screenLocation.x / cam.getWidth();
        float yFraction = screenLocation.y / cam.getHeight();
        if (xFraction > 0f && xFraction < 1f
                && yFraction > 0f && yFraction < 1f) {
            return true;
        }
        return false;
    }

    /**
     * Using this view's camera, construct a pick ray for the specified screen
     * coordinates.
     *
     * @param screenLocation screen coordinates (in pixels, measured from the
     * lower left, not null, unaffected)
     * @return new instance
     */
    @Override
    public Ray pickRay(Vector2f screenLocation) {
        Validate.nonNull(screenLocation, "screen location");

        Vector3f startLocation = cam.getWorldCoordinates(screenLocation, 0f);
        Vector3f farPoint = cam.getWorldCoordinates(screenLocation, 1f);
        Vector3f direction = farPoint.subtract(startLocation).normalizeLocal();
        Ray result = new Ray(startLocation, direction);

        return result;
    }

    /**
     * Alter this view's background color.
     *
     * @param newColor new color (not null)
     */
    @Override
    public void setBackgroundColor(ColorRGBA newColor) {
        Validate.nonNull(newColor, "color");
        viewPort.setBackgroundColor(newColor);
    }

    /**
     * Alter this view's root node.
     *
     * @param newRoot root node (or null for none)
     */
    @Override
    public void setRootNode(Node newRoot) {
        viewPort.clearScenes();
        if (newRoot != null) {
            viewPort.attachScene(newRoot);
        }
    }
}