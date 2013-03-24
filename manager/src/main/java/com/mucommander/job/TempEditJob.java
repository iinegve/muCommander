package com.mucommander.job;

import com.mucommander.command.Command;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.util.FileSet;
import com.mucommander.process.AbstractProcess;
import com.mucommander.process.ProcessRunner;
import com.mucommander.ui.dialog.file.ProgressDialog;
import com.mucommander.ui.main.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This job copies a file or a set of files to a temporary folder, makes the temporary file(s) editable and
 * executes them with a specific command. The temporary files are deleted when the JVM terminates.
 * <p/>
 * <p>It is important to understand that when this job operates on a set of files, a process is started for each file
 * to execute, so this operation should require confirmation by the user before being attempted.</p>
 *
 * @author Ivan Mamontov
 */
public class TempEditJob extends TempCopyJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempEditJob.class);

    /**
     * The command to execute, appended with the temporary file path(s)
     */
    private Command command;

    /**
     * Files to execute
     */
    private FileSet filesToOpen;

    /**
     * This list is populated with temporary files, as they are created by processFile()
     */
    private FileSet tempFiles;

    /**
     * Creates a new <code>TempEditJob</code> that operates on a single file.
     *
     * @param progressDialog the ProgressDialog that monitors this job
     * @param mainFrame      the MainFrame this job is attached to
     * @param fileToOpen     the file to copy to a temporary location and execute
     * @param command        the command used to execute the temporary file
     */
    public TempEditJob(ProgressDialog progressDialog, MainFrame mainFrame, AbstractFile fileToOpen, Command command) {
        this(progressDialog, mainFrame, new FileSet(fileToOpen.getParent(), fileToOpen), command);
    }

    /**
     * Creates a new <code>TempEditJob</code> that operates on a set of files.
     * Only a single command get executed, operating on all files.
     *
     * @param progressDialog the ProgressDialog that monitors this job
     * @param mainFrame      the MainFrame this job is attached to
     * @param filesToOpen    the set of files to copy to a temporary location and execute
     * @param command        the command used to execute the temporary file
     */
    public TempEditJob(ProgressDialog progressDialog, MainFrame mainFrame, FileSet filesToOpen, Command command) {
        super(progressDialog, mainFrame, filesToOpen);
        this.command = command;
        this.filesToOpen = filesToOpen;
        tempFiles = new FileSet(baseDestFolder);
    }

    @Override
    protected boolean processFile(AbstractFile file, Object recurseParams) {
        if (!super.processFile(file, recurseParams))
            return false;

        // Add the file to the list of files to open, only if it is one of the top-level files
        if (filesToOpen.indexOf(file) != -1) {
            tempFiles.add(currentDestFile);
        }
        return true;
    }

    @Override
    protected void jobCompleted() {
        super.jobCompleted();

        try {
            AbstractProcess process = ProcessRunner.execute(command.getTokens(tempFiles), baseDestFolder);
            process.waitFor();
        } catch (Exception e) {
            LOGGER.debug("Caught exception executing " + command + " " + tempFiles, e);
        }
    }
}
