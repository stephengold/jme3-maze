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
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
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
import jme3utilities.controls.CameraControl;
import jme3utilities.controls.LightControl;
import jme3utilities.debug.Printer;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Simple application to play a maze game. The application's main entry point is
 * here.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Maze
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
    final private static Logger logger = Logger.getLogger(Maze.class.getName());
    /**
     * asset path to the sinbad model
     */
    final private static String sinbadPath = "Models/Sinbad/Sinbad.mesh.xml";
    /**
     * application name for its window's title bar
     */
    final private static String windowTitle = "Maze";
    /**
     * world "up" direction
     */
    final private static Vector3f upDirection = Vector3f.UNIT_Y;
    // *************************************************************************
    // fields
    /**
     * the player's starting point
     */
    private NavArc startArc;
    /**
     * the player's objectives
     */
    private NavVertex[] goalVertices;
    /**
     * the player's avatar in the main scene
     */
    final private Node mainAvatar = new Node("main avatar");
    /**
     * the player's avatar in the map scene
     */
    final private Node mapAvatar = new Node("map avatar");
    /**
     * root of the map scene
     */
    private Node mapRootNode = new Node("map root node");
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the game.
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
        Maze application = new Maze();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to Maze.simpleInitApp()!
         */
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        int mazeColumns = 12;
        int mazeRows = 12;
        initializeMaze(mazeRows, mazeColumns);

        initializeMainLights();
        initializeMapLights();
        initializeCameras();
        Node[] avatars = initializeAvatars();
        for (NavVertex goalVertex : goalVertices) {
            initializeGoal(goalVertex);
        }
        /*
         * Create three application states: input, move, and turn.
         */
        InputState inputState = new InputState(mainAvatar);

        float moveSpeed = 50f; // world units per second
        MoveState moveState = new MoveState(avatars, moveSpeed, goalVertices);

        float turnRate = 5f; // radians per second
        TurnState turnState = new TurnState(avatars, turnRate);
        /*
         * Attach the application states and activate the turn state.
         */
        stateManager.attachAll(inputState, moveState, turnState);

        NavVertex startVertex = startArc.getFromVertex();
        Vector3f direction = startArc.getStartDirection();
        turnState.activate(startVertex, direction);

        Printer p = new Printer();
        p.setPrintTransform(true);
        //p.printSubtree(rootNode);
        //p.printSubtree(mapRootNode);
    }

    @Override
    public void simpleUpdate(float elapsedTime) {
        mapRootNode.updateLogicalState(elapsedTime);
        mapRootNode.updateGeometricState();
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the player's avatars, one in each scene.
     *
     * @returns a new array of avatar spatials
     */
    private Node[] initializeAvatars() {
        /*
         * Add each avatar to the corresponding scene.
         */
        rootNode.attachChild(mainAvatar);
        mapRootNode.attachChild(mapAvatar);
        /*
         * Load a sinbad model and attach it to the player's map avatar.
         */
        Logger loaderLogger = Logger.getLogger(
                "com.jme3.scene.plugins.ogre.MeshLoader");
        loaderLogger.setLevel(Level.SEVERE);
        Node sinbad = (Node) assetManager.loadModel(sinbadPath);
        mapAvatar.attachChild(sinbad);
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(FastMath.HALF_PI, upDirection);
        sinbad.setLocalRotation(rot);
        sinbad.setLocalScale(2f);
        /*
         * Initialize their locations.
         */
        NavVertex startVertex = startArc.getFromVertex();
        Vector3f location = startVertex.getLocation();
        MySpatial.setWorldLocation(mainAvatar, location);
        MySpatial.setWorldLocation(mapAvatar, location);
        /*
         * Initialize their orientations.
         */
        Vector3f direction = startArc.getStartDirection();
        Quaternion orientation = new Quaternion();
        orientation.lookAt(direction, upDirection);
        MySpatial.setWorldOrientation(mainAvatar, orientation);
        MySpatial.setWorldOrientation(mapAvatar, orientation);

        Node[] result = new Node[2];
        result[0] = mainAvatar;
        result[1] = mapAvatar;
        return result;
    }

    /**
     * Initialize the cameras and view ports.
     */
    private void initializeCameras() {
        flyCam.setEnabled(false);
        /*
         * Position the inset view in the lower right corner of the main view.
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
         * Add a control for the forward-looking main camera.
         */
        Vector3f offset2 = new Vector3f(-8f, 4f, 0f);
        Vector3f forward = Vector3f.UNIT_X;
        CameraControl forwardView =
                new CameraControl(cam, offset2, forward, upDirection);
        mainAvatar.addControl(forwardView);
        /*
         * Add a control for the downward-looking map camera.
         */
        Vector3f offset = new Vector3f(0f, 20f, 0f);
        Vector3f down = new Vector3f(0f, -1f, 0f);
        CameraControl downView =
                new CameraControl(mapCamera, offset, down, forward);
        mapAvatar.addControl(downView);
    }

    /**
     * Create a goal spatial in each scene.
     *
     * @param goalVertex (not null)
     */
    private void initializeGoal(NavVertex goalVertex) {
        String assetPath = "Models/Teapot/Teapot.obj";
        Vector3f location = goalVertex.getLocation();
        /*
         * Spatial for the main scene
         */
        ColorRGBA mainColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        Material mainMaterial =
                MyAsset.createShinyMaterial(assetManager, mainColor);
        Spatial mainSpatial = assetManager.loadModel(assetPath);
        rootNode.attachChild(mainSpatial);
        mainSpatial.setLocalScale(4f);
        mainSpatial.setMaterial(mainMaterial);
        MySpatial.setWorldLocation(mainSpatial, location);
        /*
         * Spatial for the map scene
         */
        ColorRGBA mapColor = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);
        Material mapMaterial =
                MyAsset.createShinyMaterial(assetManager, mapColor);
        Spatial mapSpatial = assetManager.loadModel(assetPath);
        mapRootNode.attachChild(mapSpatial);
        mapSpatial.setLocalScale(8f);
        mapSpatial.setMaterial(mapMaterial);
        MySpatial.setWorldLocation(mapSpatial, location);
    }

    /**
     * Add a light source to the main scene.
     */
    private void initializeMainLights() {
        /*
         * Create a point light to represent the player's torch.
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
         * Attach the point light to the avatar using a LightControl.
         */
        Vector3f offset = Vector3f.UNIT_Y;
        Vector3f direction = Vector3f.UNIT_X;
        LightControl lightControl = new LightControl(torch, offset, direction);
        mainAvatar.addControl(lightControl);
    }

    /**
     * Add light sources to the map scene.
     */
    private void initializeMapLights() {
        /*
         * Add ambient light.
         */
        AmbientLight ambientLight = new AmbientLight();
        mapRootNode.addLight(ambientLight);
        float ambientIntensity = 1f;
        ColorRGBA ambientColor = ColorRGBA.White.mult(ambientIntensity);
        ambientLight.setColor(ambientColor);
        ambientLight.setName("map ambient");
    }

    /**
     * Create a grid-based 2-D maze, determine the player's starting point and
     * goal in it, and generate 3-D representations for it.
     *
     * @param rows number of rows on the X-axis of the grid (&gt;1)
     * @param columns number of columns on the Z-axis of the grid (&gt;1)
     */
    private void initializeMaze(int rows, int columns) {
        assert rows > 1 : rows;
        assert columns > 1 : columns;

        long seed = 13498675L;
        Random generator = new Random(seed);

        float vertexSpacing = 20f; // world units
        float baseY = 0f; // world coordinate
        GridGraph graph = new GridGraph(rows, columns, vertexSpacing, generator,
                baseY);
        /*
         * Choose a random starting point.
         */
        startArc = graph.randomArc(generator);
        NavVertex startVertex = startArc.getFromVertex();
        /*
         * Make the furthest vertex be the first (and only) goal.
         */
        NavVertex goal1 = graph.findFurthest(startVertex);
        goalVertices = new NavVertex[1];
        goalVertices[0] = goal1;
        /*
         * Generate a visible 3-D representation of the maze in the map scene.
         */
        ColorRGBA ballColor = ColorRGBA.Yellow;
        Material ballMaterial =
                MyAsset.createUnshadedMaterial(assetManager, ballColor);
        ColorRGBA stickColor = ColorRGBA.Blue;
        Material stickMaterial =
                MyAsset.createUnshadedMaterial(assetManager, stickColor);
        Node mapMazeNode = new Node("map maze");
        mapRootNode.attachChild(mapMazeNode);
        float ballRadius = 2f; // world units
        float stickRadius = 1f; // world units
        graph.makeBallsAndSticks(mapMazeNode, ballRadius, stickRadius,
                ballMaterial, stickMaterial);
        /*
         * Generate a visible 3-D representation of the maze in the main scene.
         */
        ColorRGBA ceilingColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        Material ceilingMaterial =
                MyAsset.createShinyMaterial(assetManager, ceilingColor);

        String floorAssetPath = "Textures/Terrain/Pond/Pond.jpg";
        Texture floorTexture =
                MyAsset.loadTexture(assetManager, floorAssetPath);
        Material floorMaterial =
                MyAsset.createShadedMaterial(assetManager, floorTexture);

        String wallAssetPath = "Textures/Terrain/BrickWall/BrickWall.jpg";
        Texture wallTexture = MyAsset.loadTexture(assetManager, wallAssetPath);
        Material wallMaterial =
                MyAsset.createShadedMaterial(assetManager, wallTexture);
        wallMaterial.setBoolean("UseMaterialColors", true);
        ColorRGBA wallColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        wallMaterial.setColor("Diffuse", wallColor);

        Node mazeNode = new Node("main maze");
        rootNode.attachChild(mazeNode);
        float corridorWidth = 10f; // world units
        float wallHeight = 10f; // world units
        graph.constructCeiling(mazeNode, baseY + wallHeight, ceilingMaterial);
        graph.constructFloor(mazeNode, baseY, floorMaterial);
        graph.constructWalls(mazeNode, baseY, corridorWidth, wallHeight,
                wallMaterial);
    }
}