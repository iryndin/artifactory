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
package org.artifactory.update.jcr;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.common.ArtifactoryConstants;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;

import javax.annotation.PostConstruct;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * User: freds Date: Jun 3, 2008 Time: 1:31:07 PM
 */
public class JcrRepositoryForExport implements JcrSessionProvider {
    private static final Logger LOGGER =
            LogManager.getLogger(JcrRepositoryForExport.class);

    protected static final String ARTIFACTORY_NAMESPACE_PREFIX = "artifactory";
    protected static final String ARTIFACTORY_NAMESPACE = "http://artifactory.jfrog.org/1.0";
    protected static final String OCM_NAMESPACE_PREFIX = "ocm";
    protected static final String OCM_NAMESPACE = "http://jackrabbit.apache.org/ocm";

    private ResourceStreamHandle repoXml;
    private ResourceStreamHandle nodeTypes;
    private Session session;

    public ResourceStreamHandle getRepoXml() {
        return repoXml;
    }

    public void setRepoXml(ResourceStreamHandle repoXml) {
        this.repoXml = repoXml;
    }

    public ResourceStreamHandle getNodeTypes() {
        return nodeTypes;
    }

    public void setNodeTypes(ResourceStreamHandle nodeTypes) {
        this.nodeTypes = nodeTypes;
    }

    public Session getSession() {
        if (session == null) {
            init();
        }
        return session;
    }

    @PostConstruct
    public void init() {
        try {
            JackrabbitRepository repository = createJcrRepository();
            session = repository.login();
            Workspace workspace = session.getWorkspace();
            registerNamespaces(workspace);
            registerTypes(workspace, getArtifactoryNodeTypes());
        } catch (Exception e) {
            LOGGER.error("Cannot initialize JCR repository " + e.getMessage(), e);
            throw new RuntimeException("Cannot initialize JCR repository " + e.getMessage(), e);
        }
    }

    NodeTypeDef[] getArtifactoryNodeTypes() throws IOException, InvalidNodeTypeDefException {
        NodeTypeDef[] types;
        try {
            types = NodeTypeReader.read(nodeTypes.getInputStream());
        } finally {
            nodeTypes.close();
        }
        return types;
    }

    protected void registerNamespaces(Workspace workspace) throws RepositoryException {
        //Register the artifactory ns if it is not registered already
        NamespaceRegistry nsReg = workspace.getNamespaceRegistry();
        List<String> nsPrefixes = Arrays.asList(nsReg.getPrefixes());
        if (!nsPrefixes.contains(ARTIFACTORY_NAMESPACE_PREFIX)) {
            nsReg.registerNamespace(
                    ARTIFACTORY_NAMESPACE_PREFIX,
                    ARTIFACTORY_NAMESPACE);
        }
        if (!nsPrefixes.contains(OCM_NAMESPACE_PREFIX)) {
            nsReg.registerNamespace(OCM_NAMESPACE_PREFIX,
                    OCM_NAMESPACE);
        }
    }

    public JackrabbitRepository createJcrRepository() {
        JackrabbitRepository repository;
        try {
            RepositoryConfig repoConfig = RepositoryConfig.create(
                    repoXml.getInputStream(), ArtifactoryHome.getJcrRootDir().getAbsolutePath());
            if (ArtifactoryConstants.fixConsistency) {
                WorkspaceConfig wsConfig =
                        (WorkspaceConfig) repoConfig.getWorkspaceConfigs().iterator().next();
                PersistenceManagerConfig pmConfig = wsConfig.getPersistenceManagerConfig();
                String className = pmConfig.getClassName();
                Class<?> clazz = getClass().getClassLoader().loadClass(className);
                Object pm = clazz.newInstance();
                if (pm instanceof AbstractBundlePersistenceManager) {
                    pmConfig.getParameters().put("consistencyCheck", "true");
                    pmConfig.getParameters().put("consistencyFix", "true");
                    LOGGER.info("Fix consistency requested on '" + className + "'.");
                } else {
                    LOGGER.warn("Fix consistency requested on a persistence manager that " +
                            "does not support this feature.");
                }
            }
            repository = RepositoryImpl.create(repoConfig);
        } catch (Exception e) {
            throw new RuntimeException("Failed to config jcr repo.", e);
        } finally {
            repoXml.close();
        }
        return repository;
    }

    protected void registerTypes(Workspace workspace, NodeTypeDef[] types)
            throws RepositoryException, InvalidNodeTypeDefException {
        // Do nothing since we don't want to have more types

        //Get the NodeTypeManager from the Workspace.
        //Note that it must be cast from the generic JCR NodeTypeManager to the
        //Jackrabbit-specific implementation.
        NodeTypeManagerImpl ntmgr =
                (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        //Acquire the NodeTypeRegistry
        NodeTypeRegistry ntReg = ntmgr.getNodeTypeRegistry();
        //Create or update (reregister) all NodeTypeDefs
        for (NodeTypeDef ntd : types) {
            Name name = ntd.getName();
            if (!ntReg.isRegistered(name)) {
                ntReg.registerNodeType(ntd);
            } else {
                // Do nothing since we don't want to have more types
            }
        }
    }
}
