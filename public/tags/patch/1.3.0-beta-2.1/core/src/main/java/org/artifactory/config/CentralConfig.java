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
package org.artifactory.config;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.backup.BackupManager;
import org.artifactory.backup.config.Backup;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Proxy;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.index.IndexerManager;
import org.artifactory.repo.index.config.Indexer;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.repo.interceptor.UniqueSnapshotsCleanerJcrInterceptor;
import org.artifactory.repo.jaxb.LocalRepositoriesMapAdapter;
import org.artifactory.repo.jaxb.RemoteRepositoriesMapAdapter;
import org.artifactory.repo.jaxb.VirtualRepositoriesMapAdapter;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.security.config.Security;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "anonAccessEnabled", "security", "fileUploadMaxSizeMb",
                "dateFormat", "backup", "indexer", "localRepositoriesMap", "remoteRepositoriesMap",
                "virtualRepositoriesMap", "proxies"})
public class CentralConfig implements ExportableConfig {

    private final static Logger LOGGER = Logger.getLogger(CentralConfig.class);

    public static final String NS = "http://artifactory.jfrog.org/xsd/1.3.0";

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ssZ";

    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    @XmlElement(name = "localRepositories", required = true)
    @XmlJavaTypeAdapter(LocalRepositoriesMapAdapter.class)
    private OrderedMap<String, LocalRepo> localRepositoriesMap =
            new ListOrderedMap<String, LocalRepo>();

    @XmlElement(name = "remoteRepositories", required = false)
    @XmlJavaTypeAdapter(RemoteRepositoriesMapAdapter.class)
    private OrderedMap<String, RemoteRepo> remoteRepositoriesMap =
            new ListOrderedMap<String, RemoteRepo>();

    @XmlElement(name = "virtualRepositories", required = false)
    @XmlJavaTypeAdapter(VirtualRepositoriesMapAdapter.class)
    private Map<String, VirtualRepo> virtualRepositoriesMap = new HashMap<String, VirtualRepo>();

    @XmlElementWrapper(name = "proxies")
    @XmlElement(name = "proxy", required = false)
    private List<Proxy> proxies = new ArrayList<Proxy>();
    private String dateFormat = DEFAULT_DATE_FORMAT;

    private DateFormat dateFormatter;

    private int fileUploadMaxSizeMb = 100;

    private boolean anonAccessEnabled = true;

    private Backup backup;

    private Indexer indexer;

    /**
     * A name uniquely identifying this artifactory server instance
     */
    private String serverName;

    private Security security;

    @XmlTransient
    private String configFilePath;

    @XmlTransient
    private VirtualRepo globalVirtualRepo;

    @XmlTransient
    private ArtifactoryApplicationContext artifactoryContext;

    public static CentralConfig get() {
        ArtifactoryContext context = ContextHelper.get();
        return context.getCentralConfig();
    }

    public void init(ArtifactoryApplicationContext artifactoryContext) {
        this.artifactoryContext = artifactoryContext;
        checkUniqueProxies();
        //Check the repositories directories create by ArtifactoryHome
        checkWritableDirectory(ArtifactoryHome.getDataDir());
        checkWritableDirectory(ArtifactoryHome.getJcrRootDir());
        //Create the deployment dir
        checkWritableDirectory(ArtifactoryHome.getTmpDataDir());
        //Create the date formatter
        dateFormatter = new SimpleDateFormat(dateFormat);
        dateFormatter.setTimeZone(UTC_TIME_ZONE);
        //Get the server name
        if (serverName == null) {
            LOGGER.warn("Could not determine server instance id from configuration." +
                    " Using hostname instead.");
            try {
                serverName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException("Failed to use hostname as the server instacne id.", e);
            }
        }
        final JcrWrapper jcr = artifactoryContext.getJcr();
        if (jcr == null) {
            throw new RuntimeException(
                    "Configuration of Spring injection error. JcrWrapper bean cannot be null");
        }
        jcr.doInSession(new JcrCallback<Object>() {
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
                //Double init issue managed inside init
                jcr.init();
                //Init the repositories
                globalVirtualRepo = new VirtualRepo(VirtualRepo.GLOBAL_VIRTUAL_REPO_KEY,
                        localRepositoriesMap, remoteRepositoriesMap);
                List<RealRepo> repos = getGlobalVirtualRepo().getLocalAndRemoteRepositories();
                //Init real repos
                for (RealRepo repo : repos) {
                    repo.init();
                }
                //Init global repos
                Collection<VirtualRepo> virtualRepos = virtualRepositoriesMap.values();
                for (VirtualRepo repo : virtualRepos) {
                    repo.init();
                }
                return null;
            }
        });
    }

    public VirtualRepo getGlobalVirtualRepo() {
        return globalVirtualRepo;
    }

    public VirtualRepo virtualRepositoryByKey(String key) {
        VirtualRepo repo = virtualRepositoriesMap.get(key);
        if (repo == null && VirtualRepo.GLOBAL_VIRTUAL_REPO_KEY.equals(key)) {
            repo = globalVirtualRepo;
        }
        return repo;
    }

    public List<VirtualRepo> getVirtualRepositories() {
        ArrayList<VirtualRepo> list = new ArrayList<VirtualRepo>();
        list.add(globalVirtualRepo);
        list.addAll(virtualRepositoriesMap.values());
        return list;
    }

    private static void checkWritableDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            throw new IllegalArgumentException(
                    "Failed to create writable directory: " +
                            dir.getAbsolutePath());
        }
    }

    private void checkUniqueProxies() {
        Map<String, Proxy> map = new HashMap<String, Proxy>(proxies.size());
        for (Proxy proxy : proxies) {
            String key = proxy.getKey();
            Proxy oldProxy = map.put(key, proxy);
            if (oldProxy != null) {
                throw new RuntimeException(
                        "Duplicate proxy key in configuration: " + key + ".");
            }
        }
    }

    public synchronized void putLocalRepo(LocalRepo repo) {
        localRepositoriesMap.put(repo.getKey(), repo);
    }

    public synchronized void putRemoteRepo(RemoteRepo repo) {
        remoteRepositoriesMap.put(repo.getKey(), repo);
    }

    public synchronized void putVirtualRepo(VirtualRepo repo) {
        virtualRepositoriesMap.put(repo.getKey(), repo);
    }

    @XmlElement(defaultValue = DEFAULT_DATE_FORMAT)
    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @XmlElement(defaultValue = "100", required = false)
    public int getFileUploadMaxSizeMb() {
        return fileUploadMaxSizeMb;
    }

    public void setFileUploadMaxSizeMb(int fileUploadMaxSizeMb) {
        this.fileUploadMaxSizeMb = fileUploadMaxSizeMb;
    }

    @XmlElement(defaultValue = "true")
    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public void setAnonAccessEnabled(boolean anonAccessEnabled) {
        this.anonAccessEnabled = anonAccessEnabled;
    }

    public Backup getBackup() {
        return backup;
    }

    public void setBackup(Backup backup) {
        this.backup = backup;
    }

    public Indexer getIndexer() {
        return indexer;
    }

    public void setIndexer(Indexer indexer) {
        this.indexer = indexer;
    }

    @XmlElement
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public List<Proxy> getProxies() {
        return proxies;
    }

    @XmlElement
    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public DateFormat getDateFormatter() {
        return dateFormatter;
    }

    public void saveTo(String path) {
        new JaxbHelper<CentralConfig>().write(path, this);
    }

    public void reload() {
        loadFrom(configFilePath);
    }

    public void loadFrom(String path) {
        CentralConfig cc =
                new JaxbHelper<CentralConfig>().read(path, CentralConfig.class);
        cc.init(artifactoryContext);
        localRepositoriesMap = cc.localRepositoriesMap;
        remoteRepositoriesMap = cc.remoteRepositoriesMap;
        virtualRepositoriesMap = cc.virtualRepositoriesMap;
        proxies = cc.proxies;
        dateFormat = cc.dateFormat;
        dateFormatter = cc.dateFormatter;
        fileUploadMaxSizeMb = cc.fileUploadMaxSizeMb;
        anonAccessEnabled = cc.anonAccessEnabled;
        backup = cc.backup;
        indexer = cc.indexer;
        ArtifactoryContext context = ContextHelper.get();
        BackupManager backupManager = context.beanForType(BackupManager.class);
        backupManager.init();
        IndexerManager indexerManager = context.beanForType(IndexerManager.class);
        indexerManager.init();
        serverName = cc.serverName;
        globalVirtualRepo = cc.globalVirtualRepo;
        security = cc.security;
    }

    public void importFrom(String basePath, StatusHolder status) {
        //Delete all repositories first
        ArtifactoryContext context = ContextHelper.get();
        final JcrWrapper jcr = context.getJcr();
        jcr.doInSession(new JcrCallback<Object>() {
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
                List<LocalRepo> repos = getGlobalVirtualRepo().getLocalAndCachedRepositories();
                for (LocalRepo repo : repos) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Removing repository node '" + repo.getKey() + "'.");
                    }
                    repo.delete();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Repository node '" + repo.getKey() + "' deleted.");
                    }
                    //Saves the root node in order not to get lock warnings
                    session.save();
                }
                return null;
            }
        });
        String path = basePath + "/" + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE;
        loadFrom(path);
        String repoRootPath = basePath + '/' + JcrPath.get().getRepoJcrRootPath();
        importLocalRepositories(repoRootPath, status);
        //Backup the config file and overwrite it
        try {
            FileUtils.copyFile(new File(configFilePath), new File(configFilePath + ".orig"));
            FileUtils.copyFile(new File(path), new File(configFilePath));
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to backup the " + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE, e);
        }
    }

    public void exportTo(File exportDir, List<LocalRepo> repos, StatusHolder status) {
        String path = exportDir + "/" + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE;
        File destFile = new File(path);
        File srcFile = new File(configFilePath);
        try {
            FileUtils.copyFile(srcFile, destFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy " + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE,
                    e);
        }
        //Export the local repositories
        for (LocalRepo localRepo : repos) {
            localRepo.exportTo(exportDir, status);
        }
    }

    public void exportTo(File exportDir, StatusHolder status) {
        List<LocalRepo> repos = globalVirtualRepo.getLocalAndCachedRepositories();
        exportTo(exportDir, repos, status);
    }

    public void importLocalRepositories(String repoRootPath, StatusHolder status) {
        //Import the local repositories
        List<LocalRepo> repoList = globalVirtualRepo.getLocalAndCachedRepositories();
        for (LocalRepo localRepo : repoList) {
            localRepo.importFrom(repoRootPath, status);
        }
    }

    public String getConfigFilePath() {
        return configFilePath;
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    public LocalRepoInterceptor getLocalRepoInterceptor() {
        return new UniqueSnapshotsCleanerJcrInterceptor();
    }

    void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }
}
