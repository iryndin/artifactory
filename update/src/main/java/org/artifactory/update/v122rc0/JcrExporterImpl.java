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
package org.artifactory.update.v122rc0;

import org.apache.commons.collections15.OrderedMap;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.descriptor.DescriptorAware;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.update.jcr.JcrFolder;
import org.artifactory.update.jcr.JcrPathUpdate;
import org.artifactory.update.jcr.JcrSessionProvider;
import org.artifactory.update.utils.UpdateUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.util.List;

/**
 * @author freds
 * @author Yossi Shaul
 * @date Aug 15, 2008
 */
public class JcrExporterImpl implements JcrExporter {
    private static final Logger log =
            Logger.getLogger(JcrExporterImpl.class);

    private DescriptorAware<CentralConfigDescriptor> centralProvider;
    private JcrSessionProvider jcr;
    private List<String> reposToExport;
    private boolean includeCaches = false;

    public JcrSessionProvider getJcr() {
        return jcr;
    }

    public void setJcr(JcrSessionProvider jcr) {
        this.jcr = jcr;
    }

    public DescriptorAware<CentralConfigDescriptor> getCentralProvider() {
        return centralProvider;
    }

    public void setCentralProvider(DescriptorAware<CentralConfigDescriptor> centralProvider) {
        this.centralProvider = centralProvider;
    }

    public void setRepositoriesToExport(List<String> reposToExport) {
        if (includeCaches && reposToExport != null) {
            throw new IllegalStateException("Include caches cannot be true if " +
                    "repos to export is not null");
        }
        this.reposToExport = reposToExport;
    }

    public void setIncludeCaches(boolean includeCaches) {
        if (includeCaches && reposToExport != null) {
            throw new IllegalStateException("Include caches cannot be true if " +
                    "repos to export is not null");
        }
        this.includeCaches = includeCaches;
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Starting export of JCR repo data", log);
        try {
            String repoJcrRootPath = JcrPathUpdate.getRepoJcrRootPath();
            Session session = jcr.getSession();
            Node repoRoot = (Node) session.getItem(repoJcrRootPath);
            NodeIterator nodes = repoRoot.getNodes();
            while (nodes.hasNext()) {
                Node repoNode = nodes.nextNode();
                String jcrRepoName = repoNode.getName();
                if (shouldExportRepo(jcrRepoName, status)) {
                    exportRepository(settings.getBaseDir(), repoNode, jcrRepoName, status);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        status.setStatus("End of export of JCR repo data", log);
    }

    private boolean shouldExportRepo(String jcrRepoName, StatusHolder status) {
        String newRepoKey = UpdateUtils.getNewRepoKey(jcrRepoName);
        String repoKeyForDescriptorMap = getRepoKeyForDescriptorMap(jcrRepoName, newRepoKey);
        boolean export = true;
        if (!isInRepositoriesList(repoKeyForDescriptorMap)) {
            String message = "Repository in JCR with name " + newRepoKey +
                    " does not exists in the config xml file. Will be ignored.";
            status.setStatus(message, log);
            export = false;
        }

        if (reposToExport != null) {
            // export only the listed repositories
            if (!reposToExport.contains(jcrRepoName)) {
                status.setStatus("Repository " + jcrRepoName + " not in list of repos to export. " +
                        "Skipping", log);
                export = false;
            }
        } else {
            // don't export caches if --caches not passed
            if (!includeCaches && isCacheRepository(newRepoKey)) {
                status.setStatus("Skipping cached repository " + newRepoKey, log);
                export = false;
            }
        }

        return export;
    }

    private static boolean isCacheRepository(String newRepoKey) {
        return newRepoKey.endsWith(UpdateUtils.CACHE_SUFFIX);
    }

    private boolean isInReposToExport(String jcrRepoName) {
        return reposToExport == null || reposToExport.contains(jcrRepoName);
    }

    private boolean isInRepositoriesList(String repoKeyForDescriptorMap) {
        // the migrated central config
        CentralConfigDescriptor descriptor = getCentralProvider().getDescriptor();

        OrderedMap<String, LocalRepoDescriptor> localRepositoriesMap =
                descriptor.getLocalRepositoriesMap();
        OrderedMap<String, RemoteRepoDescriptor> remoteRepositoriesMap =
                descriptor.getRemoteRepositoriesMap();
        return localRepositoriesMap.containsKey(repoKeyForDescriptorMap) ||
                remoteRepositoriesMap.containsKey(repoKeyForDescriptorMap);
    }

    /**
     * The remote repositories are saved in the the jcr with the suffix '-cache' but configured
     * without it in the config file. Same for the local repos but with the '-local' suffix. This
     * method will return the name as expected in the config file.
     */
    private static String getRepoKeyForDescriptorMap(String jcrRepoName, String newRepoKey) {
        if (newRepoKey.endsWith(UpdateUtils.CACHE_SUFFIX)) {
            return newRepoKey.substring(
                    0, newRepoKey.length() - UpdateUtils.CACHE_SUFFIX.length());
        } else if (!jcrRepoName.endsWith(UpdateUtils.LOCAL_SUFFIX) &&
                newRepoKey.endsWith(UpdateUtils.LOCAL_SUFFIX)) {
            // the -local string was added to the newRepoKey - remove it
            return newRepoKey.substring(
                    0, newRepoKey.length() - UpdateUtils.LOCAL_SUFFIX.length());
        } else {
            return newRepoKey;
        }
    }

    private static void exportRepository(File exportDir, Node repoNode, String repoKey,
            StatusHolder status) throws Exception {
        status.setStatus("Exporting repository " + repoKey, log);
        File repoExportDir = JcrPathUpdate.getRepoExportDir(exportDir, repoKey);
        JcrFolder jcrFolder = new JcrFolder(repoNode, repoKey);
        jcrFolder.exportTo(repoExportDir, status);
    }


    public void importFrom(ImportSettings settings, StatusHolder status) {
    }

}
