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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.stats;

import com.ocpsoft.pretty.time.PrettyTime;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * Displays file statistics information.
 *
 * @author Yossi Shaul
 * @author Yoav Landman
 */
public class StatsTabPanel extends Panel {

    public StatsTabPanel(String id, RepoAwareActionableItem item) {
        super(id);
        LabeledValue downloadedLabel = new LabeledValue("downloaded", "Downloaded: ");
        add(downloadedLabel);
        LabeledValue lastDownloaded = new LabeledValue("lastDownloaded", "Last Downloaded: ");
        add(lastDownloaded);
        LabeledValue lastDownloadedBy = new LabeledValue("lastDownloadedBy", "Last Downloaded by: ");
        add(lastDownloadedBy);
        StatsInfo statsInfo = item.getXmlMetadata(StatsInfo.class);
        downloadedLabel.setValue(Long.toString(statsInfo.getDownloadCount()));
        long lastDownloadedVal = statsInfo.getLastDownloaded();
        if (lastDownloadedVal == 0) {
            //No download data available (either never downloaded or coming from legacy repo)
            lastDownloaded.setVisible(false);
            lastDownloadedBy.setVisible(false);
        } else {
            PrettyTime prettyTime = new PrettyTime();
            Date lastDownloadedDate = new Date(lastDownloadedVal);
            String lastDownloadedString = prettyTime.format(lastDownloadedDate) + " (" + lastDownloadedDate.toString() +
                    ")";
            lastDownloaded.setValue(lastDownloadedString);
            String lastDownloadedByVal = statsInfo.getLastDownloadedBy();
            lastDownloadedBy.setValue(StringUtils.hasLength(lastDownloadedByVal) ? lastDownloadedByVal : "");
        }
    }
}