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
package org.jfrog.maven.viewer.dao;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.validation.ModelValidationResult;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.jfrog.maven.viewer.common.ArtifactIdentifier;
import org.jfrog.maven.viewer.common.Config;
import org.jfrog.maven.viewer.dao.exception.ProjectCreationException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * User: Dror Bereznitsky
 * Date: 03/11/2006
 * Time: 22:14:02
 */
class DefaultMavenProjectDao implements MavenProjectDao {

    private HashMap<String, MavenProject> cache;

    private MavenEmbedder mavenEmbedder;

    ArtifactRepository localRepository;

    List<ArtifactRepository> remoteRepositories;

    private final List<ArtifactRepository> defaultRemoteRepositories;

    Logger logger = Logger.getLogger(DefaultMavenProjectDao.class);
    private static final String FILE_PROTOCOL = "file://";

    public DefaultMavenProjectDao() {
        cache = new HashMap<String, MavenProject>();
        defaultRemoteRepositories = new ArrayList<ArtifactRepository>();
        remoteRepositories = new ArrayList<ArtifactRepository>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // TODO Maybe this should be seperated into a maven2 helper ?

        // Creating the maven embedder that will be used for maven related actions
        try {
            mavenEmbedder = new MavenEmbedder();
            mavenEmbedder.setClassLoader(classLoader);
            mavenEmbedder.setLogger(new MavenEmbedderConsoleLogger());
            mavenEmbedder.setInteractiveMode(false);
            mavenEmbedder.start();

            String localRepositoryPath = Config.getLocalRepository();
            if (mavenEmbedder.getLocalRepository() == null || localRepositoryPath != null) {
                if (localRepositoryPath == null) {
                    localRepositoryPath = System.getProperty("user.home") + "/.m2/repository";
                    Config.setLocalRepository(localRepositoryPath);
                }
                localRepository = mavenEmbedder.createLocalRepository(localRepositoryPath, "local");
                mavenEmbedder.setLocalRepositoryDirectory(new File(localRepositoryPath));
            } else {
                localRepository = mavenEmbedder.getLocalRepository();
                String path = localRepository.getUrl();
                if (path.startsWith(FILE_PROTOCOL)) {
                    path = path.substring(FILE_PROTOCOL.length());
                }
                Config.setLocalRepository(path);
            }
            String centralRepo = Config.getCentralRemoteRepository();
            if (centralRepo != null) {
                ArtifactRepository centralRepository = mavenEmbedder.createRepository(centralRepo, "central");
                defaultRemoteRepositories.add(0, centralRepository);
                remoteRepositories.add(0, centralRepository);
            }
            if (Config.isOffline()) {
                logger.info("Working in offline mode !");
                mavenEmbedder.setOffline(true);
            }
        } catch (Exception e) {
            logger.fatal("Could not create DefaultMavenProjectDao instance", e);
            throw new RuntimeException("Could not create DefaultMavenProjectDao instance", e);
        }
    }

    public MavenProject getMavenProject(ArtifactIdentifier artifactIdentifier) throws ProjectCreationException {

        logger.debug("Fetching artifact: " + artifactIdentifier);

        if (cache.containsKey(artifactIdentifier.toString())) {
            return cache.get(artifactIdentifier.toString());
        }

        Artifact artifact = mavenEmbedder.createArtifact(artifactIdentifier.getGroupId(),
                artifactIdentifier.getArtifactId(),
                artifactIdentifier.getVersion(),
                "compile",
                "pom");

        try {
            return mavenEmbedder.buildFromRepository(artifact, remoteRepositories, localRepository, false);
        } catch (ProjectBuildingException e) {

            String message = "Could not create a maven project for - " + artifactIdentifier;
            logger.error(message + " " + e.getMessage());
            throw new ProjectCreationException(message, e);
        }
    }

    public MavenProject getMavenProject(File pom) throws ProjectCreationException {

        logger.debug("Fetching artifact from file: " + pom.getAbsolutePath());

        try {
            return mavenEmbedder.readProject(pom, false);
        } catch (ProjectBuildingException e) {

            String message =
                    "Could not create a maven project from file: " + pom.getAbsolutePath();
            if (e instanceof InvalidProjectModelException) {
                ModelValidationResult validationResult =
                        ((InvalidProjectModelException)e).getValidationResult();
                for (int i=0; i< validationResult.getMessageCount(); i++) {
                    message += "\n(" + i + ") " + validationResult.getMessage(i);
                }
            }
            logger.error(message + ": " + e.getMessage());
            throw new ProjectCreationException(message, e);
        }
    }


    void setRepositories(List<Repository> repos) {
        remoteRepositories = null;
        remoteRepositories = new ArrayList<ArtifactRepository>();
        remoteRepositories.addAll(defaultRemoteRepositories);
        for (Repository repo : repos) {
            try {
                remoteRepositories.add(
                        mavenEmbedder.createRepository(repo.getUrl(), repo.getId()));
            } catch (ComponentLookupException e) {
                logger.warn("Could not create repository: " + repo.getUrl(), e);
            }
        }
    }
}
