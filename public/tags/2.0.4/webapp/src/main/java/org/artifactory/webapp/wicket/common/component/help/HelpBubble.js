dojo.provide('artifactory.HelpBubble');

dojo.require('dijit.Tooltip');
dojo.require('dojo.parser');

dojo.declare('artifactory.HelpBubble', dijit.Tooltip, {
    postCreate: function() {
        //alert(dijit._masterTT)
        if (!dijit._masterTT) {
            dijit._masterTT = new dijit._MasterTooltip();
        }
        dijit._masterTT.connect(dijit._masterTT.domNode, 'onmouseover', this.ttPersist);
        dijit._masterTT.connect(dijit._masterTT.domNode, 'onmouseout', this.ttFade);

        this.inherited('postCreate', arguments);
    },

    ttPersist: function (e) {
        this.fadeOut.stop();
        this.fadeIn.play();
    },

    ttFade: function (e) {
        this.fadeOut.play();
    }
});
