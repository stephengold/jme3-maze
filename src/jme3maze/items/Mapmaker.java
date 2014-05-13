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
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.util.logging.Logger;
import jme3utilities.MyAsset;

/**
 * Mapmaker item in the Maze Game. Provides a map view and updates the player's
 * maps.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Mapmaker
        extends Item {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Mapmaker.class.getName());
    /**
     * asset path to the "mapmaker" icon asset
     */
    final private static String iconAssetPath =
            "Textures/map-icons/mapmaker.png";
    /**
     * asset path to the "mapmaker" 3-D model asset
     */
    final private static String modelAssetPath =
            "Models/items/mapmaker/mapmaker.j3o";
    /**
     * offset of model from vertex when free
     */
    final private static Vector3f modelOffset = new Vector3f(-3f, 0f, 3f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mapmaker.
     *
     * @param application (not null)
     */
    public Mapmaker(SimpleApplication application) {
        super("mapmaker", application);
    }
    // *************************************************************************
    // Item methods

    /**
     * Encounter this mapmaker free in the world.
     */
    @Override
    public void encounter() {
        boolean success = freeItemsState.remove(this);
        assert success;

        mapViewState.setEnabled(true);
        System.out.printf("You recruited the %s!%n", getTypeName());
    }

    /**
     * Visualize this mapmaker in the main view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMain() {
        Node node = (Node) assetManager.loadModel(modelAssetPath);
        Vector3f offset = new Vector3f(modelOffset);
        node.setLocalTranslation(offset);

        ColorRGBA brushColor = ColorRGBA.Red;
        Material brushMaterial =
                MyAsset.createShinyMaterial(assetManager, brushColor);
        Spatial brush = node.getChild("brush");
        brush.setMaterial(brushMaterial);

        Texture eyeTexture = MyAsset.loadTexture(assetManager,
                "Textures/items/brown_eye.png");
        Material eyesMaterial =
                MyAsset.createShinyMaterial(assetManager, ColorRGBA.White);
        eyesMaterial.setTexture("DiffuseMap", eyeTexture);
        Spatial eyes = node.getChild("eyes");
        eyes.setMaterial(eyesMaterial);

        ColorRGBA skirtColor = ColorRGBA.White;
        Material skirtMaterial =
                MyAsset.createShinyMaterial(assetManager, skirtColor);
        RenderState renderState = skirtMaterial.getAdditionalRenderState();
        renderState.setFaceCullMode(FaceCullMode.Off);
        Node main = (Node) node.getChild("main");
        Spatial skirt = main.getChild("main2");
        skirt.setMaterial(skirtMaterial);

        ColorRGBA tabletColor = ColorRGBA.Gray;
        Material tabletMaterial =
                MyAsset.createShinyMaterial(assetManager, tabletColor);
        Spatial tablet = node.getChild("tablet");
        tablet.setMaterial(tabletMaterial);

        return node;
    }

    /**
     * Visualize this mapmaker in the map view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMap() {
        Spatial icon = mapViewState.loadIcon(iconAssetPath, true);
        return icon;
    }
}