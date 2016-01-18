/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.repo.interceptor;

import com.google.common.collect.Lists;
import org.artifactory.api.properties.PropertiesService;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.property.Property;
import org.artifactory.md.Properties;
import org.artifactory.repo.interceptor.storage.StorageInterceptorAdapter;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.sapi.fs.VfsItem;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An interceptor that copies items into the global trashcan
 *
 * @author Shay Yaakov
 */
public class TrashInterceptor extends StorageInterceptorAdapter {

    @Autowired
    private TrashService trashService;

    @Autowired
    private PropertiesService propertiesService;

    @Override
    public void beforeDelete(VfsItem fsItem, MutableStatusHolder statusHolder, boolean moved) {
        if (!moved) {
            trashService.copyToTrash(fsItem.getRepoPath());
        }
    }

    @Override
    public void afterMove(VfsItem sourceItem, VfsItem targetItem, MutableStatusHolder statusHolder, Properties properties) {
        if (!TrashService.TRASH_KEY.equals(sourceItem.getRepoKey())) {
            return;
        }
        Map<Property, List<String>> propertiesMap = new HashMap<>();
        propertiesMap.put(new Property(TrashService.PROP_RESTORED_TIME), Lists.newArrayList(String.valueOf(System.currentTimeMillis())));
        propertiesService.addPropertyRecursivelyMultiple(targetItem.getRepoPath(), null, propertiesMap, false);
    }
}
