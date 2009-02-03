package org.artifactory.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An enum of info groups that writes all the information to the log
 *
 * @author Noam Tenne
 */
public enum InfoWriter {
    user(UserInfo.class, "User Info"),
    host(HostInfo.class, "Host Info"),
    artifactory(ArtifactoryInfo.class, "Artifactory Info"),
    javaSys(JavaSysInfo.class, "Java System Info"),
    classPath(ClassPathInfo.class, "Java Class Path Info");

    private static final Logger log = LoggerFactory.getLogger(InfoWriter.class);

    /**
     * Info group class
     */
    private final Class<? extends BaseInfoGroup> infoGroup;
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
    InfoWriter(Class<? extends BaseInfoGroup> infoGroup, String groupName) {
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
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            //Create main title
            sb.append(String.format("%n%n SYSTEM INFORMATION DUMP%n"));
            sb.append(String.format(" =======================%n"));
            for (InfoWriter writer : InfoWriter.values()) {
                BaseInfoGroup group = writer.infoGroup.newInstance();
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
            log.debug(wholeDump);
        }
    }
}
