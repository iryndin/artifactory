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
package org.jfrog.maven.viewer.domain;

import org.apache.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.viewer.common.ArtifactIdentifier;
import org.jfrog.maven.viewer.common.MavenHelper;
import org.jfrog.maven.viewer.dao.DaoFactory;
import org.jfrog.maven.viewer.dao.exception.ProjectCreationException;
import org.jfrog.maven.viewer.domain.exception.ArtifactCreationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Dror Bereznitsky
 * Date: 28/11/2006
 * Time: 15:47:13
 */
public class ArtifactFactory {
    private DaoFactory daoFactory;

    private static final Logger logger = Logger.getLogger(ArtifactFactory.class);

    public DaoFactory getDaoFactory() {
        return daoFactory;
    }

    public void setDaoFactory(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    public Artifact createArtifact(ArtifactIdentifier identifier) throws ArtifactCreationException {
        MavenProject mavenProject;
        try {
            mavenProject = daoFactory.getMavenProjectDao().getMavenProject(identifier);
        } catch (ProjectCreationException e) {
            throw new ArtifactCreationException("Could not create artifact " + identifier, e);
        }
        return createArtifact(mavenProject, identifier);
    }

    public Artifact createArtifact(File pom) throws ArtifactCreationException {
        MavenProject mavenProject;
        try {
            mavenProject = daoFactory.getMavenProjectDao().getMavenProject(pom);
        } catch (ProjectCreationException e) {
            throw new ArtifactCreationException(
                    "Could not create artifact: " + e.getMessage(), e);
        }
        ArtifactIdentifier artifactIdentifier =
                new ArtifactIdentifier(
                        mavenProject.getArtifactId(),
                        mavenProject.getGroupId(),
                        mavenProject.getVersion()
                );
        return createArtifact(mavenProject, artifactIdentifier);
    }

    private Artifact createArtifact(MavenProject mavenProject, ArtifactIdentifier identifier) {
        if (mavenProject != null) {
            ArtifactImpl artifact = new ArtifactImpl(mavenProject);
            artifact.setDependencies(resolveDependencies(mavenProject, new ArrayList<ArtifactIdentifier>()));
            return artifact;
        } else {
            return new MockArtifact(identifier);
        }
    }

    public Artifact createArtifact(ArtifactDependency dependency, Artifact dependent) {
        Artifact artifact;
        MavenProject mavenProject = null;
        try {
            mavenProject =
                    daoFactory.getMavenProjectDao(MavenHelper.getAllRepositories(dependent.getMavenProject()))
                            .getMavenProject(dependency.getDependency());
            artifact = createArtifact(mavenProject, dependency.getDependency());
        } catch (ProjectCreationException e) {
            logger.error("Could not create a MavenProject for artifact: " + dependency.getDependency(), e);
            artifact = new MockArtifact(dependency.getDependency());
        }
        artifact.setDependent(dependency.getDependent());
        artifact.setScope(dependency.getScope());
        artifact.setDependencies(resolveDependencies(mavenProject, dependency.getExclusions()));
        return artifact;
    }

    //TODO move to a helper class
    private List<ArtifactDependency> resolveDependencies(MavenProject model, List<ArtifactIdentifier> depExclusions) {
        ArrayList<ArtifactDependency> dependencies = new ArrayList<ArtifactDependency>();
        if (model != null) {
            for (Object o : model.getDependencies()) {
                Dependency d = (Dependency) o;

                //TODO configure the behavior in cases of optional dependency
                if (d.isOptional()) continue;

                ArtifactIdentifier dai = new ArtifactIdentifier(d);

                if (shouldBeExcluded(dai, depExclusions)) continue;

                List<ArtifactIdentifier> exclusions = new ArrayList<ArtifactIdentifier>();
                for (Object oe : d.getExclusions()) {
                    Exclusion ex = (Exclusion) oe;
                    ArtifactIdentifier id = new ArtifactIdentifier(ex.getArtifactId(), ex.getGroupId(), "");
                    exclusions.add(id);
                }

                ArtifactDependency dependency = new ArtifactDependency(
                        new ArtifactIdentifier(model),
                        dai,
                        d.getScope(),
                        d.isOptional(),
                        exclusions
                );
                dependencies.add(dependency);
            }
        }
        return dependencies;
    }

    //TODO move to a helper class
    private boolean shouldBeExcluded(ArtifactIdentifier dai, List<ArtifactIdentifier> depExclusions) {
        for (ArtifactIdentifier depExc : depExclusions) {
            if (depExc.getGroupId().equals(dai.getGroupId()) && depExc.getArtifactId().equals(dai.getArtifactId()))
                return true;
        }
        return false;
    }
}
