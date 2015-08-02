import EVENTS from '../../constants/artifacts_events.constants';
import API from '../../constants/api.constants';

class jfHeaderController {
    constructor($scope, $q, User, $state, $timeout, $window, GeneralConfigDao, ArtifactoryEventBus) {
        this.$scope = $scope;
        this.currentUser = User.getCurrent();
        this.generalConfigDao = GeneralConfigDao;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.user = User;
        this.state = $state;
        this.$timeout = $timeout;
        this.$window = $window;
        this.$q = $q;

        this.logoEndPoint = `${API.API_URL}/generalConfig/logo`;
        this.defaultLogoUrl = 'images/artifactory_logo.png';

        this._registerEvents();

        this._getLogoUrlAndServerName();

    }

    _registerEvents() {
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.LOGO_UPDATED, () => this._getLogoUrlAndServerName());
    }


    _getLogoUrlAndServerName() {
        this._imageExists(this.logoEndPoint).then(()=>{
            this.logoUrl = '';
            this.$timeout(()=> {
                this.logoUrl = this.logoEndPoint;
            });
        })
        .catch(()=>{
            this.generalConfigDao.getData().$promise.then((data) => {

                if (data.serverName) this.$window.document.title = data.serverName;
                else this.$window.document.title = 'Artifactory';
                this.logoUrl = '';
                this.$timeout(()=> {
                    if (data.logoUrl) {
                        this.logoUrl = data.logoUrl;
                    }
                    else {
                        this.logoUrl = this.defaultLogoUrl;
                    }
                });
            })
                .catch((err)=> {
                    if (err.status === 401) {
                        this.$timeout(()=> {
                            this.logoUrl = this.defaultLogoUrl;
                        });
                    }
                });
        });

    }

    _imageExists(url) {
        let deferred = this.$q.defer();
        let img = new Image();
        img.onload = () => {
            deferred.resolve();
        };
        img.onerror = () => {
            deferred.reject('no image found');
        };
        img.src = url;
        return deferred.promise;
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