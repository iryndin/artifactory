package org.artifactory.addon.gitlfs;

import org.apache.sshd.server.Command;
import org.artifactory.addon.Addon;
import org.artifactory.security.props.auth.TokenManager;


/**
 * @author Chen Keinan
 */
public interface GitLfsAddon extends Addon {

    boolean isGitLfsCommand(String command);

    Command createGitLfsCommand(String command, TokenManager tokenManager);

}
