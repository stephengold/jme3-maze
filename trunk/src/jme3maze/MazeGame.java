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

import jme3maze.view.MapState;
import jme3maze.view.MainState;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.model.Player;
import jme3maze.model.World;
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
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MazeGame.class.getName());
    /**
     * application name for the window's title bar
     */
    final private static String windowTitle = "Maze Game";
    // *************************************************************************
    // fields
    /**
     * main view: set by simpleInitApp()
     */
    private MainState mainView;
    /**
     * map view: set by simpleInitApp()
     */
    private MapState mapView;
    /**
     * abstract game data (MVC model): set by simpleInitApp()
     */
    private World world;
    // *************************************************************************
    // new methods exposed

    /**
     * Access the main model.
     */
    public World getWorld() {
        assert world != null;
        return world;
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
         * model
         */
        world = new World();
        /*
         * view.
         */
        initializeView();
        /*
         * controller
         */
        initializeController();
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize controller app states and controls.
     */
    private void initializeController() {
        /*
         * Add a player control to the map's avatar.
         */
        Player player = world.getPlayer();
        PlayerControl mapPlayerControl = new PlayerControl(player);
        mapView.getAvatarNode().addControl(mapPlayerControl);
        /*
         * Add a player control to the main avatar.
         */
        PlayerControl mainPlayerControl = new PlayerControl(player);
        mainView.getAvatarNode().addControl(mainPlayerControl);
        /*
         * Attach controller app states to the application.
         */
        InputState inputState = new InputState();
        MoveState moveState = new MoveState();
        TurnState turnState = new TurnState();
        stateManager.attachAll(inputState, moveState, turnState);
        /*
         * Activate the turn app state.
         */
        Vector3f direction = player.getArc().getStartDirection();
        turnState.activate(direction);
    }

    /**
     * Initialize view app states.
     */
    private void initializeView() {
        Node mapRootNode = new Node("map root node");
        /*
         * Attach view app states to the application.
         */
        mapView = new MapState(mapRootNode);
        mainView = new MainState(rootNode);
        stateManager.attachAll(mapView, mainView);
    }
}