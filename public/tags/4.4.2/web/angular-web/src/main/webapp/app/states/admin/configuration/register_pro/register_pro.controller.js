import EVENTS from '../../../../constants/artifacts_events.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminConfigurationRegisterController{

    constructor(RegisterProDao, ArtifactoryEventBus,User, $state,ArtifactoryModelSaver) {
        this.registerProDao = RegisterProDao;
        this.$state = $state;
        this.ArtifactoryEventBus = ArtifactoryEventBus;
        this.User=User;
        this.TOOLTIP = TOOLTIP.admin.configuration.registerPro;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['registerDetails']);
        this.getData();
    }

    save(registerDetails) {
        this.registerProDao.update(registerDetails).$promise.then( (data)=> {
            // Refresh the home page footer with the new license details
            this.ArtifactoryEventBus.dispatch(EVENTS.FOOTER_REFRESH);
            this.User.loadUser(true);
            this.artifactoryModelSaver.save();
            if (data.status === 200) this.$state.go('home');
        });
    }

    getData() {
        if(!this.User.currentUser.isProWithoutLicense()) {
            this.registerProDao.get().$promise.then((data)=>{
                this.registerDetails = data;
                this.artifactoryModelSaver.save();
            });
        }
    }

    reset() {
        this.artifactoryModelSaver.ask().then(()=>{
            this.getData();
        });

    }

}