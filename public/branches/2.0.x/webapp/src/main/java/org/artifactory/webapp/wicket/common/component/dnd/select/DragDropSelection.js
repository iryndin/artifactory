dojo.provide("artifactory.DragDropSelection");

dojo.declare('artifactory.DragDropSelection', null, {
    constructor: function(panelId, targetListId, textFieldId) {
        this.targetList = dojo.byId(targetListId);
        this.textField = dojo.byId(textFieldId);
        this.panel = dojo.byId(panelId);
        this.init();
    },

    init: function() {
        var me = this;

        // parse widgets
        var widgets = dojo.parser.parse(this.panel);

        widgets[0].copyState = DomUtils.cancel;
        widgets[1].copyState = DomUtils.cancel;

        // override onDndDrop event and call DragDropSelection.onDrop
        widgets[1].onDndDrop = function(source, nodes, isCopy, target) {
            var e = this.inherited('onDndDrop', arguments);
            me.onDrop(source, target);
            return e;
        };

        this.updateIndices(true);
    },

    onDrop: function(source, target) {
        if (source.parent == this.targetList || target.parent == this.targetList) {
            this.updateIndices();
        }
    },

    updateIndices: function(silent) {
        // update indices textfield
        var prevValue = this.textField.value;
        var items = this.targetList.getElementsByTagName('li');
        var value = '';
        dojo.forEach(items, function(item) {
            value += ',' + item.getAttribute('idx');
        });
        this.textField.value = value.substring(1);

        // check if order changed and trigger event
        if (!silent && prevValue != this.textField.value) {
            eval(this.textField.getAttribute('onOrderChanged'));
        }
    }
});