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

        DragDropSelection.updateIndices(targetList, textField);
    },

    updateIndices: function(targetList, textField) {
        var items = targetList.getElementsByTagName('li');
        var value = '';
        dojo.forEach(items, function(item) {
            value += ',' + item.getAttribute('idx')
        });
        textField.value = value.substring(1);
    }
};