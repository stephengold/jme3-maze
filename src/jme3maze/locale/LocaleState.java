/*
 Copyright (c) 2015, Stephen Gold
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
package jme3maze.locale;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3maze.GameAppState;

/**
 * App state for localization data.
 * <p>
 * Enabled at creation.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class LocaleState
        extends GameAppState {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(LocaleState.class.getName());
    /**
     * base name for resource bundles which contain localized messages for
     * alerts
     */
    final public static String alertBundlePrefix = "jme3maze.locale.Alerts";
    /**
     * base name for resource bundles which contain localized labels for GUI
     * controls
     */
    final public static String labelBundlePrefix = "jme3maze.locale.Labels";
    /**
     * base name for resource bundles which contain localized names for types of
     * items
     */
    final public static String typeBundlePrefix = "jme3maze.locale.ItemTypes";
    // *************************************************************************
    // fields
    /**
     * localized messages for alerts: set by initialize()
     */
    private ResourceBundle alerts;
    /**
     * localized names for types of items: set by initialize()
     */
    private ResourceBundle itemTypes;
    /**
     * localized labels for GUI controls: set by initialize()
     */
    private ResourceBundle labels;
    // *************************************************************************
    // new methods exposed

    /**
     * Read the localized messages for alerts.
     *
     * @return localized messages (not null)
     */
    public ResourceBundle getAlertsBundle() {
        assert alerts != null;
        return alerts;
    }

    /**
     * Read the localized labels for GUI controls.
     *
     * @return localized labels (not null)
     */
    public ResourceBundle getLabelsBundle() {
        assert labels != null;
        return labels;
    }

    /**
     * Read the localized names for types of items.
     *
     * @return localized item type names (not null)
     */
    public ResourceBundle getTypesBundle() {
        assert itemTypes != null;
        return itemTypes;
    }
    // *************************************************************************
    // GameAppState methods

    /**
     * Initialize this state prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application attaching application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        Locale locale = Locale.getDefault();
        logger.log(Level.INFO, "locale = {0}", locale);

        alerts = ResourceBundle.getBundle(alertBundlePrefix, locale);
        assert alerts != null;

        itemTypes = ResourceBundle.getBundle(typeBundlePrefix, locale);
        assert itemTypes != null;

        labels = ResourceBundle.getBundle(labelBundlePrefix, locale);
        assert labels != null;
    }
}