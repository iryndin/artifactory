package org.artifactory.jcr;

import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: freds Date: Jun 2, 2008 Time: 4:56:26 PM
 */
public interface JcrWrapper {
    <T> T doInSession(JcrCallback<T> callback);

    ObjectContentManager getOcm();

    boolean itemNodeExists(String absPath);

    Node getOrCreateUnstructuredNode(String absPath);

    void destroy();

    void init();

    boolean isReadOnly();

    List<String> getOcmClassesList();

    void setOcmMapper(Mapper ocmMapper);

    NodeTypeDef[] getArtifactoryNodeTypes() throws IOException, InvalidNodeTypeDefException;

    void setReadOnly(boolean readOnly);

    JcrFile importStream(
            JcrFolder parentFolder, String name, String repoKey, long lastModified, InputStream in)
            throws RepositoryException;

    boolean isCreateSessionIfNeeded();

    void setCreateSessionIfNeeded(boolean createSessionIfNeeded);
}
