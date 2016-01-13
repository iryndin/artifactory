import {ChangePasswordController} from './change_password.controller';

function changePasswordConfig ($stateProvider) {
    $stateProvider
            .state('change-password', {
                url: '/change-password',
                templateUrl: 'states/change_password/change_password.html',
                controller: 'ChangePasswordController as ChangePassword',
                parent: 'login-layout',
                params: {username: ''},
            })
}

export default angular.module('login', [])
        .config(changePasswordConfig)
        .controller('ChangePasswordController', ChangePasswordController);