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

package org.artifactory.mime.version.converter.v4;

import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;

/**
 * Adds the following entries to the mimetypes file if they don't exist:
 * <pre>
 *    <mimetype type="application/x-rubygems" extensions="gem" css="gem"/>
 *    <mimetype type="application/x-ruby-marshal" extensions="rz" css="ruby-marshal"/>
 * </pre>
 *
 * @author Yossi Shaul
 */
public class GemMimeTypeConverter implements XmlConverter {
    @Override
    public void convert(Document doc) {
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        List mimetypes = rootElement.getChildren("mimetype", namespace);
        if (mimetypes == null) {
            return;
        }

        Element gem = getByType(mimetypes, namespace, "application/x-rubygems");
        if (gem == null) {
            gem = new Element("mimetype", namespace);
            gem.setAttribute("type", "application/x-rubygems");
            gem.setAttribute("extensions", "gem");
            gem.setAttribute("css", "gem");
            rootElement.addContent(gem);
        }

        Element nuspecType = getByType(mimetypes, namespace, "application/x-ruby-marshal");
        if (nuspecType == null) {
            nuspecType = new Element("mimetype", namespace);
            nuspecType.setAttribute("type", "application/x-ruby-marshal");
            nuspecType.setAttribute("extensions", "rz");
            rootElement.addContent(nuspecType);
        }
    }

    private Element getByType(List mimetypes, Namespace namespace, String type) {
        for (Object mimetype : mimetypes) {
            Element mimeTypeElement = (Element) mimetype;
            String typeValue = mimeTypeElement.getAttributeValue("type", namespace);
            if (type.equals(typeValue)) {
                return mimeTypeElement;
            }
        }
        return null;
    }
}
