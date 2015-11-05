import EVENTS from '../../../../constants/common_events.constants';
import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminConfigurationMailController {

    constructor(MailDao, ArtifactoryEventBus, $timeout, ArtifactoryModelSaver) {
        this.mailDao = MailDao.getInstance();
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.getMailData();
        this.mailSettingsForm = null;
        this.testReceiptForm = null;
        this.TOOLTIP = TOOLTIP.admin.configuration.mail;
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['mail']);
        this.$timeout = $timeout;
    }

    getMailData() {
        this.mailDao.get().$promise.then((mail)=> {
            this.mail = mail;
            this.artifactoryModelSaver.save();
            this.artifactoryEventBus.dispatch(EVENTS.FORM_CLEAR_FIELD_VALIDATION, true);
        });
    }

    save(form) {
        this.artifactoryEventBus.dispatch(EVENTS.FORM_SUBMITTED, form.$name);
        if (this.mailSettingsForm.$valid) {
            this.mailDao.update(this.mail).$promise.then(()=>{
                this.artifactoryModelSaver.save();
            });
        }
    }

    reset() {
        this.artifactoryModelSaver.ask().then(()=>{
            this.getMailData();
        });

    }
    testReceipt(form) {
        this.artifactoryEventBus.dispatch(EVENTS.FORM_SUBMITTED, form.$name);
        if (this.testReceiptForm.$valid) {
            this.mailDao.save(this.mail);
        }
    }
}