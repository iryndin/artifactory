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

package org.artifactory.repo.index.creator;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.log.LoggerFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.slf4j.Logger;
import org.sonatype.nexus.index.creator.MinimalArtifactInfoIndexCreator;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrModelReader extends MinimalArtifactInfoIndexCreator.ModelReader {
    private static final Logger log = LoggerFactory.getLogger(JcrModelReader.class);

    @Override
    public Model readModel(File pom, String groupId, String artifactId, String version) {
        Xpp3Dom dom = readPom((JcrFile) pom);
        if (dom == null) {
            return null;
        }
        String packaging = null;
        if (dom.getChild("packaging") != null) {
            packaging = dom.getChild("packaging").getValue();
        }
        Model model = new Model();
        model.setPackaging(packaging);
        if (dom.getChild("name") != null) {
            model.setName(dom.getChild("name").getValue());
        }
        if (dom.getChild("description") != null) {
            model.setDescription(dom.getChild("description").getValue());
        }
        return model;
    }

    private static Xpp3Dom readPom(JcrFile pom) {
        Reader r = null;
        try {
            r = new InputStreamReader(pom.getStream(), "utf-8");
            return Xpp3DomBuilder.build(r);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not read artifact model.", e);
            } else {
                String msg =
                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                log.warn("Could not read artifact model.: " + msg);
            }
        }
        finally {
            IOUtils.closeQuietly(r);
        }
        return null;
    }
}