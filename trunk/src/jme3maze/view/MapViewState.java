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
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3maze.GameAppState;
import jme3maze.items.Item;
import jme3maze.model.MazeLevel;
import jme3maze.model.WorldState;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.controls.CameraControl;
import jme3utilities.debug.Printer;
import jme3utilities.math.VectorXZ;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavDebug;
import jme3utilities.navigation.NavVertex;

/**
 * Game app state to manage the (inset) map view in the Maze Game.
 * <p>
 * Disabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MapViewState
        extends GameAppState {
    // *************************************************************************
    // constants

    /**
     * background color for the map: light gray
     */
    final private static ColorRGBA mapBackground =
            new ColorRGBA(0.6f, 0.6f, 0.6f, 1f);
    /**
     * ball radius (world units)
     */
    final private static float ballRadius = 5f;
    /**
     * diameter of icons (world units)
     */
    final private static float iconDiameter = 30f;
    /**
     * bottom edge of the inset view as a fraction of the main view's height,
     * measured from the bottom edge
     */
    final private static float insetBottom = 0.65f;
    /**
     * left edge of the inset view as a fraction of the main view's width,
     * measured from the left edge
     */
    final private static float insetLeft = 0.05f;
    /**
     * right edge of the inset view as a fraction of the main view's width,
     * measured from the left edge
     */
    final private static float insetRight = 0.35f;
    /**
     * top edge of the inset view as a fraction of the main view's height,
     * measured from the bottom edge
     */
    final private static float insetTop = 0.95f;
    /**
     * stick radius (world units)
     */
    final private static float stickRadius = 2f;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MapViewState.class.getName());
    /**
     * reusable unit-square mesh
     */
    final private static Quad unitSquare = new Quad(1f, 1f);
    /**
     * asset path to the "eye" icon asset
     */
    final private static String eyeAssetPath = "Textures/map-icons/eye.png";
    /**
     * local "forward" direction (length=1)
     */
    final private static Vector3f forwardDirection = Vector3f.UNIT_Z;
    // *************************************************************************
    // fields
    /**
     * camera for rendering the map: set by addCamera()
     */
    private Camera mapCamera;
    /**
     * interval between updates (in seconds, &ge;0)
     */
    private float updateInterval = 0f;
    /**
     * map free items to their spatials
     */
    final private Map<Item, Spatial> itemSpatial = new TreeMap<>();
    /**
     * map maze arcs to their spatials
     */
    final private Map<NavArc, Spatial> arcSpatial = new TreeMap<>();
    /**
     * map maze vertices to their spatials
     */
    final private Map<NavVertex, Spatial> vertexSpatial = new TreeMap<>();
    /**
     * ball material for cul-de-sac vertex: set by initialize()
     */
    private Material culDeSacMaterial;
    /**
     * ball material for intersection vertex: set by initialize()
     */
    private Material intersectionMaterial;
    /**
     * ball material for passage vertex: set by initialize()
     */
    private Material passageMaterial;
    /**
     * stick material: set by initialize()
     */
    private Material stickMaterial;
    /**
     * node which represents the player in this view
     */
    final private Node avatarNode = new Node("map avatar node");
    /**
     * node which represents the maze in this view
     */
    final private Node mazeNode = new Node("map maze node");
    /**
     * root of this view's scene graph
     */
    final private Node mapRootNode = new Node("map root node");
    /**
     * map icon which represents the player
     */
    private Spatial eyeIcon;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled map view state.
     */
    public MapViewState() {
        setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Visualize a free item in the scene, if it isn't already visualized.
     *
     * @param item free item to represent (not null)
     */
    public void addFreeItem(Item item) {
        Validate.nonNull(item, "item");

        Spatial spatial = itemSpatial.get(item);
        if (spatial != null) {
            return;
        }
        spatial = item.visualizeMap();

        NavVertex vertex = freeItemsState.getVertex(item);
        Vector3f location = vertex.getLocation();
        spatial.move(location);

        itemSpatial.put(item, spatial);
        mapRootNode.attachChild(spatial);
    }

    /**
     * Visualize vertices on the specified line of sight, and all arcs from
     * those vertices.
     *
     * @param startVertex (not null)
     * @param direction (unit vector, positive length)
     */
    public void addMazeLineOfSight(NavVertex startVertex, VectorXZ direction) {
        Validate.nonNull(direction, "direction");
        if (direction.isZeroLength()) {
            throw new IllegalArgumentException(
                    "direction should have positive length");
        }

        VectorXZ cardinal = direction.cardinalize();
        int rowIncrement = Math.round(cardinal.getX());
        int columnIncrement = Math.round(cardinal.getZ());

        MazeLevel level = playerState.getMazeLevel();

        for (NavVertex vertex = startVertex; vertex != null;) {
            addMazeVertex(vertex);
            for (NavArc arc : vertex.getArcs()) {
                addMazeArc(arc);
            }
            vertex = level.findNextLineOfSight(vertex, rowIncrement,
                    columnIncrement);
        }
    }

    /**
     * Find the vertex (if any) in this view at the specified screen
     * coordinates.
     *
     * @param screenLocation screen coordinates (not null, in pixels, measured
     * from the lower left)
     * @return pre-existing vertex or null for none
     */
    public NavVertex findVertex(Vector2f screenLocation) {
        Validate.nonNull(screenLocation, "screen location");

        if (!isInViewPort(screenLocation)) {
            return null;
        }
        /*
         * Construct a ray based on the screen coordinates.
         */
        Vector3f startLocation =
                mapCamera.getWorldCoordinates(screenLocation, 0f);
        Vector3f farPoint = mapCamera.getWorldCoordinates(screenLocation, 1f);
        Vector3f direction = farPoint.subtract(startLocation).normalizeLocal();
        Ray ray = new Ray(startLocation, direction);
        /*
         * Trace the ray to the nearest geometry.
         */
        CollisionResults results = new CollisionResults();
        mapRootNode.collideWith(ray, results);
        CollisionResult nearest = results.getClosestCollision();
        if (nearest == null) {
            return null;
        }
        Geometry geometry = nearest.getGeometry();
        /*
         * Check whether the geometry corresponds to a vertex on the
         * same maze level as the player.
         */
        Spatial spatial = geometry;
        while (spatial != null) {
            NavVertex vertex = geometry.getUserData("vertex");
            if (vertex != null) {
                MazeLevel level = playerState.getMazeLevel();
                if (level.contains(vertex)) {
                    return vertex;
                } else {
                    return null;
                }
            }
            spatial = spatial.getParent();
        }

        return null;
    }

    /**
     * Test whether the specified screen coordinates are in this view.
     *
     * @param screenLocation screen coordinates (not null, in pixels, measured
     * from the lower left)
     * @return true if location is within this view, otherwise false
     */
    public boolean isInViewPort(Vector2f screenLocation) {
        Validate.nonNull(screenLocation, "screen location");

        if (mapCamera == null) {
            return false;
        }
        /*
         * Scale coordinates to fractions of viewport.
         */
        float xFraction = screenLocation.x / mapCamera.getWidth();
        float yFraction = screenLocation.y / mapCamera.getHeight();
        if (xFraction > insetLeft && xFraction < insetRight
                && yFraction > insetBottom && yFraction < insetTop) {
            return true;
        }
        return false;
    }

    /**
     * Load an icon from a texture asset.
     *
     * @param textureAssetPath (not null)
     * @param matchEye true to match rotation of the "eye" icon, otherwise false
     * @return new unparented instance
     */
    public Spatial loadIcon(String textureAssetPath, boolean matchEye) {
        Texture texture = assetManager.loadTexture(textureAssetPath);
        Material material =
                MyAsset.createUnshadedMaterial(assetManager, texture);
        material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        Node node = new Node("icon node");

        Geometry geometry = new Geometry("icon", unitSquare);
        node.attachChild(geometry);
        Quaternion rotation = new Quaternion();
        rotation.lookAt(Vector3f.UNIT_Y, Vector3f.UNIT_Z);
        geometry.setLocalRotation(rotation);
        geometry.setLocalScale(iconDiameter);
        float y = matchEye ? 5f : 6f;
        float iconRadius = iconDiameter / 2f;
        geometry.setLocalTranslation(iconRadius, y, -iconRadius);
        geometry.setMaterial(material);
        geometry.setQueueBucket(Bucket.Transparent);

        if (matchEye) {
            IconControl control = new IconControl(eyeIcon);
            node.addControl(control);
        }

        return node;
    }

    /**
     * Remove a free item's visualization from the scene.
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
        assert success;

        return true;
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
     * Enable or disable this state.
     *
     * @param newState true to enable, false to disable
     */
    @Override
    final public void setEnabled(boolean newState) {
        if (newState && !isEnabled()) {
            addCamera();
        }

        super.setEnabled(newState);
    }
    // *************************************************************************
    // GameAppState methods

    /**
     * Initialize this state prior to its first update.
     *
     * @param stateManager (not null)
     * @param application attaching application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        initializeMaze();
        addPlayer();
        /*
         * As a debugging aid, dump the scene graph of this view.
         */
        Printer printer = new Printer();
        printer.setPrintTransform(true);
        //printer.printSubtree(rootNode);
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Update this view before each render.
     *
     * @param renderManager (not null)
     */
    @Override
    public void render(RenderManager renderManager) {
        super.render(renderManager);
        /*
         * Update logical state here where we can be sure all controls
         * have been updated.
         */
        mapRootNode.updateLogicalState(updateInterval);
        mapRootNode.updateGeometricState();
    }

    /**
     * Update this view before each render.
     *
     * @param elapsedTime since previous frame/update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);
        updateInterval = elapsedTime;
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the camera and view port for this view.
     */
    private void addCamera() {
        /*
         * The application's default camera provides
         * a convenient starting point.
         */
        mapCamera = cam.clone();
        /*
         * Limit far plane in order to display a single maze level at a time.
         */
        float levelSpacing = WorldState.getLevelSpacing();
        mapCamera.setFrustumFar(levelSpacing);

        mapCamera.setParallelProjection(true);
        /*
         * Position the inset view port in the upper left corner
         * of the main view port.
         */
        mapCamera.setViewPort(insetLeft, insetRight, insetBottom, insetTop);

        float vertexSpacing = WorldState.getVertexSpacing();
        float yViewRadius = 3f * vertexSpacing; // world units
        MyCamera.setYTangent(mapCamera, yViewRadius);

        ViewPort insetView = renderManager.createMainView("inset", mapCamera);
        insetView.attachScene(mapRootNode);
        insetView.setBackgroundColor(mapBackground);
        insetView.setClearFlags(true, true, true);
        /*
         * Add a control for the downward-looking map camera.
         */
        float cameraHeight = 0.7f * levelSpacing; // world units
        Vector3f offset = new Vector3f(0f, cameraHeight, 0f);
        Vector3f down = new Vector3f(0f, -1f, 0f);
        CameraControl downView =
                new CameraControl(mapCamera, offset, down, forwardDirection);
        avatarNode.addControl(downView);
    }

    /**
     * Visualize a maze arc to the scene, if it isn't already visualized.
     *
     * @param arc (not null)
     */
    private void addMazeArc(NavArc arc) {
        assert arc != null;

        Spatial spatial = arcSpatial.get(arc);
        if (spatial != null) {
            return;
        }
        spatial = NavDebug.makeStick(arc, stickRadius, stickMaterial);
        arcSpatial.put(arc, spatial);
        mazeNode.attachChild(spatial);
    }

    /**
     * Visualize a maze vertex to the scene, if it isn't already visualized.
     *
     * @param vertex (not null)
     */
    private void addMazeVertex(NavVertex vertex) {
        assert vertex != null;

        Item[] items = freeItemsState.getItems(vertex);
        for (Item item : items) {
            addFreeItem(item);
        }

        Spatial spatial = vertexSpatial.get(vertex);
        if (spatial != null) {
            return;
        }
        int numArcs = vertex.getNumArcs();
        switch (numArcs) {
            case 1:
                spatial =
                        NavDebug.makeBall(vertex, ballRadius, culDeSacMaterial);
                break;
            case 2:
                spatial =
                        NavDebug.makeBall(vertex, ballRadius, passageMaterial);
                break;
            default:
                spatial = NavDebug.makeBall(vertex, ballRadius,
                        intersectionMaterial);
        }
        spatial.setUserData("vertex", vertex);
        vertexSpatial.put(vertex, spatial);
        mazeNode.attachChild(spatial);
    }

    /**
     * Visualize the player.
     */
    private void addPlayer() {
        /*
         * Add avatar node.
         */
        mapRootNode.attachChild(avatarNode);
        Vector3f location = playerState.getLocation();
        MySpatial.setWorldLocation(avatarNode, location);
        Quaternion orientation = playerState.getOrientation();
        MySpatial.setWorldOrientation(avatarNode, orientation);
        /*
         * Load the "eye" icon and attach it to the avatar.
         */
        eyeIcon = loadIcon(eyeAssetPath, false);
        avatarNode.attachChild(eyeIcon);
    }

    /**
     * Initialize maze materials and maze node.
     */
    private void initializeMaze() {
        ColorRGBA intersectionColor = ColorRGBA.Yellow;
        intersectionMaterial =
                MyAsset.createUnshadedMaterial(assetManager, intersectionColor);

        ColorRGBA culDeSacColor = ColorRGBA.Red;
        culDeSacMaterial =
                MyAsset.createUnshadedMaterial(assetManager, culDeSacColor);

        ColorRGBA passageColor = ColorRGBA.Green;
        passageMaterial =
                MyAsset.createUnshadedMaterial(assetManager, passageColor);

        ColorRGBA stickColor = ColorRGBA.Blue;
        stickMaterial =
                MyAsset.createUnshadedMaterial(assetManager, stickColor);

        mapRootNode.attachChild(mazeNode);
    }
}