/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.repo.webdav;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.webdav.WebdavService;
import org.artifactory.common.StatusHolder;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.mime.MimeType;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
 * User: freds Date: Jul 27, 2008 Time: 9:27:12 PM
 */
@Service
public class WebdavServiceImpl implements WebdavService {
    private static final Logger log = LoggerFactory.getLogger(WebdavServiceImpl.class);

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

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repoService;

    @SuppressWarnings({"OverlyComplexMethod"})
    public void handlePropfind(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        // Retrieve the resources
        RepoPath repoPath = request.getRepoPath();
        String path = repoPath.getPath();
        int depth = INFINITY;
        String depthStr = request.getHeader("Depth");
        if (depthStr != null) {
            if ("0".equals(depthStr)) {
                depth = 0;
            } else if ("1".equals(depthStr)) {
                depth = 1;
            } else if ("infinity".equals(depthStr)) {
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
        generatedXml.writeElement(null, "multistatus xmlns=\"" + DEFAULT_NAMESPACE + "\"", XmlWriter.OPENING);
        if (depth > 0) {
            recursiveParseProperties(
                    request, response, generatedXml, path, propertyFindType, properties, depth);
        } else {
            parseProperties(request, response, generatedXml, path, propertyFindType, properties);
        }
        generatedXml.writeElement(null, "multistatus", XmlWriter.CLOSING);
        generatedXml.sendData();
        response.flush();
    }

    public void handleMkcol(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        final LocalRepo repo = getLocalRepo(request, response);
        //Return 405 if folder exists
        String path = repoPath.getPath();
        if (repo.itemExists(path)) {
            response.setStatus(HttpStatus.SC_METHOD_NOT_ALLOWED);
            return;
        }
        //Check that we are allowed to write
        try {
            repoService.assertValidDeployPath(repo, path);
        } catch (RepoRejectException rre) {
            response.sendError(rre.getErrorCode(), rre.getMessage(), log);
            return;
        }
        JcrFolder targetFolder = repo.getLockedJcrFolder(repoPath, true);
        targetFolder.mkdirs();
        //Return 201 when an element is created
        response.setStatus(HttpStatus.SC_CREATED);
    }

    public void handleDelete(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        String repoKey = repoPath.getRepoKey();
        LocalRepo localRepository = repoService.localOrCachedRepositoryByKey(repoKey);
        if (localRepository == null) {
            if (repoService.virtualRepoDescriptorByKey(repoKey) != null) {
                response.setStatus(HttpStatus.SC_METHOD_NOT_ALLOWED);
                response.setHeader("Allow", "GET");
                return;
            }
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }
        if (request.isMetadata()) {
            String metadataName = NamingUtils.getMetadataName(repoPath.getPath());
            MetadataInfo metadataInfo = repoService.getMetadataInfo(repoPath, metadataName);
            if (metadataInfo == null) {
                response.setStatus(HttpStatus.SC_NOT_FOUND);
                return;
            }
        }
        StatusHolder statusHolder = repoService.undeploy(repoPath);
        if (statusHolder.isError()) {
            response.sendError(statusHolder);
            return;
        }
        response.setStatus(HttpStatus.SC_NO_CONTENT);
    }

    public void handleOptions(ArtifactoryResponse response) throws IOException {
        response.setHeader("DAV", "1,2");
        response.setHeader("Allow", WEBDAV_METHODS_LIST);
        //response.setHeader("Allow", "OPTIONS, MKCOL, PUT, GET, HEAD, POST, DELETE, PROPFIND, MOVE");
        response.setHeader("MS-Author-Via", "DAV");
        response.sendSuccess();
    }

    public void handlePost(ArtifactoryRequest request, ArtifactoryResponse response) {
        RepoPath repoPath = request.getRepoPath();
        String repoKey = repoPath.getRepoKey();
        VirtualRepoDescriptor virtualRepo = repoService.virtualRepoDescriptorByKey(repoKey);

        StringBuilder allowHeaderBuilder = new StringBuilder();
        allowHeaderBuilder.append("GET");

        if (virtualRepo == null) {
            allowHeaderBuilder.append(",PUT,DELETE");
        }
        response.setHeader("Allow", allowHeaderBuilder.toString());
        response.setStatus(HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

    public void handleMove(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        if (StringUtils.isEmpty(repoPath.getPath())) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "Cannot perform MOVE action on a repository. " +
                    "Please specify a valid path", log);
            return;
        }
        String targetRepoKey = request.getParameter("targetRepoKey");
        if (StringUtils.isEmpty(targetRepoKey)) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "Parameter 'targetRepoKey' is required.", log);
            return;
        }
        LocalRepo targetRepo = repoService.localRepositoryByKey(targetRepoKey);
        if (targetRepo == null) {
            response.sendError(HttpStatus.SC_NOT_FOUND, "Target local repository not found.", log);
            return;
        }
        if (!authService.canDelete(repoPath) || !authService.canDeploy(new RepoPathImpl(targetRepoKey, ""))) {
            response.sendError(HttpStatus.SC_UNAUTHORIZED, "Insufficient permissions.", log);
            return;
        }

        MoveMultiStatusHolder status = repoService.move(repoPath, targetRepoKey, false);
        if (!status.hasWarnings() && !status.hasErrors()) {
            response.sendSuccess();
        } else {
            response.sendError(status);
        }
    }

    /**
     * resourcetype Return JAXP document builder instance.
     *
     * @return
     * @throws javax.servlet.ServletException
     */
    private DocumentBuilder getDocumentBuilder() throws IOException {
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("JAXP document builder creation failed");
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
     * @param propertiesList<String> If the propfind type is find properties by name, then this List<String> contains
     *                               those properties
     */
    @SuppressWarnings({"OverlyComplexMethod"})
    private void parseProperties(ArtifactoryRequest request, ArtifactoryResponse response, XmlWriter generatedXml,
                                 final String path, int type, List<String> propertiesList) throws IOException {
        JcrFsItem item = getJcrFsItem(request, response, path);
        if (item == null) {
            log.warn("Item '" + path + "' not found.");
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

                    MimeType ct = NamingUtils.getMimeType(path);
                    if (ct != null) {
                        generatedXml.writeProperty(null, "getcontenttype", ct.getType());
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
                //noinspection MismatchedQueryAndUpdateOfCollection
                List<String> propertiesNotFound = new ArrayList<String>();
                // Parse the list of properties
                generatedXml.writeElement(null, "propstat", XmlWriter.OPENING);
                generatedXml.writeElement(null, "prop", XmlWriter.OPENING);
                for (String property : propertiesList) {
                    if ("creationdate".equals(property)) {
                        generatedXml.writeProperty(null, "creationdate", creationDate);
                    } else if ("displayname".equals(property)) {
                        generatedXml.writeElement(null, "displayname", XmlWriter.OPENING);
                        generatedXml.writeData(resourceName);
                        generatedXml.writeElement(null, "displayname", XmlWriter.CLOSING);
                    } else if ("getcontentlanguage".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeElement(
                                    null, "getcontentlanguage", XmlWriter.NO_CONTENT);
                        }
                    } else if ("getcontentlength".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getcontentlength", resourceLength);
                        }
                    } else if ("getcontenttype".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getcontenttype",
                                    NamingUtils.getMimeTypeByPathAsString(path));
                        }
                    } else if ("getetag".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getetag",
                                    getEtag(resourceLength, lastModified));
                        }
                    } else if ("getlastmodified".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            generatedXml.writeProperty(null, "getlastmodified", lastModified);
                        }
                    } else if ("source".equals(property)) {
                        generatedXml.writeProperty(null, "source", "");
                    } else {
                        propertiesNotFound.add(property);
                    }
                }
                //Always include resource type
                if (isFolder) {
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.OPENING);
                    generatedXml.writeElement(null, "collection", XmlWriter.NO_CONTENT);
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.CLOSING);
                } else {
                    generatedXml.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                }

                generatedXml.writeElement(null, "prop", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "status", XmlWriter.OPENING);
                generatedXml.writeText(status);
                generatedXml.writeElement(null, "status", XmlWriter.CLOSING);
                generatedXml.writeElement(null, "propstat", XmlWriter.CLOSING);

                // TODO: [by fsi] Find out what this is for?
                /*
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
                */
                break;

        }
        generatedXml.writeElement(null, "response", XmlWriter.CLOSING);
    }

    /**
     * goes recursive through all folders. used by propfind
     */
    private void recursiveParseProperties(ArtifactoryRequest request, ArtifactoryResponse response,
                                          XmlWriter generatedXml, String currentPath, int propertyFindType, List<String> properties, int depth)
            throws IOException {
        parseProperties(request, response, generatedXml, currentPath, propertyFindType, properties);
        JcrFsItem item = getJcrFsItem(request, response, currentPath);
        if (item == null) {
            log.warn("Folder '" + currentPath + "' not found.");
            return;
        }
        if (depth > 0 && item.isDirectory()) {
            JcrFolder folder = (JcrFolder) item;
            List<JcrFsItem> children = folder.getItems();
            for (JcrFsItem child : children) {
                String newPath = child.getRelativePath();
                recursiveParseProperties(
                        request, response, generatedXml, newPath,
                        propertyFindType, properties, depth - 1);
            }
        }
    }

    private JcrFsItem getJcrFsItem(ArtifactoryRequest request, ArtifactoryResponse response, String path)
            throws IOException {
        final StoringRepo repo = getLocalRepo(request, response);
        String repoKey = request.getRepoKey();
        RepoPath repoPath = new RepoPathImpl(repoKey, path);
        if (repo.isReal()) {
            StatusHolder status = ((RealRepo) repo).checkDownloadIsAllowed(repoPath);
            //Check that we are allowed to read
            if (status.isError()) {
                String msg = status.getStatusMsg();
                response.sendError(status.getStatusCode(), msg, log);
                throw new RuntimeException(msg);
            }
        }
        JcrFsItem item = repo.getJcrFsItem(repoPath);
        return item;
    }

    private LocalRepo getLocalRepo(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        String repoKey = request.getRepoKey();
        final LocalRepo repo = repoService.localRepositoryByKey(repoKey);
        if (repo == null) {
            String msg = "Could not find repo '" + repoKey + "'.";
            response.sendError(HttpStatus.SC_NOT_FOUND, msg, log);
            throw new RuntimeException(msg);
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
    private static synchronized String getIsoCreationDate(long creationDate) {
        StringBuffer creationDateValue = new StringBuffer(creationDateFormat.format(new Date(creationDate)));
        return creationDateValue.toString();
    }

}
