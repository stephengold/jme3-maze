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
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.InputState;
import jme3maze.model.FreeItemsState;
import jme3maze.model.PlayerState;
import jme3maze.view.MapViewState;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.navigation.NavVertex;

/**
 * Generic collectible item in the Maze Game. Each item has a named type.
 * Multiple instances may share the same type.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Item
        implements Comparable<Item>, Savable {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Item.class.getName());
    /**
     * description for "discard" use
     */
    final private static String discardUse = "discard";
    /**
     * asset path to the "unknown" icon asset
     */
    final private static String iconAssetPath =
            "Textures/map-icons/unknown.png";
    /**
     * description for "shift to left hand" use
     */
    final private static String shiftLeftUse = "shift to left hand";
    /**
     * description for "shift to right hand" use
     */
    final private static String shiftRightUse = "shift to right hand";
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
     * control state: set by constructor
     */
    final protected InputState inputState;
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

        inputState = stateManager.getState(InputState.class);
        assert inputState != null;

        mapViewState = stateManager.getState(MapViewState.class);
        assert mapViewState != null;

        playerState = stateManager.getState(PlayerState.class);
        assert playerState != null;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Describe how this item is used.
     *
     * @param freeFlag true if item is free, false if it's in an inventory
     * @return description suitable for a tool tip, or null if no uses
     */
    public String describeUse(boolean freeFlag) {
        List<String> possibleUses = findUses(freeFlag);
        int numUses = possibleUses.size();

        String result;
        if (numUses > 1) {
            result = String.format("use this %s", typeName);
        } else if (numUses == 1) {
            String use = possibleUses.get(0);
            result = String.format("%s this %s", use, typeName);
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Encounter this item free in the world.
     */
    public void encounter() {
        logger.log(Level.WARNING, "ignored free item of type {0}",
                MyString.quote(typeName));
    }

    /**
     * Enumerate all current uses for this item.
     *
     * @param freeFlag true if item is free, false if it's in an inventory
     * @return new instance
     */
    public List<String> findUses(boolean freeFlag) {
        List<String> result = new LinkedList<>();
        if (!freeFlag) {
            result.add("discard");
            if (playerState.isLeftHandEmpty()) {
                result.add("shift to left hand");
            }
            if (playerState.isRightHandEmpty()) {
                result.add("shift to right hand");
            }
        }

        return result;
    }

    /**
     * Read the type name of this item.
     *
     * @return name of the item's type (not null, not empty)
     */
    public String getTypeName() {
        assert typeName != null;
        assert typeName.length() > 0 : typeName;

        return typeName;
    }

    /**
     * Use this item, or if it currently has multiple uses, have the user select
     * one.
     *
     * @param freeFlag true if item is free, false if it's in an inventory
     */
    public void use(boolean freeFlag) {
        List<String> possibleUses = findUses(freeFlag);
        int numUses = possibleUses.size();
        if (numUses > 1) {
            inputState.selectUse(this);
        } else if (numUses == 1) {
            use(possibleUses.get(0));
        }
    }

    /**
     * Use this item in the specified manner.
     *
     * @param useDescription how to use this item (not null)
     */
    public void use(String useDescription) {
        Validate.nonNull(useDescription, "description");

        switch (useDescription) {
            case discardUse:
                boolean success = playerState.discard(this);
                assert success;
                NavVertex vertex = playerState.getVertex();
                freeItemsState.add(this, vertex);
                break;

            case shiftLeftUse:
                playerState.setRightHandItem(null);
                playerState.setLeftHandItem(this);
                break;

            case shiftRightUse:
                playerState.setLeftHandItem(null);
                playerState.setRightHandItem(this);
                break;

            default:
                logger.log(Level.WARNING, "ignored unknown use {0}",
                        MyString.quote(useDescription));
        }
    }

    /**
     * Visualize this item in the inventory.
     *
     * @return asset path to a texture
     */
    public String visualizeInventory() {
        logger.log(Level.WARNING, "ignored item of type {0}",
                MyString.quote(typeName));
        return iconAssetPath;
    }

    /**
     * Visualize this item, free in the main view.
     *
     * @return new unparented instance
     */
    public Spatial visualizeMain() {
        logger.log(Level.WARNING, "ignored free item of type {0}",
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
     * @return 0 if the items have the same type
     */
    @Override
    public int compareTo(Item otherItem) {
        String otherName = otherItem.getTypeName();
        return typeName.compareTo(otherName);
    }
    // *************************************************************************
    // Object methods

    /**
     * Compare for equality based on type of item.
     *
     * @param otherObject (unaffected)
     * @return true if the items have the same type, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        boolean result = false;

        if (this == otherObject) {
            result = true;

        } else if (otherObject instanceof Item) {
            Item otherItem = (Item) otherObject;
            String otherType = otherItem.getTypeName();
            result = typeName.equals(otherType);
        }

        return result;
    }

    /**
     * Generate the hash code for this item.
     */
    @Override
    public int hashCode() {
        int hash = Objects.hashCode(this.typeName);
        return hash;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this item, for example when loading from a J3O file.
     *
     * @param importer (not null)
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Serialize this item, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
