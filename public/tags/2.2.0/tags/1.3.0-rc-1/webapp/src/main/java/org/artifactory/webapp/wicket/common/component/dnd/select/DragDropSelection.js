var DragDropSelection = {
    init:function(panelId, targetListId, textFieldId) {
        dojo.parser.parse(dojo.byId(panelId));
        var targetList = dojo.byId(targetListId);
        var textField = dojo.byId(textFieldId);
        if (DragDropSelection.handler) {
            dojo.unsubscribe(DragDropSelection.handler);
        }

        DragDropSelection.handler = dojo.subscribe("/dnd/drop", function(source, nodes, isCopy, target) {
            if (source.parent == targetList || target.parent == targetList) {
                DragDropSelection.updateIndices(targetList, textField);
            }
        });

        DragDropSelection.updateIndices(targetList, textField, true);
    },

    updateIndices: function(targetList, textField, silent) {
        var prevValue = textField.value;
        var items = targetList.getElementsByTagName('li');
        var value = '';
        dojo.forEach(items, function(item) {
            value += ',' + item.getAttribute('idx')
        });
        textField.value = value.substring(1);
        // check if order changed and trigger event
        if (!silent && prevValue != textField.value) {
            eval(textField.getAttribute('onOrderChanged'));
        }
    }
};