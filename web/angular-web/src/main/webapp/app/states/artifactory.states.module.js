import AdminState       from './admin/admin.module';
import ArtifactState    from './artifacts/artifacts.module';
import BuildsState      from './builds/builds.module';
import HomeModule       from './home/home.module';
import Login            from './login/login.module';
import ForgotPassword   from './forgot_password/forgot_password.module';
import UserProfile      from './user_profile/user_profile.module';
import ServerError      from './server_error/server_error.module';
import BaseState        from './base/base.module';

angular.module('artifactory.states', [
    AdminState.name,
    ArtifactState.name,
    BuildsState.name,
    HomeModule.name,
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
run(preventAccessToPagesByPermission);
function preventAccessToPagesByPermission(User, $rootScope, ArtifactoryNotifications, $location, $timeout,
        ArtifactoryFeatures, FooterDao, ArtifactoryState) {
    $rootScope.$on('$stateChangeStart', (e, toState, toParams, fromState, fromParams) => {
        // Permissions:
        if (!User.getCurrent().canView(toState.name, toParams)) {
            if (User.getCurrent().isProWithoutLicense()) {
                $timeout(() => $location.path('admin/configuration/register_pro'));
            }else {
                ArtifactoryNotifications.create({error: 'You are not authorized to view this page'});
                e.preventDefault();
                $timeout(() => $location.path('/home'));
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
    })
}