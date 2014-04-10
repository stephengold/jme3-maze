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
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.model.FreeItemsState;
import jme3maze.model.GridGraph;
import jme3maze.model.Item;
import jme3maze.model.PlayerState;
import jme3maze.model.WorldState;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.controls.CameraControl;
import jme3utilities.debug.Printer;
import jme3utilities.navigation.NavDebug;
import jme3utilities.navigation.NavVertex;

/**
 * App state to manager the (inset) map view in the Maze Game.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MapViewState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * background color for the map; dark red
     */
    final private static ColorRGBA mapBackground =
            new ColorRGBA(0.3f, 0f, 0f, 1f);
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MapViewState.class.getName());
    /**
     * asset path to the "sinbad" 3D model asset in jME3-testdata.jar
     */
    final private static String sinbadPath = "Models/Sinbad/Sinbad.mesh.xml";
    /**
     * asset path to the "teapot" 3D model asset in jME3-testdata.jar
     */
    final private static String teapotAssetPath = "Models/Teapot/Teapot.obj";
    /**
     * local "forward" direction (unit vector)
     */
    final private static Vector3f forwardDirection = Vector3f.UNIT_Z;
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
     * interval between updates (in seconds, &ge;0)
     */
    private float updateInterval = 0f;
    /**
     * map free items to their spatials
     */
    private Map<Item, Spatial> itemSpatial = new TreeMap<>();
    /**
     * node which represents the player in this view
     */
    final private Node avatarNode = new Node("map avatar node");
    /**
     * root of this view's scene graph
     */
    final private Node rootNode = new Node("map root node");
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
        FreeItemsState freeItemsState =
                stateManager.getState(FreeItemsState.class);
        NavVertex vertex = freeItemsState.getVertex(item);
        Vector3f location = vertex.getLocation();
        String itemType = item.getTypeName();
        ColorRGBA color;
        Material material;
        switch (itemType) {
            case "Mapper":
                /*
                 * A Mapper is represented by a green cube.
                 */
                Mesh mesh = new Box(1f, 1f, 1f);
                Geometry cube = new Geometry("mapper", mesh);
                itemSpatial.put(item, cube);
                rootNode.attachChild(cube);
                cube.setLocalScale(0.5f);
                color = new ColorRGBA(0f, 0.05f, 0f, 1f);
                material = MyAsset.createShinyMaterial(assetManager, color);
                cube.setMaterial(material);

                location.y += 4f; // floating in the air
                MySpatial.setWorldLocation(cube, location);
                break;

            case "McGuffin":
                /*
                 * A McGuffin is represented by a gold teapot.
                 */
                Spatial spatial = assetManager.loadModel(teapotAssetPath);
                itemSpatial.put(item, spatial);
                rootNode.attachChild(spatial);
                spatial.setLocalScale(8f);

                color = new ColorRGBA(0.9f, 0.8f, 0.5f, 1f);
                material = MyAsset.createShinyMaterial(assetManager, color);
                spatial.setMaterial(material);
                MySpatial.setWorldLocation(spatial, location);
        }
    }

    /**
     * Remove 3-D representation of a free item from the scene.
     *
     * @param item free item to remove (not null)
     */
    public void removeFreeItem(Item item) {
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
        assetManager = application.getAssetManager();
        this.stateManager = stateManager;

        initializeView();
        /*
         * As a debugging aid, dump the scene graph of this view.
         */
        Printer printer = new Printer();
        printer.setPrintTransform(true);
        //printer.printSubtree(rootNode);
    }

    /**
     * Update this view before each render.
     *
     * @param renderManager (not null)
     */
    @Override
    public void render(RenderManager renderManager) {
        Validate.nonNull(renderManager, "render manager");
        /*
         * Update logical state here where we can be sure all controls
         * have been updated.
         */
        rootNode.updateLogicalState(updateInterval);
        rootNode.updateGeometricState();
    }

    /**
     * Update this view before each render.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        Validate.nonNegative(elapsedTime, "interval");
        updateInterval = elapsedTime;
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the camera and view port for this view.
     */
    private void addCamera() {
        Camera mapCamera = application.getCamera().clone();
        mapCamera.setParallelProjection(true);
        /*
         * Position the inset view port in the upper left corner
         * of the main view port.
         */
        float x1 = 0.05f;
        float x2 = 0.35f;
        float y1 = 0.65f;
        float y2 = 0.95f;
        mapCamera.setViewPort(x1, x2, y1, y2);
        float tanYfov = 50f; // world units
        MyCamera.setYTangent(mapCamera, tanYfov);

        RenderManager renderManager = application.getRenderManager();
        ViewPort insetView = renderManager.createMainView("inset", mapCamera);
        insetView.attachScene(rootNode);
        insetView.setBackgroundColor(mapBackground);
        insetView.setClearFlags(true, true, true);
        /*
         * Add a control for the downward-looking map camera.
         */
        Vector3f offset = new Vector3f(0f, 20f, 0f);
        Vector3f down = new Vector3f(0f, -1f, 0f);
        CameraControl downView =
                new CameraControl(mapCamera, offset, down, forwardDirection);
        avatarNode.addControl(downView);
    }

    /**
     * Add ambient light to the scene.
     */
    private void addLight() {
        AmbientLight ambientLight = new AmbientLight();
        rootNode.addLight(ambientLight);
        float ambientIntensity = 1f;
        ColorRGBA ambientColor = ColorRGBA.White.mult(ambientIntensity);
        ambientLight.setColor(ambientColor);
        ambientLight.setName("map ambient");
    }

    /**
     * Add 3-D representation of a maze to the scene.
     */
    private void addMaze(GridGraph maze) {
        ColorRGBA ballColor = ColorRGBA.White;
        Material ballMaterial =
                MyAsset.createUnshadedMaterial(assetManager, ballColor);
        ColorRGBA stickColor = ColorRGBA.Blue;
        Material stickMaterial =
                MyAsset.createUnshadedMaterial(assetManager, stickColor);
        Node mapMazeNode = new Node("map maze");
        rootNode.attachChild(mapMazeNode);

        float ballRadius = 2f; // world units
        float stickRadius = 1f; // world units
        NavDebug.makeBalls(maze, mapMazeNode, ballRadius, ballMaterial);
        NavDebug.makeSticks(maze, mapMazeNode, stickRadius, stickMaterial);
    }

    /**
     * Initialize this view.
     */
    private void initializeView() {
        /*
         * Generate a 3D representation of the maze.
         */
        WorldState worldState = stateManager.getState(WorldState.class);
        GridGraph maze = worldState.getMaze();
        addMaze(maze);
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
         * Load the "sinbad" asset and attach it to the avatar.
         */
        Logger loaderLogger = Logger.getLogger(
                "com.jme3.scene.plugins.ogre.MeshLoader");
        loaderLogger.setLevel(Level.SEVERE);
        Node sinbad = (Node) assetManager.loadModel(sinbadPath);
        avatarNode.attachChild(sinbad);
        sinbad.setLocalScale(2f);
        /*
         * Add the free items.
         */
        FreeItemsState freeItemsState =
                stateManager.getState(FreeItemsState.class);
        for (Item item : freeItemsState.getAll()) {
            addFreeItem(item);
        }
        /*
         * Add light and camera.
         */
        addLight();
        addCamera();
    }
}