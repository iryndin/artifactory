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

package org.artifactory.sapi.data;

import javax.annotation.Nullable;
import java.util.Calendar;

/**
 * Date: 8/3/11
 * Time: 11:22 PM
 *
 * @author Fred Simon
 */
public interface VfsNode {

    String getName();

    String absolutePath();

    String storageId();

    VfsNodeType nodeType();

    boolean hasChild(String relPath);

    @Nullable
    VfsNode findSubNode(String relPath);

    boolean hasChildren();

    Iterable<VfsNode> children();

    Iterable<VfsNode> children(VfsNodeFilter filter);

    boolean hasProperty(String key);

    Iterable<String> propertyKeys();

    Iterable<VfsProperty> properties();

    VfsProperty getProperty(String key);

    boolean hasContent();

    BinaryContent content();

    String getStringProperty(String key);

    Long getLongProperty(String key);

    Calendar getDateProperty(String key);
}
