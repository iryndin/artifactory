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

package org.artifactory.bintray;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.bintray.BintrayParams;
import org.artifactory.api.bintray.BintrayService;
import org.artifactory.api.bintray.BintrayUser;
import org.artifactory.api.bintray.Repo;
import org.artifactory.api.bintray.RepoPackage;
import org.artifactory.api.bintray.exception.BintrayException;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.api.mail.MailService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.build.InternalBuildService;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.StatusEntry;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.md.PropertiesFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.security.UserInfo;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.EmailException;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.type.TypeReference;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * @author Shay Yaakov
 */
@Service
public class BintrayServiceImpl implements BintrayService {
    private static final Logger log = LoggerFactory.getLogger(BintrayServiceImpl.class);

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private RepositoryService repoService;

    @Autowired
    private JcrService jcrService;

    @Autowired
    private InternalBuildService buildService;

    @Autowired
    private MailService mailService;

    @Autowired
    private AddonsManager addonsManager;

    @Override
    public MultiStatusHolder pushArtifact(ItemInfo itemInfo, BintrayParams bintrayParams) throws IOException {
        MultiStatusHolder status = new MultiStatusHolder();

        HttpClient client = createHTTPClient();
        if (itemInfo.isFolder()) {
            List<ItemInfo> children = repoService.getChildrenDeeply(itemInfo.getRepoPath());
            for (ItemInfo child : children) {
                if (!child.isFolder()) {
                    performPush(client, (FileInfo) itemInfo, bintrayParams, status);
                }
            }
        } else {
            performPush(client, (FileInfo) itemInfo, bintrayParams, status);
        }

        return status;
    }

    @Override
    public MultiStatusHolder pushBuild(Build build, BintrayParams bintrayParams) throws IOException {
        MultiStatusHolder status = new MultiStatusHolder();
        String buildNameAndNumber = build.getName() + ":" + build.getNumber();
        status.setStatus("Starting pushing build '" + buildNameAndNumber + "' to Bintray.", log);
        Set<FileInfo> artifactsToPush = collectArtifactsToPush(build, status);
        if (artifactsToPush != null) {
            HttpClient client = createHTTPClient();
            status.setStatus("Found " + artifactsToPush.size() + " artifacts to push.", log);
            for (FileInfo fileInfo : artifactsToPush) {
                bintrayParams.setPath(fileInfo.getRelPath());
                if (bintrayParams.isUseExistingProps()) {
                    BintrayParams paramsFromProperties = createParamsFromProperties(fileInfo.getRepoPath());
                    bintrayParams.setRepo(paramsFromProperties.getRepo());
                    bintrayParams.setPackageId(paramsFromProperties.getPackageId());
                    bintrayParams.setVersion(paramsFromProperties.getVersion());
                    bintrayParams.setPath(paramsFromProperties.getPath());
                }
                try {
                    performPush(client, fileInfo, bintrayParams, status);
                } catch (IOException e) {
                    sendBuildPushNotification(status, buildNameAndNumber);
                    throw e;
                }
            }
        }

        String message = String.format("Finished pushing build '%s' to Bintray with %s errors and %s warnings.",
                buildNameAndNumber, status.getErrors().size(), status.getWarnings().size());
        status.setStatus(message, log);

        if (bintrayParams.isNotify()) {
            sendBuildPushNotification(status, buildNameAndNumber);
        }

        return status;
    }

    @Override
    public void executeAsyncPushBuild(Build build, BintrayParams bintrayParams) {
        try {
            pushBuild(build, bintrayParams);
        } catch (IOException e) {
            log.error("Push failed with exception: " + e.getMessage());
        }
    }

    private void sendBuildPushNotification(MultiStatusHolder statusHolder, String buildNameAndNumber)
            throws IOException {
        log.info("Sending logs for push build '{}' by mail.", buildNameAndNumber);
        InputStream stream = null;
        try {
            //Get message body from properties and substitute variables
            stream = getClass().getResourceAsStream("/org/artifactory/email/messages/bintrayPushBuild.properties");
            ResourceBundle resourceBundle = new PropertyResourceBundle(stream);
            String body = resourceBundle.getString("body");
            String logBlock = getLogBlock(statusHolder);

            String userEmail = getCurrentUserEmail();
            if (StringUtils.isBlank(userEmail)) {
                log.warn("Couldn't find valid email address. Skipping push build to bintray email notification");
            } else {
                log.debug("Sending push build to Bintray notification to '{}'.", userEmail);
                String message = MessageFormat.format(body, logBlock);
                mailService.sendMail(new String[]{userEmail}, "Push Build to Bintray Report", message);
            }
        } catch (EmailException e) {
            log.error("Error while notification of: '" + buildNameAndNumber + "' messages.", e);
            throw e;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private String getCurrentUserEmail() {
        // currentUser() is not enough since the user might have changed his details from the profile page so the
        // database has the real details while currentUser() is the authenticated user which was not updated.
        String username = userGroupService.currentUser().getUsername();
        UserInfo userInfo = userGroupService.findUser(username);
        return userInfo.getEmail();
    }

    /**
     * Returns an HTML list block of messages extracted from the status holder
     *
     * @param statusHolder Status holder containing messages that should be included in the notification
     * @return HTML list block
     */
    private String getLogBlock(MultiStatusHolder statusHolder) {
        StringBuilder builder = new StringBuilder();

        for (StatusEntry entry : statusHolder.getAllEntries()) {

            //Make one line per row
            String message = entry.getMessage();
            Throwable throwable = entry.getException();
            if (throwable != null) {
                String throwableMessage = throwable.getMessage();
                if (StringUtils.isNotBlank(throwableMessage)) {
                    message += ": " + throwableMessage;
                }
            }
            builder.append(message).append("<br>");
        }

        builder.append("<p>");

        return builder.toString();
    }

    @Override
    public BintrayParams createParamsFromProperties(RepoPath repoPath) {
        BintrayParams bintrayParams = new BintrayParams();
        Properties properties = repoService.getMetadata(repoPath, Properties.class);
        if (properties != null) {
            bintrayParams.setRepo(properties.getFirst(BintrayService.BINTRAY_REPO));
            bintrayParams.setPackageId(properties.getFirst(BintrayService.BINTRAY_PACKAGE));
            bintrayParams.setVersion(properties.getFirst(BintrayService.BINTRAY_VERSION));
            bintrayParams.setPath(properties.getFirst(BintrayService.BINTRAY_PATH));
        }

        return bintrayParams;
    }

    @Override
    public void savePropertiesOnRepoPath(RepoPath repoPath, BintrayParams bintrayParams) {
        Properties properties = repoService.getMetadata(repoPath, Properties.class);
        if (properties == null) {
            properties = PropertiesFactory.create();
        }

        properties.replaceValues(BintrayService.BINTRAY_REPO, Lists.newArrayList(bintrayParams.getRepo()));
        properties.replaceValues(BintrayService.BINTRAY_PACKAGE, Lists.newArrayList(bintrayParams.getPackageId()));
        properties.replaceValues(BintrayService.BINTRAY_VERSION, Lists.newArrayList(bintrayParams.getVersion()));
        properties.replaceValues(BintrayService.BINTRAY_PATH, Lists.newArrayList(bintrayParams.getPath()));
        repoService.setMetadata(repoPath, Properties.class, properties);
    }

    private Set<FileInfo> collectArtifactsToPush(Build build, MultiStatusHolder status) {
        Set<FileInfo> fileInfoSet = Sets.newHashSet();

        List<Module> modules = build.getModules();
        if (modules != null) {
            for (Module module : modules) {
                List<Artifact> artifacts = module.getArtifacts();
                if (artifacts != null) {
                    for (Artifact artifact : artifacts) {
                        Set<FileInfo> artifactInfos = buildService.getBuildFileBeanInfo(build.getName(),
                                build.getNumber(), artifact, false);
                        if (artifactInfos != null && !artifactInfos.isEmpty()) {
                            fileInfoSet.add(artifactInfos.iterator().next());
                        } else {
                            status.setError("Couldn't find matching file for artifact '" + artifact + "'", log);
                            return null;
                        }
                    }
                }
            }
        }

        return fileInfoSet;
    }

    private void performPush(HttpClient client, FileInfo fileInfo, BintrayParams bintrayParams,
            MultiStatusHolder status) throws IOException {
        if (!bintrayParams.isValid()) {
            String message = String.format("Skipping push for '%s' since one of the Bintray properties is missing.",
                    fileInfo.getRelPath());
            status.setWarning(message, log);
            return;
        }

        if (!authorizationService.canAnnotate(fileInfo.getRepoPath())) {
            String message = String.format("Skipping push for '%s' since user doesn't have annotate permissions on it.",
                    fileInfo.getRelPath());
            status.setWarning(message, log);
            return;
        }

        String path = bintrayParams.getPath();
        status.setStatus("Pushing artifact " + path + " to Bintray.", log);
        String requestUrl = getBaseBintrayApiUrl() + PATH_CONTENT + "/" + bintrayParams.getRepo() + "/"
                + bintrayParams.getPackageId() + "/" + bintrayParams.getVersion() + "/" + path;

        PutMethod putMethod = createPutMethod(fileInfo, requestUrl);
        try {
            int statusCode = client.executeMethod(putMethod);
            String message;
            if (statusCode != HttpStatus.SC_CREATED) {
                message = String.format("Push failed for '%s' with status: %s %s", path, statusCode,
                        putMethod.getStatusText());
                status.setError(message, statusCode, log);
            } else {
                message = String.format(
                        "Successfully pushed '%s' to repo: '%s', package: '%s', version: '%s' in Bintray.",
                        path, bintrayParams.getRepo(), bintrayParams.getPackageId(), bintrayParams.getVersion());
                status.setStatus(message, log);
                if (!bintrayParams.isUseExistingProps()) {
                    savePropertiesOnRepoPath(fileInfo.getRepoPath(), bintrayParams);
                }
            }
        } finally {
            putMethod.releaseConnection();
        }
    }

    @Override
    public List<Repo> getReposToDeploy() throws IOException, BintrayException {
        UsernamePasswordCredentials creds = getCurrentUserBintrayCreds();
        String requestUrl = getBaseBintrayApiUrl() + PATH_REPOS + "/" + creds.getUserName();
        InputStream responseStream = null;
        try {
            responseStream = executeGet(requestUrl, creds);
            if (responseStream != null) {
                return JacksonReader.streamAsValueTypeReference(responseStream, new TypeReference<List<Repo>>() {
                });
            }
        } finally {
            IOUtils.closeQuietly(responseStream);
        }

        return null;
    }

    @Override
    public List<String> getPackagesToDeploy(String repoKey) throws IOException, BintrayException {
        UsernamePasswordCredentials creds = getCurrentUserBintrayCreds();
        String requestUrl = getBaseBintrayApiUrl() + PATH_REPOS + "/" + repoKey + "/packages";
        InputStream responseStream = null;
        try {
            responseStream = executeGet(requestUrl, creds);
            if (responseStream != null) {
                return getPackagesList(responseStream);
            }
        } finally {
            IOUtils.closeQuietly(responseStream);
        }

        return null;
    }

    private List<String> getPackagesList(InputStream responseStream) throws IOException {
        List<String> packages = Lists.newArrayList();
        JsonNode packagesTree = JacksonReader.streamAsTree(responseStream);
        Iterator<JsonNode> elements = packagesTree.getElements();
        while (elements.hasNext()) {
            JsonNode packageElement = elements.next();
            String packageName = packageElement.get("name").asText();
            boolean linked = packageElement.get("linked").asBoolean();
            if (!linked) {
                packages.add(packageName);
            }
        }
        return packages;
    }

    @Override
    public List<String> getVersions(String repoKey, String packageId) throws IOException, BintrayException {
        UsernamePasswordCredentials creds = getCurrentUserBintrayCreds();
        String requestUrl = getBaseBintrayApiUrl() + PATH_PACKAGES + "/" + repoKey + "/" + packageId;
        InputStream responseStream = null;
        try {
            responseStream = executeGet(requestUrl, creds);
            if (responseStream != null) {
                RepoPackage repoPackage = JacksonReader.streamAsClass(responseStream, RepoPackage.class);
                return repoPackage.getVersions();
            }
        } finally {
            IOUtils.closeQuietly(responseStream);
        }

        return null;
    }

    @Override
    public String getVersionFilesUrl(BintrayParams bintrayParams) {
        return getBaseBintrayUrl() + VERSION_SHOW_FILES + "/" + bintrayParams.getRepo() + "/"
                + bintrayParams.getPackageId() + "/" + bintrayParams.getVersion();
    }

    @Override
    public BintrayUser getBintrayUser(String username, String apiKey) throws IOException, BintrayException {
        String requestUrl = getBaseBintrayApiUrl() + PATH_USERS + "/" + username;
        InputStream responseStream = null;
        try {
            responseStream = executeGet(requestUrl, new UsernamePasswordCredentials(username, apiKey));
            if (responseStream != null) {
                return JacksonReader.streamAsValueTypeReference(responseStream, new TypeReference<BintrayUser>() {
                });
            }
        } finally {
            IOUtils.closeQuietly(responseStream);
        }

        return null;
    }

    @Override
    public boolean isUserHasBintrayAuth() {
        String username = userGroupService.currentUser().getUsername();
        UserInfo userInfo = userGroupService.findUser(username);
        String bintrayAuth = userInfo.getBintrayAuth();
        if (StringUtils.isNotBlank(bintrayAuth)) {
            String[] bintrayAuthTokens = StringUtils.split(bintrayAuth, ":");
            if (bintrayAuthTokens.length == 2) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getBintrayTestRepoUrl(String url) {
        try {
            URI uri = new URI(url, false);
            if (uri.getHost().contains("bintray")) {
                String bintrayRepoPath = PathUtils.stripFirstPathElement(uri.getPath());
                // Using the Bintray repo ping url
                url = getBaseBintrayApiUrl() + PATH_REPOS + "/" + bintrayRepoPath;
            }
        } catch (URIException e) {
            // Nothing to do here, we simply return the original url
        }

        return url;
    }

    @Override
    public String getBintrayRegistrationUrl() {
        String licenseKeyHash = addonsManager.getLicenseKeyHash();
        StringBuilder builder = new StringBuilder(ConstantValues.bintrayUrl.getString()).append("?source=artifactory");
        if (StringUtils.isNotBlank(licenseKeyHash)) {
            builder.append(":").append(licenseKeyHash);
        }

        return builder.toString();
    }

    private InputStream executeGet(String requestUrl, UsernamePasswordCredentials creds)
            throws IOException, BintrayException {
        GetMethod getMethod = new GetMethod(HttpUtils.encodeQuery(requestUrl));
        HttpClient client = createHTTPClient(creds);
        int status = client.executeMethod(getMethod);
        if (status != HttpStatus.SC_OK) {
            throw new BintrayException(getMethod.getStatusText(), getMethod.getStatusCode());
        } else {
            return getMethod.getResponseBodyAsStream();
        }
    }

    private String getBaseBintrayUrl() {
        return PathUtils.addTrailingSlash(ConstantValues.bintrayUrl.getString());
    }

    private String getBaseBintrayApiUrl() {
        return PathUtils.addTrailingSlash(ConstantValues.bintrayApiUrl.getString());
    }

    private PutMethod createPutMethod(FileInfo fileInfo, String requestUrl) {
        PutMethod putMethod = new PutMethod(HttpUtils.encodeQuery(requestUrl));
        InputStream elementInputStream = jcrService.getStream(
                PathFactoryHolder.get().getAbsolutePath(fileInfo.getRepoPath()));
        RequestEntity requestEntity = new InputStreamRequestEntity(elementInputStream, fileInfo.getSize());
        putMethod.setRequestEntity(requestEntity);
        return putMethod;
    }

    private UsernamePasswordCredentials getCurrentUserBintrayCreds() {
        // currentUser() is not enough since the user might have changed his details from the profile page so the
        // database has the real details while currentUser() is the authenticated user which was not updated.
        String username = userGroupService.currentUser().getUsername();
        UserInfo userInfo = userGroupService.findUser(username);
        String bintrayAuth = userInfo.getBintrayAuth();
        if (StringUtils.isNotBlank(bintrayAuth)) {
            String[] bintrayAuthTokens = StringUtils.split(bintrayAuth, ":");
            if (bintrayAuthTokens.length != 2) {
                throw new IllegalArgumentException("Found invalid Bintray credentials.");
            }

            return new UsernamePasswordCredentials(bintrayAuthTokens[0], bintrayAuthTokens[1]);
        }

        throw new IllegalArgumentException(
                "Couldn't find Bintray credentials, please configure them from the user profile page.");
    }

    private HttpClient createHTTPClient() {
        return createHTTPClient(getCurrentUserBintrayCreds());
    }

    private HttpClient createHTTPClient(UsernamePasswordCredentials creds) {
        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();

        return new HttpClientConfigurator()
                .hostFromUrl(getBaseBintrayApiUrl())
                .soTimeout(15000)
                .connectionTimeout(15000)
                .retry(0, false)
                .proxy(proxy)
                .authentication(creds)
                .getClient();
    }
}
