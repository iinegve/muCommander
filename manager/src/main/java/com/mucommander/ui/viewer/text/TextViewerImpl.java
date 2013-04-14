package com.mucommander.ui.viewer.text;

import com.google.common.io.Closeables;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.io.bom.BOMInputStream;
import com.mucommander.text.Translator;
import com.mucommander.ui.dialog.DialogToolkit;
import com.mucommander.ui.icon.SpinningDial;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Morozov
 */
public class TextViewerImpl extends TextProcessor {

    private BufferedReader reader;

    /**
     * Number of read symbols from the {@link #reader}.
     */
    private int count = 0;

    /**
     * Buffer to download file into textArea.
     * It is equal to one line width to append additional
     * lines of text during scrolling.
     * <p/>
     * Assumption of initial textArea's line width
     * (should be enough for most of the cases)
     */
    private char[] buffer = new char[200];


    @Override
    protected void initTextArea() {
        super.initTextArea();
        textArea.setEditable(false);

        textArea.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                switch (e.getScrollType()) {
                    case MouseWheelEvent.WHEEL_UNIT_SCROLL:
                        if (e.getUnitsToScroll() > 0) {
                            try {
                                readRows(e.getUnitsToScroll());
                            } catch (IOException ex) {
                                //TODO: show dialog with error message - cannot read from stream anymore
                            }
                        }
                        break;
                }
            }
        });

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            readRows(1);
                            break;
                        case KeyEvent.VK_PAGE_DOWN:
                            readRows(calcHeightInRows());
                            break;
                    }
                } catch (IOException ex) {
                    //TODO: show dialog with error message - cannot read from stream anymore
                }
            }
        });

        //Reinitialize buffer size in case of textArea's size is changed
        textArea.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                //Each time text is appended into textArea it's resizing itself.
                //Thus rely on size of its parent
                initializeBuffer(calcWidthInSymbols());
            }
        });
    }

    /**
     * Calculates height in rows for visible part of {@link #textArea}
     *
     * @return number of rows
     */
    private int calcHeightInRows() {
        FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());
        return textArea.getParent().getHeight() / metrics.getHeight();
    }

    /**
     * Calculates width in symbols for visible part of {@link #textArea}
     *
     * @return width in symbols
     */
    private int calcWidthInSymbols() {
        FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());

        //standard procedure to get symbol width
        return textArea.getParent().getWidth() / metrics.charWidth('m');
    }

    /**
     * Initializes internal buffer according to width of view size.
     * Calculates number of columns according to specified font and
     * creates buffer of such size.
     * The buffer is equal to one line of textArea.
     *
     * @param width current view width
     */
    private void initializeBuffer(int width) {
        if (width != buffer.length) {
            buffer = new char[width];
        }
    }

    @Override
    void read(AbstractFile file, String encoding) throws IOException {
        InputStream input = file.getInputStream();

        // If the encoding is UTF-something, wrap the stream in a BOMInputStream to filter out the byte-order mark
        // (see ticket #245)
        if (encoding.toLowerCase().startsWith("utf")) {
            input = new BOMInputStream(input);
        }

        reader = new BufferedReader(new InputStreamReader(input, encoding), 8192);
        final int rowsToRead = calcHeightInRows();
        readRows(rowsToRead == 0 ? 50 : rowsToRead); //read first 50 lines at the beginning

        textArea.setCaretPosition(0); // Move cursor to the top
    }

    /**
     * Reads next portion of data from {@link #reader}
     * <p/>
     * Doesn't require to have additional buffer for smooth scrolling.
     * Smooth will be supplied by BufferedReader buffer size.
     * <p/>
     * It's in times more than one general line width.
     * <p/>
     * In case there is line wrapping turned off it requires to read whole line
     * in original file. That could be comparatively long (in case of really long
     * line it could even kill the app), but in other case it gives much
     * inconvenience for user experience.
     */
    private void readRows(final int rowsToRead) throws IOException {
        SwingWorker worker = new SwingWorker() {

            private JFrame frame;

            private JFrame createFrame() {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.BOTH;

                JFrame frame = new JFrame(Translator.get("loading"));
                JPanel contentPane = new JPanel(new GridBagLayout());
                JLabel label = new JLabel(Translator.get("loading"));
                label.setIcon(new SpinningDial(24, 24, true));
                contentPane.add(label, gbc);

                frame.setContentPane(contentPane);
                frame.setSize(400, 400);
                frame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
                DialogToolkit.centerOnScreen(frame);
                System.out.println("Frame created");
                return frame;
            }

            @Override
            protected Object doInBackground() throws Exception {
                (frame = createFrame()).setVisible(true);

                TimeUnit.SECONDS.sleep(5);
                int rows = rowsToRead;
                if (count != -1) {
                    StringBuilder sb = new StringBuilder(rows * buffer.length);
                    while (rows-- > 0 && count != -1) {
                        if (textArea.getLineWrap()) {
                            count = reader.read(buffer);
                            if (count != -1) {
                                sb.append(new String(buffer, 0, count));
                            }
                        } else {
                            String line = reader.readLine();
                            if (line != null) {
                                sb.append(line).append("\n");
                            } else {
                                count = -1;
                            }
                        }
                    }
                    textArea.append(sb.toString());
                }
                return null;
            }

            @Override
            protected void done() {
                frame.dispose();
            }
        };
        worker.run();
    }

    @Override
    void write(Writer writer) throws IOException {
        //Do nothing - it's file viewer
    }

    @Override
    public void beforeCloseHook() {
        Closeables.closeQuietly(reader);
    }
}
