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

package com.mucommander.ui.viewer.text;

import com.google.common.io.Closeables;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.io.EncodingDetector;
import com.mucommander.conf.MuConfigurations;
import com.mucommander.conf.MuPreference;
import com.mucommander.conf.MuPreferences;
import com.mucommander.text.Translator;
import com.mucommander.ui.dialog.DialogOwner;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.encoding.EncodingListener;
import com.mucommander.ui.encoding.EncodingMenu;
import com.mucommander.ui.helper.MenuToolkit;
import com.mucommander.ui.helper.MnemonicHelper;
import com.mucommander.ui.viewer.FileFrame;
import com.mucommander.ui.viewer.FileViewer;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * A simple text viewer. Most of the implementation is located in {@link TextEditorImpl}.
 *
 * @author Maxence Bernard, Arik Hadas
 */
class TextViewer extends FileViewer implements EncodingListener {

    public final static String CUSTOM_FULL_SCREEN_EVENT = "CUSTOM_FULL_SCREEN_EVENT";

    private TextProcessor textViewerImpl;

    /**
     * Menu items
     */
    // Menus //
    private JMenu editMenu;
    private JMenu viewMenu;
    // Items //
    private JMenuItem copyItem;
    private JMenuItem selectAllItem;
    private JMenuItem findItem;
    private JMenuItem findNextItem;
    private JMenuItem findPreviousItem;
    private JMenuItem toggleLineWrapItem;
    private JMenuItem toggleLineNumbersItem;

    private String encoding;

    TextViewer() {
        this(new TextViewerImpl());
    }

    TextViewer(TextProcessor textViewerImpl) {
        this.textViewerImpl = textViewerImpl;

        setComponentToPresent(textViewerImpl.getTextArea());

        showLineNumbers(MuConfigurations.getPreferences().getVariable(MuPreference.LINE_NUMBERS, MuPreferences.DEFAULT_LINE_NUMBERS));
        textViewerImpl.wrap(MuConfigurations.getPreferences().getVariable(MuPreference.LINE_WRAP, MuPreferences.DEFAULT_LINE_WRAP));

        initMenuBarItems();
    }

    @Override
    public void setFrame(FileFrame frame) {
        super.setFrame(frame);

        frame.setFullScreen(isTextPresenterDisplayedInFullScreen());

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK), CUSTOM_FULL_SCREEN_EVENT);
        getActionMap().put(CUSTOM_FULL_SCREEN_EVENT, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getFrame().setFullScreen(!getFrame().isFullScreen());
            }
        });
    }

    void startEditing(AbstractFile file, DocumentListener documentListener) throws IOException {
        encoding = detectEncoding(file);
        loadDocument(file, encoding, documentListener);
    }

    private String detectEncoding(AbstractFile file) throws IOException {
        InputStream in = null;

        try {
            in = file.getInputStream();

            String encoding = EncodingDetector.detectEncoding(in);
            // If the encoding could not be detected or the detected encoding is not supported, default to UTF-8
            if (encoding == null || !Charset.isSupported(encoding)) encoding = "UTF-8";

            return encoding;
        } finally {
            Closeables.closeQuietly(in);
        }
    }

    void loadDocument(AbstractFile file, String encoding, DocumentListener documentListener) throws IOException {
        textViewerImpl.read(file, encoding);

        if (documentListener != null)
            textViewerImpl.addDocumentListener(documentListener);
    }

    @Override
    public JMenuBar getMenuBar() {
        JMenuBar menuBar = super.getMenuBar();

        // Encoding menu
        EncodingMenu encodingMenu = new EncodingMenu(new DialogOwner(getFrame()), encoding);
        encodingMenu.addEncodingListener(this);

        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(encodingMenu);

        return menuBar;
    }

    @Override
    public void beforeCloseHook() {
        MuConfigurations.getPreferences().setVariable(MuPreference.LINE_WRAP, textViewerImpl.isWrap());
        MuConfigurations.getPreferences().setVariable(MuPreference.LINE_NUMBERS, getRowHeader().getView() != null);

        setTextPresenterDisplayedInFullScreen(getFrame().isFullScreen());
        textViewerImpl.beforeCloseHook();
    }

    String getEncoding() {
        return encoding;
    }

    protected void showLineNumbers(boolean show) {
        setRowHeaderView(show ? new TextLineNumbersPanel(textViewerImpl.getTextArea()) : null);
    }

    protected void initMenuBarItems() {
        // Edit menu
        editMenu = new JMenu(Translator.get("text_viewer.edit"));
        MnemonicHelper menuItemMnemonicHelper = new MnemonicHelper();

        copyItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_viewer.copy"), menuItemMnemonicHelper, null, this);

        selectAllItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_viewer.select_all"), menuItemMnemonicHelper, null, this);
        editMenu.addSeparator();

        findItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_viewer.find"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), this);
        findNextItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_viewer.find_next"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), this);
        findPreviousItem = MenuToolkit.addMenuItem(editMenu, Translator.get("text_viewer.find_previous"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK), this);

        // View menu
        viewMenu = new JMenu(Translator.get("text_viewer.view"));

        toggleLineWrapItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, Translator.get("text_viewer.line_wrap"), menuItemMnemonicHelper, null, this);
        toggleLineWrapItem.setSelected(textViewerImpl.isWrap());
        toggleLineNumbersItem = MenuToolkit.addCheckBoxMenuItem(viewMenu, Translator.get("text_viewer.line_numbers"), menuItemMnemonicHelper, null, this);
        toggleLineNumbersItem.setSelected(getRowHeader().getView() != null);
    }

    ///////////////////////////////
    // FileViewer implementation //
    ///////////////////////////////

    @Override
    public void show(AbstractFile file) throws IOException {
        startEditing(file, null);
    }

    ///////////////////////////////////
    // ActionListener implementation //
    ///////////////////////////////////

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == copyItem)
            textViewerImpl.copy();
        else if (source == selectAllItem)
            textViewerImpl.selectAll();
        else if (source == findItem)
            textViewerImpl.find();
        else if (source == findNextItem)
            textViewerImpl.findNext();
        else if (source == findPreviousItem)
            textViewerImpl.findPrevious();
        else if (source == toggleLineWrapItem)
            textViewerImpl.wrap(toggleLineWrapItem.isSelected());
        else if (source == toggleLineNumbersItem)
            setRowHeaderView(toggleLineNumbersItem.isSelected() ? new TextLineNumbersPanel(textViewerImpl.getTextArea()) : null);
        else
            super.actionPerformed(e);
    }

    /////////////////////////////////////
    // EncodingListener implementation //
    /////////////////////////////////////

    public void encodingChanged(Object source, String oldEncoding, String newEncoding) {
        try {
            // Reload the file using the new encoding
            loadDocument(getCurrentFile(), newEncoding, null);
        } catch (IOException ex) {
            InformationDialog.showErrorDialog(getFrame(), Translator.get("read_error"), Translator.get("file_editor.cannot_read_file", getCurrentFile().getName()));
        }
    }
}
