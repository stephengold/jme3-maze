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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import java.util.logging.Logger;
import jme3maze.model.GridGraph;
import jme3maze.model.World;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.navigation.NavArc;
import jme3utilities.navigation.NavVertex;

/**
 * Visualizer for walls in the main view.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class WallsView {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(WallsView.class.getName());
    /**
     * a unit square mesh
     */
    final private static Quad unitSquare = new Quad(1f, 1f);
    // *************************************************************************
    // fields
    /**
     * Y-coordinate of the floor (in world coordinates)
     */
    private float floorY;
    /**
     * width of corridors (in world units, &gt;0)
     */
    private float corridorWidth;
    /**
     * height of walls (in world units, &gt;0)
     */
    private float wallHeight;
    /**
     * material for walls (not null)
     */
    private Material material;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a visualizer with the specified parameters.
     *
     * @param floorY Y-coordinate of the floor (in world coordinates)
     * @param corridorWidth width of corridors (in world units, &gt;0)
     * @param wallHeight height of walls (in world units, &gt;0)
     * @param material material for walls (not null)
     */
    WallsView(float floorY, float corridorWidth, float wallHeight,
            Material material) {
        Validate.positive(corridorWidth, "width");
        Validate.positive(wallHeight, "height");
        Validate.nonNull(material, "material");

        this.floorY = floorY;
        this.corridorWidth = corridorWidth;
        this.wallHeight = wallHeight;
        this.material = material;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Visualize the walls of the specified maze at the specified location in
     * the scene.
     *
     * @param maze maze to visualize (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     */
    public void visualize(GridGraph maze, Node parentNode) {
        Validate.nonNull(maze, "maze");
        Validate.nonNull(parentNode, "node");

        int gridRows = maze.getRows();
        int gridColumns = maze.getColumns();
        for (int row = 0; row < gridRows; row++) {
            for (int column = 0; column < gridColumns; column++) {
                visualize(maze, parentNode, row, column);
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a closure wall for the specified vertex and orientation.
     *
     * @param maze maze to visualize (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param vertexLocation (not null)
     * @param orientation relative to the world's +Z direction (not null)
     */
    private void addClosure(Node parentNode, Vector3f vertexLocation,
            Quaternion orientation) {
        assert parentNode != null;
        assert vertexLocation != null;

        float halfWidth = corridorWidth / 2f;

        Vector3f lowerLeftCorner = new Vector3f(halfWidth, floorY, halfWidth);
        lowerLeftCorner = orientation.mult(lowerLeftCorner);
        lowerLeftCorner.addLocal(vertexLocation);

        Vector3f lowerRightCorner = new Vector3f(-halfWidth, floorY, halfWidth);
        lowerRightCorner = orientation.mult(lowerRightCorner);
        lowerRightCorner.addLocal(vertexLocation);

        String description = "closure wall";
        addSegment(parentNode, lowerLeftCorner, lowerRightCorner, description);
    }

    /**
     * Add an opening wall for the specified vertex and orientation.
     *
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param vertexLocation (not null)
     * @param orientation relative to the world's +Z direction (not null)
     */
    private void addOpening(GridGraph maze, Node parentNode,
            Vector3f vertexLocation, Quaternion orientation) {
        assert parentNode != null;
        assert vertexLocation != null;

        float vertexSpacing = maze.getVertexSpacing();
        float halfSpacing = vertexSpacing / 2f;
        float halfWidth = corridorWidth / 2f;

        Vector3f corner1 = new Vector3f(halfSpacing, floorY, halfWidth);
        corner1 = orientation.mult(corner1);
        corner1.addLocal(vertexLocation);

        Vector3f corner2 = new Vector3f(halfWidth, floorY, halfWidth);
        corner2 = orientation.mult(corner2);
        corner2.addLocal(vertexLocation);

        Vector3f corner3 = new Vector3f(halfWidth, floorY, halfSpacing);
        corner3 = orientation.mult(corner3);
        corner3.addLocal(vertexLocation);

        Vector3f corner4 = new Vector3f(-halfWidth, floorY, halfSpacing);
        corner4 = orientation.mult(corner4);
        corner4.addLocal(vertexLocation);

        Vector3f corner5 = new Vector3f(-halfWidth, floorY, halfWidth);
        corner5 = orientation.mult(corner5);
        corner5.addLocal(vertexLocation);

        Vector3f corner6 = new Vector3f(-halfSpacing, floorY, halfWidth);
        corner6 = orientation.mult(corner6);
        corner6.addLocal(vertexLocation);

        String description = "opening wall 1";
        addSegment(parentNode, corner1, corner2, description);
        description = "opening wall 2";
        addSegment(parentNode, corner2, corner3, description);
        description = "opening wall 3";
        addSegment(parentNode, corner4, corner5, description);
        description = "opening wall 4";
        addSegment(parentNode, corner5, corner6, description);
    }

    /**
     * Add a single wall segment (quad) to a scene.
     *
     * @param maze maze to visualize (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param lowerLeftCorner world coordinates of lower left (not null)
     * @param lowerRightCorner world coordinates of lower right (not null)
     * @param description name for the geometry (not null)
     */
    private void addSegment(Node parentNode, Vector3f lowerLeftCorner,
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
        geometry.setMaterial(material);
        MySpatial.setWorldLocation(geometry, lowerLeftCorner);

        Vector3f direction = baseOffset.cross(World.upDirection);
        Quaternion orientation = new Quaternion();
        orientation.lookAt(direction, World.upDirection);
        MySpatial.setWorldOrientation(geometry, orientation);
    }

    /**
     * Add walls between a particular pair of adjacent vertices.
     *
     * @param maze maze to visualize (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param fromVertex (not null)
     * @param toVertex (not null)
     */
    public void addWallsBetween(GridGraph maze, Node parentNode,
            NavVertex fromVertex, NavVertex toVertex) {
        Validate.nonNull(parentNode, "node");
        Validate.nonNull(fromVertex, "from vertex");
        Validate.nonNull(toVertex, "to vertex");

        Vector3f fromLocation = fromVertex.getLocation();
        Vector3f toLocation = toVertex.getLocation();
        Vector3f offset = toLocation.subtract(fromLocation);
        Quaternion orientation = new Quaternion();
        orientation.lookAt(offset, World.upDirection);
        NavArc arc = fromVertex.findArcTo(toVertex);
        if (arc == null) {
            addClosure(parentNode, fromLocation, orientation);
        } else {
            addOpening(maze, parentNode, fromLocation, orientation);
        }
    }

    /**
     * Visualize walls for the specified vertex.
     *
     * @param maze maze to visualize (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param row (&ge;0)
     * @param column (&ge;0)
     */
    private void visualize(GridGraph maze, Node parentNode, int row,
            int column) {
        Validate.nonNull(maze, "maze");
        Validate.nonNull(parentNode, "node");
        Validate.nonNegative(row, "row");
        Validate.nonNegative(column, "column");

        int gridRows = maze.getRows();
        int gridColumns = maze.getColumns();
        NavVertex fromVertex = maze.getVertex(row, column);
        Vector3f vertexLocation = fromVertex.getLocation();
        Quaternion orientation = new Quaternion();

        if (row + 1 < gridRows) {
            NavVertex north = maze.getVertex(row + 1, column);
            addWallsBetween(maze, parentNode, fromVertex, north);
        } else {
            orientation.lookAt(Vector3f.UNIT_X, World.upDirection);
            addClosure(parentNode, vertexLocation, orientation);
        }

        if (column + 1 < gridColumns) {
            NavVertex east = maze.getVertex(row, column + 1);
            addWallsBetween(maze, parentNode, fromVertex, east);
        } else {
            orientation.lookAt(Vector3f.UNIT_Z, World.upDirection);
            addClosure(parentNode, vertexLocation, orientation);
        }

        if (row - 1 >= 0) {
            NavVertex south = maze.getVertex(row - 1, column);
            addWallsBetween(maze, parentNode, fromVertex, south);
        } else {
            orientation.lookAt(new Vector3f(-1f, 0f, 0f), World.upDirection);
            addClosure(parentNode, vertexLocation, orientation);
        }

        if (column - 1 >= 0) {
            NavVertex west = maze.getVertex(row, column - 1);
            addWallsBetween(maze, parentNode, fromVertex, west);
        } else {
            orientation.lookAt(new Vector3f(0f, 0f, -1f), World.upDirection);
            addClosure(parentNode, vertexLocation, orientation);
        }
    }
}