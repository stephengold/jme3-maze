/*
 Copyright (c) 2014-2019, Stephen Gold
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
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.texture.Texture;
import java.util.List;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3maze.controller.DisplaySlot;
import jme3maze.model.MazeLevel;
import jme3maze.model.WorldState;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.Validate;
import jme3utilities.debug.Dumper;
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
    final private static Logger logger
            = Logger.getLogger(MainViewState.class.getName());
    /**
     * asset path to the "pond" texture asset in jME3-testdata.jar
     */
    final private static String pondAssetPath
            = "Textures/Terrain/Pond/Pond.jpg";
    /**
     * asset path to the "wall" texture asset in jME3-testdata.jar
     */
    final private static String wallAssetPath
            = "Textures/Terrain/BrickWall/BrickWall.jpg";
    /**
     * local "forward" direction (length=1)
     */
    final private static Vector3f forwardDirection = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, enabled MainViewState.
     */
    public MainViewState() {
        super(true);
    }
    // *************************************************************************
    // fields

    /**
     * slot to display this view: set in initialize()
     */
    private DisplaySlot slot;
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
     * point light source for this view
     */
    final private PointLight torch = new PointLight();
    // *************************************************************************
    // new methods exposed

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
        logger.info("starting");
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

        Texture floorTexture
                = MyAsset.loadTexture(assetManager, pondAssetPath, true);
        floorTexture.setWrap(Texture.WrapMode.Repeat);
        if (shadedFlag) {
            floorMaterial
                    = MyAsset.createShadedMaterial(assetManager, floorTexture);
            floorMaterial.setBoolean("UseMaterialColors", true);
            floorMaterial.setColor("Diffuse", ColorRGBA.White.mult(2f));
        } else {
            floorMaterial = MyAsset.createUnshadedMaterial(assetManager,
                    floorTexture);
        }

        Texture wallTexture
                = MyAsset.loadTexture(assetManager, wallAssetPath, true);
        wallTexture.setWrap(Texture.WrapMode.Repeat);
        if (shadedFlag) {
            wallMaterial
                    = MyAsset.createShadedMaterial(assetManager, wallTexture);
            wallMaterial.setBoolean("UseMaterialColors", true);
            wallMaterial.setColor("Diffuse", ColorRGBA.White);
        } else {
            wallMaterial
                    = MyAsset.createUnshadedMaterial(assetManager, wallTexture);
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
         * Add lights and camera.
         */
        addLight();
        addCamera();

        if (debugFlag) {
            /*
             * As a debugging aid, dump the scene graph of this view.
             */
            Dumper printer = new Dumper();
            printer.setDumpTransform(true);
            printer.dump(root);
        }
        logger.info("complete");
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
            PointLightShadowRenderer plsr;
            plsr = new PointLightShadowRenderer(assetManager, shadowMapSize);
            plsr.setLight(torch);
            plsr.setShadowIntensity(1f);
            viewPort.addProcessor(plsr);
        }
    }
}
