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

package org.artifactory.log.logback;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.common.ArtifactoryHome;

import java.io.Serializable;

/**
 * @author Yoav Landman
 */
public class LoggerConfigInfo implements Serializable {

    private String contextId;
    private ArtifactoryHome home;

    public LoggerConfigInfo(String contextId) {
        this(contextId, null);
    }

    public LoggerConfigInfo(String contextId, ArtifactoryHome home) {
        this.contextId = contextId;
        this.home = home;
    }

    LoggerConfigInfo(ArtifactoryContext context) {
        this(context.getContextId(), context.getArtifactoryHome());
    }

    String getContextId() {
        return contextId;
    }

    ArtifactoryHome getHome() {
        return home;
    }
}
