function LinksColumn(iconId, panelId) {
    this.icon = document.getElementById(iconId);
    this.panel = document.getElementById(panelId);
    this.panelParent = this.panel.parentNode;
    this.hasFocus = false;
    this.icon.LinksColumn = this;
    this.panel.LinksColumn = this;
    this.row = this.getRowAnchor();

    this.connentEvents();
}

LinksColumn.prototype.connentEvents = function() {
    var me = this;

    var hideIfNoFocus = function() {
        me.hasFocus = false;
        setTimeout(function() {
            if (!me.hasFocus) {
                me.hide();
            }
        }, 100);
    };

    // connent row mouse events
    dojo.connect(this.row, 'onmouseover', function() {
        me.hasFocus = true;
        setTimeout(function() {
            if (me.hasFocus) {
                me.show();
            }
        }, 100);
    });

    dojo.connect(this.row, 'onmouseout', hideIfNoFocus);

    // connenct panel mouse events
    dojo.connect(me.panel, 'onmouseover', function() {
        me.hasFocus = true;
    });

    dojo.connect(me.panel, 'onmouseout', hideIfNoFocus);
};

LinksColumn.prototype.getRowAnchor = function() {
    var rowAnchor = this.icon;
    var tagName;
    do {
        rowAnchor = rowAnchor.parentNode;
        tagName = rowAnchor.tagName.toLowerCase();
    } while (tagName != 'tr' && tagName != 'li');

    return rowAnchor;
};

LinksColumn.prototype.show = function() {
    var parent = this.panel.parentNode;
    var cord = dojo.coords(this.icon, true);
    if (parent == this.panelParent) {
        parent.removeChild(this.panel);
        document.body.appendChild(this.panel);
    }

    this.panel.style.display = 'block';
    this.panel.style.top = (cord.y - this.panel.scrollHeight / 2 + 7) + 'px';
    this.panel.style.left = (cord.x - this.panel.scrollWidth + 5) + 'px';
    DomUtils.addStyle(this.row, 'opened');
};

LinksColumn.prototype.hide = function() {
    if (this.panel.parentNode != this.panelParent) {
        this.panel.parentNode.removeChild(this.panel);
        this.panelParent.appendChild(this.panel);
    }
    this.panel.style.display = 'none';
    DomUtils.removeStyle(this.row, 'opened');
};