/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

/*-- Utils --*/

function cancel(e) {
    window.lastTarget = e.srcElement;
    window.lastEvent = e;
    return false;
}

/*-- AjaxIndicator --*/

function AjaxIndicator(divId) {
    this._divId = divId;
    this._counter = 0;
    this.enabled = true;
    this.delayed = true;
}

AjaxIndicator.waiting = false;

AjaxIndicator.prototype.getDiv = function() {
    return document.getElementById(this._divId);
};


AjaxIndicator.prototype.disableDelay = function() {
    this.delayed = false;
};


AjaxIndicator.prototype.disableOnce = function() {
    this.enabled = false;
};

AjaxIndicator.prototype.show = function() {
    if (!this.enabled) {
        this.enabled = true;
        return;
    }

    this._counter++;
    if (this._counter != 1) {
        return;
    }

    AjaxIndicator.waiting = true;

    var myDiv = this.getDiv();
    if (myDiv) {
        window.onscroll = AjaxIndicator.positionMessage;
        AjaxIndicator.positionMessage();
        myDiv.style.display = 'block';
        if (this.delayed) {
            setTimeout(this.showIndicator, 500);
        } else {
            this.showIndicator();
        }
    }

    this.delayed = true;
};

AjaxIndicator.prototype.showIndicator = function() {
    if (ajaxIndicator._counter) {
        var doc = document.documentElement;
        var msgDiv = document.getElementById('ajaxIndicatorMessage');
        msgDiv.style.top = (doc.clientHeight - msgDiv.clientHeight) / 2 + 'px';
        msgDiv.style.left = (doc.clientWidth - msgDiv.clientWidth) / 2 + 'px';

        document.getElementById('ajaxIndicator').style.visibility = 'visible';
    }
};

AjaxIndicator.positionMessage = function() {
    var doc = document.documentElement;
    var myDiv = ajaxIndicator.getDiv();
    myDiv.style.top = doc.scrollTop + 'px';
    myDiv.style.left = doc.scrollLeft + 'px';
};

AjaxIndicator.prototype.hide = function() {
    this._counter = this._counter ? this._counter - 1 : 0;
    if (this._counter > 0) {
        return;
    }

    AjaxIndicator.waiting = false;

    var myDiv = this.getDiv();
    if (myDiv) {
        window.onscroll = null;
        myDiv.style.display = 'none';
        document.getElementById('ajaxIndicator').style.visibility = 'hidden';
    }
};

/*-- AjaxIndicator Globals --*/

var ajaxIndicator = new AjaxIndicator('ajaxIndicatorContainer');

/**
 * Global Ajax CallHandler
 */
window.wicketGlobalPreCallHandler = function() {
    ajaxIndicator.show();
};

/**
 * Global Ajax CallHandler
 */
window.wicketGlobalPostCallHandler = function() {
    ajaxIndicator.hide();
    DojoUtils.dijitCleanup();
};

/*-- DOM Utils --*/

var DomUtils = {
    addHoverStyle: function(obj) {
        DomUtils.addStyle(obj, 'hover');
    },

    removeHoverStyle: function(obj) {
        DomUtils.removeStyle(obj, 'hover');
    },

    addStyle: function(obj, append) {
        if (!obj) {
            return;
        }

        // 'register' style
        if (!obj.addedStyles) {
            obj.addedStyles = {};
        }

        if (obj.addedStyles[append]) {
            return;
        }
        obj.addedStyles[append] = true;


        // add style to className
        var trimmed = obj.className.replace(/^\s+|\s+$/g, '');
        obj.className = trimmed + ' ' + append + ' ' + trimmed.replace(/\s/g, '-' + append + ' ') + '-' + append;
    },

    removeStyle: function(obj, append) {
        if (!obj) {
            return;
        }

        // check if style registered
        if (!obj.addedStyles || !obj.addedStyles[append]) {
            return;
        }
        obj.addedStyles[append] = undefined;

        // remove style from className & cleanup className
        var classes = obj.className.split(' ');
        var regExp = new RegExp(append + '$');
        var map = {};
        dojo.forEach(classes, function(className) {
            if (className && !className.match(regExp)) {
                map[className] = true;
            }
        });

        var cssClass = '';
        for (var className in map) {
            if (className) {
                cssClass += ' ' + className;
            }
        }
        obj.className = cssClass.substring(1);
    },

    findParent: function(node, tagName) {
        tagName = tagName.toLowerCase();

        var current = node;
        while (current && current.tagName && current.tagName.toLowerCase() != tagName) {
            current = current.parentNode;
        }
        return current;
    },

    nextSibling: function(node) {
        do {
            node = node.nextSibling;
        } while (node && node.nodeType != 1);
        return node;
    },

    addOnRender: function(func) {
        if (dojo.isIE && !dojo._postLoad) {
            dojo.addOnLoad(func);
        } else {
            func();
        }
    },

    cancel: function() {
        return false;
    },

    scrollIntoView: function(node) {
        var body = document.body;
        var current = node;
        var parent = current.parentNode;
        while (parent && parent != body) {
            var overflow = dojo.style(parent).overflow;
            if (overflow == 'auto' || overflow == 'scroll') {
                DomUtils.scrollIntoParentView(current, parent);
                current = parent;
            }
            parent = parent.parentNode;
        }

        DomUtils.scrollIntoBody(node);
    },

    scrollIntoParentView: function(node, parent, parentCoords) {
        var coords = dojo.coords(node, true);
        if (!parentCoords) {
            parentCoords = dojo.coords(parent, true);
        }

        var top = coords.y + coords.h;
        var parentTop = parentCoords.y + parentCoords.h;
        var center = parentCoords.h / 2 - coords.h / 2;

        if (top > parentTop) {
            parent.scrollTop += top - parentTop + center;
        } else if (coords.y < parentCoords.y) {
            parent.scrollTop -= parentCoords.y - coords.y + center;
        }
    },

    scrollIntoBody: function(node) {
        var parent = dojo.isKhtml ? document.body : document.documentElement;
        var parentCoords = {
            y: parent.scrollTop,
            h: parent.clientHeight
        };
        DomUtils.scrollIntoParentView(node, parent, parentCoords);
    }
};

var DojoUtils = {
    widgets: new Array(),

    // widget garbage collection
    dijitCleanup: function() {
        var widgets = new Array();
        var widget;
        while (widget = DojoUtils.widgets.pop()) {
            var node = widget.domNode || widget.node;
            var body = DomUtils.findParent(node, 'body');
            if (!body) {
                try {
                    widget.destroy();
                } catch(e) {
                }
            } else {
                widgets.push(widget);
            }
        }
        DojoUtils.widgets = widgets;
    }
};

/*-- override 'dojo.parser.instantiate()' --*/

(function() {
    dojo.require('dojo.parser');

    var superInstantiate = dojo.parser.instantiate;
    dojo.parser.instantiate = function() {
        var widgets = superInstantiate.apply(this, arguments);
        dojo.forEach(widgets, function(widget) {
            if (!widget.selfCleanup) {
                DojoUtils.widgets.push(widget);
            }
        });
        return widgets;
    };
})();

/*-- detect browser for css --*/

(function() {
    var css = '';

    var browsers = ['FF','Gears','IE','Khtml','Moz','Opera'];
    dojo.forEach(browsers, function(name) {
        var version = dojo['is' + name];
        if (version) {
            css += ' ' + name + ' ' + name + '-' + version;
        }
    });
    document.documentElement.className = 'JS' + css;
})();