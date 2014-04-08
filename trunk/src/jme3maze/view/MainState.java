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
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.MazeGame;
import jme3maze.model.GridGraph;
import jme3maze.model.Item;
import jme3maze.model.World;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.controls.CameraControl;
import jme3utilities.controls.LightControl;
import jme3utilities.debug.Printer;
import jme3utilities.navigation.NavVertex;

/**
 * Application state to update the (1st-person) main view in the Maze Game.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MainState
        extends AbstractAppState {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MainState.class.getName());
    /**
     * asset path to the "pond" texture asset in jME3-testdata.jar
     */
    final private static String pondAssetPath =
            "Textures/Terrain/Pond/Pond.jpg";
    /**
     * asset path to the "teapot" 3D model asset in jME3-testdata.jar
     */
    final private static String teapotAssetPath = "Models/Teapot/Teapot.obj";
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
     * attaching application: set by initialize()
     */
    private MazeGame application = null;
    /**
     * node which represents the player in this view
     */
    final private Node avatarNode = new Node("main avatar node");
    /**
     * root of this view's scene graph: set by constructor
     */
    final private Node rootNode;
    // *************************************************************************
    // constructor

    /**
     * Instantiate an enabled view.
     *
     * @param rootNode (not null)
     */
    public MainState(Node rootNode) {
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
        Vector3f localOffset = new Vector3f(0f, 5f, -8f);
        CameraControl forwardView = new CameraControl(cam, localOffset,
                forwardDirection, World.upDirection);
        avatarNode.addControl(forwardView);
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
                spatial.setLocalScale(2f);

                ColorRGBA color = new ColorRGBA(0.09f, 0.08f, 0.05f, 1f);
                Material material =
                        MyAsset.createShinyMaterial(assetManager, color);
                spatial.setMaterial(material);

                location.y += 4f; // floating in the air
                MySpatial.setWorldLocation(spatial, location);
                break;

            default:
                logger.log(Level.WARNING,
                        "ignored free item with unknown type {0}",
                        MyString.quote(itemType));
        }
    }

    /**
     * Add light sources to the scene.
     */
    private void addLights() {
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
        avatarNode.addControl(lightControl);
    }

    /**
     * Add 3-D representation of a maze to the scene.
     */
    private void addMaze(GridGraph maze) {
        AssetManager assetManager = application.getAssetManager();

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
        float ceilingY = floorY + wallHeight; // world coordinate

        FloorView floorView = new FloorView(floorY, floorMaterial);
        floorView.visualize(maze, mazeNode);

        WallsView wallsView =
                new WallsView(floorY, corridorWidth, wallHeight, wallMaterial);
        wallsView.visualize(maze, mazeNode);

        CeilingView ceilingView = new CeilingView(ceilingY, ceilingMaterial);
        ceilingView.visualize(maze, mazeNode);
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
         * Add an avatar to represent the player.
         */
        rootNode.attachChild(avatarNode);
        /*
         * Add the free items.
         */
        World world = application.getWorld();
        for (Item item : world.getFreeItems().getItems()) {
            addFreeItem(item);
        }
        /*
         * Add lights and camera.
         */
        addLights();
        addCamera();
    }
}