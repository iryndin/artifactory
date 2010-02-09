dojo.require("dojo.dnd.Source");

function OrderedListPanel(listId, textFieldId, upLinkId, downLinkId) {
    this.list = dojo.byId(listId);
    this.textField = dojo.byId(textFieldId);
    this.upLink = dojo.byId(upLinkId);
    this.downLink = dojo.byId(downLinkId);
    this.selectedItem = null;

    var panel = this;
    DomUtils.addOnRender(function() {
        panel.init();
    });
}

OrderedListPanel.prototype.init = function() {
    var panel = this;
    this.list._panel = panel;

    this.resetIndices();

    // handle drop events
    var dndSource = dojo.parser.parse(this.list.parentNode);
    OrderedListPanel.handler = dojo.subscribe("/dnd/drop", function(source, nodes, isCopy, target) {
        if (source.parent == panel.list || target.parent == panel.list) {
            panel.updateMoveButtons();
            panel.updateIndices();
        }
    });

    // handle move up/down buttons
    this.upLink.onclick = function() {
        panel.moveUp();
        return false;
    };

    this.downLink.onclick = function() {
        panel.moveDown();
        return false;
    };
};

OrderedListPanel.prototype.updateMoveButtons = function() {
    var item = this.selectedItem;
    this.setEnable(this.downLink, 'down-link', item && item.nextSibling);
    this.setEnable(this.upLink, 'up-link', item && item.previousSibling);
};

OrderedListPanel.prototype.resetIndices = function() {
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
};

OrderedListPanel.prototype.updateIndices = function(silent) {
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
};

OrderedListPanel.prototype.setEnable = function(link, cssClass, enabled) {
    if (enabled && link.disabled) {
        link.disabled = false;
        link.className = 'button ' + cssClass;
    } else if (!enabled && !link.disabled) {
        link.disabled = true;
        link.className += 'button ' + cssClass + '-disabled';
    }
};

OrderedListPanel.prototype.moveUp = function() {
    var item = this.selectedItem;
    item.parentNode.insertBefore(item, item.previousSibling);
    this.updateMoveButtons();
    this.updateIndices();
};

OrderedListPanel.prototype.moveDown = function() {
    var item = this.selectedItem;
    item.parentNode.insertBefore(item.nextSibling, item);
    this.updateMoveButtons();
    this.updateIndices();
};

OrderedListPanel.select = function(item) {
    var panel = item.parentNode._panel;

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

