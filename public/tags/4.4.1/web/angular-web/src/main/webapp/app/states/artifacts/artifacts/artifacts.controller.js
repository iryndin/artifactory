import EVENTS   from '../../../constants/artifacts_events.constants';
import TOOLTIPS from '../../../constants/artifact_tooltip.constant';
import ICONS from '../constants/artifact_browser_icons.constant';

export class ArtifactsController {
    constructor($scope, $state, ArtifactoryEventBus, ArtifactoryState, SetMeUpModal, ArtifactoryDeployModal, User) {

        this.artifactoryEventBus = ArtifactoryEventBus;
        this.$state = $state;
        this.$scope = $scope;
        this.node = null;
        this.deployModal = ArtifactoryDeployModal;
        this.setMeUpModal = SetMeUpModal;
        this.artifactoryState = ArtifactoryState;
        this.tooltips = TOOLTIPS;
        this.icons = ICONS;

        this.user = User.getCurrent();

        this.initEvent();
    }


    getNodeIcon() {
        if (this.node && this.node.data) {
            let type = this.icons[this.node.data.iconType];
            if (!type) type = this.icons['default'];
            return type && type.icon;
        }
    }


    openSetMeUp() {
        this.setMeUpModal.launch(this.node);
    }

    openDeploy() {
        this.deployModal.launch(this.node);
    }

    initEvent() {
        this.artifactoryEventBus.registerOnScope(this.$scope, EVENTS.TREE_NODE_SELECT, node => this.selectNode(node));
    }

    selectNode(node) {
        let previousNode = this.node;
        this.node = node;
        if (node.data) {
            this.artifactoryState.setState('repoKey', this.node.data.repoKey);
            let location = true;
            if (this.$state.current.name === 'artifacts.browsers.path' && (!previousNode || (!this.$state.params.artifact && this.$state.params.tab !== 'StashInfo'))) {
                // If no artifact and selecting artifact - replace the location (fix back button bug)
                location = 'replace';
            }
            this.$state.go(this.$state.current, {artifact: node.data.fullpath}, {location: location});
        }
        else {
            this.artifactoryState.removeState('repoKey');
            this.$state.go(this.$state.current, {artifact: ''});
        }
    }

    exitStashState() {
        this.artifactoryEventBus.dispatch(EVENTS.ACTION_EXIT_STASH);
    }

    hasData() {
        return this.artifactoryState.getState('hasArtifactsData') !== false;
    }
}