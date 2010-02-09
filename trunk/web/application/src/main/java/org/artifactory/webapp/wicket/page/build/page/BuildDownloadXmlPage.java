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

package org.artifactory.webapp.wicket.page.build.page;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;

/**
 * @author Eli Givoni
 */
//@MountPath(path = BUILDS)
//@MountMixedParam(parameterNames = {BUILD_NAME, BUILD_NUMBER, BUILD_DOWNLOAD_XML})

public class BuildDownloadXmlPage extends WebPage {
    private PageParameters pageParameters;

    public BuildDownloadXmlPage(PageParameters pageParameters) {
        this.pageParameters = pageParameters;
        //    String path = buildPath();
        //    getResponse().redirect(path);
    }


}
