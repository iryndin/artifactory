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

if (!dojo._hasResource["dojo.colors"]) {
    dojo._hasResource["dojo.colors"] = true;
    dojo.provide("dojo.colors");
    (function() {
        var _1 = function(m1, m2, h) {
            if (h < 0) {
                ++h;
            }
            if (h > 1) {
                --h;
            }
            var h6 = 6 * h;
            if (h6 < 1) {
                return m1 + (m2 - m1) * h6;
            }
            if (2 * h < 1) {
                return m2;
            }
            if (3 * h < 2) {
                return m1 + (m2 - m1) * (2 / 3 - h) * 6;
            }
            return m1;
        };
        dojo.colorFromRgb = function(_6, _7) {
            var m = _6.toLowerCase().match(/^(rgba?|hsla?)\(([\s\.\-,%0-9]+)\)/);
            if (m) {
                var c = m[2].split(/\s*,\s*/),l = c.length,t = m[1];
                if ((t == "rgb" && l == 3) || (t == "rgba" && l == 4)) {
                    var r = c[0];
                    if (r.charAt(r.length - 1) == "%") {
                        var a = dojo.map(c, function(x) {
                            return parseFloat(x) * 2.56;
                        });
                        if (l == 4) {
                            a[3] = c[3];
                        }
                        return dojo.colorFromArray(a, _7);
                    }
                    return dojo.colorFromArray(c, _7);
                }
                if ((t == "hsl" && l == 3) || (t == "hsla" && l == 4)) {
                    var H = ((parseFloat(c[0]) % 360) + 360) % 360 / 360,S = parseFloat(c[1]) /
                                                                             100,L = parseFloat(c[2]) /
                                                                                     100,m2 =
                            L <= 0.5 ? L * (S + 1) : L + S - L * S,m1 = 2 * L -
                                                                        m2,a = [_1(m1, m2, H +
                                                                                           1 / 3) *
                                                                                256,_1(m1, m2, H) *
                                                                                    256,_1(m1, m2, H -
                                                                                                   1 /
                                                                                                   3) *
                                                                                        256,1];
                    if (l == 4) {
                        a[3] = c[3];
                    }
                    return dojo.colorFromArray(a, _7);
                }
            }
            return null;
        };
        var _14 = function(c, low, _17) {
            c = Number(c);
            return isNaN(c) ? _17 : c < low ? low : c > _17 ? _17 : c;
        };
        dojo.Color.prototype.sanitize = function() {
            var t = this;
            t.r = Math.round(_14(t.r, 0, 255));
            t.g = Math.round(_14(t.g, 0, 255));
            t.b = Math.round(_14(t.b, 0, 255));
            t.a = _14(t.a, 0, 1);
            return this;
        };
    })();
    dojo.colors.makeGrey = function(g, a) {
        return dojo.colorFromArray([g,g,g,a]);
    };
    dojo.Color.named =
    dojo.mixin({aliceblue:[240,248,255],antiquewhite:[250,235,215],aquamarine:[127,255,212],azure:[240,255,255],beige:[245,245,220],bisque:[255,228,196],blanchedalmond:[255,235,205],blueviolet:[138,43,226],brown:[165,42,42],burlywood:[222,184,135],cadetblue:[95,158,160],chartreuse:[127,255,0],chocolate:[210,105,30],coral:[255,127,80],cornflowerblue:[100,149,237],cornsilk:[255,248,220],crimson:[220,20,60],cyan:[0,255,255],darkblue:[0,0,139],darkcyan:[0,139,139],darkgoldenrod:[184,134,11],darkgray:[169,169,169],darkgreen:[0,100,0],darkgrey:[169,169,169],darkkhaki:[189,183,107],darkmagenta:[139,0,139],darkolivegreen:[85,107,47],darkorange:[255,140,0],darkorchid:[153,50,204],darkred:[139,0,0],darksalmon:[233,150,122],darkseagreen:[143,188,143],darkslateblue:[72,61,139],darkslategray:[47,79,79],darkslategrey:[47,79,79],darkturquoise:[0,206,209],darkviolet:[148,0,211],deeppink:[255,20,147],deepskyblue:[0,191,255],dimgray:[105,105,105],dimgrey:[105,105,105],dodgerblue:[30,144,255],firebrick:[178,34,34],floralwhite:[255,250,240],forestgreen:[34,139,34],gainsboro:[220,220,220],ghostwhite:[248,248,255],gold:[255,215,0],goldenrod:[218,165,32],greenyellow:[173,255,47],grey:[128,128,128],honeydew:[240,255,240],hotpink:[255,105,180],indianred:[205,92,92],indigo:[75,0,130],ivory:[255,255,240],khaki:[240,230,140],lavender:[230,230,250],lavenderblush:[255,240,245],lawngreen:[124,252,0],lemonchiffon:[255,250,205],lightblue:[173,216,230],lightcoral:[240,128,128],lightcyan:[224,255,255],lightgoldenrodyellow:[250,250,210],lightgray:[211,211,211],lightgreen:[144,238,144],lightgrey:[211,211,211],lightpink:[255,182,193],lightsalmon:[255,160,122],lightseagreen:[32,178,170],lightskyblue:[135,206,250],lightslategray:[119,136,153],lightslategrey:[119,136,153],lightsteelblue:[176,196,222],lightyellow:[255,255,224],limegreen:[50,205,50],linen:[250,240,230],magenta:[255,0,255],mediumaquamarine:[102,205,170],mediumblue:[0,0,205],mediumorchid:[186,85,211],mediumpurple:[147,112,219],mediumseagreen:[60,179,113],mediumslateblue:[123,104,238],mediumspringgreen:[0,250,154],mediumturquoise:[72,209,204],mediumvioletred:[199,21,133],midnightblue:[25,25,112],mintcream:[245,255,250],mistyrose:[255,228,225],moccasin:[255,228,181],navajowhite:[255,222,173],oldlace:[253,245,230],olivedrab:[107,142,35],orange:[255,165,0],orangered:[255,69,0],orchid:[218,112,214],palegoldenrod:[238,232,170],palegreen:[152,251,152],paleturquoise:[175,238,238],palevioletred:[219,112,147],papayawhip:[255,239,213],peachpuff:[255,218,185],peru:[205,133,63],pink:[255,192,203],plum:[221,160,221],powderblue:[176,224,230],rosybrown:[188,143,143],royalblue:[65,105,225],saddlebrown:[139,69,19],salmon:[250,128,114],sandybrown:[244,164,96],seagreen:[46,139,87],seashell:[255,245,238],sienna:[160,82,45],skyblue:[135,206,235],slateblue:[106,90,205],slategray:[112,128,144],slategrey:[112,128,144],snow:[255,250,250],springgreen:[0,255,127],steelblue:[70,130,180],tan:[210,180,140],thistle:[216,191,216],tomato:[255,99,71],transparent:[0,0,0,0],turquoise:[64,224,208],violet:[238,130,238],wheat:[245,222,179],whitesmoke:[245,245,245],yellowgreen:[154,205,50]}, dojo.Color.named);
}
if (!dojo._hasResource["dojo.i18n"]) {
    dojo._hasResource["dojo.i18n"] = true;
    dojo.provide("dojo.i18n");
    dojo.i18n.getLocalization = function(_1b, _1c, _1d) {
        _1d = dojo.i18n.normalizeLocale(_1d);
        var _1e = _1d.split("-");
        var _1f = [_1b,"nls",_1c].join(".");
        var _20 = dojo._loadedModules[_1f];
        if (_20) {
            var _21;
            for (var i = _1e.length; i > 0; i--) {
                var loc = _1e.slice(0, i).join("_");
                if (_20[loc]) {
                    _21 = _20[loc];
                    break;
                }
            }
            if (!_21) {
                _21 = _20.ROOT;
            }
            if (_21) {
                var _24 = function() {
                };
                _24.prototype = _21;
                return new _24();
            }
        }
        throw new Error("Bundle not found: " + _1c + " in " + _1b + " , locale=" + _1d);
    };
    dojo.i18n.normalizeLocale = function(_25) {
        var _26 = _25 ? _25.toLowerCase() : dojo.locale;
        if (_26 == "root") {
            _26 = "ROOT";
        }
        return _26;
    };
    dojo.i18n._requireLocalization = function(_27, _28, _29, _2a) {
        var _2b = dojo.i18n.normalizeLocale(_29);
        var _2c = [_27,"nls",_28].join(".");
        var _2d = "";
        if (_2a) {
            var _2e = _2a.split(",");
            for (var i = 0; i < _2e.length; i++) {
                if (_2b.indexOf(_2e[i]) == 0) {
                    if (_2e[i].length > _2d.length) {
                        _2d = _2e[i];
                    }
                }
            }
            if (!_2d) {
                _2d = "ROOT";
            }
        }
        var _30 = _2a ? _2d : _2b;
        var _31 = dojo._loadedModules[_2c];
        var _32 = null;
        if (_31) {
            if (dojo.config.localizationComplete && _31._built) {
                return;
            }
            var _33 = _30.replace(/-/g, "_");
            var _34 = _2c + "." + _33;
            _32 = dojo._loadedModules[_34];
        }
        if (!_32) {
            _31 = dojo["provide"](_2c);
            var _35 = dojo._getModuleSymbols(_27);
            var _36 = _35.concat("nls").join("/");
            var _37;
            dojo.i18n._searchLocalePath(_30, _2a, function(loc) {
                var _39 = loc.replace(/-/g, "_");
                var _3a = _2c + "." + _39;
                var _3b = false;
                if (!dojo._loadedModules[_3a]) {
                    dojo["provide"](_3a);
                    var _3c = [_36];
                    if (loc != "ROOT") {
                        _3c.push(loc);
                    }
                    _3c.push(_28);
                    var _3d = _3c.join("/") + ".js";
                    _3b = dojo._loadPath(_3d, null, function(_3e) {
                        var _3f = function() {
                        };
                        _3f.prototype = _37;
                        _31[_39] = new _3f();
                        for (var j in _3e) {
                            _31[_39][j] = _3e[j];
                        }
                    });
                } else {
                    _3b = true;
                }
                if (_3b && _31[_39]) {
                    _37 = _31[_39];
                } else {
                    _31[_39] = _37;
                }
                if (_2a) {
                    return true;
                }
            });
        }
        if (_2a && _2b != _2d) {
            _31[_2b.replace(/-/g, "_")] = _31[_2d.replace(/-/g, "_")];
        }
    };
    (function() {
        var _41 = dojo.config.extraLocale;
        if (_41) {
            if (!_41 instanceof Array) {
                _41 = [_41];
            }
            var req = dojo.i18n._requireLocalization;
            dojo.i18n._requireLocalization = function(m, b, _45, _46) {
                req(m, b, _45, _46);
                if (_45) {
                    return;
                }
                for (var i = 0; i < _41.length; i++) {
                    req(m, b, _41[i], _46);
                }
            };
        }
    })();
    dojo.i18n._searchLocalePath = function(_48, _49, _4a) {
        _48 = dojo.i18n.normalizeLocale(_48);
        var _4b = _48.split("-");
        var _4c = [];
        for (var i = _4b.length; i > 0; i--) {
            _4c.push(_4b.slice(0, i).join("-"));
        }
        _4c.push(false);
        if (_49) {
            _4c.reverse();
        }
        for (var j = _4c.length - 1; j >= 0; j--) {
            var loc = _4c[j] || "ROOT";
            var _50 = _4a(loc);
            if (_50) {
                break;
            }
        }
    };
    dojo.i18n._preloadLocalizations = function(_51, _52) {
        function preload(_53) {
            _53 = dojo.i18n.normalizeLocale(_53);
            dojo.i18n._searchLocalePath(_53, true, function(loc) {
                for (var i = 0; i < _52.length; i++) {
                    if (_52[i] == loc) {
                        dojo["require"](_51 + "_" + loc);
                        return true;
                    }
                }
                return false;
            });
        }
        ;
        preload();
        var _56 = dojo.config.extraLocale || [];
        for (var i = 0; i < _56.length; i++) {
            preload(_56[i]);
        }
    };
}
if (!dojo._hasResource["dijit.ColorPalette"]) {
    dojo._hasResource["dijit.ColorPalette"] = true;
    dojo.provide("dijit.ColorPalette");
    dojo.declare("dijit.ColorPalette", [dijit._Widget,dijit._Templated], {defaultTimeout:500,timeoutChangeRate:0.9,palette:"7x10",value:null,_currentFocus:0,_xDim:null,_yDim:null,_palettes:{"7x10":[["white","seashell","cornsilk","lemonchiffon","lightyellow","palegreen","paleturquoise","lightcyan","lavender","plum"],["lightgray","pink","bisque","moccasin","khaki","lightgreen","lightseagreen","lightskyblue","cornflowerblue","violet"],["silver","lightcoral","sandybrown","orange","palegoldenrod","chartreuse","mediumturquoise","skyblue","mediumslateblue","orchid"],["gray","red","orangered","darkorange","yellow","limegreen","darkseagreen","royalblue","slateblue","mediumorchid"],["dimgray","crimson","chocolate","coral","gold","forestgreen","seagreen","blue","blueviolet","darkorchid"],["darkslategray","firebrick","saddlebrown","sienna","olive","green","darkcyan","mediumblue","darkslateblue","darkmagenta"],["black","darkred","maroon","brown","darkolivegreen","darkgreen","midnightblue","navy","indigo","purple"]],"3x4":[["white","lime","green","blue"],["silver","yellow","fuchsia","navy"],["gray","red","purple","black"]]},_imagePaths:{"7x10":dojo.moduleUrl("dijit", "templates/colors7x10.png"),"3x4":dojo.moduleUrl("dijit", "templates/colors3x4.png")},_paletteCoords:{"leftOffset":3,"topOffset":3,"cWidth":20,"cHeight":20},templateString:"<div class=\"dijitInline dijitColorPalette\">\n\t<div class=\"dijitColorPaletteInner\" dojoAttachPoint=\"divNode\" waiRole=\"grid\" tabIndex=\"${tabIndex}\">\n\t\t<img class=\"dijitColorPaletteUnder\" dojoAttachPoint=\"imageNode\" waiRole=\"presentation\">\n\t</div>\t\n</div>\n",_paletteDims:{"7x10":{"width":"206px","height":"145px"},"3x4":{"width":"86px","height":"64px"}},tabIndex:"0",postCreate:function() {
        dojo.mixin(this.divNode.style, this._paletteDims[this.palette]);
        this.imageNode.setAttribute("src", this._imagePaths[this.palette]);
        var _58 = this._palettes[this.palette];
        this.domNode.style.position = "relative";
        this._cellNodes = [];
        this.colorNames = dojo.i18n.getLocalization("dojo", "colors", this.lang);
        var url = dojo.moduleUrl("dojo", "resources/blank.gif"),_5a = new dojo.Color(),_5b = this._paletteCoords;
        for (var row = 0; row < _58.length; row++) {
            for (var col = 0; col < _58[row].length; col++) {
                var _5e = dojo.doc.createElement("img");
                _5e.src = url;
                dojo.addClass(_5e, "dijitPaletteImg");
                var _5f = _58[row][col],_60 = _5a.setColor(dojo.Color.named[_5f]);
                _5e.alt = this.colorNames[_5f];
                _5e.color = _60.toHex();
                var _61 = _5e.style;
                _61.color = _61.backgroundColor = _5e.color;
                var _62 = dojo.doc.createElement("span");
                _62.appendChild(_5e);
                dojo.forEach(["Dijitclick","MouseEnter","Focus","Blur"], function(_63) {
                    this.connect(_62, "on" + _63.toLowerCase(), "_onCell" + _63);
                }, this);
                this.divNode.appendChild(_62);
                var _64 = _62.style;
                _64.top = _5b.topOffset + (row * _5b.cHeight) + "px";
                _64.left = _5b.leftOffset + (col * _5b.cWidth) + "px";
                dojo.attr(_62, "tabindex", "-1");
                _62.title = this.colorNames[_5f];
                dojo.addClass(_62, "dijitPaletteCell");
                dijit.setWaiRole(_62, "gridcell");
                _62.index = this._cellNodes.length;
                this._cellNodes.push(_62);
            }
        }
        this._xDim = _58[0].length;
        this._yDim = _58.length;
        this.connect(this.divNode, "onfocus", "_onDivNodeFocus");
        var _65 = {UP_ARROW:-this._xDim,DOWN_ARROW:this._xDim,RIGHT_ARROW:1,LEFT_ARROW:-1};
        for (var key in _65) {
            this._connects.push(dijit.typematic.addKeyListener(this.domNode, {keyCode:dojo.keys[key],ctrlKey:false,altKey:false,shiftKey:false}, this, function() {
                var _67 = _65[key];
                return function(_68) {
                    this._navigateByKey(_67, _68);
                };
            }(), this.timeoutChangeRate, this.defaultTimeout));
        }
    },focus:function() {
        this._focusFirst();
    },onChange:function(_69) {
    },_focusFirst:function() {
        this._currentFocus = 0;
        var _6a = this._cellNodes[this._currentFocus];
        window.setTimeout(function() {
            dijit.focus(_6a);
        }, 0);
    },_onDivNodeFocus:function(evt) {
        if (evt.target === this.divNode) {
            this._focusFirst();
        }
    },_onFocus:function() {
        dojo.attr(this.divNode, "tabindex", "-1");
    },_onBlur:function() {
        this._removeCellHighlight(this._currentFocus);
        dojo.attr(this.divNode, "tabindex", this.tabIndex);
    },_onCellDijitclick:function(evt) {
        var _6d = evt.currentTarget;
        if (this._currentFocus != _6d.index) {
            this._currentFocus = _6d.index;
            window.setTimeout(function() {
                dijit.focus(_6d);
            }, 0);
        }
        this._selectColor(_6d);
        dojo.stopEvent(evt);
    },_onCellMouseEnter:function(evt) {
        var _6f = evt.currentTarget;
        window.setTimeout(function() {
            dijit.focus(_6f);
        }, 0);
    },_onCellFocus:function(evt) {
        this._removeCellHighlight(this._currentFocus);
        this._currentFocus = evt.currentTarget.index;
        dojo.addClass(evt.currentTarget, "dijitPaletteCellHighlight");
    },_onCellBlur:function(evt) {
        this._removeCellHighlight(this._currentFocus);
    },_removeCellHighlight:function(_72) {
        dojo.removeClass(this._cellNodes[_72], "dijitPaletteCellHighlight");
    },_selectColor:function(_73) {
        var img = _73.getElementsByTagName("img")[0];
        this.onChange(this.value = img.color);
    },_navigateByKey:function(_75, _76) {
        if (_76 == -1) {
            return;
        }
        var _77 = this._currentFocus + _75;
        if (_77 < this._cellNodes.length && _77 > -1) {
            var _78 = this._cellNodes[_77];
            _78.focus();
        }
    }});
}
if (!dojo._hasResource["dijit.Declaration"]) {
    dojo._hasResource["dijit.Declaration"] = true;
    dojo.provide("dijit.Declaration");
    dojo.declare("dijit.Declaration", dijit._Widget, {_noScript:true,widgetClass:"",replaceVars:true,defaults:null,mixins:[],buildRendering:function() {
        var src = this.srcNodeRef.parentNode.removeChild(this.srcNodeRef);
        var _7a = dojo.query("> script[type='dojo/method'][event='preamble']", src).orphan();
        var _7b = dojo.query("> script[type^='dojo/']", src).orphan();
        var _7c = src.nodeName;
        var _7d = this.defaults || {};
        this.mixins = this.mixins.length ? dojo.map(this.mixins, function(_7e) {
            return dojo.getObject(_7e);
        }) : [dijit._Widget,dijit._Templated];
        if (_7a.length) {
            _7d.preamble = dojo.parser._functionFromScript(_7a[0]);
        }
        var _7f = dojo.map(_7b, function(s) {
            var evt = s.getAttribute("event") || "postscript";
            return {event:evt,func:dojo.parser._functionFromScript(s)};
        });
        this.mixins.push(function() {
            dojo.forEach(_7f, function(s) {
                dojo.connect(this, s.event, this, s.func);
            }, this);
        });
        _7d.widgetsInTemplate = true;
        _7d._skipNodeCache = true;
        _7d.templateString = "<" + _7c + " class='" + src.className + "' dojoAttachPoint='" +
                             (src.getAttribute("dojoAttachPoint") || "") + "' dojoAttachEvent='" +
                             (src.getAttribute("dojoAttachEvent") || "") + "' >" +
                             src.innerHTML.replace(/\%7B/g, "{").replace(/\%7D/g, "}") + "</" +
                             _7c + ">";
        dojo.query("[dojoType]", src).forEach(function(_83) {
            _83.removeAttribute("dojoType");
        });
        dojo.declare(this.widgetClass, this.mixins, _7d);
    }});
}
if (!dojo._hasResource["dojo.dnd.common"]) {
    dojo._hasResource["dojo.dnd.common"] = true;
    dojo.provide("dojo.dnd.common");
    dojo.dnd._copyKey = navigator.appVersion.indexOf("Macintosh") < 0 ? "ctrlKey" : "metaKey";
    dojo.dnd.getCopyKeyState = function(e) {
        return e[dojo.dnd._copyKey];
    };
    dojo.dnd._uniqueId = 0;
    dojo.dnd.getUniqueId = function() {
        var id;
        do{
            id = dojo._scopeName + "Unique" + (++dojo.dnd._uniqueId);
        } while (dojo.byId(id));
        return id;
    };
    dojo.dnd._empty = {};
    dojo.dnd.isFormElement = function(e) {
        var t = e.target;
        if (t.nodeType == 3) {
            t = t.parentNode;
        }
        return " button textarea input select option ".indexOf(" " + t.tagName.toLowerCase() +
                                                               " ") >= 0;
    };
}
if (!dojo._hasResource["dojo.dnd.autoscroll"]) {
    dojo._hasResource["dojo.dnd.autoscroll"] = true;
    dojo.provide("dojo.dnd.autoscroll");
    dojo.dnd.getViewport = function() {
        var d = dojo.doc,dd = d.documentElement,w = window,b = dojo.body();
        if (dojo.isMozilla) {
            return {w:dd.clientWidth,h:w.innerHeight};
        } else {
            if (!dojo.isOpera && w.innerWidth) {
                return {w:w.innerWidth,h:w.innerHeight};
            } else {
                if (!dojo.isOpera && dd && dd.clientWidth) {
                    return {w:dd.clientWidth,h:dd.clientHeight};
                } else {
                    if (b.clientWidth) {
                        return {w:b.clientWidth,h:b.clientHeight};
                    }
                }
            }
        }
        return null;
    };
    dojo.dnd.V_TRIGGER_AUTOSCROLL = 32;
    dojo.dnd.H_TRIGGER_AUTOSCROLL = 32;
    dojo.dnd.V_AUTOSCROLL_VALUE = 16;
    dojo.dnd.H_AUTOSCROLL_VALUE = 16;
    dojo.dnd.autoScroll = function(e) {
        var v = dojo.dnd.getViewport(),dx = 0,dy = 0;
        if (e.clientX < dojo.dnd.H_TRIGGER_AUTOSCROLL) {
            dx = -dojo.dnd.H_AUTOSCROLL_VALUE;
        } else {
            if (e.clientX > v.w - dojo.dnd.H_TRIGGER_AUTOSCROLL) {
                dx = dojo.dnd.H_AUTOSCROLL_VALUE;
            }
        }
        if (e.clientY < dojo.dnd.V_TRIGGER_AUTOSCROLL) {
            dy = -dojo.dnd.V_AUTOSCROLL_VALUE;
        } else {
            if (e.clientY > v.h - dojo.dnd.V_TRIGGER_AUTOSCROLL) {
                dy = dojo.dnd.V_AUTOSCROLL_VALUE;
            }
        }
        window.scrollBy(dx, dy);
    };
    dojo.dnd._validNodes = {"div":1,"p":1,"td":1};
    dojo.dnd._validOverflow = {"auto":1,"scroll":1};
    dojo.dnd.autoScrollNodes = function(e) {
        for (var n = e.target; n;) {
            if (n.nodeType == 1 && (n.tagName.toLowerCase() in dojo.dnd._validNodes)) {
                var s = dojo.getComputedStyle(n);
                if (s.overflow.toLowerCase() in dojo.dnd._validOverflow) {
                    var b = dojo._getContentBox(n, s),t = dojo._abs(n, true);
                    b.l += t.x + n.scrollLeft;
                    b.t += t.y + n.scrollTop;
                    var w = Math.min(dojo.dnd.H_TRIGGER_AUTOSCROLL, b.w /
                                                                    2),h = Math.min(dojo.dnd.V_TRIGGER_AUTOSCROLL, b.h /
                                                                                                                   2),rx = e.pageX -
                                                                                                                           b.l,ry = e.pageY -
                                                                                                                                    b.t,dx = 0,dy = 0;
                    if (rx > 0 && rx < b.w) {
                        if (rx < w) {
                            dx = -dojo.dnd.H_AUTOSCROLL_VALUE;
                        } else {
                            if (rx > b.w - w) {
                                dx = dojo.dnd.H_AUTOSCROLL_VALUE;
                            }
                        }
                    }
                    if (ry > 0 && ry < b.h) {
                        if (ry < h) {
                            dy = -dojo.dnd.V_AUTOSCROLL_VALUE;
                        } else {
                            if (ry > b.h - h) {
                                dy = dojo.dnd.V_AUTOSCROLL_VALUE;
                            }
                        }
                    }
                    var _9b = n.scrollLeft,_9c = n.scrollTop;
                    n.scrollLeft = n.scrollLeft + dx;
                    n.scrollTop = n.scrollTop + dy;
                    if (_9b != n.scrollLeft || _9c != n.scrollTop) {
                        return;
                    }
                }
            }
            try {
                n = n.parentNode;
            } catch(x) {
                n = null;
            }
        }
        dojo.dnd.autoScroll(e);
    };
}
if (!dojo._hasResource["dojo.dnd.Mover"]) {
    dojo._hasResource["dojo.dnd.Mover"] = true;
    dojo.provide("dojo.dnd.Mover");
    dojo.declare("dojo.dnd.Mover", null, {constructor:function(_9d, e, _9f) {
        this.node = dojo.byId(_9d);
        this.marginBox = {l:e.pageX,t:e.pageY};
        this.mouseButton = e.button;
        var h = this.host =
                _9f,d = _9d.ownerDocument,_a2 = dojo.connect(d, "onmousemove", this, "onFirstMove");
        this.events =
        [dojo.connect(d, "onmousemove", this, "onMouseMove"),dojo.connect(d, "onmouseup", this, "onMouseUp"),dojo.connect(d, "ondragstart", dojo, "stopEvent"),dojo.connect(d, "onselectstart", dojo, "stopEvent"),_a2];
        if (h && h.onMoveStart) {
            h.onMoveStart(this);
        }
    },onMouseMove:function(e) {
        dojo.dnd.autoScroll(e);
        var m = this.marginBox;
        this.host.onMove(this, {l:m.l + e.pageX,t:m.t + e.pageY});
    },onMouseUp:function(e) {
        if (this.mouseButton == e.button) {
            this.destroy();
        }
    },onFirstMove:function() {
        var s = this.node.style,l,t;
        switch (s.position) {case "relative":case "absolute":l = Math.round(parseFloat(s.left));t =
                                                                                                Math.round(parseFloat(s.top));break;default:s.position =
                                                                                                                                            "absolute";var m = dojo.marginBox(this.node);l =
                                                                                                                                                                                         m.l;t =
                                                                                                                                                                                             m.t;break;}
        this.marginBox.l = l - this.marginBox.l;
        this.marginBox.t = t - this.marginBox.t;
        this.host.onFirstMove(this);
        dojo.disconnect(this.events.pop());
    },destroy:function() {
        dojo.forEach(this.events, dojo.disconnect);
        var h = this.host;
        if (h && h.onMoveStop) {
            h.onMoveStop(this);
        }
        this.events = this.node = null;
    }});
}
if (!dojo._hasResource["dojo.dnd.Moveable"]) {
    dojo._hasResource["dojo.dnd.Moveable"] = true;
    dojo.provide("dojo.dnd.Moveable");
    dojo.declare("dojo.dnd.Moveable", null, {handle:"",delay:0,skip:false,constructor:function(_ab,
                                                                                               _ac) {
        this.node = dojo.byId(_ab);
        if (!_ac) {
            _ac = {};
        }
        this.handle = _ac.handle ? dojo.byId(_ac.handle) : null;
        if (!this.handle) {
            this.handle = this.node;
        }
        this.delay = _ac.delay > 0 ? _ac.delay : 0;
        this.skip = _ac.skip;
        this.mover = _ac.mover ? _ac.mover : dojo.dnd.Mover;
        this.events =
        [dojo.connect(this.handle, "onmousedown", this, "onMouseDown"),dojo.connect(this.handle, "ondragstart", this, "onSelectStart"),dojo.connect(this.handle, "onselectstart", this, "onSelectStart")];
    },markupFactory:function(_ad, _ae) {
        return new dojo.dnd.Moveable(_ae, _ad);
    },destroy:function() {
        dojo.forEach(this.events, dojo.disconnect);
        this.events = this.node = this.handle = null;
    },onMouseDown:function(e) {
        if (this.skip && dojo.dnd.isFormElement(e)) {
            return;
        }
        if (this.delay) {
            this.events.push(dojo.connect(this.handle, "onmousemove", this, "onMouseMove"));
            this.events.push(dojo.connect(this.handle, "onmouseup", this, "onMouseUp"));
            this._lastX = e.pageX;
            this._lastY = e.pageY;
        } else {
            new this.mover(this.node, e, this);
        }
        dojo.stopEvent(e);
    },onMouseMove:function(e) {
        if (Math.abs(e.pageX - this._lastX) > this.delay ||
            Math.abs(e.pageY - this._lastY) > this.delay) {
            this.onMouseUp(e);
            new this.mover(this.node, e, this);
        }
        dojo.stopEvent(e);
    },onMouseUp:function(e) {
        dojo.disconnect(this.events.pop());
        dojo.disconnect(this.events.pop());
    },onSelectStart:function(e) {
        if (!this.skip || !dojo.dnd.isFormElement(e)) {
            dojo.stopEvent(e);
        }
    },onMoveStart:function(_b3) {
        dojo.publish("/dnd/move/start", [_b3]);
        dojo.addClass(dojo.body(), "dojoMove");
        dojo.addClass(this.node, "dojoMoveItem");
    },onMoveStop:function(_b4) {
        dojo.publish("/dnd/move/stop", [_b4]);
        dojo.removeClass(dojo.body(), "dojoMove");
        dojo.removeClass(this.node, "dojoMoveItem");
    },onFirstMove:function(_b5) {
    },onMove:function(_b6, _b7) {
        this.onMoving(_b6, _b7);
        var s = _b6.node.style;
        s.left = _b7.l + "px";
        s.top = _b7.t + "px";
        this.onMoved(_b6, _b7);
    },onMoving:function(_b9, _ba) {
    },onMoved:function(_bb, _bc) {
    }});
}
if (!dojo._hasResource["dojo.dnd.TimedMoveable"]) {
    dojo._hasResource["dojo.dnd.TimedMoveable"] = true;
    dojo.provide("dojo.dnd.TimedMoveable");
    (function() {
        var _bd = dojo.dnd.Moveable.prototype.onMove;
        dojo.declare("dojo.dnd.TimedMoveable", dojo.dnd.Moveable, {timeout:40,constructor:function(
                _be, _bf) {
            if (!_bf) {
                _bf = {};
            }
            if (_bf.timeout && typeof _bf.timeout == "number" && _bf.timeout >= 0) {
                this.timeout = _bf.timeout;
            }
        },markupFactory:function(_c0, _c1) {
            return new dojo.dnd.TimedMoveable(_c1, _c0);
        },onMoveStop:function(_c2) {
            if (_c2._timer) {
                clearTimeout(_c2._timer);
                _bd.call(this, _c2, _c2._leftTop);
            }
            dojo.dnd.Moveable.prototype.onMoveStop.apply(this, arguments);
        },onMove:function(_c3, _c4) {
            _c3._leftTop = _c4;
            if (!_c3._timer) {
                var _t = this;
                _c3._timer = setTimeout(function() {
                    _c3._timer = null;
                    _bd.call(_t, _c3, _c3._leftTop);
                }, this.timeout);
            }
        }});
    })();
}
if (!dojo._hasResource["dojo.fx"]) {
    dojo._hasResource["dojo.fx"] = true;
    dojo.provide("dojo.fx");
    dojo.provide("dojo.fx.Toggler");
    (function() {
        var _c6 = {_fire:function(evt, _c8) {
            if (this[evt]) {
                this[evt].apply(this, _c8 || []);
            }
            return this;
        }};
        var _c9 = function(_ca) {
            this._index = -1;
            this._animations = _ca || [];
            this._current = this._onAnimateCtx = this._onEndCtx = null;
            this.duration = 0;
            dojo.forEach(this._animations, function(a) {
                this.duration += a.duration;
                if (a.delay) {
                    this.duration += a.delay;
                }
            }, this);
        };
        dojo.extend(_c9, {_onAnimate:function() {
            this._fire("onAnimate", arguments);
        },_onEnd:function() {
            dojo.disconnect(this._onAnimateCtx);
            dojo.disconnect(this._onEndCtx);
            this._onAnimateCtx = this._onEndCtx = null;
            if (this._index + 1 == this._animations.length) {
                this._fire("onEnd");
            } else {
                this._current = this._animations[++this._index];
                this._onAnimateCtx = dojo.connect(this._current, "onAnimate", this, "_onAnimate");
                this._onEndCtx = dojo.connect(this._current, "onEnd", this, "_onEnd");
                this._current.play(0, true);
            }
        },play:function(_cc, _cd) {
            if (!this._current) {
                this._current = this._animations[this._index = 0];
            }
            if (!_cd && this._current.status() == "playing") {
                return this;
            }
            var _ce = dojo.connect(this._current, "beforeBegin", this, function() {
                this._fire("beforeBegin");
            }),_cf = dojo.connect(this._current, "onBegin", this, function(arg) {
                this._fire("onBegin", arguments);
            }),_d1 = dojo.connect(this._current, "onPlay", this, function(arg) {
                this._fire("onPlay", arguments);
                dojo.disconnect(_ce);
                dojo.disconnect(_cf);
                dojo.disconnect(_d1);
            });
            if (this._onAnimateCtx) {
                dojo.disconnect(this._onAnimateCtx);
            }
            this._onAnimateCtx = dojo.connect(this._current, "onAnimate", this, "_onAnimate");
            if (this._onEndCtx) {
                dojo.disconnect(this._onEndCtx);
            }
            this._onEndCtx = dojo.connect(this._current, "onEnd", this, "_onEnd");
            this._current.play.apply(this._current, arguments);
            return this;
        },pause:function() {
            if (this._current) {
                var e = dojo.connect(this._current, "onPause", this, function(arg) {
                    this._fire("onPause", arguments);
                    dojo.disconnect(e);
                });
                this._current.pause();
            }
            return this;
        },gotoPercent:function(_d5, _d6) {
            this.pause();
            var _d7 = this.duration * _d5;
            this._current = null;
            dojo.some(this._animations, function(a) {
                if (a.duration <= _d7) {
                    this._current = a;
                    return true;
                }
                _d7 -= a.duration;
                return false;
            });
            if (this._current) {
                this._current.gotoPercent(_d7 / _current.duration, _d6);
            }
            return this;
        },stop:function(_d9) {
            if (this._current) {
                if (_d9) {
                    for (; this._index + 1 < this._animations.length; ++this._index) {
                        this._animations[this._index].stop(true);
                    }
                    this._current = this._animations[this._index];
                }
                var e = dojo.connect(this._current, "onStop", this, function(arg) {
                    this._fire("onStop", arguments);
                    dojo.disconnect(e);
                });
                this._current.stop();
            }
            return this;
        },status:function() {
            return this._current ? this._current.status() : "stopped";
        },destroy:function() {
            if (this._onAnimateCtx) {
                dojo.disconnect(this._onAnimateCtx);
            }
            if (this._onEndCtx) {
                dojo.disconnect(this._onEndCtx);
            }
        }});
        dojo.extend(_c9, _c6);
        dojo.fx.chain = function(_dc) {
            return new _c9(_dc);
        };
        var _dd = function(_de) {
            this._animations = _de || [];
            this._connects = [];
            this._finished = 0;
            this.duration = 0;
            dojo.forEach(_de, function(a) {
                var _e0 = a.duration;
                if (a.delay) {
                    _e0 += a.delay;
                }
                if (this.duration < _e0) {
                    this.duration = _e0;
                }
                this._connects.push(dojo.connect(a, "onEnd", this, "_onEnd"));
            }, this);
            this._pseudoAnimation = new dojo._Animation({curve:[0,1],duration:this.duration});
            dojo.forEach(["beforeBegin","onBegin","onPlay","onAnimate","onPause","onStop"], function(
                    evt) {
                this._connects.push(dojo.connect(this._pseudoAnimation, evt, dojo.hitch(this, "_fire", evt)));
            }, this);
        };
        dojo.extend(_dd, {_doAction:function(_e2, _e3) {
            dojo.forEach(this._animations, function(a) {
                a[_e2].apply(a, _e3);
            });
            return this;
        },_onEnd:function() {
            if (++this._finished == this._animations.length) {
                this._fire("onEnd");
            }
        },_call:function(_e5, _e6) {
            var t = this._pseudoAnimation;
            t[_e5].apply(t, _e6);
        },play:function(_e8, _e9) {
            this._finished = 0;
            this._doAction("play", arguments);
            this._call("play", arguments);
            return this;
        },pause:function() {
            this._doAction("pause", arguments);
            this._call("pause", arguments);
            return this;
        },gotoPercent:function(_ea, _eb) {
            var ms = this.duration * _ea;
            dojo.forEach(this._animations, function(a) {
                a.gotoPercent(a.duration < ms ? 1 : (ms / a.duration), _eb);
            });
            this._call("gotoProcent", arguments);
            return this;
        },stop:function(_ee) {
            this._doAction("stop", arguments);
            this._call("stop", arguments);
            return this;
        },status:function() {
            return this._pseudoAnimation.status();
        },destroy:function() {
            dojo.forEach(this._connects, dojo.disconnect);
        }});
        dojo.extend(_dd, _c6);
        dojo.fx.combine = function(_ef) {
            return new _dd(_ef);
        };
    })();
    dojo.declare("dojo.fx.Toggler", null, {constructor:function(_f0) {
        var _t = this;
        dojo.mixin(_t, _f0);
        _t.node = _f0.node;
        _t._showArgs = dojo.mixin({}, _f0);
        _t._showArgs.node = _t.node;
        _t._showArgs.duration = _t.showDuration;
        _t.showAnim = _t.showFunc(_t._showArgs);
        _t._hideArgs = dojo.mixin({}, _f0);
        _t._hideArgs.node = _t.node;
        _t._hideArgs.duration = _t.hideDuration;
        _t.hideAnim = _t.hideFunc(_t._hideArgs);
        dojo.connect(_t.showAnim, "beforeBegin", dojo.hitch(_t.hideAnim, "stop", true));
        dojo.connect(_t.hideAnim, "beforeBegin", dojo.hitch(_t.showAnim, "stop", true));
    },node:null,showFunc:dojo.fadeIn,hideFunc:dojo.fadeOut,showDuration:200,hideDuration:200,show:function(
            _f2) {
        return this.showAnim.play(_f2 || 0);
    },hide:function(_f3) {
        return this.hideAnim.play(_f3 || 0);
    }});
    dojo.fx.wipeIn = function(_f4) {
        _f4.node = dojo.byId(_f4.node);
        var _f5 = _f4.node,s = _f5.style;
        var _f7 = dojo.animateProperty(dojo.mixin({properties:{height:{start:function() {
            s.overflow = "hidden";
            if (s.visibility == "hidden" || s.display == "none") {
                s.height = "1px";
                s.display = "";
                s.visibility = "";
                return 1;
            } else {
                var _f8 = dojo.style(_f5, "height");
                return Math.max(_f8, 1);
            }
        },end:function() {
            return _f5.scrollHeight;
        }}}}, _f4));
        dojo.connect(_f7, "onEnd", function() {
            s.height = "auto";
        });
        return _f7;
    };
    dojo.fx.wipeOut = function(_f9) {
        var _fa = _f9.node = dojo.byId(_f9.node);
        var s = _fa.style;
        var _fc = dojo.animateProperty(dojo.mixin({properties:{height:{end:1}}}, _f9));
        dojo.connect(_fc, "beforeBegin", function() {
            s.overflow = "hidden";
            s.display = "";
        });
        dojo.connect(_fc, "onEnd", function() {
            s.height = "auto";
            s.display = "none";
        });
        return _fc;
    };
    dojo.fx.slideTo = function(_fd) {
        var _fe = (_fd.node = dojo.byId(_fd.node));
        var top = null;
        var left = null;
        var init = (function(n) {
            return function() {
                var cs = dojo.getComputedStyle(n);
                var pos = cs.position;
                top = (pos == "absolute" ? n.offsetTop : parseInt(cs.top) || 0);
                left = (pos == "absolute" ? n.offsetLeft : parseInt(cs.left) || 0);
                if (pos != "absolute" && pos != "relative") {
                    var ret = dojo.coords(n, true);
                    top = ret.y;
                    left = ret.x;
                    n.style.position = "absolute";
                    n.style.top = top + "px";
                    n.style.left = left + "px";
                }
            };
        })(_fe);
        init();
        var anim = dojo.animateProperty(dojo.mixin({properties:{top:{end:_fd.top ||
                                                                         0},left:{end:_fd.left ||
                                                                                      0}}}, _fd));
        dojo.connect(anim, "beforeBegin", anim, init);
        return anim;
    };
}
if (!dojo._hasResource["dijit.layout.ContentPane"]) {
    dojo._hasResource["dijit.layout.ContentPane"] = true;
    dojo.provide("dijit.layout.ContentPane");
    dojo.declare("dijit.layout.ContentPane", dijit._Widget, {href:"",extractContent:false,parseOnLoad:true,preventCache:false,preload:false,refreshOnShow:false,loadingMessage:"<span class='dijitContentPaneLoading'>${loadingState}</span>",errorMessage:"<span class='dijitContentPaneError'>${errorState}</span>",isLoaded:false,"class":"dijitContentPane",doLayout:"auto",postCreate:function() {
        this.domNode.title = "";
        if (!this.containerNode) {
            this.containerNode = this.domNode;
        }
        if (this.preload) {
            this._loadCheck();
        }
        var _107 = dojo.i18n.getLocalization("dijit", "loading", this.lang);
        this.loadingMessage = dojo.string.substitute(this.loadingMessage, _107);
        this.errorMessage = dojo.string.substitute(this.errorMessage, _107);
        var _108 = dijit.getWaiRole(this.domNode);
        if (!_108) {
            dijit.setWaiRole(this.domNode, "group");
        }
        dojo.addClass(this.domNode, this["class"]);
    },startup:function() {
        if (this._started) {
            return;
        }
        if (this.doLayout != "false" && this.doLayout !== false) {
            this._checkIfSingleChild();
            if (this._singleChild) {
                this._singleChild.startup();
            }
        }
        this._loadCheck();
        this.inherited(arguments);
    },_checkIfSingleChild:function() {
        var _109 = dojo.query(">", this.containerNode ||
                                   this.domNode),_10a = _109.filter("[widgetId]");
        if (_109.length == 1 && _10a.length == 1) {
            this.isContainer = true;
            this._singleChild = dijit.byNode(_10a[0]);
        } else {
            delete this.isContainer;
            delete this._singleChild;
        }
    },refresh:function() {
        return this._prepareLoad(true);
    },setHref:function(href) {
        this.href = href;
        return this._prepareLoad();
    },setContent:function(data) {
        if (!this._isDownloaded) {
            this.href = "";
            this._onUnloadHandler();
        }
        this._setContent(data || "");
        this._isDownloaded = false;
        if (this.parseOnLoad) {
            this._createSubWidgets();
        }
        if (this.doLayout != "false" && this.doLayout !== false) {
            this._checkIfSingleChild();
            if (this._singleChild && this._singleChild.resize) {
                this._singleChild.startup();
                this._singleChild.resize(this._contentBox ||
                                         dojo.contentBox(this.containerNode || this.domNode));
            }
        }
        this._onLoadHandler();
    },cancel:function() {
        if (this._xhrDfd && (this._xhrDfd.fired == -1)) {
            this._xhrDfd.cancel();
        }
        delete this._xhrDfd;
    },destroy:function() {
        if (this._beingDestroyed) {
            return;
        }
        this._onUnloadHandler();
        this._beingDestroyed = true;
        this.inherited("destroy", arguments);
    },resize:function(size) {
        dojo.marginBox(this.domNode, size);
        var node = this.containerNode || this.domNode,mb = dojo.mixin(dojo.marginBox(node), size ||
                                                                                            {});
        this._contentBox = dijit.layout.marginBox2contentBox(node, mb);
        if (this._singleChild && this._singleChild.resize) {
            this._singleChild.resize(this._contentBox);
        }
    },_prepareLoad:function(_110) {
        this.cancel();
        this.isLoaded = false;
        this._loadCheck(_110);
    },_isShown:function() {
        if ("open" in this) {
            return this.open;
        } else {
            var node = this.domNode;
            return (node.style.display != "none") && (node.style.visibility != "hidden");
        }
    },_loadCheck:function(_112) {
        var _113 = this._isShown();
        if (this.href && (_112 || (this.preload && !this._xhrDfd) ||
                          (this.refreshOnShow && _113 && !this._xhrDfd) ||
                          (!this.isLoaded && _113 && !this._xhrDfd))) {
            this._downloadExternalContent();
        }
    },_downloadExternalContent:function() {
        this._onUnloadHandler();
        this._setContent(this.onDownloadStart.call(this));
        var self = this;
        var _115 = {preventCache:(this.preventCache ||
                                  this.refreshOnShow),url:this.href,handleAs:"text"};
        if (dojo.isObject(this.ioArgs)) {
            dojo.mixin(_115, this.ioArgs);
        }
        var hand = this._xhrDfd = (this.ioMethod || dojo.xhrGet)(_115);
        hand.addCallback(function(html) {
            try {
                self.onDownloadEnd.call(self);
                self._isDownloaded = true;
                self.setContent.call(self, html);
            } catch(err) {
                self._onError.call(self, "Content", err);
            }
            delete self._xhrDfd;
            return html;
        });
        hand.addErrback(function(err) {
            if (!hand.cancelled) {
                self._onError.call(self, "Download", err);
            }
            delete self._xhrDfd;
            return err;
        });
    },_onLoadHandler:function() {
        this.isLoaded = true;
        try {
            this.onLoad.call(this);
        } catch(e) {
            console.error("Error " + this.widgetId + " running custom onLoad code");
        }
    },_onUnloadHandler:function() {
        this.isLoaded = false;
        this.cancel();
        try {
            this.onUnload.call(this);
        } catch(e) {
            console.error("Error " + this.widgetId + " running custom onUnload code");
        }
    },_setContent:function(cont) {
        this.destroyDescendants();
        try {
            var node = this.containerNode || this.domNode;
            while (node.firstChild) {
                dojo._destroyElement(node.firstChild);
            }
            if (typeof cont == "string") {
                if (this.extractContent) {
                    match = cont.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
                    if (match) {
                        cont = match[1];
                    }
                }
                node.innerHTML = cont;
            } else {
                if (cont.nodeType) {
                    node.appendChild(cont);
                } else {
                    dojo.forEach(cont, function(n) {
                        node.appendChild(n.cloneNode(true));
                    });
                }
            }
        } catch(e) {
            var _11c = this.onContentError(e);
            try {
                node.innerHTML = _11c;
            } catch(e) {
                console.error("Fatal " + this.id + " could not change content due to " +
                              e.message, e);
            }
        }
    },_onError:function(type, err, _11f) {
        var _120 = this["on" + type + "Error"].call(this, err);
        if (_11f) {
            console.error(_11f, err);
        } else {
            if (_120) {
                this._setContent.call(this, _120);
            }
        }
    },_createSubWidgets:function() {
        var _121 = this.containerNode || this.domNode;
        try {
            dojo.parser.parse(_121, true);
        } catch(e) {
            this._onError("Content", e, "Couldn't create widgets in " + this.id +
                                        (this.href ? " from " + this.href : ""));
        }
    },onLoad:function(e) {
    },onUnload:function(e) {
    },onDownloadStart:function() {
        return this.loadingMessage;
    },onContentError:function(_124) {
    },onDownloadError:function(_125) {
        return this.errorMessage;
    },onDownloadEnd:function() {
    }});
}
if (!dojo._hasResource["dijit.form.Form"]) {
    dojo._hasResource["dijit.form.Form"] = true;
    dojo.provide("dijit.form.Form");
    dojo.declare("dijit.form._FormMixin", null, {reset:function() {
        dojo.forEach(this.getDescendants(), function(_126) {
            if (_126.reset) {
                _126.reset();
            }
        });
    },validate:function() {
        var _127 = false;
        return dojo.every(dojo.map(this.getDescendants(), function(_128) {
            _128._hasBeenBlurred = true;
            var _129 = !_128.validate || _128.validate();
            if (!_129 && !_127) {
                dijit.scrollIntoView(_128.containerNode || _128.domNode);
                _128.focus();
                _127 = true;
            }
            return _129;
        }), "return item;");
    },setValues:function(obj) {
        var map = {};
        dojo.forEach(this.getDescendants(), function(_12c) {
            if (!_12c.name) {
                return;
            }
            var _12d = map[_12c.name] || (map[_12c.name] = []);
            _12d.push(_12c);
        });
        for (var name in map) {
            var _12f = map[name],_130 = dojo.getObject(name, false, obj);
            if (!dojo.isArray(_130)) {
                _130 = [_130];
            }
            if (typeof _12f[0].checked == "boolean") {
                dojo.forEach(_12f, function(w, i) {
                    w.setValue(dojo.indexOf(_130, w.value) != -1);
                });
            } else {
                if (_12f[0]._multiValue) {
                    _12f[0].setValue(_130);
                } else {
                    dojo.forEach(_12f, function(w, i) {
                        w.setValue(_130[i]);
                    });
                }
            }
        }
    },getValues:function() {
        var obj = {};
        dojo.forEach(this.getDescendants(), function(_136) {
            var name = _136.name;
            if (!name) {
                return;
            }
            var _138 = (_136.getValue && !_136._getValueDeprecated) ? _136.getValue() : _136.value;
            if (typeof _136.checked == "boolean") {
                if (/Radio/.test(_136.declaredClass)) {
                    if (_138 !== false) {
                        dojo.setObject(name, _138, obj);
                    }
                } else {
                    var ary = dojo.getObject(name, false, obj);
                    if (!ary) {
                        ary = [];
                        dojo.setObject(name, ary, obj);
                    }
                    if (_138 !== false) {
                        ary.push(_138);
                    }
                }
            } else {
                dojo.setObject(name, _138, obj);
            }
        });
        return obj;
    },isValid:function() {
        return dojo.every(this.getDescendants(), function(_13a) {
            return !_13a.isValid || _13a.isValid();
        });
    }});
    dojo.declare("dijit.form.Form", [dijit._Widget,dijit._Templated,dijit.form._FormMixin], {name:"",action:"",method:"",encType:"","accept-charset":"",accept:"",target:"",templateString:"<form dojoAttachPoint='containerNode' dojoAttachEvent='onreset:_onReset,onsubmit:_onSubmit' name='${name}'></form>",attributeMap:dojo.mixin(dojo.clone(dijit._Widget.prototype.attributeMap), {action:"",method:"",encType:"","accept-charset":"",accept:"",target:""}),execute:function(
            _13b) {
    },onExecute:function() {
    },setAttribute:function(attr, _13d) {
        this.inherited(arguments);
        switch (attr) {case "encType":if (dojo.isIE) {
            this.domNode.encoding = _13d;
        }}
    },postCreate:function() {
        if (dojo.isIE && this.srcNodeRef && this.srcNodeRef.attributes) {
            var item = this.srcNodeRef.attributes.getNamedItem("encType");
            if (item && !item.specified && (typeof item.value == "string")) {
                this.setAttribute("encType", item.value);
            }
        }
        this.inherited(arguments);
    },onReset:function(e) {
        return true;
    },_onReset:function(e) {
        var faux = {returnValue:true,preventDefault:function() {
            this.returnValue = false;
        },stopPropagation:function() {
        },currentTarget:e.currentTarget,target:e.target};
        if (!(this.onReset(faux) === false) && faux.returnValue) {
            this.reset();
        }
        dojo.stopEvent(e);
        return false;
    },_onSubmit:function(e) {
        var fp = dijit.form.Form.prototype;
        if (this.execute != fp.execute || this.onExecute != fp.onExecute) {
            dojo.deprecated("dijit.form.Form:execute()/onExecute() are deprecated. Use onSubmit() instead.", "", "2.0");
            this.onExecute();
            this.execute(this.getValues());
        }
        if (this.onSubmit(e) === false) {
            dojo.stopEvent(e);
        }
    },onSubmit:function(e) {
        return this.isValid();
    },submit:function() {
        if (!(this.onSubmit() === false)) {
            this.containerNode.submit();
        }
    }});
}
if (!dojo._hasResource["dijit.Dialog"]) {
    dojo._hasResource["dijit.Dialog"] = true;
    dojo.provide("dijit.Dialog");
    dojo.declare("dijit.DialogUnderlay", [dijit._Widget,dijit._Templated], {templateString:"<div class='dijitDialogUnderlayWrapper' id='${id}_wrapper'><div class='dijitDialogUnderlay ${class}' id='${id}' dojoAttachPoint='node'></div></div>",attributeMap:{},postCreate:function() {
        dojo.body().appendChild(this.domNode);
        this.bgIframe = new dijit.BackgroundIframe(this.domNode);
    },layout:function() {
        var _145 = dijit.getViewport();
        var is = this.node.style,os = this.domNode.style;
        os.top = _145.t + "px";
        os.left = _145.l + "px";
        is.width = _145.w + "px";
        is.height = _145.h + "px";
        var _148 = dijit.getViewport();
        if (_145.w != _148.w) {
            is.width = _148.w + "px";
        }
        if (_145.h != _148.h) {
            is.height = _148.h + "px";
        }
    },show:function() {
        this.domNode.style.display = "block";
        this.layout();
        if (this.bgIframe.iframe) {
            this.bgIframe.iframe.style.display = "block";
        }
        this._resizeHandler = this.connect(window, "onresize", "layout");
    },hide:function() {
        this.domNode.style.display = "none";
        if (this.bgIframe.iframe) {
            this.bgIframe.iframe.style.display = "none";
        }
        this.disconnect(this._resizeHandler);
    },uninitialize:function() {
        if (this.bgIframe) {
            this.bgIframe.destroy();
        }
    }});
    dojo.declare("dijit._DialogMixin", null, {attributeMap:dijit._Widget.prototype.attributeMap,execute:function(
            _149) {
    },onCancel:function() {
    },onExecute:function() {
    },_onSubmit:function() {
        this.onExecute();
        this.execute(this.getValues());
    },_getFocusItems:function(_14a) {
        var _14b = dijit.getFirstInTabbingOrder(_14a);
        this._firstFocusItem = _14b ? _14b : _14a;
        _14b = dijit.getLastInTabbingOrder(_14a);
        this._lastFocusItem = _14b ? _14b : this._firstFocusItem;
        if (dojo.isMoz && this._firstFocusItem.tagName.toLowerCase() == "input" &&
            dojo.attr(this._firstFocusItem, "type").toLowerCase() == "file") {
            dojo.attr(_14a, "tabindex", "0");
            this._firstFocusItem = _14a;
        }
    }});
    dojo.declare("dijit.Dialog", [dijit.layout.ContentPane,dijit._Templated,dijit.form._FormMixin,dijit._DialogMixin], {templateString:null,templateString:"<div class=\"dijitDialog\" tabindex=\"-1\" waiRole=\"dialog\" waiState=\"labelledby-${id}_title\">\n\t<div dojoAttachPoint=\"titleBar\" class=\"dijitDialogTitleBar\">\n\t<span dojoAttachPoint=\"titleNode\" class=\"dijitDialogTitle\" id=\"${id}_title\">${title}</span>\n\t<span dojoAttachPoint=\"closeButtonNode\" class=\"dijitDialogCloseIcon\" dojoAttachEvent=\"onclick: onCancel\">\n\t\t<span dojoAttachPoint=\"closeText\" class=\"closeText\">x</span>\n\t</span>\n\t</div>\n\t\t<div dojoAttachPoint=\"containerNode\" class=\"dijitDialogPaneContent\"></div>\n</div>\n",open:false,duration:400,refocus:true,_firstFocusItem:null,_lastFocusItem:null,doLayout:false,attributeMap:dojo.mixin(dojo.clone(dijit._Widget.prototype.attributeMap), {title:"titleBar"}),postCreate:function() {
        dojo.body().appendChild(this.domNode);
        this.inherited(arguments);
        var _14c = dojo.i18n.getLocalization("dijit", "common");
        if (this.closeButtonNode) {
            this.closeButtonNode.setAttribute("title", _14c.buttonCancel);
        }
        if (this.closeText) {
            this.closeText.setAttribute("title", _14c.buttonCancel);
        }
        var s = this.domNode.style;
        s.visibility = "hidden";
        s.position = "absolute";
        s.display = "";
        s.top = "-9999px";
        this.connect(this, "onExecute", "hide");
        this.connect(this, "onCancel", "hide");
        this._modalconnects = [];
    },onLoad:function() {
        this._position();
        this.inherited(arguments);
    },_setup:function() {
        if (this.titleBar) {
            this._moveable =
            new dojo.dnd.TimedMoveable(this.domNode, {handle:this.titleBar,timeout:0});
        }
        this._underlay = new dijit.DialogUnderlay({id:this.id +
                                                      "_underlay","class":dojo.map(this["class"].split(/\s/), function(
                s) {
            return s + "_underlay";
        }).join(" ")});
        var node = this.domNode;
        this._fadeIn =
        dojo.fx.combine([dojo.fadeIn({node:node,duration:this.duration}),dojo.fadeIn({node:this._underlay.domNode,duration:this.duration,onBegin:dojo.hitch(this._underlay, "show")})]);
        this._fadeOut =
        dojo.fx.combine([dojo.fadeOut({node:node,duration:this.duration,onEnd:function() {
            node.style.visibility = "hidden";
            node.style.top = "-9999px";
        }}),dojo.fadeOut({node:this._underlay.domNode,duration:this.duration,onEnd:dojo.hitch(this._underlay, "hide")})]);
    },uninitialize:function() {
        if (this._fadeIn && this._fadeIn.status() == "playing") {
            this._fadeIn.stop();
        }
        if (this._fadeOut && this._fadeOut.status() == "playing") {
            this._fadeOut.stop();
        }
        if (this._underlay) {
            this._underlay.destroy();
        }
    },_position:function() {
        if (dojo.hasClass(dojo.body(), "dojoMove")) {
            return;
        }
        var _150 = dijit.getViewport();
        var mb = dojo.marginBox(this.domNode);
        var _152 = this.domNode.style;
        _152.left = Math.floor((_150.l + (_150.w - mb.w) / 2)) + "px";
        _152.top = Math.floor((_150.t + (_150.h - mb.h) / 2)) + "px";
    },_onKey:function(evt) {
        if (evt.keyCode) {
            var node = evt.target;
            if (evt.keyCode == dojo.keys.TAB) {
                this._getFocusItems(this.domNode);
            }
            var _155 = (this._firstFocusItem == this._lastFocusItem);
            if (node == this._firstFocusItem && evt.shiftKey && evt.keyCode == dojo.keys.TAB) {
                if (!_155) {
                    dijit.focus(this._lastFocusItem);
                }
                dojo.stopEvent(evt);
            } else {
                if (node == this._lastFocusItem && evt.keyCode == dojo.keys.TAB && !evt.shiftKey) {
                    if (!_155) {
                        dijit.focus(this._firstFocusItem);
                    }
                    dojo.stopEvent(evt);
                } else {
                    while (node) {
                        if (node == this.domNode) {
                            if (evt.keyCode == dojo.keys.ESCAPE) {
                                this.hide();
                            } else {
                                return;
                            }
                        }
                        node = node.parentNode;
                    }
                    if (evt.keyCode != dojo.keys.TAB) {
                        dojo.stopEvent(evt);
                    } else {
                        if (!dojo.isOpera) {
                            try {
                                this._firstFocusItem.focus();
                            } catch(e) {
                            }
                        }
                    }
                }
            }
        }
    },show:function() {
        if (this.open) {
            return;
        }
        if (!this._alreadyInitialized) {
            this._setup();
            this._alreadyInitialized = true;
        }
        if (this._fadeOut.status() == "playing") {
            this._fadeOut.stop();
        }
        this._modalconnects.push(dojo.connect(window, "onscroll", this, "layout"));
        this._modalconnects.push(dojo.connect(dojo.doc.documentElement, "onkeypress", this, "_onKey"));
        dojo.style(this.domNode, "opacity", 0);
        this.domNode.style.visibility = "";
        this.open = true;
        this._loadCheck();
        this._position();
        this._fadeIn.play();
        this._savedFocus = dijit.getFocus(this);
        this._getFocusItems(this.domNode);
        setTimeout(dojo.hitch(this, function() {
            dijit.focus(this._firstFocusItem);
        }), 50);
    },hide:function() {
        if (!this._alreadyInitialized) {
            return;
        }
        if (this._fadeIn.status() == "playing") {
            this._fadeIn.stop();
        }
        this._fadeOut.play();
        if (this._scrollConnected) {
            this._scrollConnected = false;
        }
        dojo.forEach(this._modalconnects, dojo.disconnect);
        this._modalconnects = [];
        if (this.refocus) {
            this.connect(this._fadeOut, "onEnd", dojo.hitch(dijit, "focus", this._savedFocus));
        }
        this.open = false;
    },layout:function() {
        if (this.domNode.style.visibility != "hidden") {
            this._underlay.layout();
            this._position();
        }
    },destroy:function() {
        dojo.forEach(this._modalconnects, dojo.disconnect);
        if (this.refocus && this.open) {
            var fo = this._savedFocus;
            setTimeout(dojo.hitch(dijit, "focus", fo), 25);
        }
        this.inherited(arguments);
    }});
    dojo.declare("dijit.TooltipDialog", [dijit.layout.ContentPane,dijit._Templated,dijit.form._FormMixin,dijit._DialogMixin], {title:"",doLayout:false,_firstFocusItem:null,_lastFocusItem:null,templateString:null,templateString:"<div class=\"dijitTooltipDialog\" waiRole=\"presentation\">\n\t<div class=\"dijitTooltipContainer\" waiRole=\"presentation\">\n\t\t<div class =\"dijitTooltipContents dijitTooltipFocusNode\" dojoAttachPoint=\"containerNode\" tabindex=\"-1\" waiRole=\"dialog\"></div>\n\t</div>\n\t<div class=\"dijitTooltipConnector\" waiRole=\"presenation\"></div>\n</div>\n",postCreate:function() {
        this.inherited(arguments);
        this.connect(this.containerNode, "onkeypress", "_onKey");
        this.containerNode.title = this.title;
    },orient:function(node, _158, _159) {
        this.domNode.className =
        "dijitTooltipDialog " + " dijitTooltipAB" + (_159.charAt(1) == "L" ? "Left" : "Right") +
        " dijitTooltip" + (_159.charAt(0) == "T" ? "Below" : "Above");
    },onOpen:function(pos) {
        this._getFocusItems(this.containerNode);
        this.orient(this.domNode, pos.aroundCorner, pos.corner);
        this._loadCheck();
        dijit.focus(this._firstFocusItem);
    },_onKey:function(evt) {
        var node = evt.target;
        if (evt.keyCode == dojo.keys.TAB) {
            this._getFocusItems(this.containerNode);
        }
        var _15d = (this._firstFocusItem == this._lastFocusItem);
        if (evt.keyCode == dojo.keys.ESCAPE) {
            this.onCancel();
        } else {
            if (node == this._firstFocusItem && evt.shiftKey && evt.keyCode == dojo.keys.TAB) {
                if (!_15d) {
                    dijit.focus(this._lastFocusItem);
                }
                dojo.stopEvent(evt);
            } else {
                if (node == this._lastFocusItem && evt.keyCode == dojo.keys.TAB && !evt.shiftKey) {
                    if (!_15d) {
                        dijit.focus(this._firstFocusItem);
                    }
                    dojo.stopEvent(evt);
                } else {
                    if (evt.keyCode == dojo.keys.TAB) {
                        evt.stopPropagation();
                    }
                }
            }
        }
    }});
}
if (!dojo._hasResource["dijit._editor.selection"]) {
    dojo._hasResource["dijit._editor.selection"] = true;
    dojo.provide("dijit._editor.selection");
    dojo.mixin(dijit._editor.selection, {getType:function() {
        if (dojo.doc.selection) {
            return dojo.doc.selection.type.toLowerCase();
        } else {
            var _15e = "text";
            var oSel;
            try {
                oSel = dojo.global.getSelection();
            } catch(e) {
            }
            if (oSel && oSel.rangeCount == 1) {
                var _160 = oSel.getRangeAt(0);
                if ((_160.startContainer == _160.endContainer) &&
                    ((_160.endOffset - _160.startOffset) == 1) &&
                    (_160.startContainer.nodeType != 3)) {
                    _15e = "control";
                }
            }
            return _15e;
        }
    },getSelectedText:function() {
        if (dojo.doc.selection) {
            if (dijit._editor.selection.getType() == "control") {
                return null;
            }
            return dojo.doc.selection.createRange().text;
        } else {
            var _161 = dojo.global.getSelection();
            if (_161) {
                return _161.toString();
            }
        }
        return "";
    },getSelectedHtml:function() {
        if (dojo.doc.selection) {
            if (dijit._editor.selection.getType() == "control") {
                return null;
            }
            return dojo.doc.selection.createRange().htmlText;
        } else {
            var _162 = dojo.global.getSelection();
            if (_162 && _162.rangeCount) {
                var frag = _162.getRangeAt(0).cloneContents();
                var div = dojo.doc.createElement("div");
                div.appendChild(frag);
                return div.innerHTML;
            }
            return null;
        }
    },getSelectedElement:function() {
        if (this.getType() == "control") {
            if (dojo.doc.selection) {
                var _165 = dojo.doc.selection.createRange();
                if (_165 && _165.item) {
                    return dojo.doc.selection.createRange().item(0);
                }
            } else {
                var _166 = dojo.global.getSelection();
                return _166.anchorNode.childNodes[_166.anchorOffset];
            }
        }
        return null;
    },getParentElement:function() {
        if (this.getType() == "control") {
            var p = this.getSelectedElement();
            if (p) {
                return p.parentNode;
            }
        } else {
            if (dojo.doc.selection) {
                return dojo.doc.selection.createRange().parentElement();
            } else {
                var _168 = dojo.global.getSelection();
                if (_168) {
                    var node = _168.anchorNode;
                    while (node && (node.nodeType != 1)) {
                        node = node.parentNode;
                    }
                    return node;
                }
            }
        }
        return null;
    },hasAncestorElement:function(_16a) {
        return this.getAncestorElement.apply(this, arguments) != null;
    },getAncestorElement:function(_16b) {
        var node = this.getSelectedElement() || this.getParentElement();
        return this.getParentOfType(node, arguments);
    },isTag:function(node, tags) {
        if (node && node.tagName) {
            var _nlc = node.tagName.toLowerCase();
            for (var i = 0; i < tags.length; i++) {
                var _tlc = String(tags[i]).toLowerCase();
                if (_nlc == _tlc) {
                    return _tlc;
                }
            }
        }
        return "";
    },getParentOfType:function(node, tags) {
        while (node) {
            if (this.isTag(node, tags).length) {
                return node;
            }
            node = node.parentNode;
        }
        return null;
    },collapse:function(_174) {
        if (window["getSelection"]) {
            var _175 = dojo.global.getSelection();
            if (_175.removeAllRanges) {
                if (_174) {
                    _175.collapseToStart();
                } else {
                    _175.collapseToEnd();
                }
            } else {
                _175.collapse(_174);
            }
        } else {
            if (dojo.doc.selection) {
                var _176 = dojo.doc.selection.createRange();
                _176.collapse(_174);
                _176.select();
            }
        }
    },remove:function() {
        var _s = dojo.doc.selection;
        if (_s) {
            if (_s.type.toLowerCase() != "none") {
                _s.clear();
            }
            return _s;
        } else {
            _s = dojo.global.getSelection();
            _s.deleteFromDocument();
            return _s;
        }
    },selectElementChildren:function(_178, _179) {
        var _17a = dojo.global;
        var _17b = dojo.doc;
        _178 = dojo.byId(_178);
        if (_17b.selection && dojo.body().createTextRange) {
            var _17c = _178.ownerDocument.body.createTextRange();
            _17c.moveToElementText(_178);
            if (!_179) {
                try {
                    _17c.select();
                } catch(e) {
                }
            }
        } else {
            if (_17a.getSelection) {
                var _17d = _17a.getSelection();
                if (_17d.setBaseAndExtent) {
                    _17d.setBaseAndExtent(_178, 0, _178, _178.innerText.length - 1);
                } else {
                    if (_17d.selectAllChildren) {
                        _17d.selectAllChildren(_178);
                    }
                }
            }
        }
    },selectElement:function(_17e, _17f) {
        var _180,_181 = dojo.doc;
        _17e = dojo.byId(_17e);
        if (_181.selection && dojo.body().createTextRange) {
            try {
                _180 = dojo.body().createControlRange();
                _180.addElement(_17e);
                if (!_17f) {
                    _180.select();
                }
            } catch(e) {
                this.selectElementChildren(_17e, _17f);
            }
        } else {
            if (dojo.global.getSelection) {
                var _182 = dojo.global.getSelection();
                if (_182.removeAllRanges) {
                    _180 = _181.createRange();
                    _180.selectNode(_17e);
                    _182.removeAllRanges();
                    _182.addRange(_180);
                }
            }
        }
    }});
}
if (!dojo._hasResource["dijit._editor.html"]) {
    dojo._hasResource["dijit._editor.html"] = true;
    dojo.provide("dijit._editor.html");
    dijit._editor.escapeXml = function(str, _184) {
        str =
        str.replace(/&/gm, "&amp;").replace(/</gm, "&lt;").replace(/>/gm, "&gt;").replace(/"/gm, "&quot;");
        if (!_184) {
            str = str.replace(/'/gm, "&#39;");
        }
        return str;
    };
    dijit._editor.getNodeHtml = function(node) {
        var _186;
        switch (node.nodeType) {case 1:_186 = "<" +
                                              node.nodeName.toLowerCase();var _187 = [];if (dojo.isIE &&
                                                                                            node.outerHTML) {
            var s = node.outerHTML;
            s = s.substr(0, s.indexOf(">"));
            s = s.replace(/(['"])[^"']*\1/g, "");
            var reg = /([^\s=]+)=/g;
            var m,key;
            while ((m = reg.exec(s))) {
                key = m[1];
                if (key.substr(0, 3) != "_dj") {
                    if (key == "src" || key == "href") {
                        if (node.getAttribute("_djrealurl")) {
                            _187.push([key,node.getAttribute("_djrealurl")]);
                            continue;
                        }
                    }
                    if (key == "style") {
                        _187.push([key,node.style.cssText.toLowerCase()]);
                    } else {
                        _187.push([key,key == "class" ? node.className : node.getAttribute(key)]);
                    }
                }
            }
        } else {
            var attr,i = 0,_18e = node.attributes;
            while ((attr = _18e[i++])) {
                var n = attr.name;
                if (n.substr(0, 3) != "_dj") {
                    var v = attr.value;
                    if (n == "src" || n == "href") {
                        if (node.getAttribute("_djrealurl")) {
                            v = node.getAttribute("_djrealurl");
                        }
                    }
                    _187.push([n,v]);
                }
            }
        }_187.sort(function(a, b) {
            return a[0] < b[0] ? -1 : (a[0] == b[0] ? 0 : 1);
        });i = 0;while ((attr = _187[i++])) {
            _186 += " " + attr[0] + "=\"" +
                    (dojo.isString(attr[1]) ? dijit._editor.escapeXml(attr[1], true) : attr[1]) +
                    "\"";
        }if (node.childNodes.length) {
            _186 +=
            ">" + dijit._editor.getChildrenHtml(node) + "</" + node.nodeName.toLowerCase() + ">";
        } else {
            _186 += " />";
        }break;case 3:_186 = dijit._editor.escapeXml(node.nodeValue, true);break;case 8:_186 =
                                                                                        "<!--" +
                                                                                        dijit._editor.escapeXml(node.nodeValue, true) +
                                                                                        "-->";break;default:_186 =
                                                                                                            "Element not recognized - Type: " +
                                                                                                            node.nodeType +
                                                                                                            " Name: " +
                                                                                                            node.nodeName;}
        return _186;
    };
    dijit._editor.getChildrenHtml = function(dom) {
        var out = "";
        if (!dom) {
            return out;
        }
        var _195 = dom["childNodes"] || dom;
        var i = 0;
        var node;
        while ((node = _195[i++])) {
            out += dijit._editor.getNodeHtml(node);
        }
        return out;
    };
}
if (!dojo._hasResource["dijit._editor.RichText"]) {
    dojo._hasResource["dijit._editor.RichText"] = true;
    dojo.provide("dijit._editor.RichText");
    if (!dojo.config["useXDomain"] || dojo.config["allowXdRichTextSave"]) {
        if (dojo._postLoad) {
            (function() {
                var _198 = dojo.doc.createElement("textarea");
                _198.id = dijit._scopeName + "._editor.RichText.savedContent";
                var s = _198.style;
                s.display = "none";
                s.position = "absolute";
                s.top = "-100px";
                s.left = "-100px";
                s.height = "3px";
                s.width = "3px";
                dojo.body().appendChild(_198);
            })();
        } else {
            try {
                dojo.doc.write("<textarea id=\"" + dijit._scopeName +
                               "._editor.RichText.savedContent\" " +
                               "style=\"display:none;position:absolute;top:-100px;left:-100px;height:3px;width:3px;overflow:hidden;\"></textarea>");
            } catch(e) {
            }
        }
    }
    dojo.declare("dijit._editor.RichText", dijit._Widget, {constructor:function() {
        this.contentPreFilters = [];
        this.contentPostFilters = [];
        this.contentDomPreFilters = [];
        this.contentDomPostFilters = [];
        this.editingAreaStyleSheets = [];
        this._keyHandlers = {};
        this.contentPreFilters.push(dojo.hitch(this, "_preFixUrlAttributes"));
        if (dojo.isMoz) {
            this.contentPreFilters.push(this._fixContentForMoz);
            this.contentPostFilters.push(this._removeMozBogus);
        } else {
            if (dojo.isSafari) {
                this.contentPostFilters.push(this._removeSafariBogus);
            }
        }
        this.onLoadDeferred = new dojo.Deferred();
    },inheritWidth:false,focusOnLoad:false,name:"",styleSheets:"",_content:"",height:"300px",minHeight:"1em",isClosed:true,isLoaded:false,_SEPARATOR:"@@**%%__RICHTEXTBOUNDRY__%%**@@",onLoadDeferred:null,postCreate:function() {
        dojo.publish(dijit._scopeName + "._editor.RichText::init", [this]);
        this.open();
        this.setupDefaultShortcuts();
    },setupDefaultShortcuts:function() {
        var exec = function(cmd, arg) {
            return arguments.length == 1 ? function() {
                this.execCommand(cmd);
            } : function() {
                this.execCommand(cmd, arg);
            };
        };
        var _19d = {b:exec("bold"),i:exec("italic"),u:exec("underline"),a:exec("selectall"),s:function() {
            this.save(true);
        },"1":exec("formatblock", "h1"),"2":exec("formatblock", "h2"),"3":exec("formatblock", "h3"),"4":exec("formatblock", "h4"),"\\":exec("insertunorderedlist")};
        if (!dojo.isIE) {
            _19d.Z = exec("redo");
        }
        for (var key in _19d) {
            this.addKeyHandler(key, this.KEY_CTRL, _19d[key]);
        }
    },events:["onKeyPress","onKeyDown","onKeyUp","onClick"],captureEvents:[],_editorCommandsLocalized:false,_localizeEditorCommands:function() {
        if (this._editorCommandsLocalized) {
            return;
        }
        this._editorCommandsLocalized = true;
        var _19f = ["p","pre","address","h1","h2","h3","h4","h5","h6","ol","div","ul"];
        var _1a0 = "",_1a1,i = 0;
        while ((_1a1 = _19f[i++])) {
            if (_1a1.charAt(1) != "l") {
                _1a0 += "<" + _1a1 + "><span>content</span></" + _1a1 + ">";
            } else {
                _1a0 += "<" + _1a1 + "><li>content</li></" + _1a1 + ">";
            }
        }
        var div = dojo.doc.createElement("div");
        div.style.position = "absolute";
        div.style.left = "-2000px";
        div.style.top = "-2000px";
        dojo.doc.body.appendChild(div);
        div.innerHTML = _1a0;
        var node = div.firstChild;
        while (node) {
            dijit._editor.selection.selectElement(node.firstChild);
            dojo.withGlobal(this.window, "selectElement", dijit._editor.selection, [node.firstChild]);
            var _1a5 = node.tagName.toLowerCase();
            this._local2NativeFormatNames[_1a5] = dojo.doc.queryCommandValue("formatblock");
            this._native2LocalFormatNames[this._local2NativeFormatNames[_1a5]] = _1a5;
            node = node.nextSibling;
        }
        dojo.doc.body.removeChild(div);
    },open:function(_1a6) {
        if ((!this.onLoadDeferred) || (this.onLoadDeferred.fired >= 0)) {
            this.onLoadDeferred = new dojo.Deferred();
        }
        if (!this.isClosed) {
            this.close();
        }
        dojo.publish(dijit._scopeName + "._editor.RichText::open", [this]);
        this._content = "";
        if ((arguments.length == 1) && (_1a6["nodeName"])) {
            this.domNode = _1a6;
        }
        var html;
        if ((this.domNode["nodeName"]) && (this.domNode.nodeName.toLowerCase() == "textarea")) {
            this.textarea = this.domNode;
            this.name = this.textarea.name;
            html = this._preFilterContent(this.textarea.value);
            this.domNode = dojo.doc.createElement("div");
            this.domNode.setAttribute("widgetId", this.id);
            this.textarea.removeAttribute("widgetId");
            this.domNode.cssText = this.textarea.cssText;
            this.domNode.className += " " + this.textarea.className;
            dojo.place(this.domNode, this.textarea, "before");
            var _1a8 = dojo.hitch(this, function() {
                dojo.attr(this.textarea, "tabIndex", "-1");
                with (this.textarea.style) {
                    display = "block";
                    position = "absolute";
                    left = top = "-1000px";
                    if (dojo.isIE) {
                        this.__overflow = overflow;
                        overflow = "hidden";
                    }
                }
            });
            if (dojo.isIE) {
                setTimeout(_1a8, 10);
            } else {
                _1a8();
            }
        } else {
            html = this._preFilterContent(dijit._editor.getChildrenHtml(this.domNode));
            this.domNode.innerHTML = "";
        }
        if (html == "") {
            html = "&nbsp;";
        }
        var _1a9 = dojo.contentBox(this.domNode);
        this._oldHeight = _1a9.h;
        this._oldWidth = _1a9.w;
        if ((this.domNode["nodeName"]) && (this.domNode.nodeName == "LI")) {
            this.domNode.innerHTML = " <br>";
        }
        this.editingArea = dojo.doc.createElement("div");
        this.domNode.appendChild(this.editingArea);
        if (this.name != "" && (!dojo.config["useXDomain"] || dojo.config["allowXdRichTextSave"])) {
            var _1aa = dojo.byId(dijit._scopeName + "._editor.RichText.savedContent");
            if (_1aa.value != "") {
                var _1ab = _1aa.value.split(this._SEPARATOR),i = 0,dat;
                while ((dat = _1ab[i++])) {
                    var data = dat.split(":");
                    if (data[0] == this.name) {
                        html = data[1];
                        _1ab.splice(i, 1);
                        break;
                    }
                }
            }
            this.connect(window, "onbeforeunload", "_saveContent");
        }
        this.isClosed = false;
        if (dojo.isIE || dojo.isSafari || dojo.isOpera) {
            if (dojo.config["useXDomain"] && !dojo.config["dojoBlankHtmlUrl"]) {
                console.debug("dijit._editor.RichText: When using cross-domain Dojo builds," +
                              " please save dojo/resources/blank.html to your domain and set djConfig.dojoBlankHtmlUrl" +
                              " to the path on your domain to blank.html");
            }
            var burl = dojo.config["dojoBlankHtmlUrl"] ||
                       (dojo.moduleUrl("dojo", "resources/blank.html") + "");
            var ifr = this.editorObject = this.iframe = dojo.doc.createElement("iframe");
            ifr.id = this.id + "_iframe";
            ifr.src = burl;
            ifr.style.border = "none";
            ifr.style.width = "100%";
            ifr.frameBorder = 0;
            this.editingArea.appendChild(ifr);
            var h = null;
            var _1b2 = dojo.hitch(this, function() {
                if (h) {
                    dojo.disconnect(h);
                    h = null;
                }
                this.window = ifr.contentWindow;
                var d = this.document = this.window.document;
                d.open();
                d.write(this._getIframeDocTxt(html));
                d.close();
                if (dojo.isIE >= 7) {
                    if (this.height) {
                        ifr.style.height = this.height;
                    }
                    if (this.minHeight) {
                        ifr.style.minHeight = this.minHeight;
                    }
                } else {
                    ifr.style.height = this.height ? this.height : this.minHeight;
                }
                if (dojo.isIE) {
                    this._localizeEditorCommands();
                }
                this.onLoad();
                this.savedContent = this.getValue(true);
            });
            if (dojo.isIE && dojo.isIE < 7) {
                var t = setInterval(function() {
                    if (ifr.contentWindow.isLoaded) {
                        clearInterval(t);
                        _1b2();
                    }
                }, 100);
            } else {
                h = dojo.connect(((dojo.isIE) ? ifr.contentWindow : ifr), "onload", _1b2);
            }
        } else {
            this._drawIframe(html);
            this.savedContent = this.getValue(true);
        }
        if (this.domNode.nodeName == "LI") {
            this.domNode.lastChild.style.marginTop = "-1.2em";
        }
        this.domNode.className += " RichTextEditable";
    },_local2NativeFormatNames:{},_native2LocalFormatNames:{},_localizedIframeTitles:null,_getIframeDocTxt:function(
            html) {
        var _cs = dojo.getComputedStyle(this.domNode);
        if (dojo.isIE || (!this.height && !dojo.isMoz)) {
            html = "<div>" + html + "</div>";
        }
        var font = [_cs.fontWeight,_cs.fontSize,_cs.fontFamily].join(" ");
        var _1b8 = _cs.lineHeight;
        if (_1b8.indexOf("px") >= 0) {
            _1b8 = parseFloat(_1b8) / parseFloat(_cs.fontSize);
        } else {
            if (_1b8.indexOf("em") >= 0) {
                _1b8 = parseFloat(_1b8);
            } else {
                _1b8 = "1.0";
            }
        }
        return [this.isLeftToRight() ? "<html><head>" : "<html dir='rtl'><head>",(dojo.isMoz ?
                                                                                  "<title>" +
                                                                                  this._localizedIframeTitles.iframeEditTitle +
                                                                                  "</title>" :
                                                                                  ""),"<style>","body,html {","\tbackground:transparent;","\tfont:",font,";","\tpadding: 1em 0 0 0;","\tmargin: -1em 0 0 0;","\theight: 100%;","}","body{","\ttop:0px; left:0px; right:0px;",(
                (this.height || dojo.isOpera) ? "" :
                "position: fixed;"),"\tmin-height:",this.minHeight,";","\tline-height:",_1b8,"}","p{ margin: 1em 0 !important; }",(
                this.height ? "" :
                "body,html{height:auto;overflow-y:hidden;/*for IE*/} body > div {overflow-x:auto;/*for FF to show vertical scrollbar*/}"),"li > ul:-moz-first-node, li > ol:-moz-first-node{ padding-top: 1.2em; } ","li{ min-height:1.2em; }","</style>",this._applyEditingAreaStyleSheets(),"</head><body>" +
                                                                                                                                                                                                                                                                                              html +
                                                                                                                                                                                                                                                                                              "</body></html>"].join("");
    },_drawIframe:function(html) {
        if (!this.iframe) {
            var ifr = this.iframe = dojo.doc.createElement("iframe");
            ifr.id = this.id;
            var ifrs = ifr.style;
            ifrs.border = "none";
            ifrs.lineHeight = "0";
            ifrs.verticalAlign = "bottom";
            this.editorObject = this.iframe;
            this._localizedIframeTitles = dojo.i18n.getLocalization("dijit.form", "Textarea");
            var _1bc = dojo.query("label[for=\"" + this.id + "\"]");
            if (_1bc.length) {
                this._localizedIframeTitles.iframeEditTitle =
                _1bc[0].innerHTML + " " + this._localizedIframeTitles.iframeEditTitle;
            }
        }
        this.iframe.style.width = this.inheritWidth ? this._oldWidth : "100%";
        if (this.height) {
            this.iframe.style.height = this.height;
        } else {
            this.iframe.height = this._oldHeight;
        }
        var _1bd;
        if (this.textarea) {
            _1bd = this.srcNodeRef;
        } else {
            _1bd = dojo.doc.createElement("div");
            _1bd.style.display = "none";
            _1bd.innerHTML = html;
            this.editingArea.appendChild(_1bd);
        }
        this.editingArea.appendChild(this.iframe);
        var _1be = false;
        var _1bf = this.iframe.contentDocument;
        _1bf.open();
        if (dojo.isAIR) {
            _1bf.body.innerHTML = html;
        } else {
            _1bf.write(this._getIframeDocTxt(html));
        }
        _1bf.close();
        var _1c0 = dojo.hitch(this, function() {
            if (!_1be) {
                _1be = true;
            } else {
                return;
            }
            if (!this.editNode) {
                try {
                    if (this.iframe.contentWindow) {
                        this.window = this.iframe.contentWindow;
                        this.document = this.iframe.contentWindow.document;
                    } else {
                        if (this.iframe.contentDocument) {
                            this.window = this.iframe.contentDocument.window;
                            this.document = this.iframe.contentDocument;
                        }
                    }
                    if (!this.document.body) {
                        throw "Error";
                    }
                } catch(e) {
                    setTimeout(_1c0, 500);
                    _1be = false;
                    return;
                }
                dojo._destroyElement(_1bd);
                this.onLoad();
            } else {
                dojo._destroyElement(_1bd);
                this.editNode.innerHTML = html;
                this.onDisplayChanged();
            }
            this._preDomFilterContent(this.editNode);
        });
        _1c0();
    },_applyEditingAreaStyleSheets:function() {
        var _1c1 = [];
        if (this.styleSheets) {
            _1c1 = this.styleSheets.split(";");
            this.styleSheets = "";
        }
        _1c1 = _1c1.concat(this.editingAreaStyleSheets);
        this.editingAreaStyleSheets = [];
        var text = "",i = 0,url;
        while ((url = _1c1[i++])) {
            var _1c5 = (new dojo._Url(dojo.global.location, url)).toString();
            this.editingAreaStyleSheets.push(_1c5);
            text += "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + _1c5 + "\"/>";
        }
        return text;
    },addStyleSheet:function(uri) {
        var url = uri.toString();
        if (url.charAt(0) == "." || (url.charAt(0) != "/" && !uri.host)) {
            url = (new dojo._Url(dojo.global.location, url)).toString();
        }
        if (dojo.indexOf(this.editingAreaStyleSheets, url) > -1) {
            return;
        }
        this.editingAreaStyleSheets.push(url);
        if (this.document.createStyleSheet) {
            this.document.createStyleSheet(url);
        } else {
            var head = this.document.getElementsByTagName("head")[0];
            var _1c9 = this.document.createElement("link");
            with (_1c9) {
                rel = "stylesheet";
                type = "text/css";
                href = url;
            }
            head.appendChild(_1c9);
        }
    },removeStyleSheet:function(uri) {
        var url = uri.toString();
        if (url.charAt(0) == "." || (url.charAt(0) != "/" && !uri.host)) {
            url = (new dojo._Url(dojo.global.location, url)).toString();
        }
        var _1cc = dojo.indexOf(this.editingAreaStyleSheets, url);
        if (_1cc == -1) {
            return;
        }
        delete this.editingAreaStyleSheets[_1cc];
        dojo.withGlobal(this.window, "query", dojo, ["link:[href=\"" + url + "\"]"]).orphan();
    },disabled:true,_mozSettingProps:["styleWithCSS","insertBrOnReturn"],setDisabled:function(
            _1cd) {
        if (dojo.isIE || dojo.isSafari || dojo.isOpera) {
            if (dojo.isIE) {
                this.editNode.unselectable = "on";
            }
            this.editNode.contentEditable = !_1cd;
            if (dojo.isIE) {
                var _1ce = this;
                setTimeout(function() {
                    _1ce.editNode.unselectable = "off";
                }, 0);
            }
        } else {
            if (_1cd) {
                this._mozSettings = [false,this.blockNodeForEnter === "BR"];
            }
            this.document.designMode = (_1cd ? "off" : "on");
            if (!_1cd && this._mozSettings) {
                dojo.forEach(this._mozSettingProps, function(s, i) {
                    this.document.execCommand(s, false, this._mozSettings[i]);
                }, this);
            }
        }
        this.disabled = _1cd;
    },_isResized:function() {
        return false;
    },onLoad:function(e) {
        this.isLoaded = true;
        if (!this.window.__registeredWindow) {
            this.window.__registeredWindow = true;
            dijit.registerWin(this.window);
        }
        if (!dojo.isIE && (this.height || dojo.isMoz)) {
            this.editNode = this.document.body;
        } else {
            this.editNode = this.document.body.firstChild;
            var _1d2 = this;
            if (dojo.isIE) {
                var _1d3 = this.tabStop = dojo.doc.createElement("<div tabIndex=-1>");
                this.editingArea.appendChild(_1d3);
                this.iframe.onfocus = function() {
                    _1d2.editNode.setActive();
                };
            }
        }
        try {
            this.setDisabled(false);
        } catch(e) {
            var _1d4 = dojo.connect(this, "onClick", this, function() {
                this.setDisabled(false);
                dojo.disconnect(_1d4);
            });
        }
        this._preDomFilterContent(this.editNode);
        var _1d5 = this.events.concat(this.captureEvents),i = 0,et;
        while ((et = _1d5[i++])) {
            this.connect(this.document, et.toLowerCase(), et);
        }
        if (!dojo.isIE) {
            try {
                this.document.execCommand("styleWithCSS", false, false);
            } catch(e2) {
            }
        } else {
            this.connect(this.document, "onmousedown", "_onMouseDown");
            this.editNode.style.zoom = 1;
        }
        if (this.focusOnLoad) {
            setTimeout(dojo.hitch(this, "focus"), 0);
        }
        this.onDisplayChanged(e);
        if (this.onLoadDeferred) {
            this.onLoadDeferred.callback(true);
        }
    },onKeyDown:function(e) {
        if (dojo.isIE) {
            if (e.keyCode == dojo.keys.TAB && e.shiftKey && !e.ctrlKey && !e.altKey) {
                this.iframe.focus();
            } else {
                if (e.keyCode == dojo.keys.TAB && !e.shiftKey && !e.ctrlKey && !e.altKey) {
                    this.tabStop.focus();
                } else {
                    if (e.keyCode === dojo.keys.BACKSPACE &&
                        this.document.selection.type === "Control") {
                        dojo.stopEvent(e);
                        this.execCommand("delete");
                    } else {
                        if ((65 <= e.keyCode && e.keyCode <= 90) ||
                            (e.keyCode >= 37 && e.keyCode <= 40)) {
                            e.charCode = e.keyCode;
                            this.onKeyPress(e);
                        }
                    }
                }
            }
        } else {
            if (dojo.isMoz) {
                if (e.keyCode == dojo.keys.TAB && !e.shiftKey && !e.ctrlKey && !e.altKey &&
                    this.iframe) {
                    this.iframe.contentDocument.title =
                    this._localizedIframeTitles.iframeFocusTitle;
                    this.iframe.focus();
                    dojo.stopEvent(e);
                } else {
                    if (e.keyCode == dojo.keys.TAB && e.shiftKey) {
                        if (this.toolbar) {
                            this.toolbar.focus();
                        }
                        dojo.stopEvent(e);
                    }
                }
            }
        }
    },onKeyUp:function(e) {
        return;
    },KEY_CTRL:1,KEY_SHIFT:2,onKeyPress:function(e) {
        var _1db = (e.ctrlKey && !e.altKey) ? this.KEY_CTRL : 0 | e.shiftKey ? this.KEY_SHIFT : 0;
        var key = e.keyChar || e.keyCode;
        if (this._keyHandlers[key]) {
            var _1dd = this._keyHandlers[key],i = 0,h;
            while ((h = _1dd[i++])) {
                if (_1db == h.modifiers) {
                    if (!h.handler.apply(this, arguments)) {
                        e.preventDefault();
                    }
                    break;
                }
            }
        }
        setTimeout(dojo.hitch(this, function() {
            this.onKeyPressed(e);
        }), 1);
    },addKeyHandler:function(key, _1e1, _1e2) {
        if (!dojo.isArray(this._keyHandlers[key])) {
            this._keyHandlers[key] = [];
        }
        this._keyHandlers[key].push({modifiers:_1e1 || 0,handler:_1e2});
    },onKeyPressed:function(e) {
        this.onDisplayChanged();
    },onClick:function(e) {
        this.onDisplayChanged(e);
    },_onMouseDown:function(e) {
        if (!this._focused && !this.disabled) {
            this.focus();
        }
    },_onBlur:function(e) {
        this.inherited(arguments);
        var _c = this.getValue(true);
        if (_c != this.savedContent) {
            this.onChange(_c);
            this.savedContent = _c;
        }
        if (dojo.isMoz && this.iframe) {
            this.iframe.contentDocument.title = this._localizedIframeTitles.iframeEditTitle;
        }
    },_initialFocus:true,_onFocus:function(e) {
        this.inherited(arguments);
        if (dojo.isMoz && this._initialFocus) {
            this._initialFocus = false;
            if (this.editNode.innerHTML.replace(/^\s+|\s+$/g, "") == "&nbsp;") {
                this.placeCursorAtStart();
            }
        }
    },blur:function() {
        if (!dojo.isIE && this.window.document.documentElement &&
            this.window.document.documentElement.focus) {
            this.window.document.documentElement.focus();
        } else {
            if (dojo.doc.body.focus) {
                dojo.doc.body.focus();
            }
        }
    },focus:function() {
        if (!dojo.isIE) {
            dijit.focus(this.iframe);
        } else {
            if (this.editNode && this.editNode.focus) {
                this.iframe.fireEvent("onfocus", document.createEventObject());
            }
        }
    },updateInterval:200,_updateTimer:null,onDisplayChanged:function(e) {
        if (!this._updateTimer) {
            if (this._updateTimer) {
                clearTimeout(this._updateTimer);
            }
            this._updateTimer =
            setTimeout(dojo.hitch(this, this.onNormalizedDisplayChanged), this.updateInterval);
        }
    },onNormalizedDisplayChanged:function() {
        this._updateTimer = null;
    },onChange:function(_1ea) {
    },_normalizeCommand:function(cmd) {
        var _1ec = cmd.toLowerCase();
        if (_1ec == "hilitecolor" && !dojo.isMoz) {
            _1ec = "backcolor";
        }
        return _1ec;
    },queryCommandAvailable:function(_1ed) {
        var ie = 1;
        var _1ef = 1 << 1;
        var _1f0 = 1 << 2;
        var _1f1 = 1 << 3;
        var _1f2 = 1 << 4;
        var _1f3 = dojo.isSafari;
        function isSupportedBy(_1f4) {
            return {ie:Boolean(_1f4 & ie),mozilla:Boolean(_1f4 & _1ef),safari:Boolean(_1f4 &
                                                                                      _1f0),safari420:Boolean(_1f4 &
                                                                                                              _1f2),opera:Boolean(_1f4 &
                                                                                                                                  _1f1)};
        }
        ;
        var _1f5 = null;
        switch (_1ed.toLowerCase()) {case "bold":case "italic":case "underline":case "subscript":case "superscript":case "fontname":case "fontsize":case "forecolor":case "hilitecolor":case "justifycenter":case "justifyfull":case "justifyleft":case "justifyright":case "delete":case "selectall":case "toggledir":_1f5 =
                                                                                                                                                                                                                                                                                                                       isSupportedBy(_1ef |
                                                                                                                                                                                                                                                                                                                                     ie |
                                                                                                                                                                                                                                                                                                                                     _1f0 |
                                                                                                                                                                                                                                                                                                                                     _1f1);break;case "createlink":case "unlink":case "removeformat":case "inserthorizontalrule":case "insertimage":case "insertorderedlist":case "insertunorderedlist":case "indent":case "outdent":case "formatblock":case "inserthtml":case "undo":case "redo":case "strikethrough":_1f5 =
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       isSupportedBy(_1ef |
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     ie |
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     _1f1 |
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     _1f2);break;case "blockdirltr":case "blockdirrtl":case "dirltr":case "dirrtl":case "inlinedirltr":case "inlinedirrtl":_1f5 =
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           isSupportedBy(ie);break;case "cut":case "copy":case "paste":_1f5 =
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       isSupportedBy(ie |
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     _1ef |
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     _1f2);break;case "inserttable":_1f5 =
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    isSupportedBy(_1ef |
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  ie);break;case "insertcell":case "insertcol":case "insertrow":case "deletecells":case "deletecols":case "deleterows":case "mergecells":case "splitcell":_1f5 =
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          isSupportedBy(ie |
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        _1ef);break;default:return false;}
        return (dojo.isIE && _1f5.ie) || (dojo.isMoz && _1f5.mozilla) ||
               (dojo.isSafari && _1f5.safari) || (_1f3 && _1f5.safari420) ||
               (dojo.isOpera && _1f5.opera);
    },execCommand:function(_1f6, _1f7) {
        var _1f8;
        this.focus();
        _1f6 = this._normalizeCommand(_1f6);
        if (_1f7 != undefined) {
            if (_1f6 == "heading") {
                throw new Error("unimplemented");
            } else {
                if ((_1f6 == "formatblock") && dojo.isIE) {
                    _1f7 = "<" + _1f7 + ">";
                }
            }
        }
        if (_1f6 == "inserthtml") {
            _1f7 = this._preFilterContent(_1f7);
            if (dojo.isIE) {
                var _1f9 = this.document.selection.createRange();
                if (this.document.selection.type.toUpperCase() == "CONTROL") {
                    var n = _1f9.item(0);
                    while (_1f9.length) {
                        _1f9.remove(_1f9.item(0));
                    }
                    n.outerHTML = _1f7;
                } else {
                    _1f9.pasteHTML(_1f7);
                }
                _1f9.select();
                _1f8 = true;
            } else {
                if (dojo.isMoz && !_1f7.length) {
                    dojo.withGlobal(this.window, "remove", dijit._editor.selection);
                    _1f8 = true;
                } else {
                    _1f8 = this.document.execCommand(_1f6, false, _1f7);
                }
            }
        } else {
            if ((_1f6 == "unlink") && (this.queryCommandEnabled("unlink")) &&
                (dojo.isMoz || dojo.isSafari)) {
                var _1fb = this.window.getSelection();
                var a = dojo.withGlobal(this.window, "getAncestorElement", dijit._editor.selection, ["a"]);
                dojo.withGlobal(this.window, "selectElement", dijit._editor.selection, [a]);
                _1f8 = this.document.execCommand("unlink", false, null);
            } else {
                if ((_1f6 == "hilitecolor") && (dojo.isMoz)) {
                    this.document.execCommand("styleWithCSS", false, true);
                    _1f8 = this.document.execCommand(_1f6, false, _1f7);
                    this.document.execCommand("styleWithCSS", false, false);
                } else {
                    if ((dojo.isIE) && ((_1f6 == "backcolor") || (_1f6 == "forecolor"))) {
                        _1f7 = arguments.length > 1 ? _1f7 : null;
                        _1f8 = this.document.execCommand(_1f6, false, _1f7);
                    } else {
                        _1f7 = arguments.length > 1 ? _1f7 : null;
                        if (_1f7 || _1f6 != "createlink") {
                            _1f8 = this.document.execCommand(_1f6, false, _1f7);
                        }
                    }
                }
            }
        }
        this.onDisplayChanged();
        return _1f8;
    },queryCommandEnabled:function(_1fd) {
        if (this.disabled) {
            return false;
        }
        _1fd = this._normalizeCommand(_1fd);
        if (dojo.isMoz || dojo.isSafari) {
            if (_1fd == "unlink") {
                return dojo.withGlobal(this.window, "hasAncestorElement", dijit._editor.selection, ["a"]);
            } else {
                if (_1fd == "inserttable") {
                    return true;
                }
            }
        }
        if (dojo.isSafari) {
            if (_1fd == "copy") {
                _1fd = "cut";
            } else {
                if (_1fd == "paste") {
                    return true;
                }
            }
        }
        var elem = dojo.isIE ? this.document.selection.createRange() : this.document;
        return elem.queryCommandEnabled(_1fd);
    },queryCommandState:function(_1ff) {
        if (this.disabled) {
            return false;
        }
        _1ff = this._normalizeCommand(_1ff);
        return this.document.queryCommandState(_1ff);
    },queryCommandValue:function(_200) {
        if (this.disabled) {
            return false;
        }
        _200 = this._normalizeCommand(_200);
        if (dojo.isIE && _200 == "formatblock") {
            return this._local2NativeFormatNames[this.document.queryCommandValue(_200)];
        }
        return this.document.queryCommandValue(_200);
    },placeCursorAtStart:function() {
        this.focus();
        var _201 = false;
        if (dojo.isMoz) {
            var _202 = this.editNode.firstChild;
            while (_202) {
                if (_202.nodeType == 3) {
                    if (_202.nodeValue.replace(/^\s+|\s+$/g, "").length > 0) {
                        _201 = true;
                        dojo.withGlobal(this.window, "selectElement", dijit._editor.selection, [_202]);
                        break;
                    }
                } else {
                    if (_202.nodeType == 1) {
                        _201 = true;
                        dojo.withGlobal(this.window, "selectElementChildren", dijit._editor.selection, [_202]);
                        break;
                    }
                }
                _202 = _202.nextSibling;
            }
        } else {
            _201 = true;
            dojo.withGlobal(this.window, "selectElementChildren", dijit._editor.selection, [this.editNode]);
        }
        if (_201) {
            dojo.withGlobal(this.window, "collapse", dijit._editor.selection, [true]);
        }
    },placeCursorAtEnd:function() {
        this.focus();
        var _203 = false;
        if (dojo.isMoz) {
            var last = this.editNode.lastChild;
            while (last) {
                if (last.nodeType == 3) {
                    if (last.nodeValue.replace(/^\s+|\s+$/g, "").length > 0) {
                        _203 = true;
                        dojo.withGlobal(this.window, "selectElement", dijit._editor.selection, [last]);
                        break;
                    }
                } else {
                    if (last.nodeType == 1) {
                        _203 = true;
                        if (last.lastChild) {
                            dojo.withGlobal(this.window, "selectElement", dijit._editor.selection, [last.lastChild]);
                        } else {
                            dojo.withGlobal(this.window, "selectElement", dijit._editor.selection, [last]);
                        }
                        break;
                    }
                }
                last = last.previousSibling;
            }
        } else {
            _203 = true;
            dojo.withGlobal(this.window, "selectElementChildren", dijit._editor.selection, [this.editNode]);
        }
        if (_203) {
            dojo.withGlobal(this.window, "collapse", dijit._editor.selection, [false]);
        }
    },getValue:function(_205) {
        if (this.textarea) {
            if (this.isClosed || !this.isLoaded) {
                return this.textarea.value;
            }
        }
        return this._postFilterContent(null, _205);
    },setValue:function(html) {
        if (!this.isLoaded) {
            this.onLoadDeferred.addCallback(dojo.hitch(this, function() {
                this.setValue(html);
            }));
            return;
        }
        if (this.textarea && (this.isClosed || !this.isLoaded)) {
            this.textarea.value = html;
        } else {
            html = this._preFilterContent(html);
            var node = this.isClosed ? this.domNode : this.editNode;
            node.innerHTML = html;
            this._preDomFilterContent(node);
        }
        this.onDisplayChanged();
    },replaceValue:function(html) {
        if (this.isClosed) {
            this.setValue(html);
        } else {
            if (this.window && this.window.getSelection && !dojo.isMoz) {
                this.setValue(html);
            } else {
                if (this.window && this.window.getSelection) {
                    html = this._preFilterContent(html);
                    this.execCommand("selectall");
                    if (dojo.isMoz && !html) {
                        html = "&nbsp;";
                    }
                    this.execCommand("inserthtml", html);
                    this._preDomFilterContent(this.editNode);
                } else {
                    if (this.document && this.document.selection) {
                        this.setValue(html);
                    }
                }
            }
        }
    },_preFilterContent:function(html) {
        var ec = html;
        dojo.forEach(this.contentPreFilters, function(ef) {
            if (ef) {
                ec = ef(ec);
            }
        });
        return ec;
    },_preDomFilterContent:function(dom) {
        dom = dom || this.editNode;
        dojo.forEach(this.contentDomPreFilters, function(ef) {
            if (ef && dojo.isFunction(ef)) {
                ef(dom);
            }
        }, this);
    },_postFilterContent:function(dom, _20f) {
        var ec;
        if (!dojo.isString(dom)) {
            dom = dom || this.editNode;
            if (this.contentDomPostFilters.length) {
                if (_20f && dom["cloneNode"]) {
                    dom = dom.cloneNode(true);
                }
                dojo.forEach(this.contentDomPostFilters, function(ef) {
                    dom = ef(dom);
                });
            }
            ec = dijit._editor.getChildrenHtml(dom);
        } else {
            ec = dom;
        }
        if (!ec.replace(/^(?:\s|\xA0)+/g, "").replace(/(?:\s|\xA0)+$/g, "").length) {
            ec = "";
        }
        dojo.forEach(this.contentPostFilters, function(ef) {
            ec = ef(ec);
        });
        return ec;
    },_saveContent:function(e) {
        var _214 = dojo.byId(dijit._scopeName + "._editor.RichText.savedContent");
        _214.value += this._SEPARATOR + this.name + ":" + this.getValue();
    },escapeXml:function(str, _216) {
        dojo.deprecated("dijit.Editor::escapeXml is deprecated", "use dijit._editor.escapeXml instead", 2);
        return dijit._editor.escapeXml(str, _216);
    },getNodeHtml:function(node) {
        dojo.deprecated("dijit.Editor::getNodeHtml is deprecated", "use dijit._editor.getNodeHtml instead", 2);
        return dijit._editor.getNodeHtml(node);
    },getNodeChildrenHtml:function(dom) {
        dojo.deprecated("dijit.Editor::getNodeChildrenHtml is deprecated", "use dijit._editor.getChildrenHtml instead", 2);
        return dijit._editor.getChildrenHtml(dom);
    },close:function(save, _21a) {
        if (this.isClosed) {
            return false;
        }
        if (!arguments.length) {
            save = true;
        }
        this._content = this.getValue();
        var _21b = (this.savedContent != this._content);
        if (this.interval) {
            clearInterval(this.interval);
        }
        if (this.textarea) {
            with (this.textarea.style) {
                position = "";
                left = top = "";
                if (dojo.isIE) {
                    overflow = this.__overflow;
                    this.__overflow = null;
                }
            }
            this.textarea.value = save ? this._content : this.savedContent;
            dojo._destroyElement(this.domNode);
            this.domNode = this.textarea;
        } else {
            this.domNode.innerHTML = save ? this._content : this.savedContent;
        }
        dojo.removeClass(this.domNode, "RichTextEditable");
        this.isClosed = true;
        this.isLoaded = false;
        delete this.editNode;
        if (this.window && this.window._frameElement) {
            this.window._frameElement = null;
        }
        this.window = null;
        this.document = null;
        this.editingArea = null;
        this.editorObject = null;
        return _21b;
    },destroyRendering:function() {
    },destroy:function() {
        this.destroyRendering();
        if (!this.isClosed) {
            this.close(false);
        }
        this.inherited("destroy", arguments);
    },_removeMozBogus:function(html) {
        return html.replace(/\stype="_moz"/gi, "").replace(/\s_moz_dirty=""/gi, "");
    },_removeSafariBogus:function(html) {
        return html.replace(/\sclass="webkit-block-placeholder"/gi, "");
    },_fixContentForMoz:function(html) {
        return html.replace(/<(\/)?strong([ \>])/gi, "<$1b$2").replace(/<(\/)?em([ \>])/gi, "<$1i$2");
    },_srcInImgRegex:/(?:(<img(?=\s).*?\ssrc=)("|')(.*?)\2)|(?:(<img\s.*?src=)([^"'][^ >]+))/gi,_hrefInARegex:/(?:(<a(?=\s).*?\shref=)("|')(.*?)\2)|(?:(<a\s.*?href=)([^"'][^ >]+))/gi,_preFixUrlAttributes:function(
            html) {
        return html.replace(this._hrefInARegex, "$1$4$2$3$5$2 _djrealurl=$2$3$5$2").replace(this._srcInImgRegex, "$1$4$2$3$5$2 _djrealurl=$2$3$5$2");
    }});
}
if (!dojo._hasResource["dijit.Toolbar"]) {
    dojo._hasResource["dijit.Toolbar"] = true;
    dojo.provide("dijit.Toolbar");
    dojo.declare("dijit.Toolbar", [dijit._Widget,dijit._Templated,dijit._KeyNavContainer], {templateString:"<div class=\"dijit dijitToolbar\" waiRole=\"toolbar\" tabIndex=\"${tabIndex}\" dojoAttachPoint=\"containerNode\">" +
                                                                                                           "</div>",tabIndex:"0",postCreate:function() {
        this.connectKeyNavHandlers(this.isLeftToRight() ? [dojo.keys.LEFT_ARROW] :
                                   [dojo.keys.RIGHT_ARROW], this.isLeftToRight() ?
                                                            [dojo.keys.RIGHT_ARROW] :
                                                            [dojo.keys.LEFT_ARROW]);
    },startup:function() {
        if (this._started) {
            return;
        }
        this.startupKeyNavChildren();
        this.inherited(arguments);
    }});
    dojo.declare("dijit.ToolbarSeparator", [dijit._Widget,dijit._Templated], {templateString:"<div class=\"dijitToolbarSeparator dijitInline\"></div>",postCreate:function() {
        dojo.setSelectable(this.domNode, false);
    },isFocusable:function() {
        return false;
    }});
}
if (!dojo._hasResource["dijit.form.Button"]) {
    dojo._hasResource["dijit.form.Button"] = true;
    dojo.provide("dijit.form.Button");
    dojo.declare("dijit.form.Button", dijit.form._FormWidget, {label:"",showLabel:true,iconClass:"",type:"button",baseClass:"dijitButton",templateString:"<div class=\"dijit dijitReset dijitLeft dijitInline\"\n\tdojoAttachEvent=\"onclick:_onButtonClick,onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\"\n\twaiRole=\"presentation\"\n\t><button class=\"dijitReset dijitStretch dijitButtonNode dijitButtonContents\" dojoAttachPoint=\"focusNode,titleNode\"\n\t\ttype=\"${type}\" waiRole=\"button\" waiState=\"labelledby-${id}_label\"\n\t\t><span class=\"dijitReset dijitInline ${iconClass}\" dojoAttachPoint=\"iconNode\" \n \t\t\t><span class=\"dijitReset dijitToggleButtonIconChar\">&#10003;</span \n\t\t></span\n\t\t><div class=\"dijitReset dijitInline\"><center class=\"dijitReset dijitButtonText\" id=\"${id}_label\" dojoAttachPoint=\"containerNode\">${label}</center></div\n\t></button\n></div>\n",_onChangeMonitor:"",_onClick:function(
            e) {
        if (this.disabled || this.readOnly) {
            dojo.stopEvent(e);
            return false;
        }
        this._clicked();
        return this.onClick(e);
    },_onButtonClick:function(e) {
        if (this._onClick(e) === false) {
            dojo.stopEvent(e);
        } else {
            if (this.type == "submit" && !this.focusNode.form) {
                for (var node = this.domNode; node.parentNode; node = node.parentNode) {
                    var _223 = dijit.byNode(node);
                    if (_223 && typeof _223._onSubmit == "function") {
                        _223._onSubmit(e);
                        break;
                    }
                }
            }
        }
    },postCreate:function() {
        if (this.showLabel == false) {
            var _224 = "";
            this.label = this.containerNode.innerHTML;
            _224 = dojo.trim(this.containerNode.innerText || this.containerNode.textContent || "");
            this.titleNode.title = _224;
            dojo.addClass(this.containerNode, "dijitDisplayNone");
        }
        dojo.setSelectable(this.focusNode, false);
        this.inherited(arguments);
    },onClick:function(e) {
        return true;
    },_clicked:function(e) {
    },setLabel:function(_227) {
        this.containerNode.innerHTML = this.label = _227;
        this._layoutHack();
        if (this.showLabel == false) {
            this.titleNode.title =
            dojo.trim(this.containerNode.innerText || this.containerNode.textContent || "");
        }
    }});
    dojo.declare("dijit.form.DropDownButton", [dijit.form.Button,dijit._Container], {baseClass:"dijitDropDownButton",templateString:"<div class=\"dijit dijitReset dijitLeft dijitInline\"\n\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse,onclick:_onDropDownClick,onkeydown:_onDropDownKeydown,onblur:_onDropDownBlur,onkeypress:_onKey\"\n\twaiRole=\"presentation\"\n\t><div class='dijitReset dijitRight' waiRole=\"presentation\"\n\t><button class=\"dijitReset dijitStretch dijitButtonNode dijitButtonContents\" type=\"${type}\"\n\t\tdojoAttachPoint=\"focusNode,titleNode\" waiRole=\"button\" waiState=\"haspopup-true,labelledby-${id}_label\"\n\t\t><div class=\"dijitReset dijitInline ${iconClass}\" dojoAttachPoint=\"iconNode\" waiRole=\"presentation\"></div\n\t\t><div class=\"dijitReset dijitInline dijitButtonText\"  dojoAttachPoint=\"containerNode,popupStateNode\" waiRole=\"presentation\"\n\t\t\tid=\"${id}_label\">${label}</div\n\t\t><div class=\"dijitReset dijitInline dijitArrowButtonInner\" waiRole=\"presentation\">&thinsp;</div\n\t\t><div class=\"dijitReset dijitInline dijitArrowButtonChar\" waiRole=\"presentation\">&#9660;</div\n\t></button\n></div></div>\n",_fillContent:function() {
        if (this.srcNodeRef) {
            var _228 = dojo.query("*", this.srcNodeRef);
            dijit.form.DropDownButton.superclass._fillContent.call(this, _228[0]);
            this.dropDownContainer = this.srcNodeRef;
        }
    },startup:function() {
        if (this._started) {
            return;
        }
        if (!this.dropDown) {
            var _229 = dojo.query("[widgetId]", this.dropDownContainer)[0];
            this.dropDown = dijit.byNode(_229);
            delete this.dropDownContainer;
        }
        dijit.popup.prepare(this.dropDown.domNode);
        this.inherited(arguments);
    },destroyDescendants:function() {
        if (this.dropDown) {
            this.dropDown.destroyRecursive();
            delete this.dropDown;
        }
        this.inherited(arguments);
    },_onArrowClick:function(e) {
        if (this.disabled || this.readOnly) {
            return;
        }
        this._toggleDropDown();
    },_onDropDownClick:function(e) {
        var _22c = dojo.isFF && dojo.isFF < 3 && navigator.appVersion.indexOf("Macintosh") != -1;
        if (!_22c || e.detail != 0 || this._seenKeydown) {
            this._onArrowClick(e);
        }
        this._seenKeydown = false;
    },_onDropDownKeydown:function(e) {
        this._seenKeydown = true;
    },_onDropDownBlur:function(e) {
        this._seenKeydown = false;
    },_onKey:function(e) {
        if (this.disabled || this.readOnly) {
            return;
        }
        if (e.keyCode == dojo.keys.DOWN_ARROW) {
            if (!this.dropDown || this.dropDown.domNode.style.visibility == "hidden") {
                dojo.stopEvent(e);
                this._toggleDropDown();
            }
        }
    },_onBlur:function() {
        this._closeDropDown();
        this.inherited(arguments);
    },_toggleDropDown:function() {
        if (this.disabled || this.readOnly) {
            return;
        }
        dijit.focus(this.popupStateNode);
        var _230 = this.dropDown;
        if (!_230) {
            return;
        }
        if (!this._opened) {
            if (_230.href && !_230.isLoaded) {
                var self = this;
                var _232 = dojo.connect(_230, "onLoad", function() {
                    dojo.disconnect(_232);
                    self._openDropDown();
                });
                _230._loadCheck(true);
                return;
            } else {
                this._openDropDown();
            }
        } else {
            this._closeDropDown();
        }
    },_openDropDown:function() {
        var _233 = this.dropDown;
        var _234 = _233.domNode.style.width;
        var self = this;
        dijit.popup.open({parent:this,popup:_233,around:this.domNode,orient:this.isLeftToRight() ?
                                                                            {"BL":"TL","BR":"TR","TL":"BL","TR":"BR"} :
                                                                            {"BR":"TR","BL":"TL","TR":"BR","TL":"BL"},onExecute:function() {
            self._closeDropDown(true);
        },onCancel:function() {
            self._closeDropDown(true);
        },onClose:function() {
            _233.domNode.style.width = _234;
            self.popupStateNode.removeAttribute("popupActive");
            this._opened = false;
        }});
        if (this.domNode.offsetWidth > _233.domNode.offsetWidth) {
            var _236 = null;
            if (!this.isLeftToRight()) {
                _236 = _233.domNode.parentNode;
                var _237 = _236.offsetLeft + _236.offsetWidth;
            }
            dojo.marginBox(_233.domNode, {w:this.domNode.offsetWidth});
            if (_236) {
                _236.style.left = _237 - this.domNode.offsetWidth + "px";
            }
        }
        this.popupStateNode.setAttribute("popupActive", "true");
        this._opened = true;
        if (_233.focus) {
            _233.focus();
        }
    },_closeDropDown:function(_238) {
        if (this._opened) {
            dijit.popup.close(this.dropDown);
            if (_238) {
                this.focus();
            }
            this._opened = false;
        }
    }});
    dojo.declare("dijit.form.ComboButton", dijit.form.DropDownButton, {templateString:"<table class='dijit dijitReset dijitInline dijitLeft'\n\tcellspacing='0' cellpadding='0' waiRole=\"presentation\"\n\t><tbody waiRole=\"presentation\"><tr waiRole=\"presentation\"\n\t\t><td\tclass=\"dijitReset dijitStretch dijitButtonContents dijitButtonNode\"\n\t\t\ttabIndex=\"${tabIndex}\"\n\t\t\tdojoAttachEvent=\"ondijitclick:_onButtonClick,onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\"  dojoAttachPoint=\"titleNode\"\n\t\t\twaiRole=\"button\" waiState=\"labelledby-${id}_label\"\n\t\t\t><div class=\"dijitReset dijitInline ${iconClass}\" dojoAttachPoint=\"iconNode\" waiRole=\"presentation\"></div\n\t\t\t><div class=\"dijitReset dijitInline dijitButtonText\" id=\"${id}_label\" dojoAttachPoint=\"containerNode\" waiRole=\"presentation\">${label}</div\n\t\t></td\n\t\t><td class='dijitReset dijitStretch dijitButtonNode dijitArrowButton dijitDownArrowButton'\n\t\t\tdojoAttachPoint=\"popupStateNode,focusNode\"\n\t\t\tdojoAttachEvent=\"ondijitclick:_onArrowClick, onkeypress:_onKey,onmouseenter:_onMouse,onmouseleave:_onMouse\"\n\t\t\tstateModifier=\"DownArrow\"\n\t\t\ttitle=\"${optionsTitle}\" name=\"${name}\"\n\t\t\twaiRole=\"button\" waiState=\"haspopup-true\"\n\t\t\t><div class=\"dijitReset dijitArrowButtonInner\" waiRole=\"presentation\">&thinsp;</div\n\t\t\t><div class=\"dijitReset dijitArrowButtonChar\" waiRole=\"presentation\">&#9660;</div\n\t\t></td\n\t></tr></tbody\n></table>\n",attributeMap:dojo.mixin(dojo.clone(dijit.form._FormWidget.prototype.attributeMap), {id:"",name:""}),optionsTitle:"",baseClass:"dijitComboButton",_focusedNode:null,postCreate:function() {
        this.inherited(arguments);
        this._focalNodes = [this.titleNode,this.popupStateNode];
        dojo.forEach(this._focalNodes, dojo.hitch(this, function(node) {
            if (dojo.isIE) {
                this.connect(node, "onactivate", this._onNodeFocus);
                this.connect(node, "ondeactivate", this._onNodeBlur);
            } else {
                this.connect(node, "onfocus", this._onNodeFocus);
                this.connect(node, "onblur", this._onNodeBlur);
            }
        }));
    },focusFocalNode:function(node) {
        this._focusedNode = node;
        dijit.focus(node);
    },hasNextFocalNode:function() {
        return this._focusedNode !== this.getFocalNodes()[1];
    },focusNext:function() {
        this._focusedNode = this.getFocalNodes()[this._focusedNode ? 1 : 0];
        dijit.focus(this._focusedNode);
    },hasPrevFocalNode:function() {
        return this._focusedNode !== this.getFocalNodes()[0];
    },focusPrev:function() {
        this._focusedNode = this.getFocalNodes()[this._focusedNode ? 0 : 1];
        dijit.focus(this._focusedNode);
    },getFocalNodes:function() {
        return this._focalNodes;
    },_onNodeFocus:function(evt) {
        this._focusedNode = evt.currentTarget;
        var fnc = this._focusedNode == this.focusNode ? "dijitDownArrowButtonFocused" :
                  "dijitButtonContentsFocused";
        dojo.addClass(this._focusedNode, fnc);
    },_onNodeBlur:function(evt) {
        var fnc = evt.currentTarget == this.focusNode ? "dijitDownArrowButtonFocused" :
                  "dijitButtonContentsFocused";
        dojo.removeClass(evt.currentTarget, fnc);
    },_onBlur:function() {
        this.inherited(arguments);
        this._focusedNode = null;
    }});
    dojo.declare("dijit.form.ToggleButton", dijit.form.Button, {baseClass:"dijitToggleButton",checked:false,_onChangeMonitor:"checked",attributeMap:dojo.mixin(dojo.clone(dijit.form.Button.prototype.attributeMap), {checked:"focusNode"}),_clicked:function(
            evt) {
        this.setAttribute("checked", !this.checked);
    },setAttribute:function(attr, _241) {
        this.inherited(arguments);
        switch (attr) {case "checked":dijit.setWaiState(this.focusNode ||
                                                        this.domNode, "pressed", this.checked);this._setStateClass();this._handleOnChange(this.checked, true);}
    },setChecked:function(_242) {
        dojo.deprecated("setChecked(" + _242 + ") is deprecated. Use setAttribute('checked'," +
                        _242 + ") instead.", "", "2.0");
        this.setAttribute("checked", _242);
    },postCreate:function() {
        this.inherited(arguments);
        this.setAttribute("checked", this.checked);
    }});
}
if (!dojo._hasResource["dijit._editor._Plugin"]) {
    dojo._hasResource["dijit._editor._Plugin"] = true;
    dojo.provide("dijit._editor._Plugin");
    dojo.declare("dijit._editor._Plugin", null, {constructor:function(args, node) {
        if (args) {
            dojo.mixin(this, args);
        }
        this._connects = [];
    },editor:null,iconClassPrefix:"dijitEditorIcon",button:null,queryCommand:null,command:"",commandArg:null,useDefaultCommand:true,buttonClass:dijit.form.Button,getLabel:function(
            key) {
        return this.editor.commands[key];
    },_initButton:function(_246) {
        if (this.command.length) {
            var _247 = this.getLabel(this.command);
            var _248 = this.iconClassPrefix + " " + this.iconClassPrefix +
                       this.command.charAt(0).toUpperCase() + this.command.substr(1);
            if (!this.button) {
                _246 =
                dojo.mixin({label:_247,showLabel:false,iconClass:_248,dropDown:this.dropDown,tabIndex:"-1"}, _246 ||
                                                                                                             {});
                this.button = new this.buttonClass(_246);
            }
        }
    },destroy:function(f) {
        dojo.forEach(this._connects, dojo.disconnect);
    },connect:function(o, f, tf) {
        this._connects.push(dojo.connect(o, f, this, tf));
    },updateState:function() {
        var _e = this.editor;
        var _c = this.command;
        if (!_e) {
            return;
        }
        if (!_e.isLoaded) {
            return;
        }
        if (!_c.length) {
            return;
        }
        if (this.button) {
            try {
                var _24f = _e.queryCommandEnabled(_c);
                this.button.setAttribute("disabled", !_24f);
                if (typeof this.button.checked == "boolean") {
                    this.button.setAttribute("checked", _e.queryCommandState(_c));
                }
            } catch(e) {
                console.debug(e);
            }
        }
    },setEditor:function(_250) {
        this.editor = _250;
        this._initButton();
        if (this.command.length && !this.editor.queryCommandAvailable(this.command)) {
            if (this.button) {
                this.button.domNode.style.display = "none";
            }
        }
        if (this.button && this.useDefaultCommand) {
            this.connect(this.button, "onClick", dojo.hitch(this.editor, "execCommand", this.command, this.commandArg));
        }
        this.connect(this.editor, "onNormalizedDisplayChanged", "updateState");
    },setToolbar:function(_251) {
        if (this.button) {
            _251.addChild(this.button);
        }
    }});
}
if (!dojo._hasResource["dijit.Editor"]) {
    dojo._hasResource["dijit.Editor"] = true;
    dojo.provide("dijit.Editor");
    dojo.declare("dijit.Editor", dijit._editor.RichText, {plugins:null,extraPlugins:null,constructor:function() {
        if (!dojo.isArray(this.plugins)) {
            this.plugins =
            ["undo","redo","|","cut","copy","paste","|","bold","italic","underline","strikethrough","|","insertOrderedList","insertUnorderedList","indent","outdent","|","justifyLeft","justifyRight","justifyCenter","justifyFull"];
        }
        this._plugins = [];
        this._editInterval = this.editActionInterval * 1000;
    },postCreate:function() {
        if (this.customUndo) {
            dojo["require"]("dijit._editor.range");
            this._steps = this._steps.slice(0);
            this._undoedSteps = this._undoedSteps.slice(0);
        }
        if (dojo.isArray(this.extraPlugins)) {
            this.plugins = this.plugins.concat(this.extraPlugins);
        }
        this.inherited(arguments);
        this.commands = dojo.i18n.getLocalization("dijit._editor", "commands", this.lang);
        if (!this.toolbar) {
            this.toolbar = new dijit.Toolbar({});
            dojo.place(this.toolbar.domNode, this.editingArea, "before");
        }
        dojo.forEach(this.plugins, this.addPlugin, this);
        this.onNormalizedDisplayChanged();
    },destroy:function() {
        dojo.forEach(this._plugins, function(p) {
            if (p && p.destroy) {
                p.destroy();
            }
        });
        this._plugins = [];
        this.toolbar.destroy();
        delete this.toolbar;
        this.inherited(arguments);
    },addPlugin:function(_253, _254) {
        var args = dojo.isString(_253) ? {name:_253} : _253;
        if (!args.setEditor) {
            var o = {"args":args,"plugin":null,"editor":this};
            dojo.publish(dijit._scopeName + ".Editor.getPlugin", [o]);
            if (!o.plugin) {
                var pc = dojo.getObject(args.name);
                if (pc) {
                    o.plugin = new pc(args);
                }
            }
            if (!o.plugin) {
                console.warn("Cannot find plugin", _253);
                return;
            }
            _253 = o.plugin;
        }
        if (arguments.length > 1) {
            this._plugins[_254] = _253;
        } else {
            this._plugins.push(_253);
        }
        _253.setEditor(this);
        if (dojo.isFunction(_253.setToolbar)) {
            _253.setToolbar(this.toolbar);
        }
    },customUndo:dojo.isIE,editActionInterval:3,beginEditing:function(cmd) {
        if (!this._inEditing) {
            this._inEditing = true;
            this._beginEditing(cmd);
        }
        if (this.editActionInterval > 0) {
            if (this._editTimer) {
                clearTimeout(this._editTimer);
            }
            this._editTimer = setTimeout(dojo.hitch(this, this.endEditing), this._editInterval);
        }
    },_steps:[],_undoedSteps:[],execCommand:function(cmd) {
        if (this.customUndo && (cmd == "undo" || cmd == "redo")) {
            return this[cmd]();
        } else {
            try {
                if (this.customUndo) {
                    this.endEditing();
                    this._beginEditing();
                }
                var r = this.inherited("execCommand", arguments);
                if (this.customUndo) {
                    this._endEditing();
                }
                return r;
            } catch(e) {
                if (dojo.isMoz && /copy|cut|paste/.test(cmd)) {
                    var sub = dojo.string.substitute,_25c = {cut:"X",copy:"C",paste:"V"},_25d = navigator.userAgent.indexOf("Macintosh") !=
                                                                                                -1;
                    alert(sub(this.commands.systemShortcutFF, [this.commands[cmd],sub(this.commands[
                            _25d ? "appleKey" : "ctrlKey"], [_25c[cmd]])]));
                }
                return false;
            }
        }
    },queryCommandEnabled:function(cmd) {
        if (this.customUndo && (cmd == "undo" || cmd == "redo")) {
            return cmd == "undo" ? (this._steps.length > 1) : (this._undoedSteps.length > 0);
        } else {
            return this.inherited("queryCommandEnabled", arguments);
        }
    },_moveToBookmark:function(b) {
        var _260 = b;
        if (dojo.isIE) {
            if (dojo.isArray(b)) {
                _260 = [];
                dojo.forEach(b, function(n) {
                    _260.push(dijit.range.getNode(n, this.editNode));
                }, this);
            }
        } else {
            var r = dijit.range.create();
            r.setStart(dijit.range.getNode(b.startContainer, this.editNode), b.startOffset);
            r.setEnd(dijit.range.getNode(b.endContainer, this.editNode), b.endOffset);
            _260 = r;
        }
        dojo.withGlobal(this.window, "moveToBookmark", dijit, [_260]);
    },_changeToStep:function(from, to) {
        this.setValue(to.text);
        var b = to.bookmark;
        if (!b) {
            return;
        }
        this._moveToBookmark(b);
    },undo:function() {
        this.endEditing(true);
        var s = this._steps.pop();
        if (this._steps.length > 0) {
            this.focus();
            this._changeToStep(s, this._steps[this._steps.length - 1]);
            this._undoedSteps.push(s);
            this.onDisplayChanged();
            return true;
        }
        return false;
    },redo:function() {
        this.endEditing(true);
        var s = this._undoedSteps.pop();
        if (s && this._steps.length > 0) {
            this.focus();
            this._changeToStep(this._steps[this._steps.length - 1], s);
            this._steps.push(s);
            this.onDisplayChanged();
            return true;
        }
        return false;
    },endEditing:function(_268) {
        if (this._editTimer) {
            clearTimeout(this._editTimer);
        }
        if (this._inEditing) {
            this._endEditing(_268);
            this._inEditing = false;
        }
    },_getBookmark:function() {
        var b = dojo.withGlobal(this.window, dijit.getBookmark);
        var tmp = [];
        if (dojo.isIE) {
            if (dojo.isArray(b)) {
                dojo.forEach(b, function(n) {
                    tmp.push(dijit.range.getIndex(n, this.editNode).o);
                }, this);
                b = tmp;
            }
        } else {
            tmp = dijit.range.getIndex(b.startContainer, this.editNode).o;
            b = {startContainer:tmp,startOffset:b.startOffset,endContainer:
                    b.endContainer === b.startContainer ? tmp :
                    dijit.range.getIndex(b.endContainer, this.editNode).o,endOffset:b.endOffset};
        }
        return b;
    },_beginEditing:function(cmd) {
        if (this._steps.length === 0) {
            this._steps.push({"text":this.savedContent,"bookmark":this._getBookmark()});
        }
    },_endEditing:function(_26d) {
        var v = this.getValue(true);
        this._undoedSteps = [];
        this._steps.push({text:v,bookmark:this._getBookmark()});
    },onKeyDown:function(e) {
        if (!this.customUndo) {
            this.inherited("onKeyDown", arguments);
            return;
        }
        var k = e.keyCode,ks = dojo.keys;
        if (e.ctrlKey && !e.altKey) {
            if (k == 90 || k == 122) {
                dojo.stopEvent(e);
                this.undo();
                return;
            } else {
                if (k == 89 || k == 121) {
                    dojo.stopEvent(e);
                    this.redo();
                    return;
                }
            }
        }
        this.inherited("onKeyDown", arguments);
        switch (k) {case ks.ENTER:case ks.BACKSPACE:case ks.DELETE:this.beginEditing();break;case 88:case 86:if (e.ctrlKey &&
                                                                                                                 !e.altKey &&
                                                                                                                 !e.metaKey) {
            this.endEditing();
            if (e.keyCode == 88) {
                this.beginEditing("cut");
                setTimeout(dojo.hitch(this, this.endEditing), 1);
            } else {
                this.beginEditing("paste");
                setTimeout(dojo.hitch(this, this.endEditing), 1);
            }
            break;
        }default:if (!e.ctrlKey && !e.altKey && !e.metaKey &&
                     (e.keyCode < dojo.keys.F1 || e.keyCode > dojo.keys.F15)) {
            this.beginEditing();
            break;
        }case ks.ALT:this.endEditing();break;case ks.UP_ARROW:case ks.DOWN_ARROW:case ks.LEFT_ARROW:case ks.RIGHT_ARROW:case ks.HOME:case ks.END:case ks.PAGE_UP:case ks.PAGE_DOWN:this.endEditing(true);break;case ks.CTRL:case ks.SHIFT:case ks.TAB:break;}
    },_onBlur:function() {
        this.inherited("_onBlur", arguments);
        this.endEditing(true);
    },onClick:function() {
        this.endEditing(true);
        this.inherited("onClick", arguments);
    }});
    dojo.subscribe(dijit._scopeName + ".Editor.getPlugin", null, function(o) {
        if (o.plugin) {
            return;
        }
        var args = o.args,p;
        var _p = dijit._editor._Plugin;
        var name = args.name;
        switch (name) {case "undo":case "redo":case "cut":case "copy":case "paste":case "insertOrderedList":case "insertUnorderedList":case "indent":case "outdent":case "justifyCenter":case "justifyFull":case "justifyLeft":case "justifyRight":case "delete":case "selectAll":case "removeFormat":case "insertHorizontalRule":p =
                                                                                                                                                                                                                                                                                                                                  new _p({command:name});break;case "bold":case "italic":case "underline":case "strikethrough":case "subscript":case "superscript":p =
                                                                                                                                                                                                                                                                                                                                                                                                                                                                   new _p({buttonClass:dijit.form.ToggleButton,command:name});break;case "|":p =
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             new _p({button:new dijit.ToolbarSeparator()});}
        o.plugin = p;
    });
}
if (!dojo._hasResource["dijit.Menu"]) {
    dojo._hasResource["dijit.Menu"] = true;
    dojo.provide("dijit.Menu");
    dojo.declare("dijit.Menu", [dijit._Widget,dijit._Templated,dijit._KeyNavContainer], {constructor:function() {
        this._bindings = [];
    },templateString:"<table class=\"dijit dijitMenu dijitReset dijitMenuTable\" waiRole=\"menu\" dojoAttachEvent=\"onkeypress:_onKeyPress\">" +
                     "<tbody class=\"dijitReset\" dojoAttachPoint=\"containerNode\"></tbody>" +
                     "</table>",targetNodeIds:[],contextMenuForWindow:false,leftClickToOpen:false,parentMenu:null,popupDelay:500,_contextMenuWithMouse:false,postCreate:function() {
        if (this.contextMenuForWindow) {
            this.bindDomNode(dojo.body());
        } else {
            dojo.forEach(this.targetNodeIds, this.bindDomNode, this);
        }
        this.connectKeyNavHandlers([dojo.keys.UP_ARROW], [dojo.keys.DOWN_ARROW]);
    },startup:function() {
        if (this._started) {
            return;
        }
        dojo.forEach(this.getChildren(), function(_277) {
            _277.startup();
        });
        this.startupKeyNavChildren();
        this.inherited(arguments);
    },onExecute:function() {
    },onCancel:function(_278) {
    },_moveToPopup:function(evt) {
        if (this.focusedChild && this.focusedChild.popup && !this.focusedChild.disabled) {
            this.focusedChild._onClick(evt);
        }
    },_onKeyPress:function(evt) {
        if (evt.ctrlKey || evt.altKey) {
            return;
        }
        switch (evt.keyCode) {case dojo.keys.RIGHT_ARROW:this._moveToPopup(evt);dojo.stopEvent(evt);break;case dojo.keys.LEFT_ARROW:if (this.parentMenu) {
            this.onCancel(false);
        } else {
            dojo.stopEvent(evt);
        }break;}
    },onItemHover:function(item) {
        this.focusChild(item);
        if (this.focusedChild.popup && !this.focusedChild.disabled && !this.hover_timer) {
            this.hover_timer = setTimeout(dojo.hitch(this, "_openPopup"), this.popupDelay);
        }
    },_onChildBlur:function(item) {
        dijit.popup.close(item.popup);
        item._blur();
        this._stopPopupTimer();
    },onItemUnhover:function(item) {
    },_stopPopupTimer:function() {
        if (this.hover_timer) {
            clearTimeout(this.hover_timer);
            this.hover_timer = null;
        }
    },_getTopMenu:function() {
        for (var top = this; top.parentMenu; top = top.parentMenu) {
        }
        return top;
    },onItemClick:function(item, evt) {
        if (item.disabled) {
            return false;
        }
        if (item.popup) {
            if (!this.is_open) {
                this._openPopup();
            }
        } else {
            this.onExecute();
            item.onClick(evt);
        }
    },_iframeContentWindow:function(_281) {
        var win = dijit.getDocumentWindow(dijit.Menu._iframeContentDocument(_281)) ||
                  dijit.Menu._iframeContentDocument(_281)["__parent__"] ||
                  (_281.name && dojo.doc.frames[_281.name]) || null;
        return win;
    },_iframeContentDocument:function(_283) {
        var doc = _283.contentDocument || (_283.contentWindow && _283.contentWindow.document) ||
                  (_283.name && dojo.doc.frames[_283.name] &&
                   dojo.doc.frames[_283.name].document) || null;
        return doc;
    },bindDomNode:function(node) {
        node = dojo.byId(node);
        var win = dijit.getDocumentWindow(node.ownerDocument);
        if (node.tagName.toLowerCase() == "iframe") {
            win = this._iframeContentWindow(node);
            node = dojo.withGlobal(win, dojo.body);
        }
        var cn = (node == dojo.body() ? dojo.doc : node);
        node[this.id] = this._bindings.push([dojo.connect(cn, (this.leftClickToOpen) ? "onclick" :
                                                              "oncontextmenu", this, "_openMyself"),dojo.connect(cn, "onkeydown", this, "_contextKey"),dojo.connect(cn, "onmousedown", this, "_contextMouse")]);
    },unBindDomNode:function(_288) {
        var node = dojo.byId(_288);
        if (node) {
            var bid = node[this.id] - 1,b = this._bindings[bid];
            dojo.forEach(b, dojo.disconnect);
            delete this._bindings[bid];
        }
    },_contextKey:function(e) {
        this._contextMenuWithMouse = false;
        if (e.keyCode == dojo.keys.F10) {
            dojo.stopEvent(e);
            if (e.shiftKey && e.type == "keydown") {
                var _e = {target:e.target,pageX:e.pageX,pageY:e.pageY};
                _e.preventDefault = _e.stopPropagation = function() {
                };
                window.setTimeout(dojo.hitch(this, function() {
                    this._openMyself(_e);
                }), 1);
            }
        }
    },_contextMouse:function(e) {
        this._contextMenuWithMouse = true;
    },_openMyself:function(e) {
        if (this.leftClickToOpen && e.button > 0) {
            return;
        }
        dojo.stopEvent(e);
        var x,y;
        if (dojo.isSafari || this._contextMenuWithMouse) {
            x = e.pageX;
            y = e.pageY;
        } else {
            var _292 = dojo.coords(e.target, true);
            x = _292.x + 10;
            y = _292.y + 10;
        }
        var self = this;
        var _294 = dijit.getFocus(this);
        function closeAndRestoreFocus() {
            dijit.focus(_294);
            dijit.popup.close(self);
        }
        ;
        dijit.popup.open({popup:this,x:x,y:y,onExecute:closeAndRestoreFocus,onCancel:closeAndRestoreFocus,orient:
                this.isLeftToRight() ? "L" : "R"});
        this.focus();
        this._onBlur = function() {
            this.inherited("_onBlur", arguments);
            dijit.popup.close(this);
        };
    },onOpen:function(e) {
        this.isShowingNow = true;
    },onClose:function() {
        this._stopPopupTimer();
        this.parentMenu = null;
        this.isShowingNow = false;
        this.currentPopup = null;
        if (this.focusedChild) {
            this._onChildBlur(this.focusedChild);
            this.focusedChild = null;
        }
    },_openPopup:function() {
        this._stopPopupTimer();
        var _296 = this.focusedChild;
        var _297 = _296.popup;
        if (_297.isShowingNow) {
            return;
        }
        _297.parentMenu = this;
        var self = this;
        dijit.popup.open({parent:this,popup:_297,around:_296.arrowCell,orient:this.isLeftToRight() ?
                                                                              {"TR":"TL","TL":"TR"} :
                                                                              {"TL":"TR","TR":"TL"},onCancel:function() {
            dijit.popup.close(_297);
            _296.focus();
            self.currentPopup = null;
        }});
        this.currentPopup = _297;
        if (_297.focus) {
            _297.focus();
        }
    },uninitialize:function() {
        dojo.forEach(this.targetNodeIds, this.unBindDomNode, this);
        this.inherited(arguments);
    }});
    dojo.declare("dijit.MenuItem", [dijit._Widget,dijit._Templated,dijit._Contained], {templateString:"<tr class=\"dijitReset dijitMenuItem\" " +
                                                                                                      "dojoAttachEvent=\"onmouseenter:_onHover,onmouseleave:_onUnhover,ondijitclick:_onClick\">" +
                                                                                                      "<td class=\"dijitReset\"><div class=\"dijitMenuItemIcon ${iconClass}\" dojoAttachPoint=\"iconNode\"></div></td>" +
                                                                                                      "<td tabIndex=\"-1\" class=\"dijitReset dijitMenuItemLabel\" dojoAttachPoint=\"containerNode,focusNode\" waiRole=\"menuitem\"></td>" +
                                                                                                      "<td class=\"dijitReset\" dojoAttachPoint=\"arrowCell\">" +
                                                                                                      "<div class=\"dijitMenuExpand\" dojoAttachPoint=\"expand\" style=\"display:none\">" +
                                                                                                      "<span class=\"dijitInline dijitArrowNode dijitMenuExpandInner\">+</span>" +
                                                                                                      "</div>" +
                                                                                                      "</td>" +
                                                                                                      "</tr>",label:"",iconClass:"",disabled:false,postCreate:function() {
        dojo.setSelectable(this.domNode, false);
        this.setDisabled(this.disabled);
        if (this.label) {
            this.setLabel(this.label);
        }
    },_onHover:function() {
        this.getParent().onItemHover(this);
    },_onUnhover:function() {
        this.getParent().onItemUnhover(this);
    },_onClick:function(evt) {
        this.getParent().onItemClick(this, evt);
        dojo.stopEvent(evt);
    },onClick:function(evt) {
    },focus:function() {
        dojo.addClass(this.domNode, "dijitMenuItemHover");
        try {
            dijit.focus(this.containerNode);
        } catch(e) {
        }
    },_blur:function() {
        dojo.removeClass(this.domNode, "dijitMenuItemHover");
    },setLabel:function(_29b) {
        this.containerNode.innerHTML = this.label = _29b;
    },setDisabled:function(_29c) {
        this.disabled = _29c;
        dojo[_29c ? "addClass" : "removeClass"](this.domNode, "dijitMenuItemDisabled");
        dijit.setWaiState(this.containerNode, "disabled", _29c ? "true" : "false");
    }});
    dojo.declare("dijit.PopupMenuItem", dijit.MenuItem, {_fillContent:function() {
        if (this.srcNodeRef) {
            var _29d = dojo.query("*", this.srcNodeRef);
            dijit.PopupMenuItem.superclass._fillContent.call(this, _29d[0]);
            this.dropDownContainer = this.srcNodeRef;
        }
    },startup:function() {
        if (this._started) {
            return;
        }
        this.inherited(arguments);
        if (!this.popup) {
            var node = dojo.query("[widgetId]", this.dropDownContainer)[0];
            this.popup = dijit.byNode(node);
        }
        dojo.body().appendChild(this.popup.domNode);
        this.popup.domNode.style.display = "none";
        dojo.addClass(this.expand, "dijitMenuExpandEnabled");
        dojo.style(this.expand, "display", "");
        dijit.setWaiState(this.containerNode, "haspopup", "true");
    },destroyDescendants:function() {
        if (this.popup) {
            this.popup.destroyRecursive();
            delete this.popup;
        }
        this.inherited(arguments);
    }});
    dojo.declare("dijit.MenuSeparator", [dijit._Widget,dijit._Templated,dijit._Contained], {templateString:"<tr class=\"dijitMenuSeparator\"><td colspan=3>" +
                                                                                                           "<div class=\"dijitMenuSeparatorTop\"></div>" +
                                                                                                           "<div class=\"dijitMenuSeparatorBottom\"></div>" +
                                                                                                           "</td></tr>",postCreate:function() {
        dojo.setSelectable(this.domNode, false);
    },isFocusable:function() {
        return false;
    }});
}
if (!dojo._hasResource["dojo.regexp"]) {
    dojo._hasResource["dojo.regexp"] = true;
    dojo.provide("dojo.regexp");
    dojo.regexp.escapeString = function(str, _2a0) {
        return str.replace(/([\.$?*!=:|{}\(\)\[\]\\\/^])/g, function(ch) {
            if (_2a0 && _2a0.indexOf(ch) != -1) {
                return ch;
            }
            return "\\" + ch;
        });
    };
    dojo.regexp.buildGroupRE = function(arr, re, _2a4) {
        if (!(arr instanceof Array)) {
            return re(arr);
        }
        var b = [];
        for (var i = 0; i < arr.length; i++) {
            b.push(re(arr[i]));
        }
        return dojo.regexp.group(b.join("|"), _2a4);
    };
    dojo.regexp.group = function(_2a7, _2a8) {
        return "(" + (_2a8 ? "?:" : "") + _2a7 + ")";
    };
}
if (!dojo._hasResource["dojo.number"]) {
    dojo._hasResource["dojo.number"] = true;
    dojo.provide("dojo.number");
    dojo.number.format = function(_2a9, _2aa) {
        _2aa = dojo.mixin({}, _2aa || {});
        var _2ab = dojo.i18n.normalizeLocale(_2aa.locale);
        var _2ac = dojo.i18n.getLocalization("dojo.cldr", "number", _2ab);
        _2aa.customs = _2ac;
        var _2ad = _2aa.pattern || _2ac[(_2aa.type || "decimal") + "Format"];
        if (isNaN(_2a9)) {
            return null;
        }
        return dojo.number._applyPattern(_2a9, _2ad, _2aa);
    };
    dojo.number._numberPatternRE = /[#0,]*[#0](?:\.0*#*)?/;
    dojo.number._applyPattern = function(_2ae, _2af, _2b0) {
        _2b0 = _2b0 || {};
        var _2b1 = _2b0.customs.group;
        var _2b2 = _2b0.customs.decimal;
        var _2b3 = _2af.split(";");
        var _2b4 = _2b3[0];
        _2af = _2b3[(_2ae < 0) ? 1 : 0] || ("-" + _2b4);
        if (_2af.indexOf("%") != -1) {
            _2ae *= 100;
        } else {
            if (_2af.indexOf("‰") != -1) {
                _2ae *= 1000;
            } else {
                if (_2af.indexOf("¤") != -1) {
                    _2b1 = _2b0.customs.currencyGroup || _2b1;
                    _2b2 = _2b0.customs.currencyDecimal || _2b2;
                    _2af = _2af.replace(/\u00a4{1,3}/, function(_2b5) {
                        var prop = ["symbol","currency","displayName"][_2b5.length - 1];
                        return _2b0[prop] || _2b0.currency || "";
                    });
                } else {
                    if (_2af.indexOf("E") != -1) {
                        throw new Error("exponential notation not supported");
                    }
                }
            }
        }
        var _2b7 = dojo.number._numberPatternRE;
        var _2b8 = _2b4.match(_2b7);
        if (!_2b8) {
            throw new Error("unable to find a number expression in pattern: " + _2af);
        }
        return _2af.replace(_2b7, dojo.number._formatAbsolute(_2ae, _2b8[0], {decimal:_2b2,group:_2b1,places:_2b0.places}));
    };
    dojo.number.round = function(_2b9, _2ba, _2bb) {
        var _2bc = String(_2b9).split(".");
        var _2bd = (_2bc[1] && _2bc[1].length) || 0;
        if (_2bd > _2ba) {
            var _2be = Math.pow(10, _2ba);
            if (_2bb > 0) {
                _2be *= 10 / _2bb;
                _2ba++;
            }
            _2b9 = Math.round(_2b9 * _2be) / _2be;
            _2bc = String(_2b9).split(".");
            _2bd = (_2bc[1] && _2bc[1].length) || 0;
            if (_2bd > _2ba) {
                _2bc[1] = _2bc[1].substr(0, _2ba);
                _2b9 = Number(_2bc.join("."));
            }
        }
        return _2b9;
    };
    dojo.number._formatAbsolute = function(_2bf, _2c0, _2c1) {
        _2c1 = _2c1 || {};
        if (_2c1.places === true) {
            _2c1.places = 0;
        }
        if (_2c1.places === Infinity) {
            _2c1.places = 6;
        }
        var _2c2 = _2c0.split(".");
        var _2c3 = (_2c1.places >= 0) ? _2c1.places : (_2c2[1] && _2c2[1].length) || 0;
        if (!(_2c1.round < 0)) {
            _2bf = dojo.number.round(_2bf, _2c3, _2c1.round);
        }
        var _2c4 = String(Math.abs(_2bf)).split(".");
        var _2c5 = _2c4[1] || "";
        if (_2c1.places) {
            _2c4[1] = dojo.string.pad(_2c5.substr(0, _2c1.places), _2c1.places, "0", true);
        } else {
            if (_2c2[1] && _2c1.places !== 0) {
                var pad = _2c2[1].lastIndexOf("0") + 1;
                if (pad > _2c5.length) {
                    _2c4[1] = dojo.string.pad(_2c5, pad, "0", true);
                }
                var _2c7 = _2c2[1].length;
                if (_2c7 < _2c5.length) {
                    _2c4[1] = _2c5.substr(0, _2c7);
                }
            } else {
                if (_2c4[1]) {
                    _2c4.pop();
                }
            }
        }
        var _2c8 = _2c2[0].replace(",", "");
        pad = _2c8.indexOf("0");
        if (pad != -1) {
            pad = _2c8.length - pad;
            if (pad > _2c4[0].length) {
                _2c4[0] = dojo.string.pad(_2c4[0], pad);
            }
            if (_2c8.indexOf("#") == -1) {
                _2c4[0] = _2c4[0].substr(_2c4[0].length - pad);
            }
        }
        var _2c9 = _2c2[0].lastIndexOf(",");
        var _2ca,_2cb;
        if (_2c9 != -1) {
            _2ca = _2c2[0].length - _2c9 - 1;
            var _2cc = _2c2[0].substr(0, _2c9);
            _2c9 = _2cc.lastIndexOf(",");
            if (_2c9 != -1) {
                _2cb = _2cc.length - _2c9 - 1;
            }
        }
        var _2cd = [];
        for (var _2ce = _2c4[0]; _2ce;) {
            var off = _2ce.length - _2ca;
            _2cd.push((off > 0) ? _2ce.substr(off) : _2ce);
            _2ce = (off > 0) ? _2ce.slice(0, off) : "";
            if (_2cb) {
                _2ca = _2cb;
                delete _2cb;
            }
        }
        _2c4[0] = _2cd.reverse().join(_2c1.group || ",");
        return _2c4.join(_2c1.decimal || ".");
    };
    dojo.number.regexp = function(_2d0) {
        return dojo.number._parseInfo(_2d0).regexp;
    };
    dojo.number._parseInfo = function(_2d1) {
        _2d1 = _2d1 || {};
        var _2d2 = dojo.i18n.normalizeLocale(_2d1.locale);
        var _2d3 = dojo.i18n.getLocalization("dojo.cldr", "number", _2d2);
        var _2d4 = _2d1.pattern || _2d3[(_2d1.type || "decimal") + "Format"];
        var _2d5 = _2d3.group;
        var _2d6 = _2d3.decimal;
        var _2d7 = 1;
        if (_2d4.indexOf("%") != -1) {
            _2d7 /= 100;
        } else {
            if (_2d4.indexOf("‰") != -1) {
                _2d7 /= 1000;
            } else {
                var _2d8 = _2d4.indexOf("¤") != -1;
                if (_2d8) {
                    _2d5 = _2d3.currencyGroup || _2d5;
                    _2d6 = _2d3.currencyDecimal || _2d6;
                }
            }
        }
        var _2d9 = _2d4.split(";");
        if (_2d9.length == 1) {
            _2d9.push("-" + _2d9[0]);
        }
        var re = dojo.regexp.buildGroupRE(_2d9, function(_2db) {
            _2db = "(?:" + dojo.regexp.escapeString(_2db, ".") + ")";
            return _2db.replace(dojo.number._numberPatternRE, function(_2dc) {
                var _2dd = {signed:false,separator:_2d1.strict ? _2d5 :
                                                   [_2d5,""],fractional:_2d1.fractional,decimal:_2d6,exponent:false};
                var _2de = _2dc.split(".");
                var _2df = _2d1.places;
                if (_2de.length == 1 || _2df === 0) {
                    _2dd.fractional = false;
                } else {
                    if (_2df === undefined) {
                        _2df = _2de[1].lastIndexOf("0") + 1;
                    }
                    if (_2df && _2d1.fractional == undefined) {
                        _2dd.fractional = true;
                    }
                    if (!_2d1.places && (_2df < _2de[1].length)) {
                        _2df += "," + _2de[1].length;
                    }
                    _2dd.places = _2df;
                }
                var _2e0 = _2de[0].split(",");
                if (_2e0.length > 1) {
                    _2dd.groupSize = _2e0.pop().length;
                    if (_2e0.length > 1) {
                        _2dd.groupSize2 = _2e0.pop().length;
                    }
                }
                return "(" + dojo.number._realNumberRegexp(_2dd) + ")";
            });
        }, true);
        if (_2d8) {
            re = re.replace(/(\s*)(\u00a4{1,3})(\s*)/g, function(_2e1, _2e2, _2e3, _2e4) {
                var prop = ["symbol","currency","displayName"][_2e3.length - 1];
                var _2e6 = dojo.regexp.escapeString(_2d1[prop] || _2d1.currency || "");
                _2e2 = _2e2 ? "\\s" : "";
                _2e4 = _2e4 ? "\\s" : "";
                if (!_2d1.strict) {
                    if (_2e2) {
                        _2e2 += "*";
                    }
                    if (_2e4) {
                        _2e4 += "*";
                    }
                    return "(?:" + _2e2 + _2e6 + _2e4 + ")?";
                }
                return _2e2 + _2e6 + _2e4;
            });
        }
        return {regexp:re.replace(/[\xa0 ]/g, "[\\s\\xa0]"),group:_2d5,decimal:_2d6,factor:_2d7};
    };
    dojo.number.parse = function(_2e7, _2e8) {
        var info = dojo.number._parseInfo(_2e8);
        var _2ea = (new RegExp("^" + info.regexp + "$")).exec(_2e7);
        if (!_2ea) {
            return NaN;
        }
        var _2eb = _2ea[1];
        if (!_2ea[1]) {
            if (!_2ea[2]) {
                return NaN;
            }
            _2eb = _2ea[2];
            info.factor *= -1;
        }
        _2eb = _2eb.replace(new RegExp("[" + info.group + "\\s\\xa0" +
                                       "]", "g"), "").replace(info.decimal, ".");
        return Number(_2eb) * info.factor;
    };
    dojo.number._realNumberRegexp = function(_2ec) {
        _2ec = _2ec || {};
        if (!("places" in _2ec)) {
            _2ec.places = Infinity;
        }
        if (typeof _2ec.decimal != "string") {
            _2ec.decimal = ".";
        }
        if (!("fractional" in _2ec) || /^0/.test(_2ec.places)) {
            _2ec.fractional = [true,false];
        }
        if (!("exponent" in _2ec)) {
            _2ec.exponent = [true,false];
        }
        if (!("eSigned" in _2ec)) {
            _2ec.eSigned = [true,false];
        }
        var _2ed = dojo.number._integerRegexp(_2ec);
        var _2ee = dojo.regexp.buildGroupRE(_2ec.fractional, function(q) {
            var re = "";
            if (q && (_2ec.places !== 0)) {
                re = "\\" + _2ec.decimal;
                if (_2ec.places == Infinity) {
                    re = "(?:" + re + "\\d+)?";
                } else {
                    re += "\\d{" + _2ec.places + "}";
                }
            }
            return re;
        }, true);
        var _2f1 = dojo.regexp.buildGroupRE(_2ec.exponent, function(q) {
            if (q) {
                return "([eE]" + dojo.number._integerRegexp({signed:_2ec.eSigned}) + ")";
            }
            return "";
        });
        var _2f3 = _2ed + _2ee;
        if (_2ee) {
            _2f3 = "(?:(?:" + _2f3 + ")|(?:" + _2ee + "))";
        }
        return _2f3 + _2f1;
    };
    dojo.number._integerRegexp = function(_2f4) {
        _2f4 = _2f4 || {};
        if (!("signed" in _2f4)) {
            _2f4.signed = [true,false];
        }
        if (!("separator" in _2f4)) {
            _2f4.separator = "";
        } else {
            if (!("groupSize" in _2f4)) {
                _2f4.groupSize = 3;
            }
        }
        var _2f5 = dojo.regexp.buildGroupRE(_2f4.signed, function(q) {
            return q ? "[-+]" : "";
        }, true);
        var _2f7 = dojo.regexp.buildGroupRE(_2f4.separator, function(sep) {
            if (!sep) {
                return "(?:0|[1-9]\\d*)";
            }
            sep = dojo.regexp.escapeString(sep);
            if (sep == " ") {
                sep = "\\s";
            } else {
                if (sep == " ") {
                    sep = "\\s\\xa0";
                }
            }
            var grp = _2f4.groupSize,grp2 = _2f4.groupSize2;
            if (grp2) {
                var _2fb = "(?:0|[1-9]\\d{0," + (grp2 - 1) + "}(?:[" + sep + "]\\d{" + grp2 +
                           "})*[" + sep + "]\\d{" + grp + "})";
                return ((grp - grp2) > 0) ? "(?:" + _2fb + "|(?:0|[1-9]\\d{0," + (grp - 1) + "}))" :
                       _2fb;
            }
            return "(?:0|[1-9]\\d{0," + (grp - 1) + "}(?:[" + sep + "]\\d{" + grp + "})*)";
        }, true);
        return _2f5 + _2f7;
    };
}
if (!dojo._hasResource["dijit.ProgressBar"]) {
    dojo._hasResource["dijit.ProgressBar"] = true;
    dojo.provide("dijit.ProgressBar");
    dojo.declare("dijit.ProgressBar", [dijit._Widget,dijit._Templated], {progress:"0",maximum:100,places:0,indeterminate:false,templateString:"<div class=\"dijitProgressBar dijitProgressBarEmpty\"\n\t><div waiRole=\"progressbar\" tabindex=\"0\" dojoAttachPoint=\"internalProgress\" class=\"dijitProgressBarFull\"\n\t\t><div class=\"dijitProgressBarTile\"></div\n\t\t><span style=\"visibility:hidden\">&nbsp;</span\n\t></div\n\t><div dojoAttachPoint=\"label\" class=\"dijitProgressBarLabel\" id=\"${id}_label\">&nbsp;</div\n\t><img dojoAttachPoint=\"inteterminateHighContrastImage\" class=\"dijitProgressBarIndeterminateHighContrastImage\"\n\t></img\n></div>\n",_indeterminateHighContrastImagePath:dojo.moduleUrl("dijit", "themes/a11y/indeterminate_progress.gif"),postCreate:function() {
        this.inherited("postCreate", arguments);
        this.inteterminateHighContrastImage.setAttribute("src", this._indeterminateHighContrastImagePath);
        this.update();
    },update:function(_2fc) {
        dojo.mixin(this, _2fc || {});
        var _2fd = 1,_2fe;
        if (this.indeterminate) {
            _2fe = "addClass";
            dijit.removeWaiState(this.internalProgress, "valuenow");
            dijit.removeWaiState(this.internalProgress, "valuemin");
            dijit.removeWaiState(this.internalProgress, "valuemax");
        } else {
            _2fe = "removeClass";
            if (String(this.progress).indexOf("%") != -1) {
                _2fd = Math.min(parseFloat(this.progress) / 100, 1);
                this.progress = _2fd * this.maximum;
            } else {
                this.progress = Math.min(this.progress, this.maximum);
                _2fd = this.progress / this.maximum;
            }
            var text = this.report(_2fd);
            this.label.firstChild.nodeValue = text;
            dijit.setWaiState(this.internalProgress, "describedby", this.label.id);
            dijit.setWaiState(this.internalProgress, "valuenow", this.progress);
            dijit.setWaiState(this.internalProgress, "valuemin", 0);
            dijit.setWaiState(this.internalProgress, "valuemax", this.maximum);
        }
        dojo[_2fe](this.domNode, "dijitProgressBarIndeterminate");
        this.internalProgress.style.width = (_2fd * 100) + "%";
        this.onChange();
    },report:function(_300) {
        return dojo.number.format(_300, {type:"percent",places:this.places,locale:this.lang});
    },onChange:function() {
    }});
}
if (!dojo._hasResource["dijit.TitlePane"]) {
    dojo._hasResource["dijit.TitlePane"] = true;
    dojo.provide("dijit.TitlePane");
    dojo.declare("dijit.TitlePane", [dijit.layout.ContentPane,dijit._Templated], {title:"",open:true,duration:250,baseClass:"dijitTitlePane",templateString:"<div class=\"${baseClass}\">\n\t<div dojoAttachEvent=\"onclick:toggle,onkeypress: _onTitleKey,onfocus:_handleFocus,onblur:_handleFocus\" tabindex=\"0\"\n\t\t\twaiRole=\"button\" class=\"dijitTitlePaneTitle\" dojoAttachPoint=\"titleBarNode,focusNode\">\n\t\t<div dojoAttachPoint=\"arrowNode\" class=\"dijitInline dijitArrowNode\"><span dojoAttachPoint=\"arrowNodeInner\" class=\"dijitArrowNodeInner\"></span></div>\n\t\t<div dojoAttachPoint=\"titleNode\" class=\"dijitTitlePaneTextNode\"></div>\n\t</div>\n\t<div class=\"dijitTitlePaneContentOuter\" dojoAttachPoint=\"hideNode\">\n\t\t<div class=\"dijitReset\" dojoAttachPoint=\"wipeNode\">\n\t\t\t<div class=\"dijitTitlePaneContentInner\" dojoAttachPoint=\"containerNode\" waiRole=\"region\" tabindex=\"-1\">\n\t\t\t\t<!-- nested divs because wipeIn()/wipeOut() doesn't work right on node w/padding etc.  Put padding on inner div. -->\n\t\t\t</div>\n\t\t</div>\n\t</div>\n</div>\n",postCreate:function() {
        this.setTitle(this.title);
        if (!this.open) {
            this.hideNode.style.display = this.wipeNode.style.display = "none";
        }
        this._setCss();
        dojo.setSelectable(this.titleNode, false);
        this.inherited(arguments);
        dijit.setWaiState(this.containerNode, "labelledby", this.titleNode.id);
        dijit.setWaiState(this.focusNode, "haspopup", "true");
        var _301 = this.hideNode,_302 = this.wipeNode;
        this._wipeIn =
        dojo.fx.wipeIn({node:this.wipeNode,duration:this.duration,beforeBegin:function() {
            _301.style.display = "";
        }});
        this._wipeOut =
        dojo.fx.wipeOut({node:this.wipeNode,duration:this.duration,onEnd:function() {
            _301.style.display = "none";
        }});
    },setContent:function(_303) {
        if (!this.open || this._wipeOut.status() == "playing") {
            this.inherited(arguments);
        } else {
            if (this._wipeIn.status() == "playing") {
                this._wipeIn.stop();
            }
            dojo.marginBox(this.wipeNode, {h:dojo.marginBox(this.wipeNode).h});
            this.inherited(arguments);
            this._wipeIn.play();
        }
    },toggle:function() {
        dojo.forEach([this._wipeIn,this._wipeOut], function(_304) {
            if (_304.status() == "playing") {
                _304.stop();
            }
        });
        this[this.open ? "_wipeOut" : "_wipeIn"].play();
        this.open = !this.open;
        this._loadCheck();
        this._setCss();
    },_setCss:function() {
        var _305 = ["dijitClosed","dijitOpen"];
        var _306 = this.open;
        var node = this.titleBarNode || this.focusNode;
        dojo.removeClass(node, _305[!_306 + 0]);
        node.className += " " + _305[_306 + 0];
        this.arrowNodeInner.innerHTML = this.open ? "-" : "+";
    },_onTitleKey:function(e) {
        if (e.keyCode == dojo.keys.ENTER || e.charCode == dojo.keys.SPACE) {
            this.toggle();
        } else {
            if (e.keyCode == dojo.keys.DOWN_ARROW && this.open) {
                this.containerNode.focus();
                e.preventDefault();
            }
        }
    },_handleFocus:function(e) {
        dojo[(e.type == "focus" ? "addClass" : "removeClass")](this.focusNode, this.baseClass +
                                                                               "Focused");
    },setTitle:function(_30a) {
        this.titleNode.innerHTML = _30a;
    }});
}
if (!dojo._hasResource["dijit.Tooltip"]) {
    dojo._hasResource["dijit.Tooltip"] = true;
    dojo.provide("dijit.Tooltip");
    dojo.declare("dijit._MasterTooltip", [dijit._Widget,dijit._Templated], {duration:200,templateString:"<div class=\"dijitTooltip dijitTooltipLeft\" id=\"dojoTooltip\">\n\t<div class=\"dijitTooltipContainer dijitTooltipContents\" dojoAttachPoint=\"containerNode\" waiRole='alert'></div>\n\t<div class=\"dijitTooltipConnector\"></div>\n</div>\n",postCreate:function() {
        dojo.body().appendChild(this.domNode);
        this.bgIframe = new dijit.BackgroundIframe(this.domNode);
        this.fadeIn =
        dojo.fadeIn({node:this.domNode,duration:this.duration,onEnd:dojo.hitch(this, "_onShow")});
        this.fadeOut =
        dojo.fadeOut({node:this.domNode,duration:this.duration,onEnd:dojo.hitch(this, "_onHide")});
    },show:function(_30b, _30c, _30d) {
        if (this.aroundNode && this.aroundNode === _30c) {
            return;
        }
        if (this.fadeOut.status() == "playing") {
            this._onDeck = arguments;
            return;
        }
        this.containerNode.innerHTML = _30b;
        this.domNode.style.top = (this.domNode.offsetTop + 1) + "px";
        var _30e = {};
        var ltr = this.isLeftToRight();
        dojo.forEach((_30d && _30d.length) ? _30d : dijit.Tooltip.defaultPosition, function(pos) {
            switch (pos) {case "after":_30e[ltr ? "BR" : "BL"] =
                                       ltr ? "BL" : "BR";break;case "before":_30e[ltr ? "BL" :
                                                                                  "BR"] =
                                                                             ltr ? "BR" :
                                                                             "BL";break;case "below":_30e[
                    ltr ? "BL" : "BR"] = ltr ? "TL" : "TR";_30e[ltr ? "BR" : "BL"] = ltr ? "TR" :
                                                                                     "TL";break;case "above":default:_30e[
                    ltr ? "TL" : "TR"] = ltr ? "BL" : "BR";_30e[ltr ? "TR" : "TL"] =
                                                           ltr ? "BR" : "BL";break;}
        });
        var pos = dijit.placeOnScreenAroundElement(this.domNode, _30c, _30e, dojo.hitch(this, "orient"));
        dojo.style(this.domNode, "opacity", 0);
        this.fadeIn.play();
        this.isShowingNow = true;
        this.aroundNode = _30c;
    },orient:function(node, _313, _314) {
        node.className = "dijitTooltip " +
                         {"BL-TL":"dijitTooltipBelow dijitTooltipABLeft","TL-BL":"dijitTooltipAbove dijitTooltipABLeft","BR-TR":"dijitTooltipBelow dijitTooltipABRight","TR-BR":"dijitTooltipAbove dijitTooltipABRight","BR-BL":"dijitTooltipRight","BL-BR":"dijitTooltipLeft"}[_313 +
                                                                                                                                                                                                                                                                                "-" +
                                                                                                                                                                                                                                                                                _314];
    },_onShow:function() {
        if (dojo.isIE) {
            this.domNode.style.filter = "";
        }
    },hide:function(_315) {
        if (!this.aroundNode || this.aroundNode !== _315) {
            return;
        }
        if (this._onDeck) {
            this._onDeck = null;
            return;
        }
        this.fadeIn.stop();
        this.isShowingNow = false;
        this.aroundNode = null;
        this.fadeOut.play();
    },_onHide:function() {
        this.domNode.style.cssText = "";
        if (this._onDeck) {
            this.show.apply(this, this._onDeck);
            this._onDeck = null;
        }
    }});
    dijit.showTooltip = function(_316, _317, _318) {
        if (!dijit._masterTT) {
            dijit._masterTT = new dijit._MasterTooltip();
        }
        return dijit._masterTT.show(_316, _317, _318);
    };
    dijit.hideTooltip = function(_319) {
        if (!dijit._masterTT) {
            dijit._masterTT = new dijit._MasterTooltip();
        }
        return dijit._masterTT.hide(_319);
    };
    dojo.declare("dijit.Tooltip", dijit._Widget, {label:"",showDelay:400,connectId:[],position:[],postCreate:function() {
        if (this.srcNodeRef) {
            this.srcNodeRef.style.display = "none";
        }
        this._connectNodes = [];
        dojo.forEach(this.connectId, function(id) {
            var node = dojo.byId(id);
            if (node) {
                this._connectNodes.push(node);
                dojo.forEach(["onMouseOver","onMouseOut","onFocus","onBlur","onHover","onUnHover"], function(
                        _31c) {
                    this.connect(node, _31c.toLowerCase(), "_" + _31c);
                }, this);
                if (dojo.isIE) {
                    node.style.zoom = 1;
                }
            }
        }, this);
    },_onMouseOver:function(e) {
        this._onHover(e);
    },_onMouseOut:function(e) {
        if (dojo.isDescendant(e.relatedTarget, e.target)) {
            return;
        }
        this._onUnHover(e);
    },_onFocus:function(e) {
        this._focus = true;
        this._onHover(e);
        this.inherited(arguments);
    },_onBlur:function(e) {
        this._focus = false;
        this._onUnHover(e);
        this.inherited(arguments);
    },_onHover:function(e) {
        if (!this._showTimer) {
            var _322 = e.target;
            this._showTimer = setTimeout(dojo.hitch(this, function() {
                this.open(_322);
            }), this.showDelay);
        }
    },_onUnHover:function(e) {
        if (this._focus) {
            return;
        }
        if (this._showTimer) {
            clearTimeout(this._showTimer);
            delete this._showTimer;
        }
        this.close();
    },open:function(_324) {
        _324 = _324 || this._connectNodes[0];
        if (!_324) {
            return;
        }
        if (this._showTimer) {
            clearTimeout(this._showTimer);
            delete this._showTimer;
        }
        dijit.showTooltip(this.label || this.domNode.innerHTML, _324, this.position);
        this._connectNode = _324;
    },close:function() {
        dijit.hideTooltip(this._connectNode);
        delete this._connectNode;
        if (this._showTimer) {
            clearTimeout(this._showTimer);
            delete this._showTimer;
        }
    },uninitialize:function() {
        this.close();
    }});
    dijit.Tooltip.defaultPosition = ["after","before"];
}
if (!dojo._hasResource["dojo.cookie"]) {
    dojo._hasResource["dojo.cookie"] = true;
    dojo.provide("dojo.cookie");
    dojo.cookie = function(name, _326, _327) {
        var c = document.cookie;
        if (arguments.length == 1) {
            var _329 = c.match(new RegExp("(?:^|; )" + dojo.regexp.escapeString(name) +
                                          "=([^;]*)"));
            return _329 ? decodeURIComponent(_329[1]) : undefined;
        } else {
            _327 = _327 || {};
            var exp = _327.expires;
            if (typeof exp == "number") {
                var d = new Date();
                d.setTime(d.getTime() + exp * 24 * 60 * 60 * 1000);
                exp = _327.expires = d;
            }
            if (exp && exp.toUTCString) {
                _327.expires = exp.toUTCString();
            }
            _326 = encodeURIComponent(_326);
            var _32c = name + "=" + _326;
            for (propName in _327) {
                _32c += "; " + propName;
                var _32d = _327[propName];
                if (_32d !== true) {
                    _32c += "=" + _32d;
                }
            }
            document.cookie = _32c;
        }
    };
    dojo.cookie.isSupported = function() {
        if (!("cookieEnabled" in navigator)) {
            this("__djCookieTest__", "CookiesAllowed");
            navigator.cookieEnabled = this("__djCookieTest__") == "CookiesAllowed";
            if (navigator.cookieEnabled) {
                this("__djCookieTest__", "", {expires:-1});
            }
        }
        return navigator.cookieEnabled;
    };
}
if (!dojo._hasResource["dijit.Tree"]) {
    dojo._hasResource["dijit.Tree"] = true;
    dojo.provide("dijit.Tree");
    dojo.declare("dijit._TreeNode", [dijit._Widget,dijit._Templated,dijit._Container,dijit._Contained], {item:null,isTreeNode:true,label:"",isExpandable:null,isExpanded:false,state:"UNCHECKED",templateString:"<div class=\"dijitTreeNode\" waiRole=\"presentation\"\n\t><div dojoAttachPoint=\"rowNode\" waiRole=\"presentation\"\n\t\t><span dojoAttachPoint=\"expandoNode\" class=\"dijitTreeExpando\" waiRole=\"presentation\"\n\t\t></span\n\t\t><span dojoAttachPoint=\"expandoNodeText\" class=\"dijitExpandoText\" waiRole=\"presentation\"\n\t\t></span\n\t\t><div dojoAttachPoint=\"contentNode\" class=\"dijitTreeContent\" waiRole=\"presentation\">\n\t\t\t<div dojoAttachPoint=\"iconNode\" class=\"dijitInline dijitTreeIcon\" waiRole=\"presentation\"></div>\n\t\t\t<span dojoAttachPoint=\"labelNode\" class=\"dijitTreeLabel\" wairole=\"treeitem\" tabindex=\"-1\" waiState=\"selected-false\" dojoAttachEvent=\"onfocus:_onNodeFocus\"></span>\n\t\t</div\n\t></div>\n</div>\n",postCreate:function() {
        this.setLabelNode(this.label);
        this._setExpando();
        this._updateItemClasses(this.item);
        if (this.isExpandable) {
            dijit.setWaiState(this.labelNode, "expanded", this.isExpanded);
        }
    },markProcessing:function() {
        this.state = "LOADING";
        this._setExpando(true);
    },unmarkProcessing:function() {
        this._setExpando(false);
    },_updateItemClasses:function(item) {
        var tree = this.tree,_330 = tree.model;
        if (tree._v10Compat && item === _330.root) {
            item = null;
        }
        this.iconNode.className =
        "dijitInline dijitTreeIcon " + tree.getIconClass(item, this.isExpanded);
        this.labelNode.className = "dijitTreeLabel " + tree.getLabelClass(item, this.isExpanded);
    },_updateLayout:function() {
        var _331 = this.getParent();
        if (!_331 || _331.rowNode.style.display == "none") {
            dojo.addClass(this.domNode, "dijitTreeIsRoot");
        } else {
            dojo.toggleClass(this.domNode, "dijitTreeIsLast", !this.getNextSibling());
        }
    },_setExpando:function(_332) {
        var _333 = ["dijitTreeExpandoLoading","dijitTreeExpandoOpened","dijitTreeExpandoClosed","dijitTreeExpandoLeaf"];
        var idx = _332 ? 0 : (this.isExpandable ? (this.isExpanded ? 1 : 2) : 3);
        dojo.forEach(_333, function(s) {
            dojo.removeClass(this.expandoNode, s);
        }, this);
        dojo.addClass(this.expandoNode, _333[idx]);
        this.expandoNodeText.innerHTML =
        _332 ? "*" : (this.isExpandable ? (this.isExpanded ? "-" : "+") : "*");
    },expand:function() {
        if (this.isExpanded) {
            return;
        }
        if (this._wipeOut.status() == "playing") {
            this._wipeOut.stop();
        }
        this.isExpanded = true;
        dijit.setWaiState(this.labelNode, "expanded", "true");
        dijit.setWaiRole(this.containerNode, "group");
        this.contentNode.className = "dijitTreeContent dijitTreeContentExpanded";
        this._setExpando();
        this._updateItemClasses(this.item);
        this._wipeIn.play();
    },collapse:function() {
        if (!this.isExpanded) {
            return;
        }
        if (this._wipeIn.status() == "playing") {
            this._wipeIn.stop();
        }
        this.isExpanded = false;
        dijit.setWaiState(this.labelNode, "expanded", "false");
        this.contentNode.className = "dijitTreeContent";
        this._setExpando();
        this._updateItemClasses(this.item);
        this._wipeOut.play();
    },setLabelNode:function(_336) {
        this.labelNode.innerHTML = "";
        this.labelNode.appendChild(dojo.doc.createTextNode(_336));
    },setChildItems:function(_337) {
        var tree = this.tree,_339 = tree.model;
        this.getChildren().forEach(function(_33a) {
            dijit._Container.prototype.removeChild.call(this, _33a);
        }, this);
        this.state = "LOADED";
        if (_337 && _337.length > 0) {
            this.isExpandable = true;
            if (!this.containerNode) {
                this.containerNode = this.tree.containerNodeTemplate.cloneNode(true);
                this.domNode.appendChild(this.containerNode);
            }
            dojo.forEach(_337, function(item) {
                var id = _339.getIdentity(item),_33d = tree._itemNodeMap[id],node =
                        (_33d && !_33d.getParent()) ? _33d :
                        new dijit._TreeNode({item:item,tree:tree,isExpandable:_339.mayHaveChildren(item),label:tree.getLabel(item)});
                this.addChild(node);
                tree._itemNodeMap[id] = node;
                if (this.tree.persist) {
                    if (tree._openedItemIds[id]) {
                        tree._expandNode(node);
                    }
                }
            }, this);
            dojo.forEach(this.getChildren(), function(_33f, idx) {
                _33f._updateLayout();
            });
        } else {
            this.isExpandable = false;
        }
        if (this._setExpando) {
            this._setExpando(false);
        }
        if (!this.parent) {
            var fc = this.tree.showRoot ? this : this.getChildren()[0],_342 = fc ? fc.labelNode :
                                                                              this.domNode;
            _342.setAttribute("tabIndex", "0");
        }
        if (this.containerNode && !this._wipeIn) {
            this._wipeIn = dojo.fx.wipeIn({node:this.containerNode,duration:150});
            this._wipeOut = dojo.fx.wipeOut({node:this.containerNode,duration:150});
        }
    },removeChild:function(node) {
        this.inherited(arguments);
        var _344 = this.getChildren();
        if (_344.length == 0) {
            this.isExpandable = false;
            this.collapse();
        }
        dojo.forEach(_344, function(_345) {
            _345._updateLayout();
        });
    },makeExpandable:function() {
        this.isExpandable = true;
        this._setExpando(false);
    },_onNodeFocus:function(evt) {
        var node = dijit.getEnclosingWidget(evt.target);
        this.tree._onTreeFocus(node);
    }});
    dojo.declare("dijit.Tree", [dijit._Widget,dijit._Templated], {store:null,model:null,query:null,label:"",showRoot:true,childrenAttr:["children"],openOnClick:false,templateString:"<div class=\"dijitTreeContainer\" waiRole=\"tree\"\n\tdojoAttachEvent=\"onclick:_onClick,onkeypress:_onKeyPress\">\n</div>\n",isExpandable:true,isTree:true,persist:true,dndController:null,dndParams:["onDndDrop","itemCreator","onDndCancel","checkAcceptance","checkItemAcceptance"],onDndDrop:null,itemCreator:null,onDndCancel:null,checkAcceptance:null,checkItemAcceptance:null,_publish:function(
            _348, _349) {
        dojo.publish(this.id, [dojo.mixin({tree:this,event:_348}, _349 || {})]);
    },postMixInProperties:function() {
        this.tree = this;
        this._itemNodeMap = {};
        if (!this.cookieName) {
            this.cookieName = this.id + "SaveStateCookie";
        }
    },postCreate:function() {
        if (this.persist) {
            var _34a = dojo.cookie(this.cookieName);
            this._openedItemIds = {};
            if (_34a) {
                dojo.forEach(_34a.split(","), function(item) {
                    this._openedItemIds[item] = true;
                }, this);
            }
        }
        var div = dojo.doc.createElement("div");
        div.style.display = "none";
        div.className = "dijitTreeContainer";
        dijit.setWaiRole(div, "presentation");
        this.containerNodeTemplate = div;
        if (!this.model) {
            this._store2model();
        }
        this.connect(this.model, "onChange", "_onItemChange");
        this.connect(this.model, "onChildrenChange", "_onItemChildrenChange");
        this._load();
        this.inherited("postCreate", arguments);
        if (this.dndController) {
            if (dojo.isString(this.dndController)) {
                this.dndController = dojo.getObject(this.dndController);
            }
            var _34d = {};
            for (var i = 0; i < this.dndParams.length; i++) {
                if (this[this.dndParams[i]]) {
                    _34d[this.dndParams[i]] = this[this.dndParams[i]];
                }
            }
            this.dndController = new this.dndController(this, _34d);
        }
    },_store2model:function() {
        this._v10Compat = true;
        dojo.deprecated("Tree: from version 2.0, should specify a model object rather than a store/query");
        var _34f = {id:this.id +
                       "_ForestStoreModel",store:this.store,query:this.query,childrenAttrs:this.childrenAttr};
        if (this.params.mayHaveChildren) {
            _34f.mayHaveChildren = dojo.hitch(this, "mayHaveChildren");
        }
        if (this.params.getItemChildren) {
            _34f.getChildren = dojo.hitch(this, function(item, _351, _352) {
                this.getItemChildren((this._v10Compat && item === this.model.root) ? null :
                                     item, _351, _352);
            });
        }
        this.model = new dijit.tree.ForestStoreModel(_34f);
        this.showRoot = Boolean(this.label);
    },_load:function() {
        this.model.getRoot(dojo.hitch(this, function(item) {
            var rn = this.rootNode =
                     new dijit._TreeNode({item:item,tree:this,isExpandable:true,label:this.label ||
                                                                                      this.getLabel(item)});
            if (!this.showRoot) {
                rn.rowNode.style.display = "none";
            }
            this.domNode.appendChild(rn.domNode);
            this._itemNodeMap[this.model.getIdentity(item)] = rn;
            rn._updateLayout();
            this._expandNode(rn);
        }), function(err) {
            console.error(this, ": error loading root: ", err);
        });
    },mayHaveChildren:function(item) {
    },getItemChildren:function(_357, _358) {
    },getLabel:function(item) {
        return this.model.getLabel(item);
    },getIconClass:function(item, _35b) {
        return (!item || this.model.mayHaveChildren(item)) ?
               (_35b ? "dijitFolderOpened" : "dijitFolderClosed") : "dijitLeaf";
    },getLabelClass:function(item, _35d) {
    },_onKeyPress:function(e) {
        if (e.altKey) {
            return;
        }
        var _35f = dijit.getEnclosingWidget(e.target);
        if (!_35f) {
            return;
        }
        if (e.charCode) {
            var _360 = e.charCode;
            if (!e.altKey && !e.ctrlKey && !e.shiftKey && !e.metaKey) {
                _360 = (String.fromCharCode(_360)).toLowerCase();
                this._onLetterKeyNav({node:_35f,key:_360});
                dojo.stopEvent(e);
            }
        } else {
            var map = this._keyHandlerMap;
            if (!map) {
                map = {};
                map[dojo.keys.ENTER] = "_onEnterKey";
                map[this.isLeftToRight() ? dojo.keys.LEFT_ARROW : dojo.keys.RIGHT_ARROW] =
                "_onLeftArrow";
                map[this.isLeftToRight() ? dojo.keys.RIGHT_ARROW : dojo.keys.LEFT_ARROW] =
                "_onRightArrow";
                map[dojo.keys.UP_ARROW] = "_onUpArrow";
                map[dojo.keys.DOWN_ARROW] = "_onDownArrow";
                map[dojo.keys.HOME] = "_onHomeKey";
                map[dojo.keys.END] = "_onEndKey";
                this._keyHandlerMap = map;
            }
            if (this._keyHandlerMap[e.keyCode]) {
                this[this._keyHandlerMap[e.keyCode]]({node:_35f,item:_35f.item});
                dojo.stopEvent(e);
            }
        }
    },_onEnterKey:function(_362) {
        this._publish("execute", {item:_362.item,node:_362.node});
        this.onClick(_362.item, _362.node);
    },_onDownArrow:function(_363) {
        var node = this._getNextNode(_363.node);
        if (node && node.isTreeNode) {
            this.focusNode(node);
        }
    },_onUpArrow:function(_365) {
        var node = _365.node;
        var _367 = node.getPreviousSibling();
        if (_367) {
            node = _367;
            while (node.isExpandable && node.isExpanded && node.hasChildren()) {
                var _368 = node.getChildren();
                node = _368[_368.length - 1];
            }
        } else {
            var _369 = node.getParent();
            if (!(!this.showRoot && _369 === this.rootNode)) {
                node = _369;
            }
        }
        if (node && node.isTreeNode) {
            this.focusNode(node);
        }
    },_onRightArrow:function(_36a) {
        var node = _36a.node;
        if (node.isExpandable && !node.isExpanded) {
            this._expandNode(node);
        } else {
            if (node.hasChildren()) {
                node = node.getChildren()[0];
                if (node && node.isTreeNode) {
                    this.focusNode(node);
                }
            }
        }
    },_onLeftArrow:function(_36c) {
        var node = _36c.node;
        if (node.isExpandable && node.isExpanded) {
            this._collapseNode(node);
        } else {
            node = node.getParent();
            if (node && node.isTreeNode) {
                this.focusNode(node);
            }
        }
    },_onHomeKey:function() {
        var node = this._getRootOrFirstNode();
        if (node) {
            this.focusNode(node);
        }
    },_onEndKey:function(_36f) {
        var node = this;
        while (node.isExpanded) {
            var c = node.getChildren();
            node = c[c.length - 1];
        }
        if (node && node.isTreeNode) {
            this.focusNode(node);
        }
    },_onLetterKeyNav:function(_372) {
        var node = startNode = _372.node,key = _372.key;
        do{
            node = this._getNextNode(node);
            if (!node) {
                node = this._getRootOrFirstNode();
            }
        } while (node !== startNode && (node.label.charAt(0).toLowerCase() != key));
        if (node && node.isTreeNode) {
            if (node !== startNode) {
                this.focusNode(node);
            }
        }
    },_onClick:function(e) {
        var _376 = e.target;
        var _377 = dijit.getEnclosingWidget(_376);
        if (!_377 || !_377.isTreeNode) {
            return;
        }
        if ((this.openOnClick && _377.isExpandable) ||
            (_376 == _377.expandoNode || _376 == _377.expandoNodeText)) {
            if (_377.isExpandable) {
                this._onExpandoClick({node:_377});
            }
        } else {
            this._publish("execute", {item:_377.item,node:_377});
            this.onClick(_377.item, _377);
            this.focusNode(_377);
        }
        dojo.stopEvent(e);
    },_onExpandoClick:function(_378) {
        var node = _378.node;
        this.focusNode(node);
        if (node.isExpanded) {
            this._collapseNode(node);
        } else {
            this._expandNode(node);
        }
    },onClick:function(item, node) {
    },_getNextNode:function(node) {
        if (node.isExpandable && node.isExpanded && node.hasChildren()) {
            return node.getChildren()[0];
        } else {
            while (node && node.isTreeNode) {
                var _37d = node.getNextSibling();
                if (_37d) {
                    return _37d;
                }
                node = node.getParent();
            }
            return null;
        }
    },_getRootOrFirstNode:function() {
        return this.showRoot ? this.rootNode : this.rootNode.getChildren()[0];
    },_collapseNode:function(node) {
        if (node.isExpandable) {
            if (node.state == "LOADING") {
                return;
            }
            node.collapse();
            if (this.persist && node.item) {
                delete this._openedItemIds[this.model.getIdentity(node.item)];
                this._saveState();
            }
        }
    },_expandNode:function(node) {
        if (!node.isExpandable) {
            return;
        }
        var _380 = this.model,item = node.item;
        switch (node.state) {case "LOADING":return;case "UNCHECKED":node.markProcessing();var _382 = this;_380.getChildren(item, function(
                _383) {
            node.unmarkProcessing();
            node.setChildItems(_383);
            _382._expandNode(node);
        }, function(err) {
            console.error(_382, ": error loading root children: ", err);
        });break;default:node.expand();if (this.persist && item) {
            this._openedItemIds[_380.getIdentity(item)] = true;
            this._saveState();
        }}
    },blurNode:function() {
        var node = this.lastFocused;
        if (!node) {
            return;
        }
        var _386 = node.labelNode;
        dojo.removeClass(_386, "dijitTreeLabelFocused");
        _386.setAttribute("tabIndex", "-1");
        dijit.setWaiState(_386, "selected", false);
        this.lastFocused = null;
    },focusNode:function(node) {
        node.labelNode.focus();
    },_onBlur:function() {
        this.inherited(arguments);
        if (this.lastFocused) {
            var _388 = this.lastFocused.labelNode;
            dojo.removeClass(_388, "dijitTreeLabelFocused");
        }
    },_onTreeFocus:function(node) {
        if (node) {
            if (node != this.lastFocused) {
                this.blurNode();
            }
            var _38a = node.labelNode;
            _38a.setAttribute("tabIndex", "0");
            dijit.setWaiState(_38a, "selected", true);
            dojo.addClass(_38a, "dijitTreeLabelFocused");
            this.lastFocused = node;
        }
    },_onItemDelete:function(item) {
        var _38c = this.model.getIdentity(item);
        var node = this._itemNodeMap[_38c];
        if (node) {
            var _38e = node.getParent();
            if (_38e) {
                _38e.removeChild(node);
            }
            delete this._itemNodeMap[_38c];
            node.destroyRecursive();
        }
    },_onItemChange:function(item) {
        var _390 = this.model,_391 = _390.getIdentity(item),node = this._itemNodeMap[_391];
        if (node) {
            node.setLabelNode(this.getLabel(item));
            node._updateItemClasses(item);
        }
    },_onItemChildrenChange:function(_393, _394) {
        var _395 = this.model,_396 = _395.getIdentity(_393),_397 = this._itemNodeMap[_396];
        if (_397) {
            _397.setChildItems(_394);
        }
    },_saveState:function() {
        if (!this.persist) {
            return;
        }
        var ary = [];
        for (var id in this._openedItemIds) {
            ary.push(id);
        }
        dojo.cookie(this.cookieName, ary.join(","));
    },destroy:function() {
        if (this.rootNode) {
            this.rootNode.destroyRecursive();
        }
        this.rootNode = null;
        this.inherited(arguments);
    },destroyRecursive:function() {
        this.destroy();
    }});
    dojo.declare("dijit.tree.TreeStoreModel", null, {store:null,childrenAttrs:["children"],root:null,query:null,constructor:function(
            args) {
        dojo.mixin(this, args);
        this.connects = [];
        var _39b = this.store;
        if (!_39b.getFeatures()["dojo.data.api.Identity"]) {
            throw new Error("dijit.Tree: store must support dojo.data.Identity");
        }
        if (_39b.getFeatures()["dojo.data.api.Notification"]) {
            this.connects =
            this.connects.concat([dojo.connect(_39b, "onNew", this, "_onNewItem"),dojo.connect(_39b, "onDelete", this, "_onDeleteItem"),dojo.connect(_39b, "onSet", this, "_onSetItem")]);
        }
    },destroy:function() {
        dojo.forEach(this.connects, dojo.disconnect);
    },getRoot:function(_39c, _39d) {
        if (this.root) {
            _39c(this.root);
        } else {
            this.store.fetch({query:this.query,onComplete:dojo.hitch(this, function(_39e) {
                if (_39e.length != 1) {
                    throw new Error(this.declaredClass + ": query " + query + " returned " +
                                    _39e.length + " items, but must return exactly one item");
                }
                this.root = _39e[0];
                _39c(this.root);
            }),onError:_39d});
        }
    },mayHaveChildren:function(item) {
        return dojo.some(this.childrenAttrs, function(attr) {
            return this.store.hasAttribute(item, attr);
        }, this);
    },getChildren:function(_3a1, _3a2, _3a3) {
        var _3a4 = this.store;
        var _3a5 = [];
        for (var i = 0; i < this.childrenAttrs.length; i++) {
            var vals = _3a4.getValues(_3a1, this.childrenAttrs[i]);
            _3a5 = _3a5.concat(vals);
        }
        var _3a8 = 0;
        dojo.forEach(_3a5, function(item) {
            if (!_3a4.isItemLoaded(item)) {
                _3a8++;
            }
        });
        if (_3a8 == 0) {
            _3a2(_3a5);
        } else {
            var _3aa = function _3aa(item) {
                if (--_3a8 == 0) {
                    _3a2(_3a5);
                }
            };
            dojo.forEach(_3a5, function(item) {
                if (!_3a4.isItemLoaded(item)) {
                    _3a4.loadItem({item:item,onItem:_3aa,onError:_3a3});
                }
            });
        }
    },getIdentity:function(item) {
        return this.store.getIdentity(item);
    },getLabel:function(item) {
        return this.store.getLabel(item);
    },newItem:function(args, _3b0) {
        var _3b1 = {parent:_3b0,attribute:this.childrenAttrs[0]};
        return this.store.newItem(args, _3b1);
    },pasteItem:function(_3b2, _3b3, _3b4, _3b5) {
        var _3b6 = this.store,_3b7 = this.childrenAttrs[0];
        if (_3b3) {
            dojo.forEach(this.childrenAttrs, function(attr) {
                if (_3b6.containsValue(_3b3, attr, _3b2)) {
                    if (!_3b5) {
                        var _3b9 = dojo.filter(_3b6.getValues(_3b3, attr), function(x) {
                            return x != _3b2;
                        });
                        _3b6.setValues(_3b3, attr, _3b9);
                    }
                    _3b7 = attr;
                }
            });
        }
        if (_3b4) {
            _3b6.setValues(_3b4, _3b7, _3b6.getValues(_3b4, _3b7).concat(_3b2));
        }
    },onChange:function(item) {
    },onChildrenChange:function(_3bc, _3bd) {
    },_onNewItem:function(item, _3bf) {
        if (!_3bf) {
            return;
        }
        this.getChildren(_3bf.item, dojo.hitch(this, function(_3c0) {
            this.onChildrenChange(_3bf.item, _3c0);
        }));
    },_onDeleteItem:function(item) {
    },_onSetItem:function(item, _3c3, _3c4, _3c5) {
        if (dojo.indexOf(this.childrenAttrs, _3c3) != -1) {
            this.getChildren(item, dojo.hitch(this, function(_3c6) {
                this.onChildrenChange(item, _3c6);
            }));
        } else {
            this.onChange(item);
        }
    }});
    dojo.declare("dijit.tree.ForestStoreModel", dijit.tree.TreeStoreModel, {rootId:"$root$",rootLabel:"ROOT",query:null,constructor:function(
            _3c7) {
        this.root =
        {store:this,root:true,id:_3c7.rootId,label:_3c7.rootLabel,children:_3c7.rootChildren};
    },mayHaveChildren:function(item) {
        return item === this.root || this.inherited(arguments);
    },getChildren:function(_3c9, _3ca, _3cb) {
        if (_3c9 === this.root) {
            if (this.root.children) {
                _3ca(this.root.children);
            } else {
                this.store.fetch({query:this.query,onComplete:dojo.hitch(this, function(_3cc) {
                    this.root.children = _3cc;
                    _3ca(_3cc);
                }),onError:_3cb});
            }
        } else {
            this.inherited(arguments);
        }
    },getIdentity:function(item) {
        return (item === this.root) ? this.root.id : this.inherited(arguments);
    },getLabel:function(item) {
        return (item === this.root) ? this.root.label : this.inherited(arguments);
    },newItem:function(args, _3d0) {
        if (_3d0 === this.root) {
            this.onNewRootItem(args);
            return this.store.newItem(args);
        } else {
            return this.inherited(arguments);
        }
    },onNewRootItem:function(args) {
    },pasteItem:function(_3d2, _3d3, _3d4, _3d5) {
        if (_3d3 === this.root) {
            if (!_3d5) {
                this.onLeaveRoot(_3d2);
            }
        }
        dijit.tree.TreeStoreModel.prototype.pasteItem.call(this, _3d2, _3d3 === this.root ? null :
                                                                       _3d3, _3d4 === this.root ?
                                                                             null : _3d4);
        if (_3d4 === this.root) {
            this.onAddToRoot(_3d2);
        }
    },onAddToRoot:function(item) {
        console.log(this, ": item ", item, " added to root");
    },onLeaveRoot:function(item) {
        console.log(this, ": item ", item, " removed from root");
    },_requeryTop:function() {
        var _3d8 = this,_3d9 = this.root.children;
        this.store.fetch({query:this.query,onComplete:function(_3da) {
            _3d8.root.children = _3da;
            if (_3d9.length != _3da.length || dojo.some(_3d9, function(item, idx) {
                return _3da[idx] != item;
            })) {
                _3d8.onChildrenChange(_3d8.root, _3da);
            }
        }});
    },_onNewItem:function(item, _3de) {
        this._requeryTop();
        this.inherited(arguments);
    },_onDeleteItem:function(item) {
        if (dojo.indexOf(this.root.children, item) != -1) {
            this._requeryTop();
        }
        this.inherited(arguments);
    }});
}
if (!dojo._hasResource["dijit.form.TextBox"]) {
    dojo._hasResource["dijit.form.TextBox"] = true;
    dojo.provide("dijit.form.TextBox");
    dojo.declare("dijit.form.TextBox", dijit.form._FormValueWidget, {trim:false,uppercase:false,lowercase:false,propercase:false,maxLength:"",templateString:"<input class=\"dijit dijitReset dijitLeft\" dojoAttachPoint='textbox,focusNode' name=\"${name}\"\n\tdojoAttachEvent='onmouseenter:_onMouse,onmouseleave:_onMouse,onfocus:_onMouse,onblur:_onMouse,onkeypress:_onKeyPress,onkeyup'\n\tautocomplete=\"off\" type=\"${type}\"\n\t/>\n",baseClass:"dijitTextBox",attributeMap:dojo.mixin(dojo.clone(dijit.form._FormValueWidget.prototype.attributeMap), {maxLength:"focusNode"}),getDisplayedValue:function() {
        return this.filter(this.textbox.value);
    },getValue:function() {
        return this.parse(this.getDisplayedValue(), this.constraints);
    },setValue:function(_3e0, _3e1, _3e2) {
        var _3e3 = this.filter(_3e0);
        if ((((typeof _3e3 == typeof _3e0) && (_3e0 !== undefined)) || (_3e0 === null)) &&
            (_3e2 == null || _3e2 == undefined)) {
            _3e2 = this.format(_3e3, this.constraints);
        }
        if (_3e2 != null && _3e2 != undefined) {
            this.textbox.value = _3e2;
        }
        dijit.form.TextBox.superclass.setValue.call(this, _3e3, _3e1);
    },setDisplayedValue:function(_3e4, _3e5) {
        this.textbox.value = _3e4;
        this.setValue(this.getValue(), _3e5);
    },format:function(_3e6, _3e7) {
        return ((_3e6 == null || _3e6 == undefined) ? "" :
                (_3e6.toString ? _3e6.toString() : _3e6));
    },parse:function(_3e8, _3e9) {
        return _3e8;
    },postCreate:function() {
        this.textbox.setAttribute("value", this.getDisplayedValue());
        this.inherited(arguments);
        this._layoutHack();
    },filter:function(val) {
        if (val === null || val === undefined) {
            return "";
        } else {
            if (typeof val != "string") {
                return val;
            }
        }
        if (this.trim) {
            val = dojo.trim(val);
        }
        if (this.uppercase) {
            val = val.toUpperCase();
        }
        if (this.lowercase) {
            val = val.toLowerCase();
        }
        if (this.propercase) {
            val = val.replace(/[^\s]+/g, function(word) {
                return word.substring(0, 1).toUpperCase() + word.substring(1);
            });
        }
        return val;
    },_setBlurValue:function() {
        this.setValue(this.getValue(), (this.isValid ? this.isValid() : true));
    },_onBlur:function() {
        this._setBlurValue();
        this.inherited(arguments);
    },onkeyup:function() {
    }});
    dijit.selectInputText = function(_3ec, _3ed, stop) {
        var _3ef = dojo.global;
        var _3f0 = dojo.doc;
        _3ec = dojo.byId(_3ec);
        if (isNaN(_3ed)) {
            _3ed = 0;
        }
        if (isNaN(stop)) {
            stop = _3ec.value ? _3ec.value.length : 0;
        }
        _3ec.focus();
        if (_3f0["selection"] && dojo.body()["createTextRange"]) {
            if (_3ec.createTextRange) {
                var _3f1 = _3ec.createTextRange();
                with (_3f1) {
                    collapse(true);
                    moveStart("character", _3ed);
                    moveEnd("character", stop);
                    select();
                }
            }
        } else {
            if (_3ef["getSelection"]) {
                var _3f2 = _3ef.getSelection();
                if (_3ec.setSelectionRange) {
                    _3ec.setSelectionRange(_3ed, stop);
                }
            }
        }
    };
}
if (!dojo._hasResource["dijit.InlineEditBox"]) {
    dojo._hasResource["dijit.InlineEditBox"] = true;
    dojo.provide("dijit.InlineEditBox");
    dojo.declare("dijit.InlineEditBox", dijit._Widget, {editing:false,autoSave:true,buttonSave:"",buttonCancel:"",renderAsHtml:false,editor:"dijit.form.TextBox",editorParams:{},onChange:function(
            _3f3) {
    },width:"100%",value:"",noValueIndicator:"<span style='font-family: wingdings; text-decoration: underline;'>&nbsp;&nbsp;&nbsp;&nbsp;&#x270d;&nbsp;&nbsp;&nbsp;&nbsp;</span>",postMixInProperties:function() {
        this.inherited("postMixInProperties", arguments);
        this.displayNode = this.srcNodeRef;
        var _3f4 = {ondijitclick:"_onClick",onmouseover:"_onMouseOver",onmouseout:"_onMouseOut",onfocus:"_onMouseOver",onblur:"_onMouseOut"};
        for (var name in _3f4) {
            this.connect(this.displayNode, name, _3f4[name]);
        }
        dijit.setWaiRole(this.displayNode, "button");
        if (!this.displayNode.getAttribute("tabIndex")) {
            this.displayNode.setAttribute("tabIndex", 0);
        }
        this.setValue(this.value || this.displayNode.innerHTML);
    },setDisabled:function(_3f6) {
        this.disabled = _3f6;
        dijit.setWaiState(this.focusNode || this.domNode, "disabled", _3f6);
    },_onMouseOver:function() {
        dojo.addClass(this.displayNode, this.disabled ? "dijitDisabledClickableRegion" :
                                        "dijitClickableRegion");
    },_onMouseOut:function() {
        dojo.removeClass(this.displayNode, this.disabled ? "dijitDisabledClickableRegion" :
                                           "dijitClickableRegion");
    },_onClick:function(e) {
        if (this.disabled) {
            return;
        }
        if (e) {
            dojo.stopEvent(e);
        }
        this._onMouseOut();
        setTimeout(dojo.hitch(this, "_edit"), 0);
    },_edit:function() {
        this.editing = true;
        var _3f8 = (this.renderAsHtml ? this.value :
                    this.value.replace(/\s*\r?\n\s*/g, "").replace(/<br\/?>/gi, "\n").replace(/&gt;/g, ">").replace(/&lt;/g, "<").replace(/&amp;/g, "&"));
        var _3f9 = dojo.doc.createElement("span");
        dojo.place(_3f9, this.domNode, "before");
        var ew = this.editWidget =
                 new dijit._InlineEditor({value:dojo.trim(_3f8),autoSave:this.autoSave,buttonSave:this.buttonSave,buttonCancel:this.buttonCancel,renderAsHtml:this.renderAsHtml,editor:this.editor,editorParams:this.editorParams,style:dojo.getComputedStyle(this.displayNode),save:dojo.hitch(this, "save"),cancel:dojo.hitch(this, "cancel"),width:this.width}, _3f9);
        var ews = ew.domNode.style;
        this.displayNode.style.display = "none";
        ews.position = "static";
        ews.visibility = "visible";
        this.domNode = ew.domNode;
        setTimeout(function() {
            ew.focus();
        }, 100);
    },_showText:function(_3fc) {
        this.displayNode.style.display = "";
        var ew = this.editWidget;
        var ews = ew.domNode.style;
        ews.position = "absolute";
        ews.visibility = "hidden";
        this.domNode = this.displayNode;
        if (_3fc) {
            dijit.focus(this.displayNode);
        }
        ews.display = "none";
        setTimeout(function() {
            ew.destroy();
            delete ew;
            if (dojo.isIE) {
                dijit.focus(dijit.getFocus());
            }
        }, 1000);
    },save:function(_3ff) {
        this.editing = false;
        var _400 = this.editWidget.getValue() + "";
        if (!this.renderAsHtml) {
            _400 =
            _400.replace(/&/gm, "&amp;").replace(/</gm, "&lt;").replace(/>/gm, "&gt;").replace(/"/gm, "&quot;").replace(/\n/g, "<br>");
        }
        this.setValue(_400);
        this.onChange(_400);
        this._showText(_3ff);
    },setValue:function(val) {
        this.value = val;
        this.displayNode.innerHTML = dojo.trim(val) || this.noValueIndicator;
    },getValue:function() {
        return this.value;
    },cancel:function(_402) {
        this.editing = false;
        this._showText(_402);
    }});
    dojo.declare("dijit._InlineEditor", [dijit._Widget,dijit._Templated], {templateString:"<fieldset dojoAttachPoint=\"editNode\" waiRole=\"presentation\" style=\"position: absolute; visibility:hidden\" class=\"dijitReset dijitInline\"\n\tdojoAttachEvent=\"onkeypress: _onKeyPress\" \n\t><input dojoAttachPoint=\"editorPlaceholder\"\n\t/><span dojoAttachPoint=\"buttonContainer\"\n\t\t><button class='saveButton' dojoAttachPoint=\"saveButton\" dojoType=\"dijit.form.Button\" dojoAttachEvent=\"onClick:save\" disabled=\"true\">${buttonSave}</button\n\t\t><button class='cancelButton' dojoAttachPoint=\"cancelButton\" dojoType=\"dijit.form.Button\" dojoAttachEvent=\"onClick:cancel\">${buttonCancel}</button\n\t></span\n></fieldset>\n",widgetsInTemplate:true,postMixInProperties:function() {
        this.inherited("postMixInProperties", arguments);
        this.messages = dojo.i18n.getLocalization("dijit", "common", this.lang);
        dojo.forEach(["buttonSave","buttonCancel"], function(prop) {
            if (!this[prop]) {
                this[prop] = this.messages[prop];
            }
        }, this);
    },postCreate:function() {
        var cls = dojo.getObject(this.editor);
        var ew = this.editWidget = new cls(this.editorParams, this.editorPlaceholder);
        var _406 = this.style;
        dojo.forEach(["fontWeight","fontFamily","fontSize","fontStyle"], function(prop) {
            ew.focusNode.style[prop] = _406[prop];
        }, this);
        dojo.forEach(["marginTop","marginBottom","marginLeft","marginRight"], function(prop) {
            this.domNode.style[prop] = _406[prop];
        }, this);
        if (this.width == "100%") {
            ew.domNode.style.width = "100%";
            this.domNode.style.display = "block";
        } else {
            ew.domNode.style.width = this.width + (Number(this.width) == this.width ? "px" : "");
        }
        this.connect(ew, "onChange", "_onChange");
        this.connect(ew.focusNode || ew.domNode, "onkeypress", "_onKeyPress");
        (this.editWidget.setDisplayedValue ||
         this.editWidget.setValue).call(this.editWidget, this.value, false);
        this._initialText = this.getValue();
        if (this.autoSave) {
            this.buttonContainer.style.display = "none";
        }
    },destroy:function() {
        this.editWidget.destroy();
        this.inherited(arguments);
    },getValue:function() {
        var ew = this.editWidget;
        return ew.getDisplayedValue ? ew.getDisplayedValue() : ew.getValue();
    },_onKeyPress:function(e) {
        if (this._exitInProgress) {
            return;
        }
        if (this.autoSave) {
            if (e.altKey || e.ctrlKey) {
                return;
            }
            if (e.keyCode == dojo.keys.ESCAPE) {
                dojo.stopEvent(e);
                this._exitInProgress = true;
                this.cancel(true);
            } else {
                if (e.keyCode == dojo.keys.ENTER) {
                    dojo.stopEvent(e);
                    this._exitInProgress = true;
                    this.save(true);
                } else {
                    if (e.keyCode == dojo.keys.TAB) {
                        this._exitInProgress = true;
                        setTimeout(dojo.hitch(this, "save", false), 0);
                    }
                }
            }
        } else {
            var _40b = this;
            setTimeout(function() {
                _40b.saveButton.setAttribute("disabled", _40b.getValue() == _40b._initialText);
            }, 100);
        }
    },_onBlur:function() {
        this.inherited(arguments);
        if (this._exitInProgress) {
            return;
        }
        if (this.autoSave) {
            this._exitInProgress = true;
            if (this.getValue() == this._initialText) {
                this.cancel(false);
            } else {
                this.save(false);
            }
        }
    },enableSave:function() {
        return this.editWidget.isValid ? this.editWidget.isValid() : true;
    },_onChange:function() {
        if (this._exitInProgress) {
            return;
        }
        if (this.autoSave) {
            this._exitInProgress = true;
            this.save(true);
        } else {
            this.saveButton.setAttribute("disabled", (this.getValue() == this._initialText) ||
                                                     !this.enableSave());
        }
    },enableSave:function() {
        return this.editWidget.isValid ? this.editWidget.isValid() : true;
    },focus:function() {
        this.editWidget.focus();
        dijit.selectInputText(this.editWidget.focusNode);
    }});
}
if (!dojo._hasResource["dijit.form.CheckBox"]) {
    dojo._hasResource["dijit.form.CheckBox"] = true;
    dojo.provide("dijit.form.CheckBox");
    dojo.declare("dijit.form.CheckBox", dijit.form.ToggleButton, {templateString:"<div class=\"dijitReset dijitInline\" waiRole=\"presentation\"\n\t><input\n\t \ttype=\"${type}\" name=\"${name}\"\n\t\tclass=\"dijitReset dijitCheckBoxInput\"\n\t\tdojoAttachPoint=\"focusNode\"\n\t \tdojoAttachEvent=\"onmouseover:_onMouse,onmouseout:_onMouse,onclick:_onClick\"\n/></div>\n",baseClass:"dijitCheckBox",type:"checkbox",value:"on",setValue:function(
            _40c) {
        if (typeof _40c == "string") {
            this.setAttribute("value", _40c);
            _40c = true;
        }
        this.setAttribute("checked", _40c);
    },_getValueDeprecated:false,getValue:function() {
        return (this.checked ? this.value : false);
    },reset:function() {
        this.inherited(arguments);
        this.setAttribute("value", this._resetValueAttr);
    },postCreate:function() {
        this.inherited(arguments);
        this._resetValueAttr = this.value;
    }});
    dojo.declare("dijit.form.RadioButton", dijit.form.CheckBox, {type:"radio",baseClass:"dijitRadio",_groups:{},postCreate:function() {
        (this._groups[this.name] = this._groups[this.name] || []).push(this);
        this.inherited(arguments);
    },uninitialize:function() {
        dojo.forEach(this._groups[this.name], function(_40d, i, arr) {
            if (_40d === this) {
                arr.splice(i, 1);
                return;
            }
        }, this);
    },setAttribute:function(attr, _411) {
        this.inherited(arguments);
        switch (attr) {case "checked":if (this.checked) {
            dojo.forEach(this._groups[this.name], function(_412) {
                if (_412 != this && _412.checked) {
                    _412.setAttribute("checked", false);
                }
            }, this);
        }}
    },_clicked:function(e) {
        if (!this.checked) {
            this.setAttribute("checked", true);
        }
    }});
}
if (!dojo._hasResource["dijit.form.ValidationTextBox"]) {
    dojo._hasResource["dijit.form.ValidationTextBox"] = true;
    dojo.provide("dijit.form.ValidationTextBox");
    dojo.declare("dijit.form.ValidationTextBox", dijit.form.TextBox, {templateString:"<div class=\"dijit dijitReset dijitInlineTable dijitLeft\"\n\tid=\"widget_${id}\"\n\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\" waiRole=\"presentation\"\n\t><div style=\"overflow:hidden;\"\n\t\t><div class=\"dijitReset dijitValidationIcon\"><br></div\n\t\t><div class=\"dijitReset dijitValidationIconText\">&Chi;</div\n\t\t><div class=\"dijitReset dijitInputField\"\n\t\t\t><input class=\"dijitReset\" dojoAttachPoint='textbox,focusNode' dojoAttachEvent='onfocus:_update,onkeyup:_onkeyup,onblur:_onMouse,onkeypress:_onKeyPress' autocomplete=\"off\"\n\t\t\ttype='${type}' name='${name}'\n\t\t/></div\n\t></div\n></div>\n",baseClass:"dijitTextBox",required:false,promptMessage:"",invalidMessage:"$_unset_$",constraints:{},regExp:".*",regExpGen:function(
            _414) {
        return this.regExp;
    },state:"",tooltipPosition:[],setValue:function() {
        this.inherited(arguments);
        this.validate(this._focused);
    },validator:function(_415, _416) {
        return (new RegExp("^(" + this.regExpGen(_416) + ")" + (this.required ? "" : "?") +
                           "$")).test(_415) && (!this.required || !this._isEmpty(_415)) &&
               (this._isEmpty(_415) || this.parse(_415, _416) !== undefined);
    },isValid:function(_417) {
        return this.validator(this.textbox.value, this.constraints);
    },_isEmpty:function(_418) {
        return /^\s*$/.test(_418);
    },getErrorMessage:function(_419) {
        return this.invalidMessage;
    },getPromptMessage:function(_41a) {
        return this.promptMessage;
    },validate:function(_41b) {
        var _41c = "";
        var _41d = this.isValid(_41b);
        var _41e = this._isEmpty(this.textbox.value);
        this.state = (_41d || (!this._hasBeenBlurred && _41e)) ? "" : "Error";
        this._setStateClass();
        dijit.setWaiState(this.focusNode, "invalid", _41d ? "false" : "true");
        if (_41b) {
            if (_41e) {
                _41c = this.getPromptMessage(true);
            }
            if (!_41c && this.state == "Error") {
                _41c = this.getErrorMessage(true);
            }
        }
        this.displayMessage(_41c);
        return _41d;
    },_message:"",displayMessage:function(_41f) {
        if (this._message == _41f) {
            return;
        }
        this._message = _41f;
        dijit.hideTooltip(this.domNode);
        if (_41f) {
            dijit.showTooltip(_41f, this.domNode, this.tooltipPosition);
        }
    },_refreshState:function() {
        this.validate(this._focused);
    },_update:function(e) {
        this._refreshState();
        this._onMouse(e);
    },_onkeyup:function(e) {
        this._update(e);
        this.onkeyup(e);
    },constructor:function() {
        this.constraints = {};
    },postMixInProperties:function() {
        this.inherited(arguments);
        this.constraints.locale = this.lang;
        this.messages = dojo.i18n.getLocalization("dijit.form", "validate", this.lang);
        if (this.invalidMessage == "$_unset_$") {
            this.invalidMessage = this.messages.invalidMessage;
        }
        var p = this.regExpGen(this.constraints);
        this.regExp = p;
    }});
    dojo.declare("dijit.form.MappedTextBox", dijit.form.ValidationTextBox, {serialize:function(val,
                                                                                               _424) {
        return val.toString ? val.toString() : "";
    },toString:function() {
        var val = this.filter(this.getValue());
        return val != null ?
               (typeof val == "string" ? val : this.serialize(val, this.constraints)) : "";
    },validate:function() {
        this.valueNode.value = this.toString();
        return this.inherited(arguments);
    },setAttribute:function(attr, _427) {
        this.inherited(arguments);
        switch (attr) {case "disabled":if (this.valueNode) {
            this.valueNode.disabled = this.disabled;
        }}
    },postCreate:function() {
        var _428 = this.textbox;
        var _429 = (this.valueNode = dojo.doc.createElement("input"));
        _429.setAttribute("type", _428.type);
        _429.setAttribute("value", this.toString());
        dojo.style(_429, "display", "none");
        _429.name = this.textbox.name;
        _429.disabled = this.textbox.disabled;
        this.textbox.name = this.textbox.name + "_displayed_";
        this.textbox.removeAttribute("name");
        dojo.place(_429, _428, "after");
        this.inherited(arguments);
    }});
    dojo.declare("dijit.form.RangeBoundTextBox", dijit.form.MappedTextBox, {rangeMessage:"",compare:function(
            val1, val2) {
        return val1 - val2;
    },rangeCheck:function(_42c, _42d) {
        var _42e = "min" in _42d;
        var _42f = "max" in _42d;
        if (_42e || _42f) {
            return (!_42e || this.compare(_42c, _42d.min) >= 0) &&
                   (!_42f || this.compare(_42c, _42d.max) <= 0);
        }
        return true;
    },isInRange:function(_430) {
        return this.rangeCheck(this.getValue(), this.constraints);
    },isValid:function(_431) {
        return this.inherited(arguments) &&
               ((this._isEmpty(this.textbox.value) && !this.required) || this.isInRange(_431));
    },getErrorMessage:function(_432) {
        if (dijit.form.RangeBoundTextBox.superclass.isValid.call(this, false) &&
            !this.isInRange(_432)) {
            return this.rangeMessage;
        }
        return this.inherited(arguments);
    },postMixInProperties:function() {
        this.inherited(arguments);
        if (!this.rangeMessage) {
            this.messages = dojo.i18n.getLocalization("dijit.form", "validate", this.lang);
            this.rangeMessage = this.messages.rangeMessage;
        }
    },postCreate:function() {
        this.inherited(arguments);
        if (this.constraints.min !== undefined) {
            dijit.setWaiState(this.focusNode, "valuemin", this.constraints.min);
        }
        if (this.constraints.max !== undefined) {
            dijit.setWaiState(this.focusNode, "valuemax", this.constraints.max);
        }
    },setValue:function(_433, _434) {
        dijit.setWaiState(this.focusNode, "valuenow", _433);
        this.inherited("setValue", arguments);
    }});
}
if (!dojo._hasResource["dijit.form.ComboBox"]) {
    dojo._hasResource["dijit.form.ComboBox"] = true;
    dojo.provide("dijit.form.ComboBox");
    dojo.declare("dijit.form.ComboBoxMixin", null, {item:null,pageSize:Infinity,store:null,query:{},autoComplete:true,searchDelay:100,searchAttr:"name",queryExpr:"${0}*",ignoreCase:true,hasDownArrow:true,templateString:"<div class=\"dijit dijitReset dijitInlineTable dijitLeft\"\n\tid=\"widget_${id}\"\n\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\" dojoAttachPoint=\"comboNode\" waiRole=\"combobox\" tabIndex=\"-1\"\n\t><div style=\"overflow:hidden;\"\n\t\t><div class='dijitReset dijitRight dijitButtonNode dijitArrowButton dijitDownArrowButton'\n\t\t\tdojoAttachPoint=\"downArrowNode\" waiRole=\"presentation\"\n\t\t\tdojoAttachEvent=\"onmousedown:_onArrowMouseDown,onmouseup:_onMouse,onmouseenter:_onMouse,onmouseleave:_onMouse\"\n\t\t\t><div class=\"dijitArrowButtonInner\">&thinsp;</div\n\t\t\t><div class=\"dijitArrowButtonChar\">&#9660;</div\n\t\t></div\n\t\t><div class=\"dijitReset dijitValidationIcon\"><br></div\n\t\t><div class=\"dijitReset dijitValidationIconText\">&Chi;</div\n\t\t><div class=\"dijitReset dijitInputField\"\n\t\t\t><input type=\"text\" autocomplete=\"off\" name=\"${name}\" class='dijitReset'\n\t\t\tdojoAttachEvent=\"onkeypress:_onKeyPress, onfocus:_update, compositionend,onkeyup\"\n\t\t\tdojoAttachPoint=\"textbox,focusNode\" waiRole=\"textbox\" waiState=\"haspopup-true,autocomplete-list\"\n\t\t/></div\n\t></div\n></div>\n",baseClass:"dijitComboBox",_getCaretPos:function(
            _435) {
        var pos = 0;
        if (typeof (_435.selectionStart) == "number") {
            pos = _435.selectionStart;
        } else {
            if (dojo.isIE) {
                var tr = dojo.doc.selection.createRange().duplicate();
                var ntr = _435.createTextRange();
                tr.move("character", 0);
                ntr.move("character", 0);
                try {
                    ntr.setEndPoint("EndToEnd", tr);
                    pos = String(ntr.text).replace(/\r/g, "").length;
                } catch(e) {
                }
            }
        }
        return pos;
    },_setCaretPos:function(_439, _43a) {
        _43a = parseInt(_43a);
        dijit.selectInputText(_439, _43a, _43a);
    },_setAttribute:function(attr, _43c) {
        if (attr == "disabled") {
            dijit.setWaiState(this.comboNode, "disabled", _43c);
        }
    },_onKeyPress:function(evt) {
        if (evt.altKey || (evt.ctrlKey && evt.charCode != 118)) {
            return;
        }
        var _43e = false;
        var pw = this._popupWidget;
        var dk = dojo.keys;
        if (this._isShowingNow) {
            pw.handleKey(evt);
        }
        switch (evt.keyCode) {case dk.PAGE_DOWN:case dk.DOWN_ARROW:if (!this._isShowingNow ||
                                                                       this._prev_key_esc) {
            this._arrowPressed();
            _43e = true;
        } else {
            this._announceOption(pw.getHighlightedOption());
        }dojo.stopEvent(evt);this._prev_key_backspace = false;this._prev_key_esc =
                                                              false;break;case dk.PAGE_UP:case dk.UP_ARROW:if (this._isShowingNow) {
            this._announceOption(pw.getHighlightedOption());
        }dojo.stopEvent(evt);this._prev_key_backspace = false;this._prev_key_esc =
                                                              false;break;case dk.ENTER:var _441;if (this._isShowingNow &&
                                                                                                     (_441 =
                                                                                                      pw.getHighlightedOption())) {
            if (_441 == pw.nextButton) {
                this._nextSearch(1);
                dojo.stopEvent(evt);
                break;
            } else {
                if (_441 == pw.previousButton) {
                    this._nextSearch(-1);
                    dojo.stopEvent(evt);
                    break;
                }
            }
        } else {
            this.setDisplayedValue(this.getDisplayedValue());
        }evt.preventDefault();case dk.TAB:var _442 = this.getDisplayedValue();if (pw && (_442 ==
                                                                                         pw._messages["previousMessage"] ||
                                                                                         _442 ==
                                                                                         pw._messages["nextMessage"])) {
            break;
        }if (this._isShowingNow) {
            this._prev_key_backspace = false;
            this._prev_key_esc = false;
            if (pw.getHighlightedOption()) {
                pw.setValue({target:pw.getHighlightedOption()}, true);
            }
            this._hideResultList();
        }break;case dk.SPACE:this._prev_key_backspace = false;this._prev_key_esc =
                                                              false;if (this._isShowingNow &&
                                                                        pw.getHighlightedOption()) {
            dojo.stopEvent(evt);
            this._selectOption();
            this._hideResultList();
        } else {
            _43e = true;
        }break;case dk.ESCAPE:this._prev_key_backspace = false;this._prev_key_esc =
                                                               true;if (this._isShowingNow) {
            dojo.stopEvent(evt);
            this._hideResultList();
        }this.inherited(arguments);break;case dk.DELETE:case dk.BACKSPACE:this._prev_key_esc =
                                                                          false;this._prev_key_backspace =
                                                                                true;_43e =
                                                                                     true;break;case dk.RIGHT_ARROW:case dk.LEFT_ARROW:this._prev_key_backspace =
                                                                                                                                       false;this._prev_key_esc =
                                                                                                                                             false;break;default:this._prev_key_backspace =
                                                                                                                                                                 false;this._prev_key_esc =
                                                                                                                                                                       false;if (dojo.isIE ||
                                                                                                                                                                                 evt.charCode !=
                                                                                                                                                                                 0) {
            _43e = true;
        }}
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        if (_43e) {
            setTimeout(dojo.hitch(this, "_startSearchFromInput"), 1);
        }
    },_autoCompleteText:function(text) {
        var fn = this.focusNode;
        dijit.selectInputText(fn, fn.value.length);
        var _445 = this.ignoreCase ? "toLowerCase" : "substr";
        if (text[_445](0).indexOf(this.focusNode.value[_445](0)) == 0) {
            var cpos = this._getCaretPos(fn);
            if ((cpos + 1) > fn.value.length) {
                fn.value = text;
                dijit.selectInputText(fn, cpos);
            }
        } else {
            fn.value = text;
            dijit.selectInputText(fn);
        }
    },_openResultList:function(_447, _448) {
        if (this.disabled || this.readOnly || (_448.query[this.searchAttr] != this._lastQuery)) {
            return;
        }
        this._popupWidget.clearResultList();
        if (!_447.length) {
            this._hideResultList();
            return;
        }
        var _449 = new String(this.store.getValue(_447[0], this.searchAttr));
        if (_449 && this.autoComplete && !this._prev_key_backspace &&
            (_448.query[this.searchAttr] != "*")) {
            this._autoCompleteText(_449);
        }
        this._popupWidget.createOptions(_447, _448, dojo.hitch(this, "_getMenuLabelFromItem"));
        this._showResultList();
        if (_448.direction) {
            if (1 == _448.direction) {
                this._popupWidget.highlightFirstOption();
            } else {
                if (-1 == _448.direction) {
                    this._popupWidget.highlightLastOption();
                }
            }
            this._announceOption(this._popupWidget.getHighlightedOption());
        }
    },_showResultList:function() {
        this._hideResultList();
        var _44a = this._popupWidget.getItems(),_44b = Math.min(_44a.length, this.maxListLength);
        this._arrowPressed();
        this.displayMessage("");
        with (this._popupWidget.domNode.style) {
            width = "";
            height = "";
        }
        var best = this.open();
        var _44d = dojo.marginBox(this._popupWidget.domNode);
        this._popupWidget.domNode.style.overflow =
        ((best.h == _44d.h) && (best.w == _44d.w)) ? "hidden" : "auto";
        var _44e = best.w;
        if (best.h < this._popupWidget.domNode.scrollHeight) {
            _44e += 16;
        }
        dojo.marginBox(this._popupWidget.domNode, {h:best.h,w:Math.max(_44e, this.domNode.offsetWidth)});
        dijit.setWaiState(this.comboNode, "expanded", "true");
    },_hideResultList:function() {
        if (this._isShowingNow) {
            dijit.popup.close(this._popupWidget);
            this._arrowIdle();
            this._isShowingNow = false;
            dijit.setWaiState(this.comboNode, "expanded", "false");
            dijit.removeWaiState(this.focusNode, "activedescendant");
        }
    },_setBlurValue:function() {
        var _44f = this.getDisplayedValue();
        var pw = this._popupWidget;
        if (pw &&
            (_44f == pw._messages["previousMessage"] || _44f == pw._messages["nextMessage"])) {
            this.setValue(this._lastValueReported, true);
        } else {
            this.setDisplayedValue(_44f);
        }
    },_onBlur:function() {
        this._hideResultList();
        this._arrowIdle();
        this.inherited(arguments);
    },_announceOption:function(node) {
        if (node == null) {
            return;
        }
        var _452;
        if (node == this._popupWidget.nextButton || node == this._popupWidget.previousButton) {
            _452 = node.innerHTML;
        } else {
            _452 = this.store.getValue(node.item, this.searchAttr);
        }
        this.focusNode.value = this.focusNode.value.substring(0, this._getCaretPos(this.focusNode));
        dijit.setWaiState(this.focusNode, "activedescendant", dojo.attr(node, "id"));
        this._autoCompleteText(_452);
    },_selectOption:function(evt) {
        var tgt = null;
        if (!evt) {
            evt = {target:this._popupWidget.getHighlightedOption()};
        }
        if (!evt.target) {
            this.setDisplayedValue(this.getDisplayedValue());
            return;
        } else {
            tgt = evt.target;
        }
        if (!evt.noHide) {
            this._hideResultList();
            this._setCaretPos(this.focusNode, this.store.getValue(tgt.item, this.searchAttr).length);
        }
        this._doSelect(tgt);
    },_doSelect:function(tgt) {
        this.item = tgt.item;
        this.setValue(this.store.getValue(tgt.item, this.searchAttr), true);
    },_onArrowMouseDown:function(evt) {
        if (this.disabled || this.readOnly) {
            return;
        }
        dojo.stopEvent(evt);
        this.focus();
        if (this._isShowingNow) {
            this._hideResultList();
        } else {
            this._startSearch("");
        }
    },_startSearchFromInput:function() {
        this._startSearch(this.focusNode.value);
    },_getQueryString:function(text) {
        return dojo.string.substitute(this.queryExpr, [text]);
    },_startSearch:function(key) {
        if (!this._popupWidget) {
            var _459 = this.id + "_popup";
            this._popupWidget =
            new dijit.form._ComboBoxMenu({onChange:dojo.hitch(this, this._selectOption),id:_459});
            dijit.removeWaiState(this.focusNode, "activedescendant");
            dijit.setWaiState(this.textbox, "owns", _459);
        }
        this.item = null;
        var _45a = dojo.clone(this.query);
        this._lastQuery = _45a[this.searchAttr] = this._getQueryString(key);
        this.searchTimer = setTimeout(dojo.hitch(this, function(_45b, _45c) {
            var _45d = this.store.fetch({queryOptions:{ignoreCase:this.ignoreCase,deep:true},query:_45b,onComplete:dojo.hitch(this, "_openResultList"),onError:function(
                    _45e) {
                console.error("dijit.form.ComboBox: " + _45e);
                dojo.hitch(_45c, "_hideResultList")();
            },start:0,count:this.pageSize});
            var _45f = function(_460, _461) {
                _460.start += _460.count * _461;
                _460.direction = _461;
                this.store.fetch(_460);
            };
            this._nextSearch = this._popupWidget.onPage = dojo.hitch(this, _45f, _45d);
        }, _45a, this), this.searchDelay);
    },_getValueField:function() {
        return this.searchAttr;
    },_arrowPressed:function() {
        if (!this.disabled && !this.readOnly && this.hasDownArrow) {
            dojo.addClass(this.downArrowNode, "dijitArrowButtonActive");
        }
    },_arrowIdle:function() {
        if (!this.disabled && !this.readOnly && this.hasDownArrow) {
            dojo.removeClass(this.downArrowNode, "dojoArrowButtonPushed");
        }
    },compositionend:function(evt) {
        this.onkeypress({charCode:-1});
    },constructor:function() {
        this.query = {};
    },postMixInProperties:function() {
        if (!this.hasDownArrow) {
            this.baseClass = "dijitTextBox";
        }
        if (!this.store) {
            var _463 = this.srcNodeRef;
            this.store = new dijit.form._ComboBoxDataStore(_463);
            if (!this.value || ((typeof _463.selectedIndex == "number") &&
                                _463.selectedIndex.toString() === this.value)) {
                var item = this.store.fetchSelectedItem();
                if (item) {
                    this.value = this.store.getValue(item, this._getValueField());
                }
            }
        }
    },_postCreate:function() {
        var _465 = dojo.query("label[for=\"" + this.id + "\"]");
        if (_465.length) {
            _465[0].id = (this.id + "_label");
            var cn = this.comboNode;
            dijit.setWaiState(cn, "labelledby", _465[0].id);
            dijit.setWaiState(cn, "disabled", this.disabled);
        }
    },uninitialize:function() {
        if (this._popupWidget) {
            this._hideResultList();
            this._popupWidget.destroy();
        }
    },_getMenuLabelFromItem:function(item) {
        return {html:false,label:this.store.getValue(item, this.searchAttr)};
    },open:function() {
        this._isShowingNow = true;
        return dijit.popup.open({popup:this._popupWidget,around:this.domNode,parent:this});
    },reset:function() {
        this.item = null;
        this.inherited(arguments);
    }});
    dojo.declare("dijit.form._ComboBoxMenu", [dijit._Widget,dijit._Templated], {templateString:"<ul class='dijitMenu' dojoAttachEvent='onmousedown:_onMouseDown,onmouseup:_onMouseUp,onmouseover:_onMouseOver,onmouseout:_onMouseOut' tabIndex='-1' style='overflow:\"auto\";'>" +
                                                                                               "<li class='dijitMenuItem dijitMenuPreviousButton' dojoAttachPoint='previousButton'></li>" +
                                                                                               "<li class='dijitMenuItem dijitMenuNextButton' dojoAttachPoint='nextButton'></li>" +
                                                                                               "</ul>",_messages:null,postMixInProperties:function() {
        this._messages = dojo.i18n.getLocalization("dijit.form", "ComboBox", this.lang);
        this.inherited("postMixInProperties", arguments);
    },setValue:function(_468) {
        this.value = _468;
        this.onChange(_468);
    },onChange:function(_469) {
    },onPage:function(_46a) {
    },postCreate:function() {
        this.previousButton.innerHTML = this._messages["previousMessage"];
        this.nextButton.innerHTML = this._messages["nextMessage"];
        this.inherited("postCreate", arguments);
    },onClose:function() {
        this._blurOptionNode();
    },_createOption:function(item, _46c) {
        var _46d = _46c(item);
        var _46e = dojo.doc.createElement("li");
        dijit.setWaiRole(_46e, "option");
        if (_46d.html) {
            _46e.innerHTML = _46d.label;
        } else {
            _46e.appendChild(dojo.doc.createTextNode(_46d.label));
        }
        if (_46e.innerHTML == "") {
            _46e.innerHTML = "&nbsp;";
        }
        _46e.item = item;
        return _46e;
    },createOptions:function(_46f, _470, _471) {
        this.previousButton.style.display = (_470.start == 0) ? "none" : "";
        dojo.attr(this.previousButton, "id", this.id + "_prev");
        dojo.forEach(_46f, function(item, i) {
            var _474 = this._createOption(item, _471);
            _474.className = "dijitMenuItem";
            dojo.attr(_474, "id", this.id + i);
            this.domNode.insertBefore(_474, this.nextButton);
        }, this);
        this.nextButton.style.display = (_470.count == _46f.length) ? "" : "none";
        dojo.attr(this.nextButton, "id", this.id + "_next");
    },clearResultList:function() {
        while (this.domNode.childNodes.length > 2) {
            this.domNode.removeChild(this.domNode.childNodes[this.domNode.childNodes.length - 2]);
        }
    },getItems:function() {
        return this.domNode.childNodes;
    },getListLength:function() {
        return this.domNode.childNodes.length - 2;
    },_onMouseDown:function(evt) {
        dojo.stopEvent(evt);
    },_onMouseUp:function(evt) {
        if (evt.target === this.domNode) {
            return;
        } else {
            if (evt.target == this.previousButton) {
                this.onPage(-1);
            } else {
                if (evt.target == this.nextButton) {
                    this.onPage(1);
                } else {
                    var tgt = evt.target;
                    while (!tgt.item) {
                        tgt = tgt.parentNode;
                    }
                    this.setValue({target:tgt}, true);
                }
            }
        }
    },_onMouseOver:function(evt) {
        if (evt.target === this.domNode) {
            return;
        }
        var tgt = evt.target;
        if (!(tgt == this.previousButton || tgt == this.nextButton)) {
            while (!tgt.item) {
                tgt = tgt.parentNode;
            }
        }
        this._focusOptionNode(tgt);
    },_onMouseOut:function(evt) {
        if (evt.target === this.domNode) {
            return;
        }
        this._blurOptionNode();
    },_focusOptionNode:function(node) {
        if (this._highlighted_option != node) {
            this._blurOptionNode();
            this._highlighted_option = node;
            dojo.addClass(this._highlighted_option, "dijitMenuItemHover");
        }
    },_blurOptionNode:function() {
        if (this._highlighted_option) {
            dojo.removeClass(this._highlighted_option, "dijitMenuItemHover");
            this._highlighted_option = null;
        }
    },_highlightNextOption:function() {
        var fc = this.domNode.firstChild;
        if (!this.getHighlightedOption()) {
            this._focusOptionNode(fc.style.display == "none" ? fc.nextSibling : fc);
        } else {
            var ns = this._highlighted_option.nextSibling;
            if (ns && ns.style.display != "none") {
                this._focusOptionNode(ns);
            }
        }
        dijit.scrollIntoView(this._highlighted_option);
    },highlightFirstOption:function() {
        this._focusOptionNode(this.domNode.firstChild.nextSibling);
        dijit.scrollIntoView(this._highlighted_option);
    },highlightLastOption:function() {
        this._focusOptionNode(this.domNode.lastChild.previousSibling);
        dijit.scrollIntoView(this._highlighted_option);
    },_highlightPrevOption:function() {
        var lc = this.domNode.lastChild;
        if (!this.getHighlightedOption()) {
            this._focusOptionNode(lc.style.display == "none" ? lc.previousSibling : lc);
        } else {
            var ps = this._highlighted_option.previousSibling;
            if (ps && ps.style.display != "none") {
                this._focusOptionNode(ps);
            }
        }
        dijit.scrollIntoView(this._highlighted_option);
    },_page:function(up) {
        var _481 = 0;
        var _482 = this.domNode.scrollTop;
        var _483 = dojo.style(this.domNode, "height");
        if (!this.getHighlightedOption()) {
            this._highlightNextOption();
        }
        while (_481 < _483) {
            if (up) {
                if (!this.getHighlightedOption().previousSibling ||
                    this._highlighted_option.previousSibling.style.display == "none") {
                    break;
                }
                this._highlightPrevOption();
            } else {
                if (!this.getHighlightedOption().nextSibling ||
                    this._highlighted_option.nextSibling.style.display == "none") {
                    break;
                }
                this._highlightNextOption();
            }
            var _484 = this.domNode.scrollTop;
            _481 += (_484 - _482) * (up ? -1 : 1);
            _482 = _484;
        }
    },pageUp:function() {
        this._page(true);
    },pageDown:function() {
        this._page(false);
    },getHighlightedOption:function() {
        var ho = this._highlighted_option;
        return (ho && ho.parentNode) ? ho : null;
    },handleKey:function(evt) {
        switch (evt.keyCode) {case dojo.keys.DOWN_ARROW:this._highlightNextOption();break;case dojo.keys.PAGE_DOWN:this.pageDown();break;case dojo.keys.UP_ARROW:this._highlightPrevOption();break;case dojo.keys.PAGE_UP:this.pageUp();break;}
    }});
    dojo.declare("dijit.form.ComboBox", [dijit.form.ValidationTextBox,dijit.form.ComboBoxMixin], {postMixInProperties:function() {
        dijit.form.ComboBoxMixin.prototype.postMixInProperties.apply(this, arguments);
        dijit.form.ValidationTextBox.prototype.postMixInProperties.apply(this, arguments);
    },postCreate:function() {
        dijit.form.ComboBoxMixin.prototype._postCreate.apply(this, arguments);
        dijit.form.ValidationTextBox.prototype.postCreate.apply(this, arguments);
    },setAttribute:function(attr, _488) {
        dijit.form.ValidationTextBox.prototype.setAttribute.apply(this, arguments);
        dijit.form.ComboBoxMixin.prototype._setAttribute.apply(this, arguments);
    }});
    dojo.declare("dijit.form._ComboBoxDataStore", null, {constructor:function(root) {
        this.root = root;
    },getValue:function(item, _48b, _48c) {
        return (_48b == "value") ? item.value : (item.innerText || item.textContent || "");
    },isItemLoaded:function(_48d) {
        return true;
    },fetch:function(args) {
        var _48f = "^" +
                   args.query.name.replace(/([\\\|\(\)\[\{\^\$\+\?\.\<\>])/g, "\\$1").replace("*", ".*") +
                   "$",_490 = new RegExp(_48f, args.queryOptions.ignoreCase ? "i" :
                                               ""),_491 = dojo.query("> option", this.root).filter(function(
                _492) {
            return (_492.innerText || _492.textContent || "").match(_490);
        });
        var _493 = args.start || 0,end = ("count" in args && args.count != Infinity) ?
                                         (_493 + args.count) : _491.length;
        args.onComplete(_491.slice(_493, end), args);
        return args;
    },close:function(_495) {
        return;
    },getLabel:function(item) {
        return item.innerHTML;
    },getIdentity:function(item) {
        return dojo.attr(item, "value");
    },fetchItemByIdentity:function(args) {
        var item = dojo.query("option[value='" + args.identity + "']", this.root)[0];
        args.onItem(item);
    },fetchSelectedItem:function() {
        var root = this.root,si = root.selectedIndex;
        return dojo.query("> option:nth-child(" + (si != -1 ? si + 1 : 1) + ")", root)[0];
    }});
}
if (!dojo._hasResource["dojo.cldr.monetary"]) {
    dojo._hasResource["dojo.cldr.monetary"] = true;
    dojo.provide("dojo.cldr.monetary");
    dojo.cldr.monetary.getData = function(code) {
        var _49d = {ADP:0,BHD:3,BIF:0,BYR:0,CLF:0,CLP:0,DJF:0,ESP:0,GNF:0,IQD:3,ITL:0,JOD:3,JPY:0,KMF:0,KRW:0,KWD:3,LUF:0,LYD:3,MGA:0,MGF:0,OMR:3,PYG:0,RWF:0,TND:3,TRL:0,VUV:0,XAF:0,XOF:0,XPF:0};
        var _49e = {CHF:5};
        var _49f = _49d[code],_4a0 = _49e[code];
        if (typeof _49f == "undefined") {
            _49f = 2;
        }
        if (typeof _4a0 == "undefined") {
            _4a0 = 0;
        }
        return {places:_49f,round:_4a0};
    };
}
if (!dojo._hasResource["dojo.currency"]) {
    dojo._hasResource["dojo.currency"] = true;
    dojo.provide("dojo.currency");
    dojo.currency._mixInDefaults = function(_4a1) {
        _4a1 = _4a1 || {};
        _4a1.type = "currency";
        var _4a2 = dojo.i18n.getLocalization("dojo.cldr", "currency", _4a1.locale) || {};
        var iso = _4a1.currency;
        var data = dojo.cldr.monetary.getData(iso);
        dojo.forEach(["displayName","symbol","group","decimal"], function(prop) {
            data[prop] = _4a2[iso + "_" + prop];
        });
        data.fractional = [true,false];
        return dojo.mixin(data, _4a1);
    };
    dojo.currency.format = function(_4a6, _4a7) {
        return dojo.number.format(_4a6, dojo.currency._mixInDefaults(_4a7));
    };
    dojo.currency.regexp = function(_4a8) {
        return dojo.number.regexp(dojo.currency._mixInDefaults(_4a8));
    };
    dojo.currency.parse = function(_4a9, _4aa) {
        return dojo.number.parse(_4a9, dojo.currency._mixInDefaults(_4aa));
    };
}
if (!dojo._hasResource["dijit.form.NumberTextBox"]) {
    dojo._hasResource["dijit.form.NumberTextBox"] = true;
    dojo.provide("dijit.form.NumberTextBox");
    dojo.declare("dijit.form.NumberTextBoxMixin", null, {regExpGen:dojo.number.regexp,editOptions:{pattern:"#.######"},_onFocus:function() {
        this.setValue(this.getValue(), false);
        this.inherited(arguments);
    },_formatter:dojo.number.format,format:function(_4ab, _4ac) {
        if (typeof _4ab == "string") {
            return _4ab;
        }
        if (isNaN(_4ab)) {
            return "";
        }
        if (this.editOptions && this._focused) {
            _4ac = dojo.mixin(dojo.mixin({}, this.editOptions), this.constraints);
        }
        return this._formatter(_4ab, _4ac);
    },parse:dojo.number.parse,filter:function(_4ad) {
        if (typeof _4ad == "string") {
            return this.inherited("filter", arguments);
        }
        return isNaN(_4ad) ? "" : _4ad;
    },value:NaN});
    dojo.declare("dijit.form.NumberTextBox", [dijit.form.RangeBoundTextBox,dijit.form.NumberTextBoxMixin], {});
}
if (!dojo._hasResource["dijit.form.CurrencyTextBox"]) {
    dojo._hasResource["dijit.form.CurrencyTextBox"] = true;
    dojo.provide("dijit.form.CurrencyTextBox");
    dojo.declare("dijit.form.CurrencyTextBox", dijit.form.NumberTextBox, {currency:"",regExpGen:dojo.currency.regexp,_formatter:dojo.currency.format,parse:dojo.currency.parse,postMixInProperties:function() {
        if (this.constraints === dijit.form.ValidationTextBox.prototype.constraints) {
            this.constraints = {};
        }
        this.constraints.currency = this.currency;
        dijit.form.CurrencyTextBox.superclass.postMixInProperties.apply(this, arguments);
    }});
}
if (!dojo._hasResource["dojo.cldr.supplemental"]) {
    dojo._hasResource["dojo.cldr.supplemental"] = true;
    dojo.provide("dojo.cldr.supplemental");
    dojo.cldr.supplemental.getFirstDayOfWeek = function(_4ae) {
        var _4af = {mv:5,ae:6,af:6,bh:6,dj:6,dz:6,eg:6,er:6,et:6,iq:6,ir:6,jo:6,ke:6,kw:6,lb:6,ly:6,ma:6,om:6,qa:6,sa:6,sd:6,so:6,tn:6,ye:6,as:0,au:0,az:0,bw:0,ca:0,cn:0,fo:0,ge:0,gl:0,gu:0,hk:0,ie:0,il:0,is:0,jm:0,jp:0,kg:0,kr:0,la:0,mh:0,mo:0,mp:0,mt:0,nz:0,ph:0,pk:0,sg:0,th:0,tt:0,tw:0,um:0,us:0,uz:0,vi:0,za:0,zw:0,et:0,mw:0,ng:0,tj:0,sy:4};
        var _4b0 = dojo.cldr.supplemental._region(_4ae);
        var dow = _4af[_4b0];
        return (dow === undefined) ? 1 : dow;
    };
    dojo.cldr.supplemental._region = function(_4b2) {
        _4b2 = dojo.i18n.normalizeLocale(_4b2);
        var tags = _4b2.split("-");
        var _4b4 = tags[1];
        if (!_4b4) {
            _4b4 =
            {de:"de",en:"us",es:"es",fi:"fi",fr:"fr",hu:"hu",it:"it",ja:"jp",ko:"kr",nl:"nl",pt:"br",sv:"se",zh:"cn"}[tags[0]];
        } else {
            if (_4b4.length == 4) {
                _4b4 = tags[2];
            }
        }
        return _4b4;
    };
    dojo.cldr.supplemental.getWeekend = function(_4b5) {
        var _4b6 = {eg:5,il:5,sy:5,"in":0,ae:4,bh:4,dz:4,iq:4,jo:4,kw:4,lb:4,ly:4,ma:4,om:4,qa:4,sa:4,sd:4,tn:4,ye:4};
        var _4b7 = {ae:5,bh:5,dz:5,iq:5,jo:5,kw:5,lb:5,ly:5,ma:5,om:5,qa:5,sa:5,sd:5,tn:5,ye:5,af:5,ir:5,eg:6,il:6,sy:6};
        var _4b8 = dojo.cldr.supplemental._region(_4b5);
        var _4b9 = _4b6[_4b8];
        var end = _4b7[_4b8];
        if (_4b9 === undefined) {
            _4b9 = 6;
        }
        if (end === undefined) {
            end = 0;
        }
        return {start:_4b9,end:end};
    };
}
if (!dojo._hasResource["dojo.date"]) {
    dojo._hasResource["dojo.date"] = true;
    dojo.provide("dojo.date");
    dojo.date.getDaysInMonth = function(_4bb) {
        var _4bc = _4bb.getMonth();
        var days = [31,28,31,30,31,30,31,31,30,31,30,31];
        if (_4bc == 1 && dojo.date.isLeapYear(_4bb)) {
            return 29;
        }
        return days[_4bc];
    };
    dojo.date.isLeapYear = function(_4be) {
        var year = _4be.getFullYear();
        return !(year % 400) || (!(year % 4) && !!(year % 100));
    };
    dojo.date.getTimezoneName = function(_4c0) {
        var str = _4c0.toString();
        var tz = "";
        var _4c3;
        var pos = str.indexOf("(");
        if (pos > -1) {
            tz = str.substring(++pos, str.indexOf(")"));
        } else {
            var pat = /([A-Z\/]+) \d{4}$/;
            if ((_4c3 = str.match(pat))) {
                tz = _4c3[1];
            } else {
                str = _4c0.toLocaleString();
                pat = / ([A-Z\/]+)$/;
                if ((_4c3 = str.match(pat))) {
                    tz = _4c3[1];
                }
            }
        }
        return (tz == "AM" || tz == "PM") ? "" : tz;
    };
    dojo.date.compare = function(_4c6, _4c7, _4c8) {
        _4c6 = new Date(Number(_4c6));
        _4c7 = new Date(Number(_4c7 || new Date()));
        if (_4c8 !== "undefined") {
            if (_4c8 == "date") {
                _4c6.setHours(0, 0, 0, 0);
                _4c7.setHours(0, 0, 0, 0);
            } else {
                if (_4c8 == "time") {
                    _4c6.setFullYear(0, 0, 0);
                    _4c7.setFullYear(0, 0, 0);
                }
            }
        }
        if (_4c6 > _4c7) {
            return 1;
        }
        if (_4c6 < _4c7) {
            return -1;
        }
        return 0;
    };
    dojo.date.add = function(date, _4ca, _4cb) {
        var sum = new Date(Number(date));
        var _4cd = false;
        var _4ce = "Date";
        switch (_4ca) {case "day":break;case "weekday":var days,_4d0;var mod = _4cb % 5;if (!mod) {
            days = (_4cb > 0) ? 5 : -5;
            _4d0 = (_4cb > 0) ? ((_4cb - 5) / 5) : ((_4cb + 5) / 5);
        } else {
            days = mod;
            _4d0 = parseInt(_4cb / 5);
        }var strt = date.getDay();var adj = 0;if (strt == 6 && _4cb > 0) {
            adj = 1;
        } else {
            if (strt == 0 && _4cb < 0) {
                adj = -1;
            }
        }var trgt = strt + days;if (trgt == 0 || trgt == 6) {
            adj = (_4cb > 0) ? 2 : -2;
        }_4cb = (7 * _4d0) + days + adj;break;case "year":_4ce = "FullYear";_4cd =
                                                                            true;break;case "week":_4cb *=
                                                                                                   7;break;case "quarter":_4cb *=
                                                                                                                          3;case "month":_4cd =
                                                                                                                                         true;_4ce =
                                                                                                                                              "Month";break;case "hour":case "minute":case "second":case "millisecond":_4ce =
                                                                                                                                                                                                                       "UTC" +
                                                                                                                                                                                                                       _4ca.charAt(0).toUpperCase() +
                                                                                                                                                                                                                       _4ca.substring(1) +
                                                                                                                                                                                                                       "s";}
        if (_4ce) {
            sum["set" + _4ce](sum["get" + _4ce]() + _4cb);
        }
        if (_4cd && (sum.getDate() < date.getDate())) {
            sum.setDate(0);
        }
        return sum;
    };
    dojo.date.difference = function(_4d5, _4d6, _4d7) {
        _4d6 = _4d6 || new Date();
        _4d7 = _4d7 || "day";
        var _4d8 = _4d6.getFullYear() - _4d5.getFullYear();
        var _4d9 = 1;
        switch (_4d7) {case "quarter":var m1 = _4d5.getMonth();var m2 = _4d6.getMonth();var q1 = Math.floor(m1 /
                                                                                                            3) +
                                                                                                 1;var q2 = Math.floor(m2 /
                                                                                                                       3) +
                                                                                                            1;q2 +=
                                                                                                              (_4d8 *
                                                                                                               4);_4d9 =
                                                                                                                  q2 -
                                                                                                                  q1;break;case "weekday":var days = Math.round(dojo.date.difference(_4d5, _4d6, "day"));var _4df = parseInt(dojo.date.difference(_4d5, _4d6, "week"));var mod = days %
                                                                                                                                                                                                                                                                                 7;if (mod ==
                                                                                                                                                                                                                                                                                       0) {
            days = _4df * 5;
        } else {
            var adj = 0;
            var aDay = _4d5.getDay();
            var bDay = _4d6.getDay();
            _4df = parseInt(days / 7);
            mod = days % 7;
            var _4e4 = new Date(_4d5);
            _4e4.setDate(_4e4.getDate() + (_4df * 7));
            var _4e5 = _4e4.getDay();
            if (days > 0) {
                switch (true) {case aDay == 6:adj = -1;break;case aDay == 0:adj =
                                                                            0;break;case bDay ==
                                                                                         6:adj =
                                                                                           -1;break;case bDay ==
                                                                                                         0:adj =
                                                                                                           -2;break;case (_4e5 +
                                                                                                                          mod) >
                                                                                                                         5:adj =
                                                                                                                           -2;}
            } else {
                if (days < 0) {
                    switch (true) {case aDay == 6:adj = 0;break;case aDay == 0:adj =
                                                                               1;break;case bDay ==
                                                                                            6:adj =
                                                                                              2;break;case bDay ==
                                                                                                           0:adj =
                                                                                                             1;break;case (_4e5 +
                                                                                                                           mod) <
                                                                                                                          0:adj =
                                                                                                                            2;}
                }
            }
            days += adj;
            days -= (_4df * 2);
        }_4d9 = days;break;case "year":_4d9 = _4d8;break;case "month":_4d9 = (_4d6.getMonth() -
                                                                              _4d5.getMonth()) +
                                                                             (_4d8 *
                                                                              12);break;case "week":_4d9 =
                                                                                                    parseInt(dojo.date.difference(_4d5, _4d6, "day") /
                                                                                                             7);break;case "day":_4d9 /=
                                                                                                                                 24;case "hour":_4d9 /=
                                                                                                                                                60;case "minute":_4d9 /=
                                                                                                                                                                 60;case "second":_4d9 /=
                                                                                                                                                                                  1000;case "millisecond":_4d9 *=
                                                                                                                                                                                                          _4d6.getTime() -
                                                                                                                                                                                                          _4d5.getTime();}
        return Math.round(_4d9);
    };
}
if (!dojo._hasResource["dojo.date.locale"]) {
    dojo._hasResource["dojo.date.locale"] = true;
    dojo.provide("dojo.date.locale");
    (function() {
        function formatPattern(_4e6, _4e7, _4e8, _4e9) {
            return _4e9.replace(/([a-z])\1*/ig, function(_4ea) {
                var s,pad;
                var c = _4ea.charAt(0);
                var l = _4ea.length;
                var _4ef = ["abbr","wide","narrow"];
                switch (c) {case "G":s =
                                     _4e7[(l < 4) ? "eraAbbr" : "eraNames"][_4e6.getFullYear() < 0 ?
                                                                            0 :
                                                                            1];break;case "y":s =
                                                                                              _4e6.getFullYear();switch (l) {case 1:break;case 2:if (!_4e8) {
                    s = String(s);
                    s = s.substr(s.length - 2);
                    break;
                }default:pad = true;}break;case "Q":case "q":s = Math.ceil((_4e6.getMonth() + 1) /
                                                                           3);pad =
                                                                              true;break;case "M":case "L":var m = _4e6.getMonth();var _4f1;switch (l) {case 1:case 2:s =
                                                                                                                                                                      m +
                                                                                                                                                                      1;pad =
                                                                                                                                                                        true;break;case 3:case 4:case 5:_4f1 =
                                                                                                                                                                                                        _4ef[l -
                                                                                                                                                                                                             3];break;}if (_4f1) {
                    var _4f2 = (c == "L") ? "standalone" : "format";
                    var _4f3 = ["months",_4f2,_4f1].join("-");
                    s = _4e7[_4f3][m];
                }break;case "w":var _4f4 = 0;s = dojo.date.locale._getWeekOfYear(_4e6, _4f4);pad =
                                                                                             true;break;case "d":s =
                                                                                                                 _4e6.getDate();pad =
                                                                                                                                true;break;case "D":s =
                                                                                                                                                    dojo.date.locale._getDayOfYear(_4e6);pad =
                                                                                                                                                                                         true;break;case "E":case "e":case "c":var d = _4e6.getDay();var _4f6;switch (l) {case 1:case 2:if (c ==
                                                                                                                                                                                                                                                                                            "e") {
                    var _4f7 = dojo.cldr.supplemental.getFirstDayOfWeek(options.locale);
                    d = (d - _4f7 + 7) % 7;
                }if (c != "c") {
                    s = d + 1;
                    pad = true;
                    break;
                }case 3:case 4:case 5:_4f6 = _4ef[l - 3];break;}if (_4f6) {
                    var _4f8 = (c == "c") ? "standalone" : "format";
                    var _4f9 = ["days",_4f8,_4f6].join("-");
                    s = _4e7[_4f9][d];
                }break;case "a":var _4fa = (_4e6.getHours() < 12) ? "am" : "pm";s =
                                                                                _4e7[_4fa];break;case "h":case "H":case "K":case "k":var h = _4e6.getHours();switch (c) {case "h":s =
                                                                                                                                                                                  (h %
                                                                                                                                                                                   12) ||
                                                                                                                                                                                  12;break;case "H":s =
                                                                                                                                                                                                    h;break;case "K":s =
                                                                                                                                                                                                                     (h %
                                                                                                                                                                                                                      12);break;case "k":s =
                                                                                                                                                                                                                                         h ||
                                                                                                                                                                                                                                         24;break;}pad =
                                                                                                                                                                                                                                                   true;break;case "m":s =
                                                                                                                                                                                                                                                                       _4e6.getMinutes();pad =
                                                                                                                                                                                                                                                                                         true;break;case "s":s =
                                                                                                                                                                                                                                                                                                             _4e6.getSeconds();pad =
                                                                                                                                                                                                                                                                                                                               true;break;case "S":s =
                                                                                                                                                                                                                                                                                                                                                   Math.round(_4e6.getMilliseconds() *
                                                                                                                                                                                                                                                                                                                                                              Math.pow(10, l -
                                                                                                                                                                                                                                                                                                                                                                           3));pad =
                                                                                                                                                                                                                                                                                                                                                                               true;break;case "v":case "z":s =
                                                                                                                                                                                                                                                                                                                                                                                                            dojo.date.getTimezoneName(_4e6);if (s) {
                    break;
                }l = 4;case "Z":var _4fc = _4e6.getTimezoneOffset();var tz = [(_4fc <= 0 ? "+" :
                                                                               "-"),dojo.string.pad(Math.floor(Math.abs(_4fc) /
                                                                                                               60), 2),dojo.string.pad(Math.abs(_4fc) %
                                                                                                                                       60, 2)];if (l ==
                                                                                                                                                   4) {
                    tz.splice(0, 0, "GMT");
                    tz.splice(3, 0, ":");
                }s =
                 tz.join("");break;default:throw new Error("dojo.date.locale.format: invalid pattern char: " +
                                                           _4e9);}
                if (pad) {
                    s = dojo.string.pad(s, l);
                }
                return s;
            });
        }
        ;
        dojo.date.locale.format = function(_4fe, _4ff) {
            _4ff = _4ff || {};
            var _500 = dojo.i18n.normalizeLocale(_4ff.locale);
            var _501 = _4ff.formatLength || "short";
            var _502 = dojo.date.locale._getGregorianBundle(_500);
            var str = [];
            var _504 = dojo.hitch(this, formatPattern, _4fe, _502, _4ff.fullYear);
            if (_4ff.selector == "year") {
                var year = _4fe.getFullYear();
                if (_500.match(/^zh|^ja/)) {
                    year += "年";
                }
                return year;
            }
            if (_4ff.selector != "time") {
                var _506 = _4ff.datePattern || _502["dateFormat-" + _501];
                if (_506) {
                    str.push(_processPattern(_506, _504));
                }
            }
            if (_4ff.selector != "date") {
                var _507 = _4ff.timePattern || _502["timeFormat-" + _501];
                if (_507) {
                    str.push(_processPattern(_507, _504));
                }
            }
            var _508 = str.join(" ");
            return _508;
        };
        dojo.date.locale.regexp = function(_509) {
            return dojo.date.locale._parseInfo(_509).regexp;
        };
        dojo.date.locale._parseInfo = function(_50a) {
            _50a = _50a || {};
            var _50b = dojo.i18n.normalizeLocale(_50a.locale);
            var _50c = dojo.date.locale._getGregorianBundle(_50b);
            var _50d = _50a.formatLength || "short";
            var _50e = _50a.datePattern || _50c["dateFormat-" + _50d];
            var _50f = _50a.timePattern || _50c["timeFormat-" + _50d];
            var _510;
            if (_50a.selector == "date") {
                _510 = _50e;
            } else {
                if (_50a.selector == "time") {
                    _510 = _50f;
                } else {
                    _510 = _50e + " " + _50f;
                }
            }
            var _511 = [];
            var re = _processPattern(_510, dojo.hitch(this, _buildDateTimeRE, _511, _50c, _50a));
            return {regexp:re,tokens:_511,bundle:_50c};
        };
        dojo.date.locale.parse = function(_513, _514) {
            var info = dojo.date.locale._parseInfo(_514);
            var _516 = info.tokens,_517 = info.bundle;
            var re = new RegExp("^" + info.regexp + "$");
            var _519 = re.exec(_513);
            if (!_519) {
                return null;
            }
            var _51a = ["abbr","wide","narrow"];
            var _51b = [1970,0,1,0,0,0,0];
            var amPm = "";
            var _51d = dojo.every(_519, function(v, i) {
                if (!i) {
                    return true;
                }
                var _520 = _516[i - 1];
                var l = _520.length;
                switch (_520.charAt(0)) {case "y":if (l != 2 && _514.strict) {
                    _51b[0] = v;
                } else {
                    if (v < 100) {
                        v = Number(v);
                        var year = "" + new Date().getFullYear();
                        var _523 = year.substring(0, 2) * 100;
                        var _524 = Math.min(Number(year.substring(2, 4)) + 20, 99);
                        var num = (v < _524) ? _523 + v : _523 - 100 + v;
                        _51b[0] = num;
                    } else {
                        if (_514.strict) {
                            return false;
                        }
                        _51b[0] = v;
                    }
                }break;case "M":if (l > 2) {
                    var _526 = _517["months-format-" + _51a[l - 3]].concat();
                    if (!_514.strict) {
                        v = v.replace(".", "").toLowerCase();
                        _526 = dojo.map(_526, function(s) {
                            return s.replace(".", "").toLowerCase();
                        });
                    }
                    v = dojo.indexOf(_526, v);
                    if (v == -1) {
                        return false;
                    }
                } else {
                    v--;
                }_51b[1] = v;break;case "E":case "e":var days = _517["days-format-" + _51a[l -
                                                                                           3]].concat();if (!_514.strict) {
                    v = v.toLowerCase();
                    days = dojo.map(days, function(d) {
                        return d.toLowerCase();
                    });
                }v = dojo.indexOf(days, v);if (v == -1) {
                    return false;
                }break;case "D":_51b[1] = 0;case "d":_51b[2] = v;break;case "a":var am = _514.am ||
                                                                                         _517.am;var pm = _514.pm ||
                                                                                                          _517.pm;if (!_514.strict) {
                    var _52c = /\./g;
                    v = v.replace(_52c, "").toLowerCase();
                    am = am.replace(_52c, "").toLowerCase();
                    pm = pm.replace(_52c, "").toLowerCase();
                }if (_514.strict && v != am && v != pm) {
                    return false;
                }amPm = (v == pm) ? "p" : (v == am) ? "a" : "";break;case "K":if (v == 24) {
                    v = 0;
                }case "h":case "H":case "k":if (v > 23) {
                    return false;
                }_51b[3] = v;break;case "m":_51b[4] = v;break;case "s":_51b[5] =
                                                                       v;break;case "S":_51b[6] =
                                                                                        v;}
                return true;
            });
            var _52d = +_51b[3];
            if (amPm === "p" && _52d < 12) {
                _51b[3] = _52d + 12;
            } else {
                if (amPm === "a" && _52d == 12) {
                    _51b[3] = 0;
                }
            }
            var _52e = new Date(_51b[0], _51b[1], _51b[2], _51b[3], _51b[4], _51b[5], _51b[6]);
            if (_514.strict) {
                _52e.setFullYear(_51b[0]);
            }
            var _52f = _516.join("");
            if (!_51d || (_52f.indexOf("M") != -1 && _52e.getMonth() != _51b[1]) ||
                (_52f.indexOf("d") != -1 && _52e.getDate() != _51b[2])) {
                return null;
            }
            return _52e;
        };
        function _processPattern(_530, _531, _532, _533) {
            var _534 = function(x) {
                return x;
            };
            _531 = _531 || _534;
            _532 = _532 || _534;
            _533 = _533 || _534;
            var _536 = _530.match(/(''|[^'])+/g);
            var _537 = false;
            dojo.forEach(_536, function(_538, i) {
                if (!_538) {
                    _536[i] = "";
                } else {
                    _536[i] = (_537 ? _532 : _531)(_538);
                    _537 = !_537;
                }
            });
            return _533(_536.join(""));
        }
        ;
        function _buildDateTimeRE(_53a, _53b, _53c, _53d) {
            _53d = dojo.regexp.escapeString(_53d);
            if (!_53c.strict) {
                _53d = _53d.replace(" a", " ?a");
            }
            return _53d.replace(/([a-z])\1*/ig, function(_53e) {
                var s;
                var c = _53e.charAt(0);
                var l = _53e.length;
                var p2 = "",p3 = "";
                if (_53c.strict) {
                    if (l > 1) {
                        p2 = "0" + "{" + (l - 1) + "}";
                    }
                    if (l > 2) {
                        p3 = "0" + "{" + (l - 2) + "}";
                    }
                } else {
                    p2 = "0?";
                    p3 = "0{0,2}";
                }
                switch (c) {case "y":s = "\\d{2,4}";break;case "M":s = (l > 2) ? "\\S+" : p2 +
                                                                                          "[1-9]|1[0-2]";break;case "D":s =
                                                                                                                        p2 +
                                                                                                                        "[1-9]|" +
                                                                                                                        p3 +
                                                                                                                        "[1-9][0-9]|[12][0-9][0-9]|3[0-5][0-9]|36[0-6]";break;case "d":s =
                                                                                                                                                                                       p2 +
                                                                                                                                                                                       "[1-9]|[12]\\d|3[01]";break;case "w":s =
                                                                                                                                                                                                                            p2 +
                                                                                                                                                                                                                            "[1-9]|[1-4][0-9]|5[0-3]";break;case "E":s =
                                                                                                                                                                                                                                                                     "\\S+";break;case "h":s =
                                                                                                                                                                                                                                                                                           p2 +
                                                                                                                                                                                                                                                                                           "[1-9]|1[0-2]";break;case "k":s =
                                                                                                                                                                                                                                                                                                                         p2 +
                                                                                                                                                                                                                                                                                                                         "\\d|1[01]";break;case "H":s =
                                                                                                                                                                                                                                                                                                                                                    p2 +
                                                                                                                                                                                                                                                                                                                                                    "\\d|1\\d|2[0-3]";break;case "K":s =
                                                                                                                                                                                                                                                                                                                                                                                     p2 +
                                                                                                                                                                                                                                                                                                                                                                                     "[1-9]|1\\d|2[0-4]";break;case "m":case "s":s =
                                                                                                                                                                                                                                                                                                                                                                                                                                 "[0-5]\\d";break;case "S":s =
                                                                                                                                                                                                                                                                                                                                                                                                                                                           "\\d{" +
                                                                                                                                                                                                                                                                                                                                                                                                                                                           l +
                                                                                                                                                                                                                                                                                                                                                                                                                                                           "}";break;case "a":var am = _53c.am ||
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       _53b.am ||
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       "AM";var pm = _53c.pm ||
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     _53b.pm ||
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     "PM";if (_53c.strict) {
                    s = am + "|" + pm;
                } else {
                    s = am + "|" + pm;
                    if (am != am.toLowerCase()) {
                        s += "|" + am.toLowerCase();
                    }
                    if (pm != pm.toLowerCase()) {
                        s += "|" + pm.toLowerCase();
                    }
                }break;default:s = ".*";}
                if (_53a) {
                    _53a.push(_53e);
                }
                return "(" + s + ")";
            }).replace(/[\xa0 ]/g, "[\\s\\xa0]");
        }
        ;
    })();
    (function() {
        var _546 = [];
        dojo.date.locale.addCustomFormats = function(_547, _548) {
            _546.push({pkg:_547,name:_548});
        };
        dojo.date.locale._getGregorianBundle = function(_549) {
            var _54a = {};
            dojo.forEach(_546, function(desc) {
                var _54c = dojo.i18n.getLocalization(desc.pkg, desc.name, _549);
                _54a = dojo.mixin(_54a, _54c);
            }, this);
            return _54a;
        };
    })();
    dojo.date.locale.addCustomFormats("dojo.cldr", "gregorian");
    dojo.date.locale.getNames = function(item, type, use, _550) {
        var _551;
        var _552 = dojo.date.locale._getGregorianBundle(_550);
        var _553 = [item,use,type];
        if (use == "standAlone") {
            _551 = _552[_553.join("-")];
        }
        _553[1] = "format";
        return (_551 || _552[_553.join("-")]).concat();
    };
    dojo.date.locale.isWeekend = function(_554, _555) {
        var _556 = dojo.cldr.supplemental.getWeekend(_555);
        var day = (_554 || new Date()).getDay();
        if (_556.end < _556.start) {
            _556.end += 7;
            if (day < _556.start) {
                day += 7;
            }
        }
        return day >= _556.start && day <= _556.end;
    };
    dojo.date.locale._getDayOfYear = function(_558) {
        return dojo.date.difference(new Date(_558.getFullYear(), 0, 1), _558) + 1;
    };
    dojo.date.locale._getWeekOfYear = function(_559, _55a) {
        if (arguments.length == 1) {
            _55a = 0;
        }
        var _55b = new Date(_559.getFullYear(), 0, 1).getDay();
        var adj = (_55b - _55a + 7) % 7;
        var week = Math.floor((dojo.date.locale._getDayOfYear(_559) + adj - 1) / 7);
        if (_55b == _55a) {
            week++;
        }
        return week;
    };
}
if (!dojo._hasResource["dijit._Calendar"]) {
    dojo._hasResource["dijit._Calendar"] = true;
    dojo.provide("dijit._Calendar");
    dojo.declare("dijit._Calendar", [dijit._Widget,dijit._Templated], {templateString:"<table cellspacing=\"0\" cellpadding=\"0\" class=\"dijitCalendarContainer\">\n\t<thead>\n\t\t<tr class=\"dijitReset dijitCalendarMonthContainer\" valign=\"top\">\n\t\t\t<th class='dijitReset' dojoAttachPoint=\"decrementMonth\">\n\t\t\t\t<div class=\"dijitInline dijitCalendarIncrementControl dijitCalendarDecrease\"><span dojoAttachPoint=\"decreaseArrowNode\" class=\"dijitA11ySideArrow dijitCalendarIncrementControl dijitCalendarDecreaseInner\">-</span></div>\n\t\t\t</th>\n\t\t\t<th class='dijitReset' colspan=\"5\">\n\t\t\t\t<div dojoAttachPoint=\"monthLabelSpacer\" class=\"dijitCalendarMonthLabelSpacer\"></div>\n\t\t\t\t<div dojoAttachPoint=\"monthLabelNode\" class=\"dijitCalendarMonthLabel\"></div>\n\t\t\t</th>\n\t\t\t<th class='dijitReset' dojoAttachPoint=\"incrementMonth\">\n\t\t\t\t<div class=\"dijitInline dijitCalendarIncrementControl dijitCalendarIncrease\"><span dojoAttachPoint=\"increaseArrowNode\" class=\"dijitA11ySideArrow dijitCalendarIncrementControl dijitCalendarIncreaseInner\">+</span></div>\n\t\t\t</th>\n\t\t</tr>\n\t\t<tr>\n\t\t\t<th class=\"dijitReset dijitCalendarDayLabelTemplate\"><span class=\"dijitCalendarDayLabel\"></span></th>\n\t\t</tr>\n\t</thead>\n\t<tbody dojoAttachEvent=\"onclick: _onDayClick\" class=\"dijitReset dijitCalendarBodyContainer\">\n\t\t<tr class=\"dijitReset dijitCalendarWeekTemplate\">\n\t\t\t<td class=\"dijitReset dijitCalendarDateTemplate\"><span class=\"dijitCalendarDateLabel\"></span></td>\n\t\t</tr>\n\t</tbody>\n\t<tfoot class=\"dijitReset dijitCalendarYearContainer\">\n\t\t<tr>\n\t\t\t<td class='dijitReset' valign=\"top\" colspan=\"7\">\n\t\t\t\t<h3 class=\"dijitCalendarYearLabel\">\n\t\t\t\t\t<span dojoAttachPoint=\"previousYearLabelNode\" class=\"dijitInline dijitCalendarPreviousYear\"></span>\n\t\t\t\t\t<span dojoAttachPoint=\"currentYearLabelNode\" class=\"dijitInline dijitCalendarSelectedYear\"></span>\n\t\t\t\t\t<span dojoAttachPoint=\"nextYearLabelNode\" class=\"dijitInline dijitCalendarNextYear\"></span>\n\t\t\t\t</h3>\n\t\t\t</td>\n\t\t</tr>\n\t</tfoot>\n</table>\t\n",value:new Date(),dayWidth:"narrow",setValue:function(
            _55e) {
        if (!this.value || dojo.date.compare(_55e, this.value)) {
            _55e = new Date(_55e);
            this.displayMonth = new Date(_55e);
            if (!this.isDisabledDate(_55e, this.lang)) {
                this.value = _55e;
                this.value.setHours(0, 0, 0, 0);
                this.onChange(this.value);
            }
            this._populateGrid();
        }
    },_setText:function(node, text) {
        while (node.firstChild) {
            node.removeChild(node.firstChild);
        }
        node.appendChild(dojo.doc.createTextNode(text));
    },_populateGrid:function() {
        var _561 = this.displayMonth;
        _561.setDate(1);
        var _562 = _561.getDay();
        var _563 = dojo.date.getDaysInMonth(_561);
        var _564 = dojo.date.getDaysInMonth(dojo.date.add(_561, "month", -1));
        var _565 = new Date();
        var _566 = this.value;
        var _567 = dojo.cldr.supplemental.getFirstDayOfWeek(this.lang);
        if (_567 > _562) {
            _567 -= 7;
        }
        dojo.query(".dijitCalendarDateTemplate", this.domNode).forEach(function(_568, i) {
            i += _567;
            var date = new Date(_561);
            var _56b,_56c = "dijitCalendar",adj = 0;
            if (i < _562) {
                _56b = _564 - _562 + i + 1;
                adj = -1;
                _56c += "Previous";
            } else {
                if (i >= (_562 + _563)) {
                    _56b = i - _562 - _563 + 1;
                    adj = 1;
                    _56c += "Next";
                } else {
                    _56b = i - _562 + 1;
                    _56c += "Current";
                }
            }
            if (adj) {
                date = dojo.date.add(date, "month", adj);
            }
            date.setDate(_56b);
            if (!dojo.date.compare(date, _565, "date")) {
                _56c = "dijitCalendarCurrentDate " + _56c;
            }
            if (!dojo.date.compare(date, _566, "date")) {
                _56c = "dijitCalendarSelectedDate " + _56c;
            }
            if (this.isDisabledDate(date, this.lang)) {
                _56c = "dijitCalendarDisabledDate " + _56c;
            }
            var _56e = this.getClassForDate(date, this.lang);
            if (_56e) {
                _56c += _56e + " " + _56c;
            }
            _568.className = _56c + "Month dijitCalendarDateTemplate";
            _568.dijitDateValue = date.valueOf();
            var _56f = dojo.query(".dijitCalendarDateLabel", _568)[0];
            this._setText(_56f, date.getDate());
        }, this);
        var _570 = dojo.date.locale.getNames("months", "wide", "standAlone", this.lang);
        this._setText(this.monthLabelNode, _570[_561.getMonth()]);
        var y = _561.getFullYear() - 1;
        var d = new Date();
        dojo.forEach(["previous","current","next"], function(name) {
            d.setFullYear(y++);
            this._setText(this[name +
                               "YearLabelNode"], dojo.date.locale.format(d, {selector:"year",locale:this.lang}));
        }, this);
        var _574 = this;
        var _575 = function(_576, _577, adj) {
            dijit.typematic.addMouseListener(_574[_576], _574, function(_579) {
                if (_579 >= 0) {
                    _574._adjustDisplay(_577, adj);
                }
            }, 0.8, 500);
        };
        _575("incrementMonth", "month", 1);
        _575("decrementMonth", "month", -1);
        _575("nextYearLabelNode", "year", 1);
        _575("previousYearLabelNode", "year", -1);
    },goToToday:function() {
        this.setValue(new Date());
    },postCreate:function() {
        this.inherited(arguments);
        var _57a = dojo.hitch(this, function(_57b, n) {
            var _57d = dojo.query(_57b, this.domNode)[0];
            for (var i = 0; i < n; i++) {
                _57d.parentNode.appendChild(_57d.cloneNode(true));
            }
        });
        _57a(".dijitCalendarDayLabelTemplate", 6);
        _57a(".dijitCalendarDateTemplate", 6);
        _57a(".dijitCalendarWeekTemplate", 5);
        var _57f = dojo.date.locale.getNames("days", this.dayWidth, "standAlone", this.lang);
        var _580 = dojo.cldr.supplemental.getFirstDayOfWeek(this.lang);
        dojo.query(".dijitCalendarDayLabel", this.domNode).forEach(function(_581, i) {
            this._setText(_581, _57f[(i + _580) % 7]);
        }, this);
        var _583 = dojo.date.locale.getNames("months", "wide", "standAlone", this.lang);
        dojo.forEach(_583, function(name) {
            var _585 = dojo.doc.createElement("div");
            this._setText(_585, name);
            this.monthLabelSpacer.appendChild(_585);
        }, this);
        this.value = null;
        this.setValue(new Date());
    },_adjustDisplay:function(part, _587) {
        this.displayMonth = dojo.date.add(this.displayMonth, part, _587);
        this._populateGrid();
    },_onDayClick:function(evt) {
        var node = evt.target;
        dojo.stopEvent(evt);
        while (!node.dijitDateValue) {
            node = node.parentNode;
        }
        if (!dojo.hasClass(node, "dijitCalendarDisabledDate")) {
            this.setValue(node.dijitDateValue);
            this.onValueSelected(this.value);
        }
    },onValueSelected:function(date) {
    },onChange:function(date) {
    },isDisabledDate:function(_58c, _58d) {
    },getClassForDate:function(_58e, _58f) {
    }});
}
if (!dojo._hasResource["dijit.form._DateTimeTextBox"]) {
    dojo._hasResource["dijit.form._DateTimeTextBox"] = true;
    dojo.provide("dijit.form._DateTimeTextBox");
    dojo.declare("dijit.form._DateTimeTextBox", dijit.form.RangeBoundTextBox, {regExpGen:dojo.date.locale.regexp,compare:dojo.date.compare,format:function(
            _590, _591) {
        if (!_590) {
            return "";
        }
        return dojo.date.locale.format(_590, _591);
    },parse:function(_592, _593) {
        return dojo.date.locale.parse(_592, _593) || undefined;
    },serialize:dojo.date.stamp.toISOString,value:new Date(""),popupClass:"",_selector:"",postMixInProperties:function() {
        this.inherited(arguments);
        if (!this.value ||
            this.value.toString() == dijit.form._DateTimeTextBox.prototype.value.toString()) {
            this.value = undefined;
        }
        var _594 = this.constraints;
        _594.selector = this._selector;
        _594.fullYear = true;
        var _595 = dojo.date.stamp.fromISOString;
        if (typeof _594.min == "string") {
            _594.min = _595(_594.min);
        }
        if (typeof _594.max == "string") {
            _594.max = _595(_594.max);
        }
    },_onFocus:function(evt) {
        this._open();
    },setValue:function(_597, _598, _599) {
        this.inherited(arguments);
        if (this._picker) {
            if (!_597) {
                _597 = new Date();
            }
            this._picker.setValue(_597);
        }
    },_open:function() {
        if (this.disabled || this.readOnly || !this.popupClass) {
            return;
        }
        var _59a = this;
        if (!this._picker) {
            var _59b = dojo.getObject(this.popupClass, false);
            this._picker = new _59b({onValueSelected:function(_59c) {
                _59a.focus();
                setTimeout(dojo.hitch(_59a, "_close"), 1);
                dijit.form._DateTimeTextBox.superclass.setValue.call(_59a, _59c, true);
            },lang:_59a.lang,constraints:_59a.constraints,isDisabledDate:function(date) {
                var _59e = dojo.date.compare;
                var _59f = _59a.constraints;
                return _59f && (_59f.min && (_59e(_59f.min, date, "date") > 0) ||
                                (_59f.max && _59e(_59f.max, date, "date") < 0));
            }});
            this._picker.setValue(this.getValue() || new Date());
        }
        if (!this._opened) {
            dijit.popup.open({parent:this,popup:this._picker,around:this.domNode,onCancel:dojo.hitch(this, this._close),onClose:function() {
                _59a._opened = false;
            }});
            this._opened = true;
        }
        dojo.marginBox(this._picker.domNode, {w:this.domNode.offsetWidth});
    },_close:function() {
        if (this._opened) {
            dijit.popup.close(this._picker);
            this._opened = false;
        }
    },_onBlur:function() {
        this._close();
        if (this._picker) {
            this._picker.destroy();
            delete this._picker;
        }
        this.inherited(arguments);
    },getDisplayedValue:function() {
        return this.textbox.value;
    },setDisplayedValue:function(_5a0, _5a1) {
        this.setValue(this.parse(_5a0, this.constraints), _5a1, _5a0);
    },destroy:function() {
        if (this._picker) {
            this._picker.destroy();
            delete this._picker;
        }
        this.inherited(arguments);
    },_onKeyPress:function(e) {
        if (dijit.form._DateTimeTextBox.superclass._onKeyPress.apply(this, arguments)) {
            if (this._opened && e.keyCode == dojo.keys.ESCAPE && !e.shiftKey && !e.ctrlKey &&
                !e.altKey) {
                this._close();
                dojo.stopEvent(e);
            }
        }
    }});
}
if (!dojo._hasResource["dijit.form.DateTextBox"]) {
    dojo._hasResource["dijit.form.DateTextBox"] = true;
    dojo.provide("dijit.form.DateTextBox");
    dojo.declare("dijit.form.DateTextBox", dijit.form._DateTimeTextBox, {popupClass:"dijit._Calendar",_selector:"date"});
}
if (!dojo._hasResource["dijit.form.FilteringSelect"]) {
    dojo._hasResource["dijit.form.FilteringSelect"] = true;
    dojo.provide("dijit.form.FilteringSelect");
    dojo.declare("dijit.form.FilteringSelect", [dijit.form.MappedTextBox,dijit.form.ComboBoxMixin], {labelAttr:"",labelType:"text",_isvalid:true,_lastDisplayedValue:"",isValid:function() {
        return this._isvalid;
    },_callbackSetLabel:function(_5a3, _5a4, _5a5) {
        if (_5a4 && _5a4.query[this.searchAttr] != this._lastQuery) {
            return;
        }
        if (!_5a3.length) {
            if (!this._focused) {
                this.valueNode.value = "";
            }
            dijit.form.TextBox.superclass.setValue.call(this, undefined, !this._focused);
            this._isvalid = false;
            this.validate(this._focused);
        } else {
            this._setValueFromItem(_5a3[0], _5a5);
        }
    },_openResultList:function(_5a6, _5a7) {
        if (_5a7.query[this.searchAttr] != this._lastQuery) {
            return;
        }
        this._isvalid = _5a6.length != 0;
        this.validate(true);
        dijit.form.ComboBoxMixin.prototype._openResultList.apply(this, arguments);
    },getValue:function() {
        return this.valueNode.value;
    },_getValueField:function() {
        return "value";
    },_setValue:function(_5a8, _5a9, _5aa) {
        this.valueNode.value = _5a8;
        dijit.form.FilteringSelect.superclass.setValue.call(this, _5a8, _5aa, _5a9);
        this._lastDisplayedValue = _5a9;
    },setValue:function(_5ab, _5ac) {
        var self = this;
        var _5ae = function(item, _5b0) {
            if (item) {
                if (self.store.isItemLoaded(item)) {
                    self._callbackSetLabel([item], undefined, _5b0);
                } else {
                    self.store.loadItem({item:item,onItem:function(_5b1, _5b2) {
                        self._callbackSetLabel(_5b1, _5b2, _5b0);
                    }});
                }
            } else {
                self._isvalid = false;
                self.validate(false);
            }
        };
        this.store.fetchItemByIdentity({identity:_5ab,onItem:function(item) {
            _5ae(item, _5ac);
        }});
    },_setValueFromItem:function(item, _5b5) {
        this._isvalid = true;
        this._setValue(this.store.getIdentity(item), this.labelFunc(item, this.store), _5b5);
    },labelFunc:function(item, _5b7) {
        return _5b7.getValue(item, this.searchAttr);
    },_doSelect:function(tgt) {
        this.item = tgt.item;
        this._setValueFromItem(tgt.item, true);
    },setDisplayedValue:function(_5b9, _5ba) {
        if (this.store) {
            var _5bb = dojo.clone(this.query);
            this._lastQuery = _5bb[this.searchAttr] = _5b9;
            this.textbox.value = _5b9;
            this._lastDisplayedValue = _5b9;
            var _5bc = this;
            this.store.fetch({query:_5bb,queryOptions:{ignoreCase:this.ignoreCase,deep:true},onComplete:function(
                    _5bd, _5be) {
                dojo.hitch(_5bc, "_callbackSetLabel")(_5bd, _5be, _5ba);
            },onError:function(_5bf) {
                console.error("dijit.form.FilteringSelect: " + _5bf);
                dojo.hitch(_5bc, "_setValue")(undefined, _5b9, false);
            }});
        }
    },_getMenuLabelFromItem:function(item) {
        if (this.labelAttr) {
            return {html:this.labelType == "html",label:this.store.getValue(item, this.labelAttr)};
        } else {
            return dijit.form.ComboBoxMixin.prototype._getMenuLabelFromItem.apply(this, arguments);
        }
    },postMixInProperties:function() {
        dijit.form.ComboBoxMixin.prototype.postMixInProperties.apply(this, arguments);
        dijit.form.MappedTextBox.prototype.postMixInProperties.apply(this, arguments);
    },postCreate:function() {
        dijit.form.ComboBoxMixin.prototype._postCreate.apply(this, arguments);
        dijit.form.MappedTextBox.prototype.postCreate.apply(this, arguments);
    },setAttribute:function(attr, _5c2) {
        dijit.form.MappedTextBox.prototype.setAttribute.apply(this, arguments);
        dijit.form.ComboBoxMixin.prototype._setAttribute.apply(this, arguments);
    },undo:function() {
        this.setDisplayedValue(this._lastDisplayedValue);
    },_valueChanged:function() {
        return this.getDisplayedValue() != this._lastDisplayedValue;
    }});
}
if (!dojo._hasResource["dijit.form._Spinner"]) {
    dojo._hasResource["dijit.form._Spinner"] = true;
    dojo.provide("dijit.form._Spinner");
    dojo.declare("dijit.form._Spinner", dijit.form.RangeBoundTextBox, {defaultTimeout:500,timeoutChangeRate:0.9,smallDelta:1,largeDelta:10,templateString:"<div class=\"dijit dijitReset dijitInlineTable dijitLeft\"\n\tid=\"widget_${id}\"\n\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\" waiRole=\"presentation\"\n\t><div class=\"dijitInputLayoutContainer\"\n\t\t><div class=\"dijitReset dijitSpinnerButtonContainer\"\n\t\t\t>&nbsp;<div class=\"dijitReset dijitLeft dijitButtonNode dijitArrowButton dijitUpArrowButton\"\n\t\t\t\tdojoAttachPoint=\"upArrowNode\"\n\t\t\t\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse\"\n\t\t\t\tstateModifier=\"UpArrow\"\n\t\t\t\t><div class=\"dijitArrowButtonInner\">&thinsp;</div\n\t\t\t\t><div class=\"dijitArrowButtonChar\">&#9650;</div\n\t\t\t></div\n\t\t\t><div class=\"dijitReset dijitLeft dijitButtonNode dijitArrowButton dijitDownArrowButton\"\n\t\t\t\tdojoAttachPoint=\"downArrowNode\"\n\t\t\t\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse\"\n\t\t\t\tstateModifier=\"DownArrow\"\n\t\t\t\t><div class=\"dijitArrowButtonInner\">&thinsp;</div\n\t\t\t\t><div class=\"dijitArrowButtonChar\">&#9660;</div\n\t\t\t></div\n\t\t></div\n\t\t><div class=\"dijitReset dijitValidationIcon\"><br></div\n\t\t><div class=\"dijitReset dijitValidationIconText\">&Chi;</div\n\t\t><div class=\"dijitReset dijitInputField\"\n\t\t\t><input class='dijitReset' dojoAttachPoint=\"textbox,focusNode\" type=\"${type}\" dojoAttachEvent=\"onfocus:_update,onkeyup:_onkeyup,onkeypress:_onKeyPress\"\n\t\t\t\twaiRole=\"spinbutton\" autocomplete=\"off\" name=\"${name}\"\n\t\t/></div\n\t></div\n></div>\n",baseClass:"dijitSpinner",adjust:function(
            val, _5c4) {
        return val;
    },_arrowState:function(node, _5c6) {
        this._active = _5c6;
        this.stateModifier = node.getAttribute("stateModifier") || "";
        this._setStateClass();
    },_arrowPressed:function(_5c7, _5c8) {
        if (this.disabled || this.readOnly) {
            return;
        }
        this._arrowState(_5c7, true);
        this.setValue(this.adjust(this.getValue(), _5c8 * this.smallDelta), false);
        dijit.selectInputText(this.textbox, this.textbox.value.length);
    },_arrowReleased:function(node) {
        this._wheelTimer = null;
        if (this.disabled || this.readOnly) {
            return;
        }
        this._arrowState(node, false);
    },_typematicCallback:function(_5ca, node, evt) {
        if (node == this.textbox) {
            node = (evt.keyCode == dojo.keys.UP_ARROW) ? this.upArrowNode : this.downArrowNode;
        }
        if (_5ca == -1) {
            this._arrowReleased(node);
        } else {
            this._arrowPressed(node, (node == this.upArrowNode) ? 1 : -1);
        }
    },_wheelTimer:null,_mouseWheeled:function(evt) {
        dojo.stopEvent(evt);
        var _5ce = 0;
        if (typeof evt.wheelDelta == "number") {
            _5ce = evt.wheelDelta;
        } else {
            if (typeof evt.detail == "number") {
                _5ce = -evt.detail;
            }
        }
        var node,dir;
        if (_5ce > 0) {
            node = this.upArrowNode;
            dir = +1;
        } else {
            if (_5ce < 0) {
                node = this.downArrowNode;
                dir = -1;
            } else {
                return;
            }
        }
        this._arrowPressed(node, dir);
        if (this._wheelTimer != null) {
            clearTimeout(this._wheelTimer);
        }
        var _5d1 = this;
        this._wheelTimer = setTimeout(function() {
            _5d1._arrowReleased(node);
        }, 50);
    },postCreate:function() {
        this.inherited("postCreate", arguments);
        this.connect(this.textbox, dojo.isIE ? "onmousewheel" : "DOMMouseScroll", "_mouseWheeled");
        this._connects.push(dijit.typematic.addListener(this.upArrowNode, this.textbox, {keyCode:dojo.keys.UP_ARROW,ctrlKey:false,altKey:false,shiftKey:false}, this, "_typematicCallback", this.timeoutChangeRate, this.defaultTimeout));
        this._connects.push(dijit.typematic.addListener(this.downArrowNode, this.textbox, {keyCode:dojo.keys.DOWN_ARROW,ctrlKey:false,altKey:false,shiftKey:false}, this, "_typematicCallback", this.timeoutChangeRate, this.defaultTimeout));
        if (dojo.isIE) {
            var _5d2 = this;
            this.connect(this.domNode, "onresize", function() {
                setTimeout(dojo.hitch(_5d2, function() {
                    this.upArrowNode.style.behavior = "";
                    this.downArrowNode.style.behavior = "";
                    this._setStateClass();
                }), 0);
            });
        }
    }});
}
if (!dojo._hasResource["dijit.form.NumberSpinner"]) {
    dojo._hasResource["dijit.form.NumberSpinner"] = true;
    dojo.provide("dijit.form.NumberSpinner");
    dojo.declare("dijit.form.NumberSpinner", [dijit.form._Spinner,dijit.form.NumberTextBoxMixin], {required:true,adjust:function(
            val, _5d4) {
        var _5d5 = val + _5d4;
        if (isNaN(val) || isNaN(_5d5)) {
            return val;
        }
        if ((typeof this.constraints.max == "number") && (_5d5 > this.constraints.max)) {
            _5d5 = this.constraints.max;
        }
        if ((typeof this.constraints.min == "number") && (_5d5 < this.constraints.min)) {
            _5d5 = this.constraints.min;
        }
        return _5d5;
    }});
}
if (!dojo._hasResource["dojo.dnd.move"]) {
    dojo._hasResource["dojo.dnd.move"] = true;
    dojo.provide("dojo.dnd.move");
    dojo.declare("dojo.dnd.move.constrainedMoveable", dojo.dnd.Moveable, {constraints:function() {
    },within:false,markupFactory:function(_5d6, node) {
        return new dojo.dnd.move.constrainedMoveable(node, _5d6);
    },constructor:function(node, _5d9) {
        if (!_5d9) {
            _5d9 = {};
        }
        this.constraints = _5d9.constraints;
        this.within = _5d9.within;
    },onFirstMove:function(_5da) {
        var c = this.constraintBox = this.constraints.call(this, _5da);
        c.r = c.l + c.w;
        c.b = c.t + c.h;
        if (this.within) {
            var mb = dojo.marginBox(_5da.node);
            c.r -= mb.w;
            c.b -= mb.h;
        }
    },onMove:function(_5dd, _5de) {
        var c = this.constraintBox,s = _5dd.node.style;
        s.left = (_5de.l < c.l ? c.l : c.r < _5de.l ? c.r : _5de.l) + "px";
        s.top = (_5de.t < c.t ? c.t : c.b < _5de.t ? c.b : _5de.t) + "px";
    }});
    dojo.declare("dojo.dnd.move.boxConstrainedMoveable", dojo.dnd.move.constrainedMoveable, {box:{},markupFactory:function(
            _5e1, node) {
        return new dojo.dnd.move.boxConstrainedMoveable(node, _5e1);
    },constructor:function(node, _5e4) {
        var box = _5e4 && _5e4.box;
        this.constraints = function() {
            return box;
        };
    }});
    dojo.declare("dojo.dnd.move.parentConstrainedMoveable", dojo.dnd.move.constrainedMoveable, {area:"content",markupFactory:function(
            _5e6, node) {
        return new dojo.dnd.move.parentConstrainedMoveable(node, _5e6);
    },constructor:function(node, _5e9) {
        var area = _5e9 && _5e9.area;
        this.constraints = function() {
            var n = this.node.parentNode,s = dojo.getComputedStyle(n),mb = dojo._getMarginBox(n, s);
            if (area == "margin") {
                return mb;
            }
            var t = dojo._getMarginExtents(n, s);
            mb.l += t.l,mb.t += t.t,mb.w -= t.w,mb.h -= t.h;
            if (area == "border") {
                return mb;
            }
            t = dojo._getBorderExtents(n, s);
            mb.l += t.l,mb.t += t.t,mb.w -= t.w,mb.h -= t.h;
            if (area == "padding") {
                return mb;
            }
            t = dojo._getPadExtents(n, s);
            mb.l += t.l,mb.t += t.t,mb.w -= t.w,mb.h -= t.h;
            return mb;
        };
    }});
    dojo.dnd.move.constrainedMover = function(fun, _5f0) {
        dojo.deprecated("dojo.dnd.move.constrainedMover, use dojo.dnd.move.constrainedMoveable instead");
        var _5f1 = function(node, e, _5f4) {
            dojo.dnd.Mover.call(this, node, e, _5f4);
        };
        dojo.extend(_5f1, dojo.dnd.Mover.prototype);
        dojo.extend(_5f1, {onMouseMove:function(e) {
            dojo.dnd.autoScroll(e);
            var m = this.marginBox,c = this.constraintBox,l = m.l + e.pageX,t = m.t + e.pageY;
            l = l < c.l ? c.l : c.r < l ? c.r : l;
            t = t < c.t ? c.t : c.b < t ? c.b : t;
            this.host.onMove(this, {l:l,t:t});
        },onFirstMove:function() {
            dojo.dnd.Mover.prototype.onFirstMove.call(this);
            var c = this.constraintBox = fun.call(this);
            c.r = c.l + c.w;
            c.b = c.t + c.h;
            if (_5f0) {
                var mb = dojo.marginBox(this.node);
                c.r -= mb.w;
                c.b -= mb.h;
            }
        }});
        return _5f1;
    };
    dojo.dnd.move.boxConstrainedMover = function(box, _5fd) {
        dojo.deprecated("dojo.dnd.move.boxConstrainedMover, use dojo.dnd.move.boxConstrainedMoveable instead");
        return dojo.dnd.move.constrainedMover(function() {
            return box;
        }, _5fd);
    };
    dojo.dnd.move.parentConstrainedMover = function(area, _5ff) {
        dojo.deprecated("dojo.dnd.move.parentConstrainedMover, use dojo.dnd.move.parentConstrainedMoveable instead");
        var fun = function() {
            var n = this.node.parentNode,s = dojo.getComputedStyle(n),mb = dojo._getMarginBox(n, s);
            if (area == "margin") {
                return mb;
            }
            var t = dojo._getMarginExtents(n, s);
            mb.l += t.l,mb.t += t.t,mb.w -= t.w,mb.h -= t.h;
            if (area == "border") {
                return mb;
            }
            t = dojo._getBorderExtents(n, s);
            mb.l += t.l,mb.t += t.t,mb.w -= t.w,mb.h -= t.h;
            if (area == "padding") {
                return mb;
            }
            t = dojo._getPadExtents(n, s);
            mb.l += t.l,mb.t += t.t,mb.w -= t.w,mb.h -= t.h;
            return mb;
        };
        return dojo.dnd.move.constrainedMover(fun, _5ff);
    };
    dojo.dnd.constrainedMover = dojo.dnd.move.constrainedMover;
    dojo.dnd.boxConstrainedMover = dojo.dnd.move.boxConstrainedMover;
    dojo.dnd.parentConstrainedMover = dojo.dnd.move.parentConstrainedMover;
}
if (!dojo._hasResource["dijit.form.Slider"]) {
    dojo._hasResource["dijit.form.Slider"] = true;
    dojo.provide("dijit.form.Slider");
    dojo.declare("dijit.form.HorizontalSlider", [dijit.form._FormValueWidget,dijit._Container], {templateString:"<table class=\"dijit dijitReset dijitSlider\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" rules=\"none\"\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\" colspan=\"2\"></td\n\t\t><td dojoAttachPoint=\"containerNode,topDecoration\" class=\"dijitReset\" style=\"text-align:center;width:100%;\"></td\n\t\t><td class=\"dijitReset\" colspan=\"2\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerH\"\n\t\t\t><div class=\"dijitSliderDecrementIconH\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"decrementButton\" dojoAttachEvent=\"onclick: decrement\"><span class=\"dijitSliderButtonInner\">-</span></div\n\t\t></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperH dijitSliderLeftBumper dijitSliderLeftBumper\" dojoAttachEvent=\"onclick:_onClkDecBumper\"></div\n\t\t></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><input dojoAttachPoint=\"valueNode\" type=\"hidden\" name=\"${name}\"\n\t\t\t/><div waiRole=\"presentation\" style=\"position:relative;\" dojoAttachPoint=\"sliderBarContainer\"\n\t\t\t\t><div waiRole=\"presentation\" dojoAttachPoint=\"progressBar\" class=\"dijitSliderBar dijitSliderBarH dijitSliderProgressBar dijitSliderProgressBarH\" dojoAttachEvent=\"onclick:_onBarClick\"\n\t\t\t\t\t><div dojoAttachPoint=\"sliderHandle,focusNode\" class=\"dijitSliderMoveable dijitSliderMoveableH\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_onHandleClick\" waiRole=\"slider\" valuemin=\"${minimum}\" valuemax=\"${maximum}\"\n\t\t\t\t\t\t><div class=\"dijitSliderImageHandle dijitSliderImageHandleH\"></div\n\t\t\t\t\t></div\n\t\t\t\t></div\n\t\t\t\t><div waiRole=\"presentation\" dojoAttachPoint=\"remainingBar\" class=\"dijitSliderBar dijitSliderBarH dijitSliderRemainingBar dijitSliderRemainingBarH\" dojoAttachEvent=\"onclick:_onBarClick\"></div\n\t\t\t></div\n\t\t></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperH dijitSliderRightBumper dijitSliderRightBumper\" dojoAttachEvent=\"onclick:_onClkIncBumper\"></div\n\t\t></td\n\t\t><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerH\" style=\"right:0px;\"\n\t\t\t><div class=\"dijitSliderIncrementIconH\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"incrementButton\" dojoAttachEvent=\"onclick: increment\"><span class=\"dijitSliderButtonInner\">+</span></div\n\t\t></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\" colspan=\"2\"></td\n\t\t><td dojoAttachPoint=\"containerNode,bottomDecoration\" class=\"dijitReset\" style=\"text-align:center;\"></td\n\t\t><td class=\"dijitReset\" colspan=\"2\"></td\n\t></tr\n></table>\n",value:0,showButtons:true,minimum:0,maximum:100,discreteValues:Infinity,pageIncrement:2,clickSelect:true,slideDuration:1000,widgetsInTemplate:true,attributeMap:dojo.mixin(dojo.clone(dijit.form._FormWidget.prototype.attributeMap), {id:"",name:"valueNode"}),baseClass:"dijitSlider",_mousePixelCoord:"pageX",_pixelCount:"w",_startingPixelCoord:"x",_startingPixelCount:"l",_handleOffsetCoord:"left",_progressPixelSize:"width",_onKeyPress:function(
            e) {
        if (this.disabled || this.readOnly || e.altKey || e.ctrlKey) {
            return;
        }
        switch (e.keyCode) {case dojo.keys.HOME:this.setValue(this.minimum, true);break;case dojo.keys.END:this.setValue(this.maximum, true);break;case (
                (this._descending || this.isLeftToRight()) ? dojo.keys.RIGHT_ARROW :
                dojo.keys.LEFT_ARROW):case (this._descending === false ? dojo.keys.DOWN_ARROW :
                                            dojo.keys.UP_ARROW):case (this._descending === false ?
                                                                      dojo.keys.PAGE_DOWN :
                                                                      dojo.keys.PAGE_UP):this.increment(e);break;case (
                (this._descending || this.isLeftToRight()) ? dojo.keys.LEFT_ARROW :
                dojo.keys.RIGHT_ARROW):case (this._descending === false ? dojo.keys.UP_ARROW :
                                             dojo.keys.DOWN_ARROW):case (
                this._descending === false ? dojo.keys.PAGE_UP :
                dojo.keys.PAGE_DOWN):this.decrement(e);break;default:this.inherited(arguments);return;}
        dojo.stopEvent(e);
    },_onHandleClick:function(e) {
        if (this.disabled || this.readOnly) {
            return;
        }
        if (!dojo.isIE) {
            dijit.focus(this.sliderHandle);
        }
        dojo.stopEvent(e);
    },_isReversed:function() {
        return !this.isLeftToRight();
    },_onBarClick:function(e) {
        if (this.disabled || this.readOnly || !this.clickSelect) {
            return;
        }
        dijit.focus(this.sliderHandle);
        dojo.stopEvent(e);
        var _608 = dojo.coords(this.sliderBarContainer, true);
        var _609 = e[this._mousePixelCoord] - _608[this._startingPixelCoord];
        this._setPixelValue(this._isReversed() ? (_608[this._pixelCount] - _609) :
                            _609, _608[this._pixelCount], true);
    },_setPixelValue:function(_60a, _60b, _60c) {
        if (this.disabled || this.readOnly) {
            return;
        }
        _60a = _60a < 0 ? 0 : _60b < _60a ? _60b : _60a;
        var _60d = this.discreteValues;
        if (_60d <= 1 || _60d == Infinity) {
            _60d = _60b;
        }
        _60d--;
        var _60e = _60b / _60d;
        var _60f = Math.round(_60a / _60e);
        this.setValue((this.maximum - this.minimum) * _60f / _60d + this.minimum, _60c);
    },setValue:function(_610, _611) {
        this.valueNode.value = this.value = _610;
        dijit.setWaiState(this.focusNode, "valuenow", _610);
        this.inherited(arguments);
        var _612 = (_610 - this.minimum) / (this.maximum - this.minimum);
        var _613 = (this._descending === false) ? this.remainingBar : this.progressBar;
        var _614 = (this._descending === false) ? this.progressBar : this.remainingBar;
        if (_611 && this.slideDuration > 0 && _613.style[this._progressPixelSize]) {
            var _615 = this;
            var _616 = {};
            var _617 = parseFloat(_613.style[this._progressPixelSize]);
            var _618 = this.slideDuration * (_612 - _617 / 100);
            if (_618 == 0) {
                return;
            }
            if (_618 < 0) {
                _618 = 0 - _618;
            }
            _616[this._progressPixelSize] = {start:_617,end:_612 * 100,units:"%"};
            dojo.animateProperty({node:_613,duration:_618,onAnimate:function(v) {
                _614.style[_615._progressPixelSize] =
                (100 - parseFloat(v[_615._progressPixelSize])) + "%";
            },properties:_616}).play();
        } else {
            _613.style[this._progressPixelSize] = (_612 * 100) + "%";
            _614.style[this._progressPixelSize] = ((1 - _612) * 100) + "%";
        }
    },_bumpValue:function(_61a) {
        if (this.disabled || this.readOnly) {
            return;
        }
        var s = dojo.getComputedStyle(this.sliderBarContainer);
        var c = dojo._getContentBox(this.sliderBarContainer, s);
        var _61d = this.discreteValues;
        if (_61d <= 1 || _61d == Infinity) {
            _61d = c[this._pixelCount];
        }
        _61d--;
        var _61e = (this.value - this.minimum) * _61d / (this.maximum - this.minimum) + _61a;
        if (_61e < 0) {
            _61e = 0;
        }
        if (_61e > _61d) {
            _61e = _61d;
        }
        _61e = _61e * (this.maximum - this.minimum) / _61d + this.minimum;
        this.setValue(_61e, true);
    },_onClkIncBumper:function() {
        this.setValue(this._descending === false ? this.minimum : this.maximum, true);
    },_onClkDecBumper:function() {
        this.setValue(this._descending === false ? this.maximum : this.minimum, true);
    },decrement:function(e) {
        this._bumpValue(e.keyCode == dojo.keys.PAGE_DOWN ? -this.pageIncrement : -1);
    },increment:function(e) {
        this._bumpValue(e.keyCode == dojo.keys.PAGE_UP ? this.pageIncrement : 1);
    },_mouseWheeled:function(evt) {
        dojo.stopEvent(evt);
        var _622 = 0;
        if (typeof evt.wheelDelta == "number") {
            _622 = evt.wheelDelta;
        } else {
            if (typeof evt.detail == "number") {
                _622 = -evt.detail;
            }
        }
        if (_622 > 0) {
            this.increment(evt);
        } else {
            if (_622 < 0) {
                this.decrement(evt);
            }
        }
    },startup:function() {
        dojo.forEach(this.getChildren(), function(_623) {
            if (this[_623.container] != this.containerNode) {
                this[_623.container].appendChild(_623.domNode);
            }
        }, this);
    },postCreate:function() {
        if (this.showButtons) {
            this.incrementButton.style.display = "";
            this.decrementButton.style.display = "";
        }
        this.connect(this.domNode, dojo.isIE ? "onmousewheel" : "DOMMouseScroll", "_mouseWheeled");
        var _624 = this;
        var _625 = function() {
            dijit.form._SliderMover.apply(this, arguments);
            this.widget = _624;
        };
        dojo.extend(_625, dijit.form._SliderMover.prototype);
        this._movable = new dojo.dnd.Moveable(this.sliderHandle, {mover:_625});
        dijit.setWaiState(this.focusNode, "valuemin", this.minimum);
        dijit.setWaiState(this.focusNode, "valuemax", this.maximum);
        this.inherited(arguments);
    },destroy:function() {
        this._movable.destroy();
        this.inherited(arguments);
    }});
    dojo.declare("dijit.form.VerticalSlider", dijit.form.HorizontalSlider, {templateString:"<table class=\"dijitReset dijitSlider\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" rules=\"none\"\n><tbody class=\"dijitReset\"\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\"></td\n\t\t><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerV\"\n\t\t\t><div class=\"dijitSliderIncrementIconV\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"incrementButton\" dojoAttachEvent=\"onclick:_topButtonClicked\"><span class=\"dijitSliderButtonInner\">+</span></div\n\t\t></td\n\t\t><td class=\"dijitReset\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\"></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><center><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperV dijitSliderTopBumper dijitSliderTopBumper\" dojoAttachEvent=\"onclick:_onClkIncBumper\"></div></center\n\t\t></td\n\t\t><td class=\"dijitReset\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td dojoAttachPoint=\"leftDecoration\" class=\"dijitReset\" style=\"text-align:center;height:100%;\"></td\n\t\t><td class=\"dijitReset\" style=\"height:100%;\"\n\t\t\t><input dojoAttachPoint=\"valueNode\" type=\"hidden\" name=\"${name}\"\n\t\t\t/><center waiRole=\"presentation\" style=\"position:relative;height:100%;\" dojoAttachPoint=\"sliderBarContainer\"\n\t\t\t\t><div waiRole=\"presentation\" dojoAttachPoint=\"remainingBar\" class=\"dijitSliderBar dijitSliderBarV dijitSliderRemainingBar dijitSliderRemainingBarV\" dojoAttachEvent=\"onclick:_onBarClick\"><!--#5629--></div\n\t\t\t\t><div waiRole=\"presentation\" dojoAttachPoint=\"progressBar\" class=\"dijitSliderBar dijitSliderBarV dijitSliderProgressBar dijitSliderProgressBarV\" dojoAttachEvent=\"onclick:_onBarClick\"\n\t\t\t\t\t><div dojoAttachPoint=\"sliderHandle,focusNode\" class=\"dijitSliderMoveable\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_onHandleClick\" style=\"vertical-align:top;\" waiRole=\"slider\" valuemin=\"${minimum}\" valuemax=\"${maximum}\"\n\t\t\t\t\t\t><div class=\"dijitSliderImageHandle dijitSliderImageHandleV\"></div\n\t\t\t\t\t></div\n\t\t\t\t></div\n\t\t\t></center\n\t\t></td\n\t\t><td dojoAttachPoint=\"containerNode,rightDecoration\" class=\"dijitReset\" style=\"text-align:center;height:100%;\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\"></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><center><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperV dijitSliderBottomBumper dijitSliderBottomBumper\" dojoAttachEvent=\"onclick:_onClkDecBumper\"></div></center\n\t\t></td\n\t\t><td class=\"dijitReset\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\"></td\n\t\t><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerV\"\n\t\t\t><div class=\"dijitSliderDecrementIconV\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"decrementButton\" dojoAttachEvent=\"onclick:_bottomButtonClicked\"><span class=\"dijitSliderButtonInner\">-</span></div\n\t\t></td\n\t\t><td class=\"dijitReset\"></td\n\t></tr\n></tbody></table>\n",_mousePixelCoord:"pageY",_pixelCount:"h",_startingPixelCoord:"y",_startingPixelCount:"t",_handleOffsetCoord:"top",_progressPixelSize:"height",_descending:true,startup:function() {
        if (this._started) {
            return;
        }
        if (!this.isLeftToRight() && dojo.isMoz) {
            if (this.leftDecoration) {
                this._rtlRectify(this.leftDecoration);
            }
            if (this.rightDecoration) {
                this._rtlRectify(this.rightDecoration);
            }
        }
        this.inherited(arguments);
    },_isReversed:function() {
        return this._descending;
    },_topButtonClicked:function(e) {
        if (this._descending) {
            this.increment(e);
        } else {
            this.decrement(e);
        }
    },_bottomButtonClicked:function(e) {
        if (this._descending) {
            this.decrement(e);
        } else {
            this.increment(e);
        }
    },_rtlRectify:function(_628) {
        var _629 = [];
        while (_628.firstChild) {
            _629.push(_628.firstChild);
            _628.removeChild(_628.firstChild);
        }
        for (var i = _629.length - 1; i >= 0; i--) {
            if (_629[i]) {
                _628.appendChild(_629[i]);
            }
        }
    }});
    dojo.declare("dijit.form._SliderMover", dojo.dnd.Mover, {onMouseMove:function(e) {
        var _62c = this.widget;
        var _62d = _62c._abspos;
        if (!_62d) {
            _62d = _62c._abspos = dojo.coords(_62c.sliderBarContainer, true);
            _62c._setPixelValue_ = dojo.hitch(_62c, "_setPixelValue");
            _62c._isReversed_ = _62c._isReversed();
        }
        var _62e = e[_62c._mousePixelCoord] - _62d[_62c._startingPixelCoord];
        _62c._setPixelValue_(_62c._isReversed_ ? (_62d[_62c._pixelCount] - _62e) :
                             _62e, _62d[_62c._pixelCount], false);
    },destroy:function(e) {
        dojo.dnd.Mover.prototype.destroy.apply(this, arguments);
        var _630 = this.widget;
        _630.setValue(_630.value, true);
    }});
    dojo.declare("dijit.form.HorizontalRule", [dijit._Widget,dijit._Templated], {templateString:"<div class=\"dijitRuleContainer dijitRuleContainerH\"></div>",count:3,container:"containerNode",ruleStyle:"",_positionPrefix:"<div class=\"dijitRuleMark dijitRuleMarkH\" style=\"left:",_positionSuffix:"%;",_suffix:"\"></div>",_genHTML:function(
            pos, ndx) {
        return this._positionPrefix + pos + this._positionSuffix + this.ruleStyle + this._suffix;
    },_isHorizontal:true,postCreate:function() {
        var _633;
        if (this.count == 1) {
            _633 = this._genHTML(50, 0);
        } else {
            var i;
            var _635 = 100 / (this.count - 1);
            if (!this._isHorizontal || this.isLeftToRight()) {
                _633 = this._genHTML(0, 0);
                for (i = 1; i < this.count - 1; i++) {
                    _633 += this._genHTML(_635 * i, i);
                }
                _633 += this._genHTML(100, this.count - 1);
            } else {
                _633 = this._genHTML(100, 0);
                for (i = 1; i < this.count - 1; i++) {
                    _633 += this._genHTML(100 - _635 * i, i);
                }
                _633 += this._genHTML(0, this.count - 1);
            }
        }
        this.domNode.innerHTML = _633;
    }});
    dojo.declare("dijit.form.VerticalRule", dijit.form.HorizontalRule, {templateString:"<div class=\"dijitRuleContainer dijitRuleContainerV\"></div>",_positionPrefix:"<div class=\"dijitRuleMark dijitRuleMarkV\" style=\"top:",_isHorizontal:false});
    dojo.declare("dijit.form.HorizontalRuleLabels", dijit.form.HorizontalRule, {templateString:"<div class=\"dijitRuleContainer dijitRuleContainerH\"></div>",labelStyle:"",labels:[],numericMargin:0,minimum:0,maximum:1,constraints:{pattern:"#%"},_positionPrefix:"<div class=\"dijitRuleLabelContainer dijitRuleLabelContainerH\" style=\"left:",_labelPrefix:"\"><span class=\"dijitRuleLabel dijitRuleLabelH\">",_suffix:"</span></div>",_calcPosition:function(
            pos) {
        return pos;
    },_genHTML:function(pos, ndx) {
        return this._positionPrefix + this._calcPosition(pos) + this._positionSuffix +
               this.labelStyle + this._labelPrefix + this.labels[ndx] + this._suffix;
    },getLabels:function() {
        var _639 = this.labels;
        if (!_639.length) {
            _639 = dojo.query("> li", this.srcNodeRef).map(function(node) {
                return String(node.innerHTML);
            });
        }
        this.srcNodeRef.innerHTML = "";
        if (!_639.length && this.count > 1) {
            var _63b = this.minimum;
            var inc = (this.maximum - _63b) / (this.count - 1);
            for (var i = 0; i < this.count; i++) {
                _639.push((i < this.numericMargin || i >= (this.count - this.numericMargin)) ? "" :
                          dojo.number.format(_63b, this.constraints));
                _63b += inc;
            }
        }
        return _639;
    },postMixInProperties:function() {
        this.inherited(arguments);
        this.labels = this.getLabels();
        this.count = this.labels.length;
    }});
    dojo.declare("dijit.form.VerticalRuleLabels", dijit.form.HorizontalRuleLabels, {templateString:"<div class=\"dijitRuleContainer dijitRuleContainerV\"></div>",_positionPrefix:"<div class=\"dijitRuleLabelContainer dijitRuleLabelContainerV\" style=\"top:",_labelPrefix:"\"><span class=\"dijitRuleLabel dijitRuleLabelV\">",_calcPosition:function(
            pos) {
        return 100 - pos;
    },_isHorizontal:false});
}
if (!dojo._hasResource["dijit.form.Textarea"]) {
    dojo._hasResource["dijit.form.Textarea"] = true;
    dojo.provide("dijit.form.Textarea");
    dojo.declare("dijit.form.Textarea", dijit.form._FormValueWidget, {attributeMap:dojo.mixin(dojo.clone(dijit.form._FormValueWidget.prototype.attributeMap), {style:"styleNode","class":"styleNode"}),templateString:
            (dojo.isIE || dojo.isSafari || dojo.isFF) ?
            ((dojo.isIE || dojo.isSafari || dojo.isFF >= 3) ?
             "<fieldset id=\"${id}\" class=\"dijitInline dijitInputField dijitTextArea\" dojoAttachPoint=\"styleNode\" waiRole=\"presentation\"><div dojoAttachPoint=\"editNode,focusNode,eventNode\" dojoAttachEvent=\"onpaste:_changing,oncut:_changing\" waiRole=\"textarea\" style=\"text-decoration:none;display:block;overflow:auto;\" contentEditable=\"true\"></div>" :
             "<span id=\"${id}\" class=\"dijitReset\">" +
             "<iframe src=\"javascript:<html><head><title>${_iframeEditTitle}</title></head><body><script>var _postCreate=window.frameElement?window.frameElement.postCreate:null;if(_postCreate)_postCreate();</script></body></html>\"" +
             " dojoAttachPoint=\"iframe,styleNode\" dojoAttachEvent=\"onblur:_onIframeBlur\" class=\"dijitInline dijitInputField dijitTextArea\"></iframe>") +
            "<textarea name=\"${name}\" value=\"${value}\" dojoAttachPoint=\"formValueNode\" style=\"display:none;\"></textarea>" +
            ((dojo.isIE || dojo.isSafari || dojo.isFF >= 3) ? "</fieldset>" : "</span>") :
            "<textarea id=\"${id}\" name=\"${name}\" value=\"${value}\" dojoAttachPoint=\"formValueNode,editNode,focusNode,styleNode\" class=\"dijitInputField dijitTextArea\">" +
            dojo.isFF + "</textarea>",setAttribute:function(attr, _640) {
        this.inherited(arguments);
        switch (attr) {case "disabled":this.formValueNode.disabled =
                                       this.disabled;case "readOnly":if (dojo.isIE ||
                                                                         dojo.isSafari ||
                                                                         dojo.isFF >= 3) {
            this.editNode.contentEditable = (!this.disabled && !this.readOnly);
        } else {
            if (dojo.isFF) {
                this.iframe.contentDocument.designMode =
                (this.disabled || this.readOnly) ? "off" : "on";
            }
        }}
    },focus:function() {
        if (!this.disabled && !this.readOnly) {
            this._changing();
        }
        dijit.focus(this.iframe || this.focusNode);
    },setValue:function(_641, _642) {
        var _643 = this.editNode;
        if (typeof _641 == "string") {
            _643.innerHTML = "";
            if (_641.split) {
                var _644 = this;
                var _645 = true;
                dojo.forEach(_641.split("\n"), function(line) {
                    if (_645) {
                        _645 = false;
                    } else {
                        _643.appendChild(dojo.doc.createElement("BR"));
                    }
                    if (line) {
                        _643.appendChild(dojo.doc.createTextNode(line));
                    }
                });
            } else {
                if (_641) {
                    _643.appendChild(dojo.doc.createTextNode(_641));
                }
            }
            if (!dojo.isIE) {
                _643.appendChild(dojo.doc.createElement("BR"));
            }
        } else {
            _641 = _643.innerHTML;
            if (this.iframe) {
                _641 = _641.replace(/<div><\/div>\r?\n?$/i, "");
            }
            _641 =
            _641.replace(/\s*\r?\n|^\s+|\s+$|&nbsp;/g, "").replace(/>\s+</g, "><").replace(/<\/(p|div)>$|^<(p|div)[^>]*>/gi, "").replace(/([^>])<div>/g, "$1\n").replace(/<\/p>\s*<p[^>]*>|<br[^>]*>|<\/div>\s*<div[^>]*>/gi, "\n").replace(/<[^>]*>/g, "").replace(/&amp;/gi, "&").replace(/&lt;/gi, "<").replace(/&gt;/gi, ">");
            if (!dojo.isIE) {
                _641 = _641.replace(/\n$/, "");
            }
        }
        this.value = this.formValueNode.value = _641;
        if (this.iframe) {
            var _647 = dojo.doc.createElement("div");
            _643.appendChild(_647);
            var _648 = _647.offsetTop;
            if (_643.scrollWidth > _643.clientWidth) {
                _648 += 16;
            }
            if (this.lastHeight != _648) {
                if (_648 == 0) {
                    _648 = 16;
                }
                dojo.contentBox(this.iframe, {h:_648});
                this.lastHeight = _648;
            }
            _643.removeChild(_647);
        }
        dijit.form.Textarea.superclass.setValue.call(this, this.getValue(), _642);
    },getValue:function() {
        return this.value.replace(/\r/g, "");
    },postMixInProperties:function() {
        this.inherited(arguments);
        if (this.srcNodeRef && this.srcNodeRef.innerHTML != "") {
            this.value = this.srcNodeRef.innerHTML;
            this.srcNodeRef.innerHTML = "";
        }
        if ((!this.value || this.value == "") && this.srcNodeRef && this.srcNodeRef.value) {
            this.value = this.srcNodeRef.value;
        }
        if (!this.value) {
            this.value = "";
        }
        this.value =
        this.value.replace(/\r\n/g, "\n").replace(/&gt;/g, ">").replace(/&lt;/g, "<").replace(/&amp;/g, "&");
        if (dojo.isFF == 2) {
            var _649 = dojo.i18n.getLocalization("dijit.form", "Textarea");
            this._iframeEditTitle = _649.iframeEditTitle;
            this._iframeFocusTitle = _649.iframeFocusTitle;
            var _64a = dojo.query("label[for=\"" + this.id + "\"]");
            if (_64a.length) {
                this._iframeEditTitle = _64a[0].innerHTML + " " + this._iframeEditTitle;
            }
            var body = this.focusNode = this.editNode = dojo.doc.createElement("BODY");
            body.style.margin = "0px";
            body.style.padding = "0px";
            body.style.border = "0px";
        }
    },postCreate:function() {
        if (dojo.isIE || dojo.isSafari || dojo.isFF >= 3) {
            this.domNode.style.overflowY = "hidden";
        } else {
            if (dojo.isFF) {
                var w = this.iframe.contentWindow;
                var _64d = "";
                try {
                    _64d = this.iframe.contentDocument.title;
                } catch(e) {
                }
                if (!w || !_64d) {
                    this.iframe.postCreate = dojo.hitch(this, this.postCreate);
                    return;
                }
                var d = w.document;
                d.getElementsByTagName("HTML")[0].replaceChild(this.editNode, d.getElementsByTagName("BODY")[0]);
                if (!this.isLeftToRight()) {
                    d.getElementsByTagName("HTML")[0].dir = "rtl";
                }
                this.iframe.style.overflowY = "hidden";
                this.eventNode = d;
                w.addEventListener("resize", dojo.hitch(this, this._changed), false);
            } else {
                this.focusNode = this.domNode;
            }
        }
        if (this.eventNode) {
            this.connect(this.eventNode, "keypress", this._onKeyPress);
            this.connect(this.eventNode, "mousemove", this._changed);
            this.connect(this.eventNode, "focus", this._focused);
            this.connect(this.eventNode, "blur", this._blurred);
        }
        if (this.editNode) {
            this.connect(this.editNode, "change", this._changed);
        }
        this.inherited("postCreate", arguments);
    },_focused:function(e) {
        dojo.addClass(this.iframe || this.domNode, "dijitInputFieldFocused");
        this._changed(e);
    },_blurred:function(e) {
        dojo.removeClass(this.iframe || this.domNode, "dijitInputFieldFocused");
        this._changed(e, true);
    },_onIframeBlur:function() {
        this.iframe.contentDocument.title = this._iframeEditTitle;
    },_onKeyPress:function(e) {
        if (e.keyCode == dojo.keys.TAB && !e.shiftKey && !e.ctrlKey && !e.altKey && this.iframe) {
            this.iframe.contentDocument.title = this._iframeFocusTitle;
            this.iframe.focus();
            dojo.stopEvent(e);
        } else {
            if (e.keyCode == dojo.keys.ENTER) {
                e.stopPropagation();
            } else {
                if (this.inherited("_onKeyPress", arguments) && this.iframe) {
                    var te = dojo.doc.createEvent("KeyEvents");
                    te.initKeyEvent("keypress", true, true, null, e.ctrlKey, e.altKey, e.shiftKey, e.metaKey, e.keyCode, e.charCode);
                    this.iframe.dispatchEvent(te);
                }
            }
        }
        this._changing();
    },_changing:function(e) {
        setTimeout(dojo.hitch(this, "_changed", e, false), 1);
    },_changed:function(e, _655) {
        if (this.iframe && this.iframe.contentDocument.designMode != "on" && !this.disabled &&
            !this.readOnly) {
            this.iframe.contentDocument.designMode = "on";
        }
        this.setValue(null, _655 || false);
    }});
}
if (!dojo._hasResource["dijit.layout.StackContainer"]) {
    dojo._hasResource["dijit.layout.StackContainer"] = true;
    dojo.provide("dijit.layout.StackContainer");
    dojo.declare("dijit.layout.StackContainer", dijit.layout._LayoutWidget, {doLayout:true,_started:false,postCreate:function() {
        dijit.setWaiRole((this.containerNode || this.domNode), "tabpanel");
        this.connect(this.domNode, "onkeypress", this._onKeyPress);
    },startup:function() {
        if (this._started) {
            return;
        }
        var _656 = this.getChildren();
        dojo.forEach(_656, this._setupChild, this);
        dojo.some(_656, function(_657) {
            if (_657.selected) {
                this.selectedChildWidget = _657;
            }
            return _657.selected;
        }, this);
        var _658 = this.selectedChildWidget;
        if (!_658 && _656[0]) {
            _658 = this.selectedChildWidget = _656[0];
            _658.selected = true;
        }
        if (_658) {
            this._showChild(_658);
        }
        dojo.publish(this.id + "-startup", [{children:_656,selected:_658}]);
        this.inherited(arguments);
    },_setupChild:function(page) {
        page.domNode.style.display = "none";
        page.domNode.style.position = "relative";
        return page;
    },addChild:function(_65a, _65b) {
        dijit._Container.prototype.addChild.apply(this, arguments);
        _65a = this._setupChild(_65a);
        if (this._started) {
            this.layout();
            dojo.publish(this.id + "-addChild", [_65a,_65b]);
            if (!this.selectedChildWidget) {
                this.selectChild(_65a);
            }
        }
    },removeChild:function(page) {
        dijit._Container.prototype.removeChild.apply(this, arguments);
        if (this._beingDestroyed) {
            return;
        }
        if (this._started) {
            dojo.publish(this.id + "-removeChild", [page]);
            this.layout();
        }
        if (this.selectedChildWidget === page) {
            this.selectedChildWidget = undefined;
            if (this._started) {
                var _65d = this.getChildren();
                if (_65d.length) {
                    this.selectChild(_65d[0]);
                }
            }
        }
    },selectChild:function(page) {
        page = dijit.byId(page);
        if (this.selectedChildWidget != page) {
            this._transition(page, this.selectedChildWidget);
            this.selectedChildWidget = page;
            dojo.publish(this.id + "-selectChild", [page]);
        }
    },_transition:function(_65f, _660) {
        if (_660) {
            this._hideChild(_660);
        }
        this._showChild(_65f);
        if (this.doLayout && _65f.resize) {
            _65f.resize(this._containerContentBox || this._contentBox);
        }
    },_adjacent:function(_661) {
        var _662 = this.getChildren();
        var _663 = dojo.indexOf(_662, this.selectedChildWidget);
        _663 += _661 ? 1 : _662.length - 1;
        return _662[_663 % _662.length];
    },forward:function() {
        this.selectChild(this._adjacent(true));
    },back:function() {
        this.selectChild(this._adjacent(false));
    },_onKeyPress:function(e) {
        dojo.publish(this.id + "-containerKeyPress", [{e:e,page:this}]);
    },layout:function() {
        if (this.doLayout && this.selectedChildWidget && this.selectedChildWidget.resize) {
            this.selectedChildWidget.resize(this._contentBox);
        }
    },_showChild:function(page) {
        var _666 = this.getChildren();
        page.isFirstChild = (page == _666[0]);
        page.isLastChild = (page == _666[_666.length - 1]);
        page.selected = true;
        page.domNode.style.display = "";
        if (page._loadCheck) {
            page._loadCheck();
        }
        if (page.onShow) {
            page.onShow();
        }
    },_hideChild:function(page) {
        page.selected = false;
        page.domNode.style.display = "none";
        if (page.onHide) {
            page.onHide();
        }
    },closeChild:function(page) {
        var _669 = page.onClose(this, page);
        if (_669) {
            this.removeChild(page);
            page.destroyRecursive();
        }
    },destroy:function() {
        this._beingDestroyed = true;
        this.inherited(arguments);
    }});
    dojo.declare("dijit.layout.StackController", [dijit._Widget,dijit._Templated,dijit._Container], {templateString:"<span wairole='tablist' dojoAttachEvent='onkeypress' class='dijitStackController'></span>",containerId:"",buttonWidget:"dijit.layout._StackButton",postCreate:function() {
        dijit.setWaiRole(this.domNode, "tablist");
        this.pane2button = {};
        this.pane2menu = {};
        this._subscriptions = [dojo.subscribe(this.containerId +
                                              "-startup", this, "onStartup"),dojo.subscribe(this.containerId +
                                                                                            "-addChild", this, "onAddChild"),dojo.subscribe(this.containerId +
                                                                                                                                            "-removeChild", this, "onRemoveChild"),dojo.subscribe(this.containerId +
                                                                                                                                                                                                  "-selectChild", this, "onSelectChild"),dojo.subscribe(this.containerId +
                                                                                                                                                                                                                                                        "-containerKeyPress", this, "onContainerKeyPress")];
    },onStartup:function(info) {
        dojo.forEach(info.children, this.onAddChild, this);
        this.onSelectChild(info.selected);
    },destroy:function() {
        for (var pane in this.pane2button) {
            this.onRemoveChild(pane);
        }
        dojo.forEach(this._subscriptions, dojo.unsubscribe);
        this.inherited(arguments);
    },onAddChild:function(page, _66d) {
        var _66e = dojo.doc.createElement("span");
        this.domNode.appendChild(_66e);
        var cls = dojo.getObject(this.buttonWidget);
        var _670 = new cls({label:page.title,closeButton:page.closable}, _66e);
        this.addChild(_670, _66d);
        this.pane2button[page] = _670;
        page.controlButton = _670;
        dojo.connect(_670, "onClick", dojo.hitch(this, "onButtonClick", page));
        if (page.closable) {
            dojo.connect(_670, "onClickCloseButton", dojo.hitch(this, "onCloseButtonClick", page));
            var _671 = dojo.i18n.getLocalization("dijit", "common");
            var _672 = new dijit.Menu({targetNodeIds:[_670.id],id:_670.id + "_Menu"});
            var _673 = new dijit.MenuItem({label:_671.itemClose});
            dojo.connect(_673, "onClick", dojo.hitch(this, "onCloseButtonClick", page));
            _672.addChild(_673);
            this.pane2menu[page] = _672;
        }
        if (!this._currentChild) {
            _670.focusNode.setAttribute("tabIndex", "0");
            this._currentChild = page;
        }
        if (!this.isLeftToRight() && dojo.isIE && this._rectifyRtlTabList) {
            this._rectifyRtlTabList();
        }
    },onRemoveChild:function(page) {
        if (this._currentChild === page) {
            this._currentChild = null;
        }
        var _675 = this.pane2button[page];
        var menu = this.pane2menu[page];
        if (menu) {
            menu.destroy();
        }
        if (_675) {
            _675.destroy();
        }
        this.pane2button[page] = null;
    },onSelectChild:function(page) {
        if (!page) {
            return;
        }
        if (this._currentChild) {
            var _678 = this.pane2button[this._currentChild];
            _678.setAttribute("checked", false);
            _678.focusNode.setAttribute("tabIndex", "-1");
        }
        var _679 = this.pane2button[page];
        _679.setAttribute("checked", true);
        this._currentChild = page;
        _679.focusNode.setAttribute("tabIndex", "0");
        var _67a = dijit.byId(this.containerId);
        dijit.setWaiState(_67a.containerNode || _67a.domNode, "labelledby", _679.id);
    },onButtonClick:function(page) {
        var _67c = dijit.byId(this.containerId);
        _67c.selectChild(page);
    },onCloseButtonClick:function(page) {
        var _67e = dijit.byId(this.containerId);
        _67e.closeChild(page);
        var b = this.pane2button[this._currentChild];
        if (b) {
            dijit.focus(b.focusNode || b.domNode);
        }
    },adjacent:function(_680) {
        if (!this.isLeftToRight() && (!this.tabPosition || /top|bottom/.test(this.tabPosition))) {
            _680 = !_680;
        }
        var _681 = this.getChildren();
        var _682 = dojo.indexOf(_681, this.pane2button[this._currentChild]);
        var _683 = _680 ? 1 : _681.length - 1;
        return _681[(_682 + _683) % _681.length];
    },onkeypress:function(e) {
        if (this.disabled || e.altKey) {
            return;
        }
        var _685 = null;
        if (e.ctrlKey || !e._djpage) {
            var k = dojo.keys;
            switch (e.keyCode) {case k.LEFT_ARROW:case k.UP_ARROW:if (!e._djpage) {
                _685 = false;
            }break;case k.PAGE_UP:if (e.ctrlKey) {
                _685 = false;
            }break;case k.RIGHT_ARROW:case k.DOWN_ARROW:if (!e._djpage) {
                _685 = true;
            }break;case k.PAGE_DOWN:if (e.ctrlKey) {
                _685 = true;
            }break;case k.DELETE:if (this._currentChild.closable) {
                this.onCloseButtonClick(this._currentChild);
            }dojo.stopEvent(e);break;default:if (e.ctrlKey) {
                if (e.keyCode == k.TAB) {
                    this.adjacent(!e.shiftKey).onClick();
                    dojo.stopEvent(e);
                } else {
                    if (e.keyChar == "w") {
                        if (this._currentChild.closable) {
                            this.onCloseButtonClick(this._currentChild);
                        }
                        dojo.stopEvent(e);
                    }
                }
            }}
            if (_685 !== null) {
                this.adjacent(_685).onClick();
                dojo.stopEvent(e);
            }
        }
    },onContainerKeyPress:function(info) {
        info.e._djpage = info.page;
        this.onkeypress(info.e);
    }});
    dojo.declare("dijit.layout._StackButton", dijit.form.ToggleButton, {tabIndex:"-1",postCreate:function(
            evt) {
        dijit.setWaiRole((this.focusNode || this.domNode), "tab");
        this.inherited(arguments);
    },onClick:function(evt) {
        dijit.focus(this.focusNode);
    },onClickCloseButton:function(evt) {
        evt.stopPropagation();
    }});
    dojo.extend(dijit._Widget, {title:"",selected:false,closable:false,onClose:function() {
        return true;
    }});
}
if (!dojo._hasResource["dijit.layout.AccordionContainer"]) {
    dojo._hasResource["dijit.layout.AccordionContainer"] = true;
    dojo.provide("dijit.layout.AccordionContainer");
    dojo.declare("dijit.layout.AccordionContainer", dijit.layout.StackContainer, {duration:250,_verticalSpace:0,postCreate:function() {
        this.domNode.style.overflow = "hidden";
        this.inherited("postCreate", arguments);
        dijit.setWaiRole(this.domNode, "tablist");
        dojo.addClass(this.domNode, "dijitAccordionContainer");
    },startup:function() {
        if (this._started) {
            return;
        }
        this.inherited("startup", arguments);
        if (this.selectedChildWidget) {
            var _68b = this.selectedChildWidget.containerNode.style;
            _68b.display = "";
            _68b.overflow = "auto";
            this.selectedChildWidget._setSelectedState(true);
        }
    },layout:function() {
        var _68c = 0;
        var _68d = this.selectedChildWidget;
        dojo.forEach(this.getChildren(), function(_68e) {
            _68c += _68e.getTitleHeight();
        });
        var _68f = this._contentBox;
        this._verticalSpace = (_68f.h - _68c);
        if (_68d) {
            _68d.containerNode.style.height = this._verticalSpace + "px";
        }
    },_setupChild:function(page) {
        return page;
    },_transition:function(_691, _692) {
        if (this._inTransition) {
            return;
        }
        this._inTransition = true;
        var _693 = [];
        var _694 = this._verticalSpace;
        if (_691) {
            _691.setSelected(true);
            var _695 = _691.containerNode;
            _695.style.display = "";
            _693.push(dojo.animateProperty({node:_695,duration:this.duration,properties:{height:{start:"1",end:_694}},onEnd:function() {
                _695.style.overflow = "auto";
            }}));
        }
        if (_692) {
            _692.setSelected(false);
            var _696 = _692.containerNode;
            _696.style.overflow = "hidden";
            _693.push(dojo.animateProperty({node:_696,duration:this.duration,properties:{height:{start:_694,end:"1"}},onEnd:function() {
                _696.style.display = "none";
            }}));
        }
        this._inTransition = false;
        dojo.fx.combine(_693).play();
    },_onKeyPress:function(e) {
        if (this.disabled || e.altKey || !(e._dijitWidget || e.ctrlKey)) {
            return;
        }
        var k = dojo.keys;
        var _699 = e._dijitWidget;
        switch (e.keyCode) {case k.LEFT_ARROW:case k.UP_ARROW:if (_699) {
            this._adjacent(false)._onTitleClick();
            dojo.stopEvent(e);
        }break;case k.PAGE_UP:if (e.ctrlKey) {
            this._adjacent(false)._onTitleClick();
            dojo.stopEvent(e);
        }break;case k.RIGHT_ARROW:case k.DOWN_ARROW:if (_699) {
            this._adjacent(true)._onTitleClick();
            dojo.stopEvent(e);
        }break;case k.PAGE_DOWN:if (e.ctrlKey) {
            this._adjacent(true)._onTitleClick();
            dojo.stopEvent(e);
        }break;default:if (e.ctrlKey && e.keyCode == k.TAB) {
            this._adjacent(e._dijitWidget, !e.shiftKey)._onTitleClick();
            dojo.stopEvent(e);
        }}
    }});
    dojo.declare("dijit.layout.AccordionPane", [dijit.layout.ContentPane,dijit._Templated,dijit._Contained], {templateString:"<div class='dijitAccordionPane'\n\t><div dojoAttachPoint='titleNode,focusNode' dojoAttachEvent='ondijitclick:_onTitleClick,onkeypress:_onTitleKeyPress,onfocus:_handleFocus,onblur:_handleFocus'\n\t\tclass='dijitAccordionTitle' wairole=\"tab\"\n\t\t><div class='dijitAccordionArrow' waiRole=\"presentation\"></div\n\t\t><div class='arrowTextUp' waiRole=\"presentation\">&#9650;</div\n\t\t><div class='arrowTextDown' waiRole=\"presentation\">&#9660;</div\n\t\t><div waiRole=\"presentation\" dojoAttachPoint='titleTextNode' class='dijitAccordionText'>${title}</div></div\n\t><div><div dojoAttachPoint='containerNode' style='overflow: hidden; height: 1px; display: none'\n\t\tclass='dijitAccordionBody' wairole=\"tabpanel\"\n\t></div></div>\n</div>\n",postCreate:function() {
        this.inherited("postCreate", arguments);
        dojo.setSelectable(this.titleNode, false);
        this.setSelected(this.selected);
    },getTitleHeight:function() {
        return dojo.marginBox(this.titleNode).h;
    },_onTitleClick:function() {
        var _69a = this.getParent();
        if (!_69a._inTransition) {
            _69a.selectChild(this);
            dijit.focus(this.focusNode);
        }
    },_onTitleKeyPress:function(evt) {
        evt._dijitWidget = this;
        return this.getParent()._onKeyPress(evt);
    },_setSelectedState:function(_69c) {
        this.selected = _69c;
        dojo[(_69c ? "addClass" : "removeClass")](this.titleNode, "dijitAccordionTitle-selected");
        this.focusNode.setAttribute("tabIndex", _69c ? "0" : "-1");
    },_handleFocus:function(e) {
        dojo[(e.type == "focus" ? "addClass" :
              "removeClass")](this.focusNode, "dijitAccordionFocused");
    },setSelected:function(_69e) {
        this._setSelectedState(_69e);
        if (_69e) {
            this.onSelected();
            this._loadCheck(true);
        }
    },onSelected:function() {
    }});
}
if (!dojo._hasResource["dijit.layout.BorderContainer"]) {
    dojo._hasResource["dijit.layout.BorderContainer"] = true;
    dojo.provide("dijit.layout.BorderContainer");
    dojo.declare("dijit.layout.BorderContainer", dijit.layout._LayoutWidget, {design:"headline",liveSplitters:true,persist:false,_splitterClass:"dijit.layout._Splitter",postCreate:function() {
        this.inherited(arguments);
        this._splitters = {};
        this._splitterThickness = {};
        dojo.addClass(this.domNode, "dijitBorderContainer");
    },startup:function() {
        if (this._started) {
            return;
        }
        dojo.forEach(this.getChildren(), this._setupChild, this);
        this.inherited(arguments);
    },_setupChild:function(_69f) {
        var _6a0 = _69f.region;
        if (_6a0) {
            _69f.domNode.style.position = "absolute";
            var ltr = this.isLeftToRight();
            if (_6a0 == "leading") {
                _6a0 = ltr ? "left" : "right";
            }
            if (_6a0 == "trailing") {
                _6a0 = ltr ? "right" : "left";
            }
            this["_" + _6a0] = _69f.domNode;
            this["_" + _6a0 + "Widget"] = _69f;
            if (_69f.splitter) {
                var _6a2 = dojo.getObject(this._splitterClass);
                var flip = {left:"right",right:"left",top:"bottom",bottom:"top",leading:"trailing",trailing:"leading"};
                var _6a4 = dojo.query("[region=" + flip[_69f.region] + "]", this.domNode);
                var _6a5 = new _6a2({container:this,child:_69f,region:_6a0,oppNode:_6a4[0],live:this.liveSplitters});
                this._splitters[_6a0] = _6a5.domNode;
                dojo.place(_6a5.domNode, _69f.domNode, "after");
                this._computeSplitterThickness(_6a0);
            }
            _69f.region = _6a0;
        }
    },_computeSplitterThickness:function(_6a6) {
        var re = new RegExp("top|bottom");
        this._splitterThickness[_6a6] =
        dojo.marginBox(this._splitters[_6a6])[(re.test(_6a6) ? "h" : "w")];
    },layout:function() {
        this._layoutChildren();
    },addChild:function(_6a8, _6a9) {
        this.inherited(arguments);
        this._setupChild(_6a8);
        if (this._started) {
            this._layoutChildren();
        }
    },removeChild:function(_6aa) {
        var _6ab = _6aa.region;
        var _6ac = this._splitters[_6ab];
        if (_6ac) {
            dijit.byNode(_6ac).destroy();
            delete this._splitters[_6ab];
            delete this._splitterThickness[_6ab];
        }
        this.inherited(arguments);
        delete this["_" + _6ab];
        delete this["_" + _6ab + "Widget"];
        if (this._started) {
            this._layoutChildren(_6aa.region);
        }
    },_layoutChildren:function(_6ad) {
        var _6ae = (this.design == "sidebar");
        var _6af = 0,_6b0 = 0,_6b1 = 0,_6b2 = 0;
        var _6b3 = {},_6b4 = {},_6b5 = {},_6b6 = {},_6b7 = (this._center && this._center.style) ||
                                                           {};
        var _6b8 = /left|right/.test(_6ad);
        var _6b9 = !_6ad || (!_6b8 && !_6ae);
        var _6ba = !_6ad || (_6b8 && _6ae);
        if (this._top) {
            _6b3 = _6ba && this._top.style;
            _6af = dojo.marginBox(this._top).h;
        }
        if (this._left) {
            _6b4 = _6b9 && this._left.style;
            _6b1 = dojo.marginBox(this._left).w;
        }
        if (this._right) {
            _6b5 = _6b9 && this._right.style;
            _6b2 = dojo.marginBox(this._right).w;
        }
        if (this._bottom) {
            _6b6 = _6ba && this._bottom.style;
            _6b0 = dojo.marginBox(this._bottom).h;
        }
        var _6bb = this._splitters;
        var _6bc = _6bb.top;
        var _6bd = _6bb.bottom;
        var _6be = _6bb.left;
        var _6bf = _6bb.right;
        var _6c0 = this._splitterThickness;
        var _6c1 = _6c0.top || 0;
        var _6c2 = _6c0.left || 0;
        var _6c3 = _6c0.right || 0;
        var _6c4 = _6c0.bottom || 0;
        if (_6c2 > 50 || _6c3 > 50) {
            setTimeout(dojo.hitch(this, function() {
                for (var _6c5 in this._splitters) {
                    this._computeSplitterThickness(_6c5);
                }
                this._layoutChildren();
            }), 50);
            return false;
        }
        var _6c6 = {left:(_6ae ? _6b1 + _6c2 : "0") + "px",right:(_6ae ? _6b2 + _6c3 : "0") + "px"};
        if (_6bc) {
            dojo.mixin(_6bc.style, _6c6);
            _6bc.style.top = _6af + "px";
        }
        if (_6bd) {
            dojo.mixin(_6bd.style, _6c6);
            _6bd.style.bottom = _6b0 + "px";
        }
        _6c6 = {top:(_6ae ? "0" : _6af + _6c1) + "px",bottom:(_6ae ? "0" : _6b0 + _6c4) + "px"};
        if (_6be) {
            dojo.mixin(_6be.style, _6c6);
            _6be.style.left = _6b1 + "px";
        }
        if (_6bf) {
            dojo.mixin(_6bf.style, _6c6);
            _6bf.style.right = _6b2 + "px";
        }
        dojo.mixin(_6b7, {top:_6af + _6c1 + "px",left:_6b1 + _6c2 + "px",right:_6b2 + _6c3 +
                                                                               "px",bottom:_6b0 +
                                                                                           _6c4 +
                                                                                           "px"});
        var _6c7 = {top:_6ae ? "0" : _6b7.top,bottom:_6ae ? "0" : _6b7.bottom};
        dojo.mixin(_6b4, _6c7);
        dojo.mixin(_6b5, _6c7);
        _6b4.left = _6b5.right = _6b3.top = _6b6.bottom = "0";
        if (_6ae) {
            _6b3.left = _6b6.left = _6b1 + (this.isLeftToRight() ? _6c2 : 0) + "px";
            _6b3.right = _6b6.right = _6b2 + (this.isLeftToRight() ? 0 : _6c3) + "px";
        } else {
            _6b3.left = _6b3.right = _6b6.left = _6b6.right = "0";
        }
        var _6c8 = dojo.isIE || dojo.some(this.getChildren(), function(_6c9) {
            return _6c9.domNode.tagName == "TEXTAREA";
        });
        if (_6c8) {
            var _6ca = function(n, b) {
                n = dojo.byId(n);
                var s = dojo.getComputedStyle(n);
                if (!b) {
                    return dojo._getBorderBox(n, s);
                }
                var me = dojo._getMarginExtents(n, s);
                dojo._setMarginBox(n, b.l, b.t, b.w + me.w, b.h + me.h, s);
                return null;
            };
            var _6cf = function(_6d0, dim) {
                if (_6d0) {
                    _6d0.resize ? _6d0.resize(dim) : dojo.marginBox(_6d0.domNode, dim);
                }
            };
            var _6d2 = _6ca(this.domNode);
            var _6d3 = _6d2.h;
            var _6d4 = _6d3;
            if (this._top) {
                _6d4 -= _6af;
            }
            if (this._bottom) {
                _6d4 -= _6b0;
            }
            if (_6bc) {
                _6d4 -= _6c1;
            }
            if (_6bd) {
                _6d4 -= _6c4;
            }
            var _6d5 = {h:_6d4};
            var _6d6 = _6ae ? _6d3 : _6d4;
            if (_6be) {
                _6be.style.height = _6d6;
            }
            if (_6bf) {
                _6bf.style.height = _6d6;
            }
            _6cf(this._leftWidget, {h:_6d6});
            _6cf(this._rightWidget, {h:_6d6});
            var _6d7 = _6d2.w;
            var _6d8 = _6d7;
            if (this._left) {
                _6d8 -= _6b1;
            }
            if (this._right) {
                _6d8 -= _6b2;
            }
            if (_6be) {
                _6d8 -= _6c2;
            }
            if (_6bf) {
                _6d8 -= _6c3;
            }
            _6d5.w = _6d8;
            var _6d9 = _6ae ? _6d8 : _6d7;
            if (_6bc) {
                _6bc.style.width = _6d9;
            }
            if (_6bd) {
                _6bd.style.width = _6d9;
            }
            _6cf(this._topWidget, {w:_6d9});
            _6cf(this._bottomWidget, {w:_6d9});
            _6cf(this._centerWidget, _6d5);
        } else {
            var _6da = {};
            if (_6ad) {
                _6da[_6ad] = _6da.center = true;
                if (/top|bottom/.test(_6ad) && this.design != "sidebar") {
                    _6da.left = _6da.right = true;
                } else {
                    if (/left|right/.test(_6ad) && this.design == "sidebar") {
                        _6da.top = _6da.bottom = true;
                    }
                }
            }
            dojo.forEach(this.getChildren(), function(_6db) {
                if (_6db.resize && (!_6ad || _6db.region in _6da)) {
                    _6db.resize();
                }
            }, this);
        }
    }});
    dojo.extend(dijit._Widget, {region:"",splitter:false,minSize:0,maxSize:Infinity});
    dojo.declare("dijit.layout._Splitter", [dijit._Widget,dijit._Templated], {live:true,templateString:"<div class=\"dijitSplitter\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_startDrag\" tabIndex=\"0\" waiRole=\"separator\"><div class=\"dijitSplitterThumb\"></div></div>",postCreate:function() {
        this.inherited(arguments);
        this.horizontal = /top|bottom/.test(this.region);
        dojo.addClass(this.domNode, "dijitSplitter" + (this.horizontal ? "H" : "V"));
        this._factor = /top|left/.test(this.region) ? 1 : -1;
        this._minSize = this.child.minSize;
        this._computeMaxSize();
        this.connect(this.container, "layout", dojo.hitch(this, this._computeMaxSize));
        this._cookieName = this.container.id + "_" + this.region;
        if (this.container.persist) {
            var _6dc = dojo.cookie(this._cookieName);
            if (_6dc) {
                this.child.domNode.style[this.horizontal ? "height" : "width"] = _6dc;
            }
        }
    },_computeMaxSize:function() {
        var dim = this.horizontal ? "h" : "w";
        var _6de = dojo.contentBox(this.container.domNode)[dim] -
                   (this.oppNode ? dojo.marginBox(this.oppNode)[dim] : 0);
        this._maxSize = Math.min(this.child.maxSize, _6de);
    },_startDrag:function(e) {
        if (!this.cover) {
            this.cover = dojo.doc.createElement("div");
            dojo.addClass(this.cover, "dijitSplitterCover");
            dojo.place(this.cover, this.child.domNode, "after");
        } else {
            this.cover.style.zIndex = 1;
        }
        if (this.fake) {
            dojo._destroyElement(this.fake);
        }
        if (!(this._resize = this.live)) {
            (this.fake = this.domNode.cloneNode(true)).removeAttribute("id");
            dojo.addClass(this.domNode, "dijitSplitterShadow");
            dojo.place(this.fake, this.domNode, "after");
        }
        dojo.addClass(this.domNode, "dijitSplitterActive");
        var _6e0 = this._factor,max = this._maxSize,min = this._minSize || 10;
        var axis = this.horizontal ? "pageY" : "pageX";
        var _6e4 = e[axis];
        var _6e5 = this.domNode.style;
        var dim = this.horizontal ? "h" : "w";
        var _6e7 = dojo.marginBox(this.child.domNode)[dim];
        var _6e8 = parseInt(this.domNode.style[this.region]);
        var _6e9 = this._resize;
        var _6ea = this.region;
        var mb = {};
        var _6ec = this.child.domNode;
        var _6ed = dojo.hitch(this.container, this.container._layoutChildren);
        var de = dojo.doc.body;
        this._handlers = (this._handlers || []).concat([dojo.connect(de, "onmousemove", this._drag =
                                                                                        function(e,
                                                                                                 _6f0) {
                                                                                            var _6f1 = e[axis] -
                                                                                                       _6e4,_6f2 = _6e0 *
                                                                                                                   _6f1 +
                                                                                                                   _6e7,_6f3 = Math.max(Math.min(_6f2, max), min);
                                                                                            if (_6e9 ||
                                                                                                _6f0) {
                                                                                                mb[dim] =
                                                                                                _6f3;
                                                                                                dojo.marginBox(_6ec, mb);
                                                                                                _6ed(_6ea);
                                                                                            }
                                                                                            _6e5[_6ea] =
                                                                                            _6e0 *
                                                                                            _6f1 +
                                                                                            _6e8 +
                                                                                            (_6f3 -
                                                                                             _6f2) +
                                                                                            "px";
                                                                                        }),dojo.connect(de, "onmouseup", this, "_stopDrag")]);
        dojo.stopEvent(e);
    },_stopDrag:function(e) {
        try {
            if (this.cover) {
                this.cover.style.zIndex = -1;
            }
            if (this.fake) {
                dojo._destroyElement(this.fake);
            }
            dojo.removeClass(this.domNode, "dijitSplitterActive");
            dojo.removeClass(this.domNode, "dijitSplitterShadow");
            this._drag(e);
            this._drag(e, true);
        } finally {
            this._cleanupHandlers();
            delete this._drag;
        }
        if (this.container.persist) {
            dojo.cookie(this._cookieName, this.child.domNode.style[this.horizontal ? "height" :
                                                                   "width"]);
        }
    },_cleanupHandlers:function() {
        dojo.forEach(this._handlers, dojo.disconnect);
        delete this._handlers;
    },_onKeyPress:function(e) {
        this._resize = true;
        var _6f6 = this.horizontal;
        var tick = 1;
        var dk = dojo.keys;
        switch (e.keyCode) {case _6f6 ? dk.UP_ARROW : dk.LEFT_ARROW:tick *= -1;break;case _6f6 ?
                                                                                          dk.DOWN_ARROW :
                                                                                          dk.RIGHT_ARROW:break;default:return;}
        var _6f9 = dojo.marginBox(this.child.domNode)[_6f6 ? "h" : "w"] + this._factor * tick;
        var mb = {};
        mb[this.horizontal ? "h" : "w"] = Math.max(Math.min(_6f9, this._maxSize), this._minSize);
        dojo.marginBox(this.child.domNode, mb);
        this.container._layoutChildren(this.region);
        dojo.stopEvent(e);
    },destroy:function() {
        this._cleanupHandlers();
        delete this.child;
        delete this.container;
        delete this.fake;
        this.inherited(arguments);
    }});
}
if (!dojo._hasResource["dijit.layout.LayoutContainer"]) {
    dojo._hasResource["dijit.layout.LayoutContainer"] = true;
    dojo.provide("dijit.layout.LayoutContainer");
    dojo.declare("dijit.layout.LayoutContainer", dijit.layout._LayoutWidget, {constructor:function() {
        dojo.deprecated("dijit.layout.LayoutContainer is deprecated", "use BorderContainer instead", 2);
    },layout:function() {
        dijit.layout.layoutChildren(this.domNode, this._contentBox, this.getChildren());
    },addChild:function(_6fb, _6fc) {
        dijit._Container.prototype.addChild.apply(this, arguments);
        if (this._started) {
            dijit.layout.layoutChildren(this.domNode, this._contentBox, this.getChildren());
        }
    },removeChild:function(_6fd) {
        dijit._Container.prototype.removeChild.apply(this, arguments);
        if (this._started) {
            dijit.layout.layoutChildren(this.domNode, this._contentBox, this.getChildren());
        }
    }});
    dojo.extend(dijit._Widget, {layoutAlign:"none"});
}
if (!dojo._hasResource["dijit.layout.LinkPane"]) {
    dojo._hasResource["dijit.layout.LinkPane"] = true;
    dojo.provide("dijit.layout.LinkPane");
    dojo.declare("dijit.layout.LinkPane", [dijit.layout.ContentPane,dijit._Templated], {templateString:"<div class=\"dijitLinkPane\"></div>",postCreate:function() {
        if (this.srcNodeRef) {
            this.title += this.srcNodeRef.innerHTML;
        }
        this.inherited("postCreate", arguments);
    }});
}
if (!dojo._hasResource["dijit.layout.SplitContainer"]) {
    dojo._hasResource["dijit.layout.SplitContainer"] = true;
    dojo.provide("dijit.layout.SplitContainer");
    dojo.declare("dijit.layout.SplitContainer", dijit.layout._LayoutWidget, {constructor:function() {
        dojo.deprecated("dijit.layout.SplitContainer is deprecated", "use BorderContainer with splitter instead", 2);
    },activeSizing:false,sizerWidth:7,orientation:"horizontal",persist:true,postMixInProperties:function() {
        this.inherited("postMixInProperties", arguments);
        this.isHorizontal = (this.orientation == "horizontal");
    },postCreate:function() {
        this.inherited("postCreate", arguments);
        this.sizers = [];
        dojo.addClass(this.domNode, "dijitSplitContainer");
        if (dojo.isMozilla) {
            this.domNode.style.overflow = "-moz-scrollbars-none";
        }
        if (typeof this.sizerWidth == "object") {
            try {
                this.sizerWidth = parseInt(this.sizerWidth.toString());
            } catch(e) {
                this.sizerWidth = 7;
            }
        }
        var _6fe = this.virtualSizer = dojo.doc.createElement("div");
        _6fe.style.position = "relative";
        _6fe.style.zIndex = 10;
        _6fe.className =
        this.isHorizontal ? "dijitSplitContainerVirtualSizerH" : "dijitSplitContainerVirtualSizerV";
        this.domNode.appendChild(_6fe);
        dojo.setSelectable(_6fe, false);
    },destroy:function() {
        delete this.virtualSizer;
        dojo.forEach(this._ownconnects, dojo.disconnect);
        this.inherited(arguments);
    },startup:function() {
        if (this._started) {
            return;
        }
        dojo.forEach(this.getChildren(), function(_6ff, i, _701) {
            this._injectChild(_6ff);
            if (i < _701.length - 1) {
                this._addSizer();
            }
        }, this);
        if (this.persist) {
            this._restoreState();
        }
        this.inherited(arguments);
    },_injectChild:function(_702) {
        _702.domNode.style.position = "absolute";
        dojo.addClass(_702.domNode, "dijitSplitPane");
    },_addSizer:function() {
        var i = this.sizers.length;
        var _704 = this.sizers[i] = dojo.doc.createElement("div");
        this.domNode.appendChild(_704);
        _704.className =
        this.isHorizontal ? "dijitSplitContainerSizerH" : "dijitSplitContainerSizerV";
        var _705 = dojo.doc.createElement("div");
        _705.className = "thumb";
        _704.appendChild(_705);
        var self = this;
        var _707 = (function() {
            var _708 = i;
            return function(e) {
                self.beginSizing(e, _708);
            };
        })();
        this.connect(_704, "onmousedown", _707);
        dojo.setSelectable(_704, false);
    },removeChild:function(_70a) {
        if (this.sizers.length) {
            var i = dojo.indexOf(this.getChildren(), _70a);
            if (i != -1) {
                if (i == this.sizers.length) {
                    i--;
                }
                dojo._destroyElement(this.sizers[i]);
                this.sizers.splice(i, 1);
            }
        }
        this.inherited(arguments);
        if (this._started) {
            this.layout();
        }
    },addChild:function(_70c, _70d) {
        this.inherited("addChild", arguments);
        if (this._started) {
            this._injectChild(_70c);
            var _70e = this.getChildren();
            if (_70e.length > 1) {
                this._addSizer();
            }
            this.layout();
        }
    },layout:function() {
        this.paneWidth = this._contentBox.w;
        this.paneHeight = this._contentBox.h;
        var _70f = this.getChildren();
        if (!_70f.length) {
            return;
        }
        var _710 = this.isHorizontal ? this.paneWidth : this.paneHeight;
        if (_70f.length > 1) {
            _710 -= this.sizerWidth * (_70f.length - 1);
        }
        var _711 = 0;
        dojo.forEach(_70f, function(_712) {
            _711 += _712.sizeShare;
        });
        var _713 = _710 / _711;
        var _714 = 0;
        dojo.forEach(_70f.slice(0, _70f.length - 1), function(_715) {
            var size = Math.round(_713 * _715.sizeShare);
            _715.sizeActual = size;
            _714 += size;
        });
        _70f[_70f.length - 1].sizeActual = _710 - _714;
        this._checkSizes();
        var pos = 0;
        var size = _70f[0].sizeActual;
        this._movePanel(_70f[0], pos, size);
        _70f[0].position = pos;
        pos += size;
        if (!this.sizers) {
            return;
        }
        dojo.some(_70f.slice(1), function(_719, i) {
            if (!this.sizers[i]) {
                return true;
            }
            this._moveSlider(this.sizers[i], pos, this.sizerWidth);
            this.sizers[i].position = pos;
            pos += this.sizerWidth;
            size = _719.sizeActual;
            this._movePanel(_719, pos, size);
            _719.position = pos;
            pos += size;
        }, this);
    },_movePanel:function(_71b, pos, size) {
        if (this.isHorizontal) {
            _71b.domNode.style.left = pos + "px";
            _71b.domNode.style.top = 0;
            var box = {w:size,h:this.paneHeight};
            if (_71b.resize) {
                _71b.resize(box);
            } else {
                dojo.marginBox(_71b.domNode, box);
            }
        } else {
            _71b.domNode.style.left = 0;
            _71b.domNode.style.top = pos + "px";
            var box = {w:this.paneWidth,h:size};
            if (_71b.resize) {
                _71b.resize(box);
            } else {
                dojo.marginBox(_71b.domNode, box);
            }
        }
    },_moveSlider:function(_71f, pos, size) {
        if (this.isHorizontal) {
            _71f.style.left = pos + "px";
            _71f.style.top = 0;
            dojo.marginBox(_71f, {w:size,h:this.paneHeight});
        } else {
            _71f.style.left = 0;
            _71f.style.top = pos + "px";
            dojo.marginBox(_71f, {w:this.paneWidth,h:size});
        }
    },_growPane:function(_722, pane) {
        if (_722 > 0) {
            if (pane.sizeActual > pane.sizeMin) {
                if ((pane.sizeActual - pane.sizeMin) > _722) {
                    pane.sizeActual = pane.sizeActual - _722;
                    _722 = 0;
                } else {
                    _722 -= pane.sizeActual - pane.sizeMin;
                    pane.sizeActual = pane.sizeMin;
                }
            }
        }
        return _722;
    },_checkSizes:function() {
        var _724 = 0;
        var _725 = 0;
        var _726 = this.getChildren();
        dojo.forEach(_726, function(_727) {
            _725 += _727.sizeActual;
            _724 += _727.sizeMin;
        });
        if (_724 <= _725) {
            var _728 = 0;
            dojo.forEach(_726, function(_729) {
                if (_729.sizeActual < _729.sizeMin) {
                    _728 += _729.sizeMin - _729.sizeActual;
                    _729.sizeActual = _729.sizeMin;
                }
            });
            if (_728 > 0) {
                var list = this.isDraggingLeft ? _726.reverse() : _726;
                dojo.forEach(list, function(_72b) {
                    _728 = this._growPane(_728, _72b);
                }, this);
            }
        } else {
            dojo.forEach(_726, function(_72c) {
                _72c.sizeActual = Math.round(_725 * (_72c.sizeMin / _724));
            });
        }
    },beginSizing:function(e, i) {
        var _72f = this.getChildren();
        this.paneBefore = _72f[i];
        this.paneAfter = _72f[i + 1];
        this.isSizing = true;
        this.sizingSplitter = this.sizers[i];
        if (!this.cover) {
            this.cover = dojo.doc.createElement("div");
            this.domNode.appendChild(this.cover);
            var s = this.cover.style;
            s.position = "absolute";
            s.zIndex = 1;
            s.top = 0;
            s.left = 0;
            s.width = "100%";
            s.height = "100%";
        } else {
            this.cover.style.zIndex = 1;
        }
        this.sizingSplitter.style.zIndex = 2;
        this.originPos = dojo.coords(_72f[0].domNode, true);
        if (this.isHorizontal) {
            var _731 = (e.layerX ? e.layerX : e.offsetX);
            var _732 = e.pageX;
            this.originPos = this.originPos.x;
        } else {
            var _731 = (e.layerY ? e.layerY : e.offsetY);
            var _732 = e.pageY;
            this.originPos = this.originPos.y;
        }
        this.startPoint = this.lastPoint = _732;
        this.screenToClientOffset = _732 - _731;
        this.dragOffset =
        this.lastPoint - this.paneBefore.sizeActual - this.originPos - this.paneBefore.position;
        if (!this.activeSizing) {
            this._showSizingLine();
        }
        this._ownconnects = [];
        this._ownconnects.push(dojo.connect(dojo.doc.documentElement, "onmousemove", this, "changeSizing"));
        this._ownconnects.push(dojo.connect(dojo.doc.documentElement, "onmouseup", this, "endSizing"));
        dojo.stopEvent(e);
    },changeSizing:function(e) {
        if (!this.isSizing) {
            return;
        }
        this.lastPoint = this.isHorizontal ? e.pageX : e.pageY;
        this.movePoint();
        if (this.activeSizing) {
            this._updateSize();
        } else {
            this._moveSizingLine();
        }
        dojo.stopEvent(e);
    },endSizing:function(e) {
        if (!this.isSizing) {
            return;
        }
        if (this.cover) {
            this.cover.style.zIndex = -1;
        }
        if (!this.activeSizing) {
            this._hideSizingLine();
        }
        this._updateSize();
        this.isSizing = false;
        if (this.persist) {
            this._saveState(this);
        }
        dojo.forEach(this._ownconnects, dojo.disconnect);
    },movePoint:function() {
        var p = this.lastPoint - this.screenToClientOffset;
        var a = p - this.dragOffset;
        a = this.legaliseSplitPoint(a);
        p = a + this.dragOffset;
        this.lastPoint = p + this.screenToClientOffset;
    },legaliseSplitPoint:function(a) {
        a += this.sizingSplitter.position;
        this.isDraggingLeft = !!(a > 0);
        if (!this.activeSizing) {
            var min = this.paneBefore.position + this.paneBefore.sizeMin;
            if (a < min) {
                a = min;
            }
            var max = this.paneAfter.position +
                      (this.paneAfter.sizeActual - (this.sizerWidth + this.paneAfter.sizeMin));
            if (a > max) {
                a = max;
            }
        }
        a -= this.sizingSplitter.position;
        this._checkSizes();
        return a;
    },_updateSize:function() {
        var pos = this.lastPoint - this.dragOffset - this.originPos;
        var _73b = this.paneBefore.position;
        var _73c = this.paneAfter.position + this.paneAfter.sizeActual;
        this.paneBefore.sizeActual = pos - _73b;
        this.paneAfter.position = pos + this.sizerWidth;
        this.paneAfter.sizeActual = _73c - this.paneAfter.position;
        dojo.forEach(this.getChildren(), function(_73d) {
            _73d.sizeShare = _73d.sizeActual;
        });
        if (this._started) {
            this.layout();
        }
    },_showSizingLine:function() {
        this._moveSizingLine();
        dojo.marginBox(this.virtualSizer, this.isHorizontal ?
                                          {w:this.sizerWidth,h:this.paneHeight} :
                                          {w:this.paneWidth,h:this.sizerWidth});
        this.virtualSizer.style.display = "block";
    },_hideSizingLine:function() {
        this.virtualSizer.style.display = "none";
    },_moveSizingLine:function() {
        var pos = (this.lastPoint - this.startPoint) + this.sizingSplitter.position;
        dojo.style(this.virtualSizer, (this.isHorizontal ? "left" : "top"), pos + "px");
    },_getCookieName:function(i) {
        return this.id + "_" + i;
    },_restoreState:function() {
        dojo.forEach(this.getChildren(), function(_740, i) {
            var _742 = this._getCookieName(i);
            var _743 = dojo.cookie(_742);
            if (_743) {
                var pos = parseInt(_743);
                if (typeof pos == "number") {
                    _740.sizeShare = pos;
                }
            }
        }, this);
    },_saveState:function() {
        dojo.forEach(this.getChildren(), function(_745, i) {
            dojo.cookie(this._getCookieName(i), _745.sizeShare);
        }, this);
    }});
    dojo.extend(dijit._Widget, {sizeMin:10,sizeShare:10});
}
if (!dojo._hasResource["dijit.layout.TabContainer"]) {
    dojo._hasResource["dijit.layout.TabContainer"] = true;
    dojo.provide("dijit.layout.TabContainer");
    dojo.declare("dijit.layout.TabContainer", [dijit.layout.StackContainer,dijit._Templated], {tabPosition:"top",templateString:null,templateString:"<div class=\"dijitTabContainer\">\n\t<div dojoAttachPoint=\"tablistNode\"></div>\n\t<div class=\"dijitTabPaneWrapper\" dojoAttachPoint=\"containerNode\"></div>\n</div>\n",_controllerWidget:"dijit.layout.TabController",postCreate:function() {
        this.inherited(arguments);
        var _747 = dojo.getObject(this._controllerWidget);
        this.tablist = new _747({id:this.id +
                                    "_tablist",tabPosition:this.tabPosition,doLayout:this.doLayout,containerId:this.id}, this.tablistNode);
    },_setupChild:function(tab) {
        dojo.addClass(tab.domNode, "dijitTabPane");
        this.inherited(arguments);
        return tab;
    },startup:function() {
        if (this._started) {
            return;
        }
        this.tablist.startup();
        this.inherited(arguments);
        if (dojo.isSafari) {
            setTimeout(dojo.hitch(this, "layout"), 0);
        }
        if (dojo.isIE && !this.isLeftToRight() && this.tabPosition == "right-h" && this.tablist &&
            this.tablist.pane2button) {
            for (var pane in this.tablist.pane2button) {
                var _74a = this.tablist.pane2button[pane];
                if (!_74a.closeButton) {
                    continue;
                }
                tabButtonStyle = _74a.closeButtonNode.style;
                tabButtonStyle.position = "absolute";
                if (dojo.isIE < 7) {
                    tabButtonStyle.left = _74a.domNode.offsetWidth + "px";
                } else {
                    tabButtonStyle.padding = "0px";
                }
            }
        }
    },layout:function() {
        if (!this.doLayout) {
            return;
        }
        var _74b = this.tabPosition.replace(/-h/, "");
        var _74c = [{domNode:this.tablist.domNode,layoutAlign:_74b},{domNode:this.containerNode,layoutAlign:"client"}];
        dijit.layout.layoutChildren(this.domNode, this._contentBox, _74c);
        this._containerContentBox = dijit.layout.marginBox2contentBox(this.containerNode, _74c[1]);
        if (this.selectedChildWidget) {
            this._showChild(this.selectedChildWidget);
            if (this.doLayout && this.selectedChildWidget.resize) {
                this.selectedChildWidget.resize(this._containerContentBox);
            }
        }
    },destroy:function() {
        if (this.tablist) {
            this.tablist.destroy();
        }
        this.inherited(arguments);
    }});
    dojo.declare("dijit.layout.TabController", dijit.layout.StackController, {templateString:"<div wairole='tablist' dojoAttachEvent='onkeypress:onkeypress'></div>",tabPosition:"top",doLayout:true,buttonWidget:"dijit.layout._TabButton",postMixInProperties:function() {
        this["class"] =
        "dijitTabLabels-" + this.tabPosition + (this.doLayout ? "" : " dijitTabNoLayout");
        this.inherited(arguments);
    },_rectifyRtlTabList:function() {
        if (0 >= this.tabPosition.indexOf("-h")) {
            return;
        }
        if (!this.pane2button) {
            return;
        }
        var _74d = 0;
        for (var pane in this.pane2button) {
            _74d = Math.max(_74d, dojo.marginBox(this.pane2button[pane].innerDiv).w);
        }
        for (pane in this.pane2button) {
            this.pane2button[pane].innerDiv.style.width = _74d + "px";
        }
    }});
    dojo.declare("dijit.layout._TabButton", dijit.layout._StackButton, {baseClass:"dijitTab",templateString:"<div waiRole=\"presentation\" dojoAttachEvent='onclick:onClick,onmouseenter:_onMouse,onmouseleave:_onMouse'>\n    <div waiRole=\"presentation\" class='dijitTabInnerDiv' dojoAttachPoint='innerDiv'>\n        <div waiRole=\"presentation\" class='dijitTabContent' dojoAttachPoint='tabContent'>\n\t        <span dojoAttachPoint='containerNode,focusNode' class='tabLabel'>${!label}</span>\n\t        <span dojoAttachPoint='closeButtonNode' class='closeImage' dojoAttachEvent='onmouseenter:_onMouse, onmouseleave:_onMouse, onclick:onClickCloseButton' stateModifier='CloseButton'>\n\t            <span dojoAttachPoint='closeText' class='closeText'>x</span>\n\t        </span>\n        </div>\n    </div>\n</div>\n",postCreate:function() {
        if (this.closeButton) {
            dojo.addClass(this.innerDiv, "dijitClosable");
        } else {
            this.closeButtonNode.style.display = "none";
        }
        this.inherited(arguments);
        dojo.setSelectable(this.containerNode, false);
    }});
}
if (!dojo._hasResource["dijit.dijit-all"]) {
    dojo._hasResource["dijit.dijit-all"] = true;
    console.warn("dijit-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");
    dojo.provide("dijit.dijit-all");
}
dojo.i18n._preloadLocalizations("dijit.nls.dijit-all", ["he","nl","tr","no","ko","el","en","en-gb","ROOT","zh-cn","hu","es","fi-fi","pt-br","fi","he-il","xx","ru","it","fr","cs","de-de","fr-fr","it-it","es-es","ja","da","pl","de","sv","pt","zh-tw","pt-pt","nl-nl","ko-kr","ar","en-us","zh","ja-jp"]);
