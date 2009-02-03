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
package org.artifactory.update;

import org.apache.commons.lang.StringUtils;
import org.artifactory.ArtifactoryConstants;

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
            ArtifactoryVersion.v130beta1);

    private final String xsdUri;
    private final String xsdLocation;
    private final ArtifactoryVersion untilArtifactoryVersion;
    private final XmlConversion[] conversions;

    ArtifactoryConfigVersion(String xsdUri, String xsdLocation,
                             ArtifactoryVersion untilArtifactoryVersion, XmlConversion... conversions) {
        this.xsdUri = xsdUri;
        this.xsdLocation = xsdLocation;
        this.conversions = conversions;
        this.untilArtifactoryVersion = untilArtifactoryVersion;
    }

    public String convert(String in) {
        // Really not efficient but we don't care
        String result = StringUtils.replace(in, xsdUri, OneThree.getXsdUri());
        result = StringUtils.replace(result, xsdLocation, OneThree.getXsdLocation());
        ArtifactoryConfigVersion[] versions = ArtifactoryConfigVersion.values();
        for (ArtifactoryConfigVersion version : versions) {
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
    backup {
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
            int endBackupDir = in.indexOf(END_BACKUP_DIR);
            int backupCronExp = in.indexOf(BACKUP_CRON_EXP);
            int endBackupCronExp = in.indexOf(END_BACKUP_CRON_EXP);
            // TODO: More sanity checks on the index values
            if (backupCronExp == -1) {
                // No cron => No backup setup
                // TODO: If one index is found, should be removed all together
            } else if (backupDir == -1) {
                // Just cron no dir
                StringBuilder result = new StringBuilder();
                result.append(in, 0, backupCronExp);
                result.append("<backup>\n<cronExp>");
                result.append(in, backupCronExp + BACKUP_CRON_EXP.length(), endBackupCronExp);
                result.append("</cronExp>\n</backup>\n");
                result.append(in, endBackupCronExp + END_BACKUP_CRON_EXP.length(), in.length());
                return result.toString();
            } else {
                StringBuilder result = new StringBuilder();
                result.append(in, 0, backupDir);
                result.append("<backup>\n<dir>");
                result.append(in, backupDir + BACKUP_DIR.length(), endBackupDir);
                result.append("</dir>\n<cronExp>");
                result.append(in, backupCronExp + BACKUP_CRON_EXP.length(), endBackupCronExp);
                result.append("</cronExp>\n</backup>\n");
                result.append(in, endBackupCronExp + END_BACKUP_CRON_EXP.length(), in.length());
                return result.toString();
            }
            return in;
        }};

    private static final String BACKUP_DIR = "<backupDir>";
    private static final String BACKUP_CRON_EXP = "<backupCronExp>";
    private static final String END_BACKUP_DIR = "</backupDir>";
    private static final String END_BACKUP_CRON_EXP = "</backupCronExp>";

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
