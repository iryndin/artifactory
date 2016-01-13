import API from '../constants/api.constants';
import EVENTS from '../constants/artifacts_events.constants';
import ACTIONS from '../constants/artifacts_actions.constants';
export class ArtifactActions {
    constructor(ArtifactoryEventBus, ArtifactActionsDao, StashResultsDao, $state, $window, $rootScope, $timeout, ArtifactoryNotifications, FooterDao, $sce,
                ArtifactoryModal, selectTargetPath, selectDeleteVersions, PushToBintrayModal, $q, artifactoryIFrameDownload, NativeBrowser) {
        this.$q = $q;
        this.$state = $state;
        this.$timeout = $timeout;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.footerDao = FooterDao;
        this.artifactActionsDao = ArtifactActionsDao;
        this.stashResultsDao = StashResultsDao;
        this.pushToBintrayModal = PushToBintrayModal;
        this.modal = ArtifactoryModal;
        this.selectTargetPath = selectTargetPath;
        this.selectDeleteVersions = selectDeleteVersions;
        this.$window = $window;
        this.$rootScope = $rootScope;
        this.iframeDownload = artifactoryIFrameDownload;
        this.nativeBrowser = NativeBrowser;
        this.$sce = $sce;
    }

    perform(actionObj, node) {
        return this['_do' + actionObj.name](node);
    }

    _doRefresh(node) {
        this.artifactoryEventBus.dispatch(EVENTS.ACTION_REFRESH, node);
    }

    _doCopy(node, useNodePath) {

        let target;
        let onActionDone;
        onActionDone = (retData) => {
            target = retData.target;
            this._performActionInServer('copy', node, target)
                    .then(()=>{
                        retData.onSuccess().then((response) => {
                            this.artifactoryEventBus.dispatch(EVENTS.ACTION_COPY,{node: node, target: target});
                        });
                    })
                    .catch((err)=>{
                        retData.onFail(err.data.errors).then(onActionDone);
                    });
        }
        this.selectTargetPath('copy', node, useNodePath).then(onActionDone)
    }

    _doMove(node, useNodePath) {
        let target;
        let onActionDone;
        onActionDone = (retData) => {
            target = retData.target;
            this._performActionInServer('move', node, target)
                    .then((data)=>{
                        retData.onSuccess().then((response) => {
                            this.artifactoryEventBus.dispatch(EVENTS.ACTION_MOVE,{node: node, target: target});
                        });
                    })
                    .catch((err)=>{
                        retData.onFail(err.data.errors).then(onActionDone);
                    });
        }
        this.selectTargetPath('move', node, useNodePath).then(onActionDone);
    }


    _doStashAction(node,action) {
        let target;
        let onActionDone;
        let dryRun;
        onActionDone = (retData) => {
            target = retData.target;
            this.stashResultsDao[action]({name: 'stash', repoKey: target.targetRepoKey},{}).$promise
                .then(()=>{
                    retData.onSuccess().then((response) => {
                        this.artifactoryEventBus.dispatch(EVENTS.ACTION_MOVE_STASH,{node: node, target:{targetPath: '/', targetRepoKey: target.targetRepoKey}});
                    });
                })
                .catch((err)=>{
                    this.artifactoryEventBus.dispatch(EVENTS.ACTION_REFRESH_STASH);
                    retData.onFail(err.data.errors).then(onActionDone);
                });
        };

        dryRun = ()=>{
            let modalScope = dryRun.scope;
            this.stashResultsDao['silent'+action.charAt(0).toUpperCase()+action.substring(1)]({name: 'stash', repoKey: modalScope.target.repoKey, dryRun: true},{}).$promise
                    .then((response)=>{
                        modalScope.resultError = false;
                        modalScope.dryRunResults = [response.info];
                    })
                    .catch((response) => {
                        modalScope.resultError = true;
                        modalScope.dryRunResults = response.data.errors;
                    });
        };

        this.selectTargetPath(action+'Stash', node, false, dryRun).then(onActionDone)
    }

    _doCopyStash(node) {
        this._doStashAction(node,'copy');
    }

    _doMoveStash(node) {
        this._doStashAction(node,'move');
    }

    _doDiscardFromStash(node) {
        let doAction = () => {
            this.stashResultsDao.discard({
                name:'stash',
                repoKey:node.data.repoKey,
                path:node.data.path
            },{}).$promise.then((res)=>{
                        if (res.status === 200) {
                            this.artifactoryEventBus.dispatch(EVENTS.ACTION_DISCARD_FROM_STASH,node);
                        }
                    });
        };

        if (!node.alreadyDeleted) {
            this.modal.confirm('Are you sure you wish to discard \'' + node.text + '\' from stashed search results?',
                    'Discard from stash', {confirm: 'Discard'})
                    .then(doAction);
        }
        else doAction();


    }

    _doDiscardStash() {
        this.modal.confirm('Are you sure you wish to discard stashed search results?','Discard search results', {confirm: 'Discard'})
                .then(() => {
                    this.stashResultsDao.delete({name:'stash'}).$promise.then(()=>{
                        this.artifactoryEventBus.dispatch(EVENTS.ACTION_DISCARD_STASH);
                    });
                });
    }

    _doShowInTree(node) { //stash
        this.artifactoryEventBus.dispatch(EVENTS.ACTION_EXIT_STASH,node);
    }

    _doUploadToBintray(node) {
        this.pushToBintrayModal.launchModal('artifact', {
            repoPath: node.data.repoKey + ':' + node.data.path
        });
    }

    _doCopyContent(node) {
        this._doCopy(node, false);
    }

    _doMoveContent(node) {
        this._doMove(node, false);
    }

    _doWatch(node) {
        this._performActionInServer('watch', node, {}, {param: 'watch'})
                .then((response) => {
                    this.artifactoryEventBus.dispatch(EVENTS.ACTION_WATCH, node);
                });
    }

    _doUnwatch(node) {
        this._performActionInServer('watch', node, {}, {param: 'unwatch'})
                .then((response) => {
                    this.artifactoryEventBus.dispatch(EVENTS.ACTION_UNWATCH, node);
                });
    }

    _doView(node) {
        this._performActionInServer('view', node)
                .then((response) => {
                    this.modal.launchCodeModal(node.data.text, response.data.fileContent,
                            {name: node.data.mimeType})
                });
    }

    _doDeletePermanently(node) {
        this._doDelete(node);
    }

    _doDelete(node) {
        let permanent = this.footerDao.getInfo().trashDisabled || node.data.isInTrashcan();

        //allowPermDeletes is disabled for now (30.12.15)
        let allowPerm = false;//this.footerDao.getInfo().allowPermDeletes;

        let onPermChange = (perm, scope) => {
            scope.content = this.$sce.trustAsHtml('Are you sure you wish to' + (perm ? ' <span class="highlight-alert">permanently</span> ' : ' ') + 'delete this file?');
        };

        this.modal.confirm('Are you sure you wish to' + (permanent ? ' <span class="highlight-alert">permanently</span> ' : ' ') + 'delete this file?', 'Delete ' + node.data.text,
                {confirm: 'Delete'},allowPerm ? "Delete permanently" : undefined, onPermChange)
                .then((permDelete) => {
                    this._performActionInServer('delete', node, permDelete ? {permDelete: permDelete} : undefined).then(()=>{
                        this.artifactoryEventBus.dispatch(EVENTS.ACTION_DELETE, node);
                    })

                })
    }

    _doDeleteContent(node) {
        let permanent = this.footerDao.getInfo().trashDisabled;

        this.modal.confirm('Are you sure you want to delete the content of this repository? All artifacts will be' + (permanent ? ' <span class="highlight-alert">permanently</span> ' : ' ') + 'deleted.', 'Delete Content Of \'' + node.data.text +'\'', {confirm: 'Delete Content'})
                .then(() => this._performActionInServer('delete', node))
                .then((response) => this.artifactoryEventBus.dispatch(EVENTS.ACTION_DELETE, node));
    }

    _doDeleteVersions(node) {
        var versions;
        this.selectDeleteVersions(node)
                .then((_versions) => {
                    versions = _versions;
                    return this.modal.confirm('Are you sure you wish to delete '+_versions.length+' selected versions?\n\nThis folder may contain artifacts that are part of the result of or used as dependencies in published build(s).','Delete '+_versions.length+' Versions')
                })
                .then(() => {
                    let promise = this._performActionInServer('deleteversions', null, versions);
                    promise.then(()=>{
                        this._doRefresh(node);
                    });
                    return promise;
                });
    }

    _doZap(node) {
        this._performActionInServer('zap', node).then((data)=> {
//            console.log(data);
        })
    }

    _doZapCaches(node) {
        this._performActionInServer('zapVirtual', node).then((data)=> {
            // console.log(data);
        })
    }

    _doRecalculateIndex(node) {
//        console.log('recalculate index', node);
        this._performActionInServer('calculateIndex', node,
                {"type": node.data.repoPkgType, "repoKey": node.data.repoKey}).then((data)=> {
//            console.log(data);
        })
    }

    _doRestoreToOriginalPath(node) {
        return this._doRestore(node,false)
    }

    _doRestore(node,chooseTarget = true) {

        let defer = this.$q.defer();

        let restoreTarget = {
            targetPath: node.data.path.indexOf('/') !== -1 ? node.data.path.substr(node.data.path.indexOf('/')) : '/',
            targetRepoKey: node.data.path.indexOf('/') !== -1 ? node.data.path.substr(0,node.data.path.indexOf('/')) : node.data.path
        };

        let onActionDone = (retData) => {
            let target = retData.target;
            this._performActionInServer('restore', node, target)
                .then((data)=>{
                    if (retData.onSuccess) retData.onSuccess().then((response) => {
                        this.artifactoryEventBus.dispatch(EVENTS.ACTION_MOVE,{node: node, target: target});
                        defer.resolve();
                    });
                    else {
                        defer.resolve();
                    }
                })
                .catch((err)=>{
                    if (retData.onFail) retData.onFail(err.data.errors).then(onActionDone);
                    defer.reject();
                });
        }
        if (chooseTarget) {
            this.selectTargetPath('restore', node, false, false, restoreTarget).then(onActionDone);
        }
        else {
            onActionDone({target: restoreTarget})
        }

        return defer.promise;
/*
        this.modal.confirm('Are you sure you wish to restore this file?', 'Restore ' + node.data.text,
            {confirm: 'Restore'})
            .then(() => this._performActionInServer('restore', node,restoreTarget))
            .then((response) => this.artifactoryEventBus.dispatch(EVENTS.ACTION_MOVE,{node: node, target: restoreTarget}));
*/
    }

    _doEmptyTrash(node) {

        this.modal.confirm('Are you sure you wish to empty the trash can?', 'Empty Trash',
            {confirm: 'Empty Trash'})
            .then(() => this._performActionInServer('emptytrash', node))
            .then((response) => this.artifactoryEventBus.dispatch(EVENTS.TREE_REFRESH, node));
    }

    _doSearchTrash(node) {
        this.$state.go('artifacts.browsers.search',{'searchType':'trash'});
    }

    _doDownloadFolder(node) {
        this.artifactActionsDao.performGet({
            action: 'downloadfolderinfo',
            path: node.data.path,
            repoKey: node.data.repoKey
        }).$promise.then((data)=>{
            let modalInstance;
            let modalScope = this.$rootScope.$new();
            modalScope.totalSize = data.data.sizeMB;
            modalScope.filesCount = data.data.totalFiles;
            modalScope.folderName = node.data.text;
            modalScope.archiveTypes = ['zip','tar','tar.gz','tgz'];
            modalScope.selection = {archiveType: 'zip'};
            modalScope.download = () => {
                this.iframeDownload(`${API.API_URL}/artifactactions/downloadfolder?repoKey=${node.data.repoKey}&path=${node.data.path}&archiveType=${modalScope.selection.archiveType}`,'There are too many folder download requests currently running, try again later.');
                modalInstance.close();
            };
            modalScope.cancel = () => modalInstance.close();

            modalInstance = this.modal.launchModal('download_folder_modal', modalScope, 'sm');
        });

    }

    _doNativeBrowser(node) {
        this.$window.open(this.nativeBrowser.pathFor(node.data),"_blank");
    }

    // Do the actual action on the server via the DAO:
    _performActionInServer(actionName, node, extraData = {}, extraParams = {}) {
        let data;
        if (node) {
            data = angular.extend({
                repoKey: node.data.repoKey,
                path: node.data.path,
                param: extraParams.param
            }, extraData);
        }
        else {
            data = extraData;
        }
        var params = angular.extend({action: actionName}, extraParams);
        return this.artifactActionsDao.perform(params, data).$promise;
    }
}