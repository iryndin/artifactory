/*
* Artifactory is a binaries repository manager.
* Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.support.utils;

import com.google.common.base.Strings;

import java.util.stream.IntStream;

/**
 * @author Michael Pasternak
 */
public class StringBuilderWrapper implements java.io.Serializable, CharSequence{
    public static final String NEW_LINE = getLineSeparator();

    private static String getLineSeparator() {
        return System.getProperty("line.separator") != null ?
                System.getProperty("line.separator") : "\n";
    }

    private final StringBuilder sb;

    public StringBuilderWrapper() {
        sb = new StringBuilder();
    }

    public StringBuilderWrapper(String string) {
        sb = new StringBuilder(string);
    }

    public StringBuilderWrapper(CharSequence charSequence) {
        sb = new StringBuilder(charSequence);
    }

    /**
     * @param content
     */
    public StringBuilderWrapper append(String content) {
        if (!Strings.isNullOrEmpty(content)) {
            sb.append(content);
            sb.append(NEW_LINE);
            sb.append(NEW_LINE);
        }
        return this;
    }

    /**
     * @param object
     */
    public void append(Object object) {
        if (object != null) {
            sb.append(object);
            sb.append(NEW_LINE);
        }
    }

    /**
     * @param title
     * @param content
     */
    public void append(Object title, Object content) {
        if (title != null) {
            sb.append(title);
            sb.append(": ");
            sb.append(content);
            sb.append(NEW_LINE);
        }
    }

    public void newLine() {
        sb.append(NEW_LINE);
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    @Override
    public IntStream chars() {
        return sb.chars();
    }

    @Override
    public IntStream codePoints() {
        return sb.codePoints();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
