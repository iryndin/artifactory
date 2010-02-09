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

package org.artifactory.spring;

import org.artifactory.common.ArtifactoryHome;
import org.jboss.spring.vfs.VFSResourceLoader;
import org.jboss.spring.vfs.VFSResourcePatternResolver;
import org.springframework.beans.BeansException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Special application context for use with the JBoss application server v5.x. This is needed since JBoss use a special
 * virtual file system.<br> See: http://issues.jfrog.org/jira/browse/RTFACT-1296
 *
 * @author Noam Tenne
 */
public class ArtifactoryJBoss5ApplicationContext extends ArtifactoryApplicationContext {

    public ArtifactoryJBoss5ApplicationContext(
            String name, SpringConfigPaths springConfigPaths, ArtifactoryHome artifactoryHome)
            throws BeansException {
        super(name, springConfigPaths, artifactoryHome);
    }

    @Override
    protected ResourcePatternResolver getResourcePatternResolver() {
        return new VFSResourcePatternResolver(new VFSResourceLoader(getClassLoader()));
    }

    @Override
    public Resource getResource(String location) {
        return getResourcePatternResolver().getResource(location);
    }
}
