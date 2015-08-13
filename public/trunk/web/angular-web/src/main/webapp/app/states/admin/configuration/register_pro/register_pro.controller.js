import EVENTS from '../../../../constants/artifacts_events.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminConfigurationRegisterController{

    constructor(RegisterProDao, ArtifactoryEventBus,User) {
        this.registerProDao = RegisterProDao;
        this.getData();
        this.ArtifactoryEventBus = ArtifactoryEventBus;
        this.User=User;
        this.TOOLTIP = TOOLTIP.admin.configuration.registerPro;
    }

    save(registerDetails) {
        this.registerProDao.update(registerDetails).$promise.then( (data)=> {
            // Refresh the home page footer with the new license details
            this.ArtifactoryEventBus.dispatch(EVENTS.FOOTER_REFRESH);
            this.getData();
            this.User.loadUser(true);
        });
    }

    getData() {
        this.registerDetails = this.registerProDao.get();
    }
}