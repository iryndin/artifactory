import EVENTS     from '../../constants/common_events.constants';

export class LoginController {

    constructor($state, $stateParams, User, $location, $window, ArtifactoryState, ArtifactoryEventBus, ArtifactoryNotifications) {
        this.user = {};
        this.rememberMe = false;
        this.UserService = User;
        this.$state = $state;
        this.$window = $window;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.$location = $location;
        this.ArtifactoryState = ArtifactoryState;
        this.canResetPassword = false;
        this.canRememberMe = false;
        this.loginForm = null;
        this.pending = false;

        this.canExit = (User.currentUser.name !== 'anonymous' || User.currentUser.anonAccessEnabled);

        this.oauth = {}
        User.getOAuthLoginData().then((response) => {
            this.oauth.providers = response;
        });

        if ($stateParams.oauthError) this.errorMessage = $stateParams.oauthError;

        this.checkResetPassword();
    }

    login() {

        this.artifactoryEventBus.dispatch(EVENTS.FORM_SUBMITTED);

        if (this.loginForm.$valid && !this.pending) {
            this.pending = true;
            this.UserService.login(this.user, this.rememberMe).then(success.bind(this), error.bind(this))
        }

        function success(result) {
            this.pending = false;
            this.user = result.data;
            let urlAfterLogin = this.ArtifactoryState.getState('urlAfterLogin');
            if (urlAfterLogin) {
                this.$location.path(urlAfterLogin)
            }
            else {
                this.$state.go('home');
            }
        }

        function error(response) {
            this.pending = false;
            if (response.data) {
                if (!this.catchExpired(response)) {
                    this.errorMessage = response.data.error;
                }
            }
        }

    }

    catchExpired(response) {
        //THIS IS VERY VERY TEMPORARY, B.E. SHOULD CHANGE RESPONSE TO ENABLE CATCHING IT NOT BY ERROR STRING MATCH
        let msg = 'Your credentials have expired, You must change your password before trying to login again';
        if (response.data && response.data.error && response.data.error === msg) {
            this.artifactoryNotifications.create({error: msg});
            this.$state.go('change-password', {username: this.user.user});
            return true;
        }
        return false;
    }

    userPasswordChanged() {
        this.errorMessage = null;
    }


    checkResetPassword() {
        this.UserService.getLoginData().then((response) => {
            this.canResetPassword = response.forgotPassword;
            this.canRememberMe = response.canRememberMe;
            this.ssoProviderLink = response.ssoProviderLink;
            this.oauthProviderLink = response.oauthProviderLink;
        });
    }

    gotoForgotPwd() {
        this.$state.go('forgot-password');
    }

/*
    oauthLogin() {
        this.$window.open(this.oauthProviderLink,'_self');
    }
*/

    ssoLogin() {
        this.$window.open(this.ssoProviderLink,'_self');
    }

    isOAuthEnabled() {
        return this.oauth.providers && this.oauth.providers.length > 0;
    }

    onGotoOAuth() {
        localStorage.stateBeforeOAuth = this.$state.current.name;
    }
}
