/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.actionable.action;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yoavl
 */
public class DownloadAction extends RepoAwareItemAction {
    private static final Logger log = LoggerFactory.getLogger(DownloadAction.class);
    public static final String ACTION_NAME = "Download";

    public DownloadAction() {
        super(ACTION_NAME, null);
    }

    @Override
    public void onAction(RepoAwareItemEvent e) {
        RepoPath repoPath = e.getRepoPath();
        String downloadUrl = WebUtils.getWicketServletContextUrl()
                + "/" + repoPath.getRepoKey() + "/" + repoPath.getPath();
        if (log.isDebugEnabled()) {
            log.debug("Download URL is " + downloadUrl);
        }
        AjaxRequestTarget target = e.getTarget();
        target.prependJavascript("window.location='" + downloadUrl + "';");
    }
}