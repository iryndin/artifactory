var NotifingFeedbackPanel = {};

NotifingFeedbackPanel.init = function(feedbackId) {
    var feedback = document.getElementById(feedbackId);

    // notify
    dojo._setOpacity(feedback, 0);
    var notify = dojo.fadeIn({node: feedback,duration: 500}).play();
    notify.play();

    // scroll to feedback
    var messages = feedback.getElementsByTagName('li');
    if (messages.length > 0 && feedback.scrollIntoView) {
        setTimeout(function() {
            feedback.scrollIntoView();
        }, 100)
    }
}