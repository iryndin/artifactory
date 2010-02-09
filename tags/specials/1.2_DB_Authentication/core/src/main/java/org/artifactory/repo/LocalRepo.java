package org.artifactory.repo;

import org.apache.maven.model.Model;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.RepoResource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.io.InputStream;

public interface LocalRepo extends Repo {
    JcrFsItem getFsItem(String relPath, Session session) throws RepositoryException;

    boolean isUseSnapshotUniqueVersions();

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    Model getModel(ArtifactResource pa);

    String getPomContent(ArtifactResource pa);

    void saveResource(RepoResource res, InputStream stream);

    Node getRepoJcrNode(Session session);

    void importFolder(File folder, boolean singleTransation);

    void export(File dir);
}