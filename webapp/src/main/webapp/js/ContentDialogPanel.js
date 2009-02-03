function ContentDialog() {
}

ContentDialog.init = function () {
    Draggable(dojo.byId('contentWin'), dojo.byId('contentGrabber'));
}

ContentDialog.show = function() {
    var win = dojo.byId('contentWin');
    var textarea = win.getElementsByTagName('textarea')[0];
    var width = dojo.html.getViewport().width * 0.6;
    var height = dojo.html.getViewport().height * 0.7;
    win.style.width = width + 80 + 'px';
    document.getElementById('contentBody').style.height = height - 45 + 'px';
    textarea.style.width = width + 'px';
    textarea.style.height = height + 'px';

    // fix for IE6
    if (!window.XMLHttpRequest) {
        textarea.style.width = width - 10 + 'px';
        textarea.style.height = height - 10 + 'px';
        setTimeout("ContentDialog.fixIE6(true);", 500);
    }
    dojo.widget.byId('contentDialog').show();
}

// #@?%^#@$!! stupid IE bugs...
ContentDialog.fixIE6 = function(enableFix) {
    var win = dojo.byId('contentWin');
    var children = win.all;
    var l = children.length;
    for (var i = 0; i < l; i++) {
        children[i].style.width = children[i].clientWidth && enableFix ? children[i].clientWidth + 'px' : '';
        children[i].style.height = children[i].clientHeight && enableFix ? children[i].clientHeight + 'px' : '';
    }
    win.style.width = win.clientWidth + 'px';
}

ContentDialog.hide = function() {
    dojo.widget.byId('contentDialog').hide();

    // reset position
    window.setTimeout(function () {
        with (dojo.byId('contentWin').style) {
            position = '';
            top = '';
            bottom = '';
        }
    }, 500);

    // fix for IE6
    if (!window.XMLHttpRequest) {
        ContentDialog.fixIE6(false);
    }
}

/**
 * Draggable Class.
 * Used to create draggable elements.
 *
 * @author yoava
 */
function Draggable(winObj, grabFromObj) {
    // set move cursor
    grabFromObj.style.cursor = 'move';

    //attach event
    dojo.event.browser.addListener(grabFromObj, 'onmousedown', function (e) {
        Draggable._startDrag(winObj, e);
    });
}

/*-- event handlers --*/

Draggable._doDrag = function(winObj, dx, dy, e) {
    if (window.event) e = window.event;

    winObj.style.left = dx + e.clientX + 'px';
    winObj.style.top = dy + e.clientY + 'px';

    if (document.selection) {
        document.selection.empty();
    } else if (window.getSelection) {
        window.getSelection().removeAllRanges();
    }
}

Draggable._stopDrag = function(winObj, e) {
    document.onmousemove = null;
    document.onmouseup = null;
}

Draggable._startDrag = function(winObj, e) {
    if (window.event) e = window.event;

    var dx = winObj.offsetLeft - e.clientX;
    var dy = winObj.offsetTop - e.clientY;


    winObj.style.left = winObj.offsetLeft + 'px';
    winObj.style.position = 'absolute';

    //attach events
    document.onmousemove = function (e) {
        Draggable._doDrag(winObj, dx, dy, e);
    };
    document.onmouseup = function (e) {
        Draggable._stopDrag();
    };
}

dojo.require("dojo.widget.Dialog");

function max(a, b) {
    return (a > b ? a : b);
}


