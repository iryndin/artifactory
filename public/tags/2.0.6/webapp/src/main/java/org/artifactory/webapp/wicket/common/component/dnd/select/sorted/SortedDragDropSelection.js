dojo.declare('artifactory.SortedDragDropSelection', artifactory.DragDropSelection, {
    constructor: function() {
        this.sort();
    },

    onDrop: function() {
        this.sort();
        this.inherited(arguments);
    },

    sort: function() {
        var me = this;
        dojo.forEach(this.panel.getElementsByTagName('ul'), function(ul) {
            me.sortList(ul);
        });
    },

    sortList: function(ul) {
        var liList = ul.getElementsByTagName('li');
        var liArray = this.collectionToArray(liList);
        liArray = liArray.sort(function(o1, o2) {
            var s1 = o1.innerHTML.toLowerCase();
            var s2 = o2.innerHTML.toLowerCase();
            return ((s1 < s2) ? -1 : ((s1 > s2) ? 1 : 0));
        });

        //    target.innerHTML = '';
        dojo.forEach(liArray, function(li) {
            ul.appendChild(li);
        });
    },

    collectionToArray: function (collection) {
        var ary = [];
        for (var i = 0, len = collection.length; i < len; i++)
        {
            ary.push(collection[i]);
        }
        return ary;
    }
});

