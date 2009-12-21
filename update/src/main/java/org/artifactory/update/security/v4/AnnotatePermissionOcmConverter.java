/*
 * Copyright 2009 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.update.security.v4;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AclManager;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.UserInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * An "on the fly" converter that performs alterations directly on jcr nodes. Locates any ACE (except anonymous) with
 * admin and deploy permissions, and grants them an annotate permission
 *
 * @author Noam Y. Tenne
 */
public class AnnotatePermissionOcmConverter implements ConfigurationConverter<Session> {

    private static final Logger log = LoggerFactory.getLogger(AnnotatePermissionOcmConverter.class);

    public void convert(Session session) {
        try {
            log.info("Starting AnnotatePermissionOcmConverter");
            Node aclsNode = (Node) session.getItem("/configuration/acls/");
            NodeIterator acls = aclsNode.getNodes();

            while (acls.hasNext()) {
                Node acl = acls.nextNode();
                Node aces = acl.getNode("aces");
                NodeIterator acesIterator = aces.getNodes();
                while (acesIterator.hasNext()) {
                    Node ace = acesIterator.nextNode();
                    String principal = ace.getProperty("principal").getString();

                    if (!principal.equals(UserInfo.ANONYMOUS)) {
                        int mask = Integer.parseInt(ace.getProperty("mask").getString());

                        if ((mask & (ArtifactoryPermission.ADMIN.getMask() |
                                ArtifactoryPermission.DEPLOY.getMask())) > 0) {
                            mask |= ArtifactoryPermission.ANNOTATE.getMask();
                            ace.setProperty("mask", Integer.toString(mask));
                        }
                    }
                }
            }

            session.save();
            AclManager aclManager = ContextHelper.get().beanForType(AclManager.class);
            /**
             * Reload ACLs since this code is executed in the security service, which is after the ACL cache has already
             * been initialized
             */
            aclManager.reloadAcls();
            log.info("Finished AnnotatePermissionOcmConverter");
        } catch (RepositoryException e) {
            log.error("An error occurred during the annontate permission OCM conversion", e);
        }
    }
}