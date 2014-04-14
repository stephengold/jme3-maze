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
import com.jme3.scene.shape.Quad;
import java.util.logging.Logger;
import jme3maze.model.GridGraph;
import jme3utilities.MySpatial;
import jme3utilities.Validate;

/**
 * Visualizer for the ceiling in the main view.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class CeilingView {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(CeilingView.class.getName());
    /**
     * reusable unit-square mesh
     */
    final private static Quad unitSquare = new Quad(1f, 1f);
    // *************************************************************************
    // fields
    /**
     * Y-coordinate of the ceiling (in world coordinates)
     */
    private float ceilingY;
    /**
     * material for ceiling (not null)
     */
    private Material material;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a visualizer with the specified parameters.
     *
     * @param ceilingY Y-coordinate of the ceiling (in world coordinates)
     * @param material material for floor (not null)
     */
    CeilingView(float ceilingY, Material material) {
        Validate.nonNull(material, "material");

        this.ceilingY = ceilingY;
        this.material = material;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Visualize the ceiling of the specified maze level at the specified
     * location in the scene.
     *
     * @param level maze level to visualize (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     */
    public void visualize(GridGraph level, Node parentNode) {
        Validate.nonNull(level, "level");
        Validate.nonNull(parentNode, "node");

        int gridRows = level.getRows();
        int gridColumns = level.getColumns();
        String prefix = level.getName();
        float vertexSpacing = level.getVertexSpacing();
        for (int row = 0; row < gridRows; row++) {
            float x = vertexSpacing * (row - gridRows / 2 - 0.5f);
            for (int column = 0; column < gridColumns; column++) {
                float z = vertexSpacing * (column - gridColumns / 2 + 0.5f);
                Vector3f location = new Vector3f(x, ceilingY, z);
                String description =
                        String.format("%sCeiling%d,%d", prefix, row, column);
                addTile(level, parentNode, location, description);
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a square ceiling tile to a scene.
     *
     * @param level maze level to visualize (not null)
     * @param parentNode where in the scene to attach the geometries (not null)
     * @param location world coordinates of tile's main corner (not null)
     * @param description name for the geometry (not null)
     */
    private void addTile(GridGraph level, Node parentNode, Vector3f location,
            String description) {
        assert location != null;
        assert description != null;

        Geometry geometry = new Geometry(description, unitSquare);
        parentNode.attachChild(geometry);
        float vertexSpacing = level.getVertexSpacing();
        geometry.setLocalScale(vertexSpacing, vertexSpacing, 1f);
        geometry.setMaterial(material);
        MySpatial.setWorldLocation(geometry, location);

        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        MySpatial.setWorldOrientation(geometry, rotation);
    }
}