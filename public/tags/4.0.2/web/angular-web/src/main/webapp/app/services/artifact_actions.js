import EVENTS from '../constants/artifacts_events.constants';
import ACTIONS from '../constants/artifacts_actions.constants';
export class ArtifactActions {
    constructor(ArtifactoryEventBus, ArtifactActionsDao, 
                ArtifactoryModal, selectTargetPath, selectDeleteVersions, PushToBintrayModal) {
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactActionsDao = ArtifactActionsDao;
        this.pushToBintrayModal = PushToBintrayModal;
        this.modal = ArtifactoryModal;
        this.selectTargetPath = selectTargetPath;
        this.selectDeleteVersions = selectDeleteVersions;
    }

    perform(actionObj, node) {
        this['_do' + actionObj.name](node);
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

    _doDelete(node) {
        this.modal.confirm('Are you sure you wish to delete this file?','Delete ' + node.data.text, {confirm: 'Delete'})
                .then(() => this._performActionInServer('delete', node))
                .then((response) => this.artifactoryEventBus.dispatch(EVENTS.ACTION_DELETE, node));
    }

    _doDeleteContent(node) {
//        console.log(node);
        this.modal.confirm('Are you sure you want to delete this repository? All artifacts will be permanently deleted.', 'Delete ' + node.data.text, {confirm: 'Delete'})
                .then(() => this._performActionInServer('delete', node))
                .then((response) => this.artifactoryEventBus.dispatch(EVENTS.ACTION_DELETE, node));
    }

    _doDeleteVersions(node) {
        var versions;
        this.selectDeleteVersions(node)
                .then((_versions) => {
                    versions = _versions;
                    return this.modal.confirm('Are you sure you wish to delete the selected versions?\n\nThis folder may contain artifacts that are part of the result of or used as dependencies in published build(s).')
                })
                .then(() => {
                    return this._performActionInServer('deleteversions', null, versions)
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