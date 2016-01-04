import EVENTS     from '../../../constants/artifacts_events.constants';
import TOOLTIP  from '../../../constants/artifact_tooltip.constant';

let headerCellGroupingTemplate = require("raw!../../../ui_components/artifactory_grid/templates/headerCellTemplate.html");
export class SearchController {
    constructor($scope, $stateParams, $window, $state, ArtifactoryGridFactory, ArtifactSearchDao, ArtifactPackageSearchDao, ArtifactoryEventBus,
                ArtifactActionsDao, artifactoryDownload, RepoDataDao, ArtifactoryState, uiGridConstants, $timeout, ArtifactActions, FooterDao,
            commonGridColumns,ArtifactoryModal, ArtifactViewSourceDao, StashResultsDao, ArtifactoryNotifications, User, SetMeUpDao, UserProfileDao) {
        this.$window = $window;
        this.$timeout = $timeout;
        this.repoDataDao = RepoDataDao;
        this.artifactSearchDao = ArtifactSearchDao;
        this.userProfileDao = UserProfileDao;
        this.footerDao = FooterDao;
        this.setMeUpDao = SetMeUpDao;
        this.artifactPackageSearchDao = ArtifactPackageSearchDao;
        this.stashResultsDao = StashResultsDao;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.artifactoryState = ArtifactoryState;
        this.$stateParams = $stateParams;
        this.actions = ArtifactActions;
        this.user = User;
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
        this.TOOLTIP = TOOLTIP.artifacts.search;
        this.modal=ArtifactoryModal;
        this.query = {
            selectedRepositories: []
        };

        if (this.currentSearch === 'package') {
            this._createPackageSearchColumnsObject();
        }
        this._initSearch();
        this._createGrid();

        this.showAQL = false;

        // isSearchShown is used to show / hide the tree
        $scope.Artifact.isSearchShown = true;
        $scope.$on('$destroy', () => {
            $scope.Artifact.isSearchShown = false;
        });

        ArtifactoryEventBus.registerOnScope($scope, EVENTS.SEARCH_COLLAPSE, (collapse) => {
            this.closeSearchPanel(collapse);
        });

        this.results = [];
        this.savedToStash = false;

        this._updateStashStatus();
    }


    showInTree(row) {
        let relativePath;
        let artifactPath;
        if (this.currentSearch === 'trash') {
            relativePath = (row.originRepository + "/" + row.relativeDirPath).split('[root]').join('');
            artifactPath = (row.repoKey + "/" + relativePath + "/" + row.name).split('//').join('/');
        }
        else {
            relativePath = row.relativePath ? (row.relativePath.startsWith('./') ? row.relativePath.substr(2) : row.relativePath) : '';
            artifactPath = row.repoKey + "/" + (relativePath || row.path);
        }


        let archivePath = '';
        if (row.archiveName) {
            if(row.archivePath === '[root]') {
                row.archivePath = '';
            }
            archivePath = row.repoKey + "/" + row.archivePath + row.archiveName;
        }
        let path = (archivePath || artifactPath );
        this.$state.go('artifacts.browsers.path', {
            "browser": "tree",
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
//        this.download(row.downloadLink);
    }

    restoreTrashItem(row) {
        this.actions.perform({name: 'RestoreToOriginalPath'},
            {
                data: {
                    path: (row.originRepository + '/' + row.relativeDirPath + '/' + row.name).split('[root]').join(''),
                    repoKey: row.repoKey
                }
            }
        )
        .then(()=>{
            this._getGridData()
        })
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
            let getFuncName = this.currentSearch === 'package' ? 'getForPackageSearch' : 'getForSearch';
            this.repoDataDao[getFuncName]().$promise.then((result)=> {
                result.repoTypesList = _.map(result.repoTypesList,(repo)=>{
                    repo._iconClass = "icon " + (repo.type === 'local' ? "icon-local-repo" : (repo.type === 'remote' ? "icon-remote-repo" : (repo.type === 'virtual' ? "icon-virtual-repo" : "icon-notif-error")));
                    return repo;
                });

                this.allRepoList  = _.cloneDeep(result.repoTypesList);
                let lastIncluded = (this.$stateParams.searchParams && this.$stateParams.searchParams.selectedRepos) ? this.$stateParams.searchParams.selectedRepos : [];
                this.repoList = _.filter(result.repoTypesList,(repo)=>{
                    return !_.find(lastIncluded,{repoKey: repo.repoKey});
                });
            });
        }
        if (this.$stateParams.params) {
            this.query = JSON.parse(atob(this.$stateParams.params));

            if (this.currentSearch === 'package') {
                this.packageSearchColumns = this.query.columns;
            }
            this._getGridData([]);
        }
        else {
            if (this.currentSearch === 'package') {
                this.packageSearchColumns = ['artifact','path','repo','modified'];
            }
        }
        //get set me up data (for baseUrl)
        this.setMeUpDao.get().$promise.then((result)=> {
            this.baseUrl = result.baseUrl;
            this._updateAQL();
        });

        this.userProfileDao.getApiKey().$promise.then((res)=>{
            this.apiKey = res.apiKey;
            this._updateAQL();
        });



    }

    _updateAQL() {
        if (this.cleanAql) this.aql = `curl -H 'X-Api-Key: ${this.apiKey || '<YOUR_API_KEY>'}' -X POST ${this.baseUrl}/api/search/aql -d '\n${this.cleanAql}'`;
    }
    _createGrid() {
        if (this.currentSearch === 'package' && !this.packageSearchColumns) return;

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
        if (this.currentSearch === 'package' && !this.packageSearchColumns) return;

        if (this.currentSearch == "property") {
            this.artifactSearchDao.fetch({
                search: "property",
                propertyKeyValues: this.query.properties,
                selectedRepositories: _.pluck(this.query.selectedRepositories,'repoKey')
            }).$promise.then((result)=> {
                    this.resultsMsg = result.data.message;
                    this.gridOptions.setGridData(result.data.results);
                    this.results = result.data.results;
                    this.savedToStash = false;
                });
        }
        else if (this.currentSearch === "package" && this.query.query.search !== 'gavc') {
            this.artifactPackageSearchDao.runQuery({},this.query.query).$promise.then((result)=>{
                result = result.data;

                _.map(result.results, (result)=>{
                    if (result.extraFields) {
                        for (let key in result.extraFields) {
                            result['extraField_'+key] = result.extraFields[key].join(', ');
                        }
                        delete result.extraFields;
                    }
                });

                this.resultsMsg = result.message;
                this.gridOptions.setGridData(result.results);
                this.results = result.results;
                this.savedToStash = false;
                if (result.searchExpression) {
                    this.cleanAql = result.searchExpression;
                    this._updateAQL();
                    this.$timeout(()=>{
                        let showAqlButtonElem = $('#show-aql-button');
                        let aqlViewerElem = $('#aql-viewer');
                        let gridFilterElem = $('jf-grid-filter');
                        let gridActionElem = $('.wrapper-grid-actions');
                        gridFilterElem.append(showAqlButtonElem);
                        showAqlButtonElem.css('display','block');

                        gridActionElem.after(aqlViewerElem);
                    })
                }
            });
        }
        else {
            let theQuery = _.cloneDeep(this.currentSearch === "package" ? this.query.query : this.query);
            theQuery.selectedRepositories = _.pluck(theQuery.selectedRepositories,'repoKey');
            this.artifactSearchDao.fetch(theQuery).$promise.then((result)=> {
                this.resultsMsg = result.data.message;
                this.gridOptions.setGridData(result.data.results);
                this.results = result.data.results;
                this.savedToStash = false;
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
                path: this.currentSearch === 'trash'? ((row.originRepository + '/' + row.relativeDirPath).split('[root]').join('') + '/' + row.name).split('//').join('/') : row.relativePath,
                repoKey: row.repoKey
            }

        });
        this.artifactSearchDao.delete({artifacts:elementsToDelete}).$promise.then(() => {
            // refresh the gridData in any case
        }).finally(()=>{
            this._getGridData()
        });
    }

    _deleteSelected(rows, permanent){
        permanent = permanent || this.footerDao.getInfo().trashDisabled;
        this.modal.confirm(`Are you sure you wish to ${permanent ? ' <span class="highlight-alert">permanently</span> ' : ' '} delete ${rows[0].name}?`)
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
        return this.$stateParams.searchType !== 'remote' && this.$stateParams.searchType !== 'trash';
    }


    _buildPayloadForStash() {
        let searchType = this.$stateParams.searchType;
        if (searchType === 'checksum') searchType='quick';
        let selectedRows = this.gridOptions.api.selection.getSelectedRows();
        let rawResults = selectedRows.length ? selectedRows : this.results;

        rawResults = _.filter(rawResults, (result)=> {
            return !result.resultType || result.resultType == 'File';
        });

        let payload = _.map(rawResults, (result)=>{
            let retObj = {};
            retObj.type = searchType;
            retObj.repoKey = result.repoKey;

            if (searchType === 'class') {
                if (result.archivePath==='[root]') result.archivePath = '';
                retObj.name = result.name;
                retObj.archivePath = result.archivePath + result.archiveName;
            }
            else {
                if (result.relativePath==='[root]') result.relativePath = '';
                retObj.relativePath = result.relativePath;
            }

            return retObj;
        });

        return payload;
    }

    _doStashAction(action) {

        let payload = this._buildPayloadForStash();
        this.stashResultsDao[action]({name: 'stash'},payload).$promise.then((response)=>{
            if (action === 'save' && response.status === 200) {
                this.savedToStash = true;
                this.duringStashAnimation = false;
            }
            this._updateStashStatus();
        });
    }
    saveToStash() {
        let stashBox = $('#stash-box span'),
                stashedFly = $('#stash-animation'),
                stashResultsButton = $('#stash-results-button'),
                cssScale = 0,
                cssTop = stashResultsButton.offset().top - stashBox.offset().top,
                cssRight = stashBox.offset().left - stashResultsButton.offset().left,
                animationDuration = 800;

        this.duringStashAnimation = true;

        stashedFly.css('right', cssRight + 'px').css('top', cssTop + 'px').show().animate({
            right: (cssRight / 2) + 'px', top: (cssTop / 2)
        },{
            duration: animationDuration,
            easing: 'linear',
            step: () => {
                cssScale = cssScale + 0.015;
                stashedFly.css('transform', 'scale(' + cssScale + ')');
            },
            complete: () => {
                stashedFly.animate({right: 0, top: 0},{
                    duration: animationDuration,
                    easing: 'linear',
                    step: () => {
                        cssScale = cssScale - 0.015;
                        stashedFly.css('transform', 'scale(' + cssScale + ')');
                    },
                    complete: () => {
                        stashedFly.hide();

                        cssScale = 0;
                        stashBox.animate({'text-indent': 100},{
                            duration: 500,
                            easing: 'linear',
                            step: function(now) {
                                cssScale = cssScale + (now < 50 ? 0.008 : -0.008);
                                stashBox.css('transform', 'scale(' + (1 + cssScale) + ')');
                            },
                            complete: () => {
                                stashBox.css('text-indent', 0);

                                this._doStashAction('save');

                            }});
                    }});
            }});

    }

    addToStash() {
        this._doStashAction('add');
    }

    subtractFromStash() {
        this._doStashAction('subtract');
    }

    intersectWithStash() {
        this._doStashAction('intersect');
    }

    gotoStash() {
        this._clearSearchTab();
        this.artifactoryEventBus.dispatch(EVENTS.ACTION_REFRESH_STASH);
        this.$state.go('artifacts.browsers.path', {browser: 'stash', artifact: '', tab: 'StashInfo'});
    }

    clearStash() {
        this.modal.confirm('Are you sure you want to clear stashed results? All items will be removed from stash.','Clear Stashed Results', {confirm: 'Clear'})
                .then(() => {
                    this.stashResultsDao.delete({name: 'stash'}).$promise.then((response)=> {
                        this.artifactoryEventBus.dispatch(EVENTS.ACTION_DISCARD_STASH);
                        if (response.status === 200) {
                            this.savedToStash = false;
                            this._updateStashStatus();
                        }
                    });
                });
    }

    _updateStashStatus() {
        this.stashResultsDao.get({name:'stash'}).$promise.then((data)=>{
            this.stashedItemsCount = data.length;
            if (data.length === 0) {
                this.savedToStash = false;
            }
        });
    }

    hasStashPerms() {
        return this.user.currentUser.getCanDeploy();
    }

    getSelectedRecords() {
        if (!this.gridOptions.multiSelect || !this.gridOptions.api || !this.gridOptions.api.selection) return '';

        let count = this.gridOptions.api.selection.getSelectedRows().length;

        return count;
    }

    setShowAQL(show) {
        this.showAQL = show;
    }

    filterReposLimitByPackageType(packageType) {

        let lastIncluded = this.query.selectedRepositories;// (this.$stateParams.searchParams && this.$stateParams.searchParams.selectedRepos) ? this.$stateParams.searchParams.selectedRepos : [];

        let filterFunc = (repo)=>{
            let ret;
            if (packageType.startsWith('docker')) {
                if (packageType.endsWith('V1')) ret = repo.repoType.toLowerCase() === 'docker' && repo.dockerApiVersion === 'V1';
                else if (packageType.endsWith('V2')) ret = repo.repoType.toLowerCase() === 'docker' && repo.dockerApiVersion === 'V2';
            }
            else if (packageType === 'rpm') {
                ret = repo.repoType.toLowerCase() === 'yum';
            }
            else if (packageType === 'gavc') {
                ret = repo.repoType.toLowerCase() === 'maven' || repo.repoType.toLowerCase() === 'ivy' || repo.repoType.toLowerCase() === 'sbt' || repo.repoType.toLowerCase() === 'gradle';
            }
            else ret = repo.repoType.toLowerCase() === packageType.toLowerCase();

            return ret;
        };

        this.query.selectedRepositories = _.filter(lastIncluded, filterFunc);;

        this.repoList = _.filter(this.allRepoList,(repo)=>{
            return filterFunc(repo) && !_.find(lastIncluded,{repoKey: repo.repoKey})
        });
    }


    _getColumns() {

        switch (this.currentSearch) {
            case 'package':
            {
                return this._getColumnsForPackageSearch(this.packageSearchColumns)
            }
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
                        cellTemplate: this.commonGridColumns.downloadableColumn('autotest-quick-artifact'),
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
                        cellTemplate: '<div class="autotest-quick-path ui-grid-cell-contents">{{ row.entity.relativeDirPath}}</div>',
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
                        cellTemplate: '<div class="autotest-quick-repository ui-grid-cell-contents">{{ row.entity.repoKey}}</div>',
                        width: '15%'
                    },
                    {
                        name: "Modified",
                        displayName: "Modified",
                        cellTemplate: '<div class="autotest-quick-modified ui-grid-cell-contents">{{ row.entity.modifiedString }}</div>',
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
            case 'trash':
            {
                return [
                    {
                        name: "Artifact",
                        displayName: "Artifact",
                        field: "name",
                        sort: {
                            direction: this.uiGridConstants.ASC
                        },
                        cellTemplate: this.commonGridColumns.downloadableColumn('autotest-trash-artifact'),
                        width: '25%',
                        customActions: [
                            {
                                icon: 'icon icon-view',
                                tooltip: 'View',
                                callback: row => this.viewCodeArtifact(row),
                                visibleWhen: row => _.contains(row.actions, 'View')
                            },
                            {
                                icon: 'icon icon-trashcan-restore',
                                tooltip: 'Restore To Original Path',
                                callback: row => this.restoreTrashItem(row),
                                visibleWhen: row => _.contains(row.actions, 'Restore')
                            }
                        ],
                        actions: {
                            download: {
                                callback: row => this.downloadSelectedItems(row),
                                visibleWhen: row => _.contains(row.actions, 'Download')
                            }
                        }
                    },
                    {
                        name: "Original Path",
                        displayName: "Original Path",
                        field: "relativeDirPath",
                        headerCellTemplate: headerCellGroupingTemplate,
                        cellTemplate: '<div class="autotest-trash-origin-path ui-grid-cell-contents">{{ row.entity.relativeDirPath}}</div>',
                        width: '40%',
                        customActions: [{
                            icon: 'icon icon-show-in-tree',
                            tooltip: 'Show In Tree',
                            callback: row => this.showInTree(row),
                            visibleWhen: row => _.contains(row.actions, 'ShowInTree')
                        }]
                    },
                    {
                        name: "Original Repository",
                        displayName: "Original Repository",
                        field: "originRepository",
                        headerCellTemplate: headerCellGroupingTemplate,
                        cellTemplate: '<div class="autotest-trash-origin-repository ui-grid-cell-contents">{{ row.entity.originRepository}}</div>',
                        width: '15%'
                    },
                    {
                        name: "Deleted Time",
                        displayName: "Deleted Time",
                        cellTemplate: '<div class="autotest-trash-deleted ui-grid-cell-contents">{{ row.entity.deletedTimeString }}</div>',
                        field: "deletedTime",
                        width: '20%',
                        customActions: [
                            {
                                icon: 'icon icon-clear',
                                tooltip: 'Delete Permanently',
                                callback: row => this._deleteSelected([row],true),
                                visibleWhen: row => _.contains(row.actions, 'Delete'),
                            }
                        ]
                    }
                ]
            }

        }
    }

    _createPackageSearchColumnsObject() {
        this.packageSearchColumnsObject = {
            artifact: {
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
            path: {
                name: "Path",
                displayName: "Path",
                field: "relativePath",
                headerCellTemplate: headerCellGroupingTemplate,
                width: '40%',
                customActions: [{
                    icon: 'icon icon-show-in-tree',
                    tooltip: 'Show In Tree',
                    callback: row => this.showInTree(row),
                    visibleWhen: row => _.contains(row.actions, 'ShowInTree')
                }]
            },
            repo: {
                name: "Repository",
                displayName: "Repository",
                field: "repoKey",
                headerCellTemplate: headerCellGroupingTemplate,
                width: '15%'
            },
            modified: {
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
            },
            groupID: {
                name: 'Group ID',
                displayName: 'Group ID',
                field: 'groupID',
                headerCellTemplate: headerCellGroupingTemplate,
                width: '18%'
            },
            artifactID: {
                name: 'Artifact ID',
                displayName: 'Artifact ID',
                field: 'artifactID',
                headerCellTemplate: headerCellGroupingTemplate,
                width: '18%'
            },
            version: {
                name: 'Version',
                displayName: 'Version',
                field: 'version',
                headerCellTemplate: headerCellGroupingTemplate,
                width: '18%'
            },
            classifier: {
                name: 'Classifier',
                displayName: 'Classifier',
                field: 'classifier',
                headerCellTemplate: headerCellGroupingTemplate,
                width: '18%'
            }
        }

    }

    _getColumnsForPackageSearch(columns) {
        let columnsArray = [];
        columns.forEach((column)=>{
            if (!_.contains(column,'*')) {
                columnsArray.push(_.clone(this.packageSearchColumnsObject[column]));
            }
            else {
                let groupable = false;
                let width;
                if (_.contains(column,'@')) {
                    column = column.split('@').join('');
                    groupable = true;
                }
                if (_.contains(column,'!')) {
                    let splitted = column.split('!');
                    column = splitted[0];
                    width = splitted[1];
                }

                let splitted = column.split('*');
                let field = splitted[0];
                let name = splitted[1];
                columnsArray.push({
                    name: name,
                    displayName: name,
                    field: 'extraField_'+field,
                    width: width || '18%',
                    headerCellTemplate: groupable ? headerCellGroupingTemplate : undefined

                });
            }
        });

        this._normalizeGridColumnWidths(columnsArray);

        if (!columnsArray[0].actions) columnsArray[0].actions = {};
        if (!columnsArray[0].actions.download) {
            columnsArray[0].actions.download =  {
                callback: row => this.downloadSelectedItems(row),
                visibleWhen: row => _.contains(row.actions, 'Download')
            }
        }

        //If no path field add 'show in tree' action to first column
        if(_.findIndex(columnsArray, 'name', 'Path') < 0) {
            if (!columnsArray[0].customActions) columnsArray[0].customActions = [];
            columnsArray[0].customActions = [{
                icon: 'icon icon-show-in-tree',
                tooltip: 'Show In Tree',
                callback: row => this.showInTree(row),
                visibleWhen: row => _.contains(row.actions, 'ShowInTree')
            }]
        }

        return columnsArray;
    }

    _normalizeGridColumnWidths(columnsArray) {
        let totalWidth = 0;
        for (let key in columnsArray) {
            let obj = columnsArray[key];
            totalWidth += parseInt(obj.width);
        }
        let scale = 100/totalWidth;
        for (let key in columnsArray) {
            let obj = columnsArray[key];
            let origWidth = parseInt(obj.width);
            obj.width = (origWidth*scale) + '%';
        }

    }
}
