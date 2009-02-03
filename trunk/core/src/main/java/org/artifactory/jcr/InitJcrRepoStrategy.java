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

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.spi.Name;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.common.ResourceStreamHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: freds Date: Jun 3, 2008 Time: 12:14:52 PM
 */
public class InitJcrRepoStrategy {
    private static final Logger log = LoggerFactory.getLogger(InitJcrRepoStrategy.class);

    private final JcrServiceImpl jcrService;

    protected static final String ARTIFACTORY_NAMESPACE_PREFIX = "artifactory";
    protected static final String ARTIFACTORY_NAMESPACE = "http://artifactory.jfrog.org/1.0";
    protected static final String OCM_NAMESPACE_PREFIX = "ocm";
    protected static final String OCM_NAMESPACE = "http://jackrabbit.apache.org/ocm";

    public InitJcrRepoStrategy(JcrService jcrService) {
        this.jcrService = (JcrServiceImpl) jcrService;
    }

    public JackrabbitRepository createJcrRepository(ResourceStreamHandle repoXml) {
        JackrabbitRepository repository;
        try {
            RepositoryConfig repoConfig = RepositoryConfig.create(
                    repoXml.getInputStream(), ArtifactoryHome.getJcrRootDir().getAbsolutePath());
            if (ConstantsValue.jcrFixConsistency.getBoolean()) {
                WorkspaceConfig wsConfig =
                        (WorkspaceConfig) repoConfig.getWorkspaceConfigs().iterator().next();
                PersistenceManagerConfig pmConfig = wsConfig.getPersistenceManagerConfig();
                String className = pmConfig.getClassName();
                Class<?> clazz = getClass().getClassLoader().loadClass(className);
                Object pm = clazz.newInstance();
                if (pm instanceof AbstractBundlePersistenceManager) {
                    pmConfig.getParameters().put("consistencyCheck", "true");
                    pmConfig.getParameters().put("consistencyFix", "true");
                    log.info("Fix consistency requested on '" + className + "'.");
                } else {
                    log.warn("Fix consistency requested on a persistence manager that " +
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

    public void init() {
        try {
            JcrSession session = jcrService.getManagedSession();
            Workspace workspace = session.getWorkspace();
            registerNamespaces(workspace);
            registerTypes(workspace, jcrService.getArtifactoryNodeTypes());
            initializeOcm();
            initializeRepoRoot();
        } catch (Exception e) {
            log.error("Cannot initialize JCR repository " + e.getMessage(), e);
            throw new RuntimeException("Cannot initialize JCR repository " + e.getMessage(), e);
        }
    }

    protected void initializeRepoRoot() {
        jcrService.getOrCreateUnstructuredNode(JcrPath.get().getRepoJcrRootPath());
    }

    protected void initializeOcm() throws ClassNotFoundException {
        if (jcrService.OCM_CLASSES != null) {
            //Create the ocm configuration root
            jcrService.getOrCreateUnstructuredNode(JcrPath.get().getOcmJcrRootPath());
            //Register the ocm classes
            List<Class> ocmClasses = new ArrayList<Class>();
            for (String className : jcrService.OCM_CLASSES) {
                ocmClasses.add(getClass().getClassLoader().loadClass(className));
            }
            jcrService.setOcmMapper(new AnnotationMapperImpl(ocmClasses));
        }
    }

    protected void registerTypes(Workspace workspace, NodeTypeDef[] types)
            throws RepositoryException, InvalidNodeTypeDefException {
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
                try {
                    ntReg.reregisterNodeType(ntd);
                } catch (RepositoryException e) {
                    throw new RepositoryRuntimeException("The underlying schema has changed. " +
                            "Please start with a clean installation of Artifactory " +
                            "and import your previously exported system to it.", e);
                }
            }
        }
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

    protected JcrService getJcrService() {
        return jcrService;
    }
}
