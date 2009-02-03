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
import org.artifactory.backup.config.Backup;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Proxy;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.repo.interceptor.UniqueSnapshotsCleanerJcrInterceptor;
import org.artifactory.repo.jaxb.LocalRepositoriesMapAdapter;
import org.artifactory.repo.jaxb.RemoteRepositoriesMapAdapter;
import org.artifactory.repo.jaxb.VirtualRepositoriesMapAdapter;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "anonDownloadsAllowed", "fileUploadMaxSizeMb", "dateFormat",
                "backup", "localRepositoriesMap", "remoteRepositoriesMap",
                "virtualRepositoriesMap", "proxies"})
public class CentralConfig implements InitializingBean, DisposableBean, ExportableConfig {

    private final static Logger LOGGER = Logger.getLogger(CentralConfig.class);

    public static final String NS = "http://artifactory.jfrog.org/xsd/1.1.0";

    public static final String FILE_NAME = "artifactory.config.xml";

    public static final String DATA_DIR = (ArtifactoryHome.path() + "/data").replace('\\', '/');

    public static final String LOCAL_REPOS_DEPLOYMENT_DIR = DATA_DIR + "/tmp";

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
    private Map<String, VirtualRepo> virtualRepositoriesMap =
            new ListOrderedMap<String, VirtualRepo>();

    @XmlElementWrapper(name = "proxies")
    @XmlElement(name = "proxy", required = false)
    private List<Proxy> proxies = new ArrayList<Proxy>();

    private String dateFormat = DEFAULT_DATE_FORMAT;
    private DateFormat dateFormatter;

    private int fileUploadMaxSizeMb = 100;

    private boolean anonDownloadsAllowed = true;

    private Backup backup;

    /**
     * A name uniquely identifying this artifactory server instance
     */
    private String serverName;

    @XmlTransient
    private JcrHelper jcr;

    @XmlTransient
    private String configFilePath;

    @XmlTransient
    private VirtualRepo globalVirtualRepo;

    public static CentralConfig get() {
        ArtifactoryContext context = ContextHelper.get();
        return context.getCentralConfig();
    }

    public void afterPropertiesSet() {
        checkUniqueProxies();
        //Create the repositories directories
        File reposDir = new File(DATA_DIR);
        boolean result = mkdirs(reposDir);
        //Sanity check
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create local repository directory: " +
                            reposDir.getAbsolutePath());
        }
        //Create the deployment dir
        File deploymentDir = new File(LOCAL_REPOS_DEPLOYMENT_DIR);
        result = mkdirs(deploymentDir);
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create local repository deployment directory: " +
                            reposDir.getAbsolutePath());
        }
        //Create the date formatter
        dateFormatter = new SimpleDateFormat(dateFormat);
        dateFormatter.setTimeZone(UTC_TIME_ZONE);
        //Create the jcr repository (needs the dirs)
        if (jcr == null) {
            //chek for null in order not to recreate on system import
            jcr = new JcrHelper();
        }
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
        //Init the repositories
        globalVirtualRepo = new VirtualRepo(VirtualRepo.GLOBAL_VIRTUAL_REPO_KEY,
                localRepositoriesMap, remoteRepositoriesMap);
        Set<Repo> repos = getGlobalVirtualRepo().getLocalAndRemoteRepositories();
        try {
            for (Repo repo : repos) {
                repo.init(this);
            }
        } finally {
            jcr.unbindSession(false);
        }
    }

    public void destroy() throws Exception {
        if (jcr != null) {
            jcr.destroy();
            jcr = null;
        }
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

    private static boolean mkdirs(File file) {
        boolean result = true;
        if (!file.exists()) {
            result = file.mkdirs();
        }
        return result;
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
    public boolean isAnonDownloadsAllowed() {
        return anonDownloadsAllowed;
    }

    public void setAnonDownloadsAllowed(boolean anonDownloadsAllowed) {
        this.anonDownloadsAllowed = anonDownloadsAllowed;
    }

    @XmlElement
    public Backup getBackup() {
        return backup;
    }

    public void setBackup(Backup backup) {
        this.backup = backup;
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

    public File createTempDeploymentDir(LocalRepo repo) {
        File dir = null;
        boolean repoDirCreated = false;
        while (!repoDirCreated) {
            dir = new File(LOCAL_REPOS_DEPLOYMENT_DIR +
                    "/" + repo.getKey() + "-" + System.currentTimeMillis());
            if (!dir.exists()) {
                boolean result = dir.mkdirs();
                if (!result) {
                    LOGGER.warn("Failed to create deployment repository directory '" +
                            dir.getPath() + "'.");
                }
                repoDirCreated = true;
            }
        }
        return dir;
    }

    public JcrHelper getJcr() {
        return jcr;
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
        //Share the jcr because it cannot be intialized twice or shutdown nicely and restarted
        cc.jcr = jcr;
        cc.afterPropertiesSet();
        localRepositoriesMap = cc.localRepositoriesMap;
        remoteRepositoriesMap = cc.remoteRepositoriesMap;
        virtualRepositoriesMap = cc.virtualRepositoriesMap;
        proxies = cc.proxies;
        dateFormat = cc.dateFormat;
        dateFormatter = cc.dateFormatter;
        fileUploadMaxSizeMb = cc.fileUploadMaxSizeMb;
        anonDownloadsAllowed = cc.anonDownloadsAllowed;
        backup = cc.backup;
        serverName = cc.serverName;
        globalVirtualRepo = cc.globalVirtualRepo;
        //Update the backup
        ContextHelper.get().getBackup().update(this);
    }

    public void importFrom(String basePath, StatusHolder status) {
        //Delete all repositories first
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
                    jcr.doInSession(new JcrCallback<Object>() {
                        public Object doInJcr(JcrSessionWrapper session)
                                throws RepositoryException {
                            session.save();
                            return null;
                        }
                    });
                }
                return null;
            }
        });
        String path = basePath + "/" + CentralConfig.FILE_NAME;
        loadFrom(path);
        String repoRootPath = basePath + '/' + LocalRepo.REPO_ROOT;
        importLocalRepositories(repoRootPath, status);
        //Backup the config file and overwrite it
        try {
            FileUtils.copyFile(new File(configFilePath), new File(configFilePath + ".orig"));
            FileUtils.copyFile(new File(path), new File(configFilePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to backup the artifactory.config.xml");
        }
    }

    public void exportTo(String basePath, List<LocalRepo> repos, StatusHolder status) {
        String path = basePath + "/" + CentralConfig.FILE_NAME;
        File destFile = new File(path);
        File srcFile = new File(configFilePath);
        try {
            FileUtils.copyFile(srcFile, destFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy " + CentralConfig.FILE_NAME, e);
        }
        //Export the local repositories
        String repoRootPath = basePath + '/' + LocalRepo.REPO_ROOT;
        for (LocalRepo localRepo : repos) {
            localRepo.exportTo(repoRootPath, status);
        }
    }

    public void exportTo(String basePath, StatusHolder status) {
        List<LocalRepo> repos = globalVirtualRepo.getLocalAndCachedRepositories();
        exportTo(basePath, repos, status);
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

    public LocalRepoInterceptor getLocalRepoInterceptor() {
        return new UniqueSnapshotsCleanerJcrInterceptor();
    }

    void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }
}