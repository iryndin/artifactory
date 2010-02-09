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

package org.artifactory.jcr.md;

import com.thoughtworks.xstream.XStream;
import org.artifactory.api.xstream.XStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author freds
 */
public abstract class XStreamMetadataProvider<T> implements XmlMetadataProvider<T> {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(XStreamMetadataProvider.class);

    private final Class<? extends T> xstreamClass;
    private final XStream xstream;
    private final String metadataName;

    public XStreamMetadataProvider(Class<? extends T> xstreamClass) {
        this.xstreamClass = xstreamClass;
        this.xstream = XStreamFactory.create(xstreamClass);
        this.metadataName = xstream.getMapper().serializedClass(xstreamClass);
        if (metadataName == null) {
            throw new IllegalArgumentException(
                    "Class '" + xstreamClass.getName() + "' is not an XStream mapped class.");
        }
    }

    public XStream getXstream() {
        return xstream;
    }

    public String getMetadataName() {
        return metadataName;
    }
}
