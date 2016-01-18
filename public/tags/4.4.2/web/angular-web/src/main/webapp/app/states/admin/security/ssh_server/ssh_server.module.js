import {AdminSecuritySshServerController} from './ssh_server.controller.js';

function sshServerConfig($stateProvider) {

    $stateProvider
            .state('admin.security.ssh_server', {
                params: {feature: 'SSHSERVER'},
                url: '/ssh_server',
                templateUrl: 'states/admin/security/ssh_server/ssh_server.html',
                controller: 'AdminSecuritySshServerController as AdminSecuritySshServer'
            })
}

export default angular.module('security.ssh_server', [])
        .config(sshServerConfig)
        .controller('AdminSecuritySshServerController', AdminSecuritySshServerController);