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

package org.artifactory.jcr.version;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.jcr.version.v150.JcrMetadataConverter;
import org.artifactory.jcr.version.v150.RepoConfigConverter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionComparator;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import javax.jcr.Session;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mostly just for tracking of the jcr verion used and removing the workspaces and index folders on version change
 *
 * @author yoavl
 * @author noamt
 */
public enum JcrVersion implements SubConfigElementVersion {

    v146(ArtifactoryVersion.v130beta5, ArtifactoryVersion.v208, null, null),
    v150(ArtifactoryVersion.v210, ArtifactoryVersion.v213, new RepoConfigConverter(), new JcrMetadataConverter()),
    v160(ArtifactoryVersion.v220, ArtifactoryVersion.getCurrent(), null, null);

    private static final Logger log = LoggerFactory.getLogger(JcrVersion.class);

    private final VersionComparator comparator;
    private final ConfigurationConverter<ArtifactoryHome> preInitConverter;
    private final ConfigurationConverter<Session> jcrConverter;

    /**
     * Main constructor
     *
     * @param from             Start version
     * @param until            End version
     * @param preInitConverter Configuration converter required for the specified range
     * @param jcrConverter
     */
    JcrVersion(ArtifactoryVersion from, ArtifactoryVersion until,
            ConfigurationConverter<ArtifactoryHome> preInitConverter,
            ConfigurationConverter<Session> jcrConverter) {
        this.preInitConverter = preInitConverter;
        this.jcrConverter = jcrConverter;
        this.comparator = new VersionComparator(this, from, until);
    }

    /**
     * Updates the workspace (on every version change) and calls the converters relevant for the specified version
     * range
     *
     * @param artifactoryHome
     */
    public void preInitConvert(ArtifactoryHome artifactoryHome) {
        //Always perform this when a version changes
        updateCurrentWorkspaces(artifactoryHome);

        // First create the list of converters to apply
        List<ConfigurationConverter<ArtifactoryHome>> converters = Lists.newArrayList();

        // All converters of versions above me needs to be executed in sequence
        JcrVersion[] versions = JcrVersion.values();
        for (JcrVersion version : versions) {
            if (version.ordinal() >= ordinal() && version.preInitConverter != null) {
                converters.add(version.preInitConverter);
            }
        }

        for (ConfigurationConverter<ArtifactoryHome> converter : converters) {
            converter.convert(artifactoryHome);
        }
    }

    public void convert(Session rawSession) {
        // First create the list of converters to apply
        List<ConfigurationConverter<Session>> converters = new ArrayList<ConfigurationConverter<Session>>();

        // All converters of versions above me needs to be executed in sequence
        JcrVersion[] versions = JcrVersion.values();
        for (JcrVersion version : versions) {
            if (version.ordinal() >= this.ordinal() && version.jcrConverter != null) {
                converters.add(version.jcrConverter);
            }
        }

        for (ConfigurationConverter<Session> converter : converters) {
            converter.convert(rawSession);
        }
    }


    public VersionComparator getComparator() {
        return comparator;
    }

    /**
     * Should be called on every version change.<br> Renames the existing workspaces dir so that a new one will be
     * created by reading the updated repo.xml.
     *
     * @param artifactoryHome Artifactory Home
     */
    private void updateCurrentWorkspaces(ArtifactoryHome artifactoryHome) {
        CompoundVersionDetails originalStorageVersion = artifactoryHome.getOriginalVersionDetails();
        //Make the loaded repo.xml overwrite any existing workspace by clearing data/workspaces
        File workspacesDir = new File(artifactoryHome.getDataDir(), "workspaces");
        String origVersionValue = originalStorageVersion.getVersion().getValue();
        File origWorkspacesDir = new File(artifactoryHome.getDataDir(), "workspaces." + origVersionValue + ".orig");
        try {
            FileUtils.deleteDirectory(origWorkspacesDir);
        } catch (IOException e) {
            log.warn("Failed to remove original workspaces at {}.", origWorkspacesDir.getAbsolutePath());
        }
        try {
            FileUtils.copyDirectory(workspacesDir, origWorkspacesDir);
        } catch (IOException e) {
            log.warn("Failed to backup original workspaces from {} to {}.", workspacesDir.getAbsolutePath(),
                    origWorkspacesDir.getAbsolutePath());
        }
        try {
            FileUtils.deleteDirectory(workspacesDir);
        } catch (IOException e) {
            log.warn("Failed to remove workspaces at {}.", workspacesDir.getAbsolutePath());
        }
    }
}