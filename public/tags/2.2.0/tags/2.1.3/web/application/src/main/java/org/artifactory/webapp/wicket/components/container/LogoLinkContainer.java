/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.components.container;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.wicket.util.WicketUtils;

import java.io.File;

/**
 * @author Tomer Cohen
 */
public class LogoLinkContainer extends Panel {

    @SpringBean
    private CentralConfigService centralConfigService;

    public LogoLinkContainer(String id) {
        super(id);
        String logoPath = centralConfigService.getMutableDescriptor().getLogo();
        if (StringUtils.isBlank(logoPath)) {
            if (new File(ContextHelper.get().getArtifactoryHome().getLogoDir(), "logo").exists()) {
                logoPath = WicketUtils.getWicketAppPath() + "logo";
            }
        }
        BookmarkablePageLink homeLink = new BookmarkablePageLink("homeLink", getApplication().getHomePage());
        add(homeLink);
        LogoDisplayContainer logoDisplayContainer =
                new LogoDisplayContainer("logoLink", getApplication().getHomePage(), logoPath);
        add(logoDisplayContainer);
        if (StringUtils.isBlank(logoPath)) {
            logoDisplayContainer.setVisible(false);
        } else {
            homeLink.setVisible(false);
        }
    }
}

