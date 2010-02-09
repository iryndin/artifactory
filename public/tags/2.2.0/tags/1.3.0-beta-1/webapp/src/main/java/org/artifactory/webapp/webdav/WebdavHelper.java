/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.webdav;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import org.artifactory.config.CentralConfig;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.ArtifactoryResponse;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.utils.MimeTypes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * see: http://svn.apache.org/viewvc/tomcat/container/tc5.5.x/catalina/src/share/org/apache/catalina/servlets/WebdavServlet.java
 */
public class WebdavHelper {

    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(WebdavHelper.class);

    private ArtifactoryRequest request;
    private ArtifactoryResponse response;

    /**
     * Default depth is infite.
     */
    private static final int INFINITY = 3;// To limit tree browsing a bit

    /**
     * PROPFIND - Specify a property mask.
     */
    private static final int FIND_BY_PROPERTY = 0;

    /**
     * PROPFIND - Display all properties.
     */
    private static final int FIND_ALL_PROP = 1;

    /**
     * PROPFIND - Return property names.
     */
    private static final int FIND_PROPERTY_NAMES = 2;

    /**
     * Default namespace.
     */
    protected static final String DEFAULT_NAMESPACE = "DAV:";

    /**
     * Simple date format for the creation date ISO representation (partial).
     */
    protected static final SimpleDateFormat creationDateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        //GMT timezone - all HTTP dates are on GMT
        creationDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public WebdavHelper(ArtifactoryRequest request, ArtifactoryResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * PROPFIND Method.
     *
     * @throws ServletException
     * @throws IOException
     */
    @SuppressWarnings({"OverlyComplexMethod"})
    public void handlePropfind() throws ServletException, IOException {
        // Retrieve the resources
        RepoPath repoPath = request.getRepoPath();
        String path = repoPath.getPath();
        int depth = INFINITY;
        String depthStr = request.getHeader("Depth");
        if (depthStr != null) {
            if (depthStr.equals("0")) {
                depth = 0;
            } else if (depthStr.equals("1")) {
                depth = 1;
            } else if (depthStr.equals("infinity")) {
                depth = INFINITY;
            }
        }
        List<String> properties = null;
        int propertyFindType = FIND_ALL_PROP;
        Node propNode = null;
        //get propertyNode and type
        if (request.getContentLength() != 0) {
            DocumentBuilder documentBuilder = getDocumentBuilder();
            try {
                Document document = documentBuilder.parse(
                        new InputSource(request.getInputStream()));
                // Get the root element of the document
                Element rootElement = document.getDocumentElement();
                NodeList childList = rootElement.getChildNodes();

                for (int i = 0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                        case Node.TEXT_NODE:
                            break;
                        case Node.ELEMENT_NODE:
                            if (currentNode.getNodeName().endsWith("prop")) {
                                propertyFindType = FIND_BY_PROPERTY;
                                propNode = currentNode;
                            }
                            if (currentNode.getNodeName().endsWith("propname")) {
                                propertyFindType = FIND_PROPERTY_NAMES;
                            }
                            if (currentNode.getNodeName().endsWith("allprop")) {
                                propertyFindType = FIND_ALL_PROP;
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Webdav propfind failed.", e);
            }
        }

        if (propertyFindType == FIND_BY_PROPERTY) {
            properties = getPropertiesFromXml(propNode);
        }

        response.setStatus(WebdavStatus.SC_MULTI_STATUS);
        response.setContentType("text/xml; charset=UTF-8");

        // Create multistatus object
        //Writer writer = new StringWriter();
        Writer writer = response.getWriter();
        XmlWriter generatedXml = new XmlWriter(writer);
        generatedXml.writeXMLHeader();
        generatedXml.writeElement(null, "multistatus xmlns=\"" + DEFAULT_NAMESPACE + "\"",
                XmlWriter.OPENING);
        if (depth > 0) {
            recursiveParseProperties(generatedXml, path, propertyFindType, properties, depth);
        } else {
            parseProperties(generatedXml, path, propertyFindType, properties);
        }
        generatedXml.writeElement(null, "multistatus", XmlWriter.CLOSING);
        generatedXml.sendData();
        response.flush();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void handleMkcol() throws ServletException, IOException {
        RepoPath repoPath = request.getRepoPath();
        //Check that we are allowed to write
        checkCanDeploy(repoPath);
        final LocalRepo repo = getLocalRepo();
        String path = repoPath.getPath();
        JcrFolder targetFolder = new JcrFolder(repo.getFolder().getAbsolutePath() + "/" + path);
        targetFolder.mkdirs();
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    public void handleDelete() throws ServletException, IOException {
        RepoPath repoPath = request.getRepoPath();
        //Check that we are allowed to write
        checkCanDeploy(repoPath);
        final LocalRepo repo = getLocalRepo();
        String path = repoPath.getPath();
        repo.undeploy(path);
        response.sendOk();
    }

    public void handleOptions() throws ServletException, IOException {
        response.setHeader("DAV", "1,2");
        response.setHeader("Allow", "OPTIONS, MKCOL, PUT, GET, HEAD, POST, DELETE, PROPFIND");
        response.setHeader("MS-Author-Via", "DAV");
        response.sendOk();
    }

    private void checkCanDeploy(RepoPath repoPath) throws IOException {
        ArtifactoryContext context = ContextHelper.get();
        ArtifactorySecurityManager security = context.getSecurity();
        boolean canDeploy = security.canDeploy(repoPath);
        if (!canDeploy) {
            response.sendError(HttpStatus.SC_FORBIDDEN);
            AccessLogger.deployDenied(repoPath);
            throw new RuntimeException("Not allowed writing to '" + repoPath + "'.");
        }
    }

    /**
     * resourcetype Return JAXP document builder instance.
     *
     * @return
     * @throws javax.servlet.ServletException
     */
    private DocumentBuilder getDocumentBuilder() throws ServletException {
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ServletException("jaxp failed");
        }
        return documentBuilder;
    }

    private List<String> getPropertiesFromXml(Node propNode) {
        List<String> properties;
        properties = new ArrayList<String>();
        NodeList childList = propNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            Node currentNode = childList.item(i);
            switch (currentNode.getNodeType()) {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    String nodeName = currentNode.getNodeName();
                    String propertyName;
                    if (nodeName.indexOf(':') != -1) {
                        propertyName = nodeName.substring(nodeName.indexOf(':') + 1);
                    } else {
                        propertyName = nodeName;
                    }
                    // href is a live property which is handled differently
                    properties.add(propertyName);
                    break;
            }
        }
        return properties;
    }

    /**
     * Propfind helper method.
     *
     * @param generatedXml           XML response to the Propfind request
     * @param path                   Path of the current resource
     * @param type                   Propfind type
     * @param propertiesList<String> If the propfind type is find properties by name, then this
     *                               List<String> contains those properties
     */
    @SuppressWarnings({"OverlyComplexMethod"})
    private void parseProperties(XmlWriter generatedXml, final String path,
            int type, List<String> propertiesList) throws IOException {
        JcrFsItem item = getJcrFsItem(path);
        if (item == null) {
            LOGGER.warn("Item '" + path + "' not found.");
            return;
        }
        String creationDate = getIsoCreationDate(item.getCreated());
        boolean isFolder = item.isDirectory();
        String lastModified =
                getIsoCreationDate(isFolder ? 0 : ((JcrFile) item).getLastModified());
        String resourceLength = isFolder ? "0" : (((JcrFile) item).getSize() + "");

        generatedXml.writeElement(null, "response", XmlWriter.OPENING);
        String status = "HTTP/1.1 " + WebdavStatus.SC_OK + " " +
                WebdavStatus.getStatusText(WebdavStatus.SC_OK);

        //Generating href element
        generatedXml.writeElement(null, "href", XmlWriter.OPENING);
        String origPath = request.getPath();
        String uri = request.getUri();
        String hrefBase = uri;
        if (origPath.length() > 0) {
            int idx = uri.lastIndexOf(origPath);
            if (idx > 0) {
                //When called recursively avoid concatenating the original path on top of itself 
                hrefBase = uri.substring(0, idx);
            }
        }
        String href = hrefBase + path;
        /*if (isFolder && !href.endsWith("/")) {
            href += "/";
        }*/

        //String encodedHref = encoder.encode(href);
        generatedXml.writeText(href);
        generatedXml.writeElement(null, "href", XmlWriter.CLOSING);

        String resourceName = path;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1) {
            resourceName = resourceName.substring(lastSlash + 1);
        }

        switch (type) {
            case FIND_ALL_PROP:
                generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                generatedXml.writeElement(null, "prop", XmlWriter.OPENING);

                generatedXml.writeProperty(null, "creationdate", creationDate);
                generatedXml.writeElement(null, "displayname", XmlWriter.OPENING);
                generatedXml.writeData(resourceName);
                generatedXml.writeElement(null, "displayname", XmlWriter.CLOSING);
                if (!isFolder) {
                    generatedXml.writeProperty(null, "getlastmodified", lastModified);
                    generatedXml.writeProperty(null, "getcontentlength", resourceLength);

                    String contentType = MimeTypes.getMimeTypeByPathAsString(path);
                    if (contentType != null) {
                        generatedXml.writeProperty(null, "getcontenttype", contentType);
                    }
                    generatedXml.writeProperty(
                            null, "getetag", getEtag(resourceLength, lastModified));
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                } else {
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.OPENING);
                    generatedXml.writeElement(null, "collection", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.CLOSING);
                }
                generatedXml.writeProperty(null, "source", "");
                generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                generatedXml.writeText(status);
                generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);
                break;
            case FIND_PROPERTY_NAMES:
                generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                generatedXml.writeElement(null, "prop", XmlWriter.OPENING);
                generatedXml.writeElement(null, "creationdate", XmlWriter.NO_CONTENT);
                generatedXml.writeElement(null, "displayname", XmlWriter.NO_CONTENT);
                if (!isFolder) {
                    generatedXml.writeElement(null, "getcontentlanguage", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "getcontentlength", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "getcontenttype", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "getetag", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "getlastmodified", XmlWriter.NO_CONTENT);
                }
                generatedXml.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                generatedXml.writeElement(null, "source", XmlWriter.NO_CONTENT);
                generatedXml.writeElement(null, "lockdiscovery", XmlWriter.NO_CONTENT);
                generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                generatedXml.writeText(status);
                generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);
                break;
            case FIND_BY_PROPERTY:
                List<String> propertiesNotFound = new ArrayList<String>();
                // Parse the list of properties
                generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                generatedXml.writeElement(null, "prop", XmlWriter.OPENING);
                for (String property : propertiesList) {
                    if (property.equals("creationdate")) {
                        generatedXml.writeProperty(null, "creationdate", creationDate);
                    } else if (property.equals("displayname")) {
                        generatedXml.writeElement(null, "displayname", XmlWriter.OPENING);
                        generatedXml.writeData(resourceName);
                        generatedXml.writeElement(null, "displayname", XmlWriter.CLOSING);
                    } else if (property.equals("getcontentlanguage")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeElement(
                                    null, "getcontentlanguage", XmlWriter.NO_CONTENT);
                        }
                    } else if (property.equals("getcontentlength")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getcontentlength", resourceLength);
                        }
                    } else if (property.equals("getcontenttype")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getcontenttype",
                                    MimeTypes.getMimeTypeByPathAsString(path));
                        }
                    } else if (property.equals("getetag")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getetag",
                                    getEtag(resourceLength, lastModified));
                        }
                    } else if (property.equals("getlastmodified")) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getlastmodified", lastModified);
                        }
                    } else if (property.equals("source")) {
                        generatedXml.writeProperty(null, "source", "");
                    } else {
                        propertiesNotFound.add(property);
                    }
                    //Always include resource type
                    if (isFolder) {
                        generatedXml.writeElement(null, "resourcetype", XmlWriter.OPENING);
                        generatedXml.writeElement(null, "collection", XmlWriter.NO_CONTENT);
                        generatedXml.writeElement(null, "resourcetype", XmlWriter.CLOSING);
                    } else {
                        generatedXml.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                    }
                }

                generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                generatedXml.writeText(status);
                generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);

                if (propertiesNotFound.size() > 0) {
                    status = "HTTP/1.1 " + WebdavStatus.SC_NOT_FOUND + " " +
                            WebdavStatus.getStatusText(WebdavStatus.SC_NOT_FOUND);
                    generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                    generatedXml.writeElement(null, "prop", XmlWriter.OPENING);
                    for (String propertyNotFound : propertiesNotFound) {
                        generatedXml.writeElement(null, propertyNotFound, XmlWriter.NO_CONTENT);
                    }
                    generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                    generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                    generatedXml.writeText(status);
                    generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                    generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);

                }
                break;

        }
        generatedXml.writeElement(null, "response", XmlWriter.CLOSING);
    }

    /**
     * goes recursive through all folders. used by propfind
     */
    private void recursiveParseProperties(XmlWriter generatedXml, String currentPath,
            int propertyFindType, List<String> properties, int depth) throws IOException {
        parseProperties(generatedXml, currentPath, propertyFindType, properties);
        JcrFsItem item = getJcrFsItem(currentPath);
        if (item == null) {
            LOGGER.warn("Folder '" + currentPath + "' not found.");
            return;
        }
        if (depth > 0 && item.isDirectory()) {
            JcrFolder folder = (JcrFolder) item;
            List<JcrFsItem> children = folder.getItems();
            for (JcrFsItem child : children) {
                String newPath = child.getRelativePath();
                recursiveParseProperties(
                        generatedXml, newPath, propertyFindType, properties, depth - 1);
            }
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private JcrFsItem getJcrFsItem(final String path) throws IOException {
        final LocalRepo repo = getLocalRepo();
        String repoKey = request.getRepoKey();
        //Check that we are allowed to read
        if (!repo.allowsDownload(path)) {
            response.sendError(HttpStatus.SC_FORBIDDEN);
            throw new RuntimeException(
                    "Not allowed to read '" + path + "' in repo '" + repoKey + "'.");
        }
        JcrFsItem item = repo.getFsItem(path);
        return item;
    }

    private LocalRepo getLocalRepo() throws IOException {
        String repoKey = request.getRepoKey();
        CentralConfig cc = CentralConfig.get();
        VirtualRepo globalVirtualRepo = cc.getGlobalVirtualRepo();
        final LocalRepo repo = globalVirtualRepo.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            response.sendError(HttpStatus.SC_NOT_FOUND);
            throw new RuntimeException("Could not find repo '" + repoKey + "'.");
        }
        return repo;
    }

    /**
     * Get the ETag associated with a file.
     */
    private static String getEtag(String resourceLength, String lastModified)
            throws IOException {
        return "W/\"" + resourceLength + "-" + lastModified + "\"";
    }

    /**
     * Get creation date in ISO format.
     */
    private static String getIsoCreationDate(long creationDate) {
        StringBuffer creationDateValue =
                new StringBuffer(creationDateFormat.format(new Date(creationDate)));
        return creationDateValue.toString();
    }
}