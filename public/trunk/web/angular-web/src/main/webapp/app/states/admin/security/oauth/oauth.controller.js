import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminSecurityOAuthController {

    constructor($scope, ArtifactoryModelSaver, ArtifactoryModal, OAuthDao, ArtifactoryGridFactory, commonGridColumns, uiGridConstants) {
        this.$scope = $scope;
        this.OAuthDao = OAuthDao;
        this.modal = ArtifactoryModal;
        this.artifactoryGridFactory = ArtifactoryGridFactory;
        this.commonGridColumns = commonGridColumns;
        this.uiGridConstants = uiGridConstants;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['oauthData']);
        this.providersGridOptions = null;

        this.selectizeConfig = {
            sortField: 'text',
            create: false,
            maxItems: 1
        };
        this._createGrid();
        this._init();
    }

    _init() {
        this.OAuthDao.get().$promise.then((data)=>{
            this.oauthData = data;

            this.selectizeOptions = [{text:' ',value: '*'}];
            this.selectizeOptions = this.selectizeOptions.concat(_.map(data.providers, (p)=>Object({text:p.name,value:p.name})));
            if (!_.findWhere(data.providers,{name: data.defaultNpm})) {
                this.selectizeOptions.push({text: data.defaultNpm,value:data.defaultNpm});
            }
            data.providers.forEach((provider) => {
                provider.typeDisplayName = _.findWhere(data.availableTypes, {type: provider.providerType}).displayName;
            });
            this.providersGridOptions.setGridData(data.providers);
            this.artifactoryModelSaver.save();
        });
    }

    _createGrid() {
        this.providersGridOptions = this.artifactoryGridFactory.getGridInstance(this.$scope)
                .setColumns(this._getColumns())
                .setButtons(this._getActions())
                .setRowTemplate('default');
    }

    _getColumns() {
        return [
            {
                name: 'Name',
                displayName: 'Name',
                field: "name",
                cellTemplate: '<div class="ui-grid-cell-contents" ui-sref="^.oauth.edit({providerName: row.entity.name})"><a href="">{{row.entity.name}}</a></div>',
                sort: {
                    direction: this.uiGridConstants.ASC
                },
                width: '20%'
            },
            {
                name: 'Type',
                displayName: 'Type',
                field: "typeDisplayName",
                width: '15%'
            },
            {
                name: 'ID',
                displayName: 'ID',
                field: "id",
                width: '20%'
            },
            {
                name: 'Auth Url',
                displayName: 'Auth Url',
                field: "authUrl",
                width: '35%'
            },
            {
                name: "Enabled",
                displayName: "Enabled",
                field: "enabled",
                cellTemplate: this.commonGridColumns.booleanColumn('row.entity.enabled'),
                width: '10%'
            }
        ]
    }

    _getActions() {
        return [
            {
                icon: 'icon icon-clear',
                tooltip: 'Delete',
                callback: (row) => this.deleteProvider(row)
            }

        ];
    }

    deleteProvider(row) {
        this.modal.confirm(`Are you sure you want to delete provider '${row.name}?'`)
                .then(() => {
                    this.OAuthDao.deleteProvider({},{provider: row.name}).$promise.then(()=>{
                        this._init();
                    });
                });
    }

    save() {

        let payload = _.cloneDeep(this.oauthData);

        if (payload.defaultNpm === '*') delete payload.defaultNpm;
        delete payload.providers;
        delete payload.availableTypes;

        this.OAuthDao.update(payload).$promise.then(()=>{
            this.artifactoryModelSaver.save();
        });

    }

    cancel() {
        this.artifactoryModelSaver.ask().then(()=>{
            this._init();
        });
    }
    canSave() {
        return this.oauthForm.$valid;
    }
}