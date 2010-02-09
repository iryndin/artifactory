package org.artifactory.security;

import org.acegisecurity.acl.basic.AclObjectIdentity;
import org.acegisecurity.acl.basic.BasicAclProvider;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class CustomAclProvider extends BasicAclProvider {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(CustomAclProvider.class);

    @Override
    protected AclObjectIdentity obtainIdentity(Object domainInstance) {
        return new StringObjectIdentity(domainInstance.toString());
    }
}
