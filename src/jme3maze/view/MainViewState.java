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
package jme3maze.view;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.texture.Texture;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3maze.controller.DisplaySlot;
import jme3maze.items.Item;
import jme3maze.model.MazeLevel;
import jme3maze.model.WorldState;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.controls.CameraControl;
import jme3utilities.debug.Printer;
import jme3utilities.navigation.NavDebug;
import jme3utilities.navigation.NavGraph;
import jme3utilities.navigation.NavVertex;

/**
 * Game app state to manage the (1st-person) main view in the Maze Game.
 * <p>
 * Enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MainViewState extends GameAppState {
    // *************************************************************************
    // constants

    /**
     * flag to enable debugging aids
     */
    final private static boolean debugFlag = false;
    /**
     * flag to enable shaded materials
     */
    final private static boolean shadedFlag = true;
    /**
     * flag to enable shadows
     */
    final private static boolean shadowsFlag = true;
    /**
     * height of camera above player's base location (in world units)
     */
    final private static float eyeHeight = 5f;
    /**
     * height of maze walls (in world units)
     */
    final private static float wallHeight = 10f;
    /**
     * resolution of shadow map (in pixels per side)
     */
    final private static int shadowMapSize = 256;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MainViewState.class.getName());
    /**
     * asset path to the "pond" texture asset in jME3-testdata.jar
     */
    final private static String pondAssetPath =
            "Textures/Terrain/Pond/Pond.jpg";
    /**
     * asset path to the "wall" texture asset in jME3-testdata.jar
     */
    final private static String wallAssetPath =
            "Textures/Terrain/BrickWall/BrickWall.jpg";
    /**
     * local "forward" direction (length=1)
     */
    final private static Vector3f forwardDirection = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // fields
    
    /**
     * scene-graph control which links the camera's orientation to that of the
     * player's avatar
     */
    private CameraControl forwardView;
    /**
     * slot to display this view: set in initialize()
     */
    private DisplaySlot slot;
    /**
     * map free items to their spatials
     */
    final private Map<Item, Spatial> itemSpatial = new TreeMap<>();
    /**
     * shared material for ceiling geometries
     */
    private Material ceilingMaterial;
    /**
     * shared material for floor geometries
     */
    private Material floorMaterial;
    /**
     * shared material for wall geometries
     */
    private Material wallMaterial;
    /**
     * node which represents the player in this view
     */
    final private Node avatarNode = new Node("main avatar node");
    /**
     * point light source for this view
     */
    final private PointLight torch = new PointLight();
    // *************************************************************************
    // new methods exposed

    /**
     * Add 3-D representation of a free item to the scene.
     *
     * @param item free item to represent (not null)
     */
    public void addFreeItem(Item item) {
        Spatial spatial = item.visualizeMain();
        NavVertex vertex = freeItemsState.getVertex(item);
        Vector3f location = vertex.copyLocation();
        spatial.move(location);

        spatial.setUserData("item", item);
        itemSpatial.put(item, spatial);
        Node root = slot.getRootNode();
        root.attachChild(spatial);
    }

    /**
     * Find the item (if any) in this view at the specified screen coordinates.
     *
     * @param screenLocation screen coordinates (in pixels, measured from the
     * lower left, not null, unaffected)
     * @return pre-existing vertex or null for none
     */
    public Item findItem(Vector2f screenLocation) {
        Validate.nonNull(screenLocation, "screen location");

        if (!isInside(screenLocation)) {
            return null;
        }
        /*
         * Construct a ray based on the screen coordinates.
         */
        if (!slot.isInside(screenLocation)) {
            return null;
        }
        Ray ray = slot.pickRay(screenLocation);
        /*
         * Trace the ray to the nearest geometry.
         */
        Node root = slot.getRootNode();
        CollisionResults results = new CollisionResults();
        root.collideWith(ray, results);
        CollisionResult nearest = results.getClosestCollision();
        if (nearest == null) {
            return null;
        }
        Geometry geometry = nearest.getGeometry();
        /*
         * Check whether the geometry corresponds to an item.
         */
        Spatial spatial = geometry;
        while (spatial != null) {
            Item item = spatial.getUserData("item");
            if (item != null) {
                return item;
            }
            spatial = spatial.getParent();
        }

        return null;
    }

    /**
     * Replace CameraControl with FlyByCamera for debugging.
     */
    public void fly() {
        CameraControl cameraControl =
                avatarNode.getControl(CameraControl.class);
        cameraControl.setEnabled(false);

        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(10f);
    }

    /**
     * Access the scene's light source.
     *
     * @return pre-existing instance
     */
    public Light getLight() {
        assert torch != null;
        return torch;
    }

    /**
     * Access this view's display slot.
     *
     * @return pre-existing instance
     */
    public DisplaySlot getSlot() {
        assert slot != null;
        return slot;
    }

    /**
     * Test whether the specified screen coordinates are in this view.
     *
     * @param screenLocation screen coordinates (in pixels, measured from the
     * lower left, not null, unaffected)
     * @return true if location is within this view, otherwise false
     */
    public boolean isInside(Vector2f screenLocation) {
        Validate.nonNull(screenLocation, "screen location");
        boolean result = slot.isInside(screenLocation);
        return result;
    }

    /**
     * Remove the 3-D representation of a free item from the scene.
     *
     * @param item free item to remove (not null)
     * @return true if successful, otherwise false
     */
    public boolean removeFreeItem(Item item) {
        Validate.nonNull(item, "item");

        Spatial spatial = itemSpatial.remove(item);
        if (spatial == null) {
            return false;
        }
        boolean success = spatial.removeFromParent();
        assert success : item;

        return true;
    }

    /**
     * Alter the camera's field of view.
     *
     * @param newTangent tangent of the vertical field-of-view half-angle
     * (&gt;0)
     */
    public void setFov(float newTangent) {
        Validate.positive(newTangent, "tangent");

        Camera slotCamera = slot.getCamera();
        MyCamera.setYTangent(slotCamera, newTangent);
    }

    /**
     * Alter the direction the player is looking relative to their direction of
     * motion.
     *
     * @param newDirection direction in local coordinates (not null, positive
     * length, unaffected)
     */
    public void setLookDirection(Vector3f newDirection) {
        Validate.nonZero(newDirection, "direction");
        forwardView.setLookDirection(newDirection);
    }

    /**
     * Alter the location of the player's avatar.
     *
     * @param location world coordinates (not null)
     */
    public void setPlayerLocation(Vector3f location) {
        Validate.nonNull(location, "location");
        MySpatial.setWorldLocation(avatarNode, location);
    }

    /**
     * Alter the orientation of the player's avatar.
     *
     * @param orientation orientation in world coordinate system (not null)
     */
    public void setPlayerOrientation(Quaternion orientation) {
        Validate.nonNull(orientation, "orientation");
        MySpatial.setWorldOrientation(avatarNode, orientation);
    }

    /**
     * Alter the location of the torch.
     *
     * @param newLocation (in world coordinates, not null)
     */
    public void setTorchLocation(Vector3f newLocation) {
        Validate.nonNull(newLocation, "location");
        torch.setPosition(newLocation);
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
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        slot = bigSlotState;
        Node root = slot.getRootNode();
        root.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        /*
         * Initialize materials for ceiling, floor, and walls.
         */
        if (shadedFlag) {
            ceilingMaterial = MyAsset.createShinyMaterial(assetManager,
                    ColorRGBA.White.mult(2f));
        } else {
            ceilingMaterial = MyAsset.createUnshadedMaterial(assetManager,
                    ColorRGBA.White);
        }

        Texture floorTexture = MyAsset.loadTexture(assetManager, pondAssetPath);
        if (shadedFlag) {
            floorMaterial = MyAsset.createShadedMaterial(
                    assetManager, floorTexture);
            floorMaterial.setBoolean("UseMaterialColors", true);
            floorMaterial.setColor("Diffuse", ColorRGBA.White.mult(2f));
        } else {
            floorMaterial = MyAsset.createUnshadedMaterial(
                    assetManager, floorTexture);
        }

        Texture wallTexture = MyAsset.loadTexture(assetManager, wallAssetPath);
        if (shadedFlag) {
            wallMaterial = MyAsset.createShadedMaterial(
                    assetManager, wallTexture);
            wallMaterial.setBoolean("UseMaterialColors", true);
            wallMaterial.setColor("Diffuse", ColorRGBA.White);
        } else {
            wallMaterial = MyAsset.createUnshadedMaterial(
                    assetManager, wallTexture);
        }
        /*
         * Visualize each level of the maze.
         */
        Node mazeNode = new Node("main maze");
        root.attachChild(mazeNode);

        float corridorWidth = WorldState.getCorridorWidth();
        MazeLevelView mazeLevelView = new MazeLevelView(corridorWidth,
                wallHeight, ceilingMaterial, 30f,
                floorMaterial, 5f, wallMaterial, 10f);

        NavGraph graph = worldState.getGraph();
        List<NavVertex> notDone = graph.listVertices();
        int numLevels = worldState.getNumLevels();
        for (int levelIndex = 0; levelIndex < numLevels; levelIndex++) {
            MazeLevel level = worldState.getLevel(levelIndex);
            mazeLevelView.addLevel(level, mazeNode, graph, notDone);
        }
        /*
         * Visualize ramps between levels.
         */
        assert notDone.size() == numLevels - 1 : notDone;
        for (NavVertex vertex : notDone) {
            mazeLevelView.addNonGridVertex(mazeNode, vertex);
        }

        if (debugFlag) {
            /*
             * As a debugging aid, add navigation arcs using sticks.
             */
            Material debugMaterial = MyAsset.createUnshadedMaterial(
                    assetManager, ColorRGBA.White);
            NavDebug.addSticks(graph, mazeNode, 0.5f, debugMaterial);
        }
        /*
         * Visualize free items.
         */
        for (Item item : freeItemsState.getAll()) {
            addFreeItem(item);
        }
        /*
         * Add avatar to represent the player.
         */
        root.attachChild(avatarNode);
        Vector3f location = playerState.getLocation();
        MySpatial.setWorldLocation(avatarNode, location);
        Quaternion orientation = playerState.getOrientation();
        MySpatial.setWorldOrientation(avatarNode, orientation);
        /*
         * Add lights and camera.
         */
        addLight();
        addCamera();

        if (debugFlag) {
            /*
             * As a debugging aid, dump the scene graph of this view.
             */
            Printer printer = new Printer();
            printer.setPrintTransform(true);
            printer.printSubtree(root);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the camera and view port for this view.
     */
    private void addCamera() {
        /*
         * Disable SimpleApplication's FlyByCamera,
         * which is for debug and demos, not gameplay.
         */
        flyCam.setEnabled(false);
        /*
         * Add a control for the forward-looking main camera, five world units
         * behind the avatar.
         */
        Vector3f localOffset = new Vector3f(-5f, eyeHeight, 0f);
        Camera slotCamera = slot.getCamera();
        forwardView = new CameraControl(slotCamera, localOffset,
                forwardDirection, WorldState.upDirection);
        avatarNode.addControl(forwardView);
    }

    /**
     * Add the point light source (with shadows) to represent a torch.
     */
    private void addLight() {
        Node root = slot.getRootNode();
        root.addLight(torch);

        float lightIntensity = 1f;
        ColorRGBA pointColor = ColorRGBA.White.mult(lightIntensity);
        torch.setColor(pointColor);
        torch.setName("torch");
        float attenuationRadius = 1000f; // world units
        torch.setRadius(attenuationRadius);

        if (shadowsFlag) {
            PointLightShadowRenderer plsr = new PointLightShadowRenderer(
                    assetManager, shadowMapSize);
            plsr.setLight(torch);
            plsr.setShadowIntensity(1f);
            viewPort.addProcessor(plsr);
        }
    }
}
