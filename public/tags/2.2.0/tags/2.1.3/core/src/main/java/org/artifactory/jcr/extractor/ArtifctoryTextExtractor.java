/*
 * This file is part of Artifactory.
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

package org.artifactory.jcr.extractor;

import org.apache.jackrabbit.extractor.CompositeTextExtractor;
import org.apache.jackrabbit.extractor.PlainTextExtractor;
import org.apache.jackrabbit.extractor.XMLTextExtractor;
import org.artifactory.api.mime.ContentType;

/**
 * Composite text extractor that limit text extraction on xmls (no parsing and extraction for application/xml - it is
 * imported and stored as an eml document anyway)
 */
public class ArtifctoryTextExtractor extends CompositeTextExtractor {
    public static final String[] XML_MIME_TYPES = new String[]{ContentType.mavenPom.getMimeEntry().getMimeType()};

    /**
     * Add instances of the standard text extractors as components.
     */
    public ArtifctoryTextExtractor() {
        addTextExtractor(new PlainTextExtractor());
        addTextExtractor(new XMLTextExtractor() {
            @Override
            public String[] getContentTypes() {
                return XML_MIME_TYPES;
            }
        });
    }

}