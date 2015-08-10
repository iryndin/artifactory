import EVENTS     from '../../../constants/artifacts_events.constants';
let headerCellGroupingTemplate = require("raw!../../../ui_components/artifactory_grid/templates/headerCellTemplate.html");
export class SearchController {
    constructor($scope, $stateParams, $window, $state, ArtifactoryGridFactory, ArtifactSearchDao, ArtifactoryEventBus,
                ArtifactActionsDao, artifactoryDownload, RepoDataDao, ArtifactoryState, uiGridConstants,
            commonGridColumns,ArtifactoryModal, ArtifactViewSourceDao) {
        let self = this;
        this.$window = $window;
        this.repoDataDao = RepoDataDao;
        this.artifactSearchDao = ArtifactSearchDao;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryState = ArtifactoryState;
        this.$stateParams = $stateParams;
        this.currentSearch = $stateParams.searchType || "";
        this.download = artifactoryDownload;
        this.artifactActionsDao = ArtifactActionsDao;
        this.gridOptions = {};
        this.uiGridConstants = uiGridConstants;
        this.commonGridColumns = commonGridColumns;
        this.artifactViewSourceDao = ArtifactViewSourceDao.getInstance();
        this.$state = $state;
        this.$scope = $scope;
        this.repos = {};
        this.repoList = [];
        this.isOpenRepoList = true;
        this.resultsMsg = 'Search Results';
        this.modal=ArtifactoryModal;
        this.query = {
            selectedRepositories: []
        };
        this._createGrid();
        this._initSearch();

        // isSearchShown is used to show / hide the tree
        $scope.Artifact.isSearchShown = true;
        $scope.$on('$destroy', () => {
            $scope.Artifact.isSearchShown = false;
        });

        ArtifactoryEventBus.registerOnScope($scope, EVENTS.SEARCH_COLLAPSE, (collapse) => {
            this.closeSearchPanel(collapse);
        });
    }


    showInTree(row) {
        let artifactPath = row.repoKey + "/" + (row.relativePath || row.path);
        let archivePath = '';
        if (row.archiveName) {
            if(row.archivePath === '[root]') {
                row.archivePath = '';
            }
            archivePath = row.repoKey + "/" + row.archivePath + row.archiveName;
        }
        let self = this;
        let path = (archivePath || artifactPath );

        this.$state.go('artifacts.browsers.path', {
            "tab": "General",
            "artifact": path
        });

        this._clearSearchTab();

    }

    showInBintray(row) {
        this.$window.open('https://bintray.com/bintray/jcenter/' + row.package, '')
    }

    openRepoList() {
        this.isOpenRepoList = !this.isOpenRepoList;
    }

    downloadSelectedItems(row) {
        let archivePath = '';
        let remoteRepo = '';
        let artifactPath = row.relativePath || row.path;
        if (row.archiveName) {
            if(row.archivePath === '[root]') {
                row.archivePath = '';
            }
            archivePath = row.archivePath + row.archiveName;
        }
        if (this.$stateParams.searchType == 'remote') {
            remoteRepo = 'jcenter';
        }
        let data = {
            path: artifactPath || archivePath,
            repoKey: remoteRepo || row.repoKey
        };
        this.artifactActionsDao.perform({action: 'download'}, data).$promise
                .then((response) => this.download(response.data.path));
    }

    viewCodeArtifact(row) {
        let name = row.name;
        if(_.startsWith(name, './')) {
            name = name.slice(2);
        }
        if (row.archiveName) {
            if(row.archivePath === '[root]') {
                row.archivePath = '';
            }
            this.artifactViewSourceDao.fetch({
                archivePath: row.archivePath + row.archiveName,
                repoKey: row.repoKey,
                sourcePath: name
            }).$promise
                    .then((result) => {
                        this.modal.launchCodeModal(row.name, result.source,
                                {name: row.type, json: true});
                    })
        } else {
            let data = {
                repoKey: row.repoKey,
                path: (row.relativePath || row.path)
            };
            this.artifactActionsDao.perform({action: 'view'}, data).$promise
                    .then((result) => {
                        this.modal.launchCodeModal(row.name, result.data.fileContent,
                                {name: row.type, json: true});
                    });
        }
    }

    _initSearch() {
        if (this.$stateParams.searchType) {
            this.closeSearchPanel(false);
        }
        if (!this.repoList.length) {
            this.repoDataDao.getForSearch().$promise.then((result)=> {
                result.repoTypesList = _.map(result.repoTypesList,(repo)=>{
                    repo._iconClass = "icon " + (repo.type === 'local' ? "icon-local-repo" : (repo.type === 'remote' ? "icon-remote-repo" : (repo.type === 'virtual' ? "icon-virtual-repo" : "icon-notif-error")));
                    return repo;
                });


                let lastIncluded = (this.$stateParams.searchParams && this.$stateParams.searchParams.selectedRepos) ? this.$stateParams.searchParams.selectedRepos : [];
                this.repoList = _.filter(result.repoTypesList,(repo)=>{
                    return !_.find(lastIncluded,{repoKey: repo.repoKey});
                });
            });
        }
        if (this.$stateParams.params) {
            this.query = JSON.parse(atob(this.$stateParams.params));
            this._getGridData([]);
        }

    }

    _createGrid() {
        if(this.currentSearch == "remote" || this.currentSearch == "class") {
            this.gridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                    .setColumns(this._getColumns())
                    .setRowTemplate('default')
                    .setGridData([]);
        } else {
            this.gridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                    .setColumns(this._getColumns())
                    .setRowTemplate('default')
                    .setMultiSelect()
                    .setBatchActions(this._getBatchActions())
                    .setGridData([]);
        }

        this.gridOptions.isRowSelectable = (row) => {
            var notRepository = row.entity.relativeDirPath !== '[repo]';
            return notRepository && _.contains(row.entity.actions, 'Delete');
        };
    }

    setBatchActions(batchActions) {
        this.batchActions = batchActions;
        this.setMultiSelect();
        return this;
    }

    _getBatchActions() {
        return [
            {
                icon: 'clear',
                name: 'Delete',
                callback: () => this.bulkDelete()
            }
        ]
    }

    _getGridData() {
        if (this.currentSearch == "property") {
            this.artifactSearchDao.fetch({
                search: "property",
                propertyKeyValues: this.query.properties,
                selectedRepositories: this.query.selectedRepositories
            }).$promise.then((result)=> {
                    this.resultsMsg = result.data.message;
                    this.gridOptions.setGridData(result.data.results);
                });
        }
        else {
            this.artifactSearchDao.fetch(this.query).$promise.then((result)=> {
                this.resultsMsg = result.data.message;
                this.gridOptions.setGridData(result.data.results);
            });
        }
    }

    closeSearchPanel(collapse) {

        this.collapseSearchPanel = collapse;
    }

    _clearSearchTab() {
        this.artifactoryEventBus.dispatch(EVENTS.CLEAR_SEARCH);
    }

    bulkDelete(){
        let selectedRows = this.gridOptions.api.selection.getSelectedRows();
        //console.log(selectedRows);
        // Ask for confirmation before delete and if confirmed then delete bulk of users
        this.modal.confirm(`Are you sure you want to delete ${selectedRows.length} items?`).then(() =>{this._deleteSingleSelected(selectedRows)});
    }

    _deleteSingleSelected(rows){
        //console.log(rows);
        let elementsToDelete = _.map(rows, (row) => {
            return {
                name: row.name,
                path: row.relativePath,
                repoKey: row.repoKey
            }

        });
        this.artifactSearchDao.delete({artifacts:elementsToDelete}).$promise.then(() => {
            // refresh the gridData in any case
        }).finally(()=>{
            this._getGridData()
        });
    }

    _deleteSelected(rows){
        this.modal.confirm(`Are you sure you want to delete ${rows[0].name}?`)
                .then(() => this._deleteSingleSelected(rows));
    }

    backToBrowse() {
        this._clearSearchTab();
        var tree = this.artifactoryState.getState('lastTreeState');
        if (tree) {
            this.$state.go(tree.name, tree.params)
        }
        else {
            this.$state.go('artifacts.browsers.path', {tab: 'General', artifact: ''});
        }
    }

    showRepoList() {
        return this.$stateParams.searchType !== 'remote';
    }

    _getColumns() {
        switch (this.currentSearch) {
            case 'quick':
            {
                return [
                    {
                        name: "Artifact",
                        displayName: "Artifact",
                        field: "name",
                        sort: {
                            direction: this.uiGridConstants.ASC
                        },
                        cellTemplate: this.commonGridColumns.downloadableColumn(),
                        width: '25%',
                        customActions: [{
                            icon: 'icon icon-view',
                            tooltip: 'View',
                            callback: row => this.viewCodeArtifact(row),
                            visibleWhen: row => _.contains(row.actions, 'View')
                        }],
                        actions: {
                            download: {
                                callback: row => this.downloadSelectedItems(row),
                                visibleWhen: row => _.contains(row.actions, 'Download')
                            }
                        }
                    },
                    {
                        name: "Path",
                        displayName: "Path",
                        field: "relativeDirPath",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '40%',
                        customActions: [{
                            icon: 'icon icon-show-in-tree',
                            tooltip: 'Show In Tree',
                            callback: row => this.showInTree(row),
                            visibleWhen: row => _.contains(row.actions, 'ShowInTree')
                        }]
                    },
                    {
                        name: "Repository",
                        displayName: "Repository",
                        field: "repoKey",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '15%'
                    },
                    {
                        name: "Modified",
                        displayName: "Modified",
                        cellTemplate: '<div class="ui-grid-cell-contents">{{ row.entity.modifiedString }}</div>',
                        field: "modifiedDate",
                        width: '20%',
                        actions: {
                            delete: {
                                callback: row => this._deleteSelected([row]),
                                visibleWhen: row => _.contains(row.actions, 'Delete')
                            }
                        }
                    }
                ]
            }
            case 'class':
            {
                return [
                    {
                        name: "Name",
                        displayName: "Name",
                        field: "name",
                        sort: {
                            direction: this.uiGridConstants.ASC
                        },
                        width: '30%'
                    },

                    {
                        name: "Artifact",
                        displayName: "Artifact",
                        field: "archiveName",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '20%',
                        customActions: [{
                            icon: 'icon icon-view',
                            tooltip: 'View',
                            callback: row => this.viewCodeArtifact(row),
                            visibleWhen: row => _.contains(row.actions, 'View')
                        }],
                        actions: {
                            download: {
                                callback: row => this.downloadSelectedItems(row),
                                visibleWhen: row => _.contains(row.actions, 'Download')
                            }
                        }
                    },
                    {
                        name: "Artifact Path",
                        displayName: "Artifact Path",
                        field: "archivePath",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '25%',
                        customActions: [{
                            icon: 'icon icon-show-in-tree',
                            tooltip: 'Show In Tree',
                            callback: row => this.showInTree(row),
                            visibleWhen: row => _.contains(row.actions, 'ShowInTree')
                        }]
                    },
                    {
                        name: "Repository",
                        displayName: "Repository",
                        field: "repoKey",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '10%'
                    },
                    {
                        name: "Modified",
                        displayName: "Modified",
                        cellTemplate: '<div class="ui-grid-cell-contents">{{ row.entity.modifiedString }}</div>',
                        field: "modifiedDate",
                        width: '15%',
                        actions: {
                            delete: {
                                callback: row => this._deleteSelected([row]),
                                visibleWhen: row => _.contains(row.actions, 'Delete')
                            }
                        }
                    }
                ]
            }
            case 'gavc':
            {
                return [
                    {
                        name: 'Artifact',
                        displayName: 'Artifact',
                        field: 'name',
                        sort: {
                            direction: this.uiGridConstants.ASC
                        },
                        width: '20%',
                        customActions: [{
                            icon: 'icon icon-view',
                            tooltip: 'View',
                            callback: row => this.viewCodeArtifact(row),
                            visibleWhen: row => _.contains(row.actions, 'View')
                        }, {
                            icon: 'icon icon-show-in-tree',
                            tooltip: 'Show In Tree',
                            callback: row => this.showInTree(row),
                            visibleWhen: row => _.contains(row.actions, 'ShowInTree')
                        }],
                        actions: {
                            download: {
                                callback: row => this.downloadSelectedItems(row),
                                visibleWhen: row => _.contains(row.actions, 'Download')
                            }
                        }
                    },
                    {
                        name: 'Group ID',
                        displayName: 'Group ID',
                        field: 'groupID',
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '15%'
                    },
                    {
                        name: 'Artifact ID',
                        displayName: 'Artifact ID',
                        field: 'artifactID',
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '17%'
                    },
                    {
                        name: 'Version',
                        displayName: 'Version',
                        field: 'version',
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '13%'
                    },
                    {
                        name: 'Classifier',
                        displayName: 'Classifier',
                        field: 'classifier',
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '10%'
                    },
                    {
                        name: 'Repository',
                        displayName: 'Repository',
                        field: 'repoKey',
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '10%'
                    },
                    {
                        name: "Modified",
                        displayName: "Modified",
                        cellTemplate: '<div class="ui-grid-cell-contents">{{ row.entity.modifiedString }}</div>',
                        field: "modifiedDate",
                        width: '15%',
                        actions: {
                            delete: {
                                callback: row => this._deleteSelected([row]),
                                visibleWhen: row => _.contains(row.actions, 'Delete')
                            }
                        }
                    }
                ]
            }
            case 'property':
            {
                return [
                    {
                        name: "Item",
                        displayName: "Item",
                        field: "name",
                        sort: {
                            direction: this.uiGridConstants.ASC
                        },
                        width: '25%',
                        customActions: [{
                            icon: 'icon icon-view',
                            tooltip: 'View',
                            callback: row => this.viewCodeArtifact(row),
                            visibleWhen: row => _.contains(row.actions, 'View')
                        }],
                        actions: {
                            download: {
                                callback: row => this.downloadSelectedItems(row),
                                visibleWhen: row => _.contains(row.actions, 'Download')
                            }
                        }
                    },
                    {
                        name: "Type",
                        displayName: "Type",
                        field: "resultType",
                        cellTemplate: '<div class="ui-grid-cell-contents">' +
                        '<span jf-tooltip="{{ row.entity.resultType }}" class="icon" ng-class="{ \'icon-local-repo\': row.entity.resultType === \'Repository\', \'icon-folder\': row.entity.resultType === \'Directory\', \'icon-general\': row.entity.resultType === \'File\'}"></span></div>',
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '8%'
                    },
                    {
                        name: "Path",
                        displayName: "Path",
                        field: "relativeDirPath",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '37%',
                        customActions: [{
                            icon: 'icon icon-show-in-tree',
                            tooltip: 'Show In Tree',
                            callback: row => this.showInTree(row),
                            visibleWhen: row => _.contains(row.actions, 'ShowInTree')
                        }]
                    },
                    {
                        name: "Repository",
                        displayName: "Repository",
                        field: "repoKey",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '15%'
                    },
                    {
                        name: "Modified",
                        displayName: "Modified",
                        cellTemplate: '<div class="ui-grid-cell-contents">{{ row.entity.modifiedString }}</div>',
                        field: "modifiedDate",
                        width: '15%',
                        actions: {
                            delete: {
                                callback: row => this._deleteSelected([row]),
                                visibleWhen: row => _.contains(row.actions, 'Delete')
                            }
                        }
                    }
                ]
            }
            case 'checksum':
            {
                return [
                    {
                        name: "Artifact",
                        displayName: "Artifact",
                        field: "name",
                        sort: {
                            direction: this.uiGridConstants.ASC
                        },
                        width: '25%',
                        customActions: [{
                            icon: 'icon icon-view',
                            tooltip: 'View',
                            callback: row => this.viewCodeArtifact(row),
                            visibleWhen: row => _.contains(row.actions, 'View')
                        }],
                        actions: {
                            download: {
                                callback: row => this.downloadSelectedItems(row),
                                visibleWhen: row => _.contains(row.actions, 'Download')
                            }
                        }
                    },
                    {
                        name: "Path",
                        displayName: "Path",
                        field: "relativeDirPath",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '45%',
                        customActions: [{
                            icon: 'icon icon-show-in-tree',
                            tooltip: 'Show In Tree',
                            callback: row => this.showInTree(row),
                            visibleWhen: row => _.contains(row.actions, 'ShowInTree')
                        }]
                    },
                    {
                        name: "Repository",
                        displayName: "Repository",
                        field: "repoKey",
                        headerCellTemplate: headerCellGroupingTemplate,
                        width: '15%'
                    },
                    {
                        name: "Modified",
                        displayName: "Modified",
                        cellTemplate: '<div class="ui-grid-cell-contents">{{ row.entity.modifiedString }}</div>',
                        field: "modifiedDate",
                        width: '15%',
                        actions: {
                            delete: {
                                callback: row => this._deleteSelected([row]),
                                visibleWhen: row => _.contains(row.actions, 'Delete')
                            }
                        }
                    }
                ]
            }
            case 'remote':
            {
                return [
                    {
                        name: "Name",
                        displayName: "Name",
                        field: "name",
                        sort: {
                            direction: this.uiGridConstants.ASC
                        },
                        width: '20%',
                        actions: {
                            download: {
                                callback: row => this.downloadSelectedItems(row),
                                visibleWhen: row => _.contains(row.actions, 'Download')
                            }
                        }
                    },
                    {
                        name: "Path",
                        displayName: "Path",
                        field: "path",
                        customActions: [{
                            icon: 'icon icon-bintray',
                            tooltip: 'Show In Bintray',
                            callback: row => this.showInBintray(row)
                        }],
                        width: '30%'
                    },
                    {
                        name: "Package",
                        displayName: "Package",
                        field: "package",
                        width: '25%'
                    },
                    {
                        name: "Released",
                        displayName: "Released",
                        field: "release",
                        width: '15%'
                    },
                    {
                        name: "Cached",
                        displayName: "Cached",
                        field: "cached",
                        cellTemplate: this.commonGridColumns.booleanColumn('MODEL_COL_FIELD'),
                        width: '10%'
                    }
                ]
            }
        }
    }
}
