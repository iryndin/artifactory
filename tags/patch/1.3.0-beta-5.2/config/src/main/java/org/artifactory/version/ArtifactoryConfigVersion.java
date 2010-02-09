/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.version;

import org.apache.commons.lang.StringUtils;
import org.artifactory.common.ArtifactoryConstants;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: freds Date: May 27, 2008 Time: 11:49:48 PM
 */
public enum ArtifactoryConfigVersion {
    OneZero("http://artifactory.jfrog.org/xsd/1.0.0",
            "http://www.jfrog.org/xsd/artifactory-v1_0_0.xsd",
            ArtifactoryVersion.v122,
            XmlConversion.uniqueVersions, XmlConversion.nonUniqueVersions,
            XmlConversion.xmlIds, XmlConversion.backup) {
        @Override
        public String convert(String in) {
            // Support for very old location
            in = StringUtils.replace(in,
                    "http://artifactory.jfrog.org/xsd/artifactory-v1_0_0.xsd",
                    OneThree.getXsdLocation());
            return super.convert(in);
        }},
    OneOne("http://artifactory.jfrog.org/xsd/1.1.0",
            "http://www.jfrog.org/xsd/artifactory-v1_1_0.xsd",
            ArtifactoryVersion.v125,
            XmlConversion.nonUnique),
    OneTwo("http://artifactory.jfrog.org/xsd/1.2.0",
            "http://www.jfrog.org/xsd/artifactory-v1_2_0.xsd",
            ArtifactoryVersion.v125u1,
            XmlConversion.anonAccess),
    OneThree("http://artifactory.jfrog.org/xsd/1.3.0",
            "http://www.jfrog.org/xsd/artifactory-v1_3_0.xsd",
            ArtifactoryVersion.v130beta2,
            XmlConversion.backupList, XmlConversion.anonAccessUnderSecurity),
    OneThreeOne("http://artifactory.jfrog.org/xsd/1.3.1",
            "http://www.jfrog.org/xsd/artifactory-v1_3_1.xsd",
            ArtifactoryVersion.v130beta3,
            XmlConversion.ldapSettings),
    OneThreeTwo("http://artifactory.jfrog.org/xsd/1.3.2",
            "http://www.jfrog.org/xsd/artifactory-v1_3_2.xsd",
            ArtifactoryVersion.getCurrent());

    private final String xsdUri;
    private final String xsdLocation;
    private final ArtifactoryVersion untilArtifactoryVersion;
    private final XmlConversion[] conversions;

    public static ArtifactoryConfigVersion getCurrent() {
        ArtifactoryConfigVersion[] versions = ArtifactoryConfigVersion.values();
        return versions[versions.length - 1];
    }

    ArtifactoryConfigVersion(String xsdUri, String xsdLocation,
            ArtifactoryVersion untilArtifactoryVersion, XmlConversion... conversions) {
        this.xsdUri = xsdUri;
        this.xsdLocation = xsdLocation;
        this.conversions = conversions;
        this.untilArtifactoryVersion = untilArtifactoryVersion;
    }

    public String convert(String in) {
        // Really not efficient but we don't care, Really we don't
        String result = StringUtils.replace(in, xsdUri, getCurrent().getXsdUri());
        result = StringUtils.replace(result, xsdLocation, getCurrent().getXsdLocation());
        ArtifactoryConfigVersion[] versions = ArtifactoryConfigVersion.values();
        for (ArtifactoryConfigVersion version : versions) {
            // All versions above me needs to be executed
            if (version.ordinal() >= ordinal() && version.getConversions() != null) {
                for (XmlConversion conversion : version.getConversions()) {
                    result = conversion.convert(result);
                }
            }
        }
        return result;
    }

    public String getXsdUri() {
        return xsdUri;
    }

    public String getXsdLocation() {
        return xsdLocation;
    }

    public XmlConversion[] getConversions() {
        return conversions;
    }

    public ArtifactoryVersion getUntilArtifactoryVersion() {
        return untilArtifactoryVersion;
    }

    public static ArtifactoryConfigVersion getConfigVersion(String configXml) {
        ArtifactoryConfigVersion[] configVersions = values();
        // First check sanity of conversion
        // Make sure a conversion is never activated twice
        Set<XmlConversion> allConversions = new HashSet<XmlConversion>();
        ArtifactoryConfigVersion configVersionTest = null;
        for (ArtifactoryConfigVersion configVersion : configVersions) {
            XmlConversion[] versionConversions = configVersion.getConversions();
            for (XmlConversion conversion : versionConversions) {
                if (allConversions.contains(conversion)) {
                    throw new IllegalStateException(
                            "XML Conversion element can only be used once!\n" +
                                    "XML Conversion " + conversion + " is used in " +
                                    configVersion + " but was already used.");
                }
                allConversions.add(conversion);
            }
            configVersionTest = configVersion;
        }
        // The last should be current
        if (configVersionTest != getCurrent()) {
            throw new IllegalStateException("The last config version " + configVersionTest +
                    " is not the current one " + getCurrent());
        }
        // The last should not have any conversion
        XmlConversion[] currentConversions = getCurrent().getConversions();
        if (currentConversions != null && currentConversions.length > 0) {
            throw new IllegalStateException("The last config version " + configVersionTest +
                    " should have any conversions declared " + currentConversions);
        }
        // Find correct version by schema URI
        for (ArtifactoryConfigVersion configVersion : configVersions) {
            if (configXml.contains(configVersion.getXsdUri())) {
                return configVersion;
            }
        }
        return null;
    }
}

enum XmlConversion {
    anonAccess("anonDownloadsAllowed", "anonAccessEnabled"),
    nonUnique("nonunique", "non-unique"),
    maxUniqueSnapshots("maxUniqueSnapshots", "maxUniqueSnapshotsNumber"),
    uniqueVersions("<useSnapshotUniqueVersions>true</useSnapshotUniqueVersions>",
            "<snapshotVersionBehavior>deployer</snapshotVersionBehavior>"),
    nonUniqueVersions("<useSnapshotUniqueVersions>false</useSnapshotUniqueVersions>",
            "<snapshotVersionBehavior>non-unique</snapshotVersionBehavior>"),
    xmlIds {
        @Override
        public String convert(String in) {
            Set<Map.Entry<String, String>> substKeys =
                    ArtifactoryConstants.substituteRepoKeys.entrySet();
            String result = in;
            for (Map.Entry<String, String> substKey : substKeys) {
                result = StringUtils.replace(result,
                        "<key>" + substKey.getKey() + "</key>",
                        "<key>" + substKey.getValue() + "</key>");
            }
            return result;
        }},
    anonAccessUnderSecurity {
        @Override
        public String convert(String in) {
            // Need to convert:
            // "anonAccessEnabled", "anonAccessEnabled"
            // <anonAccessEnabled>XXX</anonAccessEnabled>
            // to:
            // <security>
            //     <anonAccessEnabled>XXX</anonAccessEnabled>
            //     .....
            // </security>
            //
            // If no annonAccess, or commented nothing to do
            int annonAccessStart = in.indexOf("<anonAccessEnabled>");
            if (annonAccessStart == -1) {
                return in;
            } else {
                int lastCommentStart = in.substring(0, annonAccessStart).lastIndexOf("<!--");
                if (lastCommentStart != -1) {
                    // If nothing between comment and start it's commented ignore
                    if (lastCommentStart + 4 >= annonAccessStart) {
                        return in;
                    }
                    if (in.substring(lastCommentStart + 4, annonAccessStart).indexOf("-->") == -1) {
                        return in;
                    }
                }
            }
            int annonAccessEnd = in.indexOf("</anonAccessEnabled>") +
                    "</anonAccessEnabled>".length();

            StringBuilder result = new StringBuilder();
            result.append(in, 0, annonAccessStart);

            int securityStart = in.indexOf(SECURITY_START);
            int securityEnd = in.indexOf(SECURITY_END);
            if (securityStart == -1 || securityEnd == -1) {
                // If fileUploadMaxSizeMb exists and no security before
                // needs to move security just before backups
                int fileUpLoadStart = in.indexOf("<fileUploadMaxSizeMb>");
                int backups;
                if (fileUpLoadStart != -1) {
                    backups = in.indexOf(BACKUPS);
                    result.append(in.substring(annonAccessEnd, backups));
                } else {
                    backups = annonAccessEnd;
                }
                // security tag not found or in comment
                result.append(SECURITY_START).append(in, annonAccessStart, annonAccessEnd);
                result.append(SECURITY_END);
                result.append(in, backups, in.length());
            } else {
                result.append(in, annonAccessEnd, securityStart);
                result.append(SECURITY_START).append(in, annonAccessStart, annonAccessEnd);
                result.append("<ldapSettings>");
                // Remove authenticationMethod
                int authMethodEnd = in.indexOf("</authenticationMethod>");
                if (authMethodEnd != -1) {
                    authMethodEnd += "</authenticationMethod>".length();
                } else {
                    authMethodEnd = securityStart + SECURITY_START.length();
                }
                // Remove searchAuthPasswordAttributeName
                int startSearchAttr = in.indexOf("<searchAuthPasswordAttributeName>");
                if (startSearchAttr == -1) {
                    startSearchAttr = securityEnd;
                }
                result.append(in, authMethodEnd, startSearchAttr);
                result.append("</ldapSettings>");
                result.append(SECURITY_END);
                result.append(in, securityEnd + SECURITY_END.length(), in.length());
            }

            return result.toString();
        }},
    backup {
        @Override
        public String convert(String in) {
            // Need to convert:
            //  <backupDir>XX</backupDir>
            //  <backupCronExp>YY</backupCronExp>
            // to:
            //  <backup>
            //    <dir>XX</dir>
            //    <cronExp>YY</cronExp>
            //  </backup>
            // The XSD specified a sequence type so the tags order is respected.
            int backupDir = in.indexOf(BACKUP_DIR);
            int endBackupDir = in.indexOf(BACKUP_DIR_END);
            int backupCronExp = in.indexOf(BACKUP_CRON_EXP);
            int endBackupCronExp = in.indexOf(BACKUP_CRON_EXP_END);
            // TODO: More sanity checks on the index values
            if (backupCronExp == -1) {
                // No cron => No backup setup
                // TODO: If one index is found, should be removed all together
            } else if (backupDir == -1) {
                // Just cron no dir
                StringBuilder result = new StringBuilder();
                result.append(in, 0, backupCronExp);
                result.append(BACKUP).append("\n<cronExp>");
                result.append(in, backupCronExp + BACKUP_CRON_EXP.length(), endBackupCronExp);
                result.append("</cronExp>\n").append(BACKUP_END).append("\n");
                result.append(in, endBackupCronExp + BACKUP_CRON_EXP_END.length(), in.length());
                return result.toString();
            } else {
                StringBuilder result = new StringBuilder();
                result.append(in, 0, backupDir);
                result.append(BACKUP).append("\n<dir>");
                result.append(in, backupDir + BACKUP_DIR.length(), endBackupDir);
                result.append("</dir>\n<cronExp>");
                result.append(in, backupCronExp + BACKUP_CRON_EXP.length(), endBackupCronExp);
                result.append("</cronExp>\n").append(BACKUP_END).append("\n");
                result.append(in, endBackupCronExp + BACKUP_CRON_EXP_END.length(), in.length());
                return result.toString();
            }
            return in;
        }},
    backupList {
        @Override
        public String convert(String in) {
            // Need to convert:
            //  <backup>...</backup>
            // to:
            //  <backups>
            //    <backup>...</backup>
            //  </backups>
            int backup = in.indexOf(BACKUP);
            int endBackup = in.indexOf(BACKUP_END);
            if (backup == -1 || endBackup == -1) {
                // No backup setup
            } else {
                StringBuilder result = new StringBuilder();
                result.append(in, 0, backup);
                result.append(BACKUPS).append("\n");
                result.append(in, backup, endBackup + BACKUP_END.length());
                result.append(BACKUPS_END).append("\n");
                result.append(in, endBackup + BACKUP_END.length(), in.length());
                return result.toString();
            }
            return in;
        }},
    ldapSettings {
        @Override
        public String convert(String in) {
            // Need to convert:
            //  <userDnPattern>...</userDnPattern>
            // to:
            //  <authenticationPatterns><authenticationPattern>
            //    <userDnPattern>...</userDnPattern>
            //  </authenticationPattern></authenticationPatterns>
            int userDnPattern = in.indexOf("<userDnPattern>");
            int endUserDnPattern = in.indexOf(USER_DN_PATTERN_END);
            if (userDnPattern == -1 || endUserDnPattern == -1) {
                // No userDnPattern setup
            } else {
                StringBuilder result = new StringBuilder();
                result.append(in, 0, userDnPattern);
                result.append("<authenticationPatterns><authenticationPattern>").append("\n");
                result.append(in, userDnPattern, endUserDnPattern + USER_DN_PATTERN_END.length());
                result.append("</authenticationPattern></authenticationPatterns>").append("\n");
                result.append(in, endUserDnPattern + USER_DN_PATTERN_END.length(), in.length());
                return result.toString();
            }
            return in;
        }};
    private static final String USER_DN_PATTERN_END = "</userDnPattern>";

    private static final String BACKUPS = "<backups>";
    private static final String BACKUPS_END = "</backups>";
    private static final String BACKUP = "<backup>";
    private static final String BACKUP_END = "</backup>";
    private static final String BACKUP_DIR = "<backupDir>";
    private static final String BACKUP_DIR_END = "</backupDir>";
    private static final String BACKUP_CRON_EXP = "<backupCronExp>";
    private static final String BACKUP_CRON_EXP_END = "</backupCronExp>";

    private static final String SECURITY_START = "<security>";
    private static final String SECURITY_END = "</security>";

    private String toFind;
    private String replacement;

    public String convert(String in) {
        return StringUtils.replace(in, toFind, replacement);
    }

    XmlConversion() {
    }

    XmlConversion(String toFind, String replacement) {
        this.toFind = toFind;
        this.replacement = replacement;
    }
}