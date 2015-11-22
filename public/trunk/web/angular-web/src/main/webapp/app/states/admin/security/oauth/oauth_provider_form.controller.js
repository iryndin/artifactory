import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminSecurityOAuthProviderFormController {

    constructor($state, $stateParams, ArtifactoryModelSaver, OAuthDao) {
        this.OAuthDao = OAuthDao;
        this.$state = $state;
        this.TOOLTIP = TOOLTIP.admin.security.OAuthSSO;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['providerData']);

        this.selectizeConfig = {
            sortField: 'text',
            create: false,
            maxItems: 1
        };


        if ($stateParams.providerName) {
            this.mode = 'edit';
            this.providerName = $stateParams.providerName;
            this.title = 'Edit ' + this.providerName + ' Provider';
        }
        else {
            this.mode = 'create';
            this.providerData = {
                enabled: true
            };
            this.title = 'Add New Provider';
        }

        this._getData();

    }

    _getData() {
        this.OAuthDao.get().$promise.then((data) => {
            this._setMandatoryFieldsData(data.availableTypes);

            this.selectizeOptions = _.map(data.availableTypes, (t)=>Object({text:t.displayName,value:t.type}));
            if (this.mode === 'create') {
                this.providerData.providerType = data.availableTypes[0].type;
                this._setDefaultValues();
            }
            else if (this.mode === 'edit') {
                this.providerData = _.findWhere(data.providers,{name: this.providerName});
            }
            this.artifactoryModelSaver.save();
        });
    }

    _setMandatoryFieldsData(typesData) {
        this.mandatoryFields = {};
        typesData.forEach((typeRec) => {
            this.mandatoryFields[typeRec.type] = {};
            typeRec.mandatoryFields.forEach((field) => {
                this.mandatoryFields[typeRec.type][field] = typeRec.fieldsValues[typeRec.mandatoryFields.indexOf(field)] || '';
            });
        });
    }

    _setDefaultValues() {
        this.providerData.apiUrl = this.providerData.authUrl = this.providerData.tokenUrl = this.providerData.basicUrl = '';
        for (let key in this.mandatoryFields[this.providerData.providerType]) {
            this.providerData[key] = this.mandatoryFields[this.providerData.providerType][key];
        };
    }

    onChangeProviderType() {
        this._setDefaultValues()
    }

    save() {
        if (this.mode === 'edit') {
            this.OAuthDao.updateProvider(this.providerData).$promise.then(()=>{
                this.artifactoryModelSaver.save();
                this.$state.go('^.oauth');
            })
        }
        else if (this.mode === 'create') {
            this.OAuthDao.createProvider(this.providerData).$promise.then(()=>{
                this.artifactoryModelSaver.save();
                this.$state.go('^.oauth');
            })
        }
    }

    cancel() {
        this.$state.go('^.oauth');
    }

    isSaveDisabled() {
        return !this.providerForm.$valid;
    }
}