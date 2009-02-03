var SingleSelectionTable = {
    onmouseover: function(row) {
        row.prevClassName = row.className;
        row.className += ' hover';
    },

    onmouseout: function(row) {
        row.className = row.prevClassName;
    }
}