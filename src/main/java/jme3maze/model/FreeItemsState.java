/*
 Copyright (c) 2014-2025 Stephen Gold

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
package jme3maze.model;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3maze.items.Ankh;
import jme3maze.items.Crown;
import jme3maze.items.Item;
import jme3maze.items.Mummy;
import jme3maze.items.Torch;
import jme3utilities.Validate;
import jme3utilities.math.noise.Generator;
import jme3utilities.navigation.NavGraph;
import jme3utilities.navigation.NavVertex;

/**
 * Appstate to manage the items not owned by any character in the Maze Game.
 * <p>
 * Enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class FreeItemsState extends GameAppState {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            FreeItemsState.class.getName());
    // *************************************************************************
    // fields

    /**
     * look up the vertex where an item is located
     */
    final private Map<Item, NavVertex> itemVertex = new TreeMap<>();
    /**
     * look up all items items at a location
     */
    final private Map<NavVertex, Set<Item>> itemsAt = new TreeMap<>();
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, enabled FreeItemsState.
     */
    public FreeItemsState() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a specified item at a specified vertex.
     *
     * @param item (not null)
     * @param vertex (not null)
     */
    public void add(Item item, NavVertex vertex) {
        Validate.nonNull(item, "item");
        Validate.nonNull(vertex, "vertex");

        NavVertex previousVertex = itemVertex.put(item, vertex);
        assert previousVertex == null : previousVertex;

        Set<Item> list = itemsAt.get(vertex);
        if (list == null) {
            list = new TreeSet<>();
            Set<Item> previousList = itemsAt.put(vertex, list);
            assert previousList == null : previousList;
        }
        boolean success = list.add(item);
        assert success;

        if (item instanceof Torch) {
            Torch torch = (Torch) item;
            Vector3f offset = torch.getLightOffset();
            Vector3f location = vertex.copyLocation();
            location.addLocal(offset);
            worldState.setTorchLocation(location);
        }

        // update view
        if (mainViewState != null && mainViewState.isInitialized()) {
            mainViewState.addFreeItem(item);
        }
    }

    /**
     * Enumerate all free items.
     *
     * @return new array
     */
    public Item[] getAll() {
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
     * Enumerate all free items at the specified vertex.
     *
     * @param vertex (not null)
     * @return new array
     */
    public Item[] getItems(NavVertex vertex) {
        Validate.nonNull(vertex, "vertex");

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
     * Locate the specified item if it is free.
     *
     * @param item (not null)
     * @return location vertex, or null if not free
     */
    public NavVertex getVertex(Item item) {
        Validate.nonNull(item, "item");
        NavVertex result = itemVertex.get(item);
        return result;
    }

    /**
     * Remove the specified item from the collection.
     *
     * @param item (not null)
     * @return true if successful, otherwise false
     */
    public boolean remove(Item item) {
        Validate.nonNull(item, "item");

        NavVertex vertex = itemVertex.remove(item);
        if (vertex == null) {
            return false;
        }
        Set<Item> list = itemsAt.get(vertex);
        assert list != null : item;
        boolean success = list.remove(item);
        assert success : item;

        // update view
        success = mainViewState.removeFreeItem(item);
        assert success : item;
        mapViewState.removeFreeItem(item);

        return true;
    }
    // *************************************************************************
    // GameAppState methods

    /**
     * Initialize this state prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application attaching application (not null)
     */
    @Override
    public void initialize(
            AppStateManager stateManager, Application application) {
        super.initialize(stateManager, application);
        /*
         * Place the crown (game-ending goal) at the vertex
         * furthest from the starting point.
         */
        NavVertex startVertex = playerState.getVertex();
        NavGraph graph = worldState.getGraph();
        List<NavVertex> farVertices = graph.findMostHops(startVertex);
        Generator generator = worldState.getGenerator();
        NavVertex goalVertex = (NavVertex) generator.pick(farVertices);
        MazeLevel bottom = worldState.getLevel(worldState.getNumLevels() - 1);
        assert goalVertex.copyLocation().y == bottom.getFloorY();
        Crown crown = new Crown(simpleApplication);
        add(crown, goalVertex);
        /*
         * Place the torch at a random vertex exactly 2 hops
         * from the starting point.
         */
        List<NavVertex> options = graph.findByHops(2, startVertex);
        options.remove(goalVertex);
        NavVertex torchVertex = (NavVertex) generator.pick(options);
        if (torchVertex != null) {
            Torch torch = new Torch(simpleApplication);
            add(torch, torchVertex);
        }
        /*
         * Place the ankh at a random vertex exactly 10 hops
         * from the starting point.
         */
        options = graph.findByHops(10, startVertex);
        options.remove(goalVertex);
        NavVertex ankhVertex = (NavVertex) generator.pick(options);
        if (ankhVertex != null) {
            Ankh ankh = new Ankh(simpleApplication);
            add(ankh, ankhVertex);
        }
        /*
         * Place a mummy at a random vertex exactly 12 hops
         * from the starting point.
         */
        options = graph.findByHops(12, startVertex);
        options.remove(goalVertex);
        NavVertex mummyVertex = (NavVertex) generator.pick(options);
        if (mummyVertex != null) {
            Mummy mummy = new Mummy(simpleApplication);
            add(mummy, mummyVertex);
        }
    }
}
