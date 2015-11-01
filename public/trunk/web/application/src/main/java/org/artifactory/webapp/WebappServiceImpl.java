/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

package org.artifactory.webapp;

import org.artifactory.api.web.WebappService;
import org.artifactory.util.HttpUtils;
import org.springframework.stereotype.Service;

/**
 * Implementation of services given by the webapp to lower level components.
 *
 * @author Yossi Shaul
 */
@Service
public class WebappServiceImpl implements WebappService {

    public static final String PATH_ID_PARAM = "pathId";
    private static final String WEBAPP_URL_BROWSE_REPO = "/browserepo.html";

    @Override
    public String createLinkToBrowsableArtifact(String artifactoryUrl, String repoPathId, String linkLabel) {
        String encodedPathId = HttpUtils.encodeQuery(repoPathId);
        String url = new StringBuilder().append(artifactoryUrl).append(HttpUtils.WEBAPP_URL_PATH_PREFIX)
                .append(WEBAPP_URL_BROWSE_REPO).append("?").append(PATH_ID_PARAM).append("=").append(encodedPathId)
                .toString();

        StringBuilder builder = new StringBuilder();
        builder.append("<a href=").append(url).append(" target=\"blank\"").append(">")
                .append(linkLabel).append("</a>");
        return builder.toString();
    }
}
