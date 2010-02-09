package org.artifactory.webapp.wicket.browse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.ArtifactorySession;
import org.artifactory.webapp.wicket.behavior.AbstractStringResponseAjaxBehavior;
import org.artifactory.webapp.wicket.panels.ContentDialogPanel;
import org.json.JSONArray;
import org.json.JSONObject;
import wicket.AttributeModifier;
import wicket.Request;
import wicket.RequestCycle;
import wicket.ajax.AbstractDefaultAjaxBehavior;
import wicket.ajax.AjaxRequestTarget;
import wicket.markup.ComponentTag;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;
import wicket.model.Model;
import wicket.protocol.http.WebRequestCycle;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class BrowseRepoPage extends ArtifactoryPage {
    private final static Logger LOGGER = Logger.getLogger(BrowseRepoPage.class);

    /*
    * * Constructor.
     */
    public BrowseRepoPage() {
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
                final String relPath;
                final String repoKey;
                try {
                    JSONObject jsonData = new JSONObject(data);
                    JSONObject selectedNode = jsonData.getJSONObject("node");
                    String nodeId = selectedNode.getString("objectId");
                    relPath = relPathFromNodeId(nodeId);
                    repoKey = repoKeyFromNodeId(nodeId);
                } catch (ParseException e) {
                    throw new RuntimeException("Failed to parse selected node from reply: " + data);
                }
                final CentralConfig cc = getCc();
                final LocalRepo repo = cc.localOrCachedRepositoryByKey(repoKey);
                final WebRequestCycle webRequestCycle = getWebRequestCycle();
                JcrHelper jcr = getCc().getJcr();
                if ("download".equals(action)) {
                    String res = (String) jcr.doInSession(
                            new JcrCallback<String>() {
                                public String doInJcr(JcrSessionWrapper session)
                                        throws RepositoryException {
                                    JcrFsItem item = repo.getFsItem(relPath, session);
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
                                            repoKey + ArtifactoryRequest.REPO_SEP +
                                            CentralConfig.REPO_URL_PATH_PREFIX + "/" + relPath;
                                    JSONObject download = new JSONObject();
                                    download.put("url", downloadUrl);
                                    return download.toString();
                                }
                            }
                    );
                    return res;
                } else if ("getChildren".equals(action)) {
                    String res = (String) jcr.doInSession(
                            new JcrCallback<String>() {
                                public String doInJcr(JcrSessionWrapper session)
                                        throws RepositoryException {
                                    JSONArray children = new JSONArray();
                                    JcrFolder dir = (JcrFolder) repo.getFsItem(relPath, session);
                                    List<JcrFsItem> items = dir.getItems();
                                    //Sort files by name
                                    Collections.sort(items);
                                    for (JcrFsItem item : items) {
                                        JSONObject child = new JSONObject();
                                        String name = item.getName();
                                        child.put("title", name);
                                        boolean directory = item.isDirectory();
                                        child.put("isFolder", directory);
                                        //Set the icon and disabling of the view action
                                        try {
                                            if (directory) {
                                                child.put("childIconSrc", "images/folder.png");
                                                child.put("actionsDisabled",
                                                        new JSONArray("[view, download]"));
                                            } else {
                                                if (name.endsWith(".jar")) {
                                                    child.put("childIconSrc", "images/jar.png");
                                                } else if (name.endsWith(".pom")) {
                                                    child.put("childIconSrc", "images/pom.png");
                                                } else if (name.endsWith(".xml") ||
                                                        name.endsWith(".md5") ||
                                                        name.endsWith(".sha1")) {
                                                    //Disable nothing
                                                    child.put("childIconSrc", "images/doc.png");
                                                } else {
                                                    child.put("childIconSrc", "images/doc.png");
                                                    child.put("actionsDisabled",
                                                            new JSONArray("[view, download]"));
                                                }
                                            }
                                        } catch (ParseException e) {
                                            throw new RuntimeException(
                                                    "Failed to create json array.", e);
                                        }
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
                                            String ageStr = "not-cached";
                                            long age = artifact.getAge();
                                            if (age > 0) {
                                                ageStr = DurationFormatUtils.formatDurationHMS(age);
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
                                        child.put("tooltip", tooltip);
                                        children.put(child);
                                    }
                                    return children.toString();
                                }
                            }
                    );
                    return res;
                } else if ("removeNode".equals(action)) {
                    //Check that the user is an admin
                    ArtifactorySession session = getArtifactorySession();
                    if (session.hasRole("admin")) {
                        repo.undeploy(relPath);
                        return "true";
                    } else {
                        LOGGER.warn("Unauthorized trial to remove a node '" +
                                relPath + "' by user '" + session.getPrincipal() + "'.");
                        return "false";
                    }
                } else {
                    return "";
                }
            }
        });
        add(treeController);
        //Add a tree node for each file repository and local cache repository
        List<LocalRepo> repos = getCc().getLocalAndCachedRepositories();
        ListView trees = new ListView("repositories", repos) {
            protected void populateItem(ListItem item) {
                //TODO: [by yl] Fix the widget id collision this is causing
                WebMarkupContainer markupContainer = new WebMarkupContainer("repository");
                LocalRepo localRepo = (LocalRepo) item.getModelObject();
                markupContainer.add(
                        new AttributeModifier("title", true, new Model(localRepo.getKey())));
                markupContainer.add(
                        new AttributeModifier("objectId",
                                true, new Model(getRepoRootPrefix(localRepo))));
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
                final String path = relPathFromNodeId(nodeId);
                String repoKey = repoKeyFromNodeId(nodeId);
                final LocalRepo repo = getCc().localOrCachedRepositoryByKey(repoKey);
                JcrHelper jcr = getCc().getJcr();
                String content = (String) jcr.doInSession(new JcrCallback<String>() {
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

    protected String getPageName() {
        return "Repository Browser";
    }

    private static String getRepoRootPrefix(LocalRepo fileRepository) {
        return fileRepository.getKey() + ArtifactoryRequest.REPO_SEP;
    }

    private static String relPathFromNodeId(String nodeId) {
        int idx = nodeId.indexOf(ArtifactoryRequest.REPO_SEP);
        if (idx + 1 == nodeId.length()) {
            return "";
        } else {
            return nodeId.substring(idx + 1);
        }
    }

    private static String repoKeyFromNodeId(String nodeId) {
        int idx = nodeId.indexOf(ArtifactoryRequest.REPO_SEP);
        return nodeId.substring(0, idx);
    }
}
