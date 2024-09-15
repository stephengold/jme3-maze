/*
 Copyright (c) 2014-2023 Stephen Gold

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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import java.util.logging.Logger;
import jme3maze.controller.AnalogInputState;
import jme3maze.controller.BigSlotState;
import jme3maze.controller.InputState;
import jme3maze.controller.InsetSlotState;
import jme3maze.controller.RawInputState;
import jme3maze.locale.LocaleState;
import jme3maze.model.FreeItemsState;
import jme3maze.model.PlayerState;
import jme3maze.model.WorldState;
import jme3maze.view.MainViewState;
import jme3maze.view.MapViewState;
import jme3utilities.SimpleAppState;

/**
 * Simple appstate with access to all other appstates in the Maze Game.
 * <p>
 * Enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GameAppState extends SimpleAppState {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SimpleAppState.class.getName());
    // *************************************************************************
    // fields

    /**
     * appstate to handle analog input: set by initialize()
     */
    protected AnalogInputState analogInputState;
    /**
     * appstate to manage the full-screen display slot: set by initialize()
     */
    protected BigSlotState bigSlotState;
    /**
     * appstate to manage free items: set by initialize()
     */
    protected FreeItemsState freeItemsState;
    /**
     * appstate to process action strings: set by initialize()
     */
    protected InputState inputState;
    /**
     * appstate to manage the inset display slot: set by initialize()
     */
    protected InsetSlotState insetSlotState;
    /**
     * appstate to manage localization: set by initialize()
     */
    protected LocaleState localeState;
    /**
     * appstate to manage the main view: set by initialize()
     */
    protected MainViewState mainViewState;
    /**
     * appstate to manage the map view: set by initialize()
     */
    protected MapViewState mapViewState;
    /**
     * appstate to translate the player along an arc: set by initialize()
     */
    protected MoveState moveState;
    /**
     * appstate to manage the player: set by initialize()
     */
    protected PlayerState playerState;
    /**
     * appstate to handle raw input: set by initialize()
     */
    protected RawInputState rawInputState;
    /**
     * appstate to rotate the player: set by initialize()
     */
    protected TurnState turnState;
    /**
     * appstate to manage the maze: set by initialize()
     */
    protected WorldState worldState;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized GameAppState.
     *
     * @param enable initial status: true &rarr; enabled, false &rarr; disabled
     */
    public GameAppState(boolean enable) {
        super(enable);
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Initialize this appstate on the 1st update after it gets attached.
     *
     * @param sm application's render manager (not null)
     * @param app application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        super.initialize(sm, app);

        analogInputState = stateManager.getState(AnalogInputState.class);
        assert analogInputState != null;

        bigSlotState = stateManager.getState(BigSlotState.class);
        assert bigSlotState != null;

        freeItemsState = stateManager.getState(FreeItemsState.class);
        assert freeItemsState != null;

        inputState = stateManager.getState(InputState.class);
        assert inputState != null;

        insetSlotState = stateManager.getState(InsetSlotState.class);
        assert insetSlotState != null;

        localeState = stateManager.getState(LocaleState.class);
        assert localeState != null;

        mainViewState = stateManager.getState(MainViewState.class);
        assert mainViewState != null;

        mapViewState = stateManager.getState(MapViewState.class);
        assert mapViewState != null;

        moveState = stateManager.getState(MoveState.class);
        assert moveState != null;

        playerState = stateManager.getState(PlayerState.class);
        assert playerState != null;

        rawInputState = stateManager.getState(RawInputState.class);
        assert rawInputState != null;

        turnState = stateManager.getState(TurnState.class);
        assert turnState != null;

        worldState = stateManager.getState(WorldState.class);
        assert worldState != null;
    }
}
