import EVENTS   from '../../../constants/artifacts_events.constants';
import TOOLTIP  from '../../../constants/artifact_tooltip.constant';

class jfTrashController {
    constructor($scope, $state, ArtifactoryEventBus) {

        this.artifactoryEventBus = ArtifactoryEventBus;
        this.$state = $state;
        this.TOOLTIP = TOOLTIP.artifacts.search.trashSearch;
        this.selectedMode = this.query.isChecksum ? 'Checksum' : 'Quick';
    }

    search() {
        this.query.isChecksum = this.selectedMode === 'Checksum';
        this.query.search = "trash";
        this.$state.go('.', {
            'searchType': this.query.search,
            'searchParams': {
                selectedRepos: this.query.selectedRepositories
            },
            'params': btoa(JSON.stringify(this.query))
        });
    }

}

export function jfTrash() {
    return {
        scope: {
            query: '='
        },
        restrict: 'EA',
        controller: jfTrashController,
        controllerAs: 'jfTrash',
        bindToController: true,
        templateUrl: 'directives/jf_search/search_tabs/jf_trash.html'
    }
}
