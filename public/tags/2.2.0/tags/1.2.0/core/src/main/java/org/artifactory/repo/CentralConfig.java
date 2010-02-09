package org.artifactory.repo;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.backup.Backup;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.repo.jaxb.LocalRepositoriesMapAdapter;
import org.artifactory.repo.jaxb.RemoteRepositoriesMapAdapter;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "anonDownloadsAllowed", "backupDir", "backupCronExp",
                "dateFormat", "localRepositoriesMap", "remoteRepositoriesMap", "proxies"})
public class CentralConfig implements InitializingBean, DisposableBean {

    private final static Logger LOGGER = Logger.getLogger(CentralConfig.class);

    public static final String NS = "http://artifactory.jfrog.org/xsd/1.0.0";

    public static final String LOCAL_REPOS_DIR =
            (ArtifactoryHome.path() + "/repos").replace('\\', '/');

    public static final String LOCAL_REPOS_DEPLOYMENT_DIR =
            LOCAL_REPOS_DIR + "/tmp";

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ssZ";

    public static final String DEFAULT_REPO_GROUP = "repo";

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

    private boolean anonDownloadsAllowed = true;

    private String backupDir;
    private String backupCronExp;

    /**
     * A name uniquely identifying this artifactory server instance
     */
    private String serverName;

    @XmlTransient
    private JcrHelper jcr;

    public void afterPropertiesSet() {
        //Create the repositories directories
        File reposDir = new File(LOCAL_REPOS_DIR);
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
        } catch (ParseException e) {
            throw new RuntimeException(
                    "Invalid backup cron expression '" + backupCronExp + "'.", e);
        }
        this.backupCronExp = backupCronExp;
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
}