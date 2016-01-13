export class AdminConfigurationReverseProxiesController {

    constructor($scope, ReverseProxiesDao, ArtifactoryGridFactory, ArtifactoryModal, $q, uiGridConstants, commonGridColumns) {
        this.gridOptions = {};
        this.commonGridColumns = commonGridColumns;
        this.uiGridConstants = uiGridConstants;
        this.reverseProxiesDao = ReverseProxiesDao;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.modal = ArtifactoryModal;
        this.$scope=$scope;
        this.$q = $q;

        this._createGrid();
        this._initReverseProxies();
    }

    _initReverseProxies() {
        this.reverseProxiesDao.get().$promise.then((reverseProxies)=> {
            this.gridOptions.setGridData(reverseProxies)
        });
    }

    _createGrid() {
        this.gridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setColumns(this._getColumns())
                .setMultiSelect()
                .setButtons(this._getButtons())
                .setBatchActions(this._getBatchActions())
                .setRowTemplate('default');
    }

    deleteSelectedReverseProxies() {
        let selectedRows = this.gridOptions.api.selection.getSelectedRows();
        this.modal.confirm(`Are you sure you want to delete ${selectedRows.length} reverse proxies?`)
            .then(() => {
                    let keys = _.map(selectedRows, (row) => {return row.key;});
                    this.reverseProxiesDao.delete({proxyKeys: keys}).$promise
                            .then(()=>this._initReverseProxies());
            })
            .then(() => this._initReverseProxies());
    }

    deleteReverseProxy(key) {
        this.modal.confirm(`Are you sure you want to delete the reverse proxy '${key}'?`)
            .then(() => this._doDeleteReverseProxy(key))
            .then(() => this._initReverseProxies());
    }

    _doDeleteReverseProxy(key) {
        return this.reverseProxiesDao.delete({proxyKeys:[key]}).$promise;
    }

    _getColumns() {
        return [
            {
                field: "key",
                name: "Key",
                displayName: "Key",
                sort: {
                    direction: this.uiGridConstants.ASC
                },
                cellTemplate: '<div class="ui-grid-cell-contents"><a ui-sref="^.reverse_proxies.edit({reverseProxyKey: row.entity.key})">{{ COL_FIELD }}</a></div>',
                width: '30%'
            },
            {
                field: "webServerType",
                name: "Web Server Type",
                displayName: "Web Server Type",
                width: '20%'
            },
            {
                field: "serverName",
                name: "Server Name",
                displayName: "Server Name",
                width: '50%'
            }
        ]
    }
    _getBatchActions() {
        return [
            {
                icon: 'clear',
                name: 'Delete',
                callback: () => this.deleteSelectedReverseProxies()
            }
        ]
    }

    _getButtons() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Delete',
                callback: row => this.deleteReverseProxy(row.key)
            }

        ];
    }

}