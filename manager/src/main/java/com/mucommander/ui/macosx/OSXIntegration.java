/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2012 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.macosx;

import com.mucommander.Launcher;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.FileFactory;
import com.mucommander.commons.runtime.OsFamilies;
import com.mucommander.commons.runtime.OsVersions;
import com.mucommander.conf.MuConfigurations;
import com.mucommander.conf.MuPreference;
import com.mucommander.conf.MuPreferences;
import com.mucommander.ui.action.ActionManager;
import com.mucommander.ui.dialog.about.AboutDialog;
import com.mucommander.ui.dialog.shutdown.QuitDialog;
import com.mucommander.ui.main.FolderPanel;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.WindowManager;
import org.simplericity.macify.eawt.Application;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.simplericity.macify.eawt.DefaultApplication;

/**
 * This class handles Mac OS X specifics when muCommander is started:
 * <ul>
 *  <li>Turns on/off brush metal based on preferences (default is on)
 *  <li>Turns screen menu bar based on preferences (default is on, no GUI for that pref)
 *  <li>Registers handlers for the 'About', 'Preferences' and 'Quit' application menu items
 * </ul>
 *
 * {@see Launcher} to find out it's usages
 *
 * @author Maxence Bernard, Eugene Morozov
 */
public class OSXIntegration implements ApplicationListener {

    public OSXIntegration() {
        if(OsFamilies.MAC_OS_X.isCurrent()) {
            // At the time of writing, the 'brushed metal' look causes the JVM to crash randomly under Leopard (10.5)
            // so we disable brushed metal on that OS version but leave it for earlier versions where it works fine.
            // See http://www.mucommander.com/forums/viewtopic.php?f=4&t=746 for more info about this issue.
            if(OsVersions.MAC_OS_X_10_4.isCurrentOrLower()) {
                // Turn on/off brush metal look (default is off because still buggy when scrolling and panning dialog windows) :
                //  "Allows you to display your main windows with the 'textured' Aqua window appearance.
                //   This property should be applied only to the primary application window,
                //   and should not affect supporting windows like dialogs or preference windows."
                System.setProperty("apple.awt.brushMetalLook",
                    ""+MuConfigurations.getPreferences().getVariable(MuPreference.USE_BRUSHED_METAL, MuPreferences.DEFAULT_USE_BRUSHED_METAL));
            }

            // Enables/Disables screen menu bar (default is on) :
            //  "if you are using the Aqua look and feel, this property puts Swing menus in the Mac OS X menu bar."
            System.setProperty("apple.laf.useScreenMenuBar", ""+MuConfigurations.getPreferences().getVariable(MuPreference.USE_SCREEN_MENU_BAR,
                                                                                                 MuPreferences.DEFAULT_USE_SCREEN_MENU_BAR));

            Application app = new DefaultApplication();
            app.setEnabledAboutMenu(true); // Enable the 'About' menu item
            app.setEnabledPreferencesMenu(true); // Enable the 'Preferences' menu item
            app.addApplicationListener(this); // Register this ApplicationListener
        }
    }

    public void handleAbout(ApplicationEvent event) {
        event.setHandled(true);
        MainFrame mainFrame = WindowManager.getCurrentMainFrame();

        // Do nothing (return) when in 'no events mode'
        if(mainFrame.getNoEventsMode())
            return;

        new AboutDialog(mainFrame).showDialog();
    }

    public void handlePreferences(ApplicationEvent event) {
        event.setHandled(true);
        MainFrame mainFrame = WindowManager.getCurrentMainFrame();

        // Do nothing (return) when in 'no events mode'
        if(mainFrame.getNoEventsMode())
            return;

        ActionManager.performAction(com.mucommander.ui.action.impl.ShowPreferencesAction.Descriptor.ACTION_ID, mainFrame);
    }

    public void handleQuit(ApplicationEvent event) {
        boolean handled = true;

        // Ask the user for confirmation and abort if user refused to quit.
        if(!QuitDialog.confirmQuit()) {
            handled = false;
        }

        // We got a green -> quit!
        WindowManager.quit();

        // Accept or reject the request to quit based on user's response
        event.setHandled(handled);
    }

    public void handleOpenApplication(ApplicationEvent event) {
        // No-op
    }

    public void handleReOpenApplication(ApplicationEvent event) {
        // No-op
    }

    public void handleOpenFile(ApplicationEvent event) {
        // Wait until the application has been launched. This step is required to properly handle the case where the
        // application is launched with a file to open, for instance when drag-n-dropping a file to the Dock icon
        // when muCommander is not started yet. In this case, this method is called while Launcher is still busy
        // launching the application (no mainframe exists yet).
        Launcher.waitUntilLaunched();

        AbstractFile file = FileFactory.getFile(event.getFilename());
        FolderPanel activePanel = WindowManager.getCurrentMainFrame().getActivePanel();
        if (file.isBrowsable())
            activePanel.tryChangeCurrentFolder(file);
        else
            activePanel.tryChangeCurrentFolder(file.getParent(), file, false);
    }

    public void handlePrintFile(ApplicationEvent event) {
        // No-op
    }
}
