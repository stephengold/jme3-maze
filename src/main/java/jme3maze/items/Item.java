/*
 Copyright (c) 2014-2017, Stephen Gold
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
import jme3maze.model.FreeItemsState;
import jme3maze.model.PlayerState;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.navigation.NavVertex;

/**
 * Generic collectible item in the Maze Game. Each item has a named type.
 * Distinct instances may share the same type.
 *
 * @author Stephen Gold sgold@sonic.net
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
    final private static String discardUse = "DISCARD";
    /**
     * asset path to the "unknown" icon asset
     */
    final private static String iconAssetPath
            = "Textures/map-icons/unknown.png";
    /**
     * description for "SHIFT_LEFT" use
     */
    final private static String shiftLeftUse = "SHIFT_LEFT";
    /**
     * description for "SHIFT_RIGHT" use
     */
    final private static String shiftRightUse = "SHIFT_RIGHT";
    /**
     * description for "TAKE" use
     */
    final protected static String takeUse = "TAKE";
    // *************************************************************************
    // fields
    /**
     * appstate manager: set by constructor
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
        Validate.nonEmpty(typeName, "typeName");
        Validate.nonNull(application, "application");

        this.typeName = typeName;

        this.application = application;
        assetManager = application.getAssetManager();
        stateManager = application.getStateManager();
        /*
         * Initialize appState references.
         */
        freeItemsState = stateManager.getState(FreeItemsState.class);
        assert freeItemsState != null;

        playerState = stateManager.getState(PlayerState.class);
        assert playerState != null;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Describe how this item may be used.
     *
     * @param freeFlag true if item is free, false if it's in an inventory
     * @return localized description suitable for a tool tip, or null if no uses
     */
    public String describeUse(boolean freeFlag) {
        return "";
    }

    /**
     * Remove this item from the player's inventory.
     */
    public void discard() {
        boolean success = playerState.discard(this);
        assert success : this;
        NavVertex vertex = playerState.getVertex();
        freeItemsState.add(this, vertex);
    }

    /**
     * Encounter this item free in the world.
     */
    public void encounter() {
    }

    /**
     * Enumerate all current uses for this item.
     *
     * @param freeFlag true if item is free, false if it's in inventory
     * @return new instance
     */
    public List<String> findUses(boolean freeFlag) {
        List<String> result = new LinkedList<>();
        if (freeFlag) {
            if (isWithinReach()) {
                result.add(takeUse);
            }
        } else {
            result.add(discardUse);
            if (playerState.isLeftHandEmpty()) {
                result.add(shiftLeftUse);
            }
            if (playerState.isRightHandEmpty()) {
                result.add(shiftRightUse);
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
        assert typeName.length() > 0;
        return typeName;
    }

    /**
     * Read the localized name of this item's type.
     *
     * @return localized name of this item's type (not null, not empty)
     */
    public String getLocalTypeName() {
        return "";
    }

    /**
     * Test whether this free item is within the player's reach.
     *
     * @return true if within reach, otherwise false
     */
    public boolean isWithinReach() {
        NavVertex itemVertex = freeItemsState.getVertex(this);
        if (itemVertex == null) {
            return false;
        }
        NavVertex playerVertex = playerState.getVertex();
        boolean result = itemVertex == playerVertex;

        return result;
    }

    /**
     * Shift this item from the left hand to the right hand.
     */
    public void shiftLeft() {
        playerState.setRightHandItem(null);
        playerState.setLeftHandItem(this);
    }

    /**
     * Shift this item from the right hand to the left hand.
     */
    public void shiftRight() {
        playerState.setLeftHandItem(null);
        playerState.setRightHandItem(this);
    }

    /**
     * Add this item to the player's inventory, favoring the right hand.
     */
    public void take() {
        boolean success = playerState.takeFavorRightHand(this);
        if (!success) {
            return;
        }

        success = freeItemsState.remove(this);
        assert success : this;
    }

    /**
     * Use this item, or if it currently has multiple uses, let the player
     * select a use and do that one.
     *
     * @param freeFlag true if item is free, false if it's in an inventory
     */
    public void use(boolean freeFlag) {
        List<String> possibleUses = findUses(freeFlag);
        int numUses = possibleUses.size();
        if (numUses > 1) {
        } else if (numUses == 1) {
            use(possibleUses.get(0));
        }
    }

    /**
     * Use this item in the specified manner.
     *
     * @param useDescription how to use this item (not null, not empty)
     */
    public void use(String useDescription) {
        Validate.nonEmpty(useDescription, "description");

        switch (useDescription) {
            case discardUse:
                discard();
                break;

            case shiftLeftUse:
                shiftLeft();
                break;

            case shiftRightUse:
                shiftRight();
                break;

            case takeUse:
                take();
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
        logger.log(Level.WARNING, "invisible free item of type {0}",
                MyString.quote(typeName));
        Node result = new Node();
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
        String otherType = otherItem.getTypeName();
        return typeName.compareTo(otherType);
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
     *
     * @return value for use in hashing
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
     * @throws IOException from importer
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
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
