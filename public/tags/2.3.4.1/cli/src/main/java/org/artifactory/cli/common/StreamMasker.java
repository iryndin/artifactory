/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.cli.common;

import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;

/**
 * A utility which outputs a prompt to the console and masks the user input<p> <a href="http://forums.sun.com/thread.jspa?threadID=490728">Original
 * implementation</a>
 *
 * @author Noam Tenne
 */
public class StreamMasker implements Runnable {
    private static final String TEN_BLANKS = StringUtils.repeat(Character.toString(' '), 10);
    private final PrintStream out;
    private final String setCursorToStart;
    private final String promptOverwrite;

    /**
     * Constructor
     *
     * @param out    A given output stream
     * @param prompt The prompt to display on the given output
     * @throws IllegalArgumentException Might occur when given null or illegal inputs and utils
     */
    public StreamMasker(PrintStream out, String prompt) throws IllegalArgumentException {
        if (out == null) {
            throw new IllegalArgumentException("Output stream cannot be nul.");
        }
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt to display on output cannot be null");
        }
        if (prompt.contains("\n")) {
            throw new IllegalArgumentException("Prompt cannot contain the new line ('\\n') char");
        }
        if (prompt.contains("\r")) {
            throw new IllegalArgumentException("prompt cannot contain the carriage return ('\\r') char");
        }
        this.out = out;
        setCursorToStart = "\r";
        promptOverwrite =
                // sets cursor back to beginning of line:
                setCursorToStart +
                        // writes prompt
                        prompt +
                        // writes 10 blanks beyond the prompt to mask out any input
                        TEN_BLANKS +
                        // sets cursor back to beginning of line
                        setCursorToStart +
                        //writes prompt again; the cursor will now be positioned immediately after prompt
                        prompt;

        // ensure that is written at least once
        out.print(promptOverwrite);
    }

    /**
     * Overwrites the printed prompt
     */
    public void printPromptOverwrite() {
        out.print(promptOverwrite);
    }

    /**
     * Runs the thread which masks the input
     *
     * @throws RuntimeException Might occur on io error
     */
    public void run() throws RuntimeException {
        int priorityOriginal = Thread.currentThread().getPriority();
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            while (!Thread.currentThread().isInterrupted()) {
                out.print(promptOverwrite);
                //confirm that everything was written correctly
                if (out.checkError()) {
                    throw new RuntimeException("an I/O problem was detected in out");
                }
                Thread.sleep(1);
            }
        }
        catch (InterruptedException ie) {
            // resets the interrupted status
            Thread.currentThread().interrupt();
        }
        finally {
            out.print(setCursorToStart);
            for (int i = 0; i < promptOverwrite.length(); i++) {
                out.print(' ');
            }
            out.print(setCursorToStart);

            Thread.currentThread().setPriority(priorityOriginal);
        }
    }
}