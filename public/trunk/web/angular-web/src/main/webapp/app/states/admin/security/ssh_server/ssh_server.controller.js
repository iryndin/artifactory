import EVENTS     from '../../../../constants/common_events.constants';
import API from '../../../../constants/api.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminSecuritySshServerController {

    constructor($timeout, FileUploader, SshServerDao,ArtifactoryEventBus,ArtifactoryModelSaver, ArtifactoryNotifications) {
        this.$timeout = $timeout;
        this.FileUploader = FileUploader;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.SshServerDao = SshServerDao.getInstance();
        this.TOOLTIP = TOOLTIP.admin.security.SSHSERVER;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['sshServer']);
        this.publicKeyValue = 'No public key installed';
        this.privateKeyValue = 'No private key installed';
        this.getSshData();
        this.initKeys();
    }

    initKeys() {
        this.uploaderPublicKey = new this.FileUploader();
        this.uploaderPublicKey.onSuccessItem = this.onUploadPublicKeySuccess.bind(this);
        this.uploaderPublicKey.url = `${API.API_URL}/sshserver/install?public=true`;
        this.uploaderPublicKey.removeAfterUpload = true;
        this.uploaderPrivateKey = new this.FileUploader();
        this.uploaderPrivateKey.url = `${API.API_URL}/sshserver/install?public=false`;
        this.uploaderPrivateKey.onSuccessItem = this.onUploadPrivateKeySuccess.bind(this);
        this.uploaderPrivateKey.removeAfterUpload = true;
    }

    getSshData(updateKeysStateOnly = false) {
        this.SshServerDao.get().$promise.then((sshServer)=> {
            if (!updateKeysStateOnly) this.sshServer = sshServer;
            this.publicKeyInstalled = sshServer.serverKey && sshServer.serverKey.publicKeyInstalled;
            this.privateKeyInstalled = sshServer.serverKey && sshServer.serverKey.privateKeyInstalled;
            this.publicKeyValue = this.publicKeyInstalled ? 'Public key is installed' : 'No public key installed';
            this.privateKeyValue = this.privateKeyInstalled ? 'Private key is installed' : 'No private key installed';
            this.publicKeyLink = sshServer.serverKey ? sshServer.serverKey.publicKeyLink : undefined;
            this.passPhrase = sshServer.serverKey ? sshServer.serverKey.passPhrase : undefined;
            if (!updateKeysStateOnly) this.artifactoryModelSaver.save();
            this.artifactoryEventBus.dispatch(EVENTS.FORM_CLEAR_FIELD_VALIDATION, true);
        });
    }

    reset() {
        this.artifactoryModelSaver.ask().then(()=>{
            this.getSshData();
        });
    }
    save() {
        this.SshServerDao.update(this.sshServer).$promise.then(()=>{
            this.artifactoryModelSaver.save();
        });
    }

    onUploadPublicKeySuccess(fileDetails, response) {
        this.getSshData(true);
        this.artifactoryNotifications.create(response.feedbackMsg);
    }

    onUploadPrivateKeySuccess(fileDetails, response) {
        this.getSshData(true);
        this.artifactoryNotifications.create(response.feedbackMsg);
    }

    upload(type) {
        if (type === 'public') {
            this.uploaderPublicKey.queue[0].upload();
        }
        if (type === 'private') {
            this.uploaderPrivateKey.queue[0].upload();
        }
    }

    removeKey(isPublic) {
        this.SshServerDao.delete({public: isPublic}).$promise.then((result) => this.getSshData(true));
    }

    verifyPhrase(shouldNotify = true) {
        let method = shouldNotify ? 'post' : 'postWithoutNotifications';
        if (this.SshServerDao[method])
            return this.SshServerDao[method]({action: 'verify', passPhrase: this.passPhrase}).$promise;
    }

    updatePhrase() {
        let verifyPromise = this.verifyPhrase(false);
        if (verifyPromise) {
            verifyPromise
                    .then(() => {
                this.SshServerDao.put({action: 'update', passPhrase: this.passPhrase});
        })
        .catch((response) => this.artifactoryNotifications.create(response.data));
            ;
        }
    }

    canUploadSshKey(uploader) {
        return this[uploader].queue.length;
    }

    canUpdatePhrase() {
        return this.publicKeyInstalled && this.privateKeyInstalled && this.passPhrase;
    }
}