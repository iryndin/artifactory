import EVENTS from '../../../../constants/artifacts_events.constants';
import DICTIONARY from './../../constants/artifact_general.constant';

class jfDockerV2Controller {
    constructor($scope, ArtifactViewsDao, ArtifactoryEventBus, ArtifactoryGridFactory) {
        this.$scope = $scope;
        this.artifactViewsDao = ArtifactViewsDao;
        this.DICTIONARY = DICTIONARY.dockerV2;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.dockerV2Data = {};
        this.labelGridOptions = {};

        this._getDockerV2Data();
        this._registerEvents();
    }

    _getDockerV2Data() {
        //Temp fix for preventing fetching data for non-file nodes (occurred when pressing "Artifacts" on sidebar)
        if (!this.currentNode.data.path) {
            return;
        }

        this.artifactViewsDao.fetch({
            "view": "dockerv2",
            "repoKey": this.currentNode.data.repoKey,
            "path": this.currentNode.data.path
        }).$promise.then((data) => {
            this.dockerV2Data = data;
            this._createGrid();
            if (this.layersController)
                this.layersController.refreshView();
        });
    }

    _createGrid() {
        if (this.dockerV2Data.tagInfo.labels) {
            if (!Object.keys(this.labelGridOptions).length) {
                this.labelGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                    .setRowTemplate('default')
                    .setColumns(this._getColumns())
                    .setGridData(this.dockerV2Data.tagInfo.labels);
            }
        }
    }

    _getColumns() {
        return [
            {
                name: 'key',
                displayName: 'Key',
                field: 'key'
            },
            {
                name: 'value',
                displayName: 'Value',
                field: 'value'
            }]
    }

    isNotEmptyValue(value) {
        return value && (!_.isArray(value) || value.length > 0);
    }

    formatValue(value) {
        if (_.isArray(value)) {
            return value.join(', ');
        }
        else return value;
    }

    _registerEvents() {
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TAB_NODE_CHANGED, (node) => {
            this.currentNode = node;
            this._getDockerV2Data();
        });
    }
}

export function jfDockerV2() {
    return {
        restrict: 'EA',
        controller: jfDockerV2Controller,
        controllerAs: 'jfDockerV2',
        scope: {
            currentNode: '='
        },
        bindToController: true,
        templateUrl: 'states/artifacts/jf_artifact_info/info_tabs/jf_docker_v2.html'
    }
}