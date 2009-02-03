var Collapsable = {
    collapseExpand: function(link) {
        var container = link.parentNode;
        if (container.collapsed) {
            container.collapsed = false;
            container.className = 'collapsible-wrapper expanded';
        } else {
            container.collapsed = true;
            container.className = 'collapsible-wrapper collapsed';
        }
    }
}