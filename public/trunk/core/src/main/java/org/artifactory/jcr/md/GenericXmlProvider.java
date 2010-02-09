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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author freds
 */
public class GenericXmlProvider implements XmlMetadataProvider<String> {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(GenericXmlProvider.class);

    private final String metadataName;

    public GenericXmlProvider(String metadataName) {
        this.metadataName = metadataName;
    }

    public String getMetadataName() {
        return metadataName;
    }

    public String fromXml(String xmlData) {
        return xmlData;
    }

    public String toXml(String metadata) {
        return metadata;
    }
}
