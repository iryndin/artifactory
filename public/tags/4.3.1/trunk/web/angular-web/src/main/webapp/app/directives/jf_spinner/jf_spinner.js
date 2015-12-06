import EVENTS from '../../constants/artifacts_events.constants';
class jfSpinnerController {

    constructor($scope, $state, ArtifactoryEventBus,$element) {
        this.$scope = $scope;
        this.$state = $state;
        this.$element = $element;
        this.show = false;
        this.count = 0;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.intervalPromise = null;

        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.SHOW_SPINNER, (domain) => {
            this.showSpinner(domain);
        });
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.HIDE_SPINNER, () => this.hideSpinner());

    }

    showSpinner(domain) {
        if ((!domain && this.domain === 'body' && this.$state.current.name === 'login') || (!domain && this.domain === 'content' && this.$state.current.name !== 'login') || (this.domain === domain) ) {

            this.count++;
            this.show = true;
//            console.log(this.count,'show')
        }
    }

    hideSpinner() {
        this.count--;
        if (this.count<0) this.count = 0;
//        console.log(this.count)
        if (this.count === 0) {
//            console.log('hide')
            this.show = false;
        }
    }


}

export function jfSpinner() {

    return {
        restrict: 'E',
        scope: {
            domain: '@'
        },
/*
        replace: true,
*/
        controller: jfSpinnerController,
        controllerAs: 'jfSpinner',
        templateUrl: 'directives/jf_spinner/jf_spinner.html',
        bindToController: true
    };
}
