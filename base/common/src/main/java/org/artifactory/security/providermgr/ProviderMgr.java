package org.artifactory.security.providermgr;

import org.artifactory.security.props.auth.model.OauthModel;

/**
 * @author Chen Keinan
 */
public interface ProviderMgr {

    OauthModel fetchTokenFromProvider();
}
