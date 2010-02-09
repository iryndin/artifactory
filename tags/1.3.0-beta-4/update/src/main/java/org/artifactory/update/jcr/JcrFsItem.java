/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.update.jcr;

import com.thoughtworks.xstream.XStream;
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.update.utils.UpdateUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * @author Yoav Landman
 * @author Yossi Shaul
 */
public abstract class JcrFsItem {

    //Properties shared by folders and files
    public static final String PROP_ARTIFACTORY_REPO_KEY = "artifactory:repoKey";
    public static final String PROP_ARTIFACTORY_MODIFIED_BY = "artifactory:modifiedBy";

    private static XStream xstream = createXStream();

    private Node node;
    protected String repoKey;
    private String newRepoKey;

    /**
     * @param node    The jcr node
     * @param repoKey The repoKey from the root jcr node (might be different from the new repository
     *                key)
     */
    protected JcrFsItem(Node node, String repoKey) {
        this.node = node;
        this.repoKey = repoKey;
        this.newRepoKey = UpdateUtils.getNewRepoKey(repoKey);
    }

    public Node getNode() {
        return node;
    }

    public String getRelativePath() throws RepositoryException {
        return JcrPathUpdate.getNodeRelativePath(node, repoKey);
    }

    public long getCreated() throws RepositoryException {
        //This property is auto-populated on node creation
        return getPropValue(JCR_CREATED).getDate().getTimeInMillis();
    }

    public String getRepoKey() throws RepositoryException {
        return getPropValue(PROP_ARTIFACTORY_REPO_KEY).getString();
    }

    public String getModifiedBy() throws RepositoryException {
        return getPropValue(PROP_ARTIFACTORY_MODIFIED_BY).getString();
    }

    protected Value getPropValue(String prop) throws RepositoryException {
        return node.getProperty(prop).getValue();
    }

    protected boolean hasProp(String prop) throws RepositoryException {
        return node.hasProperty(prop);
    }

    protected static XStream getXStream() {
        return xstream;
    }

    protected void fillWithGeneralMetadata(ItemInfo itemInfo) throws RepositoryException {
        // Get metadata in the safest way possible
        if (node.hasProperty(PROP_ARTIFACTORY_MODIFIED_BY)) {
            itemInfo.setModifiedBy(getModifiedBy());
        } else {
            itemInfo.setModifiedBy("export");
        }
        if (node.hasProperty(PROP_ARTIFACTORY_REPO_KEY)) {
            itemInfo.setRepoKey(newRepoKey);
        }
        itemInfo.setCreated(System.currentTimeMillis());
        itemInfo.setRelPath(getRelativePath());
    }

    private static XStream createXStream() {
        XStream xstream = new XStream();
        xstream.processAnnotations(new Class[]{FolderInfo.class, FileInfo.class, StatsInfo.class});
        return xstream;
    }
}
