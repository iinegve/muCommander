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
import com.mucommander.commons.io.bom.BOMInputStream;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.*;

/**
 * Text editor implementation used by {@link TextViewer} and {@link TextEditor}.
 *
 * @author Maxence Bernard, Mariusz Jakubowski, Nicolas Rinaudo, Arik Hadas
 */
class TextEditorImpl extends TextProcessor {

    @Override
    protected void initTextArea() {
        super.initTextArea();
        textArea.setEditable(true);
    }

    void read(AbstractFile file, String encoding) throws IOException {
        InputStream input = file.getInputStream();

        // If the encoding is UTF-something, wrap the stream in a BOMInputStream to filter out the byte-order mark
        // (see ticket #245)
        if (encoding.toLowerCase().startsWith("utf")) {
            input = new BOMInputStream(input);
        }

        BufferedReader isr = new BufferedReader(new InputStreamReader(input, encoding), 4096);
        try {
            textArea.read(isr, null);
        } finally {
            Closeables.closeQuietly(isr);
        }

        // Move cursor to the top
        textArea.setCaretPosition(0);
    }

    void write(Writer writer) throws IOException {
        Document document = textArea.getDocument();

        try {
            textArea.getUI().getEditorKit(textArea).write(new BufferedWriter(writer), document, 0, document.getLength());
        } catch (BadLocationException e) {
            throw new IOException(e.getMessage());
        }
    }
}
