/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.converters;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.storage.db.properties.model.DbProperties;
import org.artifactory.storage.db.properties.service.ArtifactoryCommonDbPropertiesService;
import org.artifactory.version.ArtifactoryVersionReader;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Author: gidis
 */
public class VersionProviderImpl implements VersionProvider {
    private static final Logger log = LoggerFactory.getLogger(VersionProviderImpl.class);

    /**
     * The current running version, discovered during runtime.
     */
    private CompoundVersionDetails runningVersion;
    /**
     * The initial version read from the local properties file on startup. Effective only until the conversion starts.
     */
    private CompoundVersionDetails originalHomeVersion;

    /**
     * The initial version read from the cluster properties file on startup. Effective only until the conversion starts.
     */
    private CompoundVersionDetails originalHaVersion;

    /**
     * The initial version read from the database. Not null after db ready.
     */
    private CompoundVersionDetails originalServiceVersion;

    private ArtifactoryHome artifactoryHome;


    public VersionProviderImpl(ArtifactoryHome artifactoryHome) {
        this.artifactoryHome = artifactoryHome;
        init();
    }

    private void init() {
        try {
            runningVersion = artifactoryHome.readRunningArtifactoryVersion();
            originalHomeVersion = runningVersion;
            originalHaVersion = runningVersion;
            File homeArtifactoryPropertiesFile = artifactoryHome.getHomeArtifactoryPropertiesFile();
            updateOriginalVersion(homeArtifactoryPropertiesFile, false);
            File haArtifactoryPropertiesFile = artifactoryHome.getHaArtifactoryPropertiesFile();
            updateOriginalVersion(haArtifactoryPropertiesFile, true);
        } catch (Exception e) {
            log.error("Fail to load artifactory.properties", e);
        }

    }

    private void updateOriginalVersion(File artifactoryPropertiesFile, boolean isHa) throws IOException {
        try {
            // If the properties file doesn't exists, then create it
            if (!artifactoryPropertiesFile.exists()) {
                if (isHa) {
                    artifactoryHome.writeBundledHaArtifactoryProperties();
                } else {
                    artifactoryHome.writeBundledHomeArtifactoryProperties();
                }
            }
            // Store the original version - may need to activate converters based on it
            CompoundVersionDetails readVersion = ArtifactoryVersionReader.read(artifactoryPropertiesFile);
            if (isHa) {
                originalHaVersion = readVersion;
                artifactoryHome.writeBundledHaArtifactoryProperties();
            } else {
                originalHomeVersion = readVersion;
                artifactoryHome.writeBundledHomeArtifactoryProperties();
            }
            // Reload ArtifactorySystemProperties
            Properties properties = new Properties();
            try (FileInputStream inStream = new FileInputStream(artifactoryPropertiesFile)) {
                properties.load(inStream);
            }
            for (Object o : properties.keySet()) {
                artifactoryHome.getArtifactoryProperties().setProperty((String) o, properties.getProperty((String) o));
            }
        } catch (Exception e) {
            // Do nothing
        }

    }

    @Override
    public CompoundVersionDetails getOriginalHomeVersionDetails() {
        return originalHomeVersion;
    }

    @Override
    public CompoundVersionDetails getOriginalHaVersionDetails() {
        return originalHaVersion;
    }

    @Override
    public CompoundVersionDetails getRunningVersionDetails() {
        return runningVersion;
    }

    /**
     * The originalServiceVersion value is null until access to db is allowed
     *
     * @return
     */
    @Override
    public CompoundVersionDetails getOriginalServiceVersionDetails() {
        return originalServiceVersion;
    }

    public boolean startedFromDifferentVersion() {
        return (getOriginalHomeVersionDetails() != null) && (!getOriginalHomeVersionDetails().isCurrent());
    }

    public boolean isDbPropertiesVersionCompatible() {
        // For now just compare the version
        return originalHomeVersion != null && originalHomeVersion.getVersion().before(runningVersion.getVersion());
    }

    public void dbReady() {
        // If the dbProperties doesn't exists then we can assume that source version is consistent
        originalServiceVersion = runningVersion;
        ArtifactoryCommonDbPropertiesService dbPropertiesService = ContextHelper.get().beanForType(
                ArtifactoryCommonDbPropertiesService.class);
        DbProperties dbProperties = null;
        try {
            dbProperties = dbPropertiesService.getDbProperties();
        } catch (Exception e) {
            // If the db properties doesn't exists it is ok to assume that the originalServiceVersion= originalHomeVersion
            originalServiceVersion = originalHomeVersion;
        }
        if (dbProperties != null) {
            originalServiceVersion = getDbCompoundVersionDetails(dbProperties);
        }
    }

    public static CompoundVersionDetails getDbCompoundVersionDetails(DbProperties dbProperties) {
        return ArtifactoryVersionReader.getCompoundVersionDetails(
                dbProperties.getArtifactoryVersion(),
                getRevisionStringFromInt(dbProperties.getArtifactoryRevision()),
                "" + dbProperties.getArtifactoryRelease());
    }

    private static String getRevisionStringFromInt(int rev) {
        if (rev <= 0 || rev == Integer.MAX_VALUE) {
            return "" + Integer.MAX_VALUE;
        }
        return "" + rev;
    }

    public boolean originalIsCurrentVersion(File artifactoryPropertiesFile) {
        CompoundVersionDetails readVersion = ArtifactoryVersionReader.read(artifactoryPropertiesFile);
        return readVersion.getVersion().isCurrent();
    }
}