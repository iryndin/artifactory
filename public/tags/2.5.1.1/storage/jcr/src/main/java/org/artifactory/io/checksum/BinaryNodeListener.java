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

package org.artifactory.io.checksum;

import javax.jcr.Node;

/**
 * A listener that receives events when a binary node is visited
 *
 * @author Yoav Landman
 */
public interface BinaryNodeListener {

    /**
     * A node with binary property (jcr:data). The parent is not necessarily an artifactory:file (for example build info
     * node)
     *
     * @param node           JCR node with binary property (jcr:data)
     * @param fixConsistency True if the listener is called as a result of fix consistency
     */
    void nodeVisited(Node node, boolean fixConsistency);
}
