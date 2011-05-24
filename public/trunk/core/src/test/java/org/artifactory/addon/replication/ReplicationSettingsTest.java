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

package org.artifactory.addon.replication;

import org.artifactory.addon.ReplicationAddon;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.repo.RepoPath;
import org.testng.annotations.Test;

import java.io.StringWriter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
@Test
public class ReplicationSettingsTest {

    public void testConstructor() {
        RepoPath repoPath = new RepoPathImpl("momo", "pop");
        StringWriter responseWriter = new StringWriter();
        ReplicationSettings replicationSettings = new ReplicationSettings(repoPath, true, 15, true, true,
                ReplicationAddon.Overwrite.never, responseWriter);

        assertEquals(replicationSettings.getRepoPath(), repoPath, "Unexpected repo path.");
        assertTrue(replicationSettings.isProgress(), "Unexpected progress display state.");
        assertEquals(replicationSettings.getMark(), 15, "Unexpected mark.");
        assertTrue(replicationSettings.isDeleteExisting(), "Unexpected delete existing state.");
        assertTrue(replicationSettings.isIncludeProperties(), "Unexpected property inclusion state.");
        assertEquals(replicationSettings.getOverwrite(), ReplicationAddon.Overwrite.never,
                "Unexpected overwrite switch state.");
        assertEquals(replicationSettings.getResponseWriter(), responseWriter, "Unexpected response writer.");
    }
}
