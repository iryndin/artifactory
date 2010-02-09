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

package org.artifactory.api.search.artifact;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.search.SearchResultBase;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author Yoav Landman
 */
public class ArtifactSearchResult extends SearchResultBase {
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    protected final MavenArtifactInfo artifact;

    public ArtifactSearchResult(ItemInfo itemInfo, MavenArtifactInfo artifact) {
        super(itemInfo);
        this.artifact = artifact;
    }

    public MavenArtifactInfo getArtifact() {
        return artifact;
    }

    public String getLastModifiedDay() {
        return DAY_FORMAT.format(getLastModified());
    }

    public String getLastModifiedString() {
        long lastModified = getItemInfo().getLastModified();
        return ContextHelper.get().getCentralConfig().format(lastModified);
    }

    public long getLastModified() {
        return getItemInfo().getLastModified();
    }
}