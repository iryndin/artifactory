import EVENTS from '../../constants/artifacts_events.constants';
class jfSpinnerController {

    constructor($scope, ArtifactoryEventBus) {
        this.$scope = $scope;
        this.show = false;
        this.artifactoryEventBus = ArtifactoryEventBus;

        this.intervalPromise = null;


        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.SHOW_SPINNER, () => this.showSpinner());
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.HIDE_SPINNER, () => this.hideSpinner());

    }

    showSpinner() {
        this.show = true;

    }

    hideSpinner() {
        this.show = false;
    }


}

export function jfSpinner() {

    return {
        restrict: 'E',
        scope: {},
        replace: true,
        controller: jfSpinnerController,
        controllerAs: 'jfSpinner',
        templateUrl: 'directives/jf_spinner/jf_spinner.html',
        bindToController: true
    };
}
