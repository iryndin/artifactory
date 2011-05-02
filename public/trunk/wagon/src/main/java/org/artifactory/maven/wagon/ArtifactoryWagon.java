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

package org.artifactory.maven.wagon;

import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.repository.Repository;

/**
 * A wagon that enables using "extensions" for commons http deployment without having to remove maven's own lightweight
 * http.<br/> In distribution mangement use:<br/> &lt;url&gt;repo://localhost:8080/artifactory/local-repo&lt;/url&gt;<br/>
 * And -
 * <pre>
 * &lt;build&gt;
 *     &lt;extensions&gt;
 *          &lt;extension&gt;
 *              &lt;groupId&gt;org.artifactory.maven.wagon&lt;/groupId&gt;
 *              &lt;artifactId&gt;artifactory-wagon&lt;/artifactId&gt;
 *              &lt;version&gt;1.0&lt;/version&gt;
 *          &lt;/extension&gt;
 *      &lt;/extensions&gt;
 * &lt;/build&gt;
 * </pre>
 */
public class ArtifactoryWagon extends HttpWagon {
    public static final String URL_SCHEME = "repo://";

    private Repository repository;

    @Override
    public Repository getRepository() {
        if (repository == null) {
            repository = super.getRepository();
            String origUrl = repository.getUrl();
            String newUrl = origUrl.replace(URL_SCHEME, "http://");
            repository.setUrl(newUrl);
        }
        return repository;
    }
}
