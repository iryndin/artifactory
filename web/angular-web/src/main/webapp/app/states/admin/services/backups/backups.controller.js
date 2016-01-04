export class AdminServicesBackupsController {
    constructor($scope, BackupDao, ArtifactoryGridFactory, ArtifactoryModal, RepoDataDao, $q, commonGridColumns) {
        this.backupDao = BackupDao;
        this.repoDataDao = RepoDataDao;
        this.modal = ArtifactoryModal;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.$scope = $scope;
        this.commonGridColumns = commonGridColumns;
        this._createGrid();
        this._initBackups();
        this.$q = $q;
    }

    _initBackups() {
        this.backupDao.query().$promise.then((backups)=> {
            this.gridBackupsOptions.setGridData(backups)
        });
    }

    _createGrid() {
        this.gridBackupsOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setColumns(this._getColumns())
                .setButtons(this._getActions())
                .setRowTemplate('default')
                //.setMultiSelect()
                //.setBatchActions(this._getBatchActions());
    }

    _doDeleteBackup(key) {
        return this.backupDao.delete({key: key}).$promise;
    }

   /* deleteSelectedBackups() {
        let selectedRows = this.gridBackupsOptions.api.selection.getSelectedGridRows();
        this.modal.confirm(`Are you sure you want to delete ${selectedRows.length} backups?`)
            .then(() => {
                return this.$q.all(selectedRows.map((row) => this._doDeleteBackup(row.entity.key)))
            })
            .then(() => this._initBackups())
    }*/

    deleteBackup(key) {
        this.modal.confirm(`Are you sure you want to delete the backup '${key}'?`)
            .then(() => this._doDeleteBackup(key))
            .then(() => this._initBackups())
    }

    _getColumns() {
        return [
            {
                field: "key",
                cellTemplate: '<div class="ui-grid-cell-contents"><a ui-sref="^.backups.edit({backupKey: row.entity.key})">{{ COL_FIELD }}</a></div>',
                width: "15%"
            },
            {
                name: "Repositories",
                displayName: "Repositories",
                field: "includeRepos",
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.includeRepos','row.entity.key'),
                width: "30%"
            },
            {
                name: "Cron Expression",
                displayName: "Cron Expression",
                field: "cronExp",
                cellTemplate: '<div class="ui-grid-cell-contents">{{ COL_FIELD }}</div>',
                width: "15%"
            },
            {
                name: "Next Schedule Backup",
                displayName: "Next Schedule Backup",
                field: "cronExp",
                cellTemplate: '<div class="ui-grid-cell-contents"><input class="not-like-input" type="text" jf-cron-formatter ng-model="row.entity.cronExp" disabled></div>',
                width: "20%"
            },
            {
                field: "enabled",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.enabled'),
                width: "10%"
            },
            {
                name: 'Run Now',
                displayName: 'Run Now',
                field: 'runnow',
                cellTemplate: '<div class="ui-grid-cell-contents text-center"><a class="grid-column-button icon icon-run" ng-click="grid.appScope.AdminServicesBackups.runNow(row.entity)" ng-disabled="!row.entity.enabled"></a></div>',
                width: '10%',
                enableSorting: false
            }
        ]
    }

    _getActions() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Delete',
                callback: entity => this.deleteBackup(entity.key)
            }
        ];
    }

    runNow(row) {
        if (row.enabled) {
            this.backupDao.get({key: row.key}).$promise
                    .then((backup) => {
                        this.backupDao.runNow({},backup).$promise.then((res)=>{
                            //console.log(res);
                        });
                    });
        }
    }
/*    _getBatchActions() {
        return [
            {
                icon: 'clear',
                name: 'Delete',
                callback: () => this.deleteSelectedBackups()
            },
        ]
    }*/
}