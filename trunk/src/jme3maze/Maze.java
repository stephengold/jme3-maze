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
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import static jme3utilities.MyAsset.shadedMaterialAssetPath;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.controls.CameraControl;
import jme3utilities.controls.LightControl;
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
    // *************************************************************************
    // fields
    /**
     * the player's starting point
     */
    private NavArc startArc;
    /**
     * the player's objective
     */
    private NavVertex goalVertex;
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
        initializeGoal();
        /*
         * Create three application states: input, move, and turn.
         */
        InputState inputState = new InputState(mainAvatar);

        float moveSpeed = 50f; // world units per second
        MoveState moveState = new MoveState(avatars, moveSpeed, goalVertex);

        float turnRate = 5f; // radians per second
        TurnState turnState = new TurnState(avatars, turnRate);
        /*
         * Attach the application states and activate the turn state.
         */
        stateManager.attachAll(inputState, moveState, turnState);

        NavVertex startVertex = startArc.getFromVertex();
        Vector3f direction = startArc.getStartDirection();
        turnState.activate(startVertex, direction);

        //new Printer().printSubtree(rootNode);
        //new Printer().printSubtree(mapRootNode);
    }

    @Override
    public void simpleUpdate(float elapsedTime) {
        mapRootNode.updateLogicalState(elapsedTime);
        mapRootNode.updateGeometricState();
    }
    // *************************************************************************
    // private methods

    /**
     * Create a shiny lit material with the specified color.
     *
     * @param color (not null)
     * @return a new instance
     */
    private Material createShinyMaterial(ColorRGBA color) {
        assert color != null;

        Material result = new Material(assetManager,
                shadedMaterialAssetPath);
        result.setBoolean("UseMaterialColors", true);
        result.setColor("Ambient", color);
        result.setColor("Diffuse", color);
        result.setColor("Specular", ColorRGBA.White);
        result.setFloat("Shininess", 1f);

        return result;
    }

    /**
     * Initialize the player's avatars, one for each scene.
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
        rot.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
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
        Vector3f up = Vector3f.UNIT_Y;
        Quaternion orientation = new Quaternion();
        orientation.lookAt(direction, up);
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
        float tanYfov = 50f;
        MyCamera.setYTangent(mapCamera, tanYfov);

        ViewPort insetView =
                renderManager.createMainView("inset", mapCamera);
        insetView.attachScene(mapRootNode);
        insetView.setBackgroundColor(mapBackground);
        insetView.setClearFlags(true, true, true);
        /*
         * Add a control for the forward-looking main camera.
         */
        Vector3f offset2 = new Vector3f(2f, 4f, 0f);
        Vector3f forward = Vector3f.UNIT_X;
        Vector3f up = Vector3f.UNIT_Y;
        CameraControl forwardView =
                new CameraControl(cam, offset2, forward, up);
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
     * Create a goal and attach it to both scenes.
     */
    private void initializeGoal() {
        Material goalMaterial = createShinyMaterial(ColorRGBA.Gray);

        Spatial teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        rootNode.attachChild(teapot);
        teapot.setLocalScale(8f);
        teapot.setMaterial(goalMaterial);

        Spatial teapot2 = assetManager.loadModel("Models/Teapot/Teapot.obj");
        mapRootNode.attachChild(teapot2);
        teapot2.setLocalScale(8f);
        teapot2.setMaterial(goalMaterial);

        Vector3f location = goalVertex.getLocation();
        MySpatial.setWorldLocation(teapot, location);
        MySpatial.setWorldLocation(teapot2, location);
    }

    /**
     * Add light sources to the main scene.
     */
    private void initializeMainLights() {
        /*
         * Create a point light.
         */
        PointLight pointLight = new PointLight();
        rootNode.addLight(pointLight);
        float pointIntensity = 1f;
        ColorRGBA pointColor = ColorRGBA.White.mult(pointIntensity);
        pointLight.setColor(pointColor);
        pointLight.setName("torch");
        /*
         * Attach the point light to the avatar using a LightControl.
         */
        Vector3f offset = Vector3f.UNIT_Y;
        Vector3f direction = Vector3f.UNIT_X;
        LightControl lightControl =
                new LightControl(pointLight, offset, direction);
        mainAvatar.addControl(lightControl);
        /*
         * Add ambient light.
         */
        AmbientLight ambientLight = new AmbientLight();
        //rootNode.addLight(ambientLight);
        float ambientIntensity = 0.2f;
        ColorRGBA ambientColor = ColorRGBA.White.mult(ambientIntensity);
        ambientLight.setColor(ambientColor);
        ambientLight.setName("main ambient");
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
     */
    private void initializeMaze(int rows, int columns) {
        long seed = 13498675L;
        Random generator = new Random(seed);

        float rowSpacing = 20f;
        float columnSpacing = rowSpacing;
        float baseY = 0f;
        GridGraph graph = new GridGraph(rows, columns, rowSpacing,
                columnSpacing, generator, baseY);
        /*
         * Choose a random starting point.
         */
        startArc = graph.randomArc(generator);
        NavVertex startVertex = startArc.getFromVertex();
        /*
         * Make the furthest vertex be the goal.
         */
        goalVertex = graph.findFurthest(startVertex);
        /*
         * Generate visible 3-D representations of the maze, one for each scene.
         */
        float ballRadius = 2f;
        float stickRadius = 1f;
        Material ballMaterial = createShinyMaterial(ColorRGBA.Yellow);
        Material stickMaterial = createShinyMaterial(ColorRGBA.Blue);

        Node mazeNode = new Node("main maze");
        rootNode.attachChild(mazeNode);
        graph.makeBallsAndSticks(mazeNode, ballRadius, stickRadius,
                ballMaterial, stickMaterial);

        Node mapMazeNode = new Node("map maze");
        mapRootNode.attachChild(mapMazeNode);
        graph.makeBallsAndSticks(mapMazeNode, ballRadius, stickRadius,
                ballMaterial, stickMaterial);
    }
}