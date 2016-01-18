import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminSecurityGeneralController {

    constructor(AdminSecurityGeneralDao, PasswordsEncryptionDao, ArtifactoryModelSaver, UserProfileDao, ArtifactoryModal, UserDao, ArtifactoryNotifications, User) {
        this.adminSecurityGeneralDao = AdminSecurityGeneralDao;
        this.passwordsEncryptionDao = PasswordsEncryptionDao.getInstance();
        this.options = ['SUPPORTED', 'UNSUPPORTED', 'REQUIRED'];
        this.modal = ArtifactoryModal;
        this.User = User;
        this.TOOLTIP = TOOLTIP.admin.security.general;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['generalConfig']);
        this.userProfileDao = UserProfileDao;
        this.userDao = UserDao.getInstance();
        this.artifactoryNotifications = ArtifactoryNotifications;

        this.userDao.getAll().$promise.then((users)=> {
            this.userNames = _.pluck(users,'name');
        });

        this.getGeneralConfigObject();
        this.getMasterKeyStatus();
    }

    getEncryptionButtonText() {
        return this.materKeyState.hasMasterKey ? "Decrypt" : "Encrypt";
    }

    getEncryptionStatusText() {
        return this.materKeyState.hasMasterKey ? "All passwords in your configuration are currently encrypted." :
                "All passwords in your configuration are currently visible in plain text.";
    }

    getGeneralConfigObject() {
        this.adminSecurityGeneralDao.get().$promise.then((data) => {
            this.generalConfig = data;
            this.artifactoryModelSaver.save();
        });
    }

    getMasterKeyStatus() {
        this.materKeyState = this.passwordsEncryptionDao.get();
    }
    forcePassExpForAll() {
        if (!this.generalConfig.passwordSettings.expirationPolicy.enabled) return;
        this.modal.confirm(`Are you sure you want to expire all user's passwords?`)
            .then(() => {
                this.userDao.expireAllPassword();
            });
    }

    unExpireAll() {
        if (!this.generalConfig.passwordSettings.expirationPolicy.enabled) return;
        this.modal.confirm(`Are you sure you want to unexpire all user's expired passwords?`)
            .then(() => {
                this.userDao.unExpireAllPassword();
            });
    }

    toggleEncryption() {
        let self = this;

        if (this.materKeyState.hasMasterKey) {
            this.materKeyState.$decrypt().then(function () {
                self.getMasterKeyStatus();
            })
        } else {
            this.materKeyState.$encrypt().then(function () {
                self.getMasterKeyStatus();
            });
        }
    }

    save() {
        this.adminSecurityGeneralDao.update(this.generalConfig).$promise.then(()=>{
            this.artifactoryModelSaver.save();
            this.User.reload();
        });
    }

    cancel() {
        this.artifactoryModelSaver.ask().then(()=>{
            this.getGeneralConfigObject();
        });
    }

    onClickAllowAnonymousAccess() {
        if (!this.generalConfig.anonAccessEnabled) {
            this.generalConfig.anonAccessToBuildInfosDisabled = false;
        }
    }

    canRevokeUser() {
        return _.contains(this.userNames,this.usernameToRevoke);
    }

    revokeApiKey() {
        this.userProfileDao.getApiKey({},{username: this.usernameToRevoke}).$promise.then((res)=>{
            if (res.apiKey) {
                this.modal.confirm(`Are you sure you want to revoke API key for user '${this.usernameToRevoke}'?`)
                        .then(() => {
                            this.userProfileDao.revokeApiKey({}, {username: this.usernameToRevoke});
                        });
            }
            else {
                this.artifactoryNotifications.create({error: `User '${this.usernameToRevoke}' has no valid API key`});
            }
        });

    }

    revokeApiKeys() {
        this.modal.confirm(`Are you sure you want to revoke all users API keys?`)
                .then(() => {
                    this.userProfileDao.revokeApiKey({deleteAll: 1});
                });
    }

    unlockAllUsers() {
        this.adminSecurityGeneralDao.unlockAllUsers();
    }
}