/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

dojo.provide('artifactory.DragDropSelection');

dojo.declare('artifactory.DragDropSelection', artifactory.Selectable, {
    init: function(panelId, sourceListId, targetListId, addLinkId, removeLinkId, textFieldId) {
        this.domNode = dojo.byId(panelId);
        this.sourceNode = dojo.byId(sourceListId);
        this.targetNode = dojo.byId(targetListId);
        this.textField = dojo.byId(textFieldId);
        this.addLink = dojo.byId(addLinkId);
        this.removeLink = dojo.byId(removeLinkId);
        this.domNode.widget = this;

        this.initSourceNode(this.sourceNode);
        this.initSourceNode(this.targetNode);
        this.updateIndices(true);
    },

    onLoad: function() {
        var me = this;

        // handle add/remove buttons
        this.addLink.onclick = function() {
            me.addItem();
            return false;
        };

        this.removeLink.onclick = function() {
            me.removeItem();
            return false;
        };
    },

    updateMoveButtons: function() {
        this.setButtonEnabled(this.addLink, 'add-link', this.sourceNode.sourceWidget.isAnySelected());
        this.setButtonEnabled(this.removeLink, 'remove-link', this.targetNode.sourceWidget.isAnySelected());
    },

    addItem: function() {
        this.moveItem(this.sourceNode, this.targetNode);
    },

    removeItem: function() {
        this.moveItem(this.targetNode, this.sourceNode);
    },

    /**
     * Move item from source to target list.
     * Preserves index.
     *
     * @param fromList source list
     * @param toList target list
     */
    moveItem: function(fromList, toList) {
        console.info(toList.sourceWidget)
        fromList.sourceWidget.forEachSelected(function(item, index) {
            var accepts = toList.sourceWidget.accept[item.getAttribute('dndtype')];
            if (accepts) {
                var beforeItem = toList.childNodes[index];
                if (beforeItem) {
                    toList.insertBefore(item, beforeItem);
                } else {
                    toList.appendChild(item);
                }
                delete fromList.sourceWidget.selection[item.id];
                toList.sourceWidget.selection[item.id] = 1;
            }
        });

        this.fixAnchor(fromList.sourceWidget);
        this.fixAnchor(toList.sourceWidget);
        this.updateMoveButtons();
        this.updateIndices();
    },

    fixAnchor: function(source) {
        var anchor = source.anchor;
        if (anchor) {
            source._removeAnchor();
            source._addItemClass(anchor, "Selected");
            source.selection[anchor.id] = 1;
        }
    },

    updateIndices: function(silent) {
        // update indices textfield
        var prevValue = this.textField.value;
        var items = this.targetNode.childNodes;
        var value = '';
        dojo.forEach(items, function(item) {
            value += ',' + item.getAttribute('idx');
        });
        this.textField.value = value.substring(1);

        // check if order changed
        if (!silent && prevValue != this.textField.value) {
            // needed for preserveState()
            this.sourceNode.sourceWidget.updateIndices();
            this.targetNode.sourceWidget.updateIndices();

            // trigger event
            eval(this.textField.getAttribute('onOrderChanged'));
        }
    },

    onSelection:  function() {
        this.updateMoveButtons();
    },

    onDrop: function(source, target) {
        if (source.parent == this.targetNode || target.parent == this.targetNode) {
            this.updateIndices();
            this.updateMoveButtons();
        }
    },

    destroy: function() {
        this.inherited(arguments);

        this.upLink.onclick = undefined;
        this.downLink.onclick = undefined;

        delete this.domNode.widget;
        delete this.domNode;
        delete this.textField;
        delete this.sourceNode;
        delete this.targetNode;
        delete this.addLink;
        delete this.removeLink;
    }
});
