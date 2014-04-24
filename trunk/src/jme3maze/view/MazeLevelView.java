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

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import java.util.logging.Logger;
import jme3maze.model.MazeLevel;
import jme3maze.model.WorldState;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.VectorXZ;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Visualizer for a maze level in the main view.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class MazeLevelView {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MazeLevelView.class.getName());
    /**
     * reusable unit-square mesh
     */
    final private static Quad unitSquare = new Quad(1f, 1f);
    // *************************************************************************
    // fields
    /**
     * width of corridors (in world units, &gt;0): set by constructor
     */
    final private float corridorWidth;
    /**
     * height of walls (in world units, &gt;0): set by constructor
     */
    final private float wallHeight;
    /**
     * material for ceilings (not null): set by constructor
     */
    final private Material ceilingMaterial;
    /**
     * material for floors (not null): set by constructor
     */
    final private Material floorMaterial;
    /**
     * material for walls (not null): set by constructor
     */
    final private Material wallMaterial;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a visualizer with the specified parameters.
     *
     * @param corridorWidth width of corridors (in world units, &gt;0)
     * @param wallHeight height of walls (in world units, &gt;0)
     * @param ceilingMaterial material for ceiling (not null)
     * @param floorMaterial material for floor (not null)
     * @param wallMaterial material for walls (not null)
     */
    MazeLevelView(float corridorWidth, float wallHeight,
            Material ceilingMaterial, Material floorMaterial,
            Material wallMaterial) {
        Validate.positive(corridorWidth, "width");
        Validate.positive(wallHeight, "height");
        Validate.nonNull(ceilingMaterial, "ceiling material");
        Validate.nonNull(floorMaterial, "floor material");
        Validate.nonNull(wallMaterial, "wall material");

        this.corridorWidth = corridorWidth;
        this.wallHeight = wallHeight;
        this.ceilingMaterial = ceilingMaterial;
        this.floorMaterial = floorMaterial;
        this.wallMaterial = wallMaterial;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Visualize the ceiling, floor, and walls of the specified maze level under
     * the specified scene node.
     *
     * @param level maze level to visualize (not null)
     * @param parentNode where in the scene graph to attach geometries (not
     * null)
     */
    public void visualize(MazeLevel level, Node parentNode) {
        Validate.nonNull(parentNode, "node");

        int gridRows = level.getRows();
        int gridColumns = level.getColumns();
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
                visualizeVertex(level, parentNode, row, column);
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a horizontal square ceiling tile (quad).
     *
     * @param parentNode where in the scene graph to attach geometries (not
     * null)
     * @param location world coordinates of the floor below the tile's center
     * (not null, unaffected)
     * @param description name for the geometry (not null)
     * @return new instance (parented)
     */
    private Spatial addCeilingTile(Node parentNode, Vector3f location,
            String description) {
        assert description != null;

        Geometry geometry = new Geometry(description, unitSquare);
        parentNode.attachChild(geometry);

        geometry.setLocalScale(corridorWidth, corridorWidth, 1f);
        geometry.setMaterial(ceilingMaterial);
        float halfWidth = corridorWidth / 2f;
        Vector3f cornerLocation =
                location.add(-halfWidth, wallHeight, -halfWidth);
        MySpatial.setWorldLocation(geometry, cornerLocation);

        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        MySpatial.setWorldOrientation(geometry, rotation);

        return geometry;
    }

    /**
     * Add a closure wall for the specified vertex and orientation.
     *
     * @param parentNode where in the scene graph to attach geometries (not
     * null)
     * @param vertex (not null, unaffected)
     * @param direction (positive length, unaffected)
     */
    private void addClosure(Node parentNode, NavVertex vertex,
            VectorXZ direction) {
        assert parentNode != null;
        assert direction != null;
        assert !direction.isZeroLength() : direction;

        Vector3f vertexLocation = vertex.getLocation();
        float halfWidth = corridorWidth / 2f;
        Quaternion orientation = direction.toQuaternion();

        Vector3f lowerLeftCorner = new Vector3f(halfWidth, 0f, halfWidth);
        lowerLeftCorner = orientation.mult(lowerLeftCorner);
        lowerLeftCorner.addLocal(vertexLocation);

        Vector3f lowerRightCorner = new Vector3f(-halfWidth, 0f, halfWidth);
        lowerRightCorner = orientation.mult(lowerRightCorner);
        lowerRightCorner.addLocal(vertexLocation);

        String description = "closure wall";
        addWallSegment(parentNode, lowerLeftCorner, lowerRightCorner,
                description);
    }

    /**
     * Add a horizontal square floor tile (quad).
     *
     * @param parentNode where in the scene graph to attach geometries (not
     * null)
     * @param location world coordinates of tile's center (not null, unaffected)
     * @param description name for the geometry (not null)
     * @return new instance (parented)
     */
    private Spatial addFloorTile(Node parentNode, Vector3f location,
            String description) {
        assert description != null;

        Geometry geometry = new Geometry(description, unitSquare);
        parentNode.attachChild(geometry);

        geometry.setLocalScale(corridorWidth, corridorWidth, 1f);
        geometry.setMaterial(floorMaterial);
        float halfSpacing = corridorWidth / 2f;
        Vector3f cornerLocation = location.add(-halfSpacing, 0f, halfSpacing);
        MySpatial.setWorldLocation(geometry, cornerLocation);

        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        MySpatial.setWorldOrientation(geometry, rotation);

        return geometry;
    }

    /**
     * Add corridor ceiling, floor, and walls for the specified arc.
     *
     * @param parentNode where in the scene graph to attach geometries (not
     * null)
     * @param arc (not null, unaffected)
     */
    private void addOpening(Node parentNode, NavArc arc) {
        assert parentNode != null;

        VectorXZ horizontalDirection = arc.getHorizontalDirection();
        Quaternion orientation = horizontalDirection.toQuaternion();
        Vector3f vertexLocation = arc.getFromVertex().getLocation();
        /*
         * Add horizontal tiles for ceilings and floors.
         */
        float vertexSpacing = WorldState.getVertexSpacing();
        assert corridorWidth * 3f == vertexSpacing : vertexSpacing;
        VectorXZ horizontalOffset = horizontalDirection.mult(corridorWidth);
        Vector3f location = vertexLocation.add(horizontalOffset.toVector3f());
        Spatial ceilingTile =
                addCeilingTile(parentNode, location, "corridor ceiling");
        Spatial floorTile =
                addFloorTile(parentNode, location, "corridor floor");
        /*
         * Add basic side walls.
         */
        addSideWalls(parentNode, vertexLocation, orientation);
        /*
         * Check for rampage.
         */
        int levelChange = WorldState.levelChange(arc);
        if (levelChange != 0) {
            assert levelChange == 1 || levelChange == -1 : levelChange;
            /*
             * Ramps require extra side walls.
             */
            float levelSpacing = WorldState.getLevelSpacing();
            Vector3f verticalOffset =
                    new Vector3f(0f, -levelChange * levelSpacing, 0f);
            vertexLocation.addLocal(verticalOffset);
            addSideWalls(parentNode, vertexLocation, orientation);
            /*
             * Ramps require skewing both the ceiling and the floor.
             */
            float tan = levelSpacing / (vertexSpacing - corridorWidth);
            double tangent = (double) tan;
            float sec = (float) Math.sqrt(1.0 + tangent * tangent);
            float rotationAngle = FastMath.atan(tan);
            Vector3f rotationAxis;
            float dx = horizontalDirection.getX();
            float dz = horizontalDirection.getZ();
            if (Math.abs(dx) > Math.abs(dz)) {
                if (dx < 0f) {
                    MySpatial.moveWorld(ceilingTile, verticalOffset);
                    MySpatial.moveWorld(floorTile, verticalOffset);
                }
                ceilingTile.scale(sec, 1f, 1f);
                floorTile.scale(sec, 1f, 1f);
                float sign = Math.signum(dx);
                rotationAxis = new Vector3f(0f, -sign, 0f);
            } else {
                if (dz > 0f) {
                    MySpatial.moveWorld(floorTile, verticalOffset);
                } else {
                    MySpatial.moveWorld(ceilingTile, verticalOffset);
                }
                ceilingTile.scale(1f, sec, 1f);
                floorTile.scale(1f, sec, 1f);
                float sign = Math.signum(dz);
                rotationAxis = new Vector3f(sign, 0f, 0f);
            }
            rotationAngle = levelChange * rotationAngle;
            Quaternion rotation = new Quaternion();
            rotation.fromAngleNormalAxis(rotationAngle, rotationAxis);
            ceilingTile.rotate(rotation);
            rotation.fromAngleNormalAxis(-rotationAngle, rotationAxis);
            floorTile.rotate(rotation);
        }
    }

    /**
     * Add a wall segment on each side of a corridor.
     *
     * @param parentNode where in the scene to attach geometries (not null)
     * @param vertexLocation world location of start vertex of the corridor (not
     * null, unaffected)
     * @param orientation orientation of the corridor (not null, unaffected)
     */
    private void addSideWalls(Node parentNode, Vector3f vertexLocation,
            Quaternion orientation) {
        assert vertexLocation != null;

        float vertexSpacing = WorldState.getVertexSpacing();
        float halfSpacing = vertexSpacing / 2f;
        float halfWidth = corridorWidth / 2f;
        /*
         * near corner of left wall
         */
        Vector3f nearLeftCorner = new Vector3f(halfWidth, 0f, halfWidth);
        nearLeftCorner = orientation.mult(nearLeftCorner);
        nearLeftCorner.addLocal(vertexLocation);
        /*
         * far corner of left wall
         */
        Vector3f farLeftCorner = new Vector3f(halfWidth, 0f, halfSpacing);
        farLeftCorner = orientation.mult(farLeftCorner);
        farLeftCorner.addLocal(vertexLocation);
        /*
         * far corner of right wall
         */
        Vector3f farRightCorner = new Vector3f(-halfWidth, 0f, halfSpacing);
        farRightCorner = orientation.mult(farRightCorner);
        farRightCorner.addLocal(vertexLocation);
        /*
         * near corner of right wall
         */
        Vector3f nearRightCorner = new Vector3f(-halfWidth, 0f, halfWidth);
        nearRightCorner = orientation.mult(nearRightCorner);
        nearRightCorner.addLocal(vertexLocation);

        String description = "left wall";
        addWallSegment(parentNode, nearLeftCorner, farLeftCorner, description);
        description = "right wall";
        addWallSegment(parentNode, farRightCorner, nearRightCorner,
                description);
    }

    /**
     * Add a single wall segment (quad).
     *
     * @param parentNode where in the scene to attach geometries (not null)
     * @param lowerLeftCorner world coordinates of lower left corner (not null,
     * unaffected)
     * @param lowerRightCorner world coordinates of lower right corner (not
     * null, unaffected)
     * @param description name for the geometry (not null)
     */
    private void addWallSegment(Node parentNode, Vector3f lowerLeftCorner,
            Vector3f lowerRightCorner, String description) {
        assert lowerLeftCorner != null;
        assert lowerRightCorner != null;
        assert lowerLeftCorner.y == lowerRightCorner.y;
        assert description != null;

        Geometry geometry = new Geometry(description, unitSquare);
        parentNode.attachChild(geometry);
        Vector3f baseOffset = lowerRightCorner.subtract(lowerLeftCorner);
        float width = baseOffset.length();
        Vector3f scale = new Vector3f(width, wallHeight, 1f);
        geometry.setLocalScale(scale);
        geometry.setMaterial(wallMaterial);
        MySpatial.setWorldLocation(geometry, lowerLeftCorner);

        Vector3f direction = baseOffset.cross(WorldState.upDirection);
        Quaternion orientation = WorldState.toOrientation(direction);
        MySpatial.setWorldOrientation(geometry, orientation);
    }

    /**
     * Visualize ramp/corridor ceiling, floor, and walls for the specified
     * vertex and direction.
     *
     * @param parentNode where in the scene graph to attach geometries (not
     * null)
     * @param fromVertex (not null)
     * @param direction (length=1)
     */
    private void visualizeCorridor(Node parentNode, NavVertex fromVertex,
            VectorXZ direction) {
        assert parentNode != null;
        assert direction != null;
        assert direction.isUnitVector() : direction;

        Vector3f direction3 = direction.toVector3f();
        NavArc arc = fromVertex.findLeastTurn(direction3);
        if (arc == null) {
            addClosure(parentNode, fromVertex, direction);
        } else {
            Vector3f startDirection = arc.getStartDirection();
            float dot = startDirection.dot(direction3);
            if (dot > 0.5f) {
                /*
                 * found an arc with start direction within 60 degrees
                 */
                addOpening(parentNode, arc);
            } else {
                addClosure(parentNode, fromVertex, direction);
            }
        }
    }

    /**
     * Visualize ceiling, floor, and walls for the specified vertex.
     *
     * @param level maze level to visualize (not null, unaffected)
     * @param parentNode where in the scene graph to attach geometries (not
     * null)
     * @param row grid row of the vertex (&ge;0)
     * @param column grid column of the vertex (&ge;0)
     */
    private void visualizeVertex(MazeLevel level, Node parentNode, int row,
            int column) {
        assert parentNode != null;
        assert row >= 0 : row;
        assert column >= 0 : column;

        NavVertex fromVertex = level.getVertex(row, column);
        /*
         * Add ceiling and floor for the vertex.
         */
        Vector3f location = fromVertex.getLocation();
        addFloorTile(parentNode, location, "floor tile");
        addCeilingTile(parentNode, location, "ceiling tile");
        /*
         * Add possible ramps/corridors in the four cardinal directions.
         */
        visualizeCorridor(parentNode, fromVertex, VectorXZ.east);
        visualizeCorridor(parentNode, fromVertex, VectorXZ.north);
        visualizeCorridor(parentNode, fromVertex, VectorXZ.south);
        visualizeCorridor(parentNode, fromVertex, VectorXZ.west);
    }
}
