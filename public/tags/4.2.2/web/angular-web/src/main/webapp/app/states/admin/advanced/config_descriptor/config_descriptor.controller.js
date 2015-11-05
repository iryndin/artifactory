export class AdminAdvancedConfigDescriptorController {

    constructor($timeout, ArtifactoryHttpClient, ArtifactoryNotifications, RESOURCE, ArtifactoryModelSaver) {
        this.$timeout = $timeout;
        this.RESOURCE = RESOURCE;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.artifactoryHttpClient = ArtifactoryHttpClient;
        this.configDescriptor = '';
        this.apiAccess = {};
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['configDescriptor']);

        this._getData();
    }

    _getData() {
        this.artifactoryHttpClient.get(this.RESOURCE.CONFIG_DESCRIPTOR).then((response) => {
                this.configDescriptor = response.data;
                this.artifactoryModelSaver.save();
                this.$timeout(()=> {
                    this.apiAccess.api.clearHistory();
                });
            }
        );
    }

    save(configXml) {
        this.artifactoryHttpClient.put(this.RESOURCE.CONFIG_DESCRIPTOR, {configXml})
            .success(response => {
                this.artifactoryModelSaver.save();
                this.artifactoryNotifications.create(response)
            })
            .error(response => {
                if (response.errors && response.errors.length) {
                    this.artifactoryNotifications.create(angular.fromJson(response.errors[0].message));
                }
            });
    }

    cancel() {
        this.artifactoryModelSaver.ask().then(()=>{
            this._getData();
        });
    }

}
