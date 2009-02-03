package org.artifactory.security;

import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class StringObjectIdentity extends NamedEntityObjectIdentity {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(StringObjectIdentity.class);

    public StringObjectIdentity(String id) {
        super(String.class.getName(), id);
    }
}
