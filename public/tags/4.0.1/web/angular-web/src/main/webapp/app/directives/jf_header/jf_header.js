import EVENTS from '../../constants/artifacts_events.constants';
import API from '../../constants/api.constants';

class jfHeaderController {
    constructor($scope, $q, User, $state, $timeout, $window, GeneralConfigDao, FooterDao, ArtifactoryEventBus) {
        this.$scope = $scope;
        this.currentUser = User.getCurrent();
        this.generalConfigDao = GeneralConfigDao;
        this.footerDao = FooterDao;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.user = User;
        this.state = $state;
        this.$timeout = $timeout;
        this.$window = $window;
        this.$q = $q;


        this.logoEndPoint = `${API.API_URL}/auth/screen/logo`;
        this.defaultLogoUrl = 'images/artifactory_logo.png';

        this._registerEvents();

        this._getLogoUrlAndServerName();

    }

    _registerEvents() {
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.LOGO_UPDATED, () => this._getLogoUrlAndServerName());
    }


    _getLogoUrlAndServerName() {

        this.footerDao.get(true).then(footerData => {

            if (footerData.serverName) this.$window.document.title = footerData.serverName;
            else this.$window.document.title = 'Artifactory';

            if (footerData.userLogo) {
                this.logoUrl = '';
                this.$timeout(()=> {
                    this.logoUrl = this.logoEndPoint;
                });
            }
            else if (footerData.logoUrl) {
                this.logoUrl = footerData.logoUrl;
            }
            else {
                this.logoUrl = this.defaultLogoUrl;
            }
        });

    }



    logout() {
        this.user.logout()
        .then(() => {
            this.state.go("home");
        });
    }

}

export function jfHeader() {
    return {
        scope: {
            hideSearch: '@'
        },
        controller: jfHeaderController,
        controllerAs: 'jfHeader',
        bindToController: true,
        templateUrl: 'directives/jf_header/jf_header.html'
    }
}