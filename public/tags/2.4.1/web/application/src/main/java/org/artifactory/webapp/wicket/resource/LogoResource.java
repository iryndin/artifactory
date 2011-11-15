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

package org.artifactory.webapp.wicket.resource;

import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.artifactory.common.ArtifactoryHome;

import java.io.File;

/**
 * Resource to get the uploaded user logo
 *
 * @author Tomer Cohen
 */
public class LogoResource extends WebResource {

    private ArtifactoryHome artifactoryHome;

    public LogoResource(ArtifactoryHome artifactoryHome) {
        this.artifactoryHome = artifactoryHome;
    }

    @Override
    public IResourceStream getResourceStream() {
        File logoFile = new File(artifactoryHome.getLogoDir(), "logo");
        return new FileResourceStream(logoFile);
    }
}
