package org.artifactory.security.ssh;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.command.UnknownCommand;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.gitlfs.GitLfsAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.security.props.auth.TokenManager;
import org.artifactory.security.ssh.command.CliAuthenticateCommand;
import org.artifactory.storage.security.service.UserGroupStoreService;

/**
 * @author Noam Y. Tenne
 * @author Chen Keinan
 */
public class ArtifactoryCommandFactory implements CommandFactory {

    private CentralConfigService centralConfigService;
    private UserGroupStoreService userGroupStoreService;
    private TokenManager tokenManager;

    public ArtifactoryCommandFactory(CentralConfigService centralConfigService,
                                     UserGroupStoreService userGroupStoreService, TokenManager tokenManager) {
        this.centralConfigService = centralConfigService;
        this.userGroupStoreService = userGroupStoreService;
        this.tokenManager = tokenManager;
    }

    @Override
    public Command createCommand(String command) {
        GitLfsAddon gitLfsAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(GitLfsAddon.class);
        // if git lfs command
        if (gitLfsAddon.isGitLfsCommand(command)) {
            return gitLfsAddon.createGitLfsCommand(command, tokenManager);
        }
        // if cli command
        if (CliAuthenticateCommand.COMMAND_NAME.startsWith(command)) {
            return new CliAuthenticateCommand(centralConfigService, userGroupStoreService, command, tokenManager);
        }
        return new UnknownCommand(command);
    }
}
