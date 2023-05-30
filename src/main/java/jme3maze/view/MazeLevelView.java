/*
 Copyright (c) 2014-2023, Stephen Gold
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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import jme3maze.model.MazeLevel;
import jme3maze.model.WorldState;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.ReadXZ;
import jme3utilities.math.VectorXZ;
import jme3utilities.mesh.RectangleMesh;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavGraph;
import jme3utilities.navigation.NavVertex;

/**
 * Immutable visualizer for maze levels in the main view.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class MazeLevelView {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MazeLevelView.class.getName());
    // *************************************************************************
    // fields

    /**
     * width of corridors (in world units, &gt;0, set by constructor)
     */
    final private float corridorWidth;
    /**
     * ideal length and width for a ceiling quad (in world units, &gt;0, set by
     * constructor)
     */
    final private float idealSizeCeiling;
    /**
     * ideal length and width for a floor quad (in world units, &gt;0, set by
     * constructor)
     */
    final private float idealSizeFloor;
    /**
     * ideal length and width for a wall quad (in world units, &gt;0, set by
     * constructor)
     */
    final private float idealSizeWall;
    /**
     * height of walls (in world units, &gt;0, set by constructor)
     */
    final private float wallHeight;
    /**
     * material for ceilings (not null, set by constructor)
     */
    final private Material ceilingMaterial;
    /**
     * material for floors (not null, set by constructor)
     */
    final private Material floorMaterial;
    /**
     * material for walls (not null, set by constructor)
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
     * @param idealSizeCeiling ideal width and height of a ceiling quad (in
     * world units, &gt;0)
     * @param floorMaterial material for floor (not null)
     * @param idealSizeFloor ideal width and height of a floor quad (in world
     * units, &gt;0)
     * @param wallMaterial material for walls (not null)
     * @param idealSizeWall ideal width and height of a wall quad (in world
     * units, &gt;0)
     */
    MazeLevelView(float corridorWidth, float wallHeight,
            Material ceilingMaterial, float idealSizeCeiling,
            Material floorMaterial, float idealSizeFloor,
            Material wallMaterial, float idealSizeWall) {
        assert corridorWidth > 0f : corridorWidth;
        assert corridorWidth < WorldState.getVertexSpacing();
        assert wallHeight > 0f : wallHeight;
        assert wallHeight < WorldState.getLevelSpacing();
        assert ceilingMaterial != null;
        assert idealSizeCeiling > 0f : idealSizeCeiling;
        assert floorMaterial != null;
        assert idealSizeFloor > 0f : idealSizeFloor;
        assert wallMaterial != null;
        assert idealSizeWall > 0f : idealSizeWall;

        this.corridorWidth = corridorWidth;
        this.wallHeight = wallHeight;

        this.ceilingMaterial = ceilingMaterial;
        this.idealSizeCeiling = idealSizeCeiling;

        this.floorMaterial = floorMaterial;
        this.idealSizeFloor = idealSizeFloor;

        this.wallMaterial = wallMaterial;
        this.idealSizeWall = idealSizeWall;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Visualize the specified maze level under a specified scene node.
     *
     * @param level maze level to visualize (not null)
     * @param parent where in the scene to attach geometries (not null)
     * @param graph navigation graph for the level
     * @param notDone vertices not yet visualized (not null)
     */
    public void addLevel(MazeLevel level, Node parent, NavGraph graph,
            Collection<NavVertex> notDone) {
        Validate.nonNull(parent, "node");
        Validate.nonNull(notDone, "not-done set");

        // grid vertices on this level
        int gridRows = level.numGridRows();
        int gridColumns = level.numGridColumns();
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
                NavVertex vertex = level.getGridVertex(row, column);
                assert level.contains(vertex);
                assert notDone.contains(vertex);
                addGridVertex(parent, vertex);
                notDone.remove(vertex);
            }
        }

        // non-grid vertices on this level
        Collection<NavVertex> mids = new ArrayList<>(notDone.size());
        for (NavVertex vertex : notDone) {
            if (level.contains(vertex)) {
                mids.add(vertex);
            }
        }
        for (NavVertex vertex : mids) {
            addNonGridVertex(parent, vertex);
            notDone.remove(vertex);
        }
    }

    /**
     * Visualize the specified non-grid vertex.
     *
     * @param parent where in the scene to attach geometries (not null)
     * @param vertex non-grid vertex (not null, unaffected)
     */
    public void addNonGridVertex(Node parent, NavVertex vertex) {
        assert parent != null;
        assert vertex.numOutgoing() == 2;

        Vector3f northeast = new Vector3f(1f, 0f, 1f);
        NavArc neArc = vertex.findOutgoing(northeast, 0.01);
        assert neArc != null;
        Vector3f southwest = new Vector3f(-1f, 0f, -1f);
        NavArc swArc = vertex.findOutgoing(southwest, 0.01);
        assert swArc != null;
        assert neArc != swArc;
        NavVertex swVertex = swArc.getToVertex();
        NavVertex neVertex = neArc.getToVertex();
        assert neVertex != swVertex;

        ReadXZ horizontalDirection = neArc.horizontalOffset().cardinalize();
        float vertexSpacing = WorldState.getVertexSpacing();
        assert vertexSpacing > corridorWidth : vertexSpacing;
        Quaternion orientation = horizontalDirection.toQuaternion();

        VectorXZ northExtent = new VectorXZ(
                vertexSpacing - corridorWidth, corridorWidth);
        ReadXZ extent = northExtent.mult(horizontalDirection);
        extent = extent.firstQuadrant();

        // Check for slope.
        int neLevel = WorldState.levelIndex(neVertex);
        assert neLevel >= 0 : neLevel;
        int swLevel = WorldState.levelIndex(swVertex);
        assert swLevel >= 0 : swLevel;
        /*
         * The levelChange is +1 if north/east end is below south/west end.
         * It is -1 if the north/east end is above the south/west end.
         */
        int levelChange = neLevel - swLevel;
        float levelSpacing = WorldState.getLevelSpacing();
        float yChange = levelChange * levelSpacing;
        boolean nsFlag = horizontalDirection.getZ() == 0f;

        // Visualize ceiling and floor.
        addCeilingQuad(parent, vertex, extent, nsFlag, yChange);
        addFloorQuad(parent, vertex, extent, nsFlag, yChange);

        if (neLevel == swLevel) {
            // Corridor uses one set of side walls.
            addSideWalls(parent, vertex, orientation, 0f, wallHeight);

        } else {
            /*
             * Ramp uses three sets of side walls: one for the lower level,
             * one for the upper level, and one for the gap between them.
             */
            float gapHeight = levelSpacing - wallHeight;
            float halfSpacing = levelSpacing / 2;
            addSideWalls(parent, vertex, orientation, halfSpacing, wallHeight);
            addSideWalls(parent, vertex, orientation, 0f, gapHeight);
            addSideWalls(parent, vertex, orientation, -halfSpacing, wallHeight);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a ceiling for the specified vertex.
     *
     * @param parent node in the scene graph to attach geometries (not null)
     * @param vertex (not null, unaffected)
     * @param extent dimensions of the tile (not null, both components &gt;0)
     * @param nsFlag true if elongated north-south
     * @param yChange depression of the north/east end relative to the
     * south/west end (in world units)
     */
    private void addCeilingQuad(Node parent, NavVertex vertex,
            ReadXZ extent, boolean nsFlag, float yChange) {
        assert parent != null;
        assert vertex != null;
        assert extent != null;
        assert extent.getX() > 0f : extent;
        assert extent.getZ() > 0f : extent;

        Vector3f vertexLocation = vertex.copyLocation();
        String vertexName = vertex.getName();
        String description = "ceiling for " + vertexName;
        /*
         * The quad's origin is in the southwest corner.
         * Its U-axis points north. Its V-axis points east.
         */
        Vector3f origin = vertexLocation.add(
                -extent.getX() / 2, wallHeight, -extent.getZ() / 2);
        Vector3f uOffset = new Vector3f(extent.getX(), 0f, 0f);
        Vector3f vOffset = new Vector3f(0f, 0f, extent.getZ());
        if (nsFlag) {
            // north-south
            origin.y += yChange / 2;
            uOffset.y -= yChange;

        } else { // east-west
            origin.y += yChange / 2;
            vOffset.y -= yChange;
        }
        addRectangle(parent, description, ceilingMaterial, idealSizeCeiling,
                origin, uOffset, vOffset);
    }

    /**
     * Add a closure wall for the specified grid vertex and direction.
     *
     * @param parent node in the scene graph to attach geometries (not null)
     * @param gridVertex (not null, unaffected)
     * @param direction from the center of the vertex (not null, not zero,
     * unaffected)
     * @param dirDescription textual description of the direction (not null)
     */
    private void addClosure(Node parent, NavVertex gridVertex,
            VectorXZ direction, String dirDescription) {
        assert parent != null;
        assert direction != null;
        assert !direction.isZero() : direction;

        Vector3f vertexLocation = gridVertex.copyLocation();
        Quaternion orientation = direction.toQuaternion();

        float halfWidth = corridorWidth / 2;
        Vector3f lowerLeftCorner = new Vector3f(halfWidth, 0f, -halfWidth);
        lowerLeftCorner
                = MyQuaternion.rotate(orientation, lowerLeftCorner, null);
        lowerLeftCorner.addLocal(vertexLocation);

        Vector3f lowerRightCorner = new Vector3f(halfWidth, 0f, halfWidth);
        lowerRightCorner
                = MyQuaternion.rotate(orientation, lowerRightCorner, null);
        lowerRightCorner.addLocal(vertexLocation);

        String description = String.format("%s closure wall for %s",
                dirDescription, gridVertex.getName());
        addWallSegment(parent, description, lowerLeftCorner, lowerRightCorner,
                wallHeight);
    }

    /**
     * Visualize the floor for the specified vertex.
     *
     * @param parent node in the scene graph to attach geometries (not null)
     * @param vertex (not null, unaffected)
     * @param extent dimensions of the tile (not null, both components &gt;0)
     * @param nsFlag true if elongated north-south
     * @param yChange depression of the north/east end relative to the
     * south/west end (in world units)
     */
    private void addFloorQuad(Node parent, NavVertex vertex,
            ReadXZ extent, boolean nsFlag, float yChange) {
        assert parent != null;
        assert vertex != null;
        assert extent != null;
        assert extent.getX() > 0f : extent;
        assert extent.getZ() > 0f : extent;

        Vector3f vertexLocation = vertex.copyLocation();
        String vertexName = vertex.getName();
        String description = "floor for " + vertexName;
        /*
         * The quad's origin is in the southeast corner.
         * Its U-axis points north. Its V-axis points west.
         */
        Vector3f origin
                = vertexLocation.add(-extent.getX() / 2, 0f, extent.getZ() / 2);
        Vector3f uOffset = new Vector3f(extent.getX(), 0f, 0f);
        Vector3f vOffset = new Vector3f(0f, 0f, -extent.getZ());
        if (nsFlag) {
            /* north-south */
            origin.y += yChange / 2;
            uOffset.y -= yChange;
        } else {
            /* east-west */
            origin.y -= yChange / 2;
            vOffset.y += yChange;
        }
        addRectangle(parent, description, floorMaterial, idealSizeFloor,
                origin, uOffset, vOffset);
    }

    /**
     * Visualize ceiling, floor, and walls for the specified grid vertex.
     *
     * @param parent where in the scene graph to attach geometries (not null)
     * @param gridVertex (not null, unaffected)
     */
    private void addGridVertex(Node parent, NavVertex gridVertex) {
        assert parent != null;

        // Visualize the ceiling and floor.
        VectorXZ extent = new VectorXZ(corridorWidth, corridorWidth);
        boolean nsFlag = false;
        float yChange = 0f;
        addFloorQuad(parent, gridVertex, extent, nsFlag, yChange);
        addCeilingQuad(parent, gridVertex, extent, nsFlag, yChange);

        // Visualize opening or closure in each of the four cardinal directions.
        addGridWall(parent, gridVertex, VectorXZ.east, "east");
        addGridWall(parent, gridVertex, VectorXZ.north, "north");
        addGridWall(parent, gridVertex, VectorXZ.south, "south");
        addGridWall(parent, gridVertex, VectorXZ.west, "west");
    }

    /**
     * Visualize the closure walls for the specified grid vertex and direction.
     *
     * @param parent where in the scene graph to attach geometries (not null)
     * @param gridVertex (not null, unaffected)
     * @param direction (not null, not zero, unaffected)
     * @param dirDescription (not null)
     */
    private void addGridWall(Node parent, NavVertex gridVertex,
            VectorXZ direction, String dirDescription) {
        assert parent != null;
        assert direction != null;
        assert !direction.isZero();
        assert dirDescription != null;

        double cosineTolerance = 0.75;
        NavArc arc = gridVertex.findOutgoing(direction, cosineTolerance);
        if (arc == null) {
            addClosure(parent, gridVertex, direction, dirDescription);
        }
    }

    /**
     * Visualize a rectangle with the specified name and material.
     *
     * @param parent where in the scene graph to attach geometries (not null)
     * @param description (not null)
     * @param material (not null)
     * @param idealSize (in world units, &gt;0)
     * @param origin world location of the lower left corner (not null,
     * unaffected)
     * @param uOffset offset of lower right corner from origin (not null, not
     * zero, unaffected)
     * @param vOffset offset of upper left corner from origin (not null, not
     * zero, unaffected, orthogonal to uOffset)
     */
    private void addRectangle(
            Node parent, String description, Material material, float idealSize,
            Vector3f origin, Vector3f uOffset, Vector3f vOffset) {
        assert parent != null;
        assert description != null;
        assert material != null;
        assert idealSize > 0f : idealSize;
        assert origin != null;
        assert uOffset != null;
        assert !MyVector3f.isZero(uOffset);
        assert vOffset != null;
        assert !MyVector3f.isZero(vOffset);

        // Calculate the dimentions of the rectangle, in world units.
        float uLength = uOffset.length();
        float vLength = vOffset.length();

        // Calculate the world direction of the axes and normal.
        Vector3f uDirection = uOffset.divide(uLength);
        Vector3f vDirection = vOffset.divide(vLength);
        Vector3f normal = uDirection.cross(vDirection);
        float normalLength = normal.lengthSquared();
        assert normalLength > 0.9998f && normalLength < 1.0002f : normal;

        // Calculate the world orientation of rectangle.
        Quaternion orientation = new Quaternion();
        orientation.fromAxes(uDirection, vDirection, normal);

        int numU = 1;
        if (uLength > idealSize) {
            numU = Math.round(uLength / idealSize);
        }
        int numV = 1;
        if (vLength > idealSize) {
            numV = Math.round(vLength / idealSize);
        }

        RectangleMesh mesh = new RectangleMesh(
                0f, numU, 0f, numV, 0f, uLength, 0f, vLength, 1f);
        Geometry geometry = new Geometry(description, mesh);
        parent.attachChild(geometry);
        geometry.setMaterial(material);
        MySpatial.setWorldLocation(geometry, origin);
        MySpatial.setWorldOrientation(geometry, orientation);
    }

    /**
     * Visualize wall segments on both sides of a corridor.
     *
     * @param parent where in the scene graph to attach geometries (not null)
     * @param vertex (not null, unaffected)
     * @param orientation orientation of the corridor (not null, unaffected)
     * @param yOffset vertical offset of bottom edges above the vertex location
     * (in world units)
     * @param height height of the walls (in world units, &gt;0)
     */
    private void addSideWalls(Node parent, NavVertex vertex,
            Quaternion orientation, float yOffset, float height) {
        assert parent != null;
        assert orientation != null;
        assert height > 0f : height;

        Vector3f location = vertex.copyLocation();
        location.y += yOffset;
        float vertexSpacing = WorldState.getVertexSpacing();
        float halfWidth = corridorWidth / 2f;
        float halfLength = (vertexSpacing - corridorWidth) / 2f;

        // backward corner of left wall
        Vector3f backLeft = new Vector3f(-halfLength, 0f, -halfWidth);
        backLeft = MyQuaternion.rotate(orientation, backLeft, null);
        backLeft.addLocal(location);

        // forward corner of left wall
        Vector3f forwardLeft = new Vector3f(halfLength, 0f, -halfWidth);
        forwardLeft = MyQuaternion.rotate(orientation, forwardLeft, null);
        forwardLeft.addLocal(location);

        String description = "left wall of " + vertex.getName();
        addWallSegment(parent, description, backLeft, forwardLeft, height);

        // forward corner of right wall
        Vector3f forwardRight = new Vector3f(halfLength, 0f, halfWidth);
        forwardRight = MyQuaternion.rotate(orientation, forwardRight, null);
        forwardRight.addLocal(location);

        // backward corner of right wall
        Vector3f backRight = new Vector3f(-halfLength, 0f, halfWidth);
        backRight = MyQuaternion.rotate(orientation, backRight, null);
        backRight.addLocal(location);

        description = "right wall of " + vertex.getName();
        addWallSegment(parent, description, forwardRight, backRight, height);
    }

    /**
     * Visualize a wall segment.
     *
     * @param parent where in the scene to attach geometries (not null)
     * @param description textual description of the rectangle (not null)
     * @param lowerLeft world coordinates of lower left corner (not null,
     * unaffected)
     * @param lowerRight world coordinates of lower right corner (not null,
     * unaffected)
     * @param height height of the wall (in world units, &gt;0)
     */
    private void addWallSegment(Node parent, String description,
            Vector3f lowerLeft, Vector3f lowerRight, float height) {
        assert parent != null;
        assert description != null;
        assert lowerLeft != null;
        assert lowerRight != null;
        assert lowerLeft.y == lowerRight.y : lowerLeft.y;
        assert height > 0f : height;
        /*
         * The quad's origin is in the lower left corner.
         * Its uOffset points right. Its vOffset points up.
         */
        Vector3f origin = lowerLeft;
        Vector3f uOffset = lowerRight.subtract(origin);
        Vector3f vOffset = new Vector3f(0f, height, 0f);
        addRectangle(parent, description, wallMaterial, idealSizeWall,
                origin, uOffset, vOffset);
    }
}
