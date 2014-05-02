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
package jme3maze.items;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.controls.RotationControl;

/**
 * Crown item in the Maze Game. Ends the game.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Crown
        extends Item {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Crown.class.getName());
    /**
     * asset path to the "crown" icon asset
     */
    final private static String iconAssetPath = "Textures/map-icons/crown.png";
    /**
     * asset path to the "crown" 3-D model asset
     */
    final private static String modelAssetPath = "Models/items/crown/crown.j3o";
    /**
     * offset of model from vertex when free
     */
    final private static Vector3f modelOffset = new Vector3f(0f, 5f, 0f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a crown.
     *
     * @param application (not null)
     */
    public Crown(SimpleApplication application) {
        super("crown", application);
    }
    // *************************************************************************
    // Item methods

    /**
     * Encounter this crown free in the world.
     */
    @Override
    public void encounter() {
        int moveCount = playerState.getMoveCount();
        if (moveCount == 1) {
            System.out.printf(
                    "You traversed the maze in one move!%n");
        } else {
            System.out.printf(
                    "You traversed the maze in %d moves.%n",
                    moveCount);
        }
        application.stop();
    }

    /**
     * Visualize this crown in the main view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMain() {
        Node node = (Node) assetManager.loadModel(modelAssetPath);
        Vector3f offset = new Vector3f(modelOffset);
        node.setLocalTranslation(offset);

        ColorRGBA hedjetColor = ColorRGBA.White;
        Material hedjetMaterial =
                MyAsset.createShinyMaterial(assetManager, hedjetColor);
        Spatial hedjet = node.getChild("Hedjet");
        hedjet.setMaterial(hedjetMaterial);

        ColorRGBA deshretColor = ColorRGBA.Red;
        Material deshretMaterial =
                MyAsset.createShinyMaterial(assetManager, deshretColor);
        Spatial deshret = node.getChild("Deshret");
        deshret.setMaterial(deshretMaterial);
        /*
         * Make it rotate.
         */
        Vector3f axis = Vector3f.UNIT_Y;
        float rate = 0.5f; // radians per second
        RotationControl rotationControl = new RotationControl(rate, axis);
        node.addControl(rotationControl);

        return node;
    }

    /**
     * Visualize this crown in the map view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMap() {
        Spatial icon = mapViewState.loadIcon(iconAssetPath, true);
        return icon;
    }
}