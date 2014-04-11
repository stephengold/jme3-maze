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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3maze.model.PlayerState;
import jme3maze.view.MapViewState;
import jme3utilities.MyAsset;
import jme3utilities.Validate;

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
     * asset path to the "teapot" 3D model asset in jME3-testdata.jar
     */
    final private static String teapotAssetPath = "Models/Teapot/Teapot.obj";
    // *************************************************************************
    // fields
    /**
     * application: set by constructor
     */
    final private Application application;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a crown.
     *
     * @param application (not null)
     */
    public Crown(Application application) {
        super("crown");
        Validate.nonNull(application, "application");
        this.application = application;
    }
    // *************************************************************************
    // Item methods

    /**
     * Encounter this crown free in the world.
     */
    @Override
    public void encounter() {
        AppStateManager stateManager = application.getStateManager();
        PlayerState playerState = stateManager.getState(PlayerState.class);
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
        /*
         * A crown is represented by a gold teapot.
         */
        AssetManager assetManager = application.getAssetManager();
        Spatial spatial = assetManager.loadModel(teapotAssetPath);

        spatial.setLocalScale(2f);

        Vector3f offset = new Vector3f(0f, 4f, 0f); // floating in the air
        spatial.setLocalTranslation(offset);

        ColorRGBA color = new ColorRGBA(0.09f, 0.08f, 0.05f, 1f);
        Material material = MyAsset.createShinyMaterial(assetManager, color);
        spatial.setMaterial(material);

        return spatial;
    }

    /**
     * Visualize this crown in the map view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMap() {
        AppStateManager stateManager = application.getStateManager();
        MapViewState mapViewState = stateManager.getState(MapViewState.class);
        Spatial icon = mapViewState.loadIcon(iconAssetPath, true);

        return icon;
    }
}