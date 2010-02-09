/*
 Copyright (c) 2004-2008, The Dojo Foundation
 All Rights Reserved.

 Licensed under the Academic Free License version 2.1 or above OR the
 modified BSD license. For more information on Dojo licensing, see:

 http://dojotoolkit.org/book/dojo-book-0-9/introduction/licensing
 */

/*
 This is a compiled version of Dojo, built for deployment and not for
 development. To get an editable version, please visit:

 http://dojotoolkit.org

 for documentation and information on getting the source.
 */

if (!dojo._hasResource["dijit._base.focus"]) {
    dojo._hasResource["dijit._base.focus"] = true;
    dojo.provide("dijit._base.focus");
    dojo.mixin(dijit, {_curFocus:null,_prevFocus:null,isCollapsed:function() {
        var _1 = dojo.global;
        var _2 = dojo.doc;
        if (_2.selection) {
            return !_2.selection.createRange().text;
        } else {
            var _3 = _1.getSelection();
            if (dojo.isString(_3)) {
                return !_3;
            } else {
                return _3.isCollapsed || !_3.toString();
            }
        }
    },getBookmark:function() {
        var _4,_5 = dojo.doc.selection;
        if (_5) {
            var _6 = _5.createRange();
            if (_5.type.toUpperCase() == "CONTROL") {
                if (_6.length) {
                    _4 = [];
                    var i = 0,_8 = _6.length;
                    while (i < _8) {
                        _4.push(_6.item(i++));
                    }
                } else {
                    _4 = null;
                }
            } else {
                _4 = _6.getBookmark();
            }
        } else {
            if (window.getSelection) {
                _5 = dojo.global.getSelection();
                if (_5) {
                    _6 = _5.getRangeAt(0);
                    _4 = _6.cloneRange();
                }
            } else {
                console.warn("No idea how to store the current selection for this browser!");
            }
        }
        return _4;
    },moveToBookmark:function(_9) {
        var _a = dojo.doc;
        if (_a.selection) {
            var _b;
            if (dojo.isArray(_9)) {
                _b = _a.body.createControlRange();
                dojo.forEach(_9, "range.addElement(item)");
            } else {
                _b = _a.selection.createRange();
                _b.moveToBookmark(_9);
            }
            _b.select();
        } else {
            var _c = dojo.global.getSelection && dojo.global.getSelection();
            if (_c && _c.removeAllRanges) {
                _c.removeAllRanges();
                _c.addRange(_9);
            } else {
                console.warn("No idea how to restore selection for this browser!");
            }
        }
    },getFocus:function(_d, _e) {
        return {node:_d && dojo.isDescendant(dijit._curFocus, _d.domNode) ? dijit._prevFocus :
                     dijit._curFocus,bookmark:
                !dojo.withGlobal(_e || dojo.global, dijit.isCollapsed) ?
                dojo.withGlobal(_e || dojo.global, dijit.getBookmark) : null,openedForWindow:_e};
    },focus:function(_f) {
        if (!_f) {
            return;
        }
        var _10 = "node" in _f ? _f.node : _f,_11 = _f.bookmark,_12 = _f.openedForWindow;
        if (_10) {
            var _13 = (_10.tagName.toLowerCase() == "iframe") ? _10.contentWindow : _10;
            if (_13 && _13.focus) {
                try {
                    _13.focus();
                } catch(e) {
                }
            }
            dijit._onFocusNode(_10);
        }
        if (_11 && dojo.withGlobal(_12 || dojo.global, dijit.isCollapsed)) {
            if (_12) {
                _12.focus();
            }
            try {
                dojo.withGlobal(_12 || dojo.global, dijit.moveToBookmark, null, [_11]);
            } catch(e) {
            }
        }
    },_activeStack:[],registerWin:function(_14) {
        if (!_14) {
            _14 = window;
        }
        dojo.connect(_14.document, "onmousedown", function(evt) {
            dijit._justMouseDowned = true;
            setTimeout(function() {
                dijit._justMouseDowned = false;
            }, 0);
            dijit._onTouchNode(evt.target || evt.srcElement);
        });
        var _16 = _14.document.body || _14.document.getElementsByTagName("body")[0];
        if (_16) {
            if (dojo.isIE) {
                _16.attachEvent("onactivate", function(evt) {
                    if (evt.srcElement.tagName.toLowerCase() != "body") {
                        dijit._onFocusNode(evt.srcElement);
                    }
                });
                _16.attachEvent("ondeactivate", function(evt) {
                    dijit._onBlurNode(evt.srcElement);
                });
            } else {
                _16.addEventListener("focus", function(evt) {
                    dijit._onFocusNode(evt.target);
                }, true);
                _16.addEventListener("blur", function(evt) {
                    dijit._onBlurNode(evt.target);
                }, true);
            }
        }
        _16 = null;
    },_onBlurNode:function(_1b) {
        dijit._prevFocus = dijit._curFocus;
        dijit._curFocus = null;
        if (dijit._justMouseDowned) {
            return;
        }
        if (dijit._clearActiveWidgetsTimer) {
            clearTimeout(dijit._clearActiveWidgetsTimer);
        }
        dijit._clearActiveWidgetsTimer = setTimeout(function() {
            delete dijit._clearActiveWidgetsTimer;
            dijit._setStack([]);
            dijit._prevFocus = null;
        }, 100);
    },_onTouchNode:function(_1c) {
        if (dijit._clearActiveWidgetsTimer) {
            clearTimeout(dijit._clearActiveWidgetsTimer);
            delete dijit._clearActiveWidgetsTimer;
        }
        var _1d = [];
        try {
            while (_1c) {
                if (_1c.dijitPopupParent) {
                    _1c = dijit.byId(_1c.dijitPopupParent).domNode;
                } else {
                    if (_1c.tagName && _1c.tagName.toLowerCase() == "body") {
                        if (_1c === dojo.body()) {
                            break;
                        }
                        _1c = dijit.getDocumentWindow(_1c.ownerDocument).frameElement;
                    } else {
                        var id = _1c.getAttribute && _1c.getAttribute("widgetId");
                        if (id) {
                            _1d.unshift(id);
                        }
                        _1c = _1c.parentNode;
                    }
                }
            }
        } catch(e) {
        }
        dijit._setStack(_1d);
    },_onFocusNode:function(_1f) {
        if (_1f && _1f.tagName && _1f.tagName.toLowerCase() == "body") {
            return;
        }
        dijit._onTouchNode(_1f);
        if (_1f == dijit._curFocus) {
            return;
        }
        if (dijit._curFocus) {
            dijit._prevFocus = dijit._curFocus;
        }
        dijit._curFocus = _1f;
        dojo.publish("focusNode", [_1f]);
    },_setStack:function(_20) {
        var _21 = dijit._activeStack;
        dijit._activeStack = _20;
        for (var _22 = 0; _22 < Math.min(_21.length, _20.length); _22++) {
            if (_21[_22] != _20[_22]) {
                break;
            }
        }
        for (var i = _21.length - 1; i >= _22; i--) {
            var _24 = dijit.byId(_21[i]);
            if (_24) {
                _24._focused = false;
                _24._hasBeenBlurred = true;
                if (_24._onBlur) {
                    _24._onBlur();
                }
                if (_24._setStateClass) {
                    _24._setStateClass();
                }
                dojo.publish("widgetBlur", [_24]);
            }
        }
        for (i = _22; i < _20.length; i++) {
            _24 = dijit.byId(_20[i]);
            if (_24) {
                _24._focused = true;
                if (_24._onFocus) {
                    _24._onFocus();
                }
                if (_24._setStateClass) {
                    _24._setStateClass();
                }
                dojo.publish("widgetFocus", [_24]);
            }
        }
    }});
    dojo.addOnLoad(dijit.registerWin);
}
if (!dojo._hasResource["dijit._base.manager"]) {
    dojo._hasResource["dijit._base.manager"] = true;
    dojo.provide("dijit._base.manager");
    dojo.declare("dijit.WidgetSet", null, {constructor:function() {
        this._hash = {};
    },add:function(_25) {
        if (this._hash[_25.id]) {
            throw new Error("Tried to register widget with id==" + _25.id +
                            " but that id is already registered");
        }
        this._hash[_25.id] = _25;
    },remove:function(id) {
        delete this._hash[id];
    },forEach:function(_27) {
        for (var id in this._hash) {
            _27(this._hash[id]);
        }
    },filter:function(_29) {
        var res = new dijit.WidgetSet();
        this.forEach(function(_2b) {
            if (_29(_2b)) {
                res.add(_2b);
            }
        });
        return res;
    },byId:function(id) {
        return this._hash[id];
    },byClass:function(cls) {
        return this.filter(function(_2e) {
            return _2e.declaredClass == cls;
        });
    }});
    dijit.registry = new dijit.WidgetSet();
    dijit._widgetTypeCtr = {};
    dijit.getUniqueId = function(_2f) {
        var id;
        do{
            id = _2f + "_" + (_2f in dijit._widgetTypeCtr ? ++dijit._widgetTypeCtr[_2f] :
                              dijit._widgetTypeCtr[_2f] = 0);
        } while (dijit.byId(id));
        return id;
    };
    if (dojo.isIE) {
        dojo.addOnUnload(function() {
            dijit.registry.forEach(function(_31) {
                _31.destroy();
            });
        });
    }
    dijit.byId = function(id) {
        return (dojo.isString(id)) ? dijit.registry.byId(id) : id;
    };
    dijit.byNode = function(_33) {
        return dijit.registry.byId(_33.getAttribute("widgetId"));
    };
    dijit.getEnclosingWidget = function(_34) {
        while (_34) {
            if (_34.getAttribute && _34.getAttribute("widgetId")) {
                return dijit.registry.byId(_34.getAttribute("widgetId"));
            }
            _34 = _34.parentNode;
        }
        return null;
    };
    dijit._tabElements = {area:true,button:true,input:true,object:true,select:true,textarea:true};
    dijit._isElementShown = function(_35) {
        var _36 = dojo.style(_35);
        return (_36.visibility != "hidden") && (_36.visibility != "collapsed") &&
               (_36.display != "none");
    };
    dijit.isTabNavigable = function(_37) {
        if (dojo.hasAttr(_37, "disabled")) {
            return false;
        }
        var _38 = dojo.hasAttr(_37, "tabindex");
        var _39 = dojo.attr(_37, "tabindex");
        if (_38 && _39 >= 0) {
            return true;
        }
        var _3a = _37.nodeName.toLowerCase();
        if (((_3a == "a" && dojo.hasAttr(_37, "href")) || dijit._tabElements[_3a]) &&
            (!_38 || _39 >= 0)) {
            return true;
        }
        return false;
    };
    dijit._getTabNavigable = function(_3b) {
        var _3c,_3d,_3e,_3f,_40,_41;
        var _42 = function(_43) {
            dojo.query("> *", _43).forEach(function(_44) {
                var _45 = dijit._isElementShown(_44);
                if (_45 && dijit.isTabNavigable(_44)) {
                    var _46 = dojo.attr(_44, "tabindex");
                    if (!dojo.hasAttr(_44, "tabindex") || _46 == 0) {
                        if (!_3c) {
                            _3c = _44;
                        }
                        _3d = _44;
                    } else {
                        if (_46 > 0) {
                            if (!_3e || _46 < _3f) {
                                _3f = _46;
                                _3e = _44;
                            }
                            if (!_40 || _46 >= _41) {
                                _41 = _46;
                                _40 = _44;
                            }
                        }
                    }
                }
                if (_45) {
                    _42(_44);
                }
            });
        };
        if (dijit._isElementShown(_3b)) {
            _42(_3b);
        }
        return {first:_3c,last:_3d,lowest:_3e,highest:_40};
    };
    dijit.getFirstInTabbingOrder = function(_47) {
        var _48 = dijit._getTabNavigable(dojo.byId(_47));
        return _48.lowest ? _48.lowest : _48.first;
    };
    dijit.getLastInTabbingOrder = function(_49) {
        var _4a = dijit._getTabNavigable(dojo.byId(_49));
        return _4a.last ? _4a.last : _4a.highest;
    };
}
if (!dojo._hasResource["dijit._base.place"]) {
    dojo._hasResource["dijit._base.place"] = true;
    dojo.provide("dijit._base.place");
    dijit.getViewport = function() {
        var _4b = dojo.global;
        var _4c = dojo.doc;
        var w = 0,h = 0;
        var de = _4c.documentElement;
        var dew = de.clientWidth,deh = de.clientHeight;
        if (dojo.isMozilla) {
            var _52,_53,_54,_55;
            var dbw = _4c.body.clientWidth;
            if (dbw > dew) {
                _52 = dew;
                _54 = dbw;
            } else {
                _54 = dew;
                _52 = dbw;
            }
            var dbh = _4c.body.clientHeight;
            if (dbh > deh) {
                _53 = deh;
                _55 = dbh;
            } else {
                _55 = deh;
                _53 = dbh;
            }
            w = (_54 > _4b.innerWidth) ? _52 : _54;
            h = (_55 > _4b.innerHeight) ? _53 : _55;
        } else {
            if (!dojo.isOpera && _4b.innerWidth) {
                w = _4b.innerWidth;
                h = _4b.innerHeight;
            } else {
                if (dojo.isIE && de && deh) {
                    w = dew;
                    h = deh;
                } else {
                    if (dojo.body().clientWidth) {
                        w = dojo.body().clientWidth;
                        h = dojo.body().clientHeight;
                    }
                }
            }
        }
        var _58 = dojo._docScroll();
        return {w:w,h:h,l:_58.x,t:_58.y};
    };
    dijit.placeOnScreen = function(_59, pos, _5b, _5c) {
        var _5d = dojo.map(_5b, function(_5e) {
            return {corner:_5e,pos:pos};
        });
        return dijit._place(_59, _5d);
    };
    dijit._place = function(_5f, _60, _61) {
        var _62 = dijit.getViewport();
        if (!_5f.parentNode || String(_5f.parentNode.tagName).toLowerCase() != "body") {
            dojo.body().appendChild(_5f);
        }
        var _63 = null;
        dojo.some(_60, function(_64) {
            var _65 = _64.corner;
            var pos = _64.pos;
            if (_61) {
                _61(_5f, _64.aroundCorner, _65);
            }
            var _67 = _5f.style;
            var _68 = _67.display;
            var _69 = _67.visibility;
            _67.visibility = "hidden";
            _67.display = "";
            var mb = dojo.marginBox(_5f);
            _67.display = _68;
            _67.visibility = _69;
            var _6b = (_65.charAt(1) == "L" ? pos.x : Math.max(_62.l, pos.x - mb.w)),_6c = (
                    _65.charAt(0) == "T" ? pos.y : Math.max(_62.t, pos.y - mb.h)),_6d = (
                    _65.charAt(1) == "L" ? Math.min(_62.l + _62.w, _6b + mb.w) : pos.x),_6e = (
                    _65.charAt(0) == "T" ? Math.min(_62.t + _62.h, _6c + mb.h) : pos.y),_6f = _6d -
                                                                                              _6b,_70 = _6e -
                                                                                                        _6c,_71 = (mb.w -
                                                                                                                   _6f) +
                                                                                                                  (mb.h -
                                                                                                                   _70);
            if (_63 == null || _71 < _63.overflow) {
                _63 =
                {corner:_65,aroundCorner:_64.aroundCorner,x:_6b,y:_6c,w:_6f,h:_70,overflow:_71};
            }
            return !_71;
        });
        _5f.style.left = _63.x + "px";
        _5f.style.top = _63.y + "px";
        if (_63.overflow && _61) {
            _61(_5f, _63.aroundCorner, _63.corner);
        }
        return _63;
    };
    dijit.placeOnScreenAroundElement = function(_72, _73, _74, _75) {
        _73 = dojo.byId(_73);
        var _76 = _73.style.display;
        _73.style.display = "";
        var _77 = _73.offsetWidth;
        var _78 = _73.offsetHeight;
        var _79 = dojo.coords(_73, true);
        _73.style.display = _76;
        var _7a = [];
        for (var _7b in _74) {
            _7a.push({aroundCorner:_7b,corner:_74[_7b],pos:{x:_79.x + (_7b.charAt(1) == "L" ? 0 :
                                                                       _77),y:_79.y +
                                                                              (_7b.charAt(0) ==
                                                                               "T" ? 0 : _78)}});
        }
        return dijit._place(_72, _7a, _75);
    };
}
if (!dojo._hasResource["dijit._base.window"]) {
    dojo._hasResource["dijit._base.window"] = true;
    dojo.provide("dijit._base.window");
    dijit.getDocumentWindow = function(doc) {
        if (dojo.isSafari && !doc._parentWindow) {
            var fix = function(win) {
                win.document._parentWindow = win;
                for (var i = 0; i < win.frames.length; i++) {
                    fix(win.frames[i]);
                }
            };
            fix(window.top);
        }
        if (dojo.isIE && window !== document.parentWindow && !doc._parentWindow) {
            doc.parentWindow.execScript("document._parentWindow = window;", "Javascript");
            var win = doc._parentWindow;
            doc._parentWindow = null;
            return win;
        }
        return doc._parentWindow || doc.parentWindow || doc.defaultView;
    };
}
if (!dojo._hasResource["dijit._base.popup"]) {
    dojo._hasResource["dijit._base.popup"] = true;
    dojo.provide("dijit._base.popup");
    dijit.popup = new function() {
        var _81 = [],_82 = 1000,_83 = 1;
        this.prepare = function(_84) {
            dojo.body().appendChild(_84);
            var s = _84.style;
            if (s.display == "none") {
                s.display = "";
            }
            s.visibility = "hidden";
            s.position = "absolute";
            s.top = "-9999px";
        };
        this.open = function(_86) {
            var _87 = _86.popup,_88 = _86.orient || {"BL":"TL","TL":"BL"},_89 = _86.around,id =
                    (_86.around && _86.around.id) ? (_86.around.id + "_dropdown") :
                    ("popup_" + _83++);
            var _8b = dojo.doc.createElement("div");
            dijit.setWaiRole(_8b, "presentation");
            _8b.id = id;
            _8b.className = "dijitPopup";
            _8b.style.zIndex = _82 + _81.length;
            _8b.style.visibility = "hidden";
            if (_86.parent) {
                _8b.dijitPopupParent = _86.parent.id;
            }
            dojo.body().appendChild(_8b);
            var s = _87.domNode.style;
            s.display = "";
            s.visibility = "";
            s.position = "";
            _8b.appendChild(_87.domNode);
            var _8d = new dijit.BackgroundIframe(_8b);
            var _8e = _89 ? dijit.placeOnScreenAroundElement(_8b, _89, _88, _87.orient ?
                                                                            dojo.hitch(_87, "orient") :
                                                                            null) :
                      dijit.placeOnScreen(_8b, _86, _88 == "R" ? ["TR","BR","TL","BL"] :
                                                    ["TL","BL","TR","BR"]);
            _8b.style.visibility = "visible";
            var _8f = [];
            var _90 = function() {
                for (var pi = _81.length - 1; pi > 0 && _81[pi].parent === _81[pi - 1].widget;
                     pi--) {
                }
                return _81[pi];
            };
            _8f.push(dojo.connect(_8b, "onkeypress", this, function(evt) {
                if (evt.keyCode == dojo.keys.ESCAPE && _86.onCancel) {
                    dojo.stopEvent(evt);
                    _86.onCancel();
                } else {
                    if (evt.keyCode == dojo.keys.TAB) {
                        dojo.stopEvent(evt);
                        var _93 = _90();
                        if (_93 && _93.onCancel) {
                            _93.onCancel();
                        }
                    }
                }
            }));
            if (_87.onCancel) {
                _8f.push(dojo.connect(_87, "onCancel", null, _86.onCancel));
            }
            _8f.push(dojo.connect(_87, _87.onExecute ? "onExecute" : "onChange", null, function() {
                var _94 = _90();
                if (_94 && _94.onExecute) {
                    _94.onExecute();
                }
            }));
            _81.push({wrapper:_8b,iframe:_8d,widget:_87,parent:_86.parent,onExecute:_86.onExecute,onCancel:_86.onCancel,onClose:_86.onClose,handlers:_8f});
            if (_87.onOpen) {
                _87.onOpen(_8e);
            }
            return _8e;
        };
        this.close = function(_95) {
            while (dojo.some(_81, function(_96) {
                return _96.widget == _95;
            })) {
                var top = _81.pop(),_98 = top.wrapper,_99 = top.iframe,_9a = top.widget,_9b = top.onClose;
                if (_9a.onClose) {
                    _9a.onClose();
                }
                dojo.forEach(top.handlers, dojo.disconnect);
                if (!_9a || !_9a.domNode) {
                    return;
                }
                this.prepare(_9a.domNode);
                _99.destroy();
                dojo._destroyElement(_98);
                if (_9b) {
                    _9b();
                }
            }
        };
    }();
    dijit._frames = new function() {
        var _9c = [];
        this.pop = function() {
            var _9d;
            if (_9c.length) {
                _9d = _9c.pop();
                _9d.style.display = "";
            } else {
                if (dojo.isIE) {
                    var _9e = "<iframe src='javascript:\"\"'" +
                              " style='position: absolute; left: 0px; top: 0px;" +
                              "z-index: -1; filter:Alpha(Opacity=\"0\");'>";
                    _9d = dojo.doc.createElement(_9e);
                } else {
                    _9d = dojo.doc.createElement("iframe");
                    _9d.src = "javascript:\"\"";
                    _9d.className = "dijitBackgroundIframe";
                }
                _9d.tabIndex = -1;
                dojo.body().appendChild(_9d);
            }
            return _9d;
        };
        this.push = function(_9f) {
            _9f.style.display = "";
            if (dojo.isIE) {
                _9f.style.removeExpression("width");
                _9f.style.removeExpression("height");
            }
            _9c.push(_9f);
        };
    }();
    if (dojo.isIE && dojo.isIE < 7) {
        dojo.addOnLoad(function() {
            var f = dijit._frames;
            dojo.forEach([f.pop()], f.push);
        });
    }
    dijit.BackgroundIframe = function(_a1) {
        if (!_a1.id) {
            throw new Error("no id");
        }
        if ((dojo.isIE && dojo.isIE < 7) ||
            (dojo.isFF && dojo.isFF < 3 && dojo.hasClass(dojo.body(), "dijit_a11y"))) {
            var _a2 = dijit._frames.pop();
            _a1.appendChild(_a2);
            if (dojo.isIE) {
                _a2.style.setExpression("width", dojo._scopeName + ".doc.getElementById('" +
                                                 _a1.id + "').offsetWidth");
                _a2.style.setExpression("height", dojo._scopeName + ".doc.getElementById('" +
                                                  _a1.id + "').offsetHeight");
            }
            this.iframe = _a2;
        }
    };
    dojo.extend(dijit.BackgroundIframe, {destroy:function() {
        if (this.iframe) {
            dijit._frames.push(this.iframe);
            delete this.iframe;
        }
    }});
}
if (!dojo._hasResource["dijit._base.scroll"]) {
    dojo._hasResource["dijit._base.scroll"] = true;
    dojo.provide("dijit._base.scroll");
    dijit.scrollIntoView = function(_a3) {
        var _a4 = _a3.parentNode;
        var _a5 = _a4.scrollTop + dojo.marginBox(_a4).h;
        var _a6 = _a3.offsetTop + dojo.marginBox(_a3).h;
        if (_a5 < _a6) {
            _a4.scrollTop += (_a6 - _a5);
        } else {
            if (_a4.scrollTop > _a3.offsetTop) {
                _a4.scrollTop -= (_a4.scrollTop - _a3.offsetTop);
            }
        }
    };
}
if (!dojo._hasResource["dijit._base.sniff"]) {
    dojo._hasResource["dijit._base.sniff"] = true;
    dojo.provide("dijit._base.sniff");
    (function() {
        var d = dojo;
        var ie = d.isIE;
        var _a9 = d.isOpera;
        var maj = Math.floor;
        var ff = d.isFF;
        var _ac = {dj_ie:ie,dj_ie6:maj(ie) == 6,dj_ie7:maj(ie) == 7,dj_iequirks:ie &&
                                                                                d.isQuirks,dj_opera:_a9,dj_opera8:maj(_a9) ==
                                                                                                                  8,dj_opera9:maj(_a9) ==
                                                                                                                              9,dj_khtml:d.isKhtml,dj_safari:d.isSafari,dj_gecko:d.isMozilla,dj_ff2:maj(ff) ==
                                                                                                                                                                                                    2};
        for (var p in _ac) {
            if (_ac[p]) {
                var _ae = dojo.doc.documentElement;
                if (_ae.className) {
                    _ae.className += " " + p;
                } else {
                    _ae.className = p;
                }
            }
        }
    })();
}
if (!dojo._hasResource["dijit._base.bidi"]) {
    dojo._hasResource["dijit._base.bidi"] = true;
    dojo.provide("dijit._base.bidi");
    dojo.addOnLoad(function() {
        if (!dojo._isBodyLtr()) {
            dojo.addClass(dojo.body(), "dijitRtl");
        }
    });
}
if (!dojo._hasResource["dijit._base.typematic"]) {
    dojo._hasResource["dijit._base.typematic"] = true;
    dojo.provide("dijit._base.typematic");
    dijit.typematic = {_fireEventAndReload:function() {
        this._timer = null;
        this._callback(++this._count, this._node, this._evt);
        this._currentTimeout = (this._currentTimeout < 0) ? this._initialDelay :
                               ((this._subsequentDelay > 1) ? this._subsequentDelay :
                                Math.round(this._currentTimeout * this._subsequentDelay));
        this._timer = setTimeout(dojo.hitch(this, "_fireEventAndReload"), this._currentTimeout);
    },trigger:function(evt, _b0, _b1, _b2, obj, _b4, _b5) {
        if (obj != this._obj) {
            this.stop();
            this._initialDelay = _b5 || 500;
            this._subsequentDelay = _b4 || 0.9;
            this._obj = obj;
            this._evt = evt;
            this._node = _b1;
            this._currentTimeout = -1;
            this._count = -1;
            this._callback = dojo.hitch(_b0, _b2);
            this._fireEventAndReload();
        }
    },stop:function() {
        if (this._timer) {
            clearTimeout(this._timer);
            this._timer = null;
        }
        if (this._obj) {
            this._callback(-1, this._node, this._evt);
            this._obj = null;
        }
    },addKeyListener:function(_b6, _b7, _b8, _b9, _ba, _bb) {
        return [dojo.connect(_b6, "onkeypress", this, function(evt) {
            if (evt.keyCode == _b7.keyCode && (!_b7.charCode || _b7.charCode == evt.charCode) &&
                (_b7.ctrlKey === undefined || _b7.ctrlKey == evt.ctrlKey) &&
                (_b7.altKey === undefined || _b7.altKey == evt.ctrlKey) &&
                (_b7.shiftKey === undefined || _b7.shiftKey == evt.ctrlKey)) {
                dojo.stopEvent(evt);
                dijit.typematic.trigger(_b7, _b8, _b6, _b9, _b7, _ba, _bb);
            } else {
                if (dijit.typematic._obj == _b7) {
                    dijit.typematic.stop();
                }
            }
        }),dojo.connect(_b6, "onkeyup", this, function(evt) {
            if (dijit.typematic._obj == _b7) {
                dijit.typematic.stop();
            }
        })];
    },addMouseListener:function(_be, _bf, _c0, _c1, _c2) {
        var dc = dojo.connect;
        return [dc(_be, "mousedown", this, function(evt) {
            dojo.stopEvent(evt);
            dijit.typematic.trigger(evt, _bf, _be, _c0, _be, _c1, _c2);
        }),dc(_be, "mouseup", this, function(evt) {
            dojo.stopEvent(evt);
            dijit.typematic.stop();
        }),dc(_be, "mouseout", this, function(evt) {
            dojo.stopEvent(evt);
            dijit.typematic.stop();
        }),dc(_be, "mousemove", this, function(evt) {
            dojo.stopEvent(evt);
        }),dc(_be, "dblclick", this, function(evt) {
            dojo.stopEvent(evt);
            if (dojo.isIE) {
                dijit.typematic.trigger(evt, _bf, _be, _c0, _be, _c1, _c2);
                setTimeout(dojo.hitch(this, dijit.typematic.stop), 50);
            }
        })];
    },addListener:function(_c9, _ca, _cb, _cc, _cd, _ce, _cf) {
        return this.addKeyListener(_ca, _cb, _cc, _cd, _ce, _cf).concat(this.addMouseListener(_c9, _cc, _cd, _ce, _cf));
    }};
}
if (!dojo._hasResource["dijit._base.wai"]) {
    dojo._hasResource["dijit._base.wai"] = true;
    dojo.provide("dijit._base.wai");
    dijit.wai = {onload:function() {
        var div = dojo.doc.createElement("div");
        div.id = "a11yTestNode";
        div.style.cssText =
        "border: 1px solid;" + "border-color:red green;" + "position: absolute;" + "height: 5px;" +
        "top: -999px;" + "background-image: url(\"" +
        dojo.moduleUrl("dojo", "resources/blank.gif") + "\");";
        dojo.body().appendChild(div);
        var cs = dojo.getComputedStyle(div);
        if (cs) {
            var _d2 = cs.backgroundImage;
            var _d3 = (cs.borderTopColor == cs.borderRightColor) ||
                      (_d2 != null && (_d2 == "none" || _d2 == "url(invalid-url:)"));
            dojo[_d3 ? "addClass" : "removeClass"](dojo.body(), "dijit_a11y");
            dojo.body().removeChild(div);
        }
    }};
    if (dojo.isIE || dojo.isMoz) {
        dojo._loaders.unshift(dijit.wai.onload);
    }
    dojo.mixin(dijit, {hasWaiRole:function(_d4) {
        return _d4.hasAttribute ? _d4.hasAttribute("role") : !!_d4.getAttribute("role");
    },getWaiRole:function(_d5) {
        var _d6 = _d5.getAttribute("role");
        if (_d6) {
            var _d7 = _d6.indexOf(":");
            return _d7 == -1 ? _d6 : _d6.substring(_d7 + 1);
        } else {
            return "";
        }
    },setWaiRole:function(_d8, _d9) {
        _d8.setAttribute("role", (dojo.isFF && dojo.isFF < 3) ? "wairole:" + _d9 : _d9);
    },removeWaiRole:function(_da) {
        _da.removeAttribute("role");
    },hasWaiState:function(_db, _dc) {
        if (dojo.isFF && dojo.isFF < 3) {
            return _db.hasAttributeNS("http://www.w3.org/2005/07/aaa", _dc);
        } else {
            return _db.hasAttribute ? _db.hasAttribute("aria-" + _dc) :
                   !!_db.getAttribute("aria-" + _dc);
        }
    },getWaiState:function(_dd, _de) {
        if (dojo.isFF && dojo.isFF < 3) {
            return _dd.getAttributeNS("http://www.w3.org/2005/07/aaa", _de);
        } else {
            var _df = _dd.getAttribute("aria-" + _de);
            return _df ? _df : "";
        }
    },setWaiState:function(_e0, _e1, _e2) {
        if (dojo.isFF && dojo.isFF < 3) {
            _e0.setAttributeNS("http://www.w3.org/2005/07/aaa", "aaa:" + _e1, _e2);
        } else {
            _e0.setAttribute("aria-" + _e1, _e2);
        }
    },removeWaiState:function(_e3, _e4) {
        if (dojo.isFF && dojo.isFF < 3) {
            _e3.removeAttributeNS("http://www.w3.org/2005/07/aaa", _e4);
        } else {
            _e3.removeAttribute("aria-" + _e4);
        }
    }});
}
if (!dojo._hasResource["dijit._base"]) {
    dojo._hasResource["dijit._base"] = true;
    dojo.provide("dijit._base");
    if (dojo.isSafari) {
        dojo.connect(window, "load", function() {
            window.resizeBy(1, 0);
            setTimeout(function() {
                window.resizeBy(-1, 0);
            }, 10);
        });
    }
}
if (!dojo._hasResource["dojo.date.stamp"]) {
    dojo._hasResource["dojo.date.stamp"] = true;
    dojo.provide("dojo.date.stamp");
    dojo.date.stamp.fromISOString = function(_e5, _e6) {
        if (!dojo.date.stamp._isoRegExp) {
            dojo.date.stamp._isoRegExp =
            /^(?:(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?)?(?:T(\d{2}):(\d{2})(?::(\d{2})(.\d+)?)?((?:[+-](\d{2}):(\d{2}))|Z)?)?$/;
        }
        var _e7 = dojo.date.stamp._isoRegExp.exec(_e5);
        var _e8 = null;
        if (_e7) {
            _e7.shift();
            if (_e7[1]) {
                _e7[1]--;
            }
            if (_e7[6]) {
                _e7[6] *= 1000;
            }
            if (_e6) {
                _e6 = new Date(_e6);
                dojo.map(["FullYear","Month","Date","Hours","Minutes","Seconds","Milliseconds"], function(
                        _e9) {
                    return _e6["get" + _e9]();
                }).forEach(function(_ea, _eb) {
                    if (_e7[_eb] === undefined) {
                        _e7[_eb] = _ea;
                    }
                });
            }
            _e8 =
            new Date(_e7[0] || 1970, _e7[1] || 0, _e7[2] || 1, _e7[3] || 0, _e7[4] || 0, _e7[5] ||
                                                                                         0, _e7[6] ||
                                                                                            0);
            var _ec = 0;
            var _ed = _e7[7] && _e7[7].charAt(0);
            if (_ed != "Z") {
                _ec = ((_e7[8] || 0) * 60) + (Number(_e7[9]) || 0);
                if (_ed != "-") {
                    _ec *= -1;
                }
            }
            if (_ed) {
                _ec -= _e8.getTimezoneOffset();
            }
            if (_ec) {
                _e8.setTime(_e8.getTime() + _ec * 60000);
            }
        }
        return _e8;
    };
    dojo.date.stamp.toISOString = function(_ee, _ef) {
        var _ = function(n) {
            return (n < 10) ? "0" + n : n;
        };
        _ef = _ef || {};
        var _f2 = [];
        var _f3 = _ef.zulu ? "getUTC" : "get";
        var _f4 = "";
        if (_ef.selector != "time") {
            var _f5 = _ee[_f3 + "FullYear"]();
            _f4 = ["0000".substr((_f5 + "").length) + _f5,_(_ee[_f3 + "Month"]() + 1),_(_ee[_f3 +
                                                                                            "Date"]())].join("-");
        }
        _f2.push(_f4);
        if (_ef.selector != "date") {
            var _f6 = [_(_ee[_f3 + "Hours"]()),_(_ee[_f3 + "Minutes"]()),_(_ee[_f3 +
                                                                               "Seconds"]())].join(":");
            var _f7 = _ee[_f3 + "Milliseconds"]();
            if (_ef.milliseconds) {
                _f6 += "." + (_f7 < 100 ? "0" : "") + _(_f7);
            }
            if (_ef.zulu) {
                _f6 += "Z";
            } else {
                if (_ef.selector != "time") {
                    var _f8 = _ee.getTimezoneOffset();
                    var _f9 = Math.abs(_f8);
                    _f6 += (_f8 > 0 ? "-" : "+") + _(Math.floor(_f9 / 60)) + ":" + _(_f9 % 60);
                }
            }
            _f2.push(_f6);
        }
        return _f2.join("T");
    };
}
if (!dojo._hasResource["dojo.parser"]) {
    dojo._hasResource["dojo.parser"] = true;
    dojo.provide("dojo.parser");
    dojo.parser = new function() {
        var d = dojo;
        var _fb = d._scopeName + "Type";
        var qry = "[" + _fb + "]";
        function val2type(_fd) {
            if (d.isString(_fd)) {
                return "string";
            }
            if (typeof _fd == "number") {
                return "number";
            }
            if (typeof _fd == "boolean") {
                return "boolean";
            }
            if (d.isFunction(_fd)) {
                return "function";
            }
            if (d.isArray(_fd)) {
                return "array";
            }
            if (_fd instanceof Date) {
                return "date";
            }
            if (_fd instanceof d._Url) {
                return "url";
            }
            return "object";
        }
        ;
        function str2obj(_fe, _ff) {
            switch (_ff) {case "string":return _fe;case "number":return _fe.length ? Number(_fe) :
                                                                        NaN;case "boolean":return
                typeof _fe == "boolean" ? _fe :
                !(_fe.toLowerCase() == "false");case "function":if (d.isFunction(_fe)) {
                _fe = _fe.toString();
                _fe = d.trim(_fe.substring(_fe.indexOf("{") + 1, _fe.length - 1));
            }try {
                if (_fe.search(/[^\w\.]+/i) != -1) {
                    _fe = d.parser._nameAnonFunc(new Function(_fe), this);
                }
                return d.getObject(_fe, false);
            } catch(e) {
                return new Function();
            }case "array":return _fe.split(/\s*,\s*/);case "date":switch (_fe) {case "":return new Date("");case "now":return new Date();default:return d.date.stamp.fromISOString(_fe);}case "url":return d.baseUrl +
                                                                                                                                                                                                           _fe;default:return d.fromJson(_fe);}
        }
        ;
        var _100 = {};
        function getClassInfo(_101) {
            if (!_100[_101]) {
                var cls = d.getObject(_101);
                if (!d.isFunction(cls)) {
                    throw new Error("Could not load class '" + _101 +
                                    "'. Did you spell the name correctly and use a full path, like 'dijit.form.Button'?");
                }
                var _103 = cls.prototype;
                var _104 = {};
                for (var name in _103) {
                    if (name.charAt(0) == "_") {
                        continue;
                    }
                    var _106 = _103[name];
                    _104[name] = val2type(_106);
                }
                _100[_101] = {cls:cls,params:_104};
            }
            return _100[_101];
        }
        ;
        this._functionFromScript = function(_107) {
            var _108 = "";
            var _109 = "";
            var _10a = _107.getAttribute("args");
            if (_10a) {
                d.forEach(_10a.split(/\s*,\s*/), function(part, idx) {
                    _108 += "var " + part + " = arguments[" + idx + "]; ";
                });
            }
            var _10d = _107.getAttribute("with");
            if (_10d && _10d.length) {
                d.forEach(_10d.split(/\s*,\s*/), function(part) {
                    _108 += "with(" + part + "){";
                    _109 += "}";
                });
            }
            return new Function(_108 + _107.innerHTML + _109);
        };
        this.instantiate = function(_10f) {
            var _110 = [];
            d.forEach(_10f, function(node) {
                if (!node) {
                    return;
                }
                var type = node.getAttribute(_fb);
                if ((!type) || (!type.length)) {
                    return;
                }
                var _113 = getClassInfo(type);
                var _114 = _113.cls;
                var ps = _114._noScript || _114.prototype._noScript;
                var _116 = {};
                var _117 = node.attributes;
                for (var name in _113.params) {
                    var item = _117.getNamedItem(name);
                    if (!item ||
                        (!item.specified && (!dojo.isIE || name.toLowerCase() != "value"))) {
                        continue;
                    }
                    var _11a = item.value;
                    switch (name) {case "class":_11a = node.className;break;case "style":_11a =
                                                                                         node.style &&
                                                                                         node.style.cssText;}
                    var _11b = _113.params[name];
                    _116[name] = str2obj(_11a, _11b);
                }
                if (!ps) {
                    var _11c = [],_11d = [];
                    d.query("> script[type^='dojo/']", node).orphan().forEach(function(_11e) {
                        var _11f = _11e.getAttribute("event"),type = _11e.getAttribute("type"),nf = d.parser._functionFromScript(_11e);
                        if (_11f) {
                            if (type == "dojo/connect") {
                                _11c.push({event:_11f,func:nf});
                            } else {
                                _116[_11f] = nf;
                            }
                        } else {
                            _11d.push(nf);
                        }
                    });
                }
                var _121 = _114["markupFactory"];
                if (!_121 && _114["prototype"]) {
                    _121 = _114.prototype["markupFactory"];
                }
                var _122 = _121 ? _121(_116, node, _114) : new _114(_116, node);
                _110.push(_122);
                var _123 = node.getAttribute("jsId");
                if (_123) {
                    d.setObject(_123, _122);
                }
                if (!ps) {
                    d.forEach(_11c, function(_124) {
                        d.connect(_122, _124.event, null, _124.func);
                    });
                    d.forEach(_11d, function(func) {
                        func.call(_122);
                    });
                }
            });
            d.forEach(_110, function(_126) {
                if (_126 && _126.startup && !_126._started &&
                    (!_126.getParent || !_126.getParent())) {
                    _126.startup();
                }
            });
            return _110;
        };
        this.parse = function(_127) {
            var list = d.query(qry, _127);
            var _129 = this.instantiate(list);
            return _129;
        };
    }();
    (function() {
        var _12a = function() {
            if (dojo.config["parseOnLoad"] == true) {
                dojo.parser.parse();
            }
        };
        if (dojo.exists("dijit.wai.onload") && (dijit.wai.onload === dojo._loaders[0])) {
            dojo._loaders.splice(1, 0, _12a);
        } else {
            dojo._loaders.unshift(_12a);
        }
    })();
    dojo.parser._anonCtr = 0;
    dojo.parser._anon = {};
    dojo.parser._nameAnonFunc = function(_12b, _12c) {
        var jpn = "$joinpoint";
        var nso = (_12c || dojo.parser._anon);
        if (dojo.isIE) {
            var cn = _12b["__dojoNameCache"];
            if (cn && nso[cn] === _12b) {
                return _12b["__dojoNameCache"];
            }
        }
        var ret = "__" + dojo.parser._anonCtr++;
        while (typeof nso[ret] != "undefined") {
            ret = "__" + dojo.parser._anonCtr++;
        }
        nso[ret] = _12b;
        return ret;
    };
}
if (!dojo._hasResource["dijit._Widget"]) {
    dojo._hasResource["dijit._Widget"] = true;
    dojo.provide("dijit._Widget");
    dojo.require("dijit._base");
    dojo.declare("dijit._Widget", null, {id:"",lang:"",dir:"","class":"",style:"",title:"",srcNodeRef:null,domNode:null,attributeMap:{id:"",dir:"",lang:"","class":"",style:"",title:""},postscript:function(
            _131, _132) {
        this.create(_131, _132);
    },create:function(_133, _134) {
        this.srcNodeRef = dojo.byId(_134);
        this._connects = [];
        this._attaches = [];
        if (this.srcNodeRef && (typeof this.srcNodeRef.id == "string")) {
            this.id = this.srcNodeRef.id;
        }
        if (_133) {
            this.params = _133;
            dojo.mixin(this, _133);
        }
        this.postMixInProperties();
        if (!this.id) {
            this.id = dijit.getUniqueId(this.declaredClass.replace(/\./g, "_"));
        }
        dijit.registry.add(this);
        this.buildRendering();
        if (this.domNode) {
            for (var attr in this.attributeMap) {
                var _136 = this[attr];
                if (typeof _136 != "object" &&
                    ((_136 !== "" && _136 !== false) || (_133 && _133[attr]))) {
                    this.setAttribute(attr, _136);
                }
            }
        }
        if (this.domNode) {
            this.domNode.setAttribute("widgetId", this.id);
        }
        this.postCreate();
        if (this.srcNodeRef && !this.srcNodeRef.parentNode) {
            delete this.srcNodeRef;
        }
    },postMixInProperties:function() {
    },buildRendering:function() {
        this.domNode = this.srcNodeRef || dojo.doc.createElement("div");
    },postCreate:function() {
    },startup:function() {
        this._started = true;
    },destroyRecursive:function(_137) {
        this.destroyDescendants();
        this.destroy();
    },destroy:function(_138) {
        this.uninitialize();
        dojo.forEach(this._connects, function(_139) {
            dojo.forEach(_139, dojo.disconnect);
        });
        dojo.forEach(this._supportingWidgets || [], function(w) {
            w.destroy();
        });
        this.destroyRendering(_138);
        dijit.registry.remove(this.id);
    },destroyRendering:function(_13b) {
        if (this.bgIframe) {
            this.bgIframe.destroy();
            delete this.bgIframe;
        }
        if (this.domNode) {
            dojo._destroyElement(this.domNode);
            delete this.domNode;
        }
        if (this.srcNodeRef) {
            dojo._destroyElement(this.srcNodeRef);
            delete this.srcNodeRef;
        }
    },destroyDescendants:function() {
        dojo.forEach(this.getDescendants(), function(_13c) {
            _13c.destroy();
        });
    },uninitialize:function() {
        return false;
    },onFocus:function() {
    },onBlur:function() {
    },_onFocus:function(e) {
        this.onFocus();
    },_onBlur:function() {
        this.onBlur();
    },setAttribute:function(attr, _13f) {
        var _140 = this[this.attributeMap[attr] || "domNode"];
        this[attr] = _13f;
        switch (attr) {case "class":dojo.addClass(_140, _13f);break;case "style":if (_140.style.cssText) {
            _140.style.cssText += "; " + _13f;
        } else {
            _140.style.cssText = _13f;
        }break;default:if (/^on[A-Z]/.test(attr)) {
            attr = attr.toLowerCase();
        }if (typeof _13f == "function") {
            _13f = dojo.hitch(this, _13f);
        }dojo.attr(_140, attr, _13f);}
    },toString:function() {
        return "[Widget " + this.declaredClass + ", " + (this.id || "NO ID") + "]";
    },getDescendants:function() {
        if (this.containerNode) {
            var list = dojo.query("[widgetId]", this.containerNode);
            return list.map(dijit.byNode);
        } else {
            return [];
        }
    },nodesWithKeyClick:["input","button"],connect:function(obj, _143, _144) {
        var _145 = [];
        if (_143 == "ondijitclick") {
            if (!this.nodesWithKeyClick[obj.nodeName]) {
                _145.push(dojo.connect(obj, "onkeydown", this, function(e) {
                    if (e.keyCode == dojo.keys.ENTER) {
                        return (dojo.isString(_144)) ? this[_144](e) : _144.call(this, e);
                    } else {
                        if (e.keyCode == dojo.keys.SPACE) {
                            dojo.stopEvent(e);
                        }
                    }
                }));
                _145.push(dojo.connect(obj, "onkeyup", this, function(e) {
                    if (e.keyCode == dojo.keys.SPACE) {
                        return dojo.isString(_144) ? this[_144](e) : _144.call(this, e);
                    }
                }));
            }
            _143 = "onclick";
        }
        _145.push(dojo.connect(obj, _143, this, _144));
        this._connects.push(_145);
        return _145;
    },disconnect:function(_148) {
        for (var i = 0; i < this._connects.length; i++) {
            if (this._connects[i] == _148) {
                dojo.forEach(_148, dojo.disconnect);
                this._connects.splice(i, 1);
                return;
            }
        }
    },isLeftToRight:function() {
        if (!("_ltr" in this)) {
            this._ltr = dojo.getComputedStyle(this.domNode).direction != "rtl";
        }
        return this._ltr;
    },isFocusable:function() {
        return this.focus && (dojo.style(this.domNode, "display") != "none");
    }});
}
if (!dojo._hasResource["dojo.string"]) {
    dojo._hasResource["dojo.string"] = true;
    dojo.provide("dojo.string");
    dojo.string.pad = function(text, size, ch, end) {
        var out = String(text);
        if (!ch) {
            ch = "0";
        }
        while (out.length < size) {
            if (end) {
                out += ch;
            } else {
                out = ch + out;
            }
        }
        return out;
    };
    dojo.string.substitute = function(_14f, map, _151, _152) {
        return _14f.replace(/\$\{([^\s\:\}]+)(?:\:([^\s\:\}]+))?\}/g, function(_153, key, _155) {
            var _156 = dojo.getObject(key, false, map);
            if (_155) {
                _156 = dojo.getObject(_155, false, _152)(_156);
            }
            if (_151) {
                _156 = _151(_156, key);
            }
            return _156.toString();
        });
    };
    dojo.string.trim = function(str) {
        str = str.replace(/^\s+/, "");
        for (var i = str.length - 1; i > 0; i--) {
            if (/\S/.test(str.charAt(i))) {
                str = str.substring(0, i + 1);
                break;
            }
        }
        return str;
    };
}
if (!dojo._hasResource["dijit._Templated"]) {
    dojo._hasResource["dijit._Templated"] = true;
    dojo.provide("dijit._Templated");
    dojo.declare("dijit._Templated", null, {templateNode:null,templateString:null,templatePath:null,widgetsInTemplate:false,containerNode:null,_skipNodeCache:false,_stringRepl:function(
            tmpl) {
        var _15a = this.declaredClass,_15b = this;
        return dojo.string.substitute(tmpl, this, function(_15c, key) {
            if (key.charAt(0) == "!") {
                _15c = _15b[key.substr(1)];
            }
            if (typeof _15c == "undefined") {
                throw new Error(_15a + " template:" + key);
            }
            if (!_15c) {
                return "";
            }
            return key.charAt(0) == "!" ? _15c : _15c.toString().replace(/"/g, "&quot;");
        }, this);
    },buildRendering:function() {
        var _15e = dijit._Templated.getCachedTemplate(this.templatePath, this.templateString, this._skipNodeCache);
        var node;
        if (dojo.isString(_15e)) {
            node = dijit._Templated._createNodesFromText(this._stringRepl(_15e))[0];
        } else {
            node = _15e.cloneNode(true);
        }
        this._attachTemplateNodes(node);
        var _160 = this.srcNodeRef;
        if (_160 && _160.parentNode) {
            _160.parentNode.replaceChild(node, _160);
        }
        this.domNode = node;
        if (this.widgetsInTemplate) {
            var cw = this._supportingWidgets = dojo.parser.parse(node);
            this._attachTemplateNodes(cw, function(n, p) {
                return n[p];
            });
        }
        this._fillContent(_160);
    },_fillContent:function(_164) {
        var dest = this.containerNode;
        if (_164 && dest) {
            while (_164.hasChildNodes()) {
                dest.appendChild(_164.firstChild);
            }
        }
    },_attachTemplateNodes:function(_166, _167) {
        _167 = _167 || function(n, p) {
            return n.getAttribute(p);
        };
        var _16a = dojo.isArray(_166) ? _166 : (_166.all || _166.getElementsByTagName("*"));
        var x = dojo.isArray(_166) ? 0 : -1;
        for (; x < _16a.length; x++) {
            var _16c = (x == -1) ? _166 : _16a[x];
            if (this.widgetsInTemplate && _167(_16c, "dojoType")) {
                continue;
            }
            var _16d = _167(_16c, "dojoAttachPoint");
            if (_16d) {
                var _16e,_16f = _16d.split(/\s*,\s*/);
                while ((_16e = _16f.shift())) {
                    if (dojo.isArray(this[_16e])) {
                        this[_16e].push(_16c);
                    } else {
                        this[_16e] = _16c;
                    }
                }
            }
            var _170 = _167(_16c, "dojoAttachEvent");
            if (_170) {
                var _171,_172 = _170.split(/\s*,\s*/);
                var trim = dojo.trim;
                while ((_171 = _172.shift())) {
                    if (_171) {
                        var _174 = null;
                        if (_171.indexOf(":") != -1) {
                            var _175 = _171.split(":");
                            _171 = trim(_175[0]);
                            _174 = trim(_175[1]);
                        } else {
                            _171 = trim(_171);
                        }
                        if (!_174) {
                            _174 = _171;
                        }
                        this.connect(_16c, _171, _174);
                    }
                }
            }
            var role = _167(_16c, "waiRole");
            if (role) {
                dijit.setWaiRole(_16c, role);
            }
            var _177 = _167(_16c, "waiState");
            if (_177) {
                dojo.forEach(_177.split(/\s*,\s*/), function(_178) {
                    if (_178.indexOf("-") != -1) {
                        var pair = _178.split("-");
                        dijit.setWaiState(_16c, pair[0], pair[1]);
                    }
                });
            }
        }
    }});
    dijit._Templated._templateCache = {};
    dijit._Templated.getCachedTemplate = function(_17a, _17b, _17c) {
        var _17d = dijit._Templated._templateCache;
        var key = _17b || _17a;
        var _17f = _17d[key];
        if (_17f) {
            return _17f;
        }
        if (!_17b) {
            _17b = dijit._Templated._sanitizeTemplateString(dojo._getText(_17a));
        }
        _17b = dojo.string.trim(_17b);
        if (_17c || _17b.match(/\$\{([^\}]+)\}/g)) {
            return (_17d[key] = _17b);
        } else {
            return (_17d[key] = dijit._Templated._createNodesFromText(_17b)[0]);
        }
    };
    dijit._Templated._sanitizeTemplateString = function(_180) {
        if (_180) {
            _180 = _180.replace(/^\s*<\?xml(\s)+version=[\'\"](\d)*.(\d)*[\'\"](\s)*\?>/im, "");
            var _181 = _180.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
            if (_181) {
                _180 = _181[1];
            }
        } else {
            _180 = "";
        }
        return _180;
    };
    if (dojo.isIE) {
        dojo.addOnUnload(function() {
            var _182 = dijit._Templated._templateCache;
            for (var key in _182) {
                var _184 = _182[key];
                if (!isNaN(_184.nodeType)) {
                    dojo._destroyElement(_184);
                }
                delete _182[key];
            }
        });
    }
    (function() {
        var _185 = {cell:{re:/^<t[dh][\s\r\n>]/i,pre:"<table><tbody><tr>",post:"</tr></tbody></table>"},row:{re:/^<tr[\s\r\n>]/i,pre:"<table><tbody>",post:"</tbody></table>"},section:{re:/^<(thead|tbody|tfoot)[\s\r\n>]/i,pre:"<table>",post:"</table>"}};
        var tn;
        dijit._Templated._createNodesFromText = function(text) {
            if (!tn) {
                tn = dojo.doc.createElement("div");
                tn.style.display = "none";
                dojo.body().appendChild(tn);
            }
            var _188 = "none";
            var _189 = text.replace(/^\s+/, "");
            for (var type in _185) {
                var map = _185[type];
                if (map.re.test(_189)) {
                    _188 = type;
                    text = map.pre + text + map.post;
                    break;
                }
            }
            tn.innerHTML = text;
            if (tn.normalize) {
                tn.normalize();
            }
            var tag = {cell:"tr",row:"tbody",section:"table"}[_188];
            var _18d = (typeof tag != "undefined") ? tn.getElementsByTagName(tag)[0] : tn;
            var _18e = [];
            while (_18d.firstChild) {
                _18e.push(_18d.removeChild(_18d.firstChild));
            }
            tn.innerHTML = "";
            return _18e;
        };
    })();
    dojo.extend(dijit._Widget, {dojoAttachEvent:"",dojoAttachPoint:"",waiRole:"",waiState:""});
}
if (!dojo._hasResource["dijit._Container"]) {
    dojo._hasResource["dijit._Container"] = true;
    dojo.provide("dijit._Container");
    dojo.declare("dijit._Contained", null, {getParent:function() {
        for (var p = this.domNode.parentNode; p; p = p.parentNode) {
            var id = p.getAttribute && p.getAttribute("widgetId");
            if (id) {
                var _191 = dijit.byId(id);
                return _191.isContainer ? _191 : null;
            }
        }
        return null;
    },_getSibling:function(_192) {
        var node = this.domNode;
        do{
            node = node[_192 + "Sibling"];
        } while (node && node.nodeType != 1);
        if (!node) {
            return null;
        }
        var id = node.getAttribute("widgetId");
        return dijit.byId(id);
    },getPreviousSibling:function() {
        return this._getSibling("previous");
    },getNextSibling:function() {
        return this._getSibling("next");
    }});
    dojo.declare("dijit._Container", null, {isContainer:true,addChild:function(_195, _196) {
        if (_196 === undefined) {
            _196 = "last";
        }
        var _197 = this.containerNode || this.domNode;
        if (_196 && typeof _196 == "number") {
            var _198 = dojo.query("> [widgetid]", _197);
            if (_198 && _198.length >= _196) {
                _197 = _198[_196 - 1];
                _196 = "after";
            }
        }
        dojo.place(_195.domNode, _197, _196);
        if (this._started && !_195._started) {
            _195.startup();
        }
    },removeChild:function(_199) {
        var node = _199.domNode;
        node.parentNode.removeChild(node);
    },_nextElement:function(node) {
        do{
            node = node.nextSibling;
        } while (node && node.nodeType != 1);
        return node;
    },_firstElement:function(node) {
        node = node.firstChild;
        if (node && node.nodeType != 1) {
            node = this._nextElement(node);
        }
        return node;
    },getChildren:function() {
        return dojo.query("> [widgetId]", this.containerNode || this.domNode).map(dijit.byNode);
    },hasChildren:function() {
        var cn = this.containerNode || this.domNode;
        return !!this._firstElement(cn);
    },_getSiblingOfChild:function(_19e, dir) {
        var node = _19e.domNode;
        var _1a1 = (dir > 0 ? "nextSibling" : "previousSibling");
        do{
            node = node[_1a1];
        } while (node && (node.nodeType != 1 || !dijit.byNode(node)));
        return node ? dijit.byNode(node) : null;
    }});
    dojo.declare("dijit._KeyNavContainer", [dijit._Container], {_keyNavCodes:{},connectKeyNavHandlers:function(
            _1a2, _1a3) {
        var _1a4 = this._keyNavCodes = {};
        var prev = dojo.hitch(this, this.focusPrev);
        var next = dojo.hitch(this, this.focusNext);
        dojo.forEach(_1a2, function(code) {
            _1a4[code] = prev;
        });
        dojo.forEach(_1a3, function(code) {
            _1a4[code] = next;
        });
        this.connect(this.domNode, "onkeypress", "_onContainerKeypress");
        this.connect(this.domNode, "onfocus", "_onContainerFocus");
    },startupKeyNavChildren:function() {
        dojo.forEach(this.getChildren(), dojo.hitch(this, "_startupChild"));
    },addChild:function(_1a9, _1aa) {
        dijit._KeyNavContainer.superclass.addChild.apply(this, arguments);
        this._startupChild(_1a9);
    },focus:function() {
        this.focusFirstChild();
    },focusFirstChild:function() {
        this.focusChild(this._getFirstFocusableChild());
    },focusNext:function() {
        if (this.focusedChild && this.focusedChild.hasNextFocalNode &&
            this.focusedChild.hasNextFocalNode()) {
            this.focusedChild.focusNext();
            return;
        }
        var _1ab = this._getNextFocusableChild(this.focusedChild, 1);
        if (_1ab.getFocalNodes) {
            this.focusChild(_1ab, _1ab.getFocalNodes()[0]);
        } else {
            this.focusChild(_1ab);
        }
    },focusPrev:function() {
        if (this.focusedChild && this.focusedChild.hasPrevFocalNode &&
            this.focusedChild.hasPrevFocalNode()) {
            this.focusedChild.focusPrev();
            return;
        }
        var _1ac = this._getNextFocusableChild(this.focusedChild, -1);
        if (_1ac.getFocalNodes) {
            var _1ad = _1ac.getFocalNodes();
            this.focusChild(_1ac, _1ad[_1ad.length - 1]);
        } else {
            this.focusChild(_1ac);
        }
    },focusChild:function(_1ae, node) {
        if (_1ae) {
            if (this.focusedChild && _1ae !== this.focusedChild) {
                this._onChildBlur(this.focusedChild);
            }
            this.focusedChild = _1ae;
            if (node && _1ae.focusFocalNode) {
                _1ae.focusFocalNode(node);
            } else {
                _1ae.focus();
            }
        }
    },_startupChild:function(_1b0) {
        if (_1b0.getFocalNodes) {
            dojo.forEach(_1b0.getFocalNodes(), function(node) {
                dojo.attr(node, "tabindex", -1);
                this._connectNode(node);
            }, this);
        } else {
            var node = _1b0.focusNode || _1b0.domNode;
            if (_1b0.isFocusable()) {
                dojo.attr(node, "tabindex", -1);
            }
            this._connectNode(node);
        }
    },_connectNode:function(node) {
        this.connect(node, "onfocus", "_onNodeFocus");
        this.connect(node, "onblur", "_onNodeBlur");
    },_onContainerFocus:function(evt) {
        if (evt.target === this.domNode) {
            this.focusFirstChild();
        }
    },_onContainerKeypress:function(evt) {
        if (evt.ctrlKey || evt.altKey) {
            return;
        }
        var func = this._keyNavCodes[evt.keyCode];
        if (func) {
            func();
            dojo.stopEvent(evt);
        }
    },_onNodeFocus:function(evt) {
        dojo.attr(this.domNode, "tabindex", -1);
        var _1b8 = dijit.getEnclosingWidget(evt.target);
        if (_1b8 && _1b8.isFocusable()) {
            this.focusedChild = _1b8;
        }
        dojo.stopEvent(evt);
    },_onNodeBlur:function(evt) {
        if (this.tabIndex) {
            dojo.attr(this.domNode, "tabindex", this.tabIndex);
        }
        dojo.stopEvent(evt);
    },_onChildBlur:function(_1ba) {
    },_getFirstFocusableChild:function() {
        return this._getNextFocusableChild(null, 1);
    },_getNextFocusableChild:function(_1bb, dir) {
        if (_1bb) {
            _1bb = this._getSiblingOfChild(_1bb, dir);
        }
        var _1bd = this.getChildren();
        for (var i = 0; i < _1bd.length; i++) {
            if (!_1bb) {
                _1bb = _1bd[(dir > 0) ? 0 : (_1bd.length - 1)];
            }
            if (_1bb.isFocusable()) {
                return _1bb;
            }
            _1bb = this._getSiblingOfChild(_1bb, dir);
        }
        return null;
    }});
}
if (!dojo._hasResource["dijit.layout._LayoutWidget"]) {
    dojo._hasResource["dijit.layout._LayoutWidget"] = true;
    dojo.provide("dijit.layout._LayoutWidget");
    dojo.declare("dijit.layout._LayoutWidget", [dijit._Widget,dijit._Container,dijit._Contained], {isLayoutContainer:true,postCreate:function() {
        dojo.addClass(this.domNode, "dijitContainer");
    },startup:function() {
        if (this._started) {
            return;
        }
        dojo.forEach(this.getChildren(), function(_1bf) {
            _1bf.startup();
        });
        if (!this.getParent || !this.getParent()) {
            this.resize();
            this.connect(window, "onresize", function() {
                this.resize();
            });
        }
        this.inherited(arguments);
    },resize:function(args) {
        var node = this.domNode;
        if (args) {
            dojo.marginBox(node, args);
            if (args.t) {
                node.style.top = args.t + "px";
            }
            if (args.l) {
                node.style.left = args.l + "px";
            }
        }
        var mb = dojo.mixin(dojo.marginBox(node), args || {});
        this._contentBox = dijit.layout.marginBox2contentBox(node, mb);
        this.layout();
    },layout:function() {
    }});
    dijit.layout.marginBox2contentBox = function(node, mb) {
        var cs = dojo.getComputedStyle(node);
        var me = dojo._getMarginExtents(node, cs);
        var pb = dojo._getPadBorderExtents(node, cs);
        return {l:dojo._toPixelValue(node, cs.paddingLeft),t:dojo._toPixelValue(node, cs.paddingTop),w:mb.w -
                                                                                                       (me.w +
                                                                                                        pb.w),h:mb.h -
                                                                                                                (me.h +
                                                                                                                 pb.h)};
    };
    (function() {
        var _1c8 = function(word) {
            return word.substring(0, 1).toUpperCase() + word.substring(1);
        };
        var size = function(_1cb, dim) {
            _1cb.resize ? _1cb.resize(dim) : dojo.marginBox(_1cb.domNode, dim);
            dojo.mixin(_1cb, dojo.marginBox(_1cb.domNode));
            dojo.mixin(_1cb, dim);
        };
        dijit.layout.layoutChildren = function(_1cd, dim, _1cf) {
            dim = dojo.mixin({}, dim);
            dojo.addClass(_1cd, "dijitLayoutContainer");
            _1cf = dojo.filter(_1cf, function(item) {
                return item.layoutAlign != "client";
            }).concat(dojo.filter(_1cf, function(item) {
                return item.layoutAlign == "client";
            }));
            dojo.forEach(_1cf, function(_1d2) {
                var elm = _1d2.domNode,pos = _1d2.layoutAlign;
                var _1d5 = elm.style;
                _1d5.left = dim.l + "px";
                _1d5.top = dim.t + "px";
                _1d5.bottom = _1d5.right = "auto";
                dojo.addClass(elm, "dijitAlign" + _1c8(pos));
                if (pos == "top" || pos == "bottom") {
                    size(_1d2, {w:dim.w});
                    dim.h -= _1d2.h;
                    if (pos == "top") {
                        dim.t += _1d2.h;
                    } else {
                        _1d5.top = dim.t + dim.h + "px";
                    }
                } else {
                    if (pos == "left" || pos == "right") {
                        size(_1d2, {h:dim.h});
                        dim.w -= _1d2.w;
                        if (pos == "left") {
                            dim.l += _1d2.w;
                        } else {
                            _1d5.left = dim.l + dim.w + "px";
                        }
                    } else {
                        if (pos == "client") {
                            size(_1d2, dim);
                        }
                    }
                }
            });
        };
    })();
}
if (!dojo._hasResource["dijit.form._FormWidget"]) {
    dojo._hasResource["dijit.form._FormWidget"] = true;
    dojo.provide("dijit.form._FormWidget");
    dojo.declare("dijit.form._FormWidget", [dijit._Widget,dijit._Templated], {baseClass:"",name:"",alt:"",value:"",type:"text",tabIndex:"0",disabled:false,readOnly:false,intermediateChanges:false,attributeMap:dojo.mixin(dojo.clone(dijit._Widget.prototype.attributeMap), {value:"focusNode",disabled:"focusNode",readOnly:"focusNode",id:"focusNode",tabIndex:"focusNode",alt:"focusNode"}),setAttribute:function(
            attr, _1d7) {
        this.inherited(arguments);
        switch (attr) {case "disabled":var _1d8 = this[this.attributeMap["tabIndex"] ||
                                                       "domNode"];if (_1d7) {
            this._hovering = false;
            this._active = false;
            _1d8.removeAttribute("tabIndex");
        } else {
            _1d8.setAttribute("tabIndex", this.tabIndex);
        }dijit.setWaiState(this[this.attributeMap["disabled"] ||
                                "domNode"], "disabled", _1d7);this._setStateClass();}
    },setDisabled:function(_1d9) {
        dojo.deprecated("setDisabled(" + _1d9 + ") is deprecated. Use setAttribute('disabled'," +
                        _1d9 + ") instead.", "", "2.0");
        this.setAttribute("disabled", _1d9);
    },_onMouse:function(_1da) {
        var _1db = _1da.currentTarget;
        if (_1db && _1db.getAttribute) {
            this.stateModifier = _1db.getAttribute("stateModifier") || "";
        }
        if (!this.disabled) {
            switch (_1da.type) {case "mouseenter":case "mouseover":this._hovering =
                                                                   true;this._active =
                                                                        this._mouseDown;break;case "mouseout":case "mouseleave":this._hovering =
                                                                                                                                false;this._active =
                                                                                                                                      false;break;case "mousedown":this._active =
                                                                                                                                                                   true;this._mouseDown =
                                                                                                                                                                        true;var _1dc = this.connect(dojo.body(), "onmouseup", function() {
                this._active = false;
                this._mouseDown = false;
                this._setStateClass();
                this.disconnect(_1dc);
            });if (this.isFocusable()) {
                this.focus();
            }break;}
            this._setStateClass();
        }
    },isFocusable:function() {
        return !this.disabled && !this.readOnly && this.focusNode &&
               (dojo.style(this.domNode, "display") != "none");
    },focus:function() {
        setTimeout(dojo.hitch(this, dijit.focus, this.focusNode), 0);
    },_setStateClass:function() {
        if (!("staticClass" in this)) {
            this.staticClass = (this.stateNode || this.domNode).className;
        }
        var _1dd = [this.baseClass];
        function multiply(_1de) {
            _1dd = _1dd.concat(dojo.map(_1dd, function(c) {
                return c + _1de;
            }), "dijit" + _1de);
        }
        ;
        if (this.checked) {
            multiply("Checked");
        }
        if (this.state) {
            multiply(this.state);
        }
        if (this.selected) {
            multiply("Selected");
        }
        if (this.disabled) {
            multiply("Disabled");
        } else {
            if (this.readOnly) {
                multiply("ReadOnly");
            } else {
                if (this._active) {
                    multiply(this.stateModifier + "Active");
                } else {
                    if (this._focused) {
                        multiply("Focused");
                    }
                    if (this._hovering) {
                        multiply(this.stateModifier + "Hover");
                    }
                }
            }
        }
        (this.stateNode || this.domNode).className = this.staticClass + " " + _1dd.join(" ");
    },onChange:function(_1e0) {
    },_onChangeMonitor:"value",_onChangeActive:false,_handleOnChange:function(_1e1, _1e2) {
        this._lastValue = _1e1;
        if (this._lastValueReported == undefined && (_1e2 === null || !this._onChangeActive)) {
            this._resetValue = this._lastValueReported = _1e1;
        }
        if ((this.intermediateChanges || _1e2 || _1e2 === undefined) &&
            ((_1e1 && _1e1.toString) ? _1e1.toString() : _1e1) !==
            ((this._lastValueReported && this._lastValueReported.toString) ?
             this._lastValueReported.toString() : this._lastValueReported)) {
            this._lastValueReported = _1e1;
            if (this._onChangeActive) {
                this.onChange(_1e1);
            }
        }
    },reset:function() {
        this._hasBeenBlurred = false;
        if (this.setValue && !this._getValueDeprecated) {
            this.setValue(this._resetValue, true);
        } else {
            if (this._onChangeMonitor) {
                this.setAttribute(this._onChangeMonitor,
                        (this._resetValue !== undefined && this._resetValue !== null) ?
                        this._resetValue : "");
            }
        }
    },create:function() {
        this.inherited(arguments);
        this._onChangeActive = true;
        this._setStateClass();
    },destroy:function() {
        if (this._layoutHackHandle) {
            clearTimeout(this._layoutHackHandle);
        }
        this.inherited(arguments);
    },setValue:function(_1e3) {
        dojo.deprecated("dijit.form._FormWidget:setValue(" + _1e3 +
                        ") is deprecated.  Use setAttribute('value'," + _1e3 +
                        ") instead.", "", "2.0");
        this.setAttribute("value", _1e3);
    },_getValueDeprecated:true,getValue:function() {
        dojo.deprecated("dijit.form._FormWidget:getValue() is deprecated.  Use widget.value instead.", "", "2.0");
        return this.value;
    },_layoutHack:function() {
        if (dojo.isFF == 2) {
            var node = this.domNode;
            var old = node.style.opacity;
            node.style.opacity = "0.999";
            this._layoutHackHandle = setTimeout(dojo.hitch(this, function() {
                this._layoutHackHandle = null;
                node.style.opacity = old;
            }), 0);
        }
    }});
    dojo.declare("dijit.form._FormValueWidget", dijit.form._FormWidget, {attributeMap:dojo.mixin(dojo.clone(dijit.form._FormWidget.prototype.attributeMap), {value:""}),postCreate:function() {
        this.setValue(this.value, null);
    },setValue:function(_1e6, _1e7) {
        this.value = _1e6;
        this._handleOnChange(_1e6, _1e7);
    },_getValueDeprecated:false,getValue:function() {
        return this._lastValue;
    },undo:function() {
        this.setValue(this._lastValueReported, false);
    },_valueChanged:function() {
        var v = this.getValue();
        var lv = this._lastValueReported;
        return ((v !== null && (v !== undefined) && v.toString) ? v.toString() : "") !==
               ((lv !== null && (lv !== undefined) && lv.toString) ? lv.toString() : "");
    },_onKeyPress:function(e) {
        if (e.keyCode == dojo.keys.ESCAPE && !e.shiftKey && !e.ctrlKey && !e.altKey) {
            if (this._valueChanged()) {
                this.undo();
                dojo.stopEvent(e);
                return false;
            }
        }
        return true;
    }});
}
if (!dojo._hasResource["dijit.dijit"]) {
    dojo._hasResource["dijit.dijit"] = true;
    dojo.provide("dijit.dijit");
}
