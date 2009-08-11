/**
 * Distributes feedback messages to FeedbackMessagesPanel.
 *
 * @author Yoav Aharoni
 */
var FeedbackDistributer = {
    defaultPanel: null,
    panels: new Array(),

    init: function (defaultPanel) {
        FeedbackDistributer.defaultPanel = dojo.byId(defaultPanel);
        FeedbackDistributer.clearMessages();
    },

    clearMessages: function () {
        if (FeedbackDistributer.panels.length == 0) {
            return;
        }

        dojo.forEach(FeedbackDistributer.panels, function(panel) {
            try {
                while (panel.firstChild) {
                    panel.removeChild(panel.firstChild);
                }
            } catch (e) {
            }
        });

        FeedbackDistributer.panels = new Array();
    },

    showMessages: function() {
        dojo.forEach(FeedbackDistributer.panels, function(panel) {
            if (panel.getElementsByTagName('li').length) {
                // trigger panel.onShow() event handler
                var onshow = panel.getAttribute('onshow');
                if (onshow) {
                    eval(onshow);
                }
            }
        });
    },

    addMessage: function(reporterId, level, message) {
        // get reporter and feedback panel
        var reporter = dojo.byId(reporterId);
        var panel = FeedbackDistributer.getFeedbackPanelFor(reporter);

        // setup panel if needed
        if (!panel.hasChildNodes()) {
            var ul = document.createElement('ul');
            ul.className = 'feedback';
            panel.appendChild(ul);
            FeedbackDistributer.panels.push(panel);

            // trigger panel.onClear() event handler
            var onclear = panel.getAttribute('onclear');
            if (onclear) {
                eval(onclear);
            }
        }

        // add message to panel
        var li = document.createElement('li');
        li.className = 'feedbackPanel' + level;
        li.innerHTML = '<span>' + message + '</span>';
        li.reporter = reporter;
        li.level = level;
        panel.firstChild.appendChild(li);
    },

    getFeedbackPanelFor: function(reporter) {
        var node = reporter;

        // find closest FeedbackPanel
        while (node && node != document.body) {
            var feedbackId = node.getAttribute('feedbackId');
            if (feedbackId) {
                return dojo.byId(feedbackId);
            }

            node = node.parentNode;
        }

        // add defaultPanel if needed
        if (!FeedbackDistributer.defaultPanel) {
            var dp = document.createElement('div');
            document.body.appendChild(dp);
            FeedbackDistributer.defaultPanel = dp;
        }

        // return defaultPanel
        return FeedbackDistributer.defaultPanel;
    }
};
