function FeedbackMessage(message, level) {
	this.message = message;
	this.level = level;
}

var AggregateFeedbackPanel = {
    onClear: function(panelId) {
        var panel = get(panelId);
        dojo._setOpacity(panel, 0);
    },

    onShow: function(panelId) {
        var panel = get(panelId);

        // count messages & get max width
        var count = {
            ERROR: 0,
            WARNING: 0,
            INFO: 0
        }

        var width = 0;
        var messages = panel.getElementsByTagName('li');
        foreach(messages, function(li) {
            count[li.level]++;
            width = Math.max(width, li.firstChild.offsetWidth);
        });

        // add title
        var title = AggregateFeedbackPanel.getTitle(count);
        if (title) {
            // give same width to all messages
            foreach(messages, function(li) {
                li.firstChild.style.width = width + 'px';
            });

            // add title
            var div = document.createElement('div');
            div.className = 'feedback-title feedback-title-' + title.level;
            div.innerHTML = '<span>' + title.message + '</span>';
            var ul = panel.firstChild;
            ul.className += ' aggregate-feedback feedback-' + title.level
            panel.insertBefore(div, ul);
        }

        // scroll to messages
        setTimeout(function() {
            panel.scrollIntoView();
        }, 100);

        // notify effect
        var notify = dojo.fadeIn({node: panel,duration: 500}).play();
        notify.play();
    },

    getTitle: function(count) {
        if (count.ERROR > 1) {
            return new FeedbackMessage(count.ERROR + " errors have been detected:", 'ERROR');
        }
        if (count.INFO > 1) {
            return new FeedbackMessage(count.INFO + " messages:", 'INFO');
        }
        if (count.WARNING > 1) {
            return new FeedbackMessage(count.INFO + " warnings:", 'WARNING');
        }
        return null;
    }
};