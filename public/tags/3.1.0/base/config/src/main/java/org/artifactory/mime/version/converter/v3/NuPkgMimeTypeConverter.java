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

package org.artifactory.mime.version.converter.v3;

import org.artifactory.version.converter.XmlConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.List;

/**
 * Adds the following entries to the mimetypes file if they don't exist:
 * <pre>
 *   <mimetype type="application/x-nupkg" extensions="nupkg" archive="true" index="true" css="nupkg"/>
 *   <mimetype type="application/x-nuspec+xml" extensions="nuspec" viewable="true" syntax="xml" css="xml"/>
 * </pre>
 *
 * @author Yossi Shaul
 */
public class NuPkgMimeTypeConverter implements XmlConverter {
    @Override
    public void convert(Document doc) {
        Element rootElement = doc.getRootElement();
        Namespace namespace = rootElement.getNamespace();

        List mimetypes = rootElement.getChildren("mimetype", namespace);
        if (mimetypes == null) {
            return;
        }

        Element nupkgType = getByType(mimetypes, namespace, "application/x-nupkg");
        if (nupkgType == null) {
            nupkgType = new Element("mimetype", namespace);
            nupkgType.setAttribute("type", "application/x-nupkg");
            nupkgType.setAttribute("extensions", "nupkg");
            nupkgType.setAttribute("archive", "true");
            nupkgType.setAttribute("index", "true");
            nupkgType.setAttribute("css", "nupkg");
            rootElement.addContent(nupkgType);
        }

        Element nuspecType = getByType(mimetypes, namespace, "application/x-nuspec+xml");
        if (nuspecType == null) {
            nuspecType = new Element("mimetype", namespace);
            nuspecType.setAttribute("type", "application/x-nuspec+xml");
            nuspecType.setAttribute("extensions", "nuspec");
            nuspecType.setAttribute("viewable", "true");
            nuspecType.setAttribute("syntax", "xml");
            nuspecType.setAttribute("css", "xml");
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
