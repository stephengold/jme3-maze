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
package jme3maze.model;

import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.navigation.NavVertex;

/**
 * A collection of free items (not owned by any character), indexed by location.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class FreeItems {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(FreeItems.class.getName());
    // *************************************************************************
    // fields
    /**
     * look up the vertex where an item is located
     */
    private TreeMap<Item, NavVertex> itemVertex = new TreeMap<>();
    /**
     * look up all items items at a location
     */
    private TreeMap<NavVertex, TreeSet<Item>> itemsAt = new TreeMap<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Add a specified item at a specified vertex.
     *
     * @param item (not null)
     * @param vertex (not null)
     */
    void add(Item item, NavVertex vertex) {
        assert item != null;
        assert vertex != null;

        NavVertex previousVertex = itemVertex.put(item, vertex);
        assert previousVertex == null : previousVertex;

        TreeSet<Item> list = itemsAt.get(vertex);
        if (list == null) {
            list = new TreeSet();
            TreeSet<Item> previousList = itemsAt.put(vertex, list);
            assert previousList == null : previousList;
        }
        boolean success = list.add(item);
        assert success;
    }

    /**
     * Enumerate all free items.
     *
     * @return a new array
     */
    public Item[] getItems() {
        Collection<Item> collection = itemVertex.keySet();
        int size = collection.size();
        Item[] result = new Item[size];
        int index = 0;
        for (Item item : collection) {
            result[index] = item;
            index++;
        }

        return result;
    }

    /**
     * Enumerate all free items at a particular vertex.
     *
     * @param vertex (not null)
     * @return a new array
     */
    public Item[] getItems(NavVertex vertex) {
        assert vertex != null;

        Collection<Item> collection = itemsAt.get(vertex);
        if (collection == null) {
            return new Item[0];
        }

        int size = collection.size();
        Item[] result = new Item[size];
        int index = 0;
        for (Item item : collection) {
            result[index] = item;
            index++;
        }

        return result;
    }

    /**
     * Locate a particular item if it is free.
     *
     * @param item (not null)
     * @return location vertex, or null if not free
     */
    public NavVertex getVertex(Item item) {
        assert item != null;
        NavVertex result = itemVertex.get(item);
        return result;
    }
}