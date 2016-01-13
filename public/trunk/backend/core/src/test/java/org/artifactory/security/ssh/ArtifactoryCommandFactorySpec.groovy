package org.artifactory.security.ssh

import org.apache.sshd.server.Command
import org.apache.sshd.server.command.UnknownCommand
import org.artifactory.addon.gitlfs.GitLfsAddon
import org.artifactory.api.context.ContextHelper
import spock.lang.Specification

/**
 * @author Noam Y. Tenne
 */
class ArtifactoryCommandFactorySpec extends Specification {

    def 'Create an LFS command'() {
        setup:
        def commandFactory = new ArtifactoryCommandFactory(null, null)

        when:
        Command command = commandFactory.createCommand(GitLfsAuthenticateCommand.COMMAND_NAME +
                " artifactory/jim upload 46eb912dc29d5000f4412f329c1a1a22400bead03a07491e77e33cdfe923fa76")

        then:
        GitLfsAddon gitLfsAddon = ContextHelper.get().beanForType(GitLfsAddon.class)

        // command instanceof GitLfsAuthenticateCommand
    }

    def 'Create an unknown command'() {
        setup:
        def commandFactory = new ArtifactoryCommandFactory(null, null)

        when:
        Command command = commandFactory.createCommand('git-lfs-unkown-command')

        then:
        command instanceof UnknownCommand
    }
}
