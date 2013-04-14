package com.mucommander.ui.viewer.text;

import com.mucommander.commons.file.AbstractFile;
import com.mucommander.ui.theme.*;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Eugene Morozov
 */
abstract class TextProcessor implements ThemeListener {

    private String searchString;
    protected JTextArea textArea;


    protected TextProcessor() {
        // Initialize text area
        initTextArea();

        // Listen to theme changes to update the text area if it is visible
        ThemeManager.addCurrentThemeListener(this);
    }

    protected void initTextArea() {
        textArea = new JTextArea() {
            @Override
            public Insets getInsets() {
                return new Insets(4, 3, 4, 3);
            }
        };

        // Use theme colors and font
        textArea.setForeground(ThemeManager.getCurrentColor(Theme.EDITOR_FOREGROUND_COLOR));
        textArea.setCaretColor(ThemeManager.getCurrentColor(Theme.EDITOR_FOREGROUND_COLOR));
        textArea.setBackground(ThemeManager.getCurrentColor(Theme.EDITOR_BACKGROUND_COLOR));
        textArea.setSelectedTextColor(ThemeManager.getCurrentColor(Theme.EDITOR_SELECTED_FOREGROUND_COLOR));
        textArea.setSelectionColor(ThemeManager.getCurrentColor(Theme.EDITOR_SELECTED_BACKGROUND_COLOR));
        textArea.setFont(ThemeManager.getCurrentFont(Theme.EDITOR_FONT));

        textArea.setWrapStyleWord(true);

        textArea.addMouseWheelListener(new MouseWheelListener() {

            /**
             * Mouse events bubble up until finding a component with a relative listener.
             * That's why in case we get an event that needs to initiate its default behavior,
             * we just bubble it up to the parent component of the JTextArea.
             */
            public void mouseWheelMoved(MouseWheelEvent e) {
                boolean isCtrlPressed = (e.getModifiers() & KeyEvent.CTRL_MASK) != 0;
                if (isCtrlPressed) {
                    Font currentFont = textArea.getFont();
                    int currentFontSize = currentFont.getSize();
                    boolean rotationUp = e.getWheelRotation() < 0;
                    if ((!rotationUp && currentFontSize > 1) || rotationUp) {
                        Font newFont = new Font(currentFont.getName(), currentFont.getStyle(), currentFontSize + (rotationUp ? 1 : -1));
                        textArea.setFont(newFont);
                    }
                } else {
                    textArea.getParent().dispatchEvent(e);
                    processMouseWheelMove(e);
                }
            }
        });
    }

    protected void processMouseWheelMove(MouseWheelEvent e) {
        //Do nothing here. Subject for subclassing.
    }

    /////////////////
    // Search code //
    /////////////////

    void find() {
        FindDialog findDialog = new FindDialog(null);

        if (findDialog.wasValidated()) {
            searchString = findDialog.getSearchString().toLowerCase();

            if (!searchString.equals(""))
                doSearch(0, true);
        }

        // Request the focus on the text area which could be lost after the Find dialog was disposed
        textArea.requestFocus();
    }

    void findNext() {
        doSearch(textArea.getSelectionEnd(), true);
    }

    void findPrevious() {
        doSearch(textArea.getSelectionStart() - 1, false);
    }

    private String getTextLC() {
        return textArea.getText().toLowerCase();
    }

    private void doSearch(int startPos, boolean forward) {
        if (searchString == null || searchString.length() == 0)
            return;
        int pos;
        if (forward) {
            pos = getTextLC().indexOf(searchString, startPos);
        } else {
            pos = getTextLC().lastIndexOf(searchString, startPos);
        }
        if (pos >= 0) {
            textArea.select(pos, pos + searchString.length());
        } else {
            // Beep when no match has been found.
            // The beep method is called from a separate thread because this method seems to lock until the beep has
            // been played entirely. If the 'Find next' shortcut is left pressed, a series of beeps will be played when
            // the end of the file is reached, and we don't want those beeps to played one after the other as to:
            // 1/ not lock the event thread
            // 2/ have those beeps to end rather sooner than later
            new Thread() {
                @Override
                public void run() {
                    Toolkit.getDefaultToolkit().beep();
                }
            }.start();
        }
    }

    public boolean isWrap() {
        return textArea.getLineWrap();
    }

    ////////////////////////////
    // Package-access methods //
    ////////////////////////////

    void wrap(boolean isWrap) {
        textArea.setLineWrap(isWrap);
        textArea.repaint();
    }

    void copy() {
        textArea.copy();
    }

    void cut() {
        textArea.cut();
    }

    void paste() {
        textArea.paste();
    }

    void selectAll() {
        textArea.selectAll();
    }

    void requestFocus() {
        textArea.requestFocus();
    }

    JTextArea getTextArea() {
        return textArea;
    }

    void addDocumentListener(DocumentListener documentListener) {
        textArea.getDocument().addDocumentListener(documentListener);
    }

    abstract void read(AbstractFile file, String encoding) throws IOException;

    abstract void write(Writer writer) throws IOException;

    public void beforeCloseHook() {
        //Do nothing
    }

    //////////////////////////////////
    // ThemeListener implementation //
    //////////////////////////////////

    /**
     * Receives theme color changes notifications.
     */
    public void colorChanged(ColorChangedEvent event) {
        switch (event.getColorId()) {
            case Theme.EDITOR_FOREGROUND_COLOR:
                textArea.setForeground(event.getColor());
                break;

            case Theme.EDITOR_BACKGROUND_COLOR:
                textArea.setBackground(event.getColor());
                break;

            case Theme.EDITOR_SELECTED_FOREGROUND_COLOR:
                textArea.setSelectedTextColor(event.getColor());
                break;

            case Theme.EDITOR_SELECTED_BACKGROUND_COLOR:
                textArea.setSelectionColor(event.getColor());
                break;
        }
    }

    /**
     * Receives theme font changes notifications.
     */
    public void fontChanged(FontChangedEvent event) {
        if (event.getFontId() == Theme.EDITOR_FONT)
            textArea.setFont(event.getFont());
    }


}
