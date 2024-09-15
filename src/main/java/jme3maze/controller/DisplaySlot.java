/*
 Copyright (c) 2014-2017 Stephen Gold

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
package jme3maze.controller;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

/**
 * Interface to manage a display slot: a view port with at most one scene.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public interface DisplaySlot {

    /**
     * Access this slot's camera.
     *
     * @return pre-existing instance
     */
    Camera getCamera();

    /**
     * Access this slot's root node.
     *
     * @return pre-existing instance (or null if none)
     */
    Node getRootNode();

    /**
     * Test whether the specified screen coordinates are inside this slot.
     *
     * @param screenLocation screen coordinates (in pixels, measured from the
     * lower left, not null, unaffected)
     * @return true if location is within the view port, otherwise false
     */
    boolean isInside(Vector2f screenLocation);

    /**
     * Using this slot's camera, construct a pick ray for the specified screen
     * coordinates.
     *
     * @param screenLocation screen coordinates (in pixels, measured from the
     * lower left, not null, unaffected)
     * @return new instance
     */
    Ray pickRay(Vector2f screenLocation);

    /**
     * Alter this slot's background color.
     *
     * @param newColor new color (not null)
     */
    void setBackgroundColor(ColorRGBA newColor);

    /**
     * Alter this slot's root node.
     *
     * @param newRoot root node (or null for none)
     */
    void setRootNode(Node newRoot);
}