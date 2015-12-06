import EVENTS     from '../../../../constants/common_events.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminSecurityHttpSSoController {

    constructor(HttpSsoDao,ArtifactoryEventBus,ArtifactoryModelSaver) {
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.httpSsoDao = HttpSsoDao.getInstance();
        this.sso = this.getSsoData();
        this.TOOLTIP = TOOLTIP.admin.security.HTTPSSO;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['sso']);

    }

    getSsoData() {
        this.httpSsoDao.get().$promise.then((sso)=> {
            this.sso = sso;
            this.artifactoryModelSaver.save();
            this.artifactoryEventBus.dispatch(EVENTS.FORM_CLEAR_FIELD_VALIDATION, true);
        });
    }

    reset() {
        this.artifactoryModelSaver.ask().then(()=>{
            this.getSsoData();
        });
    }
    save(sso) {
        this.httpSsoDao.update(sso).$promise.then(()=>{
            this.artifactoryModelSaver.save();
        });
    }
}