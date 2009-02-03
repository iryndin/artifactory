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

/**
 * AjaxIndicator class
 * AjaxIndicator is shown when ajax is in progress.
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

    dojo.xhrGetOrig = dojo.xhrGet;
    dojo.xhrGet = function(args) {
        wicketGlobalPreCallHandler();
        if (args.handle) {
            var _originalHandle = args.handle;
            args.handle = function(type, data, evt) {
                wicketGlobalPostCallHandler();
                return _originalHandle(type, data, evt);
            }
        }
        return dojo.xhrGetOrig(args);
    }
}


/**
 * Global Ajax CallHandler
 */
var wicketGlobalPreCallHandler = function() {
    // fix for IE6 style="position: fixed;"
    if (!window.XMLHttpRequest) {
        ajaxIndicator.fixPosition();
        dojo.connect(window, 'onscroll', win_onscroll);
    }
    ajaxIndicator.show();
}

/**
 * Global Ajax CallHandler
 */
var wicketGlobalPostCallHandler = function() {
    ajaxIndicator.hide();
    dojo.disconnect(win_onscroll);
}


// init script
dojo.connect(window, 'onload', wicketGlobalPostCallHandler);
injectGlobalHandlerToDojo();