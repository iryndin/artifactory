class jfMessagesController {
    constructor(ArtifactoryState) {
        this.artifactoryState = ArtifactoryState;
    }


    getMessages() {
        return this.artifactoryState.getState('constantMessages');
    }

}

export function jfMessages() {
    return {
        controller: jfMessagesController,
        controllerAs: 'jfMessages',
        bindToController: true,
        templateUrl: 'directives/jf_messages/jf_messages.html'
    }
}
