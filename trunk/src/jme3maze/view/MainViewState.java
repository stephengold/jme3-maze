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
package jme3maze.view;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.texture.Texture;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3maze.items.Item;
import jme3maze.model.FreeItemsState;
import jme3maze.model.GridGraph;
import jme3maze.model.PlayerState;
import jme3maze.model.WorldState;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.controls.CameraControl;
import jme3utilities.controls.LightControl;
import jme3utilities.debug.Printer;
import jme3utilities.navigation.NavDebug;
import jme3utilities.navigation.NavGraph;
import jme3utilities.navigation.NavVertex;

/**
 * App state to manage the (1st-person) main view in the Maze Game.
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MainViewState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * flag to enable debug features
     */
    final private static boolean debugFlag = false;
    /**
     * flag to enable shadows
     */
    final private static boolean shadowsFlag = true;
    /**
     * width of maze corridors (in world units)
     */
    final private static float corridorWidth = 10f;
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
    final private static Logger logger =
            Logger.getLogger(MainViewState.class.getName());
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
     * local "forward" direction (unit vector)
     */
    final private static Vector3f forwardDirection = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // fields
    /**
     * app state manager: set by initialize()
     */
    private AppStateManager stateManager;
    /**
     * asset manager: set by initialize()
     */
    private AssetManager assetManager;
    /**
     * map free items to their spatials
     */
    private Map<Item, Spatial> itemSpatial = new TreeMap<>();
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
     * root of this view's scene graph: set by initialize()
     */
    private Node rootNode;
    /**
     * node which represents the player in this view
     */
    final private Node avatarNode = new Node("main avatar node");
    /**
     * point light source for this view
     */
    final private PointLight torch = new PointLight();
    /**
     * attaching application: set by initialize()
     */
    private SimpleApplication application;
    // *************************************************************************
    // new methods exposed

    /**
     * Add 3-D representation of a free item to the scene.
     *
     * @param item free item to represent (not null)
     */
    public void addFreeItem(Item item) {
        Spatial spatial = item.visualizeMain();
        FreeItemsState freeItemsState =
                stateManager.getState(FreeItemsState.class);
        NavVertex vertex = freeItemsState.getVertex(item);
        Vector3f location = vertex.getLocation();
        spatial.move(location);

        itemSpatial.put(item, spatial);
        rootNode.attachChild(spatial);
    }

    /**
     * Access the scene's light source.
     */
    public Light getLight() {
        return torch;
    }

    /**
     * Remove 3-D representation of a free item from the scene.
     *
     * @param item free item to remove (not null)
     */
    public void removeFreeItem(Item item) {
        Validate.nonNull(item, "item");
        Spatial spatial = itemSpatial.remove(item);
        spatial.removeFromParent();
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
     * Attach the light source to the avatar using a LightControl.
     */
    public void takeTorch() {
        Vector3f localOffset = new Vector3f(2f, 7f, -3f);
        LightControl lightControl =
                new LightControl(torch, localOffset, forwardDirection);
        avatarNode.addControl(lightControl);
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this state prior to its first update.
     *
     * @param stateManager (not null)
     * @param application attaching application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isInitialized()) {
            throw new IllegalStateException("already initialized");
        }
        Validate.nonNull(application, "application");
        Validate.nonNull(stateManager, "state manager");
        super.initialize(stateManager, application);

        this.application = (SimpleApplication) application;
        this.stateManager = stateManager;

        assetManager = application.getAssetManager();
        rootNode = this.application.getRootNode();
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        /*
         * Initialize shared materials.
         */
        ceilingMaterial =
                MyAsset.createShinyMaterial(assetManager, ColorRGBA.White);

        Texture floorTexture =
                MyAsset.loadTexture(assetManager, pondAssetPath);
        floorMaterial =
                MyAsset.createShadedMaterial(assetManager, floorTexture);
        floorMaterial.setBoolean("UseMaterialColors", true);
        floorMaterial.setColor("Diffuse", ColorRGBA.White);

        Texture wallTexture = MyAsset.loadTexture(assetManager, wallAssetPath);
        wallMaterial = MyAsset.createShadedMaterial(assetManager, wallTexture);
        wallMaterial.setBoolean("UseMaterialColors", true);
        wallMaterial.setColor("Diffuse", ColorRGBA.White);
        /*
         * Generate a 3-D representation of each maze level.
         */
        Node mazeNode = new Node("main maze");
        rootNode.attachChild(mazeNode);

        WorldState worldState = stateManager.getState(WorldState.class);
        int numLevels = worldState.getNumLevels();
        for (int levelIndex = 0; levelIndex < numLevels; levelIndex++) {
            GridGraph level = worldState.getLevel(levelIndex);
            addMazeLevel(level, mazeNode);
        }
        /*
         * Add free items.
         */
        FreeItemsState freeItemsState =
                stateManager.getState(FreeItemsState.class);
        for (Item item : freeItemsState.getAll()) {
            addFreeItem(item);
        }
        /*
         * Add avatar to represent the player.
         */
        rootNode.attachChild(avatarNode);
        PlayerState playerState = stateManager.getState(PlayerState.class);
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
            printer.printSubtree(rootNode);
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
         * which is for simple demos, not games.
         */
        application.getFlyByCamera().setEnabled(false);
        /*
         * Add a control for the forward-looking main camera.
         */
        Camera cam = application.getCamera();
        Vector3f localOffset = new Vector3f(0f, eyeHeight, -5f);
        CameraControl forwardView = new CameraControl(cam, localOffset,
                forwardDirection, WorldState.upDirection);
        avatarNode.addControl(forwardView);
    }

    /**
     * Add the point light source (with shadows) to represent a torch.
     */
    private void addLight() {
        rootNode.addLight(torch);

        float lightIntensity = 1f;
        ColorRGBA pointColor = ColorRGBA.White.mult(lightIntensity);
        torch.setColor(pointColor);
        torch.setName("torch");
        float attenuationRadius = 1000f; // world units
        torch.setRadius(attenuationRadius);

        if (shadowsFlag) {
            PointLightShadowRenderer plsr =
                    new PointLightShadowRenderer(assetManager, shadowMapSize);
            plsr.setLight(torch);
            ViewPort viewPort = application.getViewPort();
            viewPort.addProcessor(plsr);
        }
    }

    /**
     * Add 3-D representation of a maze level to the scene.
     *
     * @param level level to represent (not null)
     * @param mazeNode where in the scene graph to add (not null)
     */
    private void addMazeLevel(GridGraph level, Node mazeNode) {
        assert level != null;
        assert mazeNode != null;

        float floorY = level.getFloorY();

        FloorView floorView = new FloorView(floorMaterial);
        floorView.visualize(level, mazeNode);

        WallsView wallsView =
                new WallsView(corridorWidth, wallHeight, wallMaterial);
        wallsView.visualize(level, mazeNode);

        CeilingView ceilingView = new CeilingView(wallHeight, ceilingMaterial);
        ceilingView.visualize(level, mazeNode);

        if (debugFlag) {
            WorldState worldState = stateManager.getState(WorldState.class);
            NavGraph graph = worldState.getGraph();
            Material debugMaterial = MyAsset.createUnshadedMaterial(
                    assetManager, ColorRGBA.White);
            NavDebug.addSticks(graph, mazeNode, 0.5f, debugMaterial);
        }
    }
}