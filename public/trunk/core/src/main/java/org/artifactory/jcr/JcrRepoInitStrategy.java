/*
 * This file is part of Artifactory.
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

package org.artifactory.jcr;

import org.apache.commons.io.FileUtils;
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
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.jcr.jackrabbit.query.LenientOnWorkspaceInconsistency;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.LoggingUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: freds Date: Jun 3, 2008 Time: 12:14:52 PM
 */
public class JcrRepoInitStrategy {
    private static final Logger log = LoggerFactory.getLogger(JcrRepoInitStrategy.class);

    private final JcrServiceImpl jcrService;

    protected static final String ARTIFACTORY_NAMESPACE_PREFIX = "artifactory";
    protected static final String ARTIFACTORY_NAMESPACE = "http://artifactory.jfrog.org/1.0";
    protected static final String OCM_NAMESPACE_PREFIX = "ocm";
    protected static final String OCM_NAMESPACE = "http://jackrabbit.apache.org/ocm";

    public JcrRepoInitStrategy(JcrService jcrService) {
        this.jcrService = (JcrServiceImpl) jcrService;
    }

    public JackrabbitRepository createJcrRepository(ResourceStreamHandle repoXml) {
        updateCurrentWorkspaces();
        JackrabbitRepository repository;
        try {
            ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
            RepositoryConfig repoConfig = RepositoryConfig.create(
                    repoXml.getInputStream(), artifactoryHome.getJcrRootDir().getAbsolutePath());
            if (ConstantValues.jcrFixConsistency.getBoolean()) {
                WorkspaceConfig wsConfig = (WorkspaceConfig) repoConfig.getWorkspaceConfigs().iterator().next();
                PersistenceManagerConfig pmConfig = wsConfig.getPersistenceManagerConfig();
                String className = pmConfig.getClassName();
                Class<?> clazz = getClass().getClassLoader().loadClass(className);
                Object pm = clazz.newInstance();
                if (pm instanceof AbstractBundlePersistenceManager) {
                    pmConfig.getParameters().put("consistencyCheck", "true");
                    pmConfig.getParameters().put("consistencyFix", "true");
                    log.info("Fix consistency requested on '" + className + "'.");
                } else {
                    log.warn("Fix consistency requested on a persistence manager that does not support this feature.");
                }
            }
            //register our own workspace inconsistency handler
            LenientOnWorkspaceInconsistency.init();
            //Create the repository
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
            initializeLogs();
            initializeTrash();
        } catch (Exception e) {
            log.error("Cannot initialize JCR repository " + e.getMessage(), e);
            throw new RuntimeException("Cannot initialize JCR repository " + e.getMessage(), e);
        }
    }

    protected void initializeRepoRoot() {
        jcrService.getOrCreateUnstructuredNode(JcrPath.get().getRepoJcrRootPath());
    }

    protected void initializeLogs() {
        jcrService.getOrCreateUnstructuredNode(JcrPath.get().getLogsJcrRootPath());
    }

    protected void initializeOcm() throws ClassNotFoundException {
        if (jcrService.OCM_CLASSES != null) {
            // Create the ocm configuration root
            Node ocmRoot = jcrService.getOrCreateUnstructuredNode(JcrPath.get().getConfigJcrRootPath());
            // Create the configuration root
            jcrService.getOrCreateUnstructuredNode(ocmRoot, "artifactory");
            // Register the ocm classes
            List<Class> ocmClasses = new ArrayList<Class>();
            for (String className : jcrService.OCM_CLASSES) {
                ocmClasses.add(getClass().getClassLoader().loadClass(className));
            }
            jcrService.setOcmMapper(new AnnotationMapperImpl(ocmClasses));
        }
    }

    protected void initializeTrash() {
        Node node = jcrService.getOrCreateUnstructuredNode(JcrPath.get().getTrashJcrRootPath());
        try {
            NodeIterator children = node.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                child.remove();
            }
        } catch (RepositoryException e) {
            LoggingUtils.warnOrDebug(log, "Cannot clean trash on startup", e);
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
            nsReg.registerNamespace(ARTIFACTORY_NAMESPACE_PREFIX, ARTIFACTORY_NAMESPACE);
        }
        if (!nsPrefixes.contains(OCM_NAMESPACE_PREFIX)) {
            nsReg.registerNamespace(OCM_NAMESPACE_PREFIX, OCM_NAMESPACE);
        }
    }

    protected JcrService getJcrService() {
        return jcrService;
    }

    private void updateCurrentWorkspaces() {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        if (artifactoryHome.startedFromDifferentVersion()) {
            //Make the loaded repo.xml overwrite any existing wokspace by clearing data/workspaces
            File workspacesDir = new File(artifactoryHome.getDataDir(), "workspaces");
            CompoundVersionDetails originalStorageVersion = artifactoryHome.getOriginalVersionDetails();
            if (originalStorageVersion == null) {
                //First run
                return;
            }
            String origVersionValue = originalStorageVersion.getVersion().getValue();
            File origWorkspacesDir = new File(artifactoryHome.getDataDir(), "workspaces." + origVersionValue + ".orig");
            try {
                FileUtils.deleteDirectory(origWorkspacesDir);
            } catch (IOException e) {
                log.warn("Failed to remove original workspaces at {}.", origWorkspacesDir.getAbsolutePath());
            }
            try {
                FileUtils.copyDirectory(workspacesDir, origWorkspacesDir);
            } catch (IOException e) {
                log.warn("Failed to backup original workspaces from {} to {}.", workspacesDir.getAbsolutePath(),
                        origWorkspacesDir.getAbsolutePath());
            }
            try {
                FileUtils.deleteDirectory(workspacesDir);
            } catch (IOException e) {
                log.warn("Failed to remove workspaces at {}.", workspacesDir.getAbsolutePath());
            }
        }
    }
}
