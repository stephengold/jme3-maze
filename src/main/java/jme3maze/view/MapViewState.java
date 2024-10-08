/*
 Copyright (c) 2014-2023 Stephen Gold

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import jme3maze.controller.DisplaySlot;
import jme3maze.items.Item;
import jme3maze.model.MazeLevel;
import jme3maze.model.WorldState;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.controls.CameraControl;
import jme3utilities.debug.Dumper;
import jme3utilities.math.ReadXZ;
import jme3utilities.math.VectorXZ;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavDebug;
import jme3utilities.navigation.NavVertex;

/**
 * Game appstate to manage the map view.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MapViewState extends GameAppState {
    // *************************************************************************
    // constants

    /**
     * background color for the map when readable: light gray
     */
    final private static ColorRGBA readableBackgroundColor = new ColorRGBA(
            0.6f, 0.6f, 0.6f, 1f);
    /**
     *
     * background color for the map when unreadable: dark gray
     */
    final private static ColorRGBA unreadableBackgroundColor = new ColorRGBA(
            0.2f, 0.2f, 0.2f, 2f);
    /**
     * ball radius (world units)
     */
    final private static float ballRadius = 5f;
    /**
     * diameter of icons (world units)
     */
    final private static float iconDiameter = 30f;
    /**
     * stick radius (world units)
     */
    final private static float stickRadius = 2f;
    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MapViewState.class.getName());
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
    final private static Vector3f forwardDirection = new Vector3f(1f, 0f, 0f);
    // *************************************************************************
    // fields

    /**
     * slot to display this view: set in setEnabled()
     */
    private DisplaySlot slot;
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
     * Instantiate an uninitialized, disabled map view state.
     */
    public MapViewState() {
        super(false);
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
        Vector3f location = vertex.copyLocation();
        spatial.move(location);
        spatial.setUserData("item", item);
        itemSpatial.put(item, spatial);
        mapRootNode.attachChild(spatial);
    }

    /**
     * Visualize vertices on the specified line of sight, and all arcs from
     * those vertices.
     *
     * @param startVertex (not null)
     * @param direction (not null, not zero)
     */
    public void addMazeLineOfSight(NavVertex startVertex, ReadXZ direction) {
        VectorXZ.validateNonZero(direction, "direction");

        if (!isEnabled() || !isReadable()) {
            // If the map isn't readable, then it's not writable either.
            return;
        }

        ReadXZ cardinal = direction.cardinalize();
        for (NavVertex vertex = startVertex; vertex != null;) {
            addMazeVertex(vertex);
            for (NavArc arc : vertex.copyOutgoing()) {
                addMazeArc(arc);
            }
            NavArc arc = vertex.findOutgoing(cardinal, 0.9);
            if (arc == null) {
                vertex = null;
            } else {
                vertex = arc.getToVertex();
            }
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

        if (!isEnabled() || !isReadable()) {
            return null;
        }
        if (!slot.isInside(screenLocation)) {
            return null;
        }
        Ray ray = slot.pickRay(screenLocation);

        // Trace the ray to the nearest geometry.
        CollisionResults results = new CollisionResults();
        mapRootNode.collideWith(ray, results);
        CollisionResult nearest = results.getClosestCollision();
        if (nearest == null) {
            return null;
        }
        Geometry geometry = nearest.getGeometry();
        /*
         * Check whether the geometry corresponds to a vertex or item on the
         * same maze level as the player.
         */
        Spatial spatial = geometry;
        while (spatial != null) {
            NavVertex vertex = null;
            String vertexName = spatial.getUserData("vertex");
            if (vertexName != null) {
                vertex = worldState.getGraph().find(vertexName);
            }
            if (vertex == null) {
                Item item = spatial.getUserData("item");
                if (item != null) {
                    vertex = freeItemsState.getVertex(item);
                }
            }
            if (vertex != null) {
                MazeLevel level = playerState.getMazeLevel();
                if (level.contains(vertex)) {
                    return vertex;
                }
            }
            spatial = spatial.getParent();
        }

        return null;
    }

    /**
     * Test whether the specified screen coordinates are inside the map.
     *
     * @param screenLocation screen coordinates (in pixels, measured from the
     * lower left, not null, unaffected)
     * @return true if location is within this view, otherwise false
     */
    public boolean isInside(Vector2f screenLocation) {
        Validate.nonNull(screenLocation, "screen location");

        if (!isEnabled()) {
            return false;
        }
        boolean result = slot.isInside(screenLocation);

        return result;
    }

    /**
     * Test whether the map is readable.
     *
     * @return true if readable, otherwise false
     */
    public boolean isReadable() {
        boolean result = slot.getRootNode() != null;
        return result;
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
        Material material = MyAsset.createUnshadedMaterial(
                assetManager, texture);
        material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        Node node = new Node("icon node");

        Geometry geometry = new Geometry("icon", unitSquare);
        node.attachChild(geometry);
        Quaternion rotation = new Quaternion();
        rotation.lookAt(Vector3f.UNIT_Y, Vector3f.UNIT_X);
        geometry.setLocalRotation(rotation);
        geometry.setLocalScale(iconDiameter);
        float y = matchEye ? 5f : 6f;
        float iconRadius = iconDiameter / 2f;
        geometry.setLocalTranslation(-iconRadius, y, -iconRadius);
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

        boolean litLocation = worldState.isLit(location);
        setReadable(litLocation);
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
     * Alter the camera's field of view.
     *
     * @param yViewDiameter vertical diameter of the view (in grid units, &gt;0)
     */
    public void setViewDiameter(float yViewDiameter) {
        Validate.positive(yViewDiameter, "diameter");

        float vertexSpacing = WorldState.getVertexSpacing(); // world units
        float yViewRadius = vertexSpacing * yViewDiameter / 2f; // world units
        Camera mapCamera = slot.getCamera();
        float oldYvr = mapCamera.getFrustumTop() - mapCamera.getFrustumBottom();
        MyCamera.zoom(mapCamera, yViewRadius / oldYvr);
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Enable or disable this state.
     *
     * @param newStatus true to enable, false to disable
     */
    @Override
    final public void setEnabled(boolean newStatus) {
        if (newStatus && !isEnabled()) {
            insetSlotState.setEnabled(true);
            this.slot = insetSlotState;
            slot.setRootNode(null);
            addCamera();

            Vector3f location = playerState.copyLocation();
            setPlayerLocation(location);
            Quaternion orientation = playerState.copyOrientation();
            setPlayerOrientation(orientation);
            NavVertex vertex = playerState.getVertex();
            ReadXZ direction = playerState.getDirection();
            addMazeLineOfSight(vertex, direction);
        }

        super.setEnabled(newStatus);
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
    public void initialize(
            AppStateManager stateManager, Application application) {
        super.initialize(stateManager, application);

        initializeMaze();
        addPlayer();

        // As a debugging aid, dump the scene graph of this view.
        Dumper printer = new Dumper();
        printer.setDumpTransform(true);
        //printer.printSubtree(mapRootNode);
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the camera for this view.
     */
    private void addCamera() {
        Camera mapCamera = slot.getCamera();

        // Limit far plane in order to display a single maze level at a time.
        float levelSpacing = WorldState.getLevelSpacing();
        mapCamera.setFrustumFar(levelSpacing);

        float vertexSpacing = WorldState.getVertexSpacing();
        float yViewRadius = 3f * vertexSpacing; // world units
        MyCamera.setYTangent(mapCamera, yViewRadius);

        mapCamera.setParallelProjection(true);

        // Add a control for the downward-looking map camera.
        float cameraHeight = 0.7f * levelSpacing; // world units
        Vector3f offset = new Vector3f(0f, cameraHeight, 0f);
        Vector3f down = new Vector3f(0f, -1f, 0f);
        CameraControl downView = new CameraControl(
                mapCamera, offset, down, forwardDirection);
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
        int numArcs = vertex.numOutgoing();
        switch (numArcs) {
            case 1:
                spatial = NavDebug.makeBall(
                        vertex, ballRadius, culDeSacMaterial);
                break;
            case 2:
                spatial = NavDebug.makeBall(
                        vertex, ballRadius, passageMaterial);
                break;
            default:
                spatial = NavDebug.makeBall(
                        vertex, ballRadius, intersectionMaterial);
        }
        spatial.setUserData("vertex", vertex.getName());
        vertexSpatial.put(vertex, spatial);
        mazeNode.attachChild(spatial);
    }

    /**
     * Visualize the player.
     */
    private void addPlayer() {
        // Add avatar node.
        mapRootNode.attachChild(avatarNode);
        Vector3f location = playerState.copyLocation();
        MySpatial.setWorldLocation(avatarNode, location);
        Quaternion orientation = playerState.copyOrientation();
        MySpatial.setWorldOrientation(avatarNode, orientation);

        // Load the "eye" icon and attach it to the avatar.
        this.eyeIcon = loadIcon(eyeAssetPath, false);
        avatarNode.attachChild(eyeIcon);
    }

    /**
     * Initialize maze materials and maze node.
     */
    private void initializeMaze() {
        ColorRGBA intersectionColor = ColorRGBA.Yellow;
        this.intersectionMaterial = MyAsset.createUnshadedMaterial(
                assetManager, intersectionColor);

        ColorRGBA culDeSacColor = ColorRGBA.Red;
        this.culDeSacMaterial = MyAsset.createUnshadedMaterial(
                assetManager, culDeSacColor);

        ColorRGBA passageColor = ColorRGBA.Green;
        this.passageMaterial = MyAsset.createUnshadedMaterial(
                assetManager, passageColor);

        ColorRGBA stickColor = ColorRGBA.Blue;
        this.stickMaterial = MyAsset.createUnshadedMaterial(
                assetManager, stickColor);

        mapRootNode.attachChild(mazeNode);
    }

    /**
     * Alter the 'readable' status of the map.
     *
     * @param newStatus true to make the map readable, false to black it out
     */
    private void setReadable(boolean newStatus) {
        boolean oldStatus = isReadable();
        if (newStatus && !oldStatus) {
            slot.setRootNode(mapRootNode);
            slot.setBackgroundColor(readableBackgroundColor);

        } else if (oldStatus && !newStatus) {
            slot.setRootNode(null);
            slot.setBackgroundColor(unreadableBackgroundColor);
        }

        assert isReadable() == newStatus : isReadable();
    }
}
