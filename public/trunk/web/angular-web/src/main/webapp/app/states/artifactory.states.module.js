import AdminState       from './admin/admin.module';
import ArtifactState    from './artifacts/artifacts.module';
import BuildsState      from './builds/builds.module';
import HomeModule       from './home/home.module';
import OAuthErrorModule from './oauth_error/oauth_error.module';
import Login            from './login/login.module';
import ForgotPassword   from './forgot_password/forgot_password.module';
import UserProfile      from './user_profile/user_profile.module';
import ServerError      from './server_error/server_error.module';
import BaseState        from './base/base.module';
import EVENTS from '../constants/artifacts_events.constants';

angular.module('artifactory.states', [
    AdminState.name,
    ArtifactState.name,
    BuildsState.name,
    HomeModule.name,
    OAuthErrorModule.name,
    Login.name,
    ForgotPassword.name,
    UserProfile.name,
    ServerError.name,
    BaseState.name,
    'artifactory.services',
    'artifactory.dao',
    'cfp.hotkeys',
    'ui.router'
]).
run(changeStateHook);
function changeStateHook(User, $rootScope, $q, ArtifactoryNotifications, $location, $timeout, $state,
        ArtifactoryFeatures, FooterDao, ArtifactoryState, ArtifactoryEventBus, ArtifactoryModal) {

    ArtifactoryEventBus.register(EVENTS.USER_LOGOUT, (confirmDiscard) => {
        if (confirmDiscard) {
            checkDiscardConfirmation($q).then(()=>{
                ArtifactoryEventBus.dispatch(EVENTS.USER_LOGOUT);
            })
        }
    });

    $rootScope.$on('$stateChangeStart', (e, toState, toParams, fromState, fromParams) => {

        if (fromState.name.startsWith('admin.') || fromState.name === 'user_profile') {
            checkDiscardConfirmation($q, e).then(()=>{
                $state.go(toState.name,toParams);
            });
        }

        let saveAdminState = ArtifactoryState.getState('saveAdminState');
        if (toState.name.startsWith('admin.') && saveAdminState && !e.defaultPrevented) {
            ArtifactoryState.setState('lastAdminState', toState);
            ArtifactoryState.setState('lastAdminStateParams', toParams);
            ArtifactoryState.removeState('saveAdminState');
        }
        else if (saveAdminState) {
            ArtifactoryState.removeState('saveAdminState');
        }


        if (fromState.name && toState.name && fromState.name != toState.name) {
            ArtifactoryEventBus.dispatch(EVENTS.CANCEL_SPINNER);
        }

        if (toState.name === 'artifacts.browsers.search') {
            //MOVED FROM artifacts.module.js to prevent error message (ui-router bug workaround)
            ArtifactoryEventBus.dispatch(EVENTS.SEARCH_URL_CHANGED, toParams);
        }
        else if (fromState.name === 'artifacts.browsers.search') {
            ArtifactoryEventBus.dispatch(EVENTS.CLEAR_SEARCH);
        }



        if (toState.name === 'oauth_error') {
            e.preventDefault();

            let message = $location.search().message;
            let gotoState = localStorage.stateBeforeOAuth;

            if (gotoState === 'login') {
                $state.go(gotoState,{oauthError: message, location: "replace"});
            }
            else if (gotoState === 'user_profile') {
                ArtifactoryNotifications.create({error: message});
                $state.go(gotoState,{location: "replace"});
            }
            else {
                ArtifactoryNotifications.create({error: message});
                $state.go('home',{location: "replace"});
            }
        }


        if (toState.name === 'login' && $location.path() !== '/login' && $location.path() !== '/forgot-password' && $location.path() !== '/oauth_error') {
            let afterLogin = ArtifactoryState.getState('urlAfterLogin');
            if (!afterLogin) ArtifactoryState.setState('urlAfterLogin', $location.path());
        }


        // Permissions:

        if (!User.getCurrent().canView(toState.name, toParams)) {
            if (User.getCurrent().isProWithoutLicense()) {
                $timeout(() => $location.path('admin/configuration/register_pro'));
            }else {
                if ($location.path() !== '/login') ArtifactoryState.setState('urlAfterLogin', $location.path());
                ArtifactoryNotifications.create({error: 'You are not authorized to view this page'});
                e.preventDefault();
                $timeout(() => $location.path('/login'));
            }
        }
        // Features per license:
        else {
            let feature = toParams.feature;
            // Must verify footer data is available before checking (for initial page load)
            FooterDao.get().then(() => {
                if (ArtifactoryFeatures.isDisabled(feature) || ArtifactoryFeatures.isHidden(feature)) {
                    ArtifactoryNotifications.create({error: 'Page unavailable'});
                    e.preventDefault();
                    $timeout(() => $location.path('/home'));
                }
            });
        }

        if (!e.defaultPrevented) {
            ArtifactoryEventBus.dispatch(EVENTS.CLOSE_MODAL);
        }
    })
}


function checkDiscardConfirmation($q, e) {

    let defer = $q.defer();
    let forms = $('form');
    let changeDiscovered = false;
    for (let i = 0; i< forms.length; i++) {
        let form = forms[i];
        let controller = angular.element(form).controller();
        if (controller && controller._$modelSaver$_ && controller._$modelSaver$_.confirmOnLeave && !controller._$modelSaver$_.isModelSaved()) {
            changeDiscovered = true;
            controller._$modelSaver$_.ask().then(()=>{
                controller._$modelSaver$_.confirmOnLeave =   false;
                defer.resolve();
            });
            break;
        }
    }

    if (!changeDiscovered && !e) {
        defer.resolve();
    }
    else if (changeDiscovered && e) {
        e.preventDefault();
    }

    return defer.promise;

}
