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

import javax.annotation.Nonnull;
import java.util.Calendar;

/**
 * Date: 8/4/11
 * Time: 1:12 PM
 *
 * @author Fred Simon
 */
public interface MutableVfsNode extends VfsNode {

    Iterable<MutableVfsNode> mutableChildren();

    @Nonnull
    MutableVfsNode getOrCreateSubNode(String relPath, VfsNodeType subNodeType);

    @Nonnull
    MutableVfsProperty setProperty(String key, String... values);

    @Nonnull
    MutableVfsProperty setProperty(String key, Long value);

    @Nonnull
    MutableVfsProperty setProperty(String key, Calendar value);

    void setContent(BinaryContent content);

    void moveTo(VfsNode newParentNode);

    void delete();
}
