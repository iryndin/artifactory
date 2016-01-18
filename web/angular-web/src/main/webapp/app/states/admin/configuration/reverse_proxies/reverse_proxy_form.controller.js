import API from '../../../../constants/api.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

let $state, $stateParams, ReverseProxiesDao, ArtifactoryModal, ArtifactoryModelSaver, ArtifactViewsDao, HaDao, artifactoryIFrameDownload;

export class AdminConfigurationReverseProxyFormController {

    constructor(_$state_, _$stateParams_, _ReverseProxiesDao_, _ArtifactoryModal_,_ArtifactoryModelSaver_, _ArtifactViewsDao_,_HaDao_,_artifactoryIFrameDownload_) {
        ReverseProxiesDao = _ReverseProxiesDao_;
        $stateParams = _$stateParams_;
        $state = _$state_;
        ArtifactoryModal = _ArtifactoryModal_;
        ArtifactoryModelSaver = _ArtifactoryModelSaver_.createInstance(this,['reverseProxy']);
        ArtifactViewsDao = _ArtifactViewsDao_;
        HaDao = _HaDao_;
        artifactoryIFrameDownload = _artifactoryIFrameDownload_;

        this.selectizeConfig = {
            sortField: 'text',
            create: false,
            maxItems: 1
        };

        this.webServerTypeOptions = [
            {text: 'nginx', value: 'NGINX', icon: 'nginx'},
            {text: 'apache', value: 'APACHE', icon: 'apache'}
        ];
        this.dockerReverseProxyMethodSelectizeOptions = [
            {text: '', value: 'NOVALUE'},
            {text: 'Port', value: 'PORTPERREPO'},
            {text: 'Sub Domain', value: 'SUBDOMAIN'}
        ];

        this.formTitle = `Reverse Proxy Configuration`;
        this.TOOLTIP = TOOLTIP.admin.configuration.reverseProxy;

        this._checkHaConfiguration();

        this._initReverseProxy();
    }

    _checkHaConfiguration() {
        HaDao.query().$promise.then((ha)=> {
            this.haConfigured = ha.length > 0;
        });
    }

    _initReverseProxy() {
        ReverseProxiesDao.get({key: 'dummy'}).$promise
            .then((reverseProxy) => {
                    this.reverseProxy = reverseProxy;
                    this.reverseProxy.key = 'nginx';
                    this.reverseProxy.serverNameExpression = '*.'+(this.reverseProxy.serverName ? this.reverseProxy.serverName : '<SERVER_NAME>');

                    this.reverseProxy.httpPort = this.reverseProxy.httpPort || 80;
                    if (this.reverseProxy.publicAppContext === undefined) this.reverseProxy.publicAppContext = 'artifactory';
//                    this.reverseProxy.artifactoryServerName = this.reverseProxy.artifactoryServerName || 'localhost';
                    this.reverseProxy.artifactoryPort = this.reverseProxy.artifactoryPort || 8081;
                    if (this.reverseProxy.artifactoryAppContext === undefined) this.reverseProxy.artifactoryAppContext = 'artifactory';
                    if (!this.reverseProxy.useHttp && !this.reverseProxy.useHttps) this.reverseProxy.useHttp = true;
                    this.reverseProxy.httpsPort = this.reverseProxy.httpsPort || 443;

                    this.reverseProxy.dockerReverseProxyMethod = this.reverseProxy.dockerReverseProxyMethod || 'NOVALUE';

                    this.reverseProxy.webServerType = _.findWhere(this.webServerTypeOptions, {value: this.reverseProxy.webServerType});
                    //this.reverseProxy.webServerType = this.webServerTypeOptions[0];

                    ArtifactoryModelSaver.save();
                });
    }

    onChangeServerName() {
        this.reverseProxy.serverNameExpression = '*.'+(this.reverseProxy.serverName ? this.reverseProxy.serverName : '<SERVER_NAME>');
    }

    save() {


        this.reverseProxy.publicAppContext = this.reverseProxy.publicAppContext.split('/').join('');
        this.reverseProxy.artifactoryAppContext = this.reverseProxy.artifactoryAppContext.split('/').join('');

        let payload = _.cloneDeep(this.reverseProxy);

        payload.webServerType = payload.webServerType.value;
        payload.key = payload.webServerType.toLowerCase();

        if (payload.dockerReverseProxyMethod !== 'SUBDOMAIN') delete payload.serverNameExpression;

        if (!payload.useHttps) {
            delete payload.httpsPort;
            delete payload.sslKey;
            delete payload.sslCertificate;
        }
        if (!payload.useHttp) {
            delete payload.httpPort;
        }

        let whenSaved = ReverseProxiesDao.save(payload);
        whenSaved.$promise.then(() => {
            ArtifactoryModelSaver.save();
//            this._end()
        });
    }

    viewSnippet() {
        if (!this.canViewSnippet()) return;

        ArtifactViewsDao.getDockerProxySnippet({},{repoKey: 'dummy'}).$promise.then((data)=>{
            let message = "To use your reverse proxy configuration, copy the snippet below and place it in the sites-enabled folder and reload your reverse proxy server. This will affect Artifactory's reverse proxy configuration, and Docker repositories if you have any configured."
            ArtifactoryModal.launchCodeModal("Reverse Proxy Snippet", data.template,{name:'text'},message,"Snippet");
        });
    }
    downloadSnippet() {
        if (!this.canViewSnippet()) return;
        artifactoryIFrameDownload(`${API.API_URL}/views/dockerproxy/dummy?download=true`);
    }
    canViewSnippet() {
        return ArtifactoryModelSaver.isModelSaved() && this.reverseProxyEditForm.$valid;
    }

    reset() {
        ArtifactoryModelSaver.ask().then(()=>{
            this._initReverseProxy();
        });

        //        this._end();
    }

/*
    _end() {
        $state.go('^.reverse_proxies');
    }
*/
}