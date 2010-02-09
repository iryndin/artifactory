/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.jcr.trash;

import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.tx.SessionResource;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Disposes of trashed items. Trashman instance is created per session (ie, created first time requested in the jcr
 * session and destroyed when the session completes).
 *
 * @author yoavl
 */
public class Trashman implements SessionResource {
    private static final Logger log = LoggerFactory.getLogger(Trashman.class);

    private static final Random random = new Random(System.nanoTime());

    private String sessionFolderName;
    private Set<String> itemFolderNames = new HashSet<String>();

    public void afterCompletion(boolean commit) {
        if (sessionFolderName != null && commit && hasPendingResources()) {
            InternalContextHelper.get().getJcrService().deleteFromTrash(sessionFolderName);
        }
        //Reset internal state
        sessionFolderName = null;
        itemFolderNames.clear();
    }

    public boolean hasPendingResources() {
        return itemFolderNames.size() > 0;
    }

    public void onSessionSave() {
        //Nothing to do as part of the transaction
    }

    public void addItemsToTrash(List<JcrFsItem> items, JcrService jcr) {
        List<String> jcrAbsolutePaths = new ArrayList<String>();
        for (JcrFsItem item : items) {
            String path = item.getPath();
            jcrAbsolutePaths.add(path);
        }
        addPathsToTrash(jcrAbsolutePaths, jcr);
    }

    public void addPathsToTrash(List<String> jcrAbsolutePaths, JcrService jcr) {
        String sessionFolderPath = getSessionFolderName(jcr);

        JcrSession session = jcr.getManagedSession();
        String tempItemFolderName = "" + random.nextInt(Integer.MAX_VALUE);
        try {
            Node sessionFolderNode =
                    (Node) session.getItem(JcrPath.get().getTrashJcrRootPath() + "/" + sessionFolderPath);
            Node tempItemFolderNode = sessionFolderNode.addNode(tempItemFolderName, JcrTypes.NT_UNSTRUCTURED);
            tempItemFolderNode.addMixin("mix:referenceable");
            //Move items to the trash folder
            for (String jcrPath : jcrAbsolutePaths) {
                String name = PathUtils.getName(jcrPath);
                session.move(jcrPath, tempItemFolderNode.getPath() + "/" + name);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        addTrashedFolder(tempItemFolderName);
    }

    private void addTrashedFolder(String folderName) {
        if (itemFolderNames.contains(folderName)) {
            log.warn("The folder '" + folderName + "' is already registered.");
        }
        itemFolderNames.add(folderName);
    }

    private String getSessionFolderName(JcrService jcr) {
        if (sessionFolderName == null) {
            String tempSessionFolderName = "" + random.nextInt(1 << 20);
            String tempSessionFolderPath = JcrPath.get().getTrashJcrRootPath() + "/" + tempSessionFolderName;
            try {
                JcrSession session = jcr.getManagedSession();
                if (!session.itemExists(tempSessionFolderPath)) {
                    createSessionTrashFolder(session, tempSessionFolderName);
                }
            } catch (Exception e) {
                throw new RepositoryRuntimeException(e);
            }
            sessionFolderName = tempSessionFolderName;
        }
        return sessionFolderName;
    }

    private void createSessionTrashFolder(JcrSession session, String sessionFolderName) throws RepositoryException {
        Node rootTrash = (Node) session.getItem(JcrPath.get().getTrashJcrRootPath());
        Node sessionFolderNode = rootTrash.addNode(sessionFolderName, JcrTypes.NT_UNSTRUCTURED);
        sessionFolderNode.addMixin("mix:referenceable");
        session.save();
    }
}