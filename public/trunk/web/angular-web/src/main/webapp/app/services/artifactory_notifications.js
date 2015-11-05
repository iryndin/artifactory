/**
 * wrapper around the ngToast service
 * @url http://tamerayd.in/ngToast/#
 */
export class ArtifactoryNotifications {

    constructor(toaster, $timeout) {
        this.toast = toaster;
        this.$timeout = $timeout;
        this.lastNotification = null;
    }

    create(message) {
        if (message.info) {
            if (this.lastNotification == message.info) {
                return false
            }
            this.toast.pop({
                type: 'success',
                timeout: message.timeout || 5000,
                body: message.info,
                showCloseButton: true,
                clickHandler: this.notifClickHandle
            });
            this.lastNotification = message.info;
            this.$timeout(() => {
                this.lastNotification = null
            }, message.timeout || 5000);
            //this.toast.create({animation:'fade',content:message.info});
        }

        if (message.error) {
            if (this.lastNotification == message.error) {
                return false
            }
            this.toast.pop({
                type: 'error',
                timeout: message.timeout || 10000,
                body: message.error,
                showCloseButton: true,
                clickHandler: this.notifClickHandle
            });
            this.lastNotification = message.error;
            this.$timeout(() => {
                this.lastNotification = null
            }, message.timeout || 5000);
            //this.toast.danger({animation:'fade',content:message.error});
        }
        if(message.warn) {
            if (this.lastNotification == message.warn) {
                return false
            }
            this.toast.pop({
                type: 'warning',
                timeout: message.timeout || 4000,
                body: message.warn,
                showCloseButton: true,
                clickHandler: this.notifClickHandle
            });
            this.lastNotification = message.warn;
            this.$timeout(() => {
                this.lastNotification = null
            }, message.timeout || 1000);
        }
    }

    notifClickHandle(toast, isCloseButton) {
        return isCloseButton;
    }

    /**
     * Show toast with HTML content
     *
     * @param message {{type: string, body: string}}
     */
    createMessageWithHtml(message) {
        this.toast.pop({
            type: message.type,
            body: message.body,
            bodyOutputType: 'trustedHtml',
            timeout: message.timeout,
            showCloseButton: true,
            clickHandler: this.notifClickHandle
        });
    }

    clear() {
        this.toast.clear();
    }

}