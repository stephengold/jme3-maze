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

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.SimpleControl;
import jme3utilities.Validate;

/**
 * Simple control to manage the orientation of a map icon.
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class IconControl
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(IconControl.class.getName());
    // *************************************************************************
    // fields
    /**
     * spatial to copy the orientation from (not null)
     */
    final private Spatial masterSpatial;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control to mimic the orientation of the specified
     * master spatial.
     *
     * @param masterSpatial spatial to mimic (not null)
     */
    public IconControl(Spatial masterSpatial) {
        Validate.nonNull(masterSpatial, "master spatial");
        this.masterSpatial = masterSpatial;
        assert isEnabled();
    }
    // *************************************************************************
    // SimpleControl methods

    /**
     * Copy the master spatial's orientation to the controlled spatial. Invoked
     * when the controlled spatial's geometric state is about to be updated,
     * once per frame while this control attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);
        if (spatial == null) {
            return;
        }
        /*
         * Copy the orientation.
         */
        Quaternion orientation = masterSpatial.getWorldRotation();
        MySpatial.setWorldOrientation(spatial, orientation);
    }
}