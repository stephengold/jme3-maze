/*
 Copyright (c) 2014-2022, Stephen Gold
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

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.controller.BigSlotState;
import jme3maze.model.WorldState;
import jme3maze.view.MainViewState;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.debug.Dumper;
import jme3utilities.ui.ActionApplication;

/**
 * Desktop action application to play a maze game. The application's main entry
 * point is in this class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MazeGame extends ActionApplication {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MazeGame.class.getName());
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maze Game";
    // *************************************************************************
    // fields

    /**
     * dumper for scene dumps
     */
    final private static Dumper dumper = new Dumper();
    // *************************************************************************
    // new methods exposed

    /**
     * Process a "dump" action.
     */
    public void dump() {
        dumper.setDumpBucket(true);
        dumper.setDumpCull(true);
        dumper.setDumpMatParam(true);
        dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);
        dumper.setDumpVertex(true);

        dumper.dump(renderManager);
    }

    /**
     * Main entry point for the maze game.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        MazeGame application = new MazeGame();
        Heart.parseAppArgs(application, arguments);
        /*
         * Lower the logging level for this class.
         */
        logger.setLevel(Level.INFO);
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to MazeGame.actionInitializeApplication()!
         */
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        /*
         * Log the jMonkeyEngine version string.
         */
        logger.log(Level.INFO, "jME3-core version is {0}",
                MyString.quote(JmeVersion.FULL_NAME));
        /*
         * Log the jme3-utilities-heart version string.
         */
        logger.log(Level.INFO, "Heart version is {0}",
                MyString.quote(Heart.versionShort()));

        renderer.setDefaultAnisotropicFilter(8);
        cam.setLocation(new Vector3f(-35f, 5f, 30f));
        cam.lookAtDirection(Vector3f.UNIT_X, Vector3f.UNIT_Y);
        viewPort.setBackgroundColor(ColorRGBA.Blue);
        /*
         * Disable display of JME statistics.
         * These displays can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
        /*
         * Attach model appstates to the application.
         */
        WorldState worldState = new WorldState(1);
        stateManager.attachAll(worldState);
        /*
         * Attach display slot appstates to the application.
         */
        BigSlotState bigSlotState = new BigSlotState();
        stateManager.attachAll(bigSlotState);
        /*
         * Attach view appstates to the application.
         */
        MainViewState mainViewState = new MainViewState();
        stateManager.attachAll(mainViewState);

        mainViewState.setTorchLocation(new Vector3f(3f, 4.65f, 33f));
    }

    static int frameCount = 0;

    public void simpleUpdate(float tpf) {
        ++frameCount;
        if (frameCount == 2) {
            guiNode.detachAllChildren();
            dump();
        }
    }
}
