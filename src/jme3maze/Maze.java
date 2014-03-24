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
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
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
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavNode;

/**
 * Simple application to play a maze game, a demo for app states and the
 * navigation library. The application's main entry point is here.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class Maze
        extends SimpleApplication {
    // *************************************************************************
    // constants

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
    private NavArc startArc = null;
    /**
     * the player's objective
     */
    private NavNode goalNode = null;
    /**
     * the player's avatar
     */
    private Node cameraNode = null;
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
        initializeMaze(12, 12);

        initializeLights(rootNode);
        initializeCameras();
        initializeAvatar();
        initializeGoal();
        /*
         * Create three app states: input, move, and turn.
         */
        InputState inputState = new InputState(cameraNode);

        float moveSpeed = 50f; // world units per second
        MoveState moveState = new MoveState(cameraNode, moveSpeed, goalNode);

        float turnRate = 3f; // radians per second
        TurnState turnState = new TurnState(cameraNode, turnRate);
        /*
         * Attach the app states and activate one of them.
         */
        stateManager.attachAll(inputState, moveState, turnState);
        NavNode startNode = startArc.getFromNode();
        Vector3f direction = startArc.getStartDirection();
        turnState.activate(startNode, direction);
    }
    // *************************************************************************
    // private methods

    private Material createShinyMaterial(ColorRGBA color) {
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
     * Create an avatar and attach it to the camera node.
     */
    private void initializeAvatar() {
        Logger log = Logger.getLogger("com.jme3.scene.plugins.ogre.MeshLoader");
        log.setLevel(Level.SEVERE);

        Node sinbad = (Node) assetManager.loadModel(sinbadPath);
        cameraNode.attachChild(sinbad);
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        sinbad.setLocalRotation(rot);
        sinbad.setLocalScale(2f);
    }

    /**
     * Initialize the camera node and the cameras.
     */
    private void initializeCameras() {
        flyCam.setEnabled(false);

        //float tanYfov = 2f;
        float tanYfov = 100f;
        MyCamera.setYTangent(cam, tanYfov);
        cam.setParallelProjection(true);

        cameraNode = new Node("camera node");
        rootNode.attachChild(cameraNode);

        NavNode startNode = startArc.getFromNode();
        Vector3f location = startNode.getLocation();
        MySpatial.setWorldLocation(cameraNode, location);

        Vector3f direction = startArc.getStartDirection();
        Vector3f up = Vector3f.UNIT_Y;
        Quaternion orientation = new Quaternion();
        orientation.lookAt(direction, up);
        MySpatial.setWorldOrientation(cameraNode, orientation);

        Vector3f offset = new Vector3f(0f, 20f, 0f);
        Vector3f down = new Vector3f(0f, -1f, 0f);
        Vector3f forward = Vector3f.UNIT_X;
        CameraControl downView = new CameraControl(cam, offset, down, forward);
        cameraNode.addControl(downView);
    }

    /**
     * Create a goal and attach it to the scene.
     */
    private void initializeGoal() {
        Material goalMaterial = createShinyMaterial(ColorRGBA.Gray);

        Spatial teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        rootNode.attachChild(teapot);
        teapot.setLocalScale(8f);
        teapot.setMaterial(goalMaterial);
        Vector3f location = goalNode.getLocation();
        MySpatial.setWorldLocation(teapot, location);
    }

    /**
     * Add two white light sources to a scene: directional and ambient.
     *
     * @param lightSpatial where to add lights (not null)
     */
    private void initializeLights(Spatial lightSpatial) {
        /*
         * Add directional light.
         */
        DirectionalLight light = new DirectionalLight();
        lightSpatial.addLight(light);

        Vector3f direction = new Vector3f(1f, -1f, 1f).normalizeLocal();
        float directionalIntensity = 1f;
        ColorRGBA directionalColor =
                ColorRGBA.White.multLocal(directionalIntensity);
        light.setColor(directionalColor);
        light.setDirection(direction);
        /*
         * Add ambient light.
         */
        AmbientLight ambient = new AmbientLight();
        lightSpatial.addLight(ambient);
        float ambientIntensity = 0.2f;
        ColorRGBA ambientColor = ColorRGBA.White.multLocal(ambientIntensity);
        ambient.setColor(ambientColor);
    }

    /**
     * Create a grid-based 2-D maze and determine the player's starting point
     * and goal.
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
         * Chose a starting point and a goal.
         */
        startArc = graph.randomArc(generator);
        NavNode startNode = startArc.getFromNode();
        goalNode = graph.findFurthest(startNode);
        /*
         * Generate a visible 3-D representation of the maze.
         */
        Material ballMaterial = createShinyMaterial(ColorRGBA.Yellow);
        Material stickMaterial = createShinyMaterial(ColorRGBA.Blue);

        Node mazeNode = new Node("maze");
        rootNode.attachChild(mazeNode);
        float ballRadius = 2f;
        float stickRadius = 1f;
        graph.makeBallsAndSticks(mazeNode, ballRadius, stickRadius,
                ballMaterial, stickMaterial);
    }
}