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
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.MazeGame;
import jme3maze.model.GridGraph;
import jme3maze.model.Item;
import jme3maze.model.World;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.controls.CameraControl;
import jme3utilities.debug.Printer;
import jme3utilities.navigation.NavDebug;
import jme3utilities.navigation.NavVertex;

/**
 * Application state to update the (inset) map view in the Maze Game.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MapState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * background color for the map
     */
    final private static ColorRGBA mapBackground =
            new ColorRGBA(0.3f, 0f, 0f, 1f);
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MapState.class.getName());
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
    final private static Vector3f forwardDirection = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // fields
    /**
     * interval between updates (in seconds, &ge;0)
     */
    private float updateInterval = 0f;
    /**
     * attaching application: set by initialize()
     */
    private MazeGame application = null;
    /**
     * node which represents the player in this view
     */
    final private Node avatarNode = new Node("map avatar node");
    /**
     * root of this view's scene graph
     */
    final private Node rootNode;
    // *************************************************************************
    // constructor

    /**
     * Instantiate an enabled view.
     *
     * @param rootNode (not null)
     */
    public MapState(Node rootNode) {
        assert rootNode != null;
        this.rootNode = rootNode;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access the node which represents the player in this view.
     *
     * @return (not null)
     */
    public Node getAvatarNode() {
        assert avatarNode != null;
        return avatarNode;
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

        this.application = (MazeGame) application;
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
     * @param renderManager
     */
    @Override
    public void render(RenderManager renderManager) {
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
     * Add a 3-D representation of a free item to the scene.
     *
     * @param item free item to represent (not null)
     */
    private void addFreeItem(Item item) {
        AssetManager assetManager = application.getAssetManager();
        World world = application.getWorld();
        NavVertex vertex = world.getFreeItems().getVertex(item);
        Vector3f location = vertex.getLocation();
        String itemType = item.getTypeName();
        switch (itemType) {
            case "McGuffin":
                /*
                 * A McGuffin is represented by a gold teapot.
                 */
                Spatial spatial = assetManager.loadModel(teapotAssetPath);
                rootNode.attachChild(spatial);
                spatial.setLocalScale(8f);

                ColorRGBA color = new ColorRGBA(0.9f, 0.8f, 0.5f, 1f);
                Material material = MyAsset.createShinyMaterial(assetManager,
                        color);
                spatial.setMaterial(material);
                MySpatial.setWorldLocation(spatial, location);
        }
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
        AssetManager assetManager = application.getAssetManager();
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
        GridGraph maze = application.getWorld().getMaze();
        addMaze(maze);
        /*
         * Load the "sinbad" asset and attach it to the scene.
         */
        rootNode.attachChild(avatarNode);
        Logger loaderLogger = Logger.getLogger(
                "com.jme3.scene.plugins.ogre.MeshLoader");
        loaderLogger.setLevel(Level.SEVERE);
        AssetManager assetManager = application.getAssetManager();
        Node sinbad = (Node) assetManager.loadModel(sinbadPath);
        avatarNode.attachChild(sinbad);
        sinbad.setLocalScale(2f);
        /*
         * Add the free items.
         */
        World world = application.getWorld();
        for (Item item : world.getFreeItems().getItems()) {
            addFreeItem(item);
        }
        /*
         * Add light and camera.
         */
        addLight();
        addCamera();
    }
}