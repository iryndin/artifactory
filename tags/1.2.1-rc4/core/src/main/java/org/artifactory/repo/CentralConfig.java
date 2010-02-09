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
package org.artifactory.repo;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.transform.SnapshotTransformation;
import org.artifactory.ArtifactoryHome;
import org.artifactory.backup.Backup;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.repo.jaxb.LocalRepositoriesMapAdapter;
import org.artifactory.repo.jaxb.RemoteRepositoriesMapAdapter;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.quartz.CronExpression;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "anonDownloadsAllowed", "backupDir", "backupCronExp",
                "dateFormat", "localRepositoriesMap", "remoteRepositoriesMap", "proxies"})
public class CentralConfig implements InitializingBean, DisposableBean {

    private final static Logger LOGGER = Logger.getLogger(CentralConfig.class);

    public static final String NS = "http://artifactory.jfrog.org/xsd/1.0.0";

    public static final String DATA_DIR = (ArtifactoryHome.path() + "/data").replace('\\', '/');

    public static final String LOCAL_REPOS_DEPLOYMENT_DIR = DATA_DIR + "/tmp";

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ssZ";

    public static final String GLOBAL_REPO_GROUP = "repo";

    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    @XmlElement(name = "localRepositories", required = true)
    @XmlJavaTypeAdapter(LocalRepositoriesMapAdapter.class)
    private OrderedMap<String, LocalRepo> localRepositoriesMap =
            new ListOrderedMap<String, LocalRepo>();

    @XmlElement(name = "remoteRepositories")
    @XmlJavaTypeAdapter(RemoteRepositoriesMapAdapter.class)
    private OrderedMap<String, RemoteRepo> remoteRepositoriesMap =
            new ListOrderedMap<String, RemoteRepo>();

    @XmlElementWrapper(name = "proxies")
    @XmlElement(name = "proxy")
    private List<Proxy> proxies = new ArrayList<Proxy>();

    private String dateFormat = DEFAULT_DATE_FORMAT;
    private DateFormat dateFormatter;

    private boolean anonDownloadsAllowed = true;

    private String backupDir;
    private String backupCronExp;

    /**
     * A name uniquely identifying this artifactory server instance
     */
    private String serverName;

    @XmlTransient
    private JcrHelper jcr;

    public static CentralConfig get() {
        ArtifactoryContext context = ContextUtils.getContext();
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
        jcr = new JcrHelper();
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
        for (Repo repoConfig : getLocalAndRemoteRepositoriesList()) {
            repoConfig.init(this);
        }
    }

    public void destroy() throws Exception {
        if (jcr != null) {
            jcr.destroy();
            jcr = null;
        }
    }

    public void backupRepos() {
        backupRepos(backupDir, new Date());
    }

    public void backupRepos(Date date) {
        backupRepos(backupDir, date);
    }

    public void backupRepos(String dir, Date date) {
        List<LocalRepo> localRepos = getLocalAndCachedRepositories();
        String timestamp = SnapshotTransformation.getUtcDateFormatter().format(date);
        for (LocalRepo repo : localRepos) {
            String key = repo.getKey();
            File targetDir = new File(dir + "/" + timestamp + "/" + key);
            repo.exportToDir(targetDir);
        }
    }

    public void restoreRepos(String dir) {

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

    public synchronized List<Repo> getLocalAndRemoteRepositoriesList() {
        ArrayList<Repo> repos = new ArrayList<Repo>();
        repos.addAll(localRepositoriesMap.values());
        repos.addAll(remoteRepositoriesMap.values());
        return repos;
    }

    public synchronized List<RemoteRepo> getRemoteRepositories() {
        return new ArrayList<RemoteRepo>(remoteRepositoriesMap.values());
    }

    public synchronized List<LocalCacheRepo> getLocalCaches() {
        List<LocalCacheRepo> localCaches = new ArrayList<LocalCacheRepo>();
        for (RemoteRepo repo : remoteRepositoriesMap.values()) {
            if (repo.isStoreArtifactsLocally()) {
                localCaches.add(repo.getLocalCacheRepo());
            }
        }
        return localCaches;
    }

    public synchronized List<LocalRepo> getLocalRepositories() {
        return new ArrayList<LocalRepo>(localRepositoriesMap.values());
    }

    public synchronized List<LocalRepo> getLocalAndCachedRepositories() {
        List<LocalRepo> localRepos = getLocalRepositories();
        List<LocalCacheRepo> localCaches = getLocalCaches();
        List<LocalRepo> repos = new ArrayList<LocalRepo>(localRepos);
        repos.addAll(localCaches);
        return repos;
    }

    public LocalRepo localRepositoryByKey(String key) {
        return localRepositoriesMap.get(key);
    }

    public LocalRepo localOrCachedRepositoryByKey(String key) {
        LocalRepo localRepo = localRepositoryByKey(key);
        if (localRepo == null) {
            //Try to get cached repositories
            int idx = key.lastIndexOf(LocalCacheRepo.PATH_SUFFIX);
            if (idx > 1) {
                RemoteRepo remoteRepo = remoteRepositoryByKey(key.substring(0, idx));
                if (remoteRepo != null && remoteRepo.isStoreArtifactsLocally()) {
                    localRepo = remoteRepo.getLocalCacheRepo();
                }
            }
        }
        return localRepo;
    }

    public RemoteRepo remoteRepositoryByKey(String key) {
        return remoteRepositoriesMap.get(key);
    }

    public synchronized void putLocalRepo(LocalRepo repo) {
        localRepositoriesMap.put(repo.getKey(), repo);
    }

    public synchronized void putRemoteRepo(RemoteRepo repo) {
        remoteRepositoriesMap.put(repo.getKey(), repo);
    }

    private static boolean mkdirs(File file) {
        boolean result = true;
        if (!file.exists()) {
            result = file.mkdirs();
        }
        return result;
    }

    @XmlElement(defaultValue = DEFAULT_DATE_FORMAT, required = false)
    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @XmlElement(defaultValue = "true", required = false)
    public boolean isAnonDownloadsAllowed() {
        return anonDownloadsAllowed;
    }

    public void setAnonDownloadsAllowed(boolean anonDownloadsAllowed) {
        this.anonDownloadsAllowed = anonDownloadsAllowed;
    }

    @XmlElement(required = false)
    public String getBackupDir() {
        if (backupDir == null) {
            backupDir = Backup.DEFAULT_BACKUP_DIR;
        }
        return backupDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    @XmlElement(required = false)
    public String getBackupCronExp() {
        return backupCronExp;
    }

    public void setBackupCronExp(String backupCronExp) {
        try {
            new CronExpression(backupCronExp);
            this.backupCronExp = backupCronExp;
        } catch (ParseException e) {
            LOGGER.error(
                    "Bad backup cron expression '" + backupCronExp + "' will be ignored (" +
                            e.getMessage() + ").");
        }
    }

    @XmlElement(required = false)
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
}