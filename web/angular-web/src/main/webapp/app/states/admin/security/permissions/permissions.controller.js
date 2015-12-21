export class AdminSecurityPermissionsController {
    constructor($scope,$state, ArtifactoryGridFactory, PermissionsDao, ArtifactoryModal, uiGridConstants, User, commonGridColumns) {
        this.$state=$state;
        this.currentTab = 'repo';
        this.modal = ArtifactoryModal;
        this.permissionsDao = PermissionsDao.getInstance();
        this.$scope = $scope;
        this.commonGridColumns = commonGridColumns;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.uiGridConstants = uiGridConstants;
        this.user = User.getCurrent();
        this._createGrid();
        this.initPermission();

    }

    initPermission() {
        this.permissionsDao.getAll().$promise.then((permissions)=> {
            permissions.forEach((permission)=>{
                permission.groupsList = _.pluck(permission.groups,'principal');
                permission.usersList = _.pluck(permission.users,'principal');
                permission.reposList = _.pluck(permission.repoKeys,'repoKey');
                permission.repoKeysView = permission.repoKeysView ? (permission.repoKeysView.indexOf('|') !== -1 ? permission.repoKeysView.split('|')[1].trim() : permission.repoKeysView) : '';
            });

            this.gridOption.setGridData(permissions);
        });
    }

    showNew() {
        return this.user.isAdmin();
    }

    _createGrid() {

        this.gridOption = this.artifactoryGridFactory.getGridInstance(this.$scope)
            .setColumns(this._getColumns())
            .setRowTemplate('default')
            .setMultiSelect()
            .setButtons(this._getActions())
            .setGridData([])
            .setBatchActions(this._getBatchActions());

        this.gridOption.isRowSelectable = (row) => {
            return row.entity.name !== this.user.name;
        }
    }

    _getActions() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Delete',
                callback: row => this._deletePermission(row),
                visibleWhen: row => !_.findWhere(row.users, {principal: this.user.name})
            }
/*
            {
                icon: 'icon icon-builds',
                tooltip: 'Edit',
                callback: row => this._editPermission(row)
            }
*/
        ]
    }

    editPermission(row) {
        this.$state.go('^.permissions.edit', {permission: row.name})
    }

    _deletePermission(row) {
        let json = {permissionTargetNames:[row.name]};
        this.modal.confirm(`Are you sure you want to delete permission '${row.name}?'`)
          .then(() => this.permissionsDao.deletePermission(json).$promise.then(()=>this.initPermission()));
    }

    bulkDelete() {
        //Get All selected users
        let selectedRows = this.gridOption.api.selection.getSelectedRows();
        //Create an array of the selected permission names
        let names = _.map(selectedRows, (row) => {return row.name;});
        //Create Json for the bulk request
        let json = {permissionTargetNames: names};
        //console.log('Bulk delete....');
        //Ask for confirmation before delete and if confirmed then delete bulk of users
        this.modal.confirm(`Are you sure you want to delete ${names.length} permissions?`).
        then(() => this.permissionsDao.deletePermission(json).$promise.then(() => this.initPermission()));
    }

    _getColumns() {
        return [
            {
                name: 'Permission Target Name',
                displayName: 'Permission Target Name',
                field: 'name',
                sort: {
                    direction: this.uiGridConstants.ASC
                },
                cellTemplate: '<div class="ui-grid-cell-contents"><a href ng-click="grid.appScope.Permissions.editPermission(row.entity)">{{row.entity.name}}</a></div>'
            },
            {
                name: 'Repositories',
                displayName: 'Repositories',
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.reposList','row.entity.name','row.entity.repoKeysView',true),
                field: 'repoKeysView'
            },
            {
                name: 'Groups',
                displayName: 'Groups',
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.groupsList','row.entity.name'),
                field: 'groupsList'

            },
            {
                name: 'Users',
                displayName: 'Users',
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.usersList','row.entity.name'),
                field: 'usersList'
            }
        ]
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
}