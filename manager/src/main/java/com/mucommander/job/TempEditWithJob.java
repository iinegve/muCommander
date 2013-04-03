package com.mucommander.job;

import com.mucommander.command.Command;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.util.FileSet;
import com.mucommander.process.AbstractProcess;
import com.mucommander.process.ProcessRunner;
import com.mucommander.text.Translator;
import com.mucommander.ui.dialog.file.FileCollisionDialog;
import com.mucommander.ui.dialog.file.ProgressDialog;
import com.mucommander.ui.main.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * This job copies a file to a temporary folder, makes the temporary file editable,
 * executes it with a specific command and take care of uploading changes on close.
 * The temporary files are deleted when the JVM terminates.
 *
 * @author Ivan Mamontov
 */
public class TempEditWithJob extends TempCopyJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempEditWithJob.class);

    /**
     * The command to execute, appended with the temporary file path(s)
     */
    private final Command command;

    /**
     * File to execute
     */
    private AbstractFile fileToOpen;

    /**
     * Timestamp after copying the {@link #fileToOpen} to temp directory
     */
    private long lastModified;

    /**
     * Creates a new <code>TempEditWithJob</code> that operates on a file.
     *
     * @param progressDialog   the ProgressDialog that monitors this job
     * @param mainFrame        the MainFrame this job is attached to
     * @param remoteFileToOpen file to copy to a temporary location and execute
     * @param command          the command used to execute the temporary file
     */
    public TempEditWithJob(ProgressDialog progressDialog, MainFrame mainFrame, AbstractFile remoteFileToOpen, Command command) {
        super(progressDialog, mainFrame, new FileSet(remoteFileToOpen.getParent(), remoteFileToOpen));
        this.command = command;
    }

    @Override
    protected boolean processFile(AbstractFile file, Object recurseParams) {
        if (!super.processFile(file, recurseParams))
            return false;

        //I would like to verify that it was once
        fileToOpen = currentDestFile;
        lastModified = getLastModified();
        return true;
    }

    /**
     * Returns the time that the file denoted by {@link #fileToOpen} was
     * last modified.
     *
     * @return A <code>long</code> value representing the time the file was
     *         last modified, measured in milliseconds since the epoch
     *         (00:00:00 GMT, January 1, 1970), or <code>0L</code> if the
     *         file does not exist or if an I/O error occurs
     */
    private long getLastModified() {
        return new File(fileToOpen.getPath()).lastModified();
    }

    @Override
    protected void jobCompleted() {
        super.jobCompleted();

        try {
            AbstractProcess process = ProcessRunner.execute(command.getTokens(fileToOpen), baseDestFolder);
            //dialogue is no longer needed
            getProgressDialog().dispose();

            process.waitFor();
            //check if file was modified since opened in editor, in order to avoid superfluous copying
            if (lastModified != getLastModified()) {
                ProgressDialog copyBackDialog = new ProgressDialog(getMainFrame(), Translator.get("copy_dialog.copying"));
                CopyJob copyBackJob = new CopyJob(copyBackDialog, getMainFrame(), new FileSet(baseDestFolder, fileToOpen), getBaseSourceFolder(), getCurrentFile().getName(), CopyJob.COPY_MODE, FileCollisionDialog.OVERWRITE_ACTION);
                copyBackDialog.start(copyBackJob);
            }
        } catch (Exception e) {
            LOGGER.debug("Caught exception executing " + command + " with file " + fileToOpen, e);
        }
    }
}
