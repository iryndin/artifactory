package org.artifactory.security;

import org.artifactory.common.Info;


/**
 * @author Chen Keinan
 */
public interface UserPropertyInfo extends Info {

    long getUserId();

    String getPropKey();

    String getPropValue();
}
