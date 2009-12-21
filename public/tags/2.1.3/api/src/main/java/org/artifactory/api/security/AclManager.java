/*
 * Copyright 2009 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.security;

/**
 * Main ACL manager interface
 *
 * @author Noam Y. Tenne
 */
public interface AclManager {

    /**
     * Reloads all the ACLs to the configuration
     */
    void reloadAcls();
}
