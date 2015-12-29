/*
 Copyright (c) 2014-2015, Stephen Gold
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
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.Validate;
import jme3utilities.navigation.NavVertex;

/**
 * Mummy item in the Maze Game. Can be converted to a non-player character.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Mummy
        extends Item {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(Mummy.class.getName());
    /**
     * asset path to the "mummy" icon asset
     */
    final private static String iconAssetPath =
            "Textures/map-icons/mummy.png";
    /**
     * asset path to the "mummy" 3-D model asset
     */
    final private static String modelAssetPath =
            "Models/items/mummy/mummy.j3o";
    /**
     * description for "with ankh" use
     */
    final protected static String withAnkhUse = "USE_ANKH_WITH";
    /**
     * offset of model from vertex when free
     */
    final private static Vector3f modelOffset = new Vector3f(-3f, 0f, 3f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a mummy.
     *
     * @param application (not null)
     */
    public Mummy(SimpleApplication application) {
        super("MUMMY", application);
    }
    // *************************************************************************
    // Item methods

    /**
     * Enumerate all current uses for this mummy.
     *
     * @param freeFlag true if this mummy is free, false if it's in inventory
     * @return new instance
     */
    @Override
    public List<String> findUses(boolean freeFlag) {
        List<String> uses = super.findUses(freeFlag);
        uses.remove(takeUse);
        if (freeFlag && isWithinReach()
                && playerState.findItem(Ankh.class) != null) {
            uses.add(withAnkhUse);
        }

        return uses;
    }

    /**
     * Use this mummy in the specified manner.
     *
     * @param useDescription how to use this mummy (not null)
     */
    @Override
    public void use(String useDescription) {
        Validate.nonNull(useDescription, "description");

        switch (useDescription) {
            case withAnkhUse:
                useWithAnkh();
                break;

            default:
                super.use(useDescription);
        }
    }

    /**
     * Use this mummy with an ankh in the player's inventory. This converts the
     * mummy to a non-player character.
     */
    public void useWithAnkh() {
        Ankh ankh = playerState.findItem(Ankh.class);
        assert ankh != null;
        playerState.remove(ankh);

        freeItemsState.remove(this);

        Mapmaker npc = new Mapmaker(application);
        NavVertex vertex = playerState.getVertex();
        freeItemsState.add(npc, vertex);

        inputState.alert("RESURRECTED_MAPMAKER");
    }

    /**
     * Visualize this mummy in the main view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMain() {
        Node node = (Node) assetManager.loadModel(modelAssetPath);
        Vector3f offset = new Vector3f(modelOffset);
        node.setLocalTranslation(offset);

        ColorRGBA color = ColorRGBA.White;
        Material material = MyAsset.createShinyMaterial(assetManager, color);
        node.setMaterial(material);

        return node;
    }

    /**
     * Visualize this mummy in the map view.
     *
     * @return new unparented instance
     */
    @Override
    public Spatial visualizeMap() {
        Spatial icon = mapViewState.loadIcon(iconAssetPath, true);
        return icon;
    }
}