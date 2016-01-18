import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

let $state, $stateParams, ProxiesDao, ArtifactoryModal, ArtifactoryModelSaver;

export class AdminConfigurationProxyFormController {

    constructor(_$state_, _$stateParams_, _ProxiesDao_, _ArtifactoryModal_,_ArtifactoryModelSaver_) {
        ProxiesDao = _ProxiesDao_;
        $stateParams = _$stateParams_;
        $state = _$state_;
        ArtifactoryModal = _ArtifactoryModal_;
        ArtifactoryModelSaver = _ArtifactoryModelSaver_.createInstance(this,['proxy']);;


        this.isNew = !$stateParams.proxyKey;
        this.formTitle = `${this.isNew && 'New' || 'Edit ' + $stateParams.proxyKey } Proxy`;
        this.TOOLTIP = TOOLTIP.admin.configuration.proxyForm;
        this._initProxy();
    }

    _initProxy() {
        if (this.isNew) {
            this.proxy = {};
        }
        else {
            ProxiesDao.getSingleProxy({key: $stateParams.proxyKey}).$promise
                .then((proxy) => {
                        this.proxy = proxy
                        ArtifactoryModelSaver.save();
                    });
        }
    }

    onChangeDefault() {
        if (!this.proxy.defaultProxy) return;
        ArtifactoryModal.confirm('Do you wish to use this proxy with existing remote repositories (and override any assigned proxies)?',
                '',
                {confirm: "OK"})
            .catch(() => this.proxy.defaultProxy = false);
    }

    save() {
        let whenSaved = this.isNew ? ProxiesDao.save(this.proxy) : ProxiesDao.update(this.proxy);
        whenSaved.$promise.then(() => {
            ArtifactoryModelSaver.save();
            this._end()
        });
    }

    cancel() {
        this._end();
    }

    _end() {
        $state.go('^.proxies');
    }
}