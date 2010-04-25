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

package org.artifactory.api.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.fs.FileInfo;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Holds and manages list of search files. Instances of this class doesn't allow files with the same relative path.
 *
 * @author Yossi Shaul
 */
public class SavedSearchResults implements Serializable {
    private final String name;
    private final List<FileInfo> results;

    public SavedSearchResults(String name, List<FileInfo> results) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Empty results name is not allowed");
        }
        this.name = name;
        this.results = Lists.newArrayList();
        if (results != null) {
            // make a protective copy and remove duplicates if exist
            addAll(results);
        }
    }

    public String getName() {
        return name;
    }

    public ImmutableList<FileInfo> getResults() {
        return ImmutableList.copyOf(results);
    }

    public void add(FileInfo fileInfo) {
        remove(fileInfo);
        results.add(fileInfo);
    }

    public void addAll(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            add(fileInfo);
        }
    }

    public void merge(SavedSearchResults toMerge) {
        addAll(toMerge.getResults());
    }

    public void subtract(SavedSearchResults toSubtract) {
        for (FileInfo fileInfo : toSubtract.getResults()) {
            remove(fileInfo);
        }
    }

    public void removeAll(Collection<FileInfo> fileInfos) {
        results.removeAll(fileInfos);
    }

    public boolean contains(FileInfo fileInfo) {
        return results.contains(fileInfo);
    }

    public int size() {
        return results.size();
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    private void remove(FileInfo fileInfo) {
        // files equality is driven by the relative path only. We use the relative path because the artifact may be
        // from different repos and we only display one of them
        Iterator<FileInfo> resultsIterator = results.iterator();
        while (resultsIterator.hasNext()) {
            FileInfo result = resultsIterator.next();
            if (fileInfo.getRelPath().equals(result.getRelPath())) {
                resultsIterator.remove();
                return;
            }
        }
    }
}
