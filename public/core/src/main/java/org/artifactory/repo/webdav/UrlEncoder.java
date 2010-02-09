/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.repo.webdav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.BitSet;

/**
 * This class is very similar to the java.net.URLEncoder class.
 * <p/>
 * Unfortunately, with java.net.URLEncoder there is no way to specify to the java.net.URLEncoder which characters should
 * NOT be encoded.
 * <p/>
 * This code was moved from DefaultServlet.java
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */
public class UrlEncoder {
    protected static final char[] hexadecimal =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'A', 'B', 'C', 'D', 'E', 'F'};

    //Array containing the safe characters set.
    protected BitSet safeCharacters = new BitSet(256);

    public UrlEncoder() {
        for (char i = 'a'; i <= 'z'; i++) {
            addSafeCharacter(i);
        }
        for (char i = 'A'; i <= 'Z'; i++) {
            addSafeCharacter(i);
        }
        for (char i = '0'; i <= '9'; i++) {
            addSafeCharacter(i);
        }
    }

    public void addSafeCharacter(char c) {
        safeCharacters.set(c);
    }

    public String encode(String path) {
        int maxBytesPerChar = 10;
        StringBuffer rewrittenPath = new StringBuffer(path.length());
        ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);
        OutputStreamWriter writer;
        try {
            writer = new OutputStreamWriter(buf, "UTF8");
        } catch (Exception e) {
            e.printStackTrace();
            writer = new OutputStreamWriter(buf);
        }

        for (int i = 0; i < path.length(); i++) {
            int c = (int) path.charAt(i);
            if (safeCharacters.get(c)) {
                rewrittenPath.append((char) c);
            } else {
                // convert to external encoding before hex conversion
                try {
                    writer.write((char) c);
                    writer.flush();
                } catch (IOException e) {
                    buf.reset();
                    continue;
                }
                byte[] ba = buf.toByteArray();
                for (byte toEncode : ba) {
                    // Converting each byte in the buffer
                    rewrittenPath.append('%');
                    int low = toEncode & 0x0f;
                    int high = (toEncode & 0xf0) >> 4;
                    rewrittenPath.append(hexadecimal[high]);
                    rewrittenPath.append(hexadecimal[low]);
                }
                buf.reset();
            }
        }
        return rewrittenPath.toString();
    }
}