/**
 * AjaxIndicator class
 * AjaxIndicator is showed when ajax is in progress.
 *
 * @author yoava
 */

function AjaxIndicator(divId) {
    this._divId = divId;
    this._counter = 0;
}

AjaxIndicator.prototype.getDiv = function() {
    return document.getElementById(this._divId);
};

AjaxIndicator.prototype.fixPosition = function() {
    var myDiv = this.getDiv();
    if (myDiv)
        myDiv.style.top = document.documentElement.scrollTop + document.documentElement.clientHeight
                - myDiv.clientHeight + 'px';
}

AjaxIndicator.prototype.show = function() {
    this._counter++;
    var myDiv = this.getDiv();
    if (myDiv)
        myDiv.style.display = 'block';
}

AjaxIndicator.prototype.hide = function() {
    this._counter = this._counter ? this._counter - 1 : 0;
    if (this._counter == 0) {
        var myDiv = this.getDiv();
        if (myDiv)
            myDiv.style.display = 'none';
    }
};

/*-- Globals ---------------------*/

var ajaxIndicator = new AjaxIndicator('ajaxIndicator');

function win_onscroll() {
    ajaxIndicator.fixPosition();
}

/*
 * Injects wicket global ajax call handlers to dojo
 */
function injectGlobalHandlerToDojo() {
    dojo.io._originalBind = dojo.io.bind;
    dojo.io.bind = function(request) {
        wicketGlobalPreCallHandler();
        if (request.handle) {
            var _originalHandle = request.handle;
            request.handle = function(type, data, evt) {
                wicketGlobalPostCallHandler();
                return _originalHandle(type, data, evt);
            }
        }
        return dojo.io._originalBind(request);
    }
}


/**
 * Global Ajax CallHandler
 */
var wicketGlobalPreCallHandler = function() {
    // fix for IE6 style="position: fixed;"
    if (!window.XMLHttpRequest) {
        ajaxIndicator.fixPosition();
        dojo.event.browser.addListener(window, 'onscroll', win_onscroll);
    }
    ajaxIndicator.show();
}

/**
 * Global Ajax CallHandler
 */
var wicketGlobalPostCallHandler = function() {
    ajaxIndicator.hide();
    dojo.event.browser.removeListener(window, 'onscroll', win_onscroll);
}


// init script
dojo.event.browser.addListener(window, 'onload', wicketGlobalPostCallHandler);
injectGlobalHandlerToDojo();