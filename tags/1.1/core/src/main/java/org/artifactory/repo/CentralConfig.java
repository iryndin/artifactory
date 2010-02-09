package org.artifactory.repo;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.artifactory.Startable;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.maven.MavenWrapper;
import org.artifactory.repo.jaxb.LocalRepositoriesMapAdapter;
import org.artifactory.repo.jaxb.RemoteRepositoriesMapAdapter;
import org.artifactory.security.SecurityHelper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "config")
@XmlType(name = "CentralConfigType",
        propOrder = {"serverName", "adminCredential", "localRepositoriesDir", "dateFormat",
                "localRepositoriesMap", "remoteRepositoriesMap", "proxies"})
public class CentralConfig implements Startable {

    private final static Logger LOGGER = Logger.getLogger(CentralConfig.class);

    public static final String NS = "http://artifactory.jfrog.org/xsd/1.0.0";

    public static final String ATTR_CONFIG = "artifactory.config";

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy HH:mm:ssZ";

    public static final String REPO_URL_PATH_PREFIX = "repo";

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

    private String localRepositoriesDir;

    @XmlTransient
    private String localRepositoriesDeploymentDir;

    private String dateFormat = DEFAULT_DATE_FORMAT;

    /**
     * md5 hash of the admin password
     */
    private String adminCredential;

    /**
     * A name uniquely identifying this artifactory server instance
     */
    private String serverName;

    @XmlTransient
    private MavenWrapper mavenWrapper = new MavenWrapper(this);

    @XmlTransient
    private JcrHelper jcr;

    @XmlTransient
    private SecurityHelper security;

    public void start() {
        jcr = new JcrHelper(this);
        jcr.start();
        security = new SecurityHelper(adminCredential);
        security.start();
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

    public void stop() {
        security.stop();
        jcr.stop();
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

    public MavenWrapper getMavenWrapper() {
        return mavenWrapper;
    }

    public String getLocalRepositoriesDir() {
        return localRepositoriesDir;
    }

    public String getLocalRepositoriesDeploymentDir() {
        return localRepositoriesDeploymentDir;
    }

    public void setLocalRepositoriesDir(String localRepositoriesDir) {
        //Hack around m21 that adds a leading slash to a repo, causing a relative repo to be created
        //at the absolute root path
        File repoDir = new File(localRepositoriesDir);
        this.localRepositoriesDir = repoDir.getAbsolutePath().replace('\\', '/');
        boolean result = mkdirs(repoDir);
        //Sanity check
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create local repository directory: " +
                            repoDir.getAbsolutePath());
        }
        //Create the deployment dir
        this.localRepositoriesDeploymentDir = this.localRepositoriesDir + "/tmp";
        File deploymentDir = new File(this.localRepositoriesDeploymentDir);
        result = mkdirs(deploymentDir);
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create local repository deployment directory: " +
                            repoDir.getAbsolutePath());
        }
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

    @XmlElement(required = true)
    public String getAdminCredential() {
        return adminCredential;
    }

    public void setAdminCredential(String adminCredential) {
        this.adminCredential = adminCredential;
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

    public JcrHelper getJcr() {
        return jcr;
    }

    public SecurityHelper getSecurity() {
        return security;
    }

    public File createTempDeploymentDir(LocalRepo repo) {
        String deploymentDir = getLocalRepositoriesDeploymentDir();
        File dir = null;
        boolean repoDirCreated = false;
        while (!repoDirCreated) {
            dir = new File(deploymentDir + "/" + repo.getKey() + "-" + System.currentTimeMillis());
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
}