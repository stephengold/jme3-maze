/*
 Copyright (c) 2014-2020, Stephen Gold
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

import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeVersion;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.controller.AnalogInputState;
import jme3maze.controller.BigSlotState;
import jme3maze.controller.ExploreMode;
import jme3maze.controller.InputState;
import jme3maze.controller.InsetSlotState;
import jme3maze.controller.RawInputState;
import jme3maze.locale.LocaleState;
import jme3maze.model.FreeItemsState;
import jme3maze.model.PlayerState;
import jme3maze.model.WorldState;
import jme3maze.view.MainViewState;
import jme3maze.view.MapViewState;
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
     * number of levels in the maze
     */
    final private static int numLevels = 2;
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
        dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);

        dumper.dump(renderManager);
    }

    /**
     * Main entry point for the maze game.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);
        /*
         * Lower the logging level for this class.
         */
        logger.setLevel(Level.INFO);
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
         * Attach localization appstate to the application.
         */
        LocaleState localeState = new LocaleState();
        stateManager.attach(localeState);
        /*
         * Attach model appstates to the application.
         */
        WorldState worldState = new WorldState(numLevels);
        PlayerState playerState = new PlayerState();
        FreeItemsState freeItemsState = new FreeItemsState();
        stateManager.attachAll(worldState, playerState, freeItemsState);
        /*
         * Attach display slot appstates to the application.
         */
        BigSlotState bigSlotState = new BigSlotState();
        InsetSlotState insetSlotState = new InsetSlotState();
        stateManager.attachAll(bigSlotState, insetSlotState);
        /*
         * Attach view appstates to the application.
         */
        MainViewState mainViewState = new MainViewState();
        MapViewState mapViewState = new MapViewState();
        stateManager.attachAll(mainViewState, mapViewState);
        /*
         * Attach input appstates to the application.
         */
        AnalogInputState analogInputState = new AnalogInputState();
        ExploreMode exploreMode = new ExploreMode();
        InputState inputState = new InputState();
        MoveState moveState = new MoveState();
        RawInputState rawInputState = new RawInputState();
        TurnState turnState = new TurnState();
        stateManager.attachAll(analogInputState, exploreMode, inputState,
                moveState, rawInputState, turnState);
        /*
         * Enable the "explore" input mode and disable the others.
         */
        getDefaultInputMode().setEnabled(false);
        exploreMode.setEnabled(true);
    }
}
