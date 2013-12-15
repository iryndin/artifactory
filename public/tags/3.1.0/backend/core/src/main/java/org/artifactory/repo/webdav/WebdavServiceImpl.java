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

package org.artifactory.repo.webdav;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.repo.BaseBrowsableItem;
import org.artifactory.api.repo.BrowsableItem;
import org.artifactory.api.repo.BrowsableItemCriteria;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.webdav.WebdavService;
import org.artifactory.common.StatusHolder;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.mime.MimeType;
import org.artifactory.mime.NamingUtils;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.local.ValidDeployPathContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Service class to handle webdav protocol.<p/> Webdav RFCc at: <a href="http://www.ietf.org/rfc/rfc2518.txt">rfc2518</a>,
 * <a href="http://www.ietf.org/rfc/rfc4918.txt">rfc4918</a>.
 *
 * @author Yossi Shaul
 */
@Service
public class WebdavServiceImpl implements WebdavService {
    private static final Logger log = LoggerFactory.getLogger(WebdavServiceImpl.class);

    /**
     * Default depth is infinity. And it is limited no purpose to 3 level deep.
     */
    private static final int INFINITY = 3;

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

    @Autowired
    private RepositoryBrowsingService repoBrowsing;

    @Override
    @SuppressWarnings({"OverlyComplexMethod"})
    public void handlePropfind(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        // Retrieve the resources
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
        if (request.getContentLength() > 0) {
            DocumentBuilder documentBuilder = getDocumentBuilder();
            try {
                Document document = documentBuilder.parse(
                        new InputSource(request.getInputStream()));
                logWebdavRequest(document);
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
        Writer writer = response.getWriter();
        if (log.isDebugEnabled()) {
            writer = new StringWriter(); // write to memory so we'll be able to log the result as string
        }
        XmlWriter generatedXml = new XmlWriter(writer);
        generatedXml.writeXMLHeader();
        generatedXml.writeElement(null, "multistatus xmlns=\"" + DEFAULT_NAMESPACE + "\"", XmlWriter.OPENING);

        RepoPath repoPath = request.getRepoPath();
        BrowsableItem rootItem = null;
        if (repoService.exists(repoPath)) {
            rootItem = repoBrowsing.getLocalRepoBrowsableItem(repoPath);
        }
        if (rootItem != null) {
            recursiveParseProperties(request, response, generatedXml, rootItem, propertyFindType, properties,
                    depth);
        } else {
            log.warn("Item '" + request.getRepoPath() + "' not found.");
        }
        generatedXml.writeElement(null, "multistatus", XmlWriter.CLOSING);
        generatedXml.sendData();
        if (log.isDebugEnabled()) {
            log.debug("Webdav response:\n" + writer.toString());
            //response.setContentLength(writer.toString().getBytes().length);
            response.getWriter().append(writer.toString());
        }
        response.flush();
    }

    @Override
    public void handleMkcol(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        String repoKey = request.getRepoKey();
        LocalRepo repo = repoService.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            response.sendError(HttpStatus.SC_NOT_FOUND, "Could not find repo '" + repoKey + "'.", log);
            return;
        }

        //Return 405 if called on root or the folder already exists
        String path = repoPath.getPath();
        if (StringUtils.isBlank(path) || repo.itemExists(path)) {
            response.sendError(HttpStatus.SC_METHOD_NOT_ALLOWED,
                    "MKCOL can only be executed on non-existent resource: " + repoPath, log);
            return;
        }
        //Check that we are allowed to write
        try {
            // Servlet container doesn't support long values so we take it manually from the header
            String contentLengthHeader = request.getHeader("Content-Length");
            long contentLength = StringUtils.isBlank(contentLengthHeader) ? -1 : Long.parseLong(contentLengthHeader);
            repoService.assertValidDeployPath(
                    new ValidDeployPathContext.Builder(repo, repoPath).contentLength(contentLength).build());
        } catch (RepoRejectException rre) {
            response.sendError(rre.getErrorCode(), rre.getMessage(), log);
            return;
        }

        // make sure the parent exists
        VfsFolder parentFolder = repo.getMutableFolder(repoPath.getParent());
        if (parentFolder == null) {
            response.sendError(HttpStatus.SC_CONFLICT,
                    "Directory cannot be created: parent doesn't exist: " + repoPath.getParent(), log);
            return;
        }

        repo.createOrGetFolder(repoPath);
        response.setStatus(HttpStatus.SC_CREATED);
    }

    @Override
    public void handleDelete(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        String repoKey = repoPath.getRepoKey();
        LocalRepo localRepository = repoService.localOrCachedRepositoryByKey(repoKey);
        if (localRepository == null) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }

        if (!NamingUtils.isProperties(repoPath.getPath())) {
            deleteItem(response, repoPath);
        } else {
            deleteProperties(response, repoPath);
        }
    }

    private void deleteItem(ArtifactoryResponse response, RepoPath repoPath) throws IOException {
        StatusHolder statusHolder = repoService.undeploy(repoPath);
        if (statusHolder.isError()) {
            response.sendError(statusHolder);
        } else {
            response.setStatus(HttpStatus.SC_NO_CONTENT);
        }
    }

    private void deleteProperties(ArtifactoryResponse response, RepoPath repoPath) throws IOException {
        RepoPathImpl itemRepoPath = new RepoPathImpl(repoPath.getRepoKey(),
                NamingUtils.stripMetadataFromPath(repoPath.getPath()));
        boolean removed = repoService.removeProperties(itemRepoPath);
        if (removed) {
            response.setStatus(HttpStatus.SC_NO_CONTENT);
        } else {
            response.sendError(HttpStatus.SC_NOT_FOUND, "Failed to remove properties from " + itemRepoPath, log);
        }
    }

    @Override
    public void handleOptions(ArtifactoryResponse response) throws IOException {
        response.setHeader("DAV", "1,2");
        response.setHeader("Allow", WEBDAV_METHODS_LIST);
        response.setHeader("MS-Author-Via", "DAV");
        response.sendSuccess();
    }

    @Override
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

    @Override
    public void handleMove(ArtifactoryRequest request, ArtifactoryResponse response) throws IOException {
        RepoPath repoPath = request.getRepoPath();
        if (StringUtils.isEmpty(repoPath.getPath())) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "Cannot perform MOVE action on a repository. " +
                    "Please specify a valid path", log);
            return;
        }

        String destination = URLDecoder.decode(request.getHeader("Destination"), "UTF-8");
        if (StringUtils.isEmpty(destination)) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "Header 'Destination' is required.", log);
            return;
        }

        String targetPathWithoutContextUrl = StringUtils.remove(destination, request.getServletContextUrl());
        String targetPathParent = PathUtils.getParent(targetPathWithoutContextUrl);
        RepoPath targetPath = InternalRepoPathFactory.create(targetPathParent);
        if (!authService.canDelete(repoPath) || !authService.canDeploy(targetPath)) {
            response.sendError(HttpStatus.SC_FORBIDDEN, "Insufficient permissions.", log);
            return;
        }

        MoveMultiStatusHolder status = repoService.move(repoPath, targetPath, false, true, true);
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
        properties = new ArrayList<>();
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

    @SuppressWarnings({"OverlyComplexMethod"})
    private void parseProperties(ArtifactoryRequest request, XmlWriter xmlResponse,
            BaseBrowsableItem item, int type, List<String> propertiesList) throws IOException {
        RepoPath repoPath = item.getRepoPath();
        String creationDate = getIsoDate(item.getCreated());
        boolean isFolder = item.isFolder();
        String lastModified = getIsoDate(item.getLastModified());
        String resourceLength = item.getSize() + "";

        xmlResponse.writeElement(null, "response", XmlWriter.OPENING);
        String status = "HTTP/1.1 " + WebdavStatus.SC_OK + " " +
                WebdavStatus.getStatusText(WebdavStatus.SC_OK);

        //Generating href element
        xmlResponse.writeElement(null, "href", XmlWriter.OPENING);
        String origPath = request.getPath();
        String uri = request.getUri();
        String hrefBase = uri;
        origPath = HttpUtils.encodeQuery(origPath);
        if (origPath.length() > 0) {
            int idx = uri.lastIndexOf(origPath);
            if (idx > 0) {
                //When called recursively avoid concatenating the original path on top of itself
                hrefBase = uri.substring(0, idx);
            }
        }
        String path = repoPath.getPath();
        if (StringUtils.isNotBlank(path) && !hrefBase.endsWith("/")) {
            hrefBase += "/";
        }

        // Encode only the path since the base is already encoded
        String href = hrefBase + HttpUtils.encodeQuery(path);

        xmlResponse.writeText(href);
        xmlResponse.writeElement(null, "href", XmlWriter.CLOSING);

        String resourceName = path;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1) {
            resourceName = resourceName.substring(lastSlash + 1);
        }

        switch (type) {
            case FIND_ALL_PROP:
                xmlResponse.writeElement(null, "propstat", XmlWriter.OPENING);
                xmlResponse.writeElement(null, "prop", XmlWriter.OPENING);

                xmlResponse.writeProperty(null, "creationdate", creationDate);
                xmlResponse.writeElement(null, "displayname", XmlWriter.OPENING);
                xmlResponse.writeData(resourceName);
                xmlResponse.writeElement(null, "displayname", XmlWriter.CLOSING);
                if (!isFolder) {
                    xmlResponse.writeProperty(null, "getlastmodified", lastModified);
                    xmlResponse.writeProperty(null, "getcontentlength", resourceLength);

                    MimeType ct = NamingUtils.getMimeType(path);
                    if (ct != null) {
                        xmlResponse.writeProperty(null, "getcontenttype", ct.getType());
                    }
                    xmlResponse.writeProperty(
                            null, "getetag", getEtag(resourceLength, lastModified));
                    xmlResponse.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                } else {
                    xmlResponse.writeElement(null, "resourcetype", XmlWriter.OPENING);
                    xmlResponse.writeElement(null, "collection", XmlWriter.NO_CONTENT);
                    xmlResponse.writeElement(null, "resourcetype", XmlWriter.CLOSING);
                }
                xmlResponse.writeProperty(null, "source", "");
                xmlResponse.writeElement(null, "prop", XmlWriter.CLOSING);
                xmlResponse.writeElement(null, "status", XmlWriter.OPENING);
                xmlResponse.writeText(status);
                xmlResponse.writeElement(null, "status", XmlWriter.CLOSING);
                xmlResponse.writeElement(null, "propstat", XmlWriter.CLOSING);
                break;
            case FIND_PROPERTY_NAMES:
                xmlResponse.writeElement(null, "propstat", XmlWriter.OPENING);
                xmlResponse.writeElement(null, "prop", XmlWriter.OPENING);
                xmlResponse.writeElement(null, "creationdate", XmlWriter.NO_CONTENT);
                xmlResponse.writeElement(null, "displayname", XmlWriter.NO_CONTENT);
                if (!isFolder) {
                    xmlResponse.writeElement(null, "getcontentlanguage", XmlWriter.NO_CONTENT);
                    xmlResponse.writeElement(null, "getcontentlength", XmlWriter.NO_CONTENT);
                    xmlResponse.writeElement(null, "getcontenttype", XmlWriter.NO_CONTENT);
                    xmlResponse.writeElement(null, "getetag", XmlWriter.NO_CONTENT);
                    xmlResponse.writeElement(null, "getlastmodified", XmlWriter.NO_CONTENT);
                }
                xmlResponse.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                xmlResponse.writeElement(null, "source", XmlWriter.NO_CONTENT);
                xmlResponse.writeElement(null, "lockdiscovery", XmlWriter.NO_CONTENT);
                xmlResponse.writeElement(null, "prop", XmlWriter.CLOSING);
                xmlResponse.writeElement(null, "status", XmlWriter.OPENING);
                xmlResponse.writeText(status);
                xmlResponse.writeElement(null, "status", XmlWriter.CLOSING);
                xmlResponse.writeElement(null, "propstat", XmlWriter.CLOSING);
                break;
            case FIND_BY_PROPERTY:
                //noinspection MismatchedQueryAndUpdateOfCollection
                List<String> propertiesNotFound = new ArrayList<>();
                // Parse the list of properties
                xmlResponse.writeElement(null, "propstat", XmlWriter.OPENING);
                xmlResponse.writeElement(null, "prop", XmlWriter.OPENING);
                for (String property : propertiesList) {
                    if ("creationdate".equals(property)) {
                        xmlResponse.writeProperty(null, "creationdate", creationDate);
                    } else if ("displayname".equals(property)) {
                        xmlResponse.writeElement(null, "displayname", XmlWriter.OPENING);
                        xmlResponse.writeData(resourceName);
                        xmlResponse.writeElement(null, "displayname", XmlWriter.CLOSING);
                    } else if ("getcontentlanguage".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            xmlResponse.writeElement(
                                    null, "getcontentlanguage", XmlWriter.NO_CONTENT);
                        }
                    } else if ("getcontentlength".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            xmlResponse.writeProperty(null, "getcontentlength", resourceLength);
                        }
                    } else if ("getcontenttype".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            xmlResponse.writeProperty(null, "getcontenttype",
                                    NamingUtils.getMimeTypeByPathAsString(path));
                        }
                    } else if ("getetag".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            xmlResponse.writeProperty(null, "getetag",
                                    getEtag(resourceLength, lastModified));
                        }
                    } else if ("getlastmodified".equals(property)) {
                        if (isFolder) {
                            propertiesNotFound.add(property);
                        } else {
                            xmlResponse.writeProperty(null, "getlastmodified", lastModified);
                        }
                    } else if ("source".equals(property)) {
                        xmlResponse.writeProperty(null, "source", "");
                    } else {
                        propertiesNotFound.add(property);
                    }
                }
                //Always include resource type
                if (isFolder) {
                    xmlResponse.writeElement(null, "resourcetype", XmlWriter.OPENING);
                    xmlResponse.writeElement(null, "collection", XmlWriter.NO_CONTENT);
                    xmlResponse.writeElement(null, "resourcetype", XmlWriter.CLOSING);
                } else {
                    xmlResponse.writeElement(null, "resourcetype", XmlWriter.NO_CONTENT);
                }

                xmlResponse.writeElement(null, "prop", XmlWriter.CLOSING);
                xmlResponse.writeElement(null, "status", XmlWriter.OPENING);
                xmlResponse.writeText(status);
                xmlResponse.writeElement(null, "status", XmlWriter.CLOSING);
                xmlResponse.writeElement(null, "propstat", XmlWriter.CLOSING);

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
        xmlResponse.writeElement(null, "response", XmlWriter.CLOSING);
    }

    /**
     * goes recursive through all folders. used by propfind
     */
    private void recursiveParseProperties(ArtifactoryRequest request, ArtifactoryResponse response,
            XmlWriter generatedXml, BaseBrowsableItem currentItem, int propertyFindType, List<String> properties,
            int depth)
            throws IOException {

        parseProperties(request, generatedXml, currentItem, propertyFindType, properties);

        if (depth <= 0) {
            return;
        }

        if (currentItem.isFolder()) {
            BrowsableItemCriteria criteria = new BrowsableItemCriteria.Builder(currentItem.getRepoPath()).build();
            List<BaseBrowsableItem> browsableChildren = repoBrowsing.getLocalRepoBrowsableChildren(criteria);
            for (BaseBrowsableItem child : browsableChildren) {
                recursiveParseProperties(request, response, generatedXml, child,
                        propertyFindType, properties, depth - 1);
            }
        }
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
    private static synchronized String getIsoDate(long creationDate) {
        return creationDateFormat.format(new Date(creationDate));
    }

    private void logWebdavRequest(Document document) throws TransformerException {
        if (log.isDebugEnabled()) {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            log.debug("Webdav request body:\n" + writer.toString());
        }
    }
}
