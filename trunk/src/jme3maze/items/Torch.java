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
import jme3maze.model.FreeItemsState;
import jme3maze.model.PlayerState;
import jme3maze.view.MapViewState;
import jme3utilities.MyAsset;
import jme3utilities.Validate;

/**
 * Torch item in the Maze Game. Provides illumination.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Torch
        extends Item {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Torch.class.getName());
    /**
     * asset path to the "torch" icon asset
     */
    final private static String iconAssetPath = "Textures/map-icons/torch.png";
    /**
     * asset path to the "torch" 3D model asset
     */
    final private static String modelAssetPath = "Models/items/torch/torch.j3o";
    // *************************************************************************
    // fields
    /**
     * application: set by constructor
     */
    final private Application application;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a torch.
     *
     * @param application (not null)
     */
    public Torch(Application application) {
        super("torch");
        Validate.nonNull(application, "application");
        this.application = application;
    }
    // *************************************************************************
    // Item methods

    /**
     * Encounter this torch, free in the world.
     */
    @Override
    public void encounter() {
        AppStateManager stateManager = application.getStateManager();
        FreeItemsState freeItemsState =
                stateManager.getState(FreeItemsState.class);
        freeItemsState.remove(this);

        PlayerState playerState = stateManager.getState(PlayerState.class);

        System.out.printf("You acquired a %s!%n", getTypeName());
    }

    /**
     * Visualize this torch in the main view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMain() {
        AssetManager assetManager = application.getAssetManager();
        Spatial spatial = assetManager.loadModel(modelAssetPath);

        spatial.setLocalScale(0.2f);

        Vector3f offset = new Vector3f(0f, 6f, 0f); // floating in the air
        spatial.setLocalTranslation(offset);

        ColorRGBA color = new ColorRGBA(0.09f, 0.08f, 0.05f, 1f);
        Material material = MyAsset.createShinyMaterial(assetManager, color);
        spatial.setMaterial(material);

        return spatial;
    }

    /**
     * Visualize this torch in the map view.
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