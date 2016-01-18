export class AdminSecurityGroupFormController {
    constructor($scope, $state, $stateParams, $timeout, $q, ArtifactoryGridFactory, GroupsDao, UserDao, GroupPermissionsDao,
            commonGridColumns,ArtifactoryModelSaver,RepositoriesDao) {
        this.DEFAULT_REALM = "artifactory";
        this.$scope = $scope;
        this.$state = $state;
        this.$stateParams = $stateParams;
        this.$timeout = $timeout;
        this.$q = $q;
        this.repositoriesDao = RepositoriesDao;
        this.userDao = UserDao.getInstance();
        this.groupsDao = GroupsDao.getInstance();
        this.groupPermissionsDao = GroupPermissionsDao.getInstance();
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.permissionsGridOptions = {};
        this.commonGridColumns = commonGridColumns;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['groupdata']);
        this.input = {};


        if ($stateParams.groupname) {
            this.mode = 'edit';
            this.groupname = $stateParams.groupname;
            this.title = 'Edit ' + this.groupname + ' Group';
            this._getGroupData();
            this._createGrid();
            this._getPermissions();
        }
        else {
            this.mode = 'create';
            this.title = 'Add New Group';
            this.groupdata = {};
        }

        this._getAllRepos();
        this._getAllUsers();

    }

    _getAllRepos() {
        this.reposData = {};
        this.repositoriesDao.getRepositories({type:'local'}).$promise
                .then((data) => {
                    this.reposData.locals = _.map(data,(r)=>{return r.repoKey;});
                });
        this.repositoriesDao.getRepositories({type:'remote'}).$promise
                .then((data) => {
                    this.reposData.remotes = _.map(data,(r)=>{return r.repoKey;});
                });
    }

    _getGroupData() {
        this.groupsDao.getSingle({name: this.groupname}).$promise.then((data) => {
            //console.log(data);
            this.groupdata = data;
            this.artifactoryModelSaver.save();
        });
    }

    _getAllUsers() {
        this.userDao.getAll().$promise.then((data)=> {
            this.usersData = data;
            this.usersList = _.map(data, (user)=> {
                return user.name;
            });

        });
    }


    _createGrid() {
        this.permissionsGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
            .setColumns(this._getPermissionCloumns())
            .setRowTemplate('default');

    }

    _getPermissions() {
        this.groupPermissionsDao.get({groups: [this.groupname]}).$promise.then((data)=> {
            //console.log(data);
            this._fixDataFormat(data).then((fixedData)=>{
                this.permissionsGridOptions.setGridData(fixedData);
            });
        });
    }

    _fixDataFormat(data,defer = null) {
        let defer = defer || this.$q.defer();
        if (this.reposData.locals && this.reposData.remotes) {
            data.forEach((record)=>{
                if (record.repoKeys.length === 1 && record.repoKeys[0] === 'ANY LOCAL') {
                    record.repoKeysView = 'ANY LOCAL';
                    record.reposList = angular.copy(this.reposData.locals);
                }
                else if (record.repoKeys.length === 1 && record.repoKeys[0] === 'ANY REMOTE') {
                    record.repoKeysView = 'ANY REMOTE';
                    record.reposList = angular.copy(this.reposData.remotes);
                }
                else if (record.repoKeys.length === 1 && record.repoKeys[0] === 'ANY') {
                    record.repoKeysView = 'ANY';
                    record.reposList = angular.copy(this.reposData.remotes).concat(this.reposData.locals);
                }
                else {
                    record.repoKeysView = record.repoKeys.join(', ');
                    record.reposList = angular.copy(record.repoKeys);
                }
            });
            defer.resolve(data);
        }
        else {
            this.$timeout(()=>{
                this._fixDataFormat(data,defer);
            })
        }
        return defer.promise;
    }

    updateGroup() {
        let payload = angular.copy(this.groupdata);
        _.extend(payload, this.input);
        this.groupsDao.update({name: this.groupdata.groupName}, payload).$promise.then((data) => {
            this.artifactoryModelSaver.save();
            this.$state.go('^.groups');
        });
    }

    createNewGroup() {
        let payload = angular.copy(this.groupdata);
        payload.realm = this.DEFAULT_REALM;
        _.extend(payload, this.input);
        this.groupsDao.create(payload).$promise.then((data) => {
            this.artifactoryModelSaver.save();
            this.$state.go('^.groups');
        });
    }

    save() {
        if (this.mode === 'edit')
            this.updateGroup();
        if (this.mode === 'create')
            this.createNewGroup();
    }

    isSaveDisabled() {
        return this.groupForm.$invalid;
    }


    cancel() {
        this.$state.go('^.groups');
    }

    _getPermissionCloumns() {

        let nameCellTemplate = '<div class="ui-grid-cell-contents"><a href ui-sref="admin.security.permissions.edit({permission: row.entity.permissionName})">{{row.entity.permissionName}}</a></div>';

        return [
            {
                field: "permissionName",
                name: "Permission Target",
                displayName: "Permission Target",
                cellTemplate: nameCellTemplate,
                width:'20%'
            },
/*
            {
                field: "effectivePermission.principal",
                displayName: "Inherited From"
            },
*/
            {
                field: "repoKeys",
                name: "Repositories",
                displayName: "Repositories",
                cellTemplate: this.commonGridColumns.listableColumn('row.entity.reposList','row.entity.permissionName','row.entity.repoKeysView',true),
                width:'25%'
            },
            {
                field: "effectivePermission.managed",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.managed'),
                name: "Manage",
                displayName: "Manage",
                width:'9%'
            },
            {
                field: "effectivePermission.delete",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.delete'),
                name: "Delete/Overwrite",
                displayName: "Delete/Overwrite",
                width:'15%'
            },
            {
                field: "effectivePermission.deploy",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.deploy'),
                name: "Deploy/Cache",
                displayName: "Deploy/Cache",
                width:'14%'
            },
            {
                field: "effectivePermission.annotate",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.annotate'),
                name: "Annotate",
                displayName: "Annotate",
                width:'9%'
            },
            {
                field: "effectivePermission.read",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.effectivePermission.read'),
                name: "Read",
                displayName: "Read",
                width:'8%'
            }
        ]
    }
}