class ArtifactoryModelSaver {
    constructor(controller,modelObjects,excludePaths, $timeout,ArtifactoryModal, $q) {
        this.ArtifactoryModal = ArtifactoryModal;
        this.$q = $q;

        this.controller = controller;
        this.controller._$modelSaver$_ = this;
        this.confirmOnLeave = true;
        this.modelObjects = modelObjects;
        this.excludePaths = excludePaths;
        this.savedModels = {};
        this.saved = false;

        $timeout(()=>{
            if (!this.saved) this.save();
        })
    }

    save() {
        this.modelObjects.forEach((objName)=>{
            this.savedModels[objName] = _.cloneDeep(this.controller[objName]);
        });
        this.saved = true;
    }

    isModelSaved() {
        let isSaved = true;
        for (let objectNameI in this.modelObjects) {
            let objectName = this.modelObjects[objectNameI];
            if (!angular.equals(this.savedModels[objectName],this.controller[objectName])) {
                let deefObj = DeepDiff(this.savedModels[objectName],this.controller[objectName]);
//                console.log(deefObj);
                if (this._isDiffReal(deefObj,this.excludePaths[objectNameI])) {
                    isSaved = false;
                    break;
                }
            }
        }
        return isSaved;
    }


    _isDiffReal(deefObj,excludePaths) {

        let excludes = excludePaths ? excludePaths.split(';') : [];

        let isReal = false;

        for (let key in deefObj) {
            let deef = deefObj[key];

            if (deef.path && deef.path.length && ((!_.isString(deef.path[deef.path.length-1]) || deef.path[deef.path.length-1].startsWith('$$')) || this._isExcluded(deef.path,excludes))) continue;

            if ((deef.lhs === undefined && deef.rhs === '') ||
                (deef.lhs === undefined && _.isArray(deef.rhs) && deef.rhs.length === 0) ||
                (deef.lhs === undefined && _.isObject(deef.rhs) && Object.keys(deef.rhs).length === 0)) {
                // not real
            }
            else { //real
                isReal = true;
                break;
            }
        }

        return isReal;

    }

    _isExcluded(path,excludes) {
        if (!excludes.length) return false;
        let excluded = false;
        for (let i in excludes) {
            let exclude = excludes[i];
            let exPath = exclude.split('.');
            let match = true;
            for (let pI in exPath) {
                if ((exPath[pI] !== '*' && exPath[pI] !== path[pI]) || (exPath[pI] === '*' && path[pI]) === undefined) {
                    match = false;
                    break;
                }
            }
            if (match) excluded = true;
            break;
        }

        return excluded;
    }


    ask() {
        let defer = this.$q.defer();
        if (!this.isModelSaved()) {
            this.ArtifactoryModal.confirm('You have unsaved changes. Leaving this page will discard changes.', 'Discard Changes', {confirm: 'Discard'})
                    .then(()=>{
                        defer.resolve();
                    });
        }
        else {
            defer.resolve();
        }
        return defer.promise;
    }
}

export function ArtifactoryModelSaverFactory ($timeout,ArtifactoryModal, $q) {
    return {
        createInstance: (controller,modelObjects,excludePaths) => {
            excludePaths = excludePaths || [];
            return new ArtifactoryModelSaver(controller,modelObjects,excludePaths,$timeout,ArtifactoryModal, $q);
        }
    }
}