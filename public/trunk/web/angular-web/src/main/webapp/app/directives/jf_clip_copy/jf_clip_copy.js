class jfClipCopyController {

    constructor($timeout) {

        this.FEEDBACK_DELAY = 4000;

        this.$timeout = $timeout;

        if (this.objectName) {
            this.origTooltip = this.tooltipText = "Copy " + this.objectName.toLowerCase() + " to clipboard";
        }
        else {
            this.origTooltip = this.tooltipText = "Copy to clipboard";
        }
        this.timeoutPromise = null;

    }

    onClipClick() {

        if (!this.textToCopy) return;

        if (this.timeoutPromise) {
            this.$timeout.cancel(this.timeoutPromise);
            this.timeoutPromise = null;
        }

        this.tooltipText = this.objectName ? (this.objectName.charAt(0).toUpperCase() +  this.objectName.substr(1) + ' copied to clipboard') : 'Value copied to clipboard';
        this.timeoutPromise = this.$timeout(()=>{
            this.tooltipText = this.origTooltip;
        },this.FEEDBACK_DELAY);
        
    }
}

export function jfClipCopy($compile) {

    return {
        restrict: 'E',
        scope: {
            textToCopy: '=',
            objectName: '@'
        },
        controller: jfClipCopyController,
        controllerAs: 'jfClipCopy',
        bindToController: true,
        templateUrl: 'directives/jf_clip_copy/jf_clip_copy.html',
        link: ($scope,$element,$attrs) => {
            let classList = $element[0].classList;
            let outer = angular.element($element[0]);
            let inner = angular.element($element[0].querySelector('a'));

            for (let i = classList.length-1; i >= 0; i--) {
                let cls = classList[i];
                if (!cls.startsWith('ng-')) {
                    outer.removeClass(cls);
                    inner.addClass(cls);
                }
            }
        }
    };
}
