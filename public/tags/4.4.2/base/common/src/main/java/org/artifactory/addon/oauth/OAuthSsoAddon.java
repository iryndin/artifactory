package org.artifactory.addon.oauth;

import org.artifactory.addon.Addon;

/**
 * @author Travis Foster
 */
public interface OAuthSsoAddon extends Addon {
    boolean isActive();
}
