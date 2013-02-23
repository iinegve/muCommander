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


package com.mucommander.ui.dialog;

import java.awt.*;


/**
 * FocusDialog is a modal dialog which extends JDialog to provide the following additional functionalities :
 * <ul>
 *   <li>focus can be requested on a specified JComponent once the dialog has been made visible</li>
 *   <li>the screen location of the window can be set relatively to a Component specified in the constructor</li>
 *   <li>a minimum and/or maximum size can be specified and will be used by {@link #pack()} to calculate the effective dialog size</li>
 *   <li>by default, the 'Escape' key disposes the dialog, this can be disabled using {@link #setKeyboardDisposalEnabled(boolean)}</li>
 * </ul>
 * @author Maxence Bernard
 */
public class FocusDialog extends AbstractDialog {

    public FocusDialog(Frame owner, String title, Component locationRelativeComp) {
        super(owner, title, locationRelativeComp, true);
    }

    public FocusDialog(Dialog owner, String title, Component locationRelativeComp) {
        super(owner, title, locationRelativeComp, true);
    }
}
