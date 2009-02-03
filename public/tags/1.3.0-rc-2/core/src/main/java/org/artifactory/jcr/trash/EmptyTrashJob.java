package org.artifactory.jcr.trash;

import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.LoggingUtils;
import org.artifactory.util.PathUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.List;

/**
 * Low-level jcr trash emptor
 *
 * @author yoavl
 */
public class EmptyTrashJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(EmptyTrashJob.class);

    public static final String FOLDER_NAMES = "folderNames";

    @Override
    protected void onExecute(JobExecutionContext jobContext) throws JobExecutionException {
        String folderNamesString = jobContext.getJobDetail().getJobDataMap().getString(FOLDER_NAMES);
        InternalArtifactoryContext context = InternalContextHelper.get();
        JcrSession session = context.getJcrService().getUnmanagedSession();
        try {
            String trashRootPath = JcrPath.get().getTrashJcrRootPath();
            Node trashNode = (Node) session.getItem(trashRootPath);
            List<String> folderNames = PathUtils.delimitedListToStringList(folderNamesString, ",");
            //If there are no folders, just scan all the nodes directly under the jcr trash node
            if (folderNames.size() == 0) {
                NodeIterator folderNodes = trashNode.getNodes();
                while (folderNodes.hasNext()) {
                    Node trashFolder = (Node) folderNodes.next();
                    folderNames.add(trashFolder.getName());
                }
            }
            for (String folderName : folderNames) {
                Node folderNode = null;
                try {
                    folderNode = trashNode.getNode(folderName);
                    int deleted = delete(folderNode);
                    if (deleted > 0) {
                        log.debug("Emptied " + deleted + " nodes from trash folder " + folderName + ".");
                    }
                } catch (RepositoryException e) {
                    //Fail gracefully
                    LoggingUtils.warnOrDebug(log, "Could not empty trash folder " + folderName + ".", e);
                    if (folderNode != null) {
                        log.warn("Attempting force removal of trash folder " + folderName + ".");
                        try {
                            folderNode.remove();
                            log.warn("Force removal of trash folder " + folderName + " succeeded.");
                        } catch (RepositoryException e1) {
                            LoggingUtils.warnOrDebug(
                                    log, "Cannot complete force removal of trash folder " + folderName + ".", e);
                            //Continue with the other trash folders
                        }
                    }
                    //Continue with the other trash folders
                }
            }
        } catch (Exception e) {
            //Fail gracefully
            LoggingUtils.warnOrDebug(log, "Could not empty the trash folder.", e);
        } finally {
            session.logout();
        }
    }

    public int delete(Node node) throws RepositoryException {
        int count = 0;
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = (Node) nodes.next();
            count += delete(child);
        }
        //Only delete files and folders
        String nodeType = node.getPrimaryNodeType().getName();
        if (JcrFile.NT_ARTIFACTORY_FILE.equals(nodeType) || JcrFolder.NT_ARTIFACTORY_FOLDER.equals(nodeType) ||
                "nt:unstructured".equals(nodeType)) {
            //Remove myself
            node.remove();
            count++;
        }
        //Flush - actually stores the changes and preserves in-session memory
        node.getSession().save();
        //Be nice with the rest of the world
        Thread.yield();
        return count;
    }
}