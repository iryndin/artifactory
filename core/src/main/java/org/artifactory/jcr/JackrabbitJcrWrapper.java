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
package org.artifactory.jcr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFilter;
import org.apache.commons.pool.impl.StackObjectPool;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.common.ExceptionUtils;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.config.CentralConfig;
import org.artifactory.io.ClasspathResourceLoader;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.ContextHelper;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Service
public class JackrabbitJcrWrapper implements InitializingBean, DisposableBean, JcrWrapper {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JackrabbitJcrWrapper.class);

    private InitStrategyFactory initStrategyFactory;
    private ResourceStreamHandle indexConfig;
    private ResourceStreamHandle nodeTypes;
    private ResourceStreamHandle repoXml;
    private List<String> ocmClassesList;

    private StackObjectPool sessionPool;
    private Mapper ocmMapper;
    private boolean initialized;
    private boolean readOnly;
    private boolean createSessionIfNeeded;

    public JackrabbitJcrWrapper() {
    }

    public void setInitStrategyFactory(InitStrategyFactory initStrategyFactory) {
        this.initStrategyFactory = initStrategyFactory;
    }

    public void setIndexConfig(ClasspathResourceLoader indexConfig) {
        this.indexConfig = indexConfig;
    }

    public void setNodeTypes(ResourceStreamHandle nodeTypes) {
        this.nodeTypes = nodeTypes;
    }

    public void setRepoXml(ResourceStreamHandle repoXml) {
        this.repoXml = repoXml;
    }

    public List<String> getOcmClassesList() {
        return ocmClassesList;
    }

    public void setOcmClassesList(List<String> ocmClassesList) {
        this.ocmClassesList = ocmClassesList;
    }

    public void setOcmMapper(Mapper ocmMapper) {
        this.ocmMapper = ocmMapper;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isCreateSessionIfNeeded() {
        return createSessionIfNeeded;
    }

    public void setCreateSessionIfNeeded(boolean createSessionIfNeeded) {
        this.createSessionIfNeeded = createSessionIfNeeded;
    }

    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() {
        //Can be initialized only once
        //Because it cannot be shutdown nicely and restarted
        if (initialized) {
            return;
        }
        initialized = true;
        createSessionIfNeeded = true;
        cleanupAfterLastShutdown();
        System.setProperty("derby.stream.error.file",
                new File(ArtifactoryHome.getLogDir(), "derby.log").getAbsolutePath());

        //Copy the indexes config
        try {
            File indexDir = new File(ArtifactoryHome.getJcrRootDir(), "index");
            FileUtils.forceMkdir(indexDir);
            File indexConfigFile = new File(indexDir, "index_config.xml");
            FileOutputStream fos = new FileOutputStream(indexConfigFile);
            IOUtils.copy(indexConfig.getInputStream(), fos);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to config jcr repo.", e);
        } finally {
            if (indexConfig != null) {
                indexConfig.close();
            }
        }

        //Get the initializer strategy
        InitJcrRepoStrategy strategy = initStrategyFactory.getInitJcrRepoStrategy(this);

        //Create the repository
        final JackrabbitRepository repository;
        repository = strategy.createJcrRepository(repoXml);

        //Create the session pool
        sessionPool = new StackObjectPool(new JcrSessionWrapperFactory(repository), 10, 10) {
            @Override
            public void close() throws Exception {
                super.close();
                repository.shutdown();
            }
        };

        //Initialize JCR Repository
        doInSession(strategy);
    }

    public void destroy() {
        try {
            sessionPool.close();
        } catch (Exception e) {
            LOGGER.warn("Failed to close session pool.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ObjectContentManager getOcm() {
        if (ocmMapper == null) {
            return null;
        }
        ObjectContentManager ocm = doInSession(new JcrCallback<ObjectContentManager>() {
            public ObjectContentManager doInJcr(JcrSessionWrapper session)
                    throws RepositoryException {
                return new ObjectContentManagerImpl(session, ocmMapper);
            }
        });
        return ocm;
    }

    public NodeTypeDef[] getArtifactoryNodeTypes() throws IOException, InvalidNodeTypeDefException {
        NodeTypeDef[] types;
        try {
            types = NodeTypeReader.read(nodeTypes.getInputStream());
        } finally {
            nodeTypes.close();
        }
        return types;
    }

    public <T> T doInSession(JcrCallback<T> callback) {
        T result;
        //If there is already an existing thread-bound session, use it
        boolean createdSession = false;
        JcrSessionWrapper wrapper = null;
        try {
            wrapper = JcrSessionThreadBinder.getSession();
            if (wrapper == null) {
                if (!createSessionIfNeeded) {
                    throw new IllegalStateException(
                            "Outside a jcr transaction and createSessionIfNeeded is off.");
                }
                createdSession = true;
                wrapper = newSession();
                JcrSessionThreadBinder.bind(wrapper);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created session: " + wrapper.getSession() + ".");
                }
            }
            wrapper.setReadOnly(readOnly);
            result = callback.doInJcr(wrapper);
            //Save session automatically if created, not read only, not rollback only and
            //session is alive
            if (createdSession) {
                wrapper.save();
            }
            return result;
        } catch (Throwable t) {
            if (wrapper != null) {
                wrapper.setRollbackOnly();
            }
            throw new RuntimeException("Failed to execute JcrCallback.", t);
        } finally {
            if (createdSession) {
                try {
                    if (wrapper != null) {
                        // TODO: Check if we can release on save?
                        // If release failed session is failed we should not return it to the pool
                        if (wrapper.release()) {
                            sessionPool.returnObject(wrapper);
                        } else {
                            sessionPool.invalidateObject(wrapper);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to return jcr session to pool.", e);
                } finally {
                    JcrSessionThreadBinder.unbind();
                }
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public boolean itemNodeExists(final String absPath) {
        return doInSession(new JcrCallback<Boolean>() {
            public Boolean doInJcr(JcrSessionWrapper session) throws RepositoryException {
                //Sanity check to avoid exception in "session.itemExists()"
                if (StringUtils.isEmpty(absPath) || !absPath.startsWith("/")) {
                    return false;
                }
                boolean exists;
                try {
                    exists = session.itemExists(absPath);
                } catch (RepositoryException e) {
                    Throwable cause =
                            ExceptionUtils.getCauseOfTypes(e, MalformedPathException.class);
                    if (cause != null) {
                        return false;
                    } else {
                        throw e;
                    }
                }
                return exists;
            }
        });
    }


    @SuppressWarnings({"OverlyComplexMethod"})
    public JcrFile importStream(
            JcrFolder parentFolder, String name, String repoKey, long lastModified, InputStream in)
            throws RepositoryException {
        if (isReadOnly()) {
            throw new RepositoryException("Cannot import stream named " + name +
                    " with a read only session!");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Importing '" + parentFolder.getPath() + "/" + name + "'.");
        }
        try {
            //First check that the repository approves the deployment
            String targetPath = parentFolder.getPath() + "/" + name;
            int relPathStart = targetPath.indexOf(repoKey + "/");
            String relPath = targetPath.substring(relPathStart + repoKey.length() + 1);
            CentralConfig cc = ContextHelper.get().getCentralConfig();
            VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
            LocalRepo repo = virtualRepo.localOrCachedRepositoryByKey(repoKey);
            if (repo == null) {
                throw new RepositoryException(
                        "The repository '" + repoKey + "' is not configured." +
                                "Some artifacts might have been incorrectly imported - " +
                                "please remove them manually.");
            }
            if (!repo.handles(relPath)) {
                throw new RepositoryException(
                        "The repository '" + repoKey + "' rejected the artifact '" + relPath +
                                "' due to its snapshot/release handling policy. " +
                                "Some artifacts might have been incorrectly imported - " +
                                "please remove them manually.");
            }
            if (!repo.accepts(relPath)) {
                throw new RepositoryException(
                        "The repository '" + repoKey + "' rejected the artifact '" + relPath +
                                "' due to its include/exclude patterns settings. Some artifacts " +
                                "might have been incorrectly imported - please remove them " +
                                "manually.");
            }

            JcrFile file = JcrFile.create(parentFolder, name, lastModified, in);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Imported '" + targetPath + "'.");
            }
            AccessLogger.deployed(file.getRepoPath());
            return file;
        } catch (Exception e) {
            throw new RepositoryException("Failed to import stream.", e);
        }
    }

    public Node getOrCreateUnstructuredNode(final String absPath) {
        return doInSession(new JcrCallback<Node>() {
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                Node root = session.getRootNode();
                String rootRelPath = absPath.startsWith("/") ? absPath.substring(1) : absPath;
                if (!root.hasNode(rootRelPath)) {
                    if (session.isReadOnly()) {
                        throw new RepositoryException("Cannot create root path " + rootRelPath +
                                " with a read only session!");
                    }
                    Node node = root.addNode(rootRelPath, "nt:unstructured");
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Created node: " + absPath + ".");
                    }
                    return node;
                } else {
                    return root.getNode(rootRelPath);
                }
            }
        });
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private JcrSessionWrapper newSession() {
        try {
            JcrSessionWrapper sessionWrapper = (JcrSessionWrapper) sessionPool.borrowObject();
            return sessionWrapper;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create jcr session.", e);
        }
    }

    /**
     * Dumps the contents of the given node with its children to standard output. INTERNAL for
     * debugging only.
     *
     * @param node the node to be dumped
     * @throws RepositoryException on repository errors
     */
    public static void dump(Node node) throws RepositoryException {
        System.out.println(node.getPath());
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            System.out.print(property.getPath() + "=");
            if (property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        System.out.println(",");
                    }
                    System.out.println(values[i].getString());
                }
            } else {
                System.out.print(property.getString());
            }
            System.out.println();
        }
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            dump(child);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static void cleanupAfterLastShutdown() {
        //Cleanup any left over bin*.tmp files from the temp directory, due to unclean shutdown
        String tmpDirPath;
        try {
            File tmp = File.createTempFile("bin", ".tmp");
            tmpDirPath = tmp.getParent();
        } catch (IOException e) {
            tmpDirPath = System.getProperty("java.io.tmpdir");
        }
        if (tmpDirPath != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cleanning up temp files in '" + tmpDirPath + "'.");
            }
            File tmpDir = new File(tmpDirPath);
            Collection<File> tmpfiles =
                    FileUtils.listFiles(tmpDir, new WildcardFilter("bin*.tmp"), null);
            for (File tmpfile : tmpfiles) {
                tmpfile.delete();
            }
        } else {
            LOGGER.warn("Not cleanning up any temp files: failed to determine temp dir.");
        }
    }

}
