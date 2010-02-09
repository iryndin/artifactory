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
package org.artifactory.webapp.wicket.browse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.artifactory.webapp.wicket.behavior.AbstractStringResponseAjaxBehavior;
import org.artifactory.webapp.wicket.components.ContentDialogPanel;
import org.artifactory.webapp.wicket.panel.TitlePanel;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class BrowseRepoPanel extends TitlePanel {
    private final static Logger LOGGER = Logger.getLogger(BrowseRepoPanel.class);

    private static final char REPO_SEP = ':';

    public BrowseRepoPanel(String string) {
        super(string);

        //Add the tree controller
        final WebMarkupContainer treeController = new WebMarkupContainer("controller");
        treeController.setOutputMarkupId(true);
        treeController.add(new AbstractStringResponseAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                final CharSequence actionCall = getCallbackUrl();
                tag.put("RPCUrl", actionCall);
            }

            @SuppressWarnings({"UnnecessaryLocalVariable"})
            protected String getResponse() {
                Request request = getRequest();
                String action = request.getParameter("action");
                String data = request.getParameter("data");
                final String path;
                final String repoKey;
                try {
                    JSONObject jsonData = new JSONObject(data);
                    JSONObject selectedNode = jsonData.getJSONObject("node");
                    String nodeId = selectedNode.getString("objectId");
                    path = pathFromNodeId(nodeId);
                    repoKey = repoKeyFromNodeId(nodeId);
                } catch (ParseException e) {
                    throw new RuntimeException("Failed to parse selected node from reply: " + data);
                }
                //Find out who we are
                String username = SecurityHelper.getUsername();
                //Filer the results
                ArtifactoryContext context = ContextUtils.getContext();
                final SecurityHelper security = context.getSecurity();
                RepoPath repoPath = new RepoPath(repoKey, path);
                boolean reader = security.canRead(repoPath);
                if (!reader && path.length() > 0) {
                    //Don't bother with stuff that we do not have read access to
                    return "";
                }
                final boolean deployer = security.canDeploy(repoPath);
                final CentralConfig cc = CentralConfig.get();;
                final LocalRepo repo = cc.localOrCachedRepositoryByKey(repoKey);
                final WebRequestCycle webRequestCycle = (WebRequestCycle) getRequestCycle();
                JcrHelper jcr = cc.getJcr();
                if ("download".equals(action)) {
                    String res = jcr.doInSession(
                            new JcrCallback<String>() {
                                public String doInJcr(JcrSessionWrapper session)
                                        throws RepositoryException {
                                    JcrFsItem item = repo.getFsItem(path, session);
                                    boolean directory = item.isDirectory();
                                    if (directory) {
                                        return "false";
                                    }
                                    HttpServletRequest servletRequest =
                                            webRequestCycle.getWebRequest()
                                                    .getHttpServletRequest();
                                    String downloadUrl = servletRequest.getScheme() + "://" +
                                            servletRequest.getServerName() + ":" +
                                            servletRequest.getServerPort() +
                                            servletRequest.getContextPath() + "/" +
                                            repoKey + "/" + path;
                                    if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("Download URL is " + downloadUrl);
                                    }
                                    JSONObject download = new JSONObject();
                                    download.put("url", downloadUrl);
                                    return download.toString();
                                }
                            }
                    );
                    return res;
                } else if ("getChildren".equals(action)) {
                    String res = jcr.doInSession(
                            new JcrCallback<String>() {
                                @SuppressWarnings({"ConstantConditions"})
                                public String doInJcr(JcrSessionWrapper session)
                                        throws RepositoryException {
                                    JSONArray children = new JSONArray();
                                    JcrFolder dir = (JcrFolder) repo.getFsItem(path, session);
                                    List<JcrFsItem> items = dir.getItems();
                                    //Sort files by name
                                    Collections.sort(items);
                                    for (JcrFsItem item : items) {
                                        //Check if we should return the child
                                        RepoPath childRepoPath =
                                                new RepoPath(item.repoKey(), item.relPath());
                                        boolean childReader = security.canRead(childRepoPath);
                                        if (!childReader) {
                                            //Don't bother with stuff that we do not have read
                                            //access to
                                            continue;
                                        }
                                        final boolean childDeployer =
                                                security.canDeploy(childRepoPath);
                                        String name = item.getName();
                                        //Skip checksum files
                                        if (MavenUtils.isChecksum(name)) {
                                            continue;
                                        }
                                        JSONObject child = new JSONObject();
                                        child.put("title", name);
                                        boolean directory = item.isDirectory();
                                        child.put("isFolder", directory);
                                        //Set the icon and disabling of the view action
                                        JSONArray disabledActions = new JSONArray();
                                        if (!childDeployer) {
                                            //Disable admin actions for non-admins
                                            disabledActions.put("remove");
                                        }
                                        if (!childDeployer || !repo.isCache()) {
                                            //Disable zapping for non-caches
                                            disabledActions.put("zap");
                                        }
                                        if (directory) {
                                            disabledActions.put("view");
                                            disabledActions.put("download");
                                            child.put("childIconSrc", "../images/folder.png");
                                            JcrFolder folder = (JcrFolder) item;
                                            //Expand version folders (deepest folders)
                                            if (path.length() > 0) {
                                                List<JcrFsItem> list = folder.getItems();
                                                boolean deepestFolder = true;
                                                //Check if a folder has no subfolder children
                                                for (JcrFsItem fsItem : list) {
                                                    if (fsItem.isDirectory()) {
                                                        deepestFolder = false;
                                                        break;
                                                    }
                                                }
                                                if (deepestFolder) {
                                                    child.put("expandLevel", "1");
                                                }
                                            }
                                        } else {
                                            disabledActions.put("remove");
                                            if (name.endsWith(".jar")) {
                                                child.put("childIconSrc", "../images/jar.png");
                                            } else if (name.endsWith(".pom")) {
                                                child.put("childIconSrc", "../images/pom.png");
                                            } else if (name.endsWith(".xml")) {
                                                //Disable nothing
                                                child.put("childIconSrc", "../images/doc.png");
                                            } else {
                                                child.put("childIconSrc", "../images/doc.png");
                                                disabledActions.put("view");
                                                disabledActions.put("download");
                                            }
                                        }
                                        child.put("actionsDisabled", disabledActions);
                                        String id = getRepoRootPrefix(repo) + item.relPath();
                                        child.put("objectId", id);
                                        //Add the tooltip info
                                        String tooltip;
                                        if (item.isDirectory()) {
                                            tooltip = "<div>" + item.getName() + "</div>";
                                        } else {
                                            JcrFile file = (JcrFile) item;
                                            ArtifactResource artifact = new ArtifactResource(file);
                                            long size = artifact.getSize();
                                            //If we are looking at a cached item, check the expiry
                                            //from the remote repository
                                            String ageStr = "non-cached";
                                            long age = artifact.getAge();
                                            if (age > 0) {
                                                ageStr = DurationFormatUtils
                                                        .formatDuration(age, "d'd' H'h' m'm' s's'");
                                            }
                                            tooltip = (size != -1 ? "<div>size: " +
                                                    FileUtils.byteCountToDisplaySize(size) +
                                                    "</div>" : "") + "<div>groupId: " +
                                                    artifact.getGroupId() + "</div>" +
                                                    "<div>artifactId: " +
                                                    artifact.getArtifactId() + "</div>" +
                                                    "<div>version: " +
                                                    artifact.getVersion() + "</div>" +
                                                    "<div>age: " + ageStr + "</div>";
                                        }
                                        tooltip += "<div>deployed-by: " +
                                                item.modifiedBy() + "</div>";
                                        child.put("tooltip", tooltip);
                                        children.put(child);
                                    }
                                    return children.toString();
                                }
                            }
                    );
                    return res;
                } else if ("removeNode".equals(action)) {
                    //Check that the user is a deployer
                    if (deployer) {
                        repo.undeploy(path);
                        return "true";
                    } else {
                        LOGGER.warn("Unauthorized trial to remove a node '" +
                                path + "' by user '" + username + "'.");
                        return "false";
                    }
                } else if ("zap".equals(action)) {
                    //Check that the user is a deployer
                    if (deployer) {
                        //Double sanity check that we are in a cache
                        if (repo.isCache()) {
                            LocalCacheRepo cache = (LocalCacheRepo) repo;
                            cache.expire(path);
                            return "true";
                        } else {
                            LOGGER.warn("Got a zap request on a non-local-cache node '" +
                                    path + "' by user '" + username + "'.");
                            return "false";
                        }
                    } else {
                        LOGGER.warn("Unauthorized trial to zap a node '" +
                                path + "' by user '" + username + "'.");
                        return "false";
                    }
                } else {
                    return "";
                }
            }
        });
        add(treeController);
        //Add a tree node for each file repository and local cache repository
        List<LocalRepo> repos = CentralConfig.get().getLocalAndCachedRepositories();
        ListView trees = new ListView("repositories", repos) {
            protected void populateItem(ListItem item) {
                WebMarkupContainer markupContainer = new WebMarkupContainer("repository");
                LocalRepo localRepo = (LocalRepo) item.getModelObject();
                markupContainer.add(
                        new AttributeModifier("title", true, new Model(localRepo.getKey())));
                markupContainer.add(
                        new AttributeModifier("objectId",
                                true, new Model(getRepoRootPrefix(localRepo))));
                String repoKey = localRepo.getKey();
                markupContainer.add(new AttributeModifier("widgetId", true, new Model(repoKey)));
                //Disable appropriate actions
                ArtifactoryContext context = ContextUtils.getContext();
                SecurityHelper security = context.getSecurity();
                boolean deployer = security.canDeploy(RepoPath.forRepo(repoKey));
                String disabledActions = "view;download";
                if (!deployer) {
                    disabledActions += ";remove;zap";
                } else if (!localRepo.isCache()) {
                    disabledActions += ";zap";
                }
                markupContainer.add(
                        new AttributeModifier("actionsDisabled", true, new Model(disabledActions)));
                item.add(markupContainer);
            }
        };
        add(trees);
        //Add the content viewer dialog
        final ContentDialogPanel contentViewer = new ContentDialogPanel("contentViewer");
        contentViewer.add(new AbstractDefaultAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("callbackUrl", getCallbackUrl());
            }

            @SuppressWarnings({"UnusedDeclaration"})
            protected void respond(AjaxRequestTarget target) {
                RequestCycle rc = RequestCycle.get();
                String nodeId = rc.getRequest().getParameter("nodeId");
                String title = rc.getRequest().getParameter("title");
                final String path = pathFromNodeId(nodeId);
                String repoKey = repoKeyFromNodeId(nodeId);
                CentralConfig cc = CentralConfig.get();
                final LocalRepo repo = cc.localOrCachedRepositoryByKey(repoKey);
                JcrHelper jcr = cc.getJcr();
                String content = jcr.doInSession(new JcrCallback<String>() {
                    public String doInJcr(JcrSessionWrapper session) throws RepositoryException {
                        JcrFsItem item = repo.getFsItem(path, session);
                        String content;
                        if (ArtifactResource.isStandardPackaging(item.relPath())) {
                            ArtifactResource artifact = new ArtifactResource((JcrFile) item);
                            content = getArtifactMetadataContent(artifact);
                        } else if (!item.isDirectory()) {
                            InputStream is = null;
                            try {
                                is = ((JcrFile) item).getStream();
                                content = IOUtils.toString(is, "UTF-8");
                            } catch (IOException e) {
                                content = "Failed to read content from '" + item.getName() + "' ("
                                        + e.getMessage() + ").";
                            } finally {
                                IOUtils.closeQuietly(is);
                            }
                        } else {
                            content = "No readable content found for '" + item.getName() + "'.";
                        }
                        return content;
                    }
                });
                contentViewer.ajaxUpdate(content, target);
            }
        });
        add(contentViewer);
    }

    private static String getRepoRootPrefix(LocalRepo fileRepository) {
        return fileRepository.getKey() + REPO_SEP;
    }

    private static String pathFromNodeId(String nodeId) {
        int idx = nodeId.indexOf(REPO_SEP);
        if (idx + 1 == nodeId.length()) {
            return "";
        } else {
            return nodeId.substring(idx + 1);
        }
    }

    private static String repoKeyFromNodeId(String nodeId) {
        int idx = nodeId.indexOf(REPO_SEP);
        return nodeId.substring(0, idx);
    }
}
