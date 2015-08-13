import EVENTS from '../../constants/artifacts_events.constants';
import API from '../../constants/api.constants';
import TOOLTIP from '../../constants/artifact_tooltip.constant';

class jfDeployController {

    constructor($scope, FileUploader, RepoDataDao, ArtifactDeployDao, ArtifactoryState, ArtifactoryEventBus,
            ArtifactoryNotifications) {

        if (this.comm && this.comm.setController) {
            this.comm.setController(this);
        }
        this.$scope = $scope;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactDeployDao = ArtifactDeployDao;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.repoDataDao = RepoDataDao;
        this.artifactoryState = ArtifactoryState;
        this.currentDeploy = 'single';
        this.FileUploader = FileUploader;
        this.deployFile = {};
        this.errorQueue = [];
        this.multiSuccessMessage = '';
        this.TOOLTIP = TOOLTIP.artifacts.deploy;
        this.deployFile.targetPath = '';
        this.progress = false;
        this.originalDeployPath = '';
        this.initEvent();
        this.initDeploy();

        this.firstInit = true;
        this.uploadCompleted = false;
    }

    initDeploy() {
        let UPLOAD_REST_URL = `${API.API_URL}/artifact/upload`;
        this.uploader = new this.FileUploader();

        this.repoDataDao.get({user: 'true'}).$promise.then((result)=> {
            this.reposList = result.repoTypesList;

            this.uploader.url = UPLOAD_REST_URL;
            this.uploader.onSuccessItem = this.onUploadSuccess.bind(this);
            this.uploader.onAfterAddingAll = this.onAfterAddingAll.bind(this);
            this.uploader.onAfterAddingFile = this.onAfterAddingFile.bind(this);
            this.uploader.onProgressAll = this.onProgressAll.bind(this);
            this.uploader.onErrorItem = this.onUploadError.bind(this);
            this.uploader.onCompleteAll = this.onCompleteAll.bind(this);
            let localRepo;
            localRepo = _.find(this.reposList, ((repo)=> {
                return repo.repoKey == this.node.data.repoKey;
            }));

            var targetPath = this.node.data.path;
            if (this.node.data.isInsideArchive()) {
                targetPath = "";
            }
            else {
                if (this.node.data.type == "file" || this.node.data.type == 'archive') {
                    if (targetPath.indexOf('/') > -1) {
                        targetPath = targetPath.substr(0, targetPath.lastIndexOf('/'))
                    }
                    else if (targetPath.indexOf('\\') > -1) {
                        targetPath = targetPath.substr(0, targetPath.lastIndexOf('\\'))
                    }
                    else {
                        targetPath = "";
                    }
                }
            }
            if(this.firstInit) {
                if(localRepo) {
                    this.deployFile = {
                        repoDeploy: localRepo,
                        targetPath: targetPath
                    }
                } else {
                    this.deployFile = {
                        repoDeploy: this.node.data.type == 'local' ? this.node.this.reposList[0] : '',
                        targetPath: targetPath
                    }
                }
            } else {
                //Reset garbage deployFile if exists
                if(this.deployFile && this.deployFile.unitInfo && this.deployFile.unitInfo.mavenArtifact) {
                    this.deployFile.unitInfo.mavenArtifact = false;
                }
                if(this.deployFile && this.deployFile.unitInfo && this.deployFile.unitInfo.debianArtifact) {
                    this.deployFile.unitInfo.debianArtifact = false;
                }
                this.deployFile.unitInfo = {};
                this.deployFile.fileName = '';
                this.uploader.queue = [];
                this.deployFile.targetPath = targetPath;
            }
            this.uploadCompleted = false;
        });

        this.firstInit = false;
        // set data from currently open node
    }

    multiUploadItemRemoved() {
        if (!this.uploader.queue || !this.uploader.queue.length) {
            this.uploadCompleted = false;
        }
    }

    changeMavenFileType() {
        if (this.deployFile.unitInfo.artifactType == 'maven') {
            this.deployFile.unitInfo.artifactType = 'base';
            this.deployFile.unitInfo.maven = true;
            if(this.originalDeployPath) {
                this.deployFile.targetPath = angular.copy(this.originalDeployPath);
            }
        }
        else {
            this.deployFile.unitInfo.artifactType = 'maven';
            this.originalDeployPath = angular.copy(this.deployFile.targetPath);
            this.updateMavenTargetPath();
        }
    }

    changeDebianFileType() {
        if (this.deployFile.unitInfo.artifactType == 'debian') {
            this.deployFile.unitInfo.artifactType = 'base';
            this.deployFile.unitInfo.debian = true;
        }
        else {
            this.deployFile.unitInfo.artifactType = 'debian';
        }
    }

    deployArtifacts() {
        let singleDeploy = {};
        let DEPLOY_REST_URL = `${API.API_URL}/artifact/deploy/multi`;

        if ((this.deployFile.targetPath.lastIndexOf("/") == -1 ||
                this.deployFile.targetPath.slice(-1) != "/") &&
                this.currentDeploy != 'single') {
            this.deployFile.targetPath += "/";
        }

        if (this.currentDeploy == 'single') {
            singleDeploy.action = "deploy";
            singleDeploy.unitInfo = this.deployFile.unitInfo;
            singleDeploy.unitInfo.path = angular.copy(this.deployFile.targetPath);
            singleDeploy.fileName = this.deployFile.fileName;
            singleDeploy.repoKey = this.deployFile.repoDeploy.repoKey;

            if (this.deployFile.unitInfo.Internal && this.deployFile.unitConfigFileContent) {
                singleDeploy.publishUnitConfigFile = true;
                singleDeploy.unitConfigFileContent = this.deployFile.unitConfigFileContent;
            }
            this.artifactDeployDao.post(singleDeploy).$promise.then((result)=> {
                if (result.data) {
                    this.artifactoryNotifications.createMessageWithHtml(jfDeployController.createNotification(result.data));
                    if (this.onSuccess && typeof this.onSuccess === 'function') {
                        this.onSuccess();
                    }
                    this._dispatchSuccessEvent();
                }
            })
        } else {
            this.uploader.queue.forEach((item)=> {
                item.url = DEPLOY_REST_URL + '?repoKey=' + this.deployFile.repoDeploy.repoKey + '&path=' +
                (this.deployFile.targetPath || '') + this._fixUrlPath(item.file.name);
            });
            this.uploader.uploadAll();
        }
    }

    _fixUrlPath(name) {
        name = name.replace(/&/g, '%26');
        var find = '&';
        var re = new RegExp(find, 'g');
        return name.replace(re, '%26');
    }

    initEvent() {
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.DEPLOY_FILES, (params) => {
            this.onSuccess = params.onSuccess;
            this.deployArtifacts();
        });
    }

    setDeploy(deploy) {
        if (this.comm.needToCancel && this.currentDeploy === 'single' && deploy === 'multi') {
            this.comm.cancelUploadedFile(this.deployFile.fileName);
            this.comm.needToCancel = false;
        }
        this.currentDeploy = deploy;
        this.initDeploy();
    }

    onRemoveSingle() {
        if (this.comm.needToCancel) {
            this.comm.cancelUploadedFile(this.deployFile.fileName);
            this.comm.needToCancel = false;
        }
    }

    isSelectedDeploy(deploy) {
        return this.currentDeploy === deploy;
    }

    updateMavenTargetPath() {
        let newPath = '';
        if (this.deployFile.unitInfo.groupId) {
            newPath += this.deployFile.unitInfo.groupId.replace(/\./g, '/');
        }
        newPath += '/' + (this.deployFile.unitInfo.artifactId || '');
        newPath += '/' + (this.deployFile.unitInfo.version || '');
        newPath += '/' + (this.deployFile.unitInfo.artifactId || '');
        newPath += '-' + (this.deployFile.unitInfo.version || '');
        if (this.deployFile.unitInfo.classifier) {
            newPath += '-' + this.deployFile.unitInfo.classifier;
        }
        newPath += '.' + (this.deployFile.unitInfo.type || '');
        if (typeof window.DOMParser != 'undefined' && typeof window.XMLSerializer != 'undefined'
            && this.deployFile.unitConfigFileContent) {
            //Parse the code mirror model into xml object and modify based on input fields
            let parser = new DOMParser();
            let pomXml = parser.parseFromString(this.deployFile.unitConfigFileContent, "text/xml");
            if (pomXml.getElementsByTagName('groupId').length) {
                if(pomXml.getElementsByTagName('groupId')[0].hasChildNodes()) {
                    pomXml.getElementsByTagName('groupId')[0].childNodes[0].nodeValue =
                            this.deployFile.unitInfo.groupId ? this.deployFile.unitInfo.groupId : '';
                } else {
                    pomXml.getElementsByTagName('groupId')[0].textContent =
                            this.deployFile.unitInfo.groupId ? this.deployFile.unitInfo.groupId : '';
                }
            }
            if (pomXml.getElementsByTagName('artifactId').length) {
                if(pomXml.getElementsByTagName('artifactId')[0].hasChildNodes()) {
                    pomXml.getElementsByTagName('artifactId')[0].childNodes[0].nodeValue =
                            this.deployFile.unitInfo.artifactId ? this.deployFile.unitInfo.artifactId : '';
                } else {
                    pomXml.getElementsByTagName('artifactId')[0].textContent =
                            this.deployFile.unitInfo.artifactId ? this.deployFile.unitInfo.artifactId : '';
                }
            }
            if (pomXml.getElementsByTagName('version').length) {
                if(pomXml.getElementsByTagName('version')[0].hasChildNodes()) {
                    pomXml.getElementsByTagName('version')[0].childNodes[0].nodeValue =
                            this.deployFile.unitInfo.version ? this.deployFile.unitInfo.version : '';
                } else {
                    pomXml.getElementsByTagName('version')[0].textContent =
                            this.deployFile.unitInfo.version ? this.deployFile.unitInfo.version : '';
                }
            }
            //Serialize updated pom xml back to string and re-set as model
            let backToText = new XMLSerializer();
            this.deployFile.unitConfigFileContent = backToText.serializeToString(pomXml);
        }
        this.deployFile.targetPath = newPath;
    }

    updateDebianTargetPath() {
        let path;
        if (this.deployFile.targetPath.indexOf(';') != -1) {
            path = this.deployFile.targetPath.substring(0, this.deployFile.targetPath.indexOf(';'));
        }
        else {
            path = this.deployFile.targetPath;
        }
        let newPath = '';
        newPath += ( path || '');
        if (this.deployFile.unitInfo.distribution) {
            newPath += ";deb.distribution=" + (this.deployFile.unitInfo.distribution || '');
        }
        if (this.deployFile.unitInfo.component) {
            newPath += ";deb.component=" + (this.deployFile.unitInfo.component || '');
        }
        if (this.deployFile.unitInfo.architecture) {
            newPath += ";deb.architecture=" + (this.deployFile.unitInfo.architecture || '');
        }
        this.deployFile.targetPath = '';
        this.deployFile.targetPath = newPath;
    }

    clearPath() {
        if(this.node.data.type == 'folder') {
            this.deployFile.targetPath = this.node.data.path;
        } else {
            this.deployFile.targetPath = this.deployFile.targetPath.replace("/" + this.deployFile.fileName, "");
        }
        this.uploadCompleted = false;
    }

    isReady() {
        let ok = true;
        if(this.deployFile.unitInfo && this.deployFile.unitInfo.debianArtifact) {
            ok = this.deployFile.unitInfo.distribution && this.deployFile.unitInfo.component && this.deployFile.unitInfo.architecture;
        }
        return ok && this.uploadCompleted;
    }

    /****** Angular File Upload Callbacks ******/
    onAfterAddingFile(fileItem) {
        if (this.currentDeploy == 'single') {
            this.deployFile.fileName = fileItem.file.name;
            if (this.deployFile.targetPath.slice(-1) != "/") {
                this.deployFile.targetPath += "/";
            }
            this.deployFile.targetPath += fileItem.file.name;
        }
        if (fileItem.file.size < 0) {
            fileItem.okToUploadFile = false;
            this.uploader.removeFromQueue(fileItem);
        }
        else {
            // Save original for display
            fileItem.file.originalName = fileItem.file.name;
            // Encode filename to support UTF-8 strings (server does decode)
            fileItem.file.name = encodeURI(fileItem.file.name);
            fileItem.okToUploadFile = true;
        }
    }

    onAfterAddingAll(fileItems) {
        if (this.currentDeploy == 'single') {
            if (fileItems.length > 1) {
                this.artifactoryNotifications.create({error: "You can only deploy one file"});
                this.uploader.queue = [];
                return;
            }
        }
        if (this.currentDeploy != 'single') {
            if (fileItems.length > 20) {
                this.artifactoryNotifications.create({error: "You can only deploy up to 20 files at a time"});
                this.uploader.queue = [];
                return;
            }
            //Enable the "deploy" button after all files were added.
            this.uploadCompleted = true;
        }

        let uploadAll = true;

        fileItems.forEach((item)=> {
            if (!item.okToUploadFile) {

                uploadAll = false;
                return;
            }
        });

        if (this.currentDeploy == 'single') {
            if (uploadAll) {
                this.uploader.uploadAll();
            }
            else {
                return;
            }
        }
    }

    onUploadSuccess(fileDetails, response) {
        this.deployFile.unitInfo = response.unitInfo;
        this.deployFile.unitConfigFileContent = response.unitConfigFileContent;
        //MavenArtifact causes 'deploy as' checkbox to be lit -> change deployment path according to GAVC
        if (this.deployFile.unitInfo && this.deployFile.unitInfo.mavenArtifact) {
            this.originalDeployPath = this.deployFile.targetPath;
            this.updateMavenTargetPath()
        }

        if (this.currentDeploy === 'single') {
            this.comm.needToCancel = true;
        }
        else {
            if (response.repoKey && response.artifactPath) {
                let msg = jfDeployController.createNotification(response);
                if (msg.type === 'success') this.multiSuccessMessage += msg.body + '<br>';
                else this.artifactoryNotifications.createMessageWithHtml(msg);
            }
        }
    }

    /**
     * This builds an appropriate notification for the Deploy action in the UI (with or w/o the Artifact URL)
     *
     * @param response from the server
     * @returns {{type: string, body: string}}
     */
    static createNotification(response) {
        let {repoKey, artifactPath} = response;
        artifactPath = _.trim(artifactPath, '/');
        let messageWithUrl = `<a href="#/artifacts/browse/tree/General/${repoKey}/${artifactPath}">${artifactPath}</a> has been deployed successfully to ${repoKey}`;
        let messageWithoutUrl = `${artifactPath} has been deployed successfully`;
        return {
            type: 'success',
            body: response.showUrl ? messageWithUrl : messageWithoutUrl
        }
    }

    onUploadError(item, response) {

        this.errorQueue.push({item: item, response: response});
        if (this.currentDeploy == 'single') {
            this.artifactoryNotifications.create(response);
            this.uploader.removeFromQueue(item);
        }

    }

    onCompleteAll() {
        this.uploadCompleted = true;
        this.progress = false;
        let body = '<ul>';
        this.artifactoryNotifications.clear();
        if (this.errorQueue.length) {
            this.errorQueue.forEach((error)=> {
                body += '<li>"' + error.item.file.name + '" ' + error.response.error + '</li>'
            })
            body += '</ul>';
            this.artifactoryNotifications.createMessageWithHtml({type: 'error', body: body, timeout: 10000});
            this.uploader.queue = [];
            this.errorQueue=[];
        }
        else if (this.onSuccess && typeof this.onSuccess === 'function') {
            this.artifactoryNotifications.createMessageWithHtml({type: 'success', body: this.multiSuccessMessage});
            this.onSuccess();
        }
        if (this.currentDeploy != 'single') {
            this._dispatchSuccessEvent();
        }
    }

    onProgressAll() {
        if (!this.progress && this.currentDeploy != 'single') {
            this.progress = true;
            this.artifactoryNotifications.createMessageWithHtml({
                type: 'success',
                body: 'Deploy in progress...',
                timeout: 60 * 60000
            });

        }
    }
    
    _dispatchSuccessEvent() {
        this.artifactoryEventBus.dispatch(EVENTS.ACTION_DEPLOY, this.deployFile.repoDeploy.repoKey);
    }

}

export function jfDeploy() {
    return {
        restrict: 'EA',
        scope: {
            node: '=',
            deploy: '&',
            comm: '='
        },
        controller: jfDeployController,
        controllerAs: 'jfDeploy',
        bindToController: true,
        templateUrl: 'directives/jf_deploy/jf_deploy.html'
    }
}
