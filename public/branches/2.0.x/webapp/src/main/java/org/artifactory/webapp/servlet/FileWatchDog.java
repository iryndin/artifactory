package org.artifactory.webapp.servlet;

import java.io.File;

/**
 * Check every now and then that a certain file has not changed. If it has, then call the {@link
 * #doOnChange} method. Based on the log4j FileWatchdog implementation.
 *
 * @author Yossi Shaul
 */
public abstract class FileWatchDog extends Thread {
    /**
     * The default delay between every file modification check, set to 60 seconds.
     */
    static final public long DEFAULT_DELAY = 60000;
    /**
     * The name of the file to observe  for changes.
     */
    protected String filename;

    /**
     * The delay to observe between every check. By default set {@link #DEFAULT_DELAY}.
     */
    protected long delay = DEFAULT_DELAY;

    File file;
    long lastModified = 0;
    boolean warnedAlready = false;
    boolean interrupted = false;

    protected FileWatchDog(String filename) {
        this.filename = filename;
        file = new File(filename);
        setDaemon(true);
        checkAndConfigure();
    }

    /**
     * Set the delay to observe between each check of the file changes.
     */
    public void setDelay(long delay) {
        this.delay = delay;
    }

    abstract protected void doOnChange();

    protected void checkAndConfigure() {
        boolean fileExists;
        try {
            fileExists = file.exists();
        } catch (SecurityException e) {
            System.err.printf("Was not allowed to read check file existance, file:[%s].", filename);
            interrupted = true;// there is no point in continuing
            return;
        }

        if (fileExists) {
            long l = file.lastModified();
            if (l > lastModified) {
                lastModified = l;
                doOnChange();
                warnedAlready = false;
            }
        } else {
            if (!warnedAlready) {
                System.err.printf("[%s] does not exist.", filename);
                warnedAlready = true;
            }
        }
    }

    public void run() {
        while (!interrupted) {
            try {
                Thread.currentThread().sleep(delay);
            } catch (InterruptedException e) {
                // no interruption expected
            }
            checkAndConfigure();
        }
    }
}
