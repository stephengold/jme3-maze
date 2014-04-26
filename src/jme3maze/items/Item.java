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
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.model.FreeItemsState;
import jme3maze.model.PlayerState;
import jme3maze.view.MapViewState;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * Generic collectible item in the Maze Game. Each item has a named type.
 * Multiple instances may share the same type.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Item
        implements Comparable<Item> {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Item.class.getName());
    /**
     * asset path to the "unknown" icon asset
     */
    final private static String iconAssetPath =
            "Textures/map-icons/unknown.png";
    // *************************************************************************
    // fields
    /**
     * app state manager: set by constructor
     */
    final protected AppStateManager stateManager;
    /**
     * asset manager: set by constructor
     */
    final protected AssetManager assetManager;
    /**
     * free items: set by constructor
     */
    final protected FreeItemsState freeItemsState;
    /**
     * map view: set by constructor
     */
    final protected MapViewState mapViewState;
    /**
     * player: set by constructor
     */
    final protected PlayerState playerState;
    /**
     * application: set by constructor
     */
    final protected SimpleApplication application;
    /**
     * name of this item's type: set by constructor
     */
    final private String typeName;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an item with a named type.
     *
     * @param typeName (not null, not empty)
     */
    Item(String typeName, SimpleApplication application) {
        assert typeName != null;
        assert typeName.length() > 0 : typeName;
        Validate.nonNull(application, "application");

        this.typeName = typeName;
        this.application = application;
        assetManager = application.getAssetManager();
        stateManager = application.getStateManager();

        freeItemsState = stateManager.getState(FreeItemsState.class);
        assert freeItemsState != null;
        mapViewState = stateManager.getState(MapViewState.class);
        assert mapViewState != null;
        playerState = stateManager.getState(PlayerState.class);
        assert playerState != null;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Encounter this item free in the world.
     */
    public void encounter() {
        logger.log(Level.WARNING, "ignored free item with unknown type {0}",
                MyString.quote(typeName));
    }

    /**
     * Read the type name of this item.
     *
     * @return (not null, not empty)
     */
    public String getTypeName() {
        assert typeName != null;
        assert typeName.length() > 0 : typeName;

        return typeName;
    }

    /**
     * Use this item.
     */
    public void use() {
        // TODO
    }

    /**
     * Visualize this item in an inventory.
     *
     * @return asset path of a texture
     */
    public String visualizeInventory() {
        logger.log(Level.WARNING, "ignored item with unknown type {0}",
                MyString.quote(typeName));
        return iconAssetPath;
    }

    /**
     * Visualize this item free in the main view.
     *
     * @return new unparented instance
     */
    public Spatial visualizeMain() {
        logger.log(Level.WARNING, "ignored free item with unknown type {0}",
                MyString.quote(typeName));
        Node result = new Node();
        return result;
    }

    /**
     * Visualize this item, free in the map view.
     *
     * @return new unparented instance
     */
    public Spatial visualizeMap() {
        logger.log(Level.WARNING, "free item with unknown type {0}",
                MyString.quote(typeName));
        Spatial result = mapViewState.loadIcon(iconAssetPath, true);
        return result;
    }
    // *************************************************************************
    // Comparable methods

    /**
     * Compare with another item.
     *
     * @param otherItem (not null)
     * @return 0 if the items have the same type name
     */
    @Override
    public int compareTo(Item otherItem) {
        String otherName = otherItem.getTypeName();
        return typeName.compareTo(otherName);
    }
}