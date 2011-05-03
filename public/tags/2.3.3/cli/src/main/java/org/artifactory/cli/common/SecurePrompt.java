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

import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Arrays;

/**
 * Uses the stream masker to prompt for a password with masked input and secures the input
 *
 * @author Noam Tenne
 */
public abstract class SecurePrompt {
    private SecurePrompt() {
        // utility class
    }

    /**
     * Sends a prompt message to the console with masked input, and secure it (the input)
     *
     * @param prompt The prompt to display on the given output
     * @return char[] - Character array containing secured input
     * @throws IOException          Might occur on stream errors
     * @throws InterruptedException Might occur on threading error
     */
    public static char[] readConsoleSecure(String prompt) throws IOException, InterruptedException {
        StreamMasker masker = new StreamMasker(System.out, prompt);
        Thread threadMasking = new Thread(masker, "EncryptUtil_StreamMasker");
        threadMasking.setPriority(Thread.NORM_PRIORITY);
        threadMasking.start();

        masker.printPromptOverwrite();

        // block this current thread (allowing masker to mask all user input)
        // while the user is in the middle of typing the password.
        PushbackInputStream pin = new PushbackInputStream(System.in);
        int b = pin.read();

        threadMasking.interrupt();
        threadMasking.join();

        // check for errors:
        if (b == -1) {
            throw new IOException("end-of-file was detected in System.in without any data being read");
        }
        if (System.out.checkError()) {
            throw new IOException("an I/O problem was detected in System.out");
        }

        pin.unread(b);
        return readLineSecure(pin);
    }

    /**
     * Reads the input line in a secure manner
     *
     * @param in The stream to read from
     * @return char[] - Character array containing secured input
     * @throws IllegalArgumentException Might occur if the input stream is null
     * @throws IOException              Might occur on io error
     */
    private static char[] readLineSecure(PushbackInputStream in) throws IllegalArgumentException, IOException {
        if (in == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }

        char[] buffer = null;
        try {
            buffer = new char[128];
            int offset = 0;

            loop:
            while (true) {
                int c = in.read();
                switch (c) {
                    case -1:
                    case '\n':
                        break loop;

                    case '\r':
                        int c2 = in.read();
                        if ((c2 != '\n') && (c2 != -1)) {
                            // guarantees that mac & dos line end sequences are completely read thru but not beyond
                            in.unread(c2);
                        }
                        break loop;

                    default:
                        buffer = checkBuffer(buffer, offset);
                        buffer[offset++] = (char) c;
                        break;
                }
            }

            char[] result = new char[offset];
            System.arraycopy(buffer, 0, result, 0, offset);
            return result;
        }
        finally {
            eraseChars(buffer);
        }
    }

    /**
     * Validates the buffer size
     *
     * @param buffer Buffer
     * @param offset Offest
     * @return char[] - Secured character array
     * @throws IllegalArgumentException Might occur if buffer is null or if the offset is invalid
     */
    private static char[] checkBuffer(char[] buffer, int offset) throws IllegalArgumentException {
        if (buffer == null) {
            throw new IllegalArgumentException("The buffer cannot be null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be < 0");
        }

        if (offset < buffer.length) {
            return buffer;
        } else {
            try {
                char[] bufferNew = new char[offset + 128];
                System.arraycopy(buffer, 0, bufferNew, 0, buffer.length);
                return bufferNew;
            }
            finally {
                eraseChars(buffer);
            }
        }
    }


    /**
     * If buffer is not null, fills buffer with space (' ') chars.
     *
     * @param buffer Buffer
     */
    private static void eraseChars(char[] buffer) {
        if (buffer != null) {
            Arrays.fill(buffer, ' ');
        }
    }
}
