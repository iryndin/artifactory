class jfMessagesController {
    constructor($scope, $state, $window, ArtifactoryState, User) {

        this.$state = $state;
        this.user = User;
        this.artifactoryState = ArtifactoryState;
        this.$window = $window;

        angular.element(this.$window).on('resize', this.handleSizing.bind(this));
        $scope.$on('$destroy', () => {
            angular.element(this.$window).off('resize');
        });

        setTimeout(() => {
            this.handleSizing();

            $(document).on('mouseenter', '.message-text a', () => {
                $('.message-container').addClass('pause-animation')
            });
            $(document).on('mouseleave', '.message-text a', () => {
                $('.message-container').removeClass('pause-animation')
            });
        }, 300);
    }

    getConstantMessages() {
        let msgs = this.artifactoryState.getState('constantMessages');
        if (msgs) this.addPasswordExpirationMessages(msgs);
        return msgs;
    }

    addPasswordExpirationMessages(msgs) {
        let daysToExpiration = this.user.currentUser.currentPasswordValidFor;
        let profileUpdatable = this.user.currentUser.profileUpdatable;
        if (daysToExpiration <= 2 && this.$state.current.name !== 'user_profile' && !_.findWhere(msgs,{code: 'expiration'})) {
            msgs.push({
                message: `Your password will expire in ${daysToExpiration} days. ${profileUpdatable ? 'Click <a href="#/profile">here</a> to change it now.' : 'Contact your system administrator to change it.'}`,
                type: 'warning',
                code: 'expiration'
            })
        }
        else if (this.$state.current.name === 'user_profile' && _.findWhere(msgs,{code: 'expiration'})) {
            let index = msgs.indexOf(!_.findWhere(msgs,{code: 'expiration'}));
            msgs.splice(index,1);
        }
    }

    getSystemMessage() {
        let msgObj = this.artifactoryState.getState('systemMessage');
        if (msgObj && msgObj.enabled && (msgObj.inAllPages || this.$state.current.name === 'home')) {
            this.systemMessage = msgObj;
            this.handleSizing();
        }
        else
            this.systemMessage = null;

        return this.systemMessage;
    }

    handleSizing() {
        if ($('.constant-message.system').length) {
            let maxMessageSize = this.$window.innerWidth - $('.constant-message.system .message-title').width() - ($('.constant-message.system .message-container').offset().left * 2) - 10,
                    msgText = $('.constant-message.system .message-text');

            if (msgText.find('span').width() > maxMessageSize)
                msgText.css('width', maxMessageSize).addClass('marqueed');
            else
                msgText.css('width', 'auto').removeClass('marqueed');
        }
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
