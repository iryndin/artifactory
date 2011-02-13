/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.jackrabbit.query.LenientOnWorkspaceInconsistency;
import org.artifactory.jcr.version.JcrVersion;
import org.artifactory.log.LoggerFactory;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: freds Date: Jun 3, 2008 Time: 12:14:52 PM
 */
abstract class JcrRepoInitHelper {
    private static final Logger log = LoggerFactory.getLogger(JcrRepoInitHelper.class);

    private JcrRepoInitHelper() {
        // utility class
    }

    public static JackrabbitRepository createJcrRepository(ResourceStreamHandle repoXml, boolean preInit) {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        //Remove the exiting workspaces
        File workspacesDir = new File(artifactoryHome.getDataDir(), "workspaces");
        try {
            FileUtils.deleteDirectory(workspacesDir);
        } catch (IOException e) {
            log.warn("Could not remove original workspaces directory at '{}'.", workspacesDir.getAbsolutePath());
        }

        /**
         * Calling the jcr version converters from here since these are actions that should be performed before the repo
         * is initialized and "standard" conversion of the application context is too late
         */
        if (preInit) {
            preInitConvert(artifactoryHome);
        }

        //Copy the index config only after the pre-init conversion is done
        copyLatestIndexConfig(artifactoryHome);

        JackrabbitRepository repository;
        try {
            RepositoryConfig repoConfig = RepositoryConfig.create(
                    repoXml.getInputStream(), artifactoryHome.getJcrRootDir().getAbsolutePath());
            if (ConstantValues.jcrFixConsistency.getBoolean()) {
                WorkspaceConfig wsConfig = repoConfig.getWorkspaceConfigs().iterator().next();
                PersistenceManagerConfig pmConfig = wsConfig.getPersistenceManagerConfig();
                String className = pmConfig.getClassName();
                Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                Object pm = clazz.newInstance();
                if (pm instanceof AbstractBundlePersistenceManager) {
                    pmConfig.getParameters().put("consistencyCheck", "true");
                    pmConfig.getParameters().put("consistencyFix", "true");
                    log.warn("\n###########################################################################\n" +
                            "                STARTUP CONSISTENCY CHECKS ARE ACTIVE!\n" +
                            "Startup will take considerably longer while running consistency checks.\n" +
                            "Make sure you do not leave consistency checks permanently on by\n" +
                            "disabling/commenting-out the '{}' property\n" +
                            "in the '$ARTIFACTORY_HOME/etc/artifactory.system.properties' file.\n" +
                            "###########################################################################",
                            ConstantValues.jcrFixConsistency.getPropertyName());
                } else {
                    log.warn("Consistency checks requested on a persistence manager that doesn't support it: {}.",
                            className);
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

    public static void init(JcrServiceImpl jcrService) {
        try {
            JcrSession session = jcrService.getManagedSession();
            Workspace workspace = session.getWorkspace();
            registerTypes(workspace, jcrService.getArtifactoryNodeTypes());
            initializeOcm(jcrService);
            initializeRepoRoot(jcrService);
            initializeLogs(jcrService);
            initializeTrash(jcrService);
        } catch (Exception e) {
            log.error("Cannot initialize JCR repository " + e.getMessage(), e);
            throw new RuntimeException("Cannot initialize JCR repository " + e.getMessage(), e);
        }
    }

    public static void registerTypes(Workspace workspace, QNodeTypeDefinition[] types)
            throws RepositoryException, InvalidNodeTypeDefException {
        //Get the NodeTypeManager from the Workspace.
        //Note that it must be cast from the generic JCR NodeTypeManager to the
        //Jackrabbit-specific implementation.
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        //Acquire the NodeTypeRegistry
        NodeTypeRegistry ntReg = ntmgr.getNodeTypeRegistry();
        //Create or update (re-register) all QNodeTypeDefinitions
        for (QNodeTypeDefinition ntd : types) {
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

    protected static void initializeTrash(JcrServiceImpl jcrService) {
        jcrService.getOrCreateUnstructuredNode(JcrPath.get().getTrashJcrRootPath());
        //Empty whatever is left in the trash after initialization is complete
        InternalContextHelper.get().getJcrService().emptyTrashAfterCommit();
    }


    /**
     * Copy/override the indexing config from the jar to the index directory.
     */
    private static void copyLatestIndexConfig(ArtifactoryHome artifactoryHome) {
        FileOutputStream indexFileOutputStream = null;
        InputStream indexConfigInputStream = null;
        try {
            indexConfigInputStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("META-INF/jcr/index_config.xml");

            File indexDir = new File(artifactoryHome.getJcrRootDir(), "index");
            FileUtils.forceMkdir(indexDir);
            File indexConfigFile = new File(indexDir, "index_config.xml");
            indexFileOutputStream = new FileOutputStream(indexConfigFile);
            IOUtils.copy(indexConfigInputStream, indexFileOutputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to config jcr repo indexing.", e);
        } finally {
            IOUtils.closeQuietly(indexFileOutputStream);
            IOUtils.closeQuietly(indexConfigInputStream);
        }
    }

    private static void initializeRepoRoot(JcrServiceImpl jcrService) {
        jcrService.getOrCreateUnstructuredNode(JcrPath.get().getRepoJcrRootPath());
    }

    private static void initializeLogs(JcrServiceImpl jcrService) {
        jcrService.getOrCreateUnstructuredNode(JcrPath.get().getLogsJcrRootPath());
    }

    private static void initializeOcm(JcrServiceImpl jcrService) throws ClassNotFoundException {
        if (jcrService.OCM_CLASSES != null) {
            // Create the ocm configuration root
            Node ocmRoot = jcrService.getOrCreateUnstructuredNode(JcrPath.get().getConfigJcrRootPath());
            // Create the configuration root
            jcrService.getOrCreateUnstructuredNode(ocmRoot, "artifactory");
            // Register the ocm classes
            List<Class> ocmClasses = new ArrayList<Class>();
            for (String className : jcrService.OCM_CLASSES) {
                ocmClasses.add(Thread.currentThread().getContextClassLoader().loadClass(className));
            }
            jcrService.setOcmMapper(new AnnotationMapperImpl(ocmClasses));
        }
    }

    /**
     * Runs the pre JCR initialization converters in case of a version upgrade
     */
    private static void preInitConvert(ArtifactoryHome artifactoryHome) {
        boolean startedFormDifferentVersion = artifactoryHome.startedFromDifferentVersion();
        if (startedFormDifferentVersion) {
            CompoundVersionDetails source = artifactoryHome.getOriginalVersionDetails();
            if (source == null) {
                //First run
                return;
            }
            JcrVersion.values();
            JcrVersion originalJcrVersion = source.getVersion().getSubConfigElementVersion(JcrVersion.class);
            originalJcrVersion.preInitConvert(artifactoryHome);
        }
    }
}