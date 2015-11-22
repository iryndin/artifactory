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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds known Date patterns and corresponding templates
 *
 * @author Michael Pasternak
 */
public class DatePatternsHolder {
    private static final Pattern datePattern1 = Pattern.compile(DatePattern.PATTERN_1.getPattern());
    private static final Pattern datePattern2 = Pattern.compile(DatePattern.PATTERN_2.getPattern());
    private static final Pattern datePattern3 = Pattern.compile(DatePattern.PATTERN_3.getPattern());
    private static final Pattern datePattern4 = Pattern.compile(DatePattern.PATTERN_4.getPattern());
    private static final Pattern dateZipPattern = Pattern.compile(DatePattern.PATTERN_ZIP_LOG.getPattern());

    /**
     * Produces {@link Matcher} from str according to datePattern1
     *
     * @param str string to compile
     *
     * @return {@link Matcher}
     */
    public static Matcher getMatcher1(String str) {
        return datePattern1.matcher(str);
    }

    /**
     * Produces {@link Matcher} from str according to datePattern2
     *
     * @param str string to compile
     *
     * @return {@link Matcher}
     */
    public static Matcher getMatcher2(String str) {
        return datePattern2.matcher(str);
    }

    /**
     * Produces {@link Matcher} from str according to datePattern3
     *
     * @param str string to compile
     *
     * @return {@link Matcher}
     */
    public static Matcher getMatcher3(String str) {
        return datePattern3.matcher(str);
    }

    /**
     * Produces {@link Matcher} from str according to datePattern4
     *
     * @param str string to compile
     *
     * @return {@link Matcher}
     */
    public static Matcher getMatcher4(String str) {
        return datePattern4.matcher(str);
    }

    /**
     * Produces {@link Matcher} from str according to dateZipPattern
     *
     * @param str string to compile
     *
     * @return {@link Matcher}
     */
    public static Matcher getDateZipPattern(String str) {
        return dateZipPattern.matcher(str);
    }


    public enum DatePattern {
        PATTERN_1("(19|20|30)\\d\\d[- /. /_](0[1-9]|1[012])[- /. /_](0[1-9]|[12][0-9]|3[01])",
                "yyyy_MM_dd", "yyyy.MM.dd", "yyyy-MM-dd"
        ),
        PATTERN_2("(0[1-9]|1[012])[- /. /_](0[1-9]|[12][0-9]|3[01])[- /. /_](19|20|30)",
                "MM_dd_yyyy", "MM.dd.yyyy", "MM-dd-yyyy"
        ),
        PATTERN_3("(19|20|30)\\d\\d[- /. /_](0[1-9]|[12][0-9]|3[01])[- /. /_](0[1-9]|1[012])",
                "yyyy_dd_MM", "yyyy.dd.MM", "yyyy-dd-MM"
        ),
        PATTERN_4("(0[1-9]|[12][0-9]|3[01])[- /. /_](0[1-9]|1[012])[- /. /_](19|20|30)",
                "dd_MM_yyyy", "dd.MM.yyyy", "dd-MM-yyyy"
        ),
        PATTERN_ZIP_LOG("artifactory\\.[0-9].*log\\.zip",
                "\\d+"
        );

        DatePattern(String pattern, String... patternTemplates) {
            this.pattern = pattern;
            this.patternTemplates = patternTemplates;
        }

        private final String pattern;
        private final String[] patternTemplates;

        public String[] getPatternTemplates() {
            return patternTemplates;
        }

        public String getPattern() {
            return pattern;
        }
    };
}
