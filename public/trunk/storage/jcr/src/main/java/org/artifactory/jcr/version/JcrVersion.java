/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.version.v150.JcrMetadataConverter;
import org.artifactory.jcr.version.v150.RepoConfigConverterV150;
import org.artifactory.jcr.version.v160.MetadataNamePropertyConverter;
import org.artifactory.jcr.version.v210.RepoConfigConverterV210;
import org.artifactory.jcr.version.v225.LatestBuildPropertyConverter;
import org.artifactory.jcr.version.v228.DeleteForConsistencyFixConverter;
import org.artifactory.jcr.version.v228.FileStoreLayoutConverter;
import org.artifactory.jcr.version.v228.MarkerFileConverter;
import org.artifactory.jcr.version.v228.MavenPluginPropertyConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionComparator;
import org.artifactory.version.converter.ConfigurationConverter;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * Mostly just for tracking of the jcr version used and removing the workspaces and index folders on version change
 *
 * @author yoavl
 * @author noamt
 */
public enum JcrVersion implements SubConfigElementVersion {

    /**
     * IMPORTANT: before adding a new post-init converter, note that when converting from a version earlier than 1.6,
     * All post init converters are called -twice-. This occurs since the JCR metadata converter inits the JCR service
     * (invokes the post init), converts the data, destroys the service and re-inits it (invokes the post init once
     * more)
     */

    v146(ArtifactoryVersion.v130beta5, ArtifactoryVersion.v208, null, null, null),
    v150(ArtifactoryVersion.v210, ArtifactoryVersion.v213, new RepoConfigConverterV150(), new JcrMetadataConverter(),
            null),
    v160(ArtifactoryVersion.v220, ArtifactoryVersion.v221, null, null, new MetadataNamePropertyConverter()),
    v161(ArtifactoryVersion.v222, ArtifactoryVersion.v223, new RepoConfigConverterV210(), null, null),
    v210(ArtifactoryVersion.v224, ArtifactoryVersion.v225, null, null, new LatestBuildPropertyConverter()),
    v225(ArtifactoryVersion.v230, ArtifactoryVersion.v2341, new FileStoreLayoutConverter(),
            new CompositeJcrConverter(new DeleteForConsistencyFixConverter(), new MavenPluginPropertyConverter()),
            new MarkerFileConverter()),
    v228(ArtifactoryVersion.v240, ArtifactoryVersion.getCurrent(), null, null, null);

    private final VersionComparator comparator;
    private final ConfigurationConverter<ArtifactoryHome> preInitConverter;
    private final ConfigurationConverter<Session> jcrConverter;
    private final ConfigurationConverter<JcrSession> postInitConverter;

    /**
     * Main constructor
     *
     * @param from              Start version
     * @param until             End version
     * @param preInitConverter  Configuration converter required for the specified range
     * @param jcrConverter
     * @param postInitConverter Configuration converters that should be called after the JCR service is initialized
     */
    JcrVersion(ArtifactoryVersion from, ArtifactoryVersion until,
            ConfigurationConverter<ArtifactoryHome> preInitConverter,
            ConfigurationConverter<Session> jcrConverter,
            ConfigurationConverter<JcrSession> postInitConverter) {
        this.preInitConverter = preInitConverter;
        this.jcrConverter = jcrConverter;
        this.postInitConverter = postInitConverter;
        this.comparator = new VersionComparator(this, from, until);
    }

    /**
     * Updates the workspace (on every version change) and calls the converters relevant for the specified version
     * range
     *
     * @param artifactoryHome
     */
    public void preInitConvert(ArtifactoryHome artifactoryHome) {

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

    /**
     * Performs the conversions that should be done only after the initialization of the JCR service
     *
     * @param unManaged Unmanaged JCR session
     */
    public void postInitConvert(JcrSession unManaged) {
        // First create the list of converters to apply
        List<ConfigurationConverter<JcrSession>> converters = Lists.newArrayList();

        // All converters of versions above me needs to be executed in sequence
        JcrVersion[] versions = JcrVersion.values();
        for (JcrVersion version : versions) {
            if (version.ordinal() >= ordinal() && version.postInitConverter != null) {
                converters.add(version.postInitConverter);
            }
        }

        for (ConfigurationConverter<JcrSession> converter : converters) {
            converter.convert(unManaged);
        }
    }

    public VersionComparator getComparator() {
        return comparator;
    }
}
