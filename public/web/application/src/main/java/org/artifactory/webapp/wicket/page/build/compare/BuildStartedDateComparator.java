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

package org.artifactory.webapp.wicket.page.build.compare;

import org.apache.commons.lang.StringUtils;
import org.artifactory.build.api.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * A custom comparator for sorting a build's started date objects
 *
 * @author Noam Y. Tenne
 */
public class BuildStartedDateComparator implements Comparator<Build> {

    public int compare(Build build1, Build build2) {
        if ((build1 == null) || (build2 == null)) {
            return 0;
        }

        String build1Started = build1.getStarted();
        String build2Started = build2.getStarted();

        if (StringUtils.isBlank(build1Started) || StringUtils.isBlank(build2Started)) {
            return 0;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        try {
            Date build1StartedDate = simpleDateFormat.parse(build1Started);
            Date build2StartedDate = simpleDateFormat.parse(build2Started);

            return build1StartedDate.compareTo(build2StartedDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
