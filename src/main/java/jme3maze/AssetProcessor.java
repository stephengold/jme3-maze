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
package jme3maze;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.ModelKey;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.export.JmeExporter;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderLoader;
import com.jme3.system.JmeContext;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.debug.Dumper;

/**
 * A headless simple application to convert Blender assets to J3O files.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AssetProcessor extends SimpleApplication {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            AssetProcessor.class.getName());
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the Maze Game asset builder.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        // Mute the chatty loggers found in some imported packages.
        Heart.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        // Set the logging level for this class.
        logger.setLevel(Level.INFO);

        // Instantiate the application.
        AssetProcessor application = new AssetProcessor();
        application.start(JmeContext.Type.Headless);

        // ... and onward to AssetProcessor.simpleInitApp()!
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        assetManager.registerLoader(BlenderLoader.class, "blend");

        String userDir = System.getProperty("user.dir");
        logger.log(Level.INFO, "working directory is {0}",
                MyString.quote(userDir));

        processItemModels();

        stop();
    }
    // *************************************************************************
    // private methods

    /**
     * Process models in the "Models/items" subfolder.
     */
    private void processItemModels() {
        String[] paths = {
            "ankh/ankh", "crown/crown", "mapmaker/mapmaker", "mummy/mummy",
            "torch/torch"
        };
        processModelFolder("items", paths);
    }

    /**
     * Convert a Blender model asset to J3O format.
     *
     * @param path (not null)
     */
    private void processModel(String path) {
        assert path != null;
        logger.log(Level.FINE, "path={0}", path);

        String sourceAssetPath = String.format("Models/%s.blend", path);
        String targetFilePath = String.format("Written Assets/Models/%s.j3o", path);
        File targetFile = new File(targetFilePath);

        // Load the Blender model.
        ModelKey key = new ModelKey(sourceAssetPath);
        Spatial model = assetManager.loadModel(key);
        logger.log(Level.INFO, "read Blender asset {0}",
                MyString.quote(sourceAssetPath));
        validateSpatial(model);

        new Dumper().dump(model);

        // Save the model in J3O format.
        JmeExporter exporter = BinaryExporter.getInstance();
        try {
            exporter.save(model, targetFile);
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "write to {0} failed",
                    MyString.quote(targetFilePath));
            throw new RuntimeException();
        }
        logger.log(Level.INFO, "wrote file {0}",
                MyString.quote(targetFilePath));
    }

    private void processModelFolder(String folderPath, String[] paths) {
        for (String modelPath : paths) {
            String assetPath = String.format("%s/%s", folderPath, modelPath);
            processModel(assetPath);
        }
    }

    /**
     * Validate a geometry which is part of an imported model.
     *
     * @param geometry (not null)
     */
    private void validateGeometry(Geometry geometry) {
        Mesh mesh = geometry.getMesh();
        Mode mode = mesh.getMode();
        if (mode != Mesh.Mode.Triangles) {
            throw new AssetLoadException("expected a mesh in Triangles mode");
        }
    }

    /**
     * Validate a spatial which is part of an imported model.
     *
     * @param spatial (not null)
     */
    private void validateSpatial(Spatial spatial) {
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            validateGeometry(geometry);
            return;
        }
        assert spatial instanceof Node;
        Node node = (Node) spatial;
        for (Spatial child : node.getChildren()) {
            validateSpatial(child);
        }
    }
}
