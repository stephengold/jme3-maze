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
package jme3maze;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.model.FreeItemsState;
import jme3maze.model.PlayerState;
import jme3maze.model.WorldState;
import jme3maze.view.MainViewState;
import jme3maze.view.MapViewState;
import jme3utilities.Misc;

/**
 * Desktop simple application to play a maze game. The application's main entry
 * point is in this class.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MazeGame
        extends SimpleApplication {
    // *************************************************************************
    // constants

    /**
     * number of levels in the maze
     */
    final private static int numLevels = 2;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MazeGame.class.getName());
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maze Game";
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the maze game.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        /*
         * Instantiate the application.
         */
        MazeGame application = new MazeGame();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(windowTitle);
        application.setSettings(settings);

        application.start();
        /*
         * ... and onward to MazeGame.simpleInitApp()!
         */
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        /*
         * Disable display of JME statistics.
         * These displays can be re-enabled by pressing the F5 hotkey.
         */
        setDisplayFps(false);
        setDisplayStatView(false);
        /*
         * Capture a screenshot each time the SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        stateManager.attach(screenShotState);
        /*
         * Attach model app states to the application.
         */
        WorldState worldState = new WorldState(numLevels);
        PlayerState playerState = new PlayerState();
        FreeItemsState freeItemsState = new FreeItemsState();
        stateManager.attachAll(worldState, playerState, freeItemsState);
        /*
         * Attach view app states to the application.
         */
        MainViewState mainViewState = new MainViewState();
        MapViewState mapViewState = new MapViewState();
        stateManager.attachAll(mainViewState, mapViewState);
        /*
         * Attach controller app states to the application.
         */
        AnalogInputState analogInputState = new AnalogInputState();
        InputState inputState = new InputState();
        MoveState moveState = new MoveState();
        RawInputState rawInputState = new RawInputState();
        TurnState turnState = new TurnState();
        stateManager.attachAll(analogInputState, inputState, moveState,
                rawInputState, turnState);
    }
}