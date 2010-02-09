package org.artifactory.jcr;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import static org.artifactory.jcr.ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE;
import static org.artifactory.jcr.ArtifactoryJcrConstants.NT_ARTIFACTORY_FOLDER;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFolder extends JcrFsItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFolder.class);


    public JcrFolder(Node node) {
        super(node);
    }

    public List<JcrFsItem> getItems() {
        List<JcrFsItem> items = new ArrayList<JcrFsItem>();
        try {
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String typeName = node.getPrimaryNodeType().getName();
                if (typeName.equals(NT_ARTIFACTORY_FOLDER)) {
                    items.add(new JcrFolder(node));
                } else if (typeName.equals(NT_ARTIFACTORY_FILE)) {
                    items.add(new JcrFile(node));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve folder node items.", e);
        }
        return items;
    }

    public void export(final File targetDir) {
        try {
            List<JcrFsItem> list = getItems();
            for (JcrFsItem item : list) {
                String relPath = item.relPath();
                File targetFile = new File(targetDir, relPath);
                if (item.isDirectory()) {
                    LOGGER.info("Exporting directory '" + relPath + "'...");
                    boolean res = targetFile.mkdirs();
                    if (res) {
                        JcrFolder jcrFolder = ((JcrFolder) item);
                        jcrFolder.export(targetDir);
                    } else {
                        throw new IOException(
                                "Failed to create directory '" + targetFile.getPath() + "'.");
                    }
                } else {
                    LOGGER.info("Exporting file '" + relPath + "'...");
                    FileOutputStream os = new FileOutputStream(targetFile);
                    JcrFile jcrFile = ((JcrFile) item);
                    InputStream is = jcrFile.getStream();
                    try {
                        IOUtils.copy(is, os);
                    } finally {
                        IOUtils.closeQuietly(is);
                        IOUtils.closeQuietly(os);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to export to dir '" + targetDir.getPath() + "'.", e);
        }
    }

    public boolean isDirectory() {
        return true;
    }
}
