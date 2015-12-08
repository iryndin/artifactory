import EVENTS from '../../constants/common_events.constants.js';

export class UserProfileController {

    constructor($state,$scope,UserProfileDao, ArtifactoryFeatures, BintrayDao, ArtifactoryNotifications, User, ArtifactoryEventBus, ArtifactoryModelSaver, OAuthDao, ArtifactoryGridFactory, ArtifactoryModal) {
        this.$scope = $scope;
        this.$state = $state;
        this.userProfileDao = UserProfileDao;
        this.bintrayDao = BintrayDao.getInstance();
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.User = User;
        this.currentUser = User.getCurrent();
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.OAuthDao = OAuthDao;
        this.features = ArtifactoryFeatures;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['userInfo'],['apiKey']);
        this.modal = ArtifactoryModal;

        this.userInfo = {};
        this.currentPassword = null;
        this.showUserApiKey = false;
        this.showBintrayApiKey = false;
        this.profileLocked = true;

        if(this.currentUser.name=='anonymous'){
            $state.go('home');
        }

        $('body').addClass('load-complete');

    }

    unlock() {
        this.userProfileDao.fetch({password: this.currentPassword}).$promise
            .then(response => {
                this.userInfo = response.data;
                    this.artifactoryModelSaver.save();
                //console.log(this.userInfo);
                this.profileLocked = false;

                if (!this.features.isOss()) {
                    this._initOAuthData();
                }

                this._getApiKey();

            });
    }

    _getApiKey() {
        this.userProfileDao.getApiKey().$promise.then((res)=>{
            this.userInfo.apiKey = res.apiKey;
        });
    }


    revokeApiKey() {
        this.modal.confirm(`Are you sure you want to revoke your API key?`)
                .then(() => {
                    this.userProfileDao.revokeApiKey().$promise.then((res)=> {
                        this._getApiKey();
                    });
                });
    }
    regenerateApiKey() {
        this.userProfileDao.regenerateApiKey().$promise.then((res)=>{
            if (res.apiKey) {
                this.artifactoryNotifications.create({info: 'Successfully regenerated API key'});
                this.userInfo.apiKey = res.apiKey;
            }
            else {
                this.artifactoryNotifications.create({error: 'Failed to regenerate API key'});
            }
        });
    }
    generateApiKey() {
        this.userProfileDao.getAndCreateApiKey().$promise.then((res)=>{
            if (res.apiKey) {
                this.artifactoryNotifications.create({info: 'Successfully generated API key'});
                this.userInfo.apiKey = res.apiKey;
            }
            else {
                this.artifactoryNotifications.create({error: 'Failed to generate API key'});
            }
        });
    }

    _initOAuthData() {

        this.oauth = {};

        this.User.getOAuthLoginData().then((response) => {
            this.oauth.providers = response;

            this.OAuthDao.getUserTokens().$promise.then((data)=>{
                data.forEach((providerName) => {
                    let provider = _.findWhere(this.oauth.providers, {name: providerName});
                    if (provider) {
                        provider.binded = true;
                    }
                });
            });
        });

    }

    unbindOAuthProvider(providerName) {
        this.OAuthDao.deleteUserToken({},{username: this.currentUser.name, provider: providerName}).$promise.then((res)=>{
            this._initOAuthData();
        });
    }

    save() {
        if (this.userInfo.user.newPassword && this.userInfo.user.newPassword !== this.userInfo.user.retypePassword) {
            this.artifactoryNotifications.create({error: 'Passwords do not match'});
            return;
        }

        let params = {
            user: {
                email: this.userInfo.user.email,
                password: this.userInfo.user.newPassword
            },
            bintray: this.userInfo.bintray
        };
        this.userProfileDao.update(params).$promise.then(()=>{
            this.artifactoryModelSaver.save();
        });
    }

    testBintray() {
        this.artifactoryEventBus.dispatch(EVENTS.FORM_SUBMITTED, this.bintrayForm.$name);
        this.bintrayDao.fetch(this.userInfo.bintray);
    }

    isOAuthEnabled() {
        return this.oauth && this.oauth.providers && this.oauth.providers.length > 0;
    }

    onGotoOAuth() {
        localStorage.stateBeforeOAuth = this.$state.current.name;
    }

}