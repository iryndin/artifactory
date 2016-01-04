import EVENTS from '../constants/artifacts_events.constants';
import SNIPPETS from '../constants/setmeup_snippets.constants';
import FIELD_OPTIONS from '../constants/field_options.constats';

export class SetMeUpModal {

    constructor(ArtifactoryModal, ArtifactoryState, SetMeUpDao, ArtifactDeployDao, RepoDataDao, ArtifactoryEventBus, ArtifactoryNotifications, FilteredResourceDao,
                RepositoriesDao, ReverseProxiesDao, ArtifactoryFeatures, ArtifactViewsDao, User, UserProfileDao, parseUrl, $sce, $rootScope, $timeout, $compile) {
        this.modal = ArtifactoryModal;
        this.setMeUpDao = SetMeUpDao;
        this.artifactDeployDao = ArtifactDeployDao;
        this.userProfileDao = UserProfileDao;
        this.repoDataDao = RepoDataDao;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.filteredResourceDao = FilteredResourceDao;
        this.repositoriesDao = RepositoriesDao;
        this.reverseProxiesDao = ReverseProxiesDao;
        this.artifactoryFeatures = ArtifactoryFeatures;
        this.artifactViewsDao = ArtifactViewsDao;
        this.artifactoryState = ArtifactoryState;
        this.user = User.getCurrent();
        this.parseUrl = parseUrl;
        this.$sce = $sce;
        this.$rootScope = $rootScope;
        this.$timeout = $timeout;
        this.$compile = $compile;

        this.repoPackageTypes = FIELD_OPTIONS.repoPackageTypes.slice(0);//make a copy

        this._removeP2();
        this._removeDisabledFeatures();

    }

    _removeP2(){
        for (let i = 0; i < this.repoPackageTypes.length; i++) {
            if (this.repoPackageTypes[i].value.toLowerCase() == "p2") {
                this.repoPackageTypes.splice(i, 1);
            }
        }
    }

    _removeDisabledFeatures() {
        this.repoPackageTypes = _.filter(this.repoPackageTypes,
            (item) => !this.artifactoryFeatures.isDisabled(item.value));
    }

    launch(node) {
        this.node = node;
        this._initSetMeUpScope();
        this.modalInstance = this.modal.launchModal('set_me_up_modal', this.setMeUpScope);
    }

    _getSetMeUpData() {
        this.setMeUpDao.get().$promise.then((data)=> {
//            let url = new URL(data.baseUrl) //CAUSES PROBLEM ON IE, NOT REALY NEEDED...

            let parser = this.parseUrl(data.baseUrl);
            this.setMeUpScope.baseUrl = parser.href;
            this.setMeUpScope.host = this.artifactoryFeatures.isAol() ? parser.host.split(':')[0] : parser.host; //split by ':' in aol to remove the port number that IE returns in .host
            this.setMeUpScope.serverId = data.serverId;
            this.setMeUpScope.protocol = parser.protocol+'//';
            this.setMeUpScope.path = parser.pathname;

            data.repoKeyTypes.sort((a,b) => {
                return (a.repoKey > b.repoKey)?1:-1;
            });
            this.setMeUpScope.reposAndTypes = data.repoKeyTypes.map((item) => {
                return { text : item.repoKey, value : item.repoType.toLowerCase(), read : item.canRead, deploy: item.canDeploy, local: item.isLocal, remote: item.isRemote, virtual: item.isVirtual, defaultDeploymentConfigured: item.isDefaultDeploymentConfigured }
            });

            // Select the repo according to current node
            for (let i = 0; i < this.setMeUpScope.reposAndTypes.length; i++) {
                if (this.setMeUpScope.reposAndTypes[i].text.toLowerCase() == this.setMeUpScope.node.text.toLowerCase() ||
                    this.setMeUpScope.reposAndTypes[i].text.concat("-cache").toLowerCase() == this.setMeUpScope.node.text.toLowerCase()) {
                    this.setMeUpScope.selection.repo = this.setMeUpScope.reposAndTypes[i];
                    this.setMeUpScope.resolveSnippet();
                    break;
                }
            }

            let repoData = this._getRepoData(this.setMeUpScope);

            //Populate general snippets
            this._setGeneralSnippets(repoData);

            this._setRepositories(this.setMeUpScope);

            this._setShowSettings(this.setMeUpScope);

        })
    }

    _initSetMeUpScope() {
        let setMeUpDao = this.setMeUpDao;
        this.setMeUpScope = this.$rootScope.$new();

        this.setMeUpScope.settingPage = false;

        this.setMeUpScope.id = this.setMeUpScope.$id;
        this.setMeUpScope.$sce = this.$sce;
        this.setMeUpScope.settings = {};
        this.setMeUpScope.selection = {};
        this.setMeUpScope.close = ()=>this.modalInstance.close();
        this.setMeUpScope.title = "Set Me Up";
        this.setMeUpScope.shownRepos = [];
        this.setMeUpScope.deploySnippets = [];
        this.setMeUpScope.readSnippets = [];
        this.setMeUpScope.generalSnippets = [];

        this.setMeUpScope.node = this.node.data.getRoot();

        this._prepareSnippets();

        let previousInjectionData = this.artifactoryState.getState('setMeUpUserData');
        if (previousInjectionData) {
            this.injectionData = previousInjectionData;
            this.useApiKey = !!previousInjectionData.apiKey;
            this.setMeUpScope.userDataInjected = true;
            this._getUserData(null,true);
        }
        else {
            this.injectionData = {};
            this._getUserData();
        }




        this.setMeUpScope.repoTypes = this.repoPackageTypes;

        // Select the repo type according to current node
        for (let i = 0; i < this.setMeUpScope.repoTypes.length; i++) {
            if (this.setMeUpScope.node.repoPkgType.toLowerCase() == this.setMeUpScope.repoTypes[i].value.toLowerCase()) {
                this.setMeUpScope.selection.repoType = this.setMeUpScope.repoTypes[i];
                break;
            }
        }

        this._getSetMeUpData();

        let sc = this.setMeUpScope;

        this.setMeUpScope.$watch('selection', () => {
            if (sc.generateSettings && sc.snippet) sc.generateBuildSettings();
        }, true);


        this.setMeUpScope.me = () => {
            let scope = this.setMeUpScope;
            while (scope.$id != this.setMeUpScope.id && scope.$parent) {
                scope = scope.$parent;
            }
            return scope;
        };


        this.setMeUpScope.canInjectUserData = this.user.existsInDB && this.user.name !== 'anonymous' && !(this.user.requireProfileUnlock && !this.user.requireProfilePassword);


        this.setMeUpScope.injection = {};
        this.setMeUpScope.gotoInjectionMode = () => {
            if (this.user.requireProfileUnlock === false) {
                this.setMeUpScope.injection.password = '';
                this.setMeUpScope.injectUserData();
            }
            else {
                this.setMeUpScope.injectionMode = true;
                this.setMeUpScope.toggleInjectUserData(true);
            }
        };
        this.setMeUpScope.cancelInjection = () => {
            this.setMeUpScope.injectionMode = false;
            this.setMeUpScope.toggleInjectUserData(false);
        };
        this.setMeUpScope.injectUserData = () => {

            this._getUserData(this.setMeUpScope.injection.password,true);

            this.setMeUpScope.injectionMode = false;
            this.setMeUpScope.toggleInjectUserData(false);

            this.setMeUpScope.injection.password = '';

        };

        this.setMeUpScope.removeUserData = () => {
            this._prepareSnippets();
            this.artifactoryState.removeState('setMeUpUserData');
        };

        this.setMeUpScope.toggleInjectUserData = (bShow) => {
            if (bShow)
                $('#insert-credentials-box').show().animate({
                    width: '295px',
                    height: '62px'
                }, 400, function() {
                    $(this).find('.input-text').focus();
                });
            else
                $('#insert-credentials-box').hide().css('width', 0).css('height', 0).find('.icon-clear').hide();
        };


        this.setMeUpScope.checkLayoutSettings = (settings, repoType) => {
            if (this.setMeUpScope.select && this.setMeUpScope.select.selected) {
                if (repoType == 'ivy') {
                    this.setMeUpScope.selection.gradle[settings + 'UseIvy'] = true;
                    this.setMeUpScope.selection.gradle[settings + 'UseMaven'] = false;
                }
                else if (repoType == 'maven') {
                    this.setMeUpScope.selection.gradle[settings + 'UseMaven'] = true;
                    this.setMeUpScope.selection.gradle[settings + 'UseIvy'] = false;
                }
            }
            else {
                if (repoType == 'ivy') {
                    if (!this.setMeUpScope.selection.gradle[settings + 'UseMaven']) {
                        this.setMeUpScope.selection.gradle[settings + 'UseMaven'] = true;
                    }
                }
                else if (repoType == 'maven') {
                    if (!this.setMeUpScope.selection.gradle[settings + 'UseIvy']) {
                        this.setMeUpScope.selection.gradle[settings + 'UseIvy'] = true;
                    }
                }
            }
        };

        this.setMeUpScope.getMavenProps = () => {
            let scope = this.setMeUpScope.me();
            return JSON.stringify({
                release: scope.selection.maven.releases,
                snapshot: scope.selection.maven.snapshots,
                pluginRelease: scope.selection.maven.pluginReleases,
                pluginSnapshot: scope.selection.maven.pluginSnapshots,
                mirror: (scope.selection.maven.mirror) ? scope.selection.maven.mirrorAny : ''
            })
        };

        this.setMeUpScope.getGradleProps = () => {
            let scope = this.setMeUpScope.me();
            return JSON.stringify({
                pluginRepoKey: scope.selection.gradle.pluginResolver,
                libsResolverRepoKey: scope.selection.gradle.libsResolver,
                libsPublisherRepoKey: scope.selection.gradle.libsPublisher,
                pluginUseMaven: scope.selection.gradle.pluginUseMaven,
                resolverUseMaven: scope.selection.gradle.libsUseMaven,
                publisherUseMaven: scope.selection.gradle.publishUseMaven,
                pluginUseIvy: scope.selection.gradle.pluginUseIvy,
                resolverUseIvy: scope.selection.gradle.libsUseIvy,
                publisherUseIvy: scope.selection.gradle.publishUseIvy,
                pluginResolverLayout: scope.selection.gradle.pluginLayout,
                libsResolverLayout: scope.selection.gradle.libsLayout,
                libsPublisherLayouts: scope.selection.gradle.publishLayout
            })
        };

        this.setMeUpScope.getIvyProps = () => {
            let scope = this.setMeUpScope.me();
            return JSON.stringify({
                libsRepo: scope.selection.ivy.libsRepository,
                libsRepoLayout: scope.selection.ivy.libsRepositoryLayout,
                libsResolverName: scope.selection.ivy.libsResolverName,
                useIbiblioResolver: !!(scope.selection.ivy.ibiblio),
                m2Compatible: !!(scope.selection.ivy.maven2)
            })
        };

        this.setMeUpScope.generateBuildSettings = () => {
            let scope = this.setMeUpScope.me();
            if (!scope.generate) return false;

            if (scope.generate.maven) {
                setMeUpDao.maven_snippet({
                    release: scope.selection.maven.releases,
                    snapshot: scope.selection.maven.snapshots,
                    pluginRelease: scope.selection.maven.pluginReleases,
                    pluginSnapshot: scope.selection.maven.pluginSnapshots,
                    mirror: (scope.selection.maven.mirror) ? scope.selection.maven.mirrorAny : ''
                }).$promise.then((result)=> {
                        scope.snippet = result.mavenSnippet;
                    })
            }
            else if (scope.generate.gradle) {
                setMeUpDao.gradle_snippet({
                    pluginRepoKey: scope.selection.gradle.pluginResolver,
                    libsResolverRepoKey: scope.selection.gradle.libsResolver,
                    libsPublisherRepoKey: scope.selection.gradle.libsPublisher,
                    pluginUseMaven: scope.selection.gradle.pluginUseMaven,
                    resolverUseMaven: scope.selection.gradle.libsUseMaven,
                    publisherUseMaven: scope.selection.gradle.publishUseMaven,
                    pluginUseIvy: scope.selection.gradle.pluginUseIvy,
                    resolverUseIvy: scope.selection.gradle.libsUseIvy,
                    publisherUseIvy: scope.selection.gradle.publishUseIvy,
                    pluginResolverLayout: scope.selection.gradle.pluginLayout,
                    libsResolverLayout: scope.selection.gradle.libsLayout,
                    libsPublisherLayouts: scope.selection.gradle.publishLayout
                }).$promise.then((result)=> {
                        scope.snippet = result.gradleSnippet;
                    })
            }
            else if (scope.generate.ivy) {
                setMeUpDao.ivy_snippet({
                    libsRepo: scope.selection.ivy.libsRepository,
                    libsRepoLayout: scope.selection.ivy.libsRepositoryLayout,
                    libsResolverName: scope.selection.ivy.libsResolverName,
                    useIbiblioResolver: !!(scope.selection.ivy.ibiblio),
                    m2Compatible: !!(scope.selection.ivy.maven2)
                }).$promise.then((result)=> {
                        scope.snippet = result.ivySnippet;
                    })
            }
        };

        this.setMeUpScope.filterByType = () => {
            if (!this.setMeUpScope.reposAndTypes) return false;

            let scope = this.setMeUpScope.me();
            scope.settingPage = false;
            if (scope.selection && scope.selection.repo) {
                scope.selection.repo = null;
            }
            scope.snippet = scope.readSnippet = scope.deploySnippet = null;
            scope.generateSettings = false;
            scope.generate = {};

            scope.deploySettingsMode = false;


            scope.generalSnippets = [];
            scope.readSnippets = [];
            scope.deploySnippets = [];

            this._setShowSettings(scope);
            this._setRepositories(scope);
            this._selectRepoByType(scope);
            let repoData = this._getRepoData(scope);
            //Populate general snippets
            this._setGeneralSnippets(repoData);
        };

        this.setMeUpScope.getGeneratorRepos = (type) => {
            let scope = this.setMeUpScope.me();
            scope.settingPage = true;
            if (!scope.generate) scope.generate = {};

            scope.readSnippet = scope.deploySnippet = null;

            switch (type) {
                case 'Maven':
                    setMeUpDao.maven().$promise.then((result)=> {
                        scope.generateSettings = true;
                        scope.generate = {maven: true};
                        scope.settings.maven = result;
                        this.setMeUpScope.selection.maven = {
                            releases: scope.settings.maven.releases[0],
                            snapshots: scope.settings.maven.snapshots[0],
                            pluginReleases: scope.settings.maven.pluginReleases[0],
                            pluginSnapshots: scope.settings.maven.pluginSnapshots[0],
                            mirrorAny: scope.settings.maven.anyMirror[0],
                            mirror: false
                        };
                    });
                    break;
                case 'Gradle':
                    setMeUpDao.gradle().$promise.then((result)=> {
                        scope.generateSettings = true;
                        scope.generate = {gradle: true};
                        scope.settings.gradle = result;
                        this.setMeUpScope.selection.gradle = {
                            pluginResolver: scope.settings.gradle.pluginResolver[0],
                            pluginUseMaven: true,
                            pluginUseIvy: false,
                            pluginLayout: scope.settings.gradle.layouts[0],
                            libsResolver: scope.settings.gradle.libsResolver[0],
                            libsUseMaven: true,
                            libsUseIvy: false,
                            libsLayout: scope.settings.gradle.layouts[0],
                            libsPublisher: scope.settings.gradle.libsPublisher[0],
                            publishUseMaven: true,
                            publishUseIvy: false,
                            publishLayout: scope.settings.gradle.layouts[0]
                        };
                    });
                    break;
                case 'Ivy':
                    setMeUpDao.ivy().$promise.then((result)=> {
                        scope.generateSettings = true;
                        scope.generate = {ivy: true};
                        scope.settings.ivy = result;
                        this.setMeUpScope.selection.ivy = {
                            libsRepository: scope.settings.ivy.libsRepository[0],
                            libsRepositoryLayout: scope.settings.ivy.libsRepositoryLayout[0],
                            ibiblio: true,
                            maven2: true
                        }
                    });
                    break;
                default:
                    scope.generateSettings = false;
                    break;
            }

        };

        this.setMeUpScope.resolveSnippet = (resolveDockerReverseProxy) => {
            if (resolveDockerReverseProxy === undefined) resolveDockerReverseProxy = true;

            if (!this.setMeUpScope.selection.repoType) {
                return;
            }
            let scope = this.setMeUpScope.me();
            let repoData = this._getRepoData(scope);
            let repoType = this.setMeUpScope.selection.repoType.value;

            scope.deploySnippets = [];
            scope.readSnippets = [];
            scope.generalSnippets = [];

            if (this.setMeUpScope.snippets[repoType]) {
                this._setDeploySnippets(repoData);
                this._setReadSnippets(repoData);
                this._setGeneralSnippets(repoData);
            }

            //Warn the user if he doesn't have deploy permissions
            if(!repoData.deploy && (repoData.local || repoData.defaultDeploymentConfigured)) {
                scope.generalSnippets.push({
                    title: this.setMeUpScope.$sce.trustAsHtml("<b>You don't have deploy permissions on this repository!<b/>")
                });
            }

            if (this.setMeUpScope.selection.repoType.value === 'docker' && resolveDockerReverseProxy && !this.artifactoryFeatures.isAol() && !this.artifactoryFeatures.isOss() && this.user.name !== 'anonymous') {

                this.artifactViewsDao.getDockerProxySnippet({},{repoKey: "dummy" /*this.selection.repo.text*/}).$promise.then((data)=>{
                    this.setMeUpDao.reverse_proxy_data({repoKey: this.setMeUpScope.selection.repo.text}).$promise.then((reverseProxiesData)=>{

                        if (reverseProxiesData.methodSelected) this.setMeUpScope.reverseProxySnippet = data.template;

                        let snip;
                        if (reverseProxiesData.usingPorts) snip = `${reverseProxiesData.serverName}:${reverseProxiesData.repoPort || '<port>'}`;
                        else snip = `${this.setMeUpScope.selection.repo.text}.${reverseProxiesData.serverName}`;

                        if (reverseProxiesData.methodSelected && !reverseProxiesData.usingHttps) {
                            this.setMeUpScope.snippets.docker.general.title = "Using Docker with Artifactory requires a reverse proxy such as Nginx or Apache.<br/>For more details please visit our <a href=\"http://www.jfrog.com/confluence/display/RTF/Docker+Repositories#DockerRepositories-RequirementforaReverseProxy(Nginx/Apache)\" target=\"_blank\">documentation</a>.<br>Not using an SSL certificate requires Docker clients to add an --insecure-registry flag to the <b>DOCKER_OPTS</b>";
                            this.setMeUpScope.snippets.docker.general.snippet = `export DOCKER_OPTS+=" --insecure-registry ${snip}"`;
                        }
                        else {
                            this.setMeUpScope.snippets.docker.general.title = "Using Docker with Artifactory requires a reverse proxy such as Nginx or Apache.<br/>For more details please visit our <a href=\"http://www.jfrog.com/confluence/display/RTF/Docker+Repositories#DockerRepositories-RequirementforaReverseProxy(Nginx/Apache)\" target=\"_blank\">documentation</a>.";
                            delete this.setMeUpScope.snippets.docker.general.snippet;
                        }
                        this.setMeUpScope.resolveSnippet(false);
                    });
                });
            }
            else if (resolveDockerReverseProxy){
                delete this.setMeUpScope.reverseProxySnippet;
                delete this.setMeUpScope.snippets.docker.general.snippet;
            }
        };



        this.setMeUpScope.setDeploySettingsMode = () => {

            let defaultTargetPath;

            switch (this.setMeUpScope.selection.repoType.value) {
                case "maven":
                    defaultTargetPath="settings.xml";
                    break;
                case "gradle":
                    defaultTargetPath="build.gradle";
                    break;
                case "ivy":
                    defaultTargetPath="ivysettings.xml";
                    break;
            }

            this.setMeUpScope.deploySettingsMode = true;
            this.setMeUpScope.snippetDeploy = {
                targetPath:  defaultTargetPath,
                targetRepo: ''
            };

            this.repoDataDao.get({user: 'true'}).$promise.then((result)=> {
                this.setMeUpScope.snippetDeploy.reposList = result.repoTypesList;
            });

        };

        this.setMeUpScope.deploySettingsSnippet = () => {
            let doActualDeployment;
            let scope = this.setMeUpScope.me();
            if (scope.generate.maven) {
                setMeUpDao.maven_snippet({deploy:true},{
                    release: scope.selection.maven.releases,
                    snapshot: scope.selection.maven.snapshots,
                    pluginRelease: scope.selection.maven.pluginReleases,
                    pluginSnapshot: scope.selection.maven.pluginSnapshots,
                    mirror: (scope.selection.maven.mirror) ? scope.selection.maven.mirrorAny : ''
                }).$promise.then((result)=> {
                        doActualDeployment(result);
                    })
            }
            else if (scope.generate.gradle) {
                setMeUpDao.gradle_snippet({deploy:true},{
                    pluginRepoKey: scope.selection.gradle.pluginResolver,
                    libsResolverRepoKey: scope.selection.gradle.libsResolver,
                    libsPublisherRepoKey: scope.selection.gradle.libsPublisher,
                    pluginUseMaven: scope.selection.gradle.pluginUseMaven,
                    resolverUseMaven: scope.selection.gradle.libsUseMaven,
                    publisherUseMaven: scope.selection.gradle.publishUseMaven,
                    pluginUseIvy: scope.selection.gradle.pluginUseIvy,
                    resolverUseIvy: scope.selection.gradle.libsUseIvy,
                    publisherUseIvy: scope.selection.gradle.publishUseIvy,
                    pluginResolverLayout: scope.selection.gradle.pluginLayout,
                    libsResolverLayout: scope.selection.gradle.libsLayout,
                    libsPublisherLayouts: scope.selection.gradle.publishLayout
                }).$promise.then((result)=> {
                        doActualDeployment(result);
                    })
            }
            else if (scope.generate.ivy) {
                setMeUpDao.ivy_snippet({deploy:true},{
                    libsRepo: scope.selection.ivy.libsRepository,
                    libsRepoLayout: scope.selection.ivy.libsRepositoryLayout,
                    libsResolverName: scope.selection.ivy.libsResolverName,
                    useIbiblioResolver: !!(scope.selection.ivy.ibiblio),
                    m2Compatible: !!(scope.selection.ivy.maven2)
                }).$promise.then((result)=> {
                        doActualDeployment(result);
                    })
            }


            doActualDeployment = (config) => {
                let singleDeploy = {};

                singleDeploy.action = "deploy";
                singleDeploy.unitInfo = {
                    artifactType: "base",
                    path: this.setMeUpScope.snippetDeploy.targetPath
                };
                singleDeploy.fileName = config.savedSnippetName;
                singleDeploy.repoKey = this.setMeUpScope.snippetDeploy.targetRepo.repoKey;

                this.artifactDeployDao.post(singleDeploy).$promise.then((result)=> {
                    if (result.data) {
                        this.artifactoryEventBus.dispatch(EVENTS.TREE_REFRESH);
                        this.artifactoryNotifications.createMessageWithHtml({
                            type: 'success',
                            body: `<div id="toaster-with-link">Successfully deployed <a ui-sref="artifacts.browsers.path({tab: 'General', browser: 'tree', artifact: '${result.data.repoKey}/${result.data.artifactPath}'})">${result.data.artifactPath} into ${result.data.repoKey}</a></div>`,
                            timeout: 10000
                        });
                        this.$timeout(()=>{ //compile the element, so the ui-sref will work
                            let e = angular.element($('#toaster-with-link'));
                            this.$compile(e)(this.$rootScope);
                        });

                        this.filteredResourceDao.setFiltered({setFiltered: true},{
                            repoKey: result.data.repoKey,
                            path: result.data.artifactPath
                        });
                    }
                });
            }
        };

    }

    _fixTPL(tpl) {
        let temp = tpl;
        let protocol;
        if (_.contains(tpl, 'http://')) {
            protocol = 'http://';
        }
        else if (_.contains(tpl, 'https://')) {
            protocol = 'https://';
        }
        temp = temp.split(protocol).join('@@protocol@@');
        temp = temp.split('//').join('/');
        temp = temp.split('@@protocol@@').join(this.setMeUpScope.protocol);

        if (_.contains(temp, this.setMeUpScope.host + "/artifactory") && this.setMeUpScope.path !== "/artifactory") {
            temp = temp.replace(this.setMeUpScope.host + "/artifactory", this.setMeUpScope.host + this.setMeUpScope.path);
        }

        return temp;
    }

    _setShowSettings(scope) {
        let selection = this.setMeUpScope.selection;
        if (scope.selection && selection.repoType && scope.selection.repoType.value.match('(ivy|maven|gradle)')) {
            scope.showSettings = selection.repoType.text;
        }
        else {
            scope.showSettings = false;
        }
    }

    _setRepositories(scope) {
        scope.shownRepos = this.setMeUpScope.reposAndTypes.filter((d) => {
            if (!this.setMeUpScope.selection || !this.setMeUpScope.selection.repoType || this.setMeUpScope.selection.repoType.value == 'generic') return d;
            if (this.setMeUpScope.selection.repoType.value == 'maven' && !d.local && !d.defaultDeploymentConfigured) return false;
            let isRepoMavenish = this.setMeUpScope.selection.repoType.value.match(/(maven|ivy|gradle|sbt)/gi) ? true : false;
            let isSelectionMavenish = d.value.match(/(maven|ivy|gradle|sbt)/gi) ? true : false;
            if (d.value == this.setMeUpScope.selection.repoType.value || d.value == this.setMeUpScope.selection.repoType.value
                || (isRepoMavenish && isSelectionMavenish)) return d;
        })
    }

    _selectRepoByType(scope) {
        // Select the repo according to current node
        for (let i = 0; i < scope.reposAndTypes.length; i++) {
            if (scope.reposAndTypes[i].value.toLowerCase() == scope.selection.repoType.value) {
                scope.selection.repo = scope.reposAndTypes[i];
                scope.resolveSnippet();
                break;
            }
        }
    }

    _getRepoData(scope) {
        let repoData = this.setMeUpScope.reposAndTypes.filter((item) => {
            if (scope.selection.repo && item.text == scope.selection.repo.text) {
                return item;
            }
        });
        repoData = (repoData.length > 0) ? repoData[0] : null;

        return repoData;
    }

    _setDeploySnippets(repoData) {
        let scope = this.setMeUpScope.me();
        let repoType = this.setMeUpScope.selection.repoType.value;

        // Maven from server
        if (repoType == 'maven') {
            scope.deploySnippets = [];
            this.setMeUpDao.maven_distribution({repoKey: repoData.text}).$promise.then((result)=> {
                if (repoData.local || repoData.defaultDeploymentConfigured) {
                    scope.deploySnippets.push({
                        before: (this.setMeUpScope.snippets[repoType]['deploy']) ? this.setMeUpScope.snippets[repoType]['deploy']['before'] : '',
                        snippet: result.distributedManagement,
                        after: (this.setMeUpScope.snippets[repoType]['deploy']) ? this.setMeUpScope.snippets[repoType]['deploy']['after'] : ''
                    })
                }
            })
        }

        if (repoType != 'maven' && (repoData.local || repoData.defaultDeploymentConfigured) && this.setMeUpScope.snippets[repoType]['deploy']) {
            scope.deploySnippets = [];
            if (this.setMeUpScope.snippets[repoType]['deploy'] instanceof Array) {
                for (let i = 0; i < this.setMeUpScope.snippets[repoType]['deploy'].length; i++) {
                    let tpl = (this.setMeUpScope.snippets[repoType]['deploy']) ?
                        this.setMeUpScope.snippets[repoType]['deploy'][i]['snippet'] : null;
                    if (tpl) {
                        tpl = tpl.replace(/\$1/g, repoData.text).replace(/\$2/g, this.setMeUpScope.baseUrl).replace(/\$3/g,
                            this.setMeUpScope.serverId).replace(/\$4/g, this.setMeUpScope.host);
                        tpl = this._fixTPL(tpl);
                        scope.deploySnippets.push({
                            before: this.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['deploy'][i]['before']),
                            snippet: tpl,
                            after: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['deploy'][i]['after'])
                        })
                    }
                }
            }
            else {
                let tpl = (this.setMeUpScope.snippets[repoType]['deploy']) ? this.setMeUpScope.snippets[repoType]['deploy']['snippet'] : null;
                if (tpl) {
                    tpl = tpl.replace(/\$1/g, repoData.text).replace(/\$2/g, this.setMeUpScope.baseUrl).replace(/\$3/g,
                        this.setMeUpScope.serverId).replace(/\$4/g, this.setMeUpScope.host);
                    tpl = this._fixTPL(tpl);
                    scope.deploySnippets.push({
                        before: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['deploy']['before']),
                        snippet: tpl,
                        after: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['deploy']['after'])
                    })
                }
            }
        }
    }

    _setReadSnippets(repoData) {
        let scope = this.setMeUpScope.me();
        scope.readSnippets = [];
        let repoType = this.setMeUpScope.selection.repoType.value;

        if (repoData.read && this.setMeUpScope.snippets[repoType]['read']) {
            if (this.setMeUpScope.snippets[repoType]['read'] instanceof Array) {
                for (let i = 0; i < this.setMeUpScope.snippets[repoType]['read'].length; i++) {
                    let tpl = (this.setMeUpScope.snippets[repoType]['read']) ?
                        this.setMeUpScope.snippets[repoType]['read'][i]['snippet'] : null;
                    if (tpl) {
                        tpl = tpl.replace(/\$1/g, repoData.text).replace(/\$2/g, this.setMeUpScope.baseUrl).replace(/\$3/g,
                            this.setMeUpScope.serverId).replace(/\$4/g, this.setMeUpScope.host);
                        tpl = this._fixTPL(tpl);
                        scope.readSnippets.push({
                            before: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['read'][i]['before']),
                            snippet: tpl,
                            after: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['read'][i]['after'])
                        });
                    }
                }
            }
            else {
                let tpl = (this.setMeUpScope.snippets[repoType]['read']) ? this.setMeUpScope.snippets[repoType]['read']['snippet'] : null;
                if (tpl) {
                    tpl = tpl.replace(/\$1/g, repoData.text).replace(/\$2/g, this.setMeUpScope.baseUrl).replace(/\$3/g,
                        this.setMeUpScope.serverId).replace(/\$4/g, this.setMeUpScope.host);
                    tpl = this._fixTPL(tpl);
                    scope.readSnippets.push({
                        before: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['read']['before']),
                        snippet: tpl,
                        after: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['read']['after'])
                    });
                }
            }
        }
    }

    _setGeneralSnippets(repoData) {
        if (!this.setMeUpScope.selection.repoType) {
            return;
        }
        let scope = this.setMeUpScope.me();
        let repoType = this.setMeUpScope.selection.repoType.value;

        scope.generalSnippets = [];
        if (this.setMeUpScope.snippets[repoType]['general']) {
            if (this.setMeUpScope.snippets[repoType]['general'] instanceof Array) {
                for (let i = 0; i < this.setMeUpScope.snippets[repoType]['general'].length; i++) {
                    let tpl = (this.setMeUpScope.snippets[repoType]['general']) ?
                        this.setMeUpScope.snippets[repoType]['general'][i]['snippet'] : null;
                    if (tpl && repoData) {
                        tpl = tpl.replace(/\$1/g, repoData.text).replace(/\$2/g, this.setMeUpScope.baseUrl).replace(/\$3/g,
                            this.setMeUpScope.serverId).replace(/\$4/g, this.setMeUpScope.host);
                        tpl = this._fixTPL(tpl);
                    }
                    scope.generalSnippets.push({
                        title: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['general'][i]['title']),
                        before: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['general'][i]['before']),
                        snippet: tpl,
                        after: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['general'][i]['after'])
                    });
                }
            }
            else {
                let tpl = (this.setMeUpScope.snippets[repoType]['general']) ? this.setMeUpScope.snippets[repoType]['general']['snippet'] : null;
                if (tpl && repoData) {
                    tpl = tpl.replace(/\$1/g, repoData.text).replace(/\$2/g, this.setMeUpScope.baseUrl).replace(/\$3/g,
                        this.setMeUpScope.serverId).replace(/\$4/g, this.setMeUpScope.host);
                    tpl = this._fixTPL(tpl);
                }
                scope.generalSnippets.push({
                    title: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['general']['title']),
                    before: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['general']['before']),
                    snippet: tpl,
                    after: this.setMeUpScope.$sce.trustAsHtml(this.setMeUpScope.snippets[repoType]['general']['after'])
                });
            }
        }
    }

    _prepareSnippets(injectUserData) {
        let stringified = JSON.stringify(SNIPPETS);

        var curlAuthString = this.useApiKey ? "-H 'X-Api-Key: <API_KEY>'" : "-u<USERNAME>:<PASSWORD>";

        stringified = stringified.split('<CURL_AUTH>').join(curlAuthString);

        if (injectUserData) {
            if (this.injectionData.userName) stringified = stringified.split('<USERNAME>').join(this.injectionData.userName);
            if (this.injectionData.password) stringified = stringified.split('<PASSWORD>').join(this.injectionData.password);
            if (this.injectionData.apiKey) stringified = stringified.split('<API_KEY>').join(this.injectionData.apiKey);
            this.setMeUpScope.userDataInjected = true;
            this.artifactoryState.setState('setMeUpUserData',this.injectionData);
        }
        else {
            this.setMeUpScope.userDataInjected = false;
        }

        this.setMeUpScope.snippets = JSON.parse(stringified);

        if (this.setMeUpScope.filterByType) this.setMeUpScope.filterByType();
    }

    _getUserData(password, inject) {

        let getUnprotected = () => {
            this.userProfileDao.getApiKey().$promise.then((res)=>{
                this.useApiKey = !!res.apiKey;
                this.injectionData.apiKey = res.apiKey;
                this.injectionData.userName = this.user.name;

                if (this.user.requireProfileUnlock === false && !this.useApiKey) this.setMeUpScope.canInjectUserData = false;

                this._prepareSnippets(inject);
            });
        };

        if (password && this.user.requireProfileUnlock !== false) {
            this.userProfileDao.fetch({password: password || ''}).$promise.then(res => {
                this.injectionData.password = res.data.user.password;
                getUnprotected();
            });
        }
        else getUnprotected();

    }

}
