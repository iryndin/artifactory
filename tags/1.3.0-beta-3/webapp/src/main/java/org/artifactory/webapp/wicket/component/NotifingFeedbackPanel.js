var NotifingFeedbackPanel = {};

NotifingFeedbackPanel.init = function(feedbackId) {
    var feedback = document.getElementById(feedbackId);
    var notify = dojo.fadeOut({node: feedback,duration: 1
        ,onEnd: function() {
        dojo.fadeIn({node: feedback,duration: 300}).play();
    }});

    notify.play();
}