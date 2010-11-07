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

package org.artifactory.api.repo.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.repo.RepoPath;

/**
 * Converts a RepoPath interface to the impl.
 * <pre>
 * &lt;properties&gt;
 *     &lt;key1&gt;val1&lt;/key1&gt;
 *     &lt;key2&gt;val2&lt;/key2&gt;
 * &lt;/properties&gt;
 * </pre >
 *
 * @author Yoav Landman
 */
public class RepoPathConverter implements Converter {
    private static final String REPO_KEY = "repoKey";
    private static final String PATH = "path";

    public boolean canConvert(Class type) {
        return type.equals(RepoPath.class);
    }

    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        RepoPath repoPath = (RepoPath) source;
        //Key
        writer.startNode(REPO_KEY);
        writeValueOrNull(writer, repoPath.getRepoKey());
        writer.endNode();
        //Key
        writer.startNode(PATH);
        writeValueOrNull(writer, repoPath.getPath());
        writer.endNode();
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String repoKey = null;
        String path = null;
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if (REPO_KEY.equalsIgnoreCase(reader.getNodeName())) {
                repoKey = readValueOrNull(reader);
            } else if (PATH.equalsIgnoreCase(reader.getNodeName())) {
                path = readValueOrNull(reader);
            }
            reader.moveUp();
        }
        return new RepoPathImpl(repoKey, path);
    }

    private String readValueOrNull(HierarchicalStreamReader reader) {
        String val;
        if (reader.hasMoreChildren()) {
            //Handle nulls
            val = null;
        } else {
            val = reader.getValue();
        }
        return val;
    }

    private static void writeValueOrNull(HierarchicalStreamWriter writer, String val) {
        if (val != null) {
            writer.setValue(val);
        } else {
            writer.startNode("null");
            writer.endNode();
        }
    }
}