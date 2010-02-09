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

package org.artifactory.info;

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;


/**
 * An enum of info groups that writes all the information to the log
 *
 * @author Noam Tenne
 */
public enum InfoWriter {
    user(UserPropInfo.class, "User Info"),
    host(HostPropInfo.class, "Host Info"),
    artifactory(ArtifactoryPropInfo.class, "Artifactory Info"),
    javaSys(JavaSysPropInfo.class, "Java System Info"),
    classPath(ClassPathPropInfo.class, "Java Class Path Info");

    private static final Logger log = LoggerFactory.getLogger(InfoWriter.class);

    /**
     * Info group class
     */
    private final Class<? extends BasePropInfoGroup> infoGroup;
    /**
     * Group name (used for title)
     */
    private final String groupName;
    /**
     * The format of the list to be printed
     */
    private static String listFormat = "   %1$-60s| %2$s%n";

    /**
     * Main constructor
     *
     * @param infoGroup InfoGroupClass
     * @param groupName Name of info group
     */
    InfoWriter(Class<? extends BasePropInfoGroup> infoGroup, String groupName) {
        this.infoGroup = infoGroup;
        this.groupName = groupName;
    }

    /**
     * Dumps the info from all the groups in the enum to the log
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void writeInfo() throws IllegalAccessException, InstantiationException {
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            //Create main title
            sb.append(String.format("%n%n SYSTEM INFORMATION DUMP%n"));
            sb.append(String.format(" =======================%n"));
            for (InfoWriter writer : InfoWriter.values()) {
                BasePropInfoGroup group = writer.infoGroup.newInstance();
                //Create group title
                sb.append(String.format("%n ")).
                        append(writer.groupName).append(String.format("%n"));
                sb.append(String.format(" ========================%n"));
                //Iterate over all info objects
                for (InfoObject infoObject : group.getInfo()) {
                    String propertyName = infoObject.getPropertyName();
                    String value = infoObject.getPropertyValue();
                    String formattedLine = String.format(listFormat, propertyName, value);
                    sb.append(formattedLine);
                }
            }

            //Dump the info to the log
            String wholeDump = sb.toString();
            log.info(wholeDump);
        }
    }
}
