dojo.provide("artifactory.DragDropSelection");

dojo.require("dojo.dnd.Source");

dojo.declare('OrderedListPanel', null, {
    constructor: function(listId, textFieldId, upLinkId, downLinkId) {
        this.list = dojo.byId(listId);
        this.textField = dojo.byId(textFieldId);
        this.upLink = dojo.byId(upLinkId);
        this.downLink = dojo.byId(downLinkId);
        this.selectedItem = null;

        var me = this;
        DomUtils.addOnRender(function() {
            me.init();
        });
    },

    init: function() {
        this.list.widget = this;

        this.resetIndices();

        // parse widgets
        var me = this;
        var widgets = dojo.parser.instantiate([this.list]);

        // override onDndDrop event and call OrderedListPanel.onDrop
        widgets[0].onDndDrop = function(source, nodes, isCopy, target) {
            this.inherited('onDndDrop', arguments);
            me.onDrop(source, target);
        };

        widgets[0].copyState = DomUtils.cancel;

        // handle move up/down buttons
        this.upLink.onclick = function() {
            me.moveUp();
            return false;
        };

        this.downLink.onclick = function() {
            me.moveDown();
            return false;
        };
    },

    onDrop: function(source, target) {
        if (source.parent == this.list || target.parent == this.list) {
            this.updateMoveButtons();
            this.updateIndices();
        }
    },

    updateMoveButtons: function() {
        var item = this.selectedItem;
        this.setEnable(this.downLink, 'down-link', item && item.nextSibling);
        this.setEnable(this.upLink, 'up-link', item && item.previousSibling);
    },

    resetIndices: function() {
        var next;
        var i = 0;
        var item = this.list.firstChild;
        while (item) {
            next = item.nextSibling;
            if (item.nodeType == 1) {
                item.idx = i;
                i++;
            } else {
                item.parentNode.removeChild(item);
            }
            item = next;
        }

        this.updateIndices(true);
    },

    updateIndices: function(silent) {
        var textfield = this.textField;
        var prevValue = textfield.value;

        // update indices list
        var value = '';
        var item = this.list.firstChild;
        var i = 0;
        while (item) {
            if (i % 2) {
                item.className = item.className.replace(/even/, 'odd');
            } else {
                item.className = item.className.replace(/odd/, 'even');
            }
            value += ',' + item.idx;

            i++;
            item = item.nextSibling;
        }
        textfield.value = value.substring(1);

        // check if order changed and trigger event
        if (!silent && prevValue != textfield.value) {
            eval(textfield.getAttribute('onOrderChanged'));
        }
    },

    setEnable: function(link, cssClass, enabled) {
        if (enabled && link.disabled) {
            link.disabled = false;
            link.className = 'button ' + cssClass;
        } else if (!enabled && !link.disabled) {
            link.disabled = true;
            link.className += 'button ' + cssClass + '-disabled';
        }
    },

    moveUp: function() {
        var item = this.selectedItem;
        item.parentNode.insertBefore(item, item.previousSibling);
        this.updateMoveButtons();
        this.updateIndices();
    },

    moveDown: function() {
        var item = this.selectedItem;
        item.parentNode.insertBefore(item.nextSibling, item);
        this.updateMoveButtons();
        this.updateIndices();
    }
});

OrderedListPanel.select = function(item) {
    var panel = item.parentNode.widget;

    // disselect prev selectedItem
    var selectedItem = panel.selectedItem;
    if (selectedItem) {
        selectedItem.className = selectedItem.className.replace(/ selected/, '');
    }

    // select item
    panel.selectedItem = item;
    item.className += ' selected';

    panel.updateMoveButtons();
};

