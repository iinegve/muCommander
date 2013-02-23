package com.mucommander.ui.dialog;

import java.awt.*;

/**
 * Specifically non modal dialog
 *
 * @author Eugene Morozov
 */
public class NonModalDialog extends AbstractDialog {

    public NonModalDialog(Frame owner, String title, Component locationRelativeComp) {
        super(owner, title, locationRelativeComp, false);
    }
}
