/*
 Copyright (c) 2014-2024 Stephen Gold
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
package jme3maze.controller;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.AssetManager;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.KeyInput;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.MazeGame;
import jme3utilities.MyString;
import jme3utilities.ui.InputMode;

/**
 * Input mode for when the player is exploring the maze.
 * <p>
 * Disabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ExploreMode extends InputMode {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ExploreMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    public ExploreMode() {
        super("explore");
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time per frame (in seconds)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        // Ignore actions that aren't ongoing.
        if (!ongoing) {
            return;
        }

        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));

        if (actionString.equals("close")) {
            simpleApplication.stop();
            return;
        } else if (actionString.equals("dump")) {
            ((MazeGame) simpleApplication).dump();
            return;
        } else if (actionString.equals("ScreenShot")) {
            ScreenshotAppState screenshotAppState
                    = stateManager.getState(ScreenshotAppState.class);
            screenshotAppState.onAction(actionString, ongoing, tpf);
        } else if (actionString.equals(
                SimpleApplication.INPUT_MAPPING_HIDE_STATS)) {
            StatsAppState statsAppState
                    = stateManager.getState(StatsAppState.class);
            statsAppState.toggleStats();
        }

        InputState inputState = stateManager.getState(InputState.class);
        inputState.setAction(actionString);
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add the default hotkey bindings. These are the bindings which will be
     * used if no custom bindings are found.
     */
    @Override
    protected void defaultBindings() {
        /*
         * Windows uses:
         *  KEY_LMETA to open the Windows menu
         *  KEY_NUMLOCK to toggle keypad key definitions
         *
         * For now, avoid re-assigning those hotkeys.
         */
        bind("close", KeyInput.KEY_ESCAPE);
        bind("dump", KeyInput.KEY_P);
        bind(InputState.leftActionString,
                KeyInput.KEY_A, KeyInput.KEY_LEFT, KeyInput.KEY_NUMPAD4);
        bind(InputState.rightActionString,
                KeyInput.KEY_D, KeyInput.KEY_RIGHT, KeyInput.KEY_NUMPAD6);
        bind(InputState.resetActionString, KeyInput.KEY_NUMPAD0);
        bind(InputState.advanceActionString, KeyInput.KEY_S, KeyInput.KEY_UP,
                KeyInput.KEY_W, KeyInput.KEY_NUMPAD8);
        bind(InputState.useRightHandItemActionString, KeyInput.KEY_X);
        bind(InputState.useLeftHandItemActionString, KeyInput.KEY_Z);

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            bind("ScreenShot", KeyInput.KEY_SCROLL); // window mgr blocks SYSRQ
        } else {
            bind("ScreenShot", KeyInput.KEY_SYSRQ);
        }

        bind(SimpleApplication.INPUT_MAPPING_HIDE_STATS, KeyInput.KEY_F5);
    }

    /**
     * Initialize this input mode prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(
            AppStateManager stateManager, Application application) {
        AssetManager am = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) am.loadAsset(assetPath);
        setCursor(cursor);

        super.initialize(stateManager, application);
    }
}
