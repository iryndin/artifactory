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

package org.artifactory.jcr.md;

import org.artifactory.sapi.fs.VfsMetadataAware;

import javax.jcr.Node;

/**
 * This interface represent an entity that can receive metadata. For now only JcrFsItem can have Metadata attached. This
 * interface is used by the XmlProvider and PersistenceHandler to manage the metadata object.
 * <p/>
 * User: freds Date: Aug 10, 2008 Time: 3:39:02 PM
 */
public interface MetadataAware extends VfsMetadataAware {

    /**
     * @return the actual JCR node of the entity on which Metadata is added
     */
    Node getNode();

}
