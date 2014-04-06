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
package jme3maze;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.controls.CameraControl;
import jme3utilities.controls.LightControl;
import jme3utilities.debug.Printer;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Simple desktop application to play a maze game. The application's main entry
 * point is in this class.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MazeGame
        extends SimpleApplication {
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
            Logger.getLogger(MazeGame.class.getName());
    /**
     * default seed for pseudo-random number generator
     */
    final private static long defaultSeed = 13498675L;
    /**
     * asset path to the "pond" texture asset
     */
    final private static String pondAssetPath =
            "Textures/Terrain/Pond/Pond.jpg";
    /**
     * asset path to the "sinbad" 3D model asset
     */
    final private static String sinbadPath = "Models/Sinbad/Sinbad.mesh.xml";
    /**
     * asset path to the "teapot" 3D model asset
     */
    final private static String teapotAssetPath = "Models/Teapot/Teapot.obj";
    /**
     * asset path to the "wall" texture asset
     */
    final private static String wallAssetPath =
            "Textures/Terrain/BrickWall/BrickWall.jpg";
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maze Game";
    /**
     * local "forward" direction (unit vector)
     */
    final private static Vector3f forwardDirection = new Vector3f(0f, 0f, 1f);
    /**
     * world "up" direction (unit vector)
     */
    final private static Vector3f upDirection = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // fields
    /**
     * maze model instance
     */
    private GridGraph mazeModel;
    /**
     * the player's starting point
     */
    private NavArc startArc;
    /**
     * avatar which represents the player in the main view
     */
    final private Node mainAvatar = new Node("main avatar");
    /**
     * avatar which represents the player in the map view
     */
    final private Node mapAvatar = new Node("map avatar");
    /**
     * root of the map view's scene graph
     */
    final private Node mapRootNode = new Node("map root node");
    /**
     * player model instance
     */
    private PlayerModel playerModel;
    /**
     * pseudo-random number generator
     */
    final private Random generator = new Random(defaultSeed);
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the maze game.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        /*
         * Instantiate the application.
         */
        MazeGame application = new MazeGame();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to MazeGame.simpleInitApp()!
         */
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        /*
         * models
         */
        initializeMazeModel();
        initializePlayerModel();
        /*
         * views
         */
        initializeMainView();
        initializeMapView();
        /*
         * controllers
         */
        initializeControllers();
        /*
         * As a debugging aid, dump the scene graph for each view.
         */
        Printer printer = new Printer();
        printer.setPrintTransform(true);
        //printer.printSubtree(rootNode);
        //printer.printSubtree(mapRootNode);
    }

    /**
     * Update the map view before each render.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float elapsedTime) {
        Validate.nonNegative(elapsedTime, "interval");

        mapRootNode.updateLogicalState(elapsedTime);
        mapRootNode.updateGeometricState();
    }
    // *************************************************************************
    // private methods

    /**
     * Add a 3-D representation of a goal to the main view.
     *
     * @param goal goal to represent (not null)
     */
    private void addGoalToMainView(NavVertex goal) {
        Spatial spatial = assetManager.loadModel(teapotAssetPath);
        rootNode.attachChild(spatial);
        spatial.setLocalScale(2f);

        ColorRGBA color = new ColorRGBA(0.09f, 0.08f, 0.05f, 1f);
        Material material = MyAsset.createShinyMaterial(assetManager, color);
        spatial.setMaterial(material);

        Vector3f location = goal.getLocation();
        location.y += 4f; // floating in the air
        MySpatial.setWorldLocation(spatial, location);
    }

    /**
     * Add a 3-D representation of a goal to the map view.
     *
     * @param goal goal to represent (not null)
     */
    private void addGoalToMapView(NavVertex goal) {
        Spatial spatial = assetManager.loadModel(teapotAssetPath);
        mapRootNode.attachChild(spatial);
        spatial.setLocalScale(8f);

        ColorRGBA color = new ColorRGBA(0.9f, 0.8f, 0.5f, 1f);
        Material material = MyAsset.createShinyMaterial(assetManager, color);
        spatial.setMaterial(material);

        Vector3f location = goal.getLocation();
        MySpatial.setWorldLocation(spatial, location);
    }

    /**
     * Add a 3-D representation of the maze to the main view.
     */
    private void addMazeToMainView() {
        ColorRGBA ceilingColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        Material ceilingMaterial =
                MyAsset.createShinyMaterial(assetManager, ceilingColor);

        Texture floorTexture =
                MyAsset.loadTexture(assetManager, pondAssetPath);
        Material floorMaterial =
                MyAsset.createShadedMaterial(assetManager, floorTexture);
        floorMaterial.setBoolean("UseMaterialColors", true);
        ColorRGBA floorColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        floorMaterial.setColor("Diffuse", floorColor);

        Texture wallTexture = MyAsset.loadTexture(assetManager, wallAssetPath);
        Material wallMaterial =
                MyAsset.createShadedMaterial(assetManager, wallTexture);
        wallMaterial.setBoolean("UseMaterialColors", true);
        ColorRGBA wallColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        wallMaterial.setColor("Diffuse", wallColor);

        Node mazeNode = new Node("main maze");
        rootNode.attachChild(mazeNode);

        float floorY = 0f; // world coordinate
        float corridorWidth = 10f; // world units
        float wallHeight = 10f; // world units
        mazeModel.constructFloor(mazeNode, floorY, floorMaterial);
        mazeModel.constructWalls(mazeNode, floorY, corridorWidth, wallHeight,
                wallMaterial);
        mazeModel.constructCeiling(mazeNode, floorY + wallHeight,
                ceilingMaterial);
    }

    /**
     * Add a 3-D representation of the maze to the map view.
     */
    private void addMazeToMapView() {
        ColorRGBA ballColor = ColorRGBA.White;
        Material ballMaterial =
                MyAsset.createUnshadedMaterial(assetManager, ballColor);
        ColorRGBA stickColor = ColorRGBA.Blue;
        Material stickMaterial =
                MyAsset.createUnshadedMaterial(assetManager, stickColor);
        Node mapMazeNode = new Node("map maze");
        mapRootNode.attachChild(mapMazeNode);
        float ballRadius = 2f; // world units
        float stickRadius = 1f; // world units
        mazeModel.makeBallsAndSticks(mapMazeNode, ballRadius, stickRadius,
                ballMaterial, stickMaterial);
    }

    /**
     * Initialize the control instances of the maze game.
     */
    private void initializeControllers() {
        /*
         * Add a player control to the main avatar.
         */
        PlayerControl mainPlayerControl = new PlayerControl(playerModel);
        mainAvatar.addControl(mainPlayerControl);
        /*
         * Add a player control to the map avatar.
         */
        PlayerControl mapPlayerControl = new PlayerControl(playerModel);
        mapAvatar.addControl(mapPlayerControl);
        /*
         * Attach three app states to the application.
         */
        InputState inputState = new InputState(playerModel);
        MoveState moveState = new MoveState(playerModel);
        TurnState turnState = new TurnState(playerModel);
        stateManager.attachAll(inputState, moveState, turnState);
        /*
         * Activate the turn app state.
         */
        NavVertex startVertex = startArc.getFromVertex();
        Vector3f direction = startArc.getStartDirection();
        turnState.activate(startVertex, direction);
    }

    /**
     * Initialize the camera for the main view.
     */
    private void initializeMainCamera() {
        flyCam.setEnabled(false);
        /*
         * Add a control for the forward-looking main camera.
         */
        Vector3f localOffset = new Vector3f(0f, 5f, -8f);
        CameraControl forwardView = new CameraControl(cam, localOffset, 
                forwardDirection, upDirection);
        mainAvatar.addControl(forwardView);
    }

    /**
     * Add a light source to the main scene.
     */
    private void initializeMainLights() {
        /*
         * Create a point light source to represent the player's torch.
         */
        PointLight torch = new PointLight();
        rootNode.addLight(torch);
        float pointIntensity = 10f;
        ColorRGBA pointColor = ColorRGBA.White.mult(pointIntensity);
        torch.setColor(pointColor);
        torch.setName("torch");
        float attenuationRadius = 1000f; // world units
        torch.setRadius(attenuationRadius);
        /*
         * Attach the light source to the avatar using a LightControl.
         */
        Vector3f localOffset = new Vector3f(0f, 6f, -8f);
        LightControl lightControl =
                new LightControl(torch, localOffset, forwardDirection);
        mainAvatar.addControl(lightControl);
    }

    /**
     * Initialize the main (first-person) view.
     */
    private void initializeMainView() {
        /*
         * Generate a 3D representation of the maze.
         */
        addMazeToMainView();
        /*
         * Add an avatar to represent the player.
         */
        rootNode.attachChild(mainAvatar);
        /*
         * Load assets to represent the player's goals.
         */
        NavVertex[] goals = playerModel.getGoals();
        for (NavVertex goal : goals) {
            addGoalToMainView(goal);
        }
        /*
         * Initialize lights and cameras.
         */
        initializeMainLights();
        initializeMainCamera();
    }

    /**
     * Initialize the camera and view port for the map view.
     */
    private void initializeMapCamera() {
        /*
         * Position the inset view port in the upper left corner
         * of the main view port.
         */
        Camera mapCamera = cam.clone();
        mapCamera.setParallelProjection(true);
        float x1 = 0.05f;
        float x2 = 0.35f;
        float y1 = 0.65f;
        float y2 = 0.95f;
        mapCamera.setViewPort(x1, x2, y1, y2);
        float tanYfov = 50f; // world units
        MyCamera.setYTangent(mapCamera, tanYfov);

        ViewPort insetView =
                renderManager.createMainView("inset", mapCamera);
        insetView.attachScene(mapRootNode);
        insetView.setBackgroundColor(mapBackground);
        insetView.setClearFlags(true, true, true);
        /*
         * Add a control for the downward-looking map camera.
         */
        Vector3f offset = new Vector3f(0f, 20f, 0f);
        Vector3f down = new Vector3f(0f, -1f, 0f);
        CameraControl downView =
                new CameraControl(mapCamera, offset, down, forwardDirection);
        mapAvatar.addControl(downView);
    }

    /**
     * Add ambient light to the map scene.
     */
    private void initializeMapLights() {
        AmbientLight ambientLight = new AmbientLight();
        mapRootNode.addLight(ambientLight);
        float ambientIntensity = 1f;
        ColorRGBA ambientColor = ColorRGBA.White.mult(ambientIntensity);
        ambientLight.setColor(ambientColor);
        ambientLight.setName("map ambient");
    }

    /**
     * Initialize the (inset) map view.
     */
    private void initializeMapView() {
        /*
         * Generate a 3D representation of the maze.
         */
        addMazeToMapView();
        /*
         * Add an avatar to represent the player.
         */
        mapRootNode.attachChild(mapAvatar);
        /*
         * Load the "sinbad" asset and attach it to the avatar.
         */
        Logger loaderLogger = Logger.getLogger(
                "com.jme3.scene.plugins.ogre.MeshLoader");
        loaderLogger.setLevel(Level.SEVERE);
        Node sinbad = (Node) assetManager.loadModel(sinbadPath);
        mapAvatar.attachChild(sinbad);
        sinbad.setLocalScale(2f);
        /*
         * Load assets to represent the player's goals.
         */
        NavVertex[] goals = playerModel.getGoals();
        for (NavVertex goal : goals) {
            addGoalToMapView(goal);
        }
        /*
         * Initialize lights and cameras.
         */
        initializeMapLights();
        initializeMapCamera();
    }

    /**
     * Create the model instance for the maze.
     */
    private void initializeMazeModel() {
        int mazeColumns = 12;
        int mazeRows = 12;
        float vertexSpacing = 20f; // world units
        float baseY = 0f; // world coordinate
        mazeModel = new GridGraph(mazeRows, mazeColumns, vertexSpacing,
                generator, baseY);
    }

    /**
     * Initialize the model data for the player.
     */
    private void initializePlayerModel() {
        /*
         * Choose a random starting point.
         */
        startArc = mazeModel.randomArc(generator);
        NavVertex startVertex = startArc.getFromVertex();
        /*
         * Let the vertex furthest from the starting point
         * be the first (and only) goal.
         */
        NavVertex[] goalVertices = new NavVertex[1];
        goalVertices[0] = mazeModel.findFurthest(startVertex);
        /*
         * Create the player model instance.
         */
        playerModel = new PlayerModel(startArc, goalVertices);
    }
}