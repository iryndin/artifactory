import EVENTS     from '../../constants/artifacts_events.constants';
class jfSearchController {
    constructor($scope, $state, $stateParams, ArtifactoryEventBus, ArtifactoryState, ArtifactoryFeatures, User) {
        this.$stateParams = $stateParams;
        this.currentSearch = this.$stateParams.searchType || '';
        this.user = User.currentUser;
        this.searchTabs = this.getSearchTabs();
        this.$state = $state;
        this.collapseSearchPanel = this.$stateParams.searchType ? false : true;
        this.artifactoryState = ArtifactoryState;
        this.artifactoryEventBus = ArtifactoryEventBus;
        this.features = ArtifactoryFeatures;
        ArtifactoryEventBus.dispatch(EVENTS.SEARCH_COLLAPSE, this.collapseSearchPanel);
        ArtifactoryEventBus.registerOnScope($scope, EVENTS.CLEAR_SEARCH, () => {
            this.collapseSearchPanel = true;
            this.currentSearch = '';
        });
        ArtifactoryEventBus.registerOnScope($scope, EVENTS.SEARCH_URL_CHANGED, (stateParams) => {
            if (this.currentSearch != stateParams.searchType) {            
                this.selectSearch(stateParams.searchType, stateParams.params);
            }
        });
    }

    onClick(tab) {
        if (this.features.isEnabled(tab.feature)) this.selectSearch(tab.key);
    }

    selectSearch(_currentSearch, params) {
        if (this.collapseSearchPanel) {
            this.collapseSearchPanel = false;
        }
        else if (this.currentSearch == _currentSearch) {
            this.collapseSearchPanel = true;
            this.currentSearch = '';

            var state = this.artifactoryState.getState('lastTreeState');
            if (state) {
                this.$state.go(state.name, state.params);
            }
            else {
                this.$state.go('artifacts.browsers.path', {tab: 'General', artifact: '', browser: 'tree'})
            }
        }

        if (!this.collapseSearchPanel) {
            this.currentSearch = _currentSearch;
            this.$state.go('artifacts.browsers.search', {'searchType': _currentSearch, params: params || ''});
        }
        this.artifactoryEventBus.dispatch(EVENTS.SEARCH_COLLAPSE, this.collapseSearchPanel);
    }

    getSearchTabs() {
        let tabs = [
            {key: 'quick', value: 'Quick'},
            {key: 'package', value: 'Package'},
            {key: 'class', value: 'Archive'},
            {key: 'property', value: 'Property', feature: 'properties'},
            {key: 'checksum', value: 'Checksum'},
            {key: 'remote', value: 'Remote'}
        ];

/*
        if (this.user.isAdmin()) {
            tabs.push({key: 'trash', value: 'Trash'});
        }
*/

        return tabs;
    }

    isActiveTab(tab) {
        return this.currentSearch == tab && !this.collapseSearchPanel;
    }
}

export function jfSearch() {
    return {
        controller: jfSearchController,
        controllerAs: 'jfSearch',
        templateUrl: 'directives/jf_search/jf_search.html'
    }
}
