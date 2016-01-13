package org.artifactory.security.ssh

import org.apache.sshd.server.session.ServerSession
import org.artifactory.api.security.UserInfoBuilder
import org.artifactory.storage.security.service.UserGroupStoreService
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
import spock.lang.Specification

import java.security.spec.RSAPublicKeySpec

/**
 * @author Noam Y. Tenne
 */
class PublicKeyAuthenticatorSpec extends Specification {

    def 'Authenticate public key'() {
        def userGroupStoreService = Mock(UserGroupStoreService)
        def serverSession = Mock(ServerSession)
        def authenticator = new PublicKeyAuthenticator(userGroupStoreService)

        def publicKey = new BCRSAPublicKey(new RSAPublicKeySpec(1.toBigInteger(), 3.toBigInteger()));

        when:
        def result = authenticator.authenticate('git', publicKey, serverSession)

        then:
        result
        1 * userGroupStoreService.findUserByProperty('sshPublicKey', 'ssh-rsa AAAAB3NzaC1yc2EAAAABAwAAAAEB') >> {
            return new UserInfoBuilder('jim').build()
        }
        1 * serverSession.setAttribute(_ as UsernameAttributeKey, 'jim')
    }

    def 'Authenticate public key and find no user '() {
        def userGroupStoreService = Mock(UserGroupStoreService)
        def serverSession = Mock(ServerSession)
        def authenticator = new PublicKeyAuthenticator(userGroupStoreService)

        def publicKey = new BCRSAPublicKey(new RSAPublicKeySpec(1.toBigInteger(), 3.toBigInteger()));

        when:
        def result = authenticator.authenticate('git', publicKey, serverSession)

        then:
        !result
        1 * userGroupStoreService.findUserByProperty('sshPublicKey', 'ssh-rsa AAAAB3NzaC1yc2EAAAABAwAAAAEB') >> {
            return null
        }
    }
}
