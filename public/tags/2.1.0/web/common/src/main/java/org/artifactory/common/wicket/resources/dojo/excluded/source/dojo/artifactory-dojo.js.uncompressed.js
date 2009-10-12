/*
 Copyright (c) 2004-2009, The Dojo Foundation All Rights Reserved.
 Available via Academic Free License >= 2.1 OR the modified BSD license.
 see: http://dojotoolkit.org/license for details
 */

/*
 This is a compiled version of Dojo, built for deployment and not for
 development. To get an editable version, please visit:

 http://dojotoolkit.org

 for documentation and information on getting the source.
 */

if (!dojo._hasResource["dojo.date.stamp"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.date.stamp"] = true;
    dojo.provide("dojo.date.stamp");

    // Methods to convert dates to or from a wire (string) format using well-known conventions

    dojo.date.stamp.fromISOString = function(/*String*/formattedString, /*Number?*/defaultTime) {
        //	summary:
        //		Returns a Date object given a string formatted according to a subset of the ISO-8601 standard.
        //
        //	description:
        //		Accepts a string formatted according to a profile of ISO8601 as defined by
        //		[RFC3339](http://www.ietf.org/rfc/rfc3339.txt), except that partial input is allowed.
        //		Can also process dates as specified [by the W3C](http://www.w3.org/TR/NOTE-datetime)
        //		The following combinations are valid:
        //
        //			* dates only
        //			|	* yyyy
        //			|	* yyyy-MM
        //			|	* yyyy-MM-dd
        // 			* times only, with an optional time zone appended
        //			|	* THH:mm
        //			|	* THH:mm:ss
        //			|	* THH:mm:ss.SSS
        // 			* and "datetimes" which could be any combination of the above
        //
        //		timezones may be specified as Z (for UTC) or +/- followed by a time expression HH:mm
        //		Assumes the local time zone if not specified.  Does not validate.  Improperly formatted
        //		input may return null.  Arguments which are out of bounds will be handled
        // 		by the Date constructor (e.g. January 32nd typically gets resolved to February 1st)
        //		Only years between 100 and 9999 are supported.
        //
        //	formattedString:
        //		A string such as 2005-06-30T08:05:00-07:00 or 2005-06-30 or T08:05:00
        //
        //	defaultTime:
        //		Used for defaults for fields omitted in the formattedString.
        //		Uses 1970-01-01T00:00:00.0Z by default.

        if (!dojo.date.stamp._isoRegExp) {
            dojo.date.stamp._isoRegExp =
                //TODO: could be more restrictive and check for 00-59, etc.
            /^(?:(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?)?(?:T(\d{2}):(\d{2})(?::(\d{2})(.\d+)?)?((?:[+-](\d{2}):(\d{2}))|Z)?)?$/;
        }

        var match = dojo.date.stamp._isoRegExp.exec(formattedString);
        var result = null;

        if (match) {
            match.shift();
            if (match[1]) {
                match[1]--;
            } // Javascript Date months are 0-based
            if (match[6]) {
                match[6] *= 1000;
            } // Javascript Date expects fractional seconds as milliseconds

            if (defaultTime) {
                // mix in defaultTime.  Relatively expensive, so use || operators for the fast path of defaultTime === 0
                defaultTime = new Date(defaultTime);
                dojo.map(["FullYear", "Month", "Date", "Hours", "Minutes", "Seconds", "Milliseconds"], function(prop) {
                    return defaultTime["get" + prop]();
                }).forEach(function(value, index) {
                    if (match[index] === undefined) {
                        match[index] = value;
                    }
                });
            }
            result = new Date(match[0] || 1970, match[1] || 0, match[2] || 1, match[3] || 0, match[4] || 0, match[5] || 0, match[6] || 0);
            //		result.setFullYear(match[0]||1970); // for year < 100

            var offset = 0;
            var zoneSign = match[7] && match[7].charAt(0);
            if (zoneSign != 'Z') {
                offset = ((match[8] || 0) * 60) + (Number(match[9]) || 0);
                if (zoneSign != '-') {
                    offset *= -1;
                }
            }
            if (zoneSign) {
                offset -= result.getTimezoneOffset();
            }
            if (offset) {
                result.setTime(result.getTime() + offset * 60000);
            }
        }

        return result; // Date or null
    }

    /*=====
     dojo.date.stamp.__Options = function(){
     //	selector: String
     //		"date" or "time" for partial formatting of the Date object.
     //		Both date and time will be formatted by default.
     //	zulu: Boolean
     //		if true, UTC/GMT is used for a timezone
     //	milliseconds: Boolean
     //		if true, output milliseconds
     this.selector = selector;
     this.zulu = zulu;
     this.milliseconds = milliseconds;
     }
     =====*/

    dojo.date.stamp.toISOString = function(/*Date*/dateObject, /*dojo.date.stamp.__Options?*/options) {
        //	summary:
        //		Format a Date object as a string according a subset of the ISO-8601 standard
        //
        //	description:
        //		When options.selector is omitted, output follows [RFC3339](http://www.ietf.org/rfc/rfc3339.txt)
        //		The local time zone is included as an offset from GMT, except when selector=='time' (time without a date)
        //		Does not check bounds.  Only years between 100 and 9999 are supported.
        //
        //	dateObject:
        //		A Date object

        var _ = function(n) {
            return (n < 10) ? "0" + n : n;
        };
        options = options || {};
        var formattedDate = [];
        var getter = options.zulu ? "getUTC" : "get";
        var date = "";
        if (options.selector != "time") {
            var year = dateObject[getter + "FullYear"]();
            date = ["0000".substr((year + "").length) + year, _(dateObject[getter + "Month"]() + 1), _(dateObject[getter + "Date"]())].join('-');
        }
        formattedDate.push(date);
        if (options.selector != "date") {
            var time = [_(dateObject[getter + "Hours"]()), _(dateObject[getter + "Minutes"]()), _(dateObject[getter + "Seconds"]())].join(':');
            var millis = dateObject[getter + "Milliseconds"]();
            if (options.milliseconds) {
                time += "." + (millis < 100 ? "0" : "") + _(millis);
            }
            if (options.zulu) {
                time += "Z";
            } else if (options.selector != "time") {
                var timezoneOffset = dateObject.getTimezoneOffset();
                var absOffset = Math.abs(timezoneOffset);
                time += (timezoneOffset > 0 ? "-" : "+") +
                        _(Math.floor(absOffset / 60)) + ":" + _(absOffset % 60);
            }
            formattedDate.push(time);
        }
        return formattedDate.join('T'); // String
    }

}

if (!dojo._hasResource["dojo.parser"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.parser"] = true;
    dojo.provide("dojo.parser");


    dojo.parser = new function() {
        // summary: The Dom/Widget parsing package

        var d = dojo;
        var dtName = d._scopeName + "Type";
        var qry = "[" + dtName + "]";

        var _anonCtr = 0, _anon = {};
        var nameAnonFunc = function(/*Function*/anonFuncPtr, /*Object*/thisObj) {
            // summary:
            //		Creates a reference to anonFuncPtr in thisObj with a completely
            //		unique name. The new name is returned as a String.
            var nso = thisObj || _anon;
            if (dojo.isIE) {
                var cn = anonFuncPtr["__dojoNameCache"];
                if (cn && nso[cn] === anonFuncPtr) {
                    return cn;
                }
            }
            var name;
            do{
                name = "__" + _anonCtr++;
            } while (name in nso)
            nso[name] = anonFuncPtr;
            return name; // String
        }

        function val2type(/*Object*/ value) {
            // summary:
            //		Returns name of type of given value.

            if (d.isString(value)) {
                return "string";
            }
            if (typeof value == "number") {
                return "number";
            }
            if (typeof value == "boolean") {
                return "boolean";
            }
            if (d.isFunction(value)) {
                return "function";
            }
            if (d.isArray(value)) {
                return "array";
            } // typeof [] == "object"
            if (value instanceof Date) {
                return "date";
            } // assume timestamp
            if (value instanceof d._Url) {
                return "url";
            }
            return "object";
        }

        function str2obj(/*String*/ value, /*String*/ type) {
            // summary:
            //		Convert given string value to given type
            switch (type) {
                case "string":
                    return value;
                case "number":
                    return value.length ? Number(value) : NaN;
                case "boolean":
                    // for checked/disabled value might be "" or "checked".  interpret as true.
                    return typeof value == "boolean" ? value : !(value.toLowerCase() == "false");
                case "function":
                    if (d.isFunction(value)) {
                        // IE gives us a function, even when we say something like onClick="foo"
                        // (in which case it gives us an invalid function "function(){ foo }").
                        //  Therefore, convert to string
                        value = value.toString();
                        value = d.trim(value.substring(value.indexOf('{') + 1, value.length - 1));
                    }
                    try {
                        if (value.search(/[^\w\.]+/i) != -1) {
                            // TODO: "this" here won't work
                            value = nameAnonFunc(new Function(value), this);
                        }
                        return d.getObject(value, false);
                    } catch(e) {
                        return new Function();
                    }
                case "array":
                    return value ? value.split(/\s*,\s*/) : [];
                case "date":
                    switch (value) {
                        case "": return new Date("");    // the NaN of dates
                        case "now": return new Date();    // current date
                        default: return d.date.stamp.fromISOString(value);
                    }
                case "url":
                    return d.baseUrl + value;
                default:
                    return d.fromJson(value);
            }
        }

        var instanceClasses = {
            // map from fully qualified name (like "dijit.Button") to structure like
            // { cls: dijit.Button, params: {label: "string", disabled: "boolean"} }
        };

        function getClassInfo(/*String*/ className) {
            // className:
            //		fully qualified name (like "dijit.form.Button")
            // returns:
            //		structure like
            //			{
            //				cls: dijit.Button,
            //				params: { label: "string", disabled: "boolean"}
            //			}

            if (!instanceClasses[className]) {
                // get pointer to widget class
                var cls = d.getObject(className);
                if (!d.isFunction(cls)) {
                    throw new Error("Could not load class '" + className +
                                    "'. Did you spell the name correctly and use a full path, like 'dijit.form.Button'?");
                }
                var proto = cls.prototype;

                // get table of parameter names & types
                var params = {}, dummyClass = {};
                for (var name in proto) {
                    if (name.charAt(0) == "_") {
                        continue;
                    } 	// skip internal properties
                    if (name in dummyClass) {
                        continue;
                    }		// skip "constructor" and "toString"
                    var defVal = proto[name];
                    params[name] = val2type(defVal);
                }

                instanceClasses[className] = { cls: cls, params: params };
            }
            return instanceClasses[className];
        }

        this._functionFromScript = function(script) {
            var preamble = "";
            var suffix = "";
            var argsStr = script.getAttribute("args");
            if (argsStr) {
                d.forEach(argsStr.split(/\s*,\s*/), function(part, idx) {
                    preamble += "var " + part + " = arguments[" + idx + "]; ";
                });
            }
            var withStr = script.getAttribute("with");
            if (withStr && withStr.length) {
                d.forEach(withStr.split(/\s*,\s*/), function(part) {
                    preamble += "with(" + part + "){";
                    suffix += "}";
                });
            }
            return new Function(preamble + script.innerHTML + suffix);
        }

        this.instantiate = function(/* Array */nodes, /* Object? */mixin) {
            // summary:
            //		Takes array of nodes, and turns them into class instances and
            //		potentially calls a layout method to allow them to connect with
            //		any children
            // mixin: Object
            //		An object that will be mixed in with each node in the array.
            //		Values in the mixin will override values in the node, if they
            //		exist.
            var thelist = [];
            mixin = mixin || {};
            d.forEach(nodes, function(node) {
                if (!node) {
                    return;
                }
                var type = dtName in mixin ? mixin[dtName] : node.getAttribute(dtName);
                if (!type || !type.length) {
                    return;
                }
                var clsInfo = getClassInfo(type),
                        clazz = clsInfo.cls,
                        ps = clazz._noScript || clazz.prototype._noScript;

                // read parameters (ie, attributes).
                // clsInfo.params lists expected params like {"checked": "boolean", "n": "number"}
                var params = {},
                        attributes = node.attributes;
                for (var name in clsInfo.params) {
                    var item = name in mixin ? {value:mixin[name],specified:true} : attributes.getNamedItem(name);
                    if (!item || (!item.specified && (!dojo.isIE || name.toLowerCase() != "value"))) {
                        continue;
                    }
                    var value = item.value;
                    // Deal with IE quirks for 'class' and 'style'
                    switch (name) {
                        case "class":
                            value = "className" in mixin ? mixin.className : node.className;
                            break;
                        case "style":
                            value = "style" in mixin ? mixin.style : (node.style && node.style.cssText); // FIXME: Opera?
                    }
                    var _type = clsInfo.params[name];
                    if (typeof value == "string") {
                        params[name] = str2obj(value, _type);
                    } else {
                        params[name] = value;
                    }
                }

                // Process <script type="dojo/*"> script tags
                // <script type="dojo/method" event="foo"> tags are added to params, and passed to
                // the widget on instantiation.
                // <script type="dojo/method"> tags (with no event) are executed after instantiation
                // <script type="dojo/connect" event="foo"> tags are dojo.connected after instantiation
                // note: dojo/* script tags cannot exist in self closing widgets, like <input />
                if (!ps) {
                    var connects = [],    // functions to connect after instantiation
                            calls = [];		// functions to call after instantiation

                    d.query("> script[type^='dojo/']", node).orphan().forEach(function(script) {
                        var event = script.getAttribute("event"),
                                type = script.getAttribute("type"),
                                nf = d.parser._functionFromScript(script);
                        if (event) {
                            if (type == "dojo/connect") {
                                connects.push({event: event, func: nf});
                            } else {
                                params[event] = nf;
                            }
                        } else {
                            calls.push(nf);
                        }
                    });
                }

                var markupFactory = clazz["markupFactory"];
                if (!markupFactory && clazz["prototype"]) {
                    markupFactory = clazz.prototype["markupFactory"];
                }
                // create the instance
                var instance = markupFactory ? markupFactory(params, node, clazz) : new clazz(params, node);
                thelist.push(instance);

                // map it to the JS namespace if that makes sense
                var jsname = node.getAttribute("jsId");
                if (jsname) {
                    d.setObject(jsname, instance);
                }

                // process connections and startup functions
                if (!ps) {
                    d.forEach(connects, function(connect) {
                        d.connect(instance, connect.event, null, connect.func);
                    });
                    d.forEach(calls, function(func) {
                        func.call(instance);
                    });
                }
            });

            // Call startup on each top level instance if it makes sense (as for
            // widgets).  Parent widgets will recursively call startup on their
            // (non-top level) children
            d.forEach(thelist, function(instance) {
                if (instance &&
                    instance.startup &&
                    !instance._started &&
                    (!instance.getParent || !instance.getParent())
                        ) {
                    instance.startup();
                }
            });
            return thelist;
        };

        this.parse = function(/*DomNode?*/ rootNode) {
            // summary:
            //		Search specified node (or root node) recursively for class instances,
            //		and instantiate them Searches for
            //		dojoType="qualified.class.name"
            var list = d.query(qry, rootNode);
            // go build the object instances
            var instances = this.instantiate(list);
            return instances;
        };
    }();

    //Register the parser callback. It should be the first callback
    //after the a11y test.

    (function() {
        var parseRunner = function() {
            if (dojo.config["parseOnLoad"] == true) {
                dojo.parser.parse();
            }
        };

        // FIXME: need to clobber cross-dependency!!
        if (dojo.exists("dijit.wai.onload") && (dijit.wai.onload === dojo._loaders[0])) {
            dojo._loaders.splice(1, 0, parseRunner);
        } else {
            dojo._loaders.unshift(parseRunner);
        }
    })();

}

if (!dojo._hasResource["dojo.dnd.common"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.dnd.common"] = true;
    dojo.provide("dojo.dnd.common");

    dojo.dnd._isMac = navigator.appVersion.indexOf("Macintosh") >= 0;
    dojo.dnd._copyKey = dojo.dnd._isMac ? "metaKey" : "ctrlKey";

    dojo.dnd.getCopyKeyState = function(e) {
        // summary: abstracts away the difference between selection on Mac and PC,
        //	and returns the state of the "copy" key to be pressed.
        // e: Event: mouse event
        return e[dojo.dnd._copyKey];	// Boolean
    };

    dojo.dnd._uniqueId = 0;
    dojo.dnd.getUniqueId = function() {
        // summary: returns a unique string for use with any DOM element
        var id;
        do{
            id = dojo._scopeName + "Unique" + (++dojo.dnd._uniqueId);
        } while (dojo.byId(id));
        return id;
    };

    dojo.dnd._empty = {};

    dojo.dnd.isFormElement = function(/*Event*/ e) {
        // summary: returns true, if user clicked on a form element
        var t = e.target;
        if (t.nodeType == 3 /*TEXT_NODE*/) {
            t = t.parentNode;
        }
        return " button textarea input select option ".indexOf(" " + t.tagName.toLowerCase() + " ") >= 0;	// Boolean
    };

    // doesn't take into account when multiple buttons are pressed
    dojo.dnd._lmb = dojo.isIE ? 1 : 0;	// left mouse button

    dojo.dnd._isLmbPressed = dojo.isIE ?
                             function(e) {
                                 return e.button & 1;
                             } : // intentional bit-and
                             function(e) {
                                 return e.button === 0;
                             };

}

if (!dojo._hasResource["dojo.dnd.Container"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.dnd.Container"] = true;
    dojo.provide("dojo.dnd.Container");


    /*
     Container states:
     ""		- normal state
     "Over"	- mouse over a container
     Container item states:
     ""		- normal state
     "Over"	- mouse over a container item
     */

    dojo.declare("dojo.dnd.Container", null, {
        // summary: a Container object, which knows when mouse hovers over it,
        //	and over which element it hovers

        // object attributes (for markup)
        skipForm: false,

        constructor: function(node, params) {
            // summary: a constructor of the Container
            // node: Node: node or node's id to build the container on
            // params: Object: a dict of parameters, recognized parameters are:
            //	creator: Function: a creator function, which takes a data item, and returns an object like that:
            //		{node: newNode, data: usedData, type: arrayOfStrings}
            //	skipForm: Boolean: don't start the drag operation, if clicked on form elements
            //	dropParent: Node: node or node's id to use as the parent node for dropped items
            //		(must be underneath the 'node' parameter in the DOM)
            //	_skipStartup: Boolean: skip startup(), which collects children, for deferred initialization
            //		(this is used in the markup mode)
            this.node = dojo.byId(node);
            if (!params) {
                params = {};
            }
            this.creator = params.creator || null;
            this.skipForm = params.skipForm;
            this.parent = params.dropParent && dojo.byId(params.dropParent);

            // class-specific variables
            this.map = {};
            this.current = null;

            // states
            this.containerState = "";
            dojo.addClass(this.node, "dojoDndContainer");

            // mark up children
            if (!(params && params._skipStartup)) {
                this.startup();
            }

            // set up events
            this.events = [
                dojo.connect(this.node, "onmouseover", this, "onMouseOver"),
                dojo.connect(this.node, "onmouseout", this, "onMouseOut"),
                // cancel text selection and text dragging
                dojo.connect(this.node, "ondragstart", this, "onSelectStart"),
                dojo.connect(this.node, "onselectstart", this, "onSelectStart")
            ];
        },

        // object attributes (for markup)
        creator: function() {
        },    // creator function, dummy at the moment

        // abstract access to the map
        getItem: function(/*String*/ key) {
            // summary: returns a data item by its key (id)
            return this.map[key];	// Object
        },
        setItem: function(/*String*/ key, /*Object*/ data) {
            // summary: associates a data item with its key (id)
            this.map[key] = data;
        },
        delItem: function(/*String*/ key) {
            // summary: removes a data item from the map by its key (id)
            delete this.map[key];
        },
        forInItems: function(/*Function*/ f, /*Object?*/ o) {
            // summary: iterates over a data map skipping members, which
            //	are present in the empty object (IE and/or 3rd-party libraries).
            o = o || dojo.global;
            var m = this.map, e = dojo.dnd._empty;
            for (var i in m) {
                if (i in e) {
                    continue;
                }
                f.call(o, m[i], i, this);
            }
            return o;	// Object
        },
        clearItems: function() {
            // summary: removes all data items from the map
            this.map = {};
        },

        // methods
        getAllNodes: function() {
            // summary: returns a list (an array) of all valid child nodes
            return dojo.query("> .dojoDndItem", this.parent);	// NodeList
        },
        sync: function() {
            // summary: synch up the node list with the data map
            var map = {};
            this.getAllNodes().forEach(function(node) {
                if (node.id) {
                    var item = this.getItem(node.id);
                    if (item) {
                        map[node.id] = item;
                        return;
                    }
                } else {
                    node.id = dojo.dnd.getUniqueId();
                }
                var type = node.getAttribute("dndType"),
                        data = node.getAttribute("dndData");
                map[node.id] = {
                    data: data || node.innerHTML,
                    type: type ? type.split(/\s*,\s*/) : ["text"]
                };
            }, this);
            this.map = map;
            return this;	// self
        },
        insertNodes: function(data, before, anchor) {
            // summary: inserts an array of new nodes before/after an anchor node
            // data: Array: a list of data items, which should be processed by the creator function
            // before: Boolean: insert before the anchor, if true, and after the anchor otherwise
            // anchor: Node: the anchor node to be used as a point of insertion
            if (!this.parent.firstChild) {
                anchor = null;
            } else if (before) {
                if (!anchor) {
                    anchor = this.parent.firstChild;
                }
            } else {
                if (anchor) {
                    anchor = anchor.nextSibling;
                }
            }
            if (anchor) {
                for (var i = 0; i < data.length; ++i) {
                    var t = this._normalizedCreator(data[i]);
                    this.setItem(t.node.id, {data: t.data, type: t.type});
                    this.parent.insertBefore(t.node, anchor);
                }
            } else {
                for (var i = 0; i < data.length; ++i) {
                    var t = this._normalizedCreator(data[i]);
                    this.setItem(t.node.id, {data: t.data, type: t.type});
                    this.parent.appendChild(t.node);
                }
            }
            return this;	// self
        },
        destroy: function() {
            // summary: prepares the object to be garbage-collected
            dojo.forEach(this.events, dojo.disconnect);
            this.clearItems();
            this.node = this.parent = this.current = null;
        },

        // markup methods
        markupFactory: function(params, node) {
            params._skipStartup = true;
            return new dojo.dnd.Container(node, params);
        },
        startup: function() {
            // summary: collects valid child items and populate the map

            // set up the real parent node
            if (!this.parent) {
                // use the standard algorithm, if not assigned
                this.parent = this.node;
                if (this.parent.tagName.toLowerCase() == "table") {
                    var c = this.parent.getElementsByTagName("tbody");
                    if (c && c.length) {
                        this.parent = c[0];
                    }
                }
            }
            this.defaultCreator = dojo.dnd._defaultCreator(this.parent);

            // process specially marked children
            this.sync();
        },

        // mouse events
        onMouseOver: function(e) {
            // summary: event processor for onmouseover
            // e: Event: mouse event
            var n = e.relatedTarget;
            while (n) {
                if (n == this.node) {
                    break;
                }
                try {
                    n = n.parentNode;
                } catch(x) {
                    n = null;
                }
            }
            if (!n) {
                this._changeState("Container", "Over");
                this.onOverEvent();
            }
            n = this._getChildByEvent(e);
            if (this.current == n) {
                return;
            }
            if (this.current) {
                this._removeItemClass(this.current, "Over");
            }
            if (n) {
                this._addItemClass(n, "Over");
            }
            this.current = n;
        },
        onMouseOut: function(e) {
            // summary: event processor for onmouseout
            // e: Event: mouse event
            for (var n = e.relatedTarget; n;) {
                if (n == this.node) {
                    return;
                }
                try {
                    n = n.parentNode;
                } catch(x) {
                    n = null;
                }
            }
            if (this.current) {
                this._removeItemClass(this.current, "Over");
                this.current = null;
            }
            this._changeState("Container", "");
            this.onOutEvent();
        },
        onSelectStart: function(e) {
            // summary: event processor for onselectevent and ondragevent
            // e: Event: mouse event
            if (!this.skipForm || !dojo.dnd.isFormElement(e)) {
                dojo.stopEvent(e);
            }
        },

        // utilities
        onOverEvent: function() {
            // summary: this function is called once, when mouse is over our container
        },
        onOutEvent: function() {
            // summary: this function is called once, when mouse is out of our container
        },
        _changeState: function(type, newState) {
            // summary: changes a named state to new state value
            // type: String: a name of the state to change
            // newState: String: new state
            var prefix = "dojoDnd" + type;
            var state = type.toLowerCase() + "State";
            //dojo.replaceClass(this.node, prefix + newState, prefix + this[state]);
            dojo.removeClass(this.node, prefix + this[state]);
            dojo.addClass(this.node, prefix + newState);
            this[state] = newState;
        },
        _addItemClass: function(node, type) {
            // summary: adds a class with prefix "dojoDndItem"
            // node: Node: a node
            // type: String: a variable suffix for a class name
            dojo.addClass(node, "dojoDndItem" + type);
        },
        _removeItemClass: function(node, type) {
            // summary: removes a class with prefix "dojoDndItem"
            // node: Node: a node
            // type: String: a variable suffix for a class name
            dojo.removeClass(node, "dojoDndItem" + type);
        },
        _getChildByEvent: function(e) {
            // summary: gets a child, which is under the mouse at the moment, or null
            // e: Event: a mouse event
            var node = e.target;
            if (node) {
                for (var parent = node.parentNode; parent; node = parent,parent = node.parentNode) {
                    if (parent == this.parent && dojo.hasClass(node, "dojoDndItem")) {
                        return node;
                    }
                }
            }
            return null;
        },
        _normalizedCreator: function(item, hint) {
            // summary: adds all necessary data to the output of the user-supplied creator function
            var t = (this.creator || this.defaultCreator).call(this, item, hint);
            if (!dojo.isArray(t.type)) {
                t.type = ["text"];
            }
            if (!t.node.id) {
                t.node.id = dojo.dnd.getUniqueId();
            }
            dojo.addClass(t.node, "dojoDndItem");
            return t;
        }
    });

    dojo.dnd._createNode = function(tag) {
        // summary: returns a function, which creates an element of given tag
        //	(SPAN by default) and sets its innerHTML to given text
        // tag: String: a tag name or empty for SPAN
        if (!tag) {
            return dojo.dnd._createSpan;
        }
        return function(text) {    // Function
            return dojo.create(tag, {innerHTML: text});	// Node
        };
    };

    dojo.dnd._createTrTd = function(text) {
        // summary: creates a TR/TD structure with given text as an innerHTML of TD
        // text: String: a text for TD
        var tr = dojo.create("tr");
        dojo.create("td", {innerHTML: text}, tr);
        return tr;	// Node
    };

    dojo.dnd._createSpan = function(text) {
        // summary: creates a SPAN element with given text as its innerHTML
        // text: String: a text for SPAN
        return dojo.create("span", {innerHTML: text});	// Node
    };

    // dojo.dnd._defaultCreatorNodes: Object: a dicitionary, which maps container tag names to child tag names
    dojo.dnd._defaultCreatorNodes = {ul: "li", ol: "li", div: "div", p: "div"};

    dojo.dnd._defaultCreator = function(node) {
        // summary: takes a parent node, and returns an appropriate creator function
        // node: Node: a container node
        var tag = node.tagName.toLowerCase();
        var c = tag == "tbody" || tag == "thead" ? dojo.dnd._createTrTd :
                dojo.dnd._createNode(dojo.dnd._defaultCreatorNodes[tag]);
        return function(item, hint) {    // Function
            var isObj = item && dojo.isObject(item), data, type, n;
            if (isObj && item.tagName && item.nodeType && item.getAttribute) {
                // process a DOM node
                data = item.getAttribute("dndData") || item.innerHTML;
                type = item.getAttribute("dndType");
                type = type ? type.split(/\s*,\s*/) : ["text"];
                n = item;	// this node is going to be moved rather than copied
            } else {
                // process a DnD item object or a string
                data = (isObj && item.data) ? item.data : item;
                type = (isObj && item.type) ? item.type : ["text"];
                n = (hint == "avatar" ? dojo.dnd._createSpan : c)(String(data));
            }
            n.id = dojo.dnd.getUniqueId();
            return {node: n, data: data, type: type};
        };
    };

}

if (!dojo._hasResource["dojo.dnd.Selector"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.dnd.Selector"] = true;
    dojo.provide("dojo.dnd.Selector");


    /*
     Container item states:
     ""			- an item is not selected
     "Selected"	- an item is selected
     "Anchor"	- an item is selected, and is an anchor for a "shift" selection
     */

    dojo.declare("dojo.dnd.Selector", dojo.dnd.Container, {
        // summary: a Selector object, which knows how to select its children

        constructor: function(node, params) {
            // summary: a constructor of the Selector
            // node: Node: node or node's id to build the selector on
            // params: Object: a dict of parameters, recognized parameters are:
            //	singular: Boolean
            //		allows selection of only one element, if true
            //		the rest of parameters are passed to the container
            //	autoSync: Boolean
            //		autosynchronizes the source with its list of DnD nodes,
            //		false by default
            if (!params) {
                params = {};
            }
            this.singular = params.singular;
            this.autoSync = params.autoSync;
            // class-specific variables
            this.selection = {};
            this.anchor = null;
            this.simpleSelection = false;
            // set up events
            this.events.push(
                    dojo.connect(this.node, "onmousedown", this, "onMouseDown"),
                    dojo.connect(this.node, "onmouseup", this, "onMouseUp"));
        },

        // object attributes (for markup)
        singular: false,    // is singular property

        // methods
        getSelectedNodes: function() {
            // summary: returns a list (an array) of selected nodes
            var t = new dojo.NodeList();
            var e = dojo.dnd._empty;
            for (var i in this.selection) {
                if (i in e) {
                    continue;
                }
                t.push(dojo.byId(i));
            }
            return t;	// Array
        },
        selectNone: function() {
            // summary: unselects all items
            return this._removeSelection()._removeAnchor();	// self
        },
        selectAll: function() {
            // summary: selects all items
            this.forInItems(function(data, id) {
                this._addItemClass(dojo.byId(id), "Selected");
                this.selection[id] = 1;
            }, this);
            return this._removeAnchor();	// self
        },
        deleteSelectedNodes: function() {
            // summary: deletes all selected items
            var e = dojo.dnd._empty;
            for (var i in this.selection) {
                if (i in e) {
                    continue;
                }
                var n = dojo.byId(i);
                this.delItem(i);
                dojo.destroy(n);
            }
            this.anchor = null;
            this.selection = {};
            return this;	// self
        },
        forInSelectedItems: function(/*Function*/ f, /*Object?*/ o) {
            // summary: iterates over selected items,
            // see dojo.dnd.Container.forInItems() for details
            o = o || dojo.global;
            var s = this.selection, e = dojo.dnd._empty;
            for (var i in s) {
                if (i in e) {
                    continue;
                }
                f.call(o, this.getItem(i), i, this);
            }
        },
        sync: function() {
            // summary: synch up the node list with the data map

            dojo.dnd.Selector.superclass.sync.call(this);

            // fix the anchor
            if (this.anchor) {
                if (!this.getItem(this.anchor.id)) {
                    this.anchor = null;
                }
            }

            // fix the selection
            var t = [], e = dojo.dnd._empty;
            for (var i in this.selection) {
                if (i in e) {
                    continue;
                }
                if (!this.getItem(i)) {
                    t.push(i);
                }
            }
            dojo.forEach(t, function(i) {
                delete this.selection[i];
            }, this);

            return this;	// self
        },
        insertNodes: function(addSelected, data, before, anchor) {
            // summary: inserts new data items (see Container's insertNodes method for details)
            // addSelected: Boolean: all new nodes will be added to selected items, if true, no selection change otherwise
            // data: Array: a list of data items, which should be processed by the creator function
            // before: Boolean: insert before the anchor, if true, and after the anchor otherwise
            // anchor: Node: the anchor node to be used as a point of insertion
            var oldCreator = this._normalizedCreator;
            this._normalizedCreator = function(item, hint) {
                var t = oldCreator.call(this, item, hint);
                if (addSelected) {
                    if (!this.anchor) {
                        this.anchor = t.node;
                        this._removeItemClass(t.node, "Selected");
                        this._addItemClass(this.anchor, "Anchor");
                    } else if (this.anchor != t.node) {
                        this._removeItemClass(t.node, "Anchor");
                        this._addItemClass(t.node, "Selected");
                    }
                    this.selection[t.node.id] = 1;
                } else {
                    this._removeItemClass(t.node, "Selected");
                    this._removeItemClass(t.node, "Anchor");
                }
                return t;
            };
            dojo.dnd.Selector.superclass.insertNodes.call(this, data, before, anchor);
            this._normalizedCreator = oldCreator;
            return this;	// self
        },
        destroy: function() {
            // summary: prepares the object to be garbage-collected
            dojo.dnd.Selector.superclass.destroy.call(this);
            this.selection = this.anchor = null;
        },

        // markup methods
        markupFactory: function(params, node) {
            params._skipStartup = true;
            return new dojo.dnd.Selector(node, params);
        },

        // mouse events
        onMouseDown: function(e) {
            // summary: event processor for onmousedown
            // e: Event: mouse event
            if (this.autoSync) {
                this.sync();
            }
            if (!this.current) {
                return;
            }
            if (!this.singular && !dojo.dnd.getCopyKeyState(e) && !e.shiftKey && (this.current.id in this.selection)) {
                this.simpleSelection = true;
                if (e.button === dojo.dnd._lmb) {
                    // accept the left button and stop the event
                    // for IE we don't stop event when multiple buttons are pressed
                    dojo.stopEvent(e);
                }
                return;
            }
            if (!this.singular && e.shiftKey) {
                if (!dojo.dnd.getCopyKeyState(e)) {
                    this._removeSelection();
                }
                var c = this.getAllNodes();
                if (c.length) {
                    if (!this.anchor) {
                        this.anchor = c[0];
                        this._addItemClass(this.anchor, "Anchor");
                    }
                    this.selection[this.anchor.id] = 1;
                    if (this.anchor != this.current) {
                        var i = 0;
                        for (; i < c.length; ++i) {
                            var node = c[i];
                            if (node == this.anchor || node == this.current) {
                                break;
                            }
                        }
                        for (++i; i < c.length; ++i) {
                            var node = c[i];
                            if (node == this.anchor || node == this.current) {
                                break;
                            }
                            this._addItemClass(node, "Selected");
                            this.selection[node.id] = 1;
                        }
                        this._addItemClass(this.current, "Selected");
                        this.selection[this.current.id] = 1;
                    }
                }
            } else {
                if (this.singular) {
                    if (this.anchor == this.current) {
                        if (dojo.dnd.getCopyKeyState(e)) {
                            this.selectNone();
                        }
                    } else {
                        this.selectNone();
                        this.anchor = this.current;
                        this._addItemClass(this.anchor, "Anchor");
                        this.selection[this.current.id] = 1;
                    }
                } else {
                    if (dojo.dnd.getCopyKeyState(e)) {
                        if (this.anchor == this.current) {
                            delete this.selection[this.anchor.id];
                            this._removeAnchor();
                        } else {
                            if (this.current.id in this.selection) {
                                this._removeItemClass(this.current, "Selected");
                                delete this.selection[this.current.id];
                            } else {
                                if (this.anchor) {
                                    this._removeItemClass(this.anchor, "Anchor");
                                    this._addItemClass(this.anchor, "Selected");
                                }
                                this.anchor = this.current;
                                this._addItemClass(this.current, "Anchor");
                                this.selection[this.current.id] = 1;
                            }
                        }
                    } else {
                        if (!(this.current.id in this.selection)) {
                            this.selectNone();
                            this.anchor = this.current;
                            this._addItemClass(this.current, "Anchor");
                            this.selection[this.current.id] = 1;
                        }
                    }
                }
            }
            dojo.stopEvent(e);
        },
        onMouseUp: function(e) {
            // summary: event processor for onmouseup
            // e: Event: mouse event
            if (!this.simpleSelection) {
                return;
            }
            this.simpleSelection = false;
            this.selectNone();
            if (this.current) {
                this.anchor = this.current;
                this._addItemClass(this.anchor, "Anchor");
                this.selection[this.current.id] = 1;
            }
        },
        onMouseMove: function(e) {
            // summary: event processor for onmousemove
            // e: Event: mouse event
            this.simpleSelection = false;
        },

        // utilities
        onOverEvent: function() {
            // summary: this function is called once, when mouse is over our container
            this.onmousemoveEvent = dojo.connect(this.node, "onmousemove", this, "onMouseMove");
        },
        onOutEvent: function() {
            // summary: this function is called once, when mouse is out of our container
            dojo.disconnect(this.onmousemoveEvent);
            delete this.onmousemoveEvent;
        },
        _removeSelection: function() {
            // summary: unselects all items
            var e = dojo.dnd._empty;
            for (var i in this.selection) {
                if (i in e) {
                    continue;
                }
                var node = dojo.byId(i);
                if (node) {
                    this._removeItemClass(node, "Selected");
                }
            }
            this.selection = {};
            return this;	// self
        },
        _removeAnchor: function() {
            if (this.anchor) {
                this._removeItemClass(this.anchor, "Anchor");
                this.anchor = null;
            }
            return this;	// self
        }
    });

}

if (!dojo._hasResource["dojo.dnd.autoscroll"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.dnd.autoscroll"] = true;
    dojo.provide("dojo.dnd.autoscroll");

    dojo.dnd.getViewport = function() {
        // summary: returns a viewport size (visible part of the window)

        // FIXME: need more docs!!
        var d = dojo.doc, dd = d.documentElement, w = window, b = dojo.body();
        if (dojo.isMozilla) {
            return {w: dd.clientWidth, h: w.innerHeight};	// Object
        } else if (!dojo.isOpera && w.innerWidth) {
            return {w: w.innerWidth, h: w.innerHeight};		// Object
        } else if (!dojo.isOpera && dd && dd.clientWidth) {
            return {w: dd.clientWidth, h: dd.clientHeight};	// Object
        } else if (b.clientWidth) {
            return {w: b.clientWidth, h: b.clientHeight};	// Object
        }
        return null;	// Object
    };

    dojo.dnd.V_TRIGGER_AUTOSCROLL = 32;
    dojo.dnd.H_TRIGGER_AUTOSCROLL = 32;

    dojo.dnd.V_AUTOSCROLL_VALUE = 16;
    dojo.dnd.H_AUTOSCROLL_VALUE = 16;

    dojo.dnd.autoScroll = function(e) {
        // summary:
        //		a handler for onmousemove event, which scrolls the window, if
        //		necesary
        // e: Event:
        //		onmousemove event

        // FIXME: needs more docs!
        var v = dojo.dnd.getViewport(), dx = 0, dy = 0;
        if (e.clientX < dojo.dnd.H_TRIGGER_AUTOSCROLL) {
            dx = -dojo.dnd.H_AUTOSCROLL_VALUE;
        } else if (e.clientX > v.w - dojo.dnd.H_TRIGGER_AUTOSCROLL) {
            dx = dojo.dnd.H_AUTOSCROLL_VALUE;
        }
        if (e.clientY < dojo.dnd.V_TRIGGER_AUTOSCROLL) {
            dy = -dojo.dnd.V_AUTOSCROLL_VALUE;
        } else if (e.clientY > v.h - dojo.dnd.V_TRIGGER_AUTOSCROLL) {
            dy = dojo.dnd.V_AUTOSCROLL_VALUE;
        }
        window.scrollBy(dx, dy);
    };

    dojo.dnd._validNodes = {"div": 1, "p": 1, "td": 1};
    dojo.dnd._validOverflow = {"auto": 1, "scroll": 1};

    dojo.dnd.autoScrollNodes = function(e) {
        // summary:
        //		a handler for onmousemove event, which scrolls the first avaialble
        //		Dom element, it falls back to dojo.dnd.autoScroll()
        // e: Event:
        //		onmousemove event

        // FIXME: needs more docs!
        for (var n = e.target; n;) {
            if (n.nodeType == 1 && (n.tagName.toLowerCase() in dojo.dnd._validNodes)) {
                var s = dojo.getComputedStyle(n);
                if (s.overflow.toLowerCase() in dojo.dnd._validOverflow) {
                    var b = dojo._getContentBox(n, s), t = dojo._abs(n, true);
                    //console.log(b.l, b.t, t.x, t.y, n.scrollLeft, n.scrollTop);
                    var w = Math.min(dojo.dnd.H_TRIGGER_AUTOSCROLL, b.w / 2),
                            h = Math.min(dojo.dnd.V_TRIGGER_AUTOSCROLL, b.h / 2),
                            rx = e.pageX - t.x, ry = e.pageY - t.y, dx = 0, dy = 0;
                    if (dojo.isWebKit || dojo.isOpera) {
                        // FIXME: this code should not be here, it should be taken into account
                        // either by the event fixing code, or the dojo._abs()
                        // FIXME: this code doesn't work on Opera 9.5 Beta
                        rx += dojo.body().scrollLeft,ry += dojo.body().scrollTop;
                    }
                    if (rx > 0 && rx < b.w) {
                        if (rx < w) {
                            dx = -w;
                        } else if (rx > b.w - w) {
                            dx = w;
                        }
                    }
                    //console.log("ry =", ry, "b.h =", b.h, "h =", h);
                    if (ry > 0 && ry < b.h) {
                        if (ry < h) {
                            dy = -h;
                        } else if (ry > b.h - h) {
                            dy = h;
                        }
                    }
                    var oldLeft = n.scrollLeft, oldTop = n.scrollTop;
                    n.scrollLeft = n.scrollLeft + dx;
                    n.scrollTop = n.scrollTop + dy;
                    if (oldLeft != n.scrollLeft || oldTop != n.scrollTop) {
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

if (!dojo._hasResource["dojo.dnd.Avatar"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.dnd.Avatar"] = true;
    dojo.provide("dojo.dnd.Avatar");


    dojo.declare("dojo.dnd.Avatar", null, {
        // summary: an object, which represents transferred DnD items visually
        // manager: Object: a DnD manager object

        constructor: function(manager) {
            this.manager = manager;
            this.construct();
        },

        // methods
        construct: function() {
            // summary: a constructor function;
            //	it is separate so it can be (dynamically) overwritten in case of need
            var a = dojo.create("table", {
                "class": "dojoDndAvatar",
                style: {
                    position: "absolute",
                    zIndex:   "1999",
                    margin:   "0px"
                }
            }),
                    b = dojo.create("tbody", null, a),
                    tr = dojo.create("tr", null, b),
                    td = dojo.create("td", {
                        innerHTML: this._generateText()
                    }, tr),
                    k = Math.min(5, this.manager.nodes.length), i = 0,
                    source = this.manager.source, node;
            // we have to set the opacity on IE only after the node is live
            dojo.attr(tr, {
                "class": "dojoDndAvatarHeader",
                style: {opacity: 0.9}
            });
            for (; i < k; ++i) {
                if (source.creator) {
                    // create an avatar representation of the node
                    node = source._normalizedCreator(source.getItem(this.manager.nodes[i].id).data, "avatar").node;
                } else {
                    // or just clone the node and hope it works
                    node = this.manager.nodes[i].cloneNode(true);
                    if (node.tagName.toLowerCase() == "tr") {
                        // insert extra table nodes
                        var table = dojo.create("table"),
                                tbody = dojo.create("tbody", null, table);
                        tbody.appendChild(node);
                        node = table;
                    }
                }
                node.id = "";
                tr = dojo.create("tr", null, b);
                td = dojo.create("td", null, tr);
                td.appendChild(node);
                dojo.attr(tr, {
                    "class": "dojoDndAvatarItem",
                    style: {opacity: (9 - i) / 10}
                });
            }
            this.node = a;
        },
        destroy: function() {
            // summary: a desctructor for the avatar, called to remove all references so it can be garbage-collected
            dojo.destroy(this.node);
            this.node = false;
        },
        update: function() {
            // summary: updates the avatar to reflect the current DnD state
            dojo[(this.manager.canDropFlag ? "add" : "remove") + "Class"](this.node, "dojoDndAvatarCanDrop");
            // replace text
            dojo.query("tr.dojoDndAvatarHeader td", this.node).forEach(function(node) {
                node.innerHTML = this._generateText();
            }, this);
        },
        _generateText: function() {
            // summary: generates a proper text to reflect copying or moving of items
            return this.manager.nodes.length.toString();
        }
    });

}

if (!dojo._hasResource["dojo.dnd.Manager"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.dnd.Manager"] = true;
    dojo.provide("dojo.dnd.Manager");


    dojo.declare("dojo.dnd.Manager", null, {
        // summary: the manager of DnD operations (usually a singleton)
        constructor: function() {
            this.avatar = null;
            this.source = null;
            this.nodes = [];
            this.copy = true;
            this.target = null;
            this.canDropFlag = false;
            this.events = [];
        },

        // avatar's offset from the mouse
        OFFSET_X: 16,
        OFFSET_Y: 16,

        // methods
        overSource: function(source) {
            // summary: called when a source detected a mouse-over conditiion
            // source: Object: the reporter
            if (this.avatar) {
                this.target = (source && source.targetState != "Disabled") ? source : null;
                this.canDropFlag = Boolean(this.target);
                this.avatar.update();
            }
            dojo.publish("/dnd/source/over", [source]);
        },
        outSource: function(source) {
            // summary: called when a source detected a mouse-out conditiion
            // source: Object: the reporter
            if (this.avatar) {
                if (this.target == source) {
                    this.target = null;
                    this.canDropFlag = false;
                    this.avatar.update();
                    dojo.publish("/dnd/source/over", [null]);
                }
            } else {
                dojo.publish("/dnd/source/over", [null]);
            }
        },
        startDrag: function(source, nodes, copy) {
            // summary: called to initiate the DnD operation
            // source: Object: the source which provides items
            // nodes: Array: the list of transferred items
            // copy: Boolean: copy items, if true, move items otherwise
            this.source = source;
            this.nodes = nodes;
            this.copy = Boolean(copy); // normalizing to true boolean
            this.avatar = this.makeAvatar();
            dojo.body().appendChild(this.avatar.node);
            dojo.publish("/dnd/start", [source, nodes, this.copy]);
            this.events = [
                dojo.connect(dojo.doc, "onmousemove", this, "onMouseMove"),
                dojo.connect(dojo.doc, "onmouseup", this, "onMouseUp"),
                dojo.connect(dojo.doc, "onkeydown", this, "onKeyDown"),
                dojo.connect(dojo.doc, "onkeyup", this, "onKeyUp"),
                // cancel text selection and text dragging
                dojo.connect(dojo.doc, "ondragstart", dojo.stopEvent),
                dojo.connect(dojo.body(), "onselectstart", dojo.stopEvent)
            ];
            var c = "dojoDnd" + (copy ? "Copy" : "Move");
            dojo.addClass(dojo.body(), c);
        },
        canDrop: function(flag) {
            // summary: called to notify if the current target can accept items
            var canDropFlag = Boolean(this.target && flag);
            if (this.canDropFlag != canDropFlag) {
                this.canDropFlag = canDropFlag;
                this.avatar.update();
            }
        },
        stopDrag: function() {
            // summary: stop the DnD in progress
            dojo.removeClass(dojo.body(), "dojoDndCopy");
            dojo.removeClass(dojo.body(), "dojoDndMove");
            dojo.forEach(this.events, dojo.disconnect);
            this.events = [];
            this.avatar.destroy();
            this.avatar = null;
            this.source = this.target = null;
            this.nodes = [];
        },
        makeAvatar: function() {
            // summary: makes the avatar, it is separate to be overwritten dynamically, if needed
            return new dojo.dnd.Avatar(this);
        },
        updateAvatar: function() {
            // summary: updates the avatar, it is separate to be overwritten dynamically, if needed
            this.avatar.update();
        },

        // mouse event processors
        onMouseMove: function(e) {
            // summary: event processor for onmousemove
            // e: Event: mouse event
            var a = this.avatar;
            if (a) {
                dojo.dnd.autoScrollNodes(e);
                //dojo.dnd.autoScroll(e);
                var s = a.node.style;
                s.left = (e.pageX + this.OFFSET_X) + "px";
                s.top = (e.pageY + this.OFFSET_Y) + "px";
                var copy = Boolean(this.source.copyState(dojo.dnd.getCopyKeyState(e)));
                if (this.copy != copy) {
                    this._setCopyStatus(copy);
                }
            }
        },
        onMouseUp: function(e) {
            // summary: event processor for onmouseup
            // e: Event: mouse event
            if (this.avatar) {
                if (this.target && this.canDropFlag) {
                    var copy = Boolean(this.source.copyState(dojo.dnd.getCopyKeyState(e))),
                            params = [this.source, this.nodes, copy, this.target];
                    dojo.publish("/dnd/drop/before", params);
                    dojo.publish("/dnd/drop", params);
                } else {
                    dojo.publish("/dnd/cancel");
                }
                this.stopDrag();
            }
        },

        // keyboard event processors
        onKeyDown: function(e) {
            // summary: event processor for onkeydown:
            //	watching for CTRL for copy/move status, watching for ESCAPE to cancel the drag
            // e: Event: keyboard event
            if (this.avatar) {
                switch (e.keyCode) {
                    case dojo.keys.CTRL:
                        var copy = Boolean(this.source.copyState(true));
                        if (this.copy != copy) {
                            this._setCopyStatus(copy);
                        }
                        break;
                    case dojo.keys.ESCAPE:
                        dojo.publish("/dnd/cancel");
                        this.stopDrag();
                        break;
                }
            }
        },
        onKeyUp: function(e) {
            // summary: event processor for onkeyup, watching for CTRL for copy/move status
            // e: Event: keyboard event
            if (this.avatar && e.keyCode == dojo.keys.CTRL) {
                var copy = Boolean(this.source.copyState(false));
                if (this.copy != copy) {
                    this._setCopyStatus(copy);
                }
            }
        },

        // utilities
        _setCopyStatus: function(copy) {
            // summary: changes the copy status
            // copy: Boolean: the copy status
            this.copy = copy;
            this.source._markDndStatus(this.copy);
            this.updateAvatar();
            dojo.removeClass(dojo.body(), "dojoDnd" + (this.copy ? "Move" : "Copy"));
            dojo.addClass(dojo.body(), "dojoDnd" + (this.copy ? "Copy" : "Move"));
        }
    });

    // summary: the manager singleton variable, can be overwritten, if needed
    dojo.dnd._manager = null;

    dojo.dnd.manager = function() {
        // summary: returns the current DnD manager, creates one if it is not created yet
        if (!dojo.dnd._manager) {
            dojo.dnd._manager = new dojo.dnd.Manager();
        }
        return dojo.dnd._manager;	// Object
    };

}

if (!dojo._hasResource["dojo.dnd.Source"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.dnd.Source"] = true;
    dojo.provide("dojo.dnd.Source");


    /*
     Container property:
     "Horizontal"- if this is the horizontal container
     Source states:
     ""			- normal state
     "Moved"		- this source is being moved
     "Copied"	- this source is being copied
     Target states:
     ""			- normal state
     "Disabled"	- the target cannot accept an avatar
     Target anchor state:
     ""			- item is not selected
     "Before"	- insert point is before the anchor
     "After"		- insert point is after the anchor
     */

    /*=====
     dojo.dnd.__SourceArgs = function(){
     //	summary:
     //		a dict of parameters for DnD Source configuration. Note that any
     //		property on Source elements may be configured, but this is the
     //		short-list
     //	isSource: Boolean?
     //		can be used as a DnD source. Defaults to true.
     //	accept: Array?
     //		list of accepted types (text strings) for a target; defaults to
     //		["text"]
     //	autoSync: Boolean
     //		if true refreshes the node list on every operation; false by default
     //	copyOnly: Boolean?
     //		copy items, if true, use a state of Ctrl key otherwise,
     //		see selfCopy and selfAccept for more details
     //	delay: Number
     //		the move delay in pixels before detecting a drag; 0 by default
     //	horizontal: Boolean?
     //		a horizontal container, if true, vertical otherwise or when omitted
     //	selfCopy: Boolean?
     //		copy items by default when dropping on itself,
     //		false by default, works only if copyOnly is true
     //	selfAccept: Boolean?
     //		accept its own items when copyOnly is true,
     //		true by default, works only if copyOnly is true
     //	withHandles: Boolean?
     //		allows dragging only by handles, false by default
     this.isSource = isSource;
     this.accept = accept;
     this.autoSync = autoSync;
     this.copyOnly = copyOnly;
     this.delay = delay;
     this.horizontal = horizontal;
     this.selfCopy = selfCopy;
     this.selfAccept = selfAccept;
     this.withHandles = withHandles;
     }
     =====*/

    dojo.declare("dojo.dnd.Source", dojo.dnd.Selector, {
        // summary: a Source object, which can be used as a DnD source, or a DnD target

        // object attributes (for markup)
        isSource: true,
        horizontal: false,
        copyOnly: false,
        selfCopy: false,
        selfAccept: true,
        skipForm: false,
        withHandles: false,
        autoSync: false,
        delay: 0, // pixels
        accept: ["text"],

        constructor: function(/*DOMNode|String*/node, /*dojo.dnd.__SourceArgs?*/params) {
            // summary:
            //		a constructor of the Source
            // node:
            //		node or node's id to build the source on
            // params:
            //		any property of this class may be configured via the params
            //		object which is mixed-in to the `dojo.dnd.Source` instance
            dojo.mixin(this, dojo.mixin({}, params));
            var type = this.accept;
            if (type.length) {
                this.accept = {};
                for (var i = 0; i < type.length; ++i) {
                    this.accept[type[i]] = 1;
                }
            }
            // class-specific variables
            this.isDragging = false;
            this.mouseDown = false;
            this.targetAnchor = null;
            this.targetBox = null;
            this.before = true;
            this._lastX = 0;
            this._lastY = 0;
            // states
            this.sourceState = "";
            if (this.isSource) {
                dojo.addClass(this.node, "dojoDndSource");
            }
            this.targetState = "";
            if (this.accept) {
                dojo.addClass(this.node, "dojoDndTarget");
            }
            if (this.horizontal) {
                dojo.addClass(this.node, "dojoDndHorizontal");
            }
            // set up events
            this.topics = [
                dojo.subscribe("/dnd/source/over", this, "onDndSourceOver"),
                dojo.subscribe("/dnd/start", this, "onDndStart"),
                dojo.subscribe("/dnd/drop", this, "onDndDrop"),
                dojo.subscribe("/dnd/cancel", this, "onDndCancel")
            ];
        },

        // methods
        checkAcceptance: function(source, nodes) {
            // summary: checks, if the target can accept nodes from this source
            // source: Object: the source which provides items
            // nodes: Array: the list of transferred items
            if (this == source) {
                return !this.copyOnly || this.selfAccept;
            }
            for (var i = 0; i < nodes.length; ++i) {
                var type = source.getItem(nodes[i].id).type;
                // type instanceof Array
                var flag = false;
                for (var j = 0; j < type.length; ++j) {
                    if (type[j] in this.accept) {
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    return false;	// Boolean
                }
            }
            return true;	// Boolean
        },
        copyState: function(keyPressed, self) {
            // summary: Returns true, if we need to copy items, false to move.
            //		It is separated to be overwritten dynamically, if needed.
            // keyPressed: Boolean: the "copy" was pressed
            // self: Boolean?: optional flag, which means that we are about to drop on itself

            if (keyPressed) {
                return true;
            }
            if (arguments.length < 2) {
                self = this == dojo.dnd.manager().target;
            }
            if (self) {
                if (this.copyOnly) {
                    return this.selfCopy;
                }
            } else {
                return this.copyOnly;
            }
            return false;	// Boolean
        },
        destroy: function() {
            // summary: prepares the object to be garbage-collected
            dojo.dnd.Source.superclass.destroy.call(this);
            dojo.forEach(this.topics, dojo.unsubscribe);
            this.targetAnchor = null;
        },

        // markup methods
        markupFactory: function(params, node) {
            params._skipStartup = true;
            return new dojo.dnd.Source(node, params);
        },

        // mouse event processors
        onMouseMove: function(e) {
            // summary: event processor for onmousemove
            // e: Event: mouse event
            if (this.isDragging && this.targetState == "Disabled") {
                return;
            }
            dojo.dnd.Source.superclass.onMouseMove.call(this, e);
            var m = dojo.dnd.manager();
            if (this.isDragging) {
                // calculate before/after
                var before = false;
                if (this.current) {
                    if (!this.targetBox || this.targetAnchor != this.current) {
                        this.targetBox = {
                            xy: dojo.coords(this.current, true),
                            w: this.current.offsetWidth,
                            h: this.current.offsetHeight
                        };
                    }
                    if (this.horizontal) {
                        before = (e.pageX - this.targetBox.xy.x) < (this.targetBox.w / 2);
                    } else {
                        before = (e.pageY - this.targetBox.xy.y) < (this.targetBox.h / 2);
                    }
                }
                if (this.current != this.targetAnchor || before != this.before) {
                    this._markTargetAnchor(before);
                    m.canDrop(!this.current || m.source != this || !(this.current.id in this.selection));
                }
            } else {
                if (this.mouseDown && this.isSource &&
                    (Math.abs(e.pageX - this._lastX) > this.delay || Math.abs(e.pageY - this._lastY) > this.delay)) {
                    var nodes = this.getSelectedNodes();
                    if (nodes.length) {
                        m.startDrag(this, nodes, this.copyState(dojo.dnd.getCopyKeyState(e), true));
                    }
                }
            }
        },
        onMouseDown: function(e) {
            // summary: event processor for onmousedown
            // e: Event: mouse event
            if (!this.mouseDown && this._legalMouseDown(e) && (!this.skipForm || !dojo.dnd.isFormElement(e))) {
                this.mouseDown = true;
                this._lastX = e.pageX;
                this._lastY = e.pageY;
                dojo.dnd.Source.superclass.onMouseDown.call(this, e);
            }
        },
        onMouseUp: function(e) {
            // summary: event processor for onmouseup
            // e: Event: mouse event
            if (this.mouseDown) {
                this.mouseDown = false;
                dojo.dnd.Source.superclass.onMouseUp.call(this, e);
            }
        },

        // topic event processors
        onDndSourceOver: function(source) {
            // summary: topic event processor for /dnd/source/over, called when detected a current source
            // source: Object: the source which has the mouse over it
            if (this != source) {
                this.mouseDown = false;
                if (this.targetAnchor) {
                    this._unmarkTargetAnchor();
                }
            } else if (this.isDragging) {
                var m = dojo.dnd.manager();
                m.canDrop(this.targetState != "Disabled" && (!this.current || m.source != this || !(this.current.id in this.selection)));
            }
        },
        onDndStart: function(source, nodes, copy) {
            // summary: topic event processor for /dnd/start, called to initiate the DnD operation
            // source: Object: the source which provides items
            // nodes: Array: the list of transferred items
            // copy: Boolean: copy items, if true, move items otherwise
            if (this.autoSync) {
                this.sync();
            }
            if (this.isSource) {
                this._changeState("Source", this == source ? (copy ? "Copied" : "Moved") : "");
            }
            var accepted = this.accept && this.checkAcceptance(source, nodes);
            this._changeState("Target", accepted ? "" : "Disabled");
            if (this == source) {
                dojo.dnd.manager().overSource(this);
            }
            this.isDragging = true;
        },
        onDndDrop: function(source, nodes, copy, target) {
            // summary: topic event processor for /dnd/drop, called to finish the DnD operation
            // source: Object: the source which provides items
            // nodes: Array: the list of transferred items
            // copy: Boolean: copy items, if true, move items otherwise
            // target: Object: the target which accepts items
            if (this == target) {
                // this one is for us => move nodes!
                this.onDrop(source, nodes, copy);
            }
            this.onDndCancel();
        },
        onDndCancel: function() {
            // summary: topic event processor for /dnd/cancel, called to cancel the DnD operation
            if (this.targetAnchor) {
                this._unmarkTargetAnchor();
                this.targetAnchor = null;
            }
            this.before = true;
            this.isDragging = false;
            this.mouseDown = false;
            this._changeState("Source", "");
            this._changeState("Target", "");
        },

        // local events
        onDrop: function(source, nodes, copy) {
            // summary: called only on the current target, when drop is performed
            // source: Object: the source which provides items
            // nodes: Array: the list of transferred items
            // copy: Boolean: copy items, if true, move items otherwise

            if (this != source) {
                this.onDropExternal(source, nodes, copy);
            } else {
                this.onDropInternal(nodes, copy);
            }
        },
        onDropExternal: function(source, nodes, copy) {
            // summary: called only on the current target, when drop is performed
            //	from an external source
            // source: Object: the source which provides items
            // nodes: Array: the list of transferred items
            // copy: Boolean: copy items, if true, move items otherwise

            var oldCreator = this._normalizedCreator;
            // transferring nodes from the source to the target
            if (this.creator) {
                // use defined creator
                this._normalizedCreator = function(node, hint) {
                    return oldCreator.call(this, source.getItem(node.id).data, hint);
                };
            } else {
                // we have no creator defined => move/clone nodes
                if (copy) {
                    // clone nodes
                    this._normalizedCreator = function(node, hint) {
                        var t = source.getItem(node.id);
                        var n = node.cloneNode(true);
                        n.id = dojo.dnd.getUniqueId();
                        return {node: n, data: t.data, type: t.type};
                    };
                } else {
                    // move nodes
                    this._normalizedCreator = function(node, hint) {
                        var t = source.getItem(node.id);
                        source.delItem(node.id);
                        return {node: node, data: t.data, type: t.type};
                    };
                }
            }
            this.selectNone();
            if (!copy && !this.creator) {
                source.selectNone();
            }
            this.insertNodes(true, nodes, this.before, this.current);
            if (!copy && this.creator) {
                source.deleteSelectedNodes();
            }
            this._normalizedCreator = oldCreator;
        },
        onDropInternal: function(nodes, copy) {
            // summary: called only on the current target, when drop is performed
            //	from the same target/source
            // nodes: Array: the list of transferred items
            // copy: Boolean: copy items, if true, move items otherwise

            var oldCreator = this._normalizedCreator;
            // transferring nodes within the single source
            if (this.current && this.current.id in this.selection) {
                // do nothing
                return;
            }
            if (copy) {
                if (this.creator) {
                    // create new copies of data items
                    this._normalizedCreator = function(node, hint) {
                        return oldCreator.call(this, this.getItem(node.id).data, hint);
                    };
                } else {
                    // clone nodes
                    this._normalizedCreator = function(node, hint) {
                        var t = this.getItem(node.id);
                        var n = node.cloneNode(true);
                        n.id = dojo.dnd.getUniqueId();
                        return {node: n, data: t.data, type: t.type};
                    };
                }
            } else {
                // move nodes
                if (!this.current) {
                    // do nothing
                    return;
                }
                this._normalizedCreator = function(node, hint) {
                    var t = this.getItem(node.id);
                    return {node: node, data: t.data, type: t.type};
                };
            }
            this._removeSelection();
            this.insertNodes(true, nodes, this.before, this.current);
            this._normalizedCreator = oldCreator;
        },
        onDraggingOver: function() {
            // summary: called during the active DnD operation, when items
            // are dragged over this target, and it is not disabled
        },
        onDraggingOut: function() {
            // summary: called during the active DnD operation, when items
            // are dragged away from this target, and it is not disabled
        },

        // utilities
        onOverEvent: function() {
            // summary: this function is called once, when mouse is over our container
            dojo.dnd.Source.superclass.onOverEvent.call(this);
            dojo.dnd.manager().overSource(this);
            if (this.isDragging && this.targetState != "Disabled") {
                this.onDraggingOver();
            }
        },
        onOutEvent: function() {
            // summary: this function is called once, when mouse is out of our container
            dojo.dnd.Source.superclass.onOutEvent.call(this);
            dojo.dnd.manager().outSource(this);
            if (this.isDragging && this.targetState != "Disabled") {
                this.onDraggingOut();
            }
        },
        _markTargetAnchor: function(before) {
            // summary: assigns a class to the current target anchor based on "before" status
            // before: Boolean: insert before, if true, after otherwise
            if (this.current == this.targetAnchor && this.before == before) {
                return;
            }
            if (this.targetAnchor) {
                this._removeItemClass(this.targetAnchor, this.before ? "Before" : "After");
            }
            this.targetAnchor = this.current;
            this.targetBox = null;
            this.before = before;
            if (this.targetAnchor) {
                this._addItemClass(this.targetAnchor, this.before ? "Before" : "After");
            }
        },
        _unmarkTargetAnchor: function() {
            // summary: removes a class of the current target anchor based on "before" status
            if (!this.targetAnchor) {
                return;
            }
            this._removeItemClass(this.targetAnchor, this.before ? "Before" : "After");
            this.targetAnchor = null;
            this.targetBox = null;
            this.before = true;
        },
        _markDndStatus: function(copy) {
            // summary: changes source's state based on "copy" status
            this._changeState("Source", copy ? "Copied" : "Moved");
        },
        _legalMouseDown: function(e) {
            // summary: checks if user clicked on "approved" items
            // e: Event: mouse event

            // accept only the left mouse button
            if (!dojo.dnd._isLmbPressed(e)) {
                return false;
            }

            if (!this.withHandles) {
                return true;
            }

            // check for handles
            for (var node = e.target; node && node !== this.node; node = node.parentNode) {
                if (dojo.hasClass(node, "dojoDndHandle")) {
                    return true;
                }
                if (dojo.hasClass(node, "dojoDndItem")) {
                    break;
                }
            }
            return false;	// Boolean
        }
    });

    dojo.declare("dojo.dnd.Target", dojo.dnd.Source, {
        // summary: a Target object, which can be used as a DnD target

        constructor: function(node, params) {
            // summary: a constructor of the Target --- see the Source constructor for details
            this.isSource = false;
            dojo.removeClass(this.node, "dojoDndSource");
        },

        // markup methods
        markupFactory: function(params, node) {
            params._skipStartup = true;
            return new dojo.dnd.Target(node, params);
        }
    });

    dojo.declare("dojo.dnd.AutoSource", dojo.dnd.Source, {
        // summary: a source, which syncs its DnD nodes by default

        constructor: function(node, params) {
            // summary: a constructor of the AutoSource --- see the Source constructor for details
            this.autoSync = true;
        },

        // markup methods
        markupFactory: function(params, node) {
            params._skipStartup = true;
            return new dojo.dnd.AutoSource(node, params);
        }
    });

}

if (!dojo._hasResource["dijit._base.focus"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.focus"] = true;
    dojo.provide("dijit._base.focus");

    // summary:
    //		These functions are used to query or set the focus and selection.
    //
    //		Also, they trace when widgets become actived/deactivated,
    //		so that the widget can fire _onFocus/_onBlur events.
    //		"Active" here means something similar to "focused", but
    //		"focus" isn't quite the right word because we keep track of
    //		a whole stack of "active" widgets.  Example:  Combobutton --> Menu -->
    //		MenuItem.   The onBlur event for Combobutton doesn't fire due to focusing
    //		on the Menu or a MenuItem, since they are considered part of the
    //		Combobutton widget.  It only happens when focus is shifted
    //		somewhere completely different.

    dojo.mixin(dijit,
    {
        // _curFocus: DomNode
        //		Currently focused item on screen
        _curFocus: null,

        // _prevFocus: DomNode
        //		Previously focused item on screen
        _prevFocus: null,

        isCollapsed: function() {
            // summary:
            //		Returns true if there is no text selected
            var _document = dojo.doc;
            if (_document.selection) { // IE
                var s = _document.selection;
                if (s.type == 'Text') {
                    return !s.createRange().htmlText.length; // Boolean
                } else { //Control range
                    return !s.createRange().length; // Boolean
                }
            } else {
                var _window = dojo.global;
                var selection = _window.getSelection();

                if (dojo.isString(selection)) { // Safari
                    // TODO: this is dead code; safari is taking the else branch.  remove after 1.3.
                    return !selection; // Boolean
                } else { // Mozilla/W3
                    return !selection || selection.isCollapsed || !selection.toString(); // Boolean
                }
            }
        },

        getBookmark: function() {
            // summary:
            //		Retrieves a bookmark that can be used with moveToBookmark to return to the same range
            var bookmark, selection = dojo.doc.selection;
            if (selection) { // IE
                var range = selection.createRange();
                if (selection.type.toUpperCase() == 'CONTROL') {
                    if (range.length) {
                        bookmark = [];
                        var i = 0,len = range.length;
                        while (i < len) {
                            bookmark.push(range.item(i++));
                        }
                    } else {
                        bookmark = null;
                    }
                } else {
                    bookmark = range.getBookmark();
                }
            } else {
                if (window.getSelection) {
                    selection = dojo.global.getSelection();
                    if (selection) {
                        range = selection.getRangeAt(0);
                        bookmark = range.cloneRange();
                    }
                } else {
                    console.warn("No idea how to store the current selection for this browser!");
                }
            }
            return bookmark; // Array
        },

        moveToBookmark: function(/*Object*/bookmark) {
            // summary:
            //		Moves current selection to a bookmark
            // bookmark:
            //		This should be a returned object from dojo.html.selection.getBookmark()
            var _document = dojo.doc;
            if (_document.selection) { // IE
                var range;
                if (dojo.isArray(bookmark)) {
                    range = _document.body.createControlRange();
                    //range.addElement does not have call/apply method, so can not call it directly
                    //range is not available in "range.addElement(item)", so can't use that either
                    dojo.forEach(bookmark, function(n) {
                        range.addElement(n);
                    });
                } else {
                    range = _document.selection.createRange();
                    range.moveToBookmark(bookmark);
                }
                range.select();
            } else { //Moz/W3C
                var selection = dojo.global.getSelection && dojo.global.getSelection();
                if (selection && selection.removeAllRanges) {
                    selection.removeAllRanges();
                    selection.addRange(bookmark);
                } else {
                    console.warn("No idea how to restore selection for this browser!");
                }
            }
        },

        getFocus: function(/*Widget?*/menu, /*Window?*/openedForWindow) {
            // summary:
            //		Returns the current focus and selection.
            //		Called when a popup appears (either a top level menu or a dialog),
            //		or when a toolbar/menubar receives focus
            //
            // menu:
            //		The menu that's being opened
            //
            // openedForWindow:
            //		iframe in which menu was opened
            //
            // returns:
            //		A handle to restore focus/selection

            return {
                // Node to return focus to
                node: menu && dojo.isDescendant(dijit._curFocus, menu.domNode) ? dijit._prevFocus : dijit._curFocus,

                // Previously selected text
                bookmark:
                        !dojo.withGlobal(openedForWindow || dojo.global, dijit.isCollapsed) ?
                        dojo.withGlobal(openedForWindow || dojo.global, dijit.getBookmark) :
                        null,

                openedForWindow: openedForWindow
            }; // Object
        },

        focus: function(/*Object || DomNode */ handle) {
            // summary:
            //		Sets the focused node and the selection according to argument.
            //		To set focus to an iframe's content, pass in the iframe itself.
            // handle:
            //		object returned by get(), or a DomNode

            if (!handle) {
                return;
            }

            var node = "node" in handle ? handle.node : handle,        // because handle is either DomNode or a composite object
                    bookmark = handle.bookmark,
                    openedForWindow = handle.openedForWindow;

            // Set the focus
            // Note that for iframe's we need to use the <iframe> to follow the parentNode chain,
            // but we need to set focus to iframe.contentWindow
            if (node) {
                var focusNode = (node.tagName.toLowerCase() == "iframe") ? node.contentWindow : node;
                if (focusNode && focusNode.focus) {
                    try {
                        // Gecko throws sometimes if setting focus is impossible,
                        // node not displayed or something like that
                        focusNode.focus();
                    } catch(e) {/*quiet*/
                    }
                }
                dijit._onFocusNode(node);
            }

            // set the selection
            // do not need to restore if current selection is not empty
            // (use keyboard to select a menu item)
            if (bookmark && dojo.withGlobal(openedForWindow || dojo.global, dijit.isCollapsed)) {
                if (openedForWindow) {
                    openedForWindow.focus();
                }
                try {
                    dojo.withGlobal(openedForWindow || dojo.global, dijit.moveToBookmark, null, [bookmark]);
                } catch(e) {
                    /*squelch IE internal error, see http://trac.dojotoolkit.org/ticket/1984 */
                }
            }
        },

        // _activeStack: Array
        //		List of currently active widgets (focused widget and it's ancestors)
        _activeStack: [],

        registerIframe: function(/*DomNode*/ iframe) {
            // summary:
            //		Registers listeners on the specified iframe so that any click
            //		or focus event on that iframe (or anything in it) is reported
            //		as a focus/click event on the <iframe> itself.
            // description:
            //		Currently only used by editor.
            dijit.registerWin(iframe.contentWindow, iframe);
        },


        registerWin: function(/*Window?*/targetWindow, /*DomNode?*/ effectiveNode) {
            // summary:
            //		Registers listeners on the specified window (either the main
            //		window or an iframe's window) to detect when the user has clicked somewhere
            //		or focused somewhere.
            // description:
            //		Users should call registerIframe() instead of this method.
            // targetWindow:
            //		If specified this is the window associated with the iframe,
            //		i.e. iframe.contentWindow.
            // effectiveNode:
            //		If specified, report any focus events inside targetWindow as
            //		an event on effectiveNode, rather than on evt.target.

            // TODO: make this function private in 2.0; Editor/users should call registerIframe(),
            // or if Editor stops using <iframe> altogether than we can probably just drop
            // the whole public API.

            dojo.connect(targetWindow.document, "onmousedown", function(evt) {
                dijit._justMouseDowned = true;
                setTimeout(function() {
                    dijit._justMouseDowned = false;
                }, 0);
                dijit._onTouchNode(effectiveNode || evt.target || evt.srcElement);
            });
            //dojo.connect(targetWindow, "onscroll", ???);

            // Listen for blur and focus events on targetWindow's document.
            // IIRC, I'm using attachEvent() rather than dojo.connect() because focus/blur events don't bubble
            // through dojo.connect(), and also maybe to catch the focus events early, before onfocus handlers
            // fire.
            var doc = targetWindow.document;
            if (doc) {
                if (dojo.isIE) {
                    doc.attachEvent('onactivate', function(evt) {
                        if (evt.srcElement.tagName.toLowerCase() != "#document") {
                            dijit._onFocusNode(effectiveNode || evt.srcElement);
                        }
                    });
                    doc.attachEvent('ondeactivate', function(evt) {
                        dijit._onBlurNode(effectiveNode || evt.srcElement);
                    });
                } else {
                    doc.addEventListener('focus', function(evt) {
                        dijit._onFocusNode(effectiveNode || evt.target);
                    }, true);
                    doc.addEventListener('blur', function(evt) {
                        dijit._onBlurNode(effectiveNode || evt.target);
                    }, true);
                }
            }
            doc = null;	// prevent memory leak (apparent circular reference via closure)
        },

        _onBlurNode: function(/*DomNode*/ node) {
            // summary:
            // 		Called when focus leaves a node.
            //		Usually ignored, _unless_ it *isn't* follwed by touching another node,
            //		which indicates that we tabbed off the last field on the page,
            //		in which case every widget is marked inactive
            dijit._prevFocus = dijit._curFocus;
            dijit._curFocus = null;

            if (dijit._justMouseDowned) {
                // the mouse down caused a new widget to be marked as active; this blur event
                // is coming late, so ignore it.
                return;
            }

            // if the blur event isn't followed by a focus event then mark all widgets as inactive.
            if (dijit._clearActiveWidgetsTimer) {
                clearTimeout(dijit._clearActiveWidgetsTimer);
            }
            dijit._clearActiveWidgetsTimer = setTimeout(function() {
                delete dijit._clearActiveWidgetsTimer;
                dijit._setStack([]);
                dijit._prevFocus = null;
            }, 100);
        },

        _onTouchNode: function(/*DomNode*/ node) {
            // summary:
            //		Callback when node is focused or mouse-downed

            // ignore the recent blurNode event
            if (dijit._clearActiveWidgetsTimer) {
                clearTimeout(dijit._clearActiveWidgetsTimer);
                delete dijit._clearActiveWidgetsTimer;
            }

            // compute stack of active widgets (ex: ComboButton --> Menu --> MenuItem)
            var newStack = [];
            try {
                while (node) {
                    if (node.dijitPopupParent) {
                        node = dijit.byId(node.dijitPopupParent).domNode;
                    } else if (node.tagName && node.tagName.toLowerCase() == "body") {
                        // is this the root of the document or just the root of an iframe?
                        if (node === dojo.body()) {
                            // node is the root of the main document
                            break;
                        }
                        // otherwise, find the iframe this node refers to (can't access it via parentNode,
                        // need to do this trick instead). window.frameElement is supported in IE/FF/Webkit
                        node = dijit.getDocumentWindow(node.ownerDocument).frameElement;
                    } else {
                        var id = node.getAttribute && node.getAttribute("widgetId");
                        if (id) {
                            newStack.unshift(id);
                        }
                        node = node.parentNode;
                    }
                }
            } catch(e) { /* squelch */
            }

            dijit._setStack(newStack);
        },

        _onFocusNode: function(/*DomNode*/ node) {
            // summary:
            //		Callback when node is focused

            if (!node) {
                return;
            }

            if (node.nodeType == 9) {
                // Ignore focus events on the document itself.  This is here so that
                // (for example) clicking the up/down arrows of a spinner
                // (which don't get focus) won't cause that widget to blur. (FF issue)
                return;
            }

            dijit._onTouchNode(node);

            if (node == dijit._curFocus) {
                return;
            }
            if (dijit._curFocus) {
                dijit._prevFocus = dijit._curFocus;
            }
            dijit._curFocus = node;
            dojo.publish("focusNode", [node]);
        },

        _setStack: function(newStack) {
            // summary:
            //		The stack of active widgets has changed.  Send out appropriate events and records new stack.

            var oldStack = dijit._activeStack;
            dijit._activeStack = newStack;

            // compare old stack to new stack to see how many elements they have in common
            for (var nCommon = 0; nCommon < Math.min(oldStack.length, newStack.length); nCommon++) {
                if (oldStack[nCommon] != newStack[nCommon]) {
                    break;
                }
            }

            // for all elements that have gone out of focus, send blur event
            for (var i = oldStack.length - 1; i >= nCommon; i--) {
                var widget = dijit.byId(oldStack[i]);
                if (widget) {
                    widget._focused = false;
                    widget._hasBeenBlurred = true;
                    if (widget._onBlur) {
                        widget._onBlur();
                    }
                    if (widget._setStateClass) {
                        widget._setStateClass();
                    }
                    dojo.publish("widgetBlur", [widget]);
                }
            }

            // for all element that have come into focus, send focus event
            for (i = nCommon; i < newStack.length; i++) {
                widget = dijit.byId(newStack[i]);
                if (widget) {
                    widget._focused = true;
                    if (widget._onFocus) {
                        widget._onFocus();
                    }
                    if (widget._setStateClass) {
                        widget._setStateClass();
                    }
                    dojo.publish("widgetFocus", [widget]);
                }
            }
        }
    });

    // register top window and all the iframes it contains
    dojo.addOnLoad(function() {
        dijit.registerWin(window);
    });

}

if (!dojo._hasResource["dijit._base.manager"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.manager"] = true;
    dojo.provide("dijit._base.manager");

    dojo.declare("dijit.WidgetSet", null, {
        // summary:
        //		A set of widgets indexed by id. A default instance of this class is
        //		available as `dijit.registry`
        //
        // example:
        //		Create a small list of widgets:
        //		|	var ws = new dijit.WidgetSet();
        //		|	ws.add(dijit.byId("one"));
        //		| 	ws.add(dijit.byId("two"));
        //		|	// destroy both:
        //		|	ws.forEach(function(w){ w.destroy(); });
        //
        // example:
        //		Using dijit.registry:
        //		|	dijit.registry.forEach(function(w){ /* do something */ });

        constructor: function() {
            this._hash = {};
        },

        add: function(/*Widget*/ widget) {
            // summary:
            //		Add a widget to this list. If a duplicate ID is detected, a error is thrown.
            //
            // widget: dijit._Widget
            //		Any dijit._Widget subclass.
            if (this._hash[widget.id]) {
                throw new Error("Tried to register widget with id==" + widget.id + " but that id is already registered");
            }
            this._hash[widget.id] = widget;
        },

        remove: function(/*String*/ id) {
            // summary:
            //		Remove a widget from this WidgetSet. Does not destroy the widget; simply
            //		removes the reference.
            delete this._hash[id];
        },

        forEach: function(/*Function*/ func) {
            // summary:
            //		Call specified function for each widget in this set.
            //
            // func:
            //		A callback function to run for each item. Is passed a the widget.
            //
            // example:
            //		Using the default `dijit.registry` instance:
            //		|	dijit.registry.forEach(function(widget){
            //		|		console.log(widget.declaredClass);
            //		|	});
            for (var id in this._hash) {
                func(this._hash[id]);
            }
        },

        filter: function(/*Function*/ filter) {
            // summary:
            //		Filter down this WidgetSet to a smaller new WidgetSet
            //		Works the same as `dojo.filter` and `dojo.NodeList.filter`
            //
            // filter:
            //		Callback function to test truthiness.
            //
            // example:
            //		Arbitrary: select the odd widgets in this list
            //		|	var i = 0;
            //		|	dijit.registry.filter(function(w){
            //		|		return ++i % 2 == 0;
            //		|	}).forEach(function(w){ /* odd ones */ });

            var res = new dijit.WidgetSet();
            this.forEach(function(widget) {
                if (filter(widget)) {
                    res.add(widget);
                }
            });
            return res; // dijit.WidgetSet
        },

        byId: function(/*String*/ id) {
            // summary:
            //		Find a widget in this list by it's id.
            // example:
            //		Test if an id is in a particular WidgetSet
            //		| var ws = new dijit.WidgetSet();
            //		| ws.add(dijit.byId("bar"));
            //		| var t = ws.byId("bar") // returns a widget
            //		| var x = ws.byId("foo"); // returns undefined

            return this._hash[id];	// dijit._Widget
        },

        byClass: function(/*String*/ cls) {
            // summary:
            //		Reduce this widgetset to a new WidgetSet of a particular declaredClass
            //
            // example:
            //		Find all titlePane's in a page:
            //		|	dijit.registry.byClass("dijit.TitlePane").forEach(function(tp){ tp.close(); });

            return this.filter(function(widget) {
                return widget.declaredClass == cls;
            });	// dijit.WidgetSet
        }

    });

    /*=====
     dijit.registry = {
     // summary: A list of widgets on a page.
     // description: Is an instance of `dijit.WidgetSet`
     };
     =====*/
    dijit.registry = new dijit.WidgetSet();

    dijit._widgetTypeCtr = {};

    dijit.getUniqueId = function(/*String*/widgetType) {
        // summary: Generates a unique id for a given widgetType

        var id;
        do{
            id = widgetType + "_" +
                 (widgetType in dijit._widgetTypeCtr ?
                  ++dijit._widgetTypeCtr[widgetType] : dijit._widgetTypeCtr[widgetType] = 0);
        } while (dijit.byId(id));
        return id; // String
    };

    dijit.findWidgets = function(/*DomNode*/ root) {
        // summary:
        //		Search subtree under root, putting found widgets in outAry.
        //		Doesn't search for nested widgets (ie, widgets inside other widgets)

        var outAry = [];

        function getChildrenHelper(root) {
            var list = dojo.isIE ? root.children : root.childNodes, i = 0, node;
            while (node = list[i++]) {
                if (node.nodeType != 1) {
                    continue;
                }
                var widgetId = node.getAttribute("widgetId");
                if (widgetId) {
                    var widget = dijit.byId(widgetId);
                    outAry.push(widget);
                } else {
                    getChildrenHelper(node);
                }
            }
        }

        getChildrenHelper(root);
        return outAry;
    };

    if (dojo.isIE) {
        // Only run this for IE because we think it's only necessary in that case,
        // and because it causes problems on FF.  See bug #3531 for details.
        dojo.addOnWindowUnload(function() {
            dojo.forEach(dijit.findWidgets(dojo.body()), function(widget) {
                if (widget.destroyRecursive) {
                    widget.destroyRecursive();
                } else if (widget.destroy) {
                    widget.destroy();
                }
            });
        });
    }

    dijit.byId = function(/*String|Widget*/id) {
        // summary:
        //		Returns a widget by it's id, or if passed a widget, no-op (like dojo.byId())
        return (dojo.isString(id)) ? dijit.registry.byId(id) : id; // Widget
    };

    dijit.byNode = function(/* DOMNode */ node) {
        // summary:
        //		Returns the widget corresponding to the given DOMNode
        return dijit.registry.byId(node.getAttribute("widgetId")); // Widget
    };

    dijit.getEnclosingWidget = function(/* DOMNode */ node) {
        // summary:
        //		Returns the widget whose DOM tree contains the specified DOMNode, or null if
        //		the node is not contained within the DOM tree of any widget
        while (node) {
            if (node.getAttribute && node.getAttribute("widgetId")) {
                return dijit.registry.byId(node.getAttribute("widgetId"));
            }
            node = node.parentNode;
        }
        return null;
    };

    // elements that are tab-navigable if they have no tabindex value set
    // (except for "a", which must have an href attribute)
    dijit._tabElements = {
        area: true,
        button: true,
        input: true,
        object: true,
        select: true,
        textarea: true
    };

    dijit._isElementShown = function(/*Element*/elem) {
        var style = dojo.style(elem);
        return (style.visibility != "hidden")
                && (style.visibility != "collapsed")
                && (style.display != "none")
                && (dojo.attr(elem, "type") != "hidden");
    }

    dijit.isTabNavigable = function(/*Element*/elem) {
        // summary:
        //		Tests if an element is tab-navigable
        if (dojo.hasAttr(elem, "disabled")) {
            return false;
        }
        var hasTabindex = dojo.hasAttr(elem, "tabindex");
        var tabindex = dojo.attr(elem, "tabindex");
        if (hasTabindex && tabindex >= 0) {
            return true; // boolean
        }
        var name = elem.nodeName.toLowerCase();
        if (((name == "a" && dojo.hasAttr(elem, "href"))
                || dijit._tabElements[name])
                && (!hasTabindex || tabindex >= 0)) {
            return true; // boolean
        }
        return false; // boolean
    };

    dijit._getTabNavigable = function(/*DOMNode*/root) {
        // summary:
        //		Finds descendants of the specified root node.
        //
        // description:
        //		Finds the following descendants of the specified root node:
        //		* the first tab-navigable element in document order
        //		  without a tabindex or with tabindex="0"
        //		* the last tab-navigable element in document order
        //		  without a tabindex or with tabindex="0"
        //		* the first element in document order with the lowest
        //		  positive tabindex value
        //		* the last element in document order with the highest
        //		  positive tabindex value
        var first, last, lowest, lowestTabindex, highest, highestTabindex;
        var walkTree = function(/*DOMNode*/parent) {
            dojo.query("> *", parent).forEach(function(child) {
                var isShown = dijit._isElementShown(child);
                if (isShown && dijit.isTabNavigable(child)) {
                    var tabindex = dojo.attr(child, "tabindex");
                    if (!dojo.hasAttr(child, "tabindex") || tabindex == 0) {
                        if (!first) {
                            first = child;
                        }
                        last = child;
                    } else if (tabindex > 0) {
                        if (!lowest || tabindex < lowestTabindex) {
                            lowestTabindex = tabindex;
                            lowest = child;
                        }
                        if (!highest || tabindex >= highestTabindex) {
                            highestTabindex = tabindex;
                            highest = child;
                        }
                    }
                }
                if (isShown && child.nodeName.toUpperCase() != 'SELECT') {
                    walkTree(child)
                }
            });
        };
        if (dijit._isElementShown(root)) {
            walkTree(root)
        }
        return { first: first, last: last, lowest: lowest, highest: highest };
    }
    dijit.getFirstInTabbingOrder = function(/*String|DOMNode*/root) {
        // summary:
        //		Finds the descendant of the specified root node
        //		that is first in the tabbing order
        var elems = dijit._getTabNavigable(dojo.byId(root));
        return elems.lowest ? elems.lowest : elems.first; // DomNode
    };

    dijit.getLastInTabbingOrder = function(/*String|DOMNode*/root) {
        // summary:
        //		Finds the descendant of the specified root node
        //		that is last in the tabbing order
        var elems = dijit._getTabNavigable(dojo.byId(root));
        return elems.last ? elems.last : elems.highest; // DomNode
    };

    /*=====
     dojo.mixin(dijit, {
     // defaultDuration: Integer
     //		The default animation speed (in ms) to use for all Dijit
     //		transitional animations, unless otherwise specified
     //		on a per-instance basis. Defaults to 200, overrided by
     //		`djConfig.defaultDuration`
     defaultDuration: 300
     });
     =====*/

    dijit.defaultDuration = dojo.config["defaultDuration"] || 200;

}

if (!dojo._hasResource["dojo.AdapterRegistry"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.AdapterRegistry"] = true;
    dojo.provide("dojo.AdapterRegistry");

    dojo.AdapterRegistry = function(/*Boolean?*/ returnWrappers) {
        //	summary:
        //		A registry to make contextual calling/searching easier.
        //	description:
        //		Objects of this class keep list of arrays in the form [name, check,
        //		wrap, directReturn] that are used to determine what the contextual
        //		result of a set of checked arguments is. All check/wrap functions
        //		in this registry should be of the same arity.
        //	example:
        //	|	// create a new registry
        //	|	var reg = new dojo.AdapterRegistry();
        //	|	reg.register("handleString",
        //	|		dojo.isString,
        //	|		function(str){
        //	|			// do something with the string here
        //	|		}
        //	|	);
        //	|	reg.register("handleArr",
        //	|		dojo.isArray,
        //	|		function(arr){
        //	|			// do something with the array here
        //	|		}
        //	|	);
        //	|
        //	|	// now we can pass reg.match() *either* an array or a string and
        //	|	// the value we pass will get handled by the right function
        //	|	reg.match("someValue"); // will call the first function
        //	|	reg.match(["someValue"]); // will call the second

        this.pairs = [];
        this.returnWrappers = returnWrappers || false; // Boolean
    }

    dojo.extend(dojo.AdapterRegistry, {
        register: function(/*String*/ name, /*Function*/ check, /*Function*/ wrap, /*Boolean?*/ directReturn, /*Boolean?*/ override) {
            //	summary:
            //		register a check function to determine if the wrap function or
            //		object gets selected
            //	name:
            //		a way to identify this matcher.
            //	check:
            //		a function that arguments are passed to from the adapter's
            //		match() function.  The check function should return true if the
            //		given arguments are appropriate for the wrap function.
            //	directReturn:
            //		If directReturn is true, the value passed in for wrap will be
            //		returned instead of being called. Alternately, the
            //		AdapterRegistry can be set globally to "return not call" using
            //		the returnWrappers property. Either way, this behavior allows
            //		the registry to act as a "search" function instead of a
            //		function interception library.
            //	override:
            //		If override is given and true, the check function will be given
            //		highest priority. Otherwise, it will be the lowest priority
            //		adapter.
            this.pairs[((override) ? "unshift" : "push")]([name, check, wrap, directReturn]);
        },

        match: function(/* ... */) {
            // summary:
            //		Find an adapter for the given arguments. If no suitable adapter
            //		is found, throws an exception. match() accepts any number of
            //		arguments, all of which are passed to all matching functions
            //		from the registered pairs.
            for (var i = 0; i < this.pairs.length; i++) {
                var pair = this.pairs[i];
                if (pair[1].apply(this, arguments)) {
                    if ((pair[3]) || (this.returnWrappers)) {
                        return pair[2];
                    } else {
                        return pair[2].apply(this, arguments);
                    }
                }
            }
            throw new Error("No match found");
        },

        unregister: function(name) {
            // summary: Remove a named adapter from the registry

            // FIXME: this is kind of a dumb way to handle this. On a large
            // registry this will be slow-ish and we can use the name as a lookup
            // should we choose to trade memory for speed.
            for (var i = 0; i < this.pairs.length; i++) {
                var pair = this.pairs[i];
                if (pair[0] == name) {
                    this.pairs.splice(i, 1);
                    return true;
                }
            }
            return false;
        }
    });

}

if (!dojo._hasResource["dijit._base.place"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.place"] = true;
    dojo.provide("dijit._base.place");


    // ported from dojo.html.util

    dijit.getViewport = function() {
        // summary:
        //		Returns the dimensions and scroll position of the viewable area of a browser window

        var scrollRoot = (dojo.doc.compatMode == 'BackCompat') ? dojo.body() : dojo.doc.documentElement;

        // get scroll position
        var scroll = dojo._docScroll(); // scrollRoot.scrollTop/Left should work
        return { w: scrollRoot.clientWidth, h: scrollRoot.clientHeight, l: scroll.x, t: scroll.y };
    };

    /*=====
     dijit.__Position = function(){
     // x: Integer
     //		horizontal coordinate in pixels, relative to document body
     // y: Integer
     //		vertical coordinate in pixels, relative to document body

     thix.x = x;
     this.y = y;
     }
     =====*/


    dijit.placeOnScreen = function(
        /* DomNode */            node,
        /* dijit.__Position */    pos,
        /* String[] */            corners,
        /* dijit.__Position? */    padding) {
        //	summary:
        //		Positions one of the node's corners at specified position
        //		such that node is fully visible in viewport.
        //	description:
        //		NOTE: node is assumed to be absolutely or relatively positioned.
        //	pos:
        //		Object like {x: 10, y: 20}
        //	corners:
        //		Array of Strings representing order to try corners in, like ["TR", "BL"].
        //		Possible values are:
        //			* "BL" - bottom left
        //			* "BR" - bottom right
        //			* "TL" - top left
        //			* "TR" - top right
        //	padding:
        //		set padding to put some buffer around the element you want to position.
        //	example:
        //		Try to place node's top right corner at (10,20).
        //		If that makes node go (partially) off screen, then try placing
        //		bottom left corner at (10,20).
        //	|	placeOnScreen(node, {x: 10, y: 20}, ["TR", "BL"])

        var choices = dojo.map(corners, function(corner) {
            var c = { corner: corner, pos: {x:pos.x,y:pos.y} };
            if (padding) {
                c.pos.x += corner.charAt(1) == 'L' ? padding.x : -padding.x;
                c.pos.y += corner.charAt(0) == 'T' ? padding.y : -padding.y;
            }
            return c;
        });

        return dijit._place(node, choices);
    }

    dijit._place = function(/*DomNode*/ node, /* Array */ choices, /* Function */ layoutNode) {
        // summary:
        //		Given a list of spots to put node, put it at the first spot where it fits,
        //		of if it doesn't fit anywhere then the place with the least overflow
        // choices: Array
        //		Array of elements like: {corner: 'TL', pos: {x: 10, y: 20} }
        //		Above example says to put the top-left corner of the node at (10,20)
        // layoutNode: Function(node, aroundNodeCorner, nodeCorner)
        //		for things like tooltip, they are displayed differently (and have different dimensions)
        //		based on their orientation relative to the parent.   This adjusts the popup based on orientation.

        // get {x: 10, y: 10, w: 100, h:100} type obj representing position of
        // viewport over document
        var view = dijit.getViewport();

        // This won't work if the node is inside a <div style="position: relative">,
        // so reattach it to dojo.doc.body.   (Otherwise, the positioning will be wrong
        // and also it might get cutoff)
        if (!node.parentNode || String(node.parentNode.tagName).toLowerCase() != "body") {
            dojo.body().appendChild(node);
        }

        var best = null;
        dojo.some(choices, function(choice) {
            var corner = choice.corner;
            var pos = choice.pos;

            // configure node to be displayed in given position relative to button
            // (need to do this in order to get an accurate size for the node, because
            // a tooltips size changes based on position, due to triangle)
            if (layoutNode) {
                layoutNode(node, choice.aroundCorner, corner);
            }

            // get node's size
            var style = node.style;
            var oldDisplay = style.display;
            var oldVis = style.visibility;
            style.visibility = "hidden";
            style.display = "";
            var mb = dojo.marginBox(node);
            style.display = oldDisplay;
            style.visibility = oldVis;

            // coordinates and size of node with specified corner placed at pos,
            // and clipped by viewport
            var startX = (corner.charAt(1) == 'L' ? pos.x : Math.max(view.l, pos.x - mb.w)),
                    startY = (corner.charAt(0) == 'T' ? pos.y : Math.max(view.t, pos.y - mb.h)),
                    endX = (corner.charAt(1) == 'L' ? Math.min(view.l + view.w, startX + mb.w) : pos.x),
                    endY = (corner.charAt(0) == 'T' ? Math.min(view.t + view.h, startY + mb.h) : pos.y),
                    width = endX - startX,
                    height = endY - startY,
                    overflow = (mb.w - width) + (mb.h - height);

            if (best == null || overflow < best.overflow) {
                best = {
                    corner: corner,
                    aroundCorner: choice.aroundCorner,
                    x: startX,
                    y: startY,
                    w: width,
                    h: height,
                    overflow: overflow
                };
            }
            return !overflow;
        });

        node.style.left = best.x + "px";
        node.style.top = best.y + "px";
        if (best.overflow && layoutNode) {
            layoutNode(node, best.aroundCorner, best.corner);
        }
        return best;
    }

    dijit.placeOnScreenAroundNode = function(
        /* DomNode */        node,
        /* DomNode */        aroundNode,
        /* Object */        aroundCorners,
        /* Function? */        layoutNode) {

        // summary:
        //		Position node adjacent or kitty-corner to aroundNode
        //		such that it's fully visible in viewport.
        //
        // description:
        //		Place node such that corner of node touches a corner of
        //		aroundNode, and that node is fully visible.
        //
        // aroundCorners:
        //		Ordered list of pairs of corners to try matching up.
        //		Each pair of corners is represented as a key/value in the hash,
        //		where the key corresponds to the aroundNode's corner, and
        //		the value corresponds to the node's corner:
        //
        //	|	{ aroundNodeCorner1: nodeCorner1, aroundNodeCorner2: nodeCorner2,  ...}
        //
        //		The following strings are used to represent the four corners:
        //			* "BL" - bottom left
        //			* "BR" - bottom right
        //			* "TL" - top left
        //			* "TR" - top right
        //
        // layoutNode: Function(node, aroundNodeCorner, nodeCorner)
        //		For things like tooltip, they are displayed differently (and have different dimensions)
        //		based on their orientation relative to the parent.   This adjusts the popup based on orientation.
        //
        // example:
        //	|	dijit.placeOnScreenAroundNode(node, aroundNode, {'BL':'TL', 'TR':'BR'});
        //		This will try to position node such that node's top-left corner is at the same position
        //		as the bottom left corner of the aroundNode (ie, put node below
        //		aroundNode, with left edges aligned).  If that fails it will try to put
        // 		the bottom-right corner of node where the top right corner of aroundNode is
        //		(ie, put node above aroundNode, with right edges aligned)
        //

        // get coordinates of aroundNode
        aroundNode = dojo.byId(aroundNode);
        var oldDisplay = aroundNode.style.display;
        aroundNode.style.display = "";
        // #3172: use the slightly tighter border box instead of marginBox
        var aroundNodeW = aroundNode.offsetWidth; //mb.w;
        var aroundNodeH = aroundNode.offsetHeight; //mb.h;
        var aroundNodePos = dojo.coords(aroundNode, true);
        aroundNode.style.display = oldDisplay;

        // place the node around the calculated rectangle
        return dijit._placeOnScreenAroundRect(node,
                aroundNodePos.x, aroundNodePos.y, aroundNodeW, aroundNodeH, // rectangle
                aroundCorners, layoutNode);
    };

    /*=====
     dijit.__Rectangle = function(){
     // x: Integer
     //		horizontal offset in pixels, relative to document body
     // y: Integer
     //		vertical offset in pixels, relative to document body
     // width: Integer
     //		width in pixels
     // height: Integer
     //		height in pixels

     thix.x = x;
     this.y = y;
     thix.width = width;
     this.height = height;
     }
     =====*/


    dijit.placeOnScreenAroundRectangle = function(
        /* DomNode */            node,
        /* dijit.__Rectangle */    aroundRect,
        /* Object */            aroundCorners,
        /* Function */            layoutNode) {

        // summary:
        //		Like dijit.placeOnScreenAroundNode(), except that the "around"
        //		parameter is an arbitrary rectangle on the screen (x, y, width, height)
        //		instead of a dom node.

        return dijit._placeOnScreenAroundRect(node,
                aroundRect.x, aroundRect.y, aroundRect.width, aroundRect.height, // rectangle
                aroundCorners, layoutNode);
    };

    dijit._placeOnScreenAroundRect = function(
        /* DomNode */        node,
        /* Number */        x,
        /* Number */        y,
        /* Number */        width,
        /* Number */        height,
        /* Object */        aroundCorners,
        /* Function */        layoutNode) {

        // summary:
        //		Like dijit.placeOnScreenAroundNode(), except it accepts coordinates
        //		of a rectangle to place node adjacent to.

        // TODO: combine with placeOnScreenAroundRectangle()

        // Generate list of possible positions for node
        var choices = [];
        for (var nodeCorner in aroundCorners) {
            choices.push({
                aroundCorner: nodeCorner,
                corner: aroundCorners[nodeCorner],
                pos: {
                    x: x + (nodeCorner.charAt(1) == 'L' ? 0 : width),
                    y: y + (nodeCorner.charAt(0) == 'T' ? 0 : height)
                }
            });
        }

        return dijit._place(node, choices, layoutNode);
    };

    dijit.placementRegistry = new dojo.AdapterRegistry();
    dijit.placementRegistry.register("node",
            function(n, x) {
                return typeof x == "object" &&
                       typeof x.offsetWidth != "undefined" && typeof x.offsetHeight != "undefined";
            },
            dijit.placeOnScreenAroundNode);
    dijit.placementRegistry.register("rect",
            function(n, x) {
                return typeof x == "object" &&
                       "x" in x && "y" in x && "width" in x && "height" in x;
            },
            dijit.placeOnScreenAroundRectangle);

    dijit.placeOnScreenAroundElement = function(
        /* DomNode */        node,
        /* Object */        aroundElement,
        /* Object */        aroundCorners,
        /* Function */        layoutNode) {

        // summary:
        //		Like dijit.placeOnScreenAroundNode(), except it accepts an arbitrary object
        //		for the "around" argument and finds a proper processor to place a node.

        return dijit.placementRegistry.match.apply(dijit.placementRegistry, arguments);
    };

}

if (!dojo._hasResource["dijit._base.window"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.window"] = true;
    dojo.provide("dijit._base.window");

    // TODO: remove this in 2.0, it's not used anymore, or at least not internally

    dijit.getDocumentWindow = function(doc) {
        // summary:
        // 		Get window object associated with document doc

        // In some IE versions (at least 6.0), document.parentWindow does not return a
        // reference to the real window object (maybe a copy), so we must fix it as well
        // We use IE specific execScript to attach the real window reference to
        // document._parentWindow for later use
        if (dojo.isIE && window !== document.parentWindow && !doc._parentWindow) {
            /*
             In IE 6, only the variable "window" can be used to connect events (others
             may be only copies).
             */
            doc.parentWindow.execScript("document._parentWindow = window;", "Javascript");
            //to prevent memory leak, unset it after use
            //another possibility is to add an onUnload handler which seems overkill to me (liucougar)
            var win = doc._parentWindow;
            doc._parentWindow = null;
            return win;	//	Window
        }

        return doc._parentWindow || doc.parentWindow || doc.defaultView;	//	Window
    }

}

if (!dojo._hasResource["dijit._base.popup"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.popup"] = true;
    dojo.provide("dijit._base.popup");


    dijit.popup = new function() {
        // summary:
        //		This class is used to show/hide widgets as popups.

        var stack = [],
                beginZIndex = 1000,
                idGen = 1;

        this.prepare = function(/*DomNode*/ node) {
            // summary:
            //		Prepares a node to be used as a popup
            //
            // description:
            //		Attaches node to dojo.doc.body, and
            //		positions it off screen, but not display:none, so that
            //		the widget doesn't appear in the page flow and/or cause a blank
            //		area at the bottom of the viewport (making scrollbar longer), but
            //		initialization of contained widgets works correctly

            var s = node.style;
            s.visibility = "hidden";	// so TAB key doesn't navigate to hidden popup
            s.position = "absolute";
            s.top = "-9999px";
            if (s.display == "none") {
                s.display = "";
            }
            dojo.body().appendChild(node);
        };

        /*=====
         dijit.popup.__OpenArgs = function(){
         // popup: Widget
         //		widget to display
         // parent: Widget
         //		the button etc. that is displaying this popup
         // around: DomNode
         //		DOM node (typically a button); place popup relative to this node.  (Specify this *or* "x" and "y" parameters.)
         // x: Integer
         //		Absolute horizontal position (in pixels) to place node at.  (Specify this *or* "around" parameter.)
         // y: Integer
         //		Absolute vertical position (in pixels) to place node at.  (Specity this *or* "around" parameter.)
         // orient: Object || String
         //		When the around parameter is specified, orient should be an
         //		ordered list of tuples of the form (around-node-corner, popup-node-corner).
         //		dijit.popup.open() tries to position the popup according to each tuple in the list, in order,
         //		until the popup appears fully within the viewport.
         //
         //		The default value is {BL:'TL', TL:'BL'}, which represents a list of two tuples:
         //			1. (BL, TL)
         //			2. (TL, BL)
         //		where BL means "bottom left" and "TL" means "top left".
         //		So by default, it first tries putting the popup below the around node, left-aligning them,
         //		and then tries to put it above the around node, still left-aligning them.   Note that the
         //		default is horizontally reversed when in RTL mode.
         //
         //		When an (x,y) position is specified rather than an around node, orient is either
         //		"R" or "L".  R (for right) means that it tries to put the popup to the right of the mouse,
         //		specifically positioning the popup's top-right corner at the mouse position, and if that doesn't
         //		fit in the viewport, then it tries, in order, the bottom-right corner, the top left corner,
         //		and the top-right corner.
         // onCancel: Function
         //		callback when user has canceled the popup by
         //			1. hitting ESC or
         //			2. by using the popup widget's proprietary cancel mechanism (like a cancel button in a dialog);
         //			   i.e. whenever popupWidget.onCancel() is called, args.onCancel is called
         // onClose: Function
         //		callback whenever this popup is closed
         // onExecute: Function
         //		callback when user "executed" on the popup/sub-popup by selecting a menu choice, etc. (top menu only)
         // padding: dijit.__Position
         //		adding a buffer around the opening position. This is only useful when around is not set.
         this.popup = popup;
         this.parent = parent;
         this.around = around;
         this.x = x;
         this.y = y;
         this.orient = orient;
         this.onCancel = onCancel;
         this.onClose = onClose;
         this.onExecute = onExecute;
         this.padding = padding;
         }
         =====*/
        this.open = function(/*dijit.popup.__OpenArgs*/ args) {
            // summary:
            //		Popup the widget at the specified position
            //
            // example:
            //		opening at the mouse position
            //		|		dijit.popup.open({popup: menuWidget, x: evt.pageX, y: evt.pageY});
            //
            // example:
            //		opening the widget as a dropdown
            //		|		dijit.popup.open({parent: this, popup: menuWidget, around: this.domNode, onClose: function(){...}  });
            //
            //		Note that whatever widget called dijit.popup.open() should also listen to its own _onBlur callback
            //		(fired from _base/focus.js) to know that focus has moved somewhere else and thus the popup should be closed.

            var widget = args.popup,
                    orient = args.orient || {'BL':'TL', 'TL':'BL'},
                    around = args.around,
                    id = (args.around && args.around.id) ? (args.around.id + "_dropdown") : ("popup_" + idGen++);

            // make wrapper div to hold widget and possibly hold iframe behind it.
            // we can't attach the iframe as a child of the widget.domNode because
            // widget.domNode might be a <table>, <ul>, etc.
            var wrapper = dojo.create("div", {
                id: id,
                "class":"dijitPopup",
                style:{
                    zIndex: beginZIndex + stack.length,
                    visibility:"hidden"
                }
            }, dojo.body());
            dijit.setWaiRole(wrapper, "presentation");

            // prevent transient scrollbar causing misalign (#5776)
            wrapper.style.left = wrapper.style.top = "0px";

            if (args.parent) {
                wrapper.dijitPopupParent = args.parent.id;
            }

            var s = widget.domNode.style;
            s.display = "";
            s.visibility = "";
            s.position = "";
            s.top = "0px";
            wrapper.appendChild(widget.domNode);

            var iframe = new dijit.BackgroundIframe(wrapper);

            // position the wrapper node
            var best = around ?
                       dijit.placeOnScreenAroundElement(wrapper, around, orient, widget.orient ? dojo.hitch(widget, "orient") : null) :
                       dijit.placeOnScreen(wrapper, args, orient == 'R' ? ['TR','BR','TL','BL'] : ['TL','BL','TR','BR'], args.padding);

            wrapper.style.visibility = "visible";
            // TODO: use effects to fade in wrapper

            var handlers = [];

            // Compute the closest ancestor popup that's *not* a child of another popup.
            // Ex: For a TooltipDialog with a button that spawns a tree of menus, find the popup of the button.
            var getTopPopup = function() {
                for (var pi = stack.length - 1; pi > 0 && stack[pi].parent === stack[pi - 1].widget; pi--) {
                    /* do nothing, just trying to get right value for pi */
                }
                return stack[pi];
            }

            // provide default escape and tab key handling
            // (this will work for any widget, not just menu)
            handlers.push(dojo.connect(wrapper, "onkeypress", this, function(evt) {
                if (evt.charOrCode == dojo.keys.ESCAPE && args.onCancel) {
                    dojo.stopEvent(evt);
                    args.onCancel();
                } else if (evt.charOrCode === dojo.keys.TAB) {
                    dojo.stopEvent(evt);
                    var topPopup = getTopPopup();
                    if (topPopup && topPopup.onCancel) {
                        topPopup.onCancel();
                    }
                }
            }));

            // watch for cancel/execute events on the popup and notify the caller
            // (for a menu, "execute" means clicking an item)
            if (widget.onCancel) {
                handlers.push(dojo.connect(widget, "onCancel", null, args.onCancel));
            }

            handlers.push(dojo.connect(widget, widget.onExecute ? "onExecute" : "onChange", null, function() {
                var topPopup = getTopPopup();
                if (topPopup && topPopup.onExecute) {
                    topPopup.onExecute();
                }
            }));

            stack.push({
                wrapper: wrapper,
                iframe: iframe,
                widget: widget,
                parent: args.parent,
                onExecute: args.onExecute,
                onCancel: args.onCancel,
                onClose: args.onClose,
                handlers: handlers
            });

            if (widget.onOpen) {
                widget.onOpen(best);
            }

            return best;
        };

        this.close = function(/*Widget*/ popup) {
            // summary:
            //		Close specified popup and any popups that it parented
            while (dojo.some(stack, function(elem) {
                return elem.widget == popup;
            })) {
                var top = stack.pop(),
                        wrapper = top.wrapper,
                        iframe = top.iframe,
                        widget = top.widget,
                        onClose = top.onClose;

                if (widget.onClose) {
                    widget.onClose();
                }
                dojo.forEach(top.handlers, dojo.disconnect);

                // #2685: check if the widget still has a domNode so ContentPane can change its URL without getting an error
                if (!widget || !widget.domNode) {
                    return;
                }

                this.prepare(widget.domNode);

                iframe.destroy();
                dojo.destroy(wrapper);

                if (onClose) {
                    onClose();
                }
            }
        };
    }();

    dijit._frames = new function() {
        // summary: cache of iframes
        var queue = [];

        this.pop = function() {
            var iframe;
            if (queue.length) {
                iframe = queue.pop();
                iframe.style.display = "";
            } else {
                if (dojo.isIE) {
                    var burl = dojo.config["dojoBlankHtmlUrl"] || (dojo.moduleUrl("dojo", "resources/blank.html") + "") || "javascript:\"\"";
                    var html = "<iframe src='" + burl + "'"
                            + " style='position: absolute; left: 0px; top: 0px;"
                            + "z-index: -1; filter:Alpha(Opacity=\"0\");'>";
                    iframe = dojo.doc.createElement(html);
                } else {
                    iframe = dojo.create("iframe");
                    iframe.src = 'javascript:""';
                    iframe.className = "dijitBackgroundIframe";
                }
                iframe.tabIndex = -1; // Magic to prevent iframe from getting focus on tab keypress - as style didnt work.
                dojo.body().appendChild(iframe);
            }
            return iframe;
        };

        this.push = function(iframe) {
            iframe.style.display = "none";
            if (dojo.isIE) {
                iframe.style.removeExpression("width");
                iframe.style.removeExpression("height");
            }
            queue.push(iframe);
        }
    }();


    dijit.BackgroundIframe = function(/* DomNode */node) {
        // summary:
        //		For IE z-index schenanigans. id attribute is required.
        //
        // description:
        //		new dijit.BackgroundIframe(node)
        //			Makes a background iframe as a child of node, that fills
        //			area (and position) of node

        if (!node.id) {
            throw new Error("no id");
        }
        if (dojo.isIE < 7 || (dojo.isFF < 3 && dojo.hasClass(dojo.body(), "dijit_a11y"))) {
            var iframe = dijit._frames.pop();
            node.appendChild(iframe);
            if (dojo.isIE) {
                iframe.style.setExpression("width", dojo._scopeName + ".doc.getElementById('" + node.id + "').offsetWidth");
                iframe.style.setExpression("height", dojo._scopeName + ".doc.getElementById('" + node.id + "').offsetHeight");
            }
            this.iframe = iframe;
        }
    };

    dojo.extend(dijit.BackgroundIframe, {
        destroy: function() {
            //	summary: destroy the iframe
            if (this.iframe) {
                dijit._frames.push(this.iframe);
                delete this.iframe;
            }
        }
    });

}

if (!dojo._hasResource["dijit._base.scroll"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.scroll"] = true;
    dojo.provide("dijit._base.scroll");

    dijit.scrollIntoView = function(/* DomNode */node) {
        // summary:
        //		Scroll the passed node into view, if it is not.

        // don't rely on that node.scrollIntoView works just because the function is there
        // it doesnt work in Konqueror or Opera even though the function is there and probably
        //	not safari either
        // native scrollIntoView() causes FF3's whole window to scroll if there is no scroll bar
        //	on the immediate parent
        // dont like browser sniffs implementations but sometimes you have to use it
        // It's not enough just to scroll the menu node into view if
        // node.scrollIntoView hides part of the parent's scrollbar,
        // so just manage the parent scrollbar ourselves

        //var testdir="H"; //debug
        try { // catch unexpected/unrecreatable errors (#7808) since we can recover using a semi-acceptable native method
            node = dojo.byId(node);
            var doc = dojo.doc;
            var body = dojo.body();
            var html = body.parentNode;
            // if FF2 (which is perfect) or an untested browser, then use the native method

            if ((!(dojo.isFF >= 3 || dojo.isIE || dojo.isWebKit) || node == body || node == html) && (typeof node.scrollIntoView == "function")) { // FF2 is perfect, too bad FF3 is not
                node.scrollIntoView(false); // short-circuit to native if possible
                return;
            }
            var ltr = dojo._isBodyLtr();
            var isIE8strict = dojo.isIE >= 8 && !compatMode;
            var rtl = !ltr && !isIE8strict; // IE8 flips scrolling so pretend it's ltr
            // body and html elements are all messed up due to browser bugs and inconsistencies related to doctype
            // normalize the values before proceeding (FF2 is not listed since its native behavior is perfect)
            // for computation simplification, client and offset width and height are the same for body and html
            // strict:       html:       |      body:       | compatMode:
            //           width   height  |  width   height  |------------
            //    ie*:  clientW  clientH | scrollW  clientH | CSS1Compat
            //    ff3:  clientW  clientH |HscrollW  clientH | CSS1Compat
            //    sf3:  clientW  clientH | clientW HclientH | CSS1Compat
            //    op9:  clientW  clientH |HscrollW  clientH | CSS1Compat
            // ---------------------------------------------|-----------
            //   none:        html:      |      body:       |
            //           width    height |  width   height  |
            //    ie*: BclientW BclientH | clientW  clientH | BackCompat
            //    ff3: BclientW BclientH | clientW  clientH | BackCompat
            //    sf3:  clientW  clientH | clientW HclientH | CSS1Compat
            //    op9: BclientW BclientH | clientW  clientH | BackCompat
            // ---------------------------------------------|-----------
            //  loose:        html:      |      body:       |
            //           width    height |  width   height  |
            //    ie*:  clientW  clientH | scrollW  clientH | CSS1Compat
            //    ff3: BclientW BclientH | clientW  clientH | BackCompat
            //    sf3:  clientW  clientH | clientW HclientH | CSS1Compat
            //    op9:  clientW  clientH |HscrollW  clientH | CSS1Compat
            var scrollRoot = body;
            var compatMode = doc.compatMode == 'BackCompat';
            if (compatMode) { // BODY is scrollable, HTML has same client size
                // body client values already OK
                html._offsetWidth = html._clientWidth = body._offsetWidth = body.clientWidth;
                html._offsetHeight = html._clientHeight = body._offsetHeight = body.clientHeight;
            } else {
                if (dojo.isWebKit) {
                    body._offsetWidth = body._clientWidth = html.clientWidth;
                    body._offsetHeight = body._clientHeight = html.clientHeight;
                } else {
                    scrollRoot = html;
                }
                html._offsetHeight = html.clientHeight;
                html._offsetWidth = html.clientWidth;
            }

            function isFixedPosition(element) {
                var ie = dojo.isIE;
                return ((ie <= 6 || (ie >= 7 && compatMode)) ? false : (dojo.style(element, 'position').toLowerCase() == "fixed"));
            }

            function addPseudoAttrs(element) {
                var parent = element.parentNode;
                var offsetParent = element.offsetParent;
                if (offsetParent == null || isFixedPosition(element)) { // position:fixed has no real offsetParent
                    offsetParent = html; // prevents exeptions
                    parent = (element == body) ? html : null;
                }
                // all the V/H object members below are to reuse code for both directions
                element._offsetParent = offsetParent;
                element._parent = parent;
                //console.debug('parent = ' + (element._parentTag = element._parent?element._parent.tagName:'NULL'));
                //console.debug('offsetParent = ' + (element._offsetParentTag = element._offsetParent.tagName));
                var bp = dojo._getBorderExtents(element);
                element._borderStart = { H:(isIE8strict && !ltr) ? (bp.w - bp.l) : bp.l, V:bp.t };
                element._borderSize = { H:bp.w, V:bp.h };
                element._scrolledAmount = { H:element.scrollLeft, V:element.scrollTop };
                element._offsetSize = { H: element._offsetWidth || element.offsetWidth, V: element._offsetHeight || element.offsetHeight };
                //console.debug('element = ' + element.tagName + ', '+testdir+' size = ' + element[testdir=='H'?"offsetWidth":"offsetHeight"] + ', parent = ' + element._parentTag);
                // IE8 flips everything in rtl mode except offsetLeft and borderLeft - so manually change offsetLeft to offsetRight here
                element._offsetStart = { H:(isIE8strict && !ltr) ? offsetParent.clientWidth - element.offsetLeft - element._offsetSize.H : element.offsetLeft, V:element.offsetTop };
                //console.debug('element = ' + element.tagName + ', initial _relativeOffset = ' + element._offsetStart[testdir]);
                element._clientSize = { H:element._clientWidth || element.clientWidth, V:element._clientHeight || element.clientHeight };
                if (element != body && element != html && element != node) {
                    for (var dir in element._offsetSize) { // for both x and y directions
                        var scrollBarSize = element._offsetSize[dir] - element._clientSize[dir] - element._borderSize[dir];
                        //if(dir==testdir)console.log('element = ' + element.tagName + ', scrollBarSize = ' + scrollBarSize + ', clientSize = ' + element._clientSize[dir] + ', offsetSize = ' + element._offsetSize[dir] + ', border size = ' + element._borderSize[dir]);
                        var hasScrollBar = element._clientSize[dir] > 0 && scrollBarSize > 0; // can't check for a specific scrollbar size since it changes dramatically as you zoom
                        //if(dir==testdir)console.log('element = ' + element.tagName + ', hasScrollBar = ' + hasScrollBar);
                        if (hasScrollBar) {
                            element._offsetSize[dir] -= scrollBarSize;
                            if (dojo.isIE && rtl && dir == "H") {
                                element._offsetStart[dir] += scrollBarSize;
                            }
                        }
                    }
                }
            }

            var element = node;
            while (element != null) {
                if (isFixedPosition(element)) {
                    node.scrollIntoView(false);
                    return;
                } //TODO: handle without native call
                addPseudoAttrs(element);
                element = element._parent;
            }
            if (dojo.isIE && node._parent) { // if no parent, then offsetParent._borderStart may not tbe set
                var offsetParent = node._offsetParent;
                //console.debug('adding offsetParent borderStart = ' + offsetParent._borderStart.H + ' to node offsetStart');
                node._offsetStart.H += offsetParent._borderStart.H;
                node._offsetStart.V += offsetParent._borderStart.V;
            }
            if (dojo.isIE >= 7 && scrollRoot == html && rtl && body._offsetStart && body._offsetStart.H == 0) { // IE7 bug
                var scroll = html.scrollWidth - html._offsetSize.H;
                if (scroll > 0) {
                    //console.debug('adjusting html scroll by ' + -scroll + ', scrollWidth = ' + html.scrollWidth + ', offsetSize = ' + html._offsetSize.H);
                    body._offsetStart.H = -scroll;
                }
            }
            if (dojo.isIE <= 6 && !compatMode) {
                html._offsetSize.H += html._borderSize.H;
                html._offsetSize.V += html._borderSize.V;
            }
            // eliminate offsetLeft/Top oddities by tweaking scroll for ease of computation
            if (rtl && body._offsetStart && scrollRoot == html && html._scrolledAmount) {
                var ofs = body._offsetStart.H;
                if (ofs < 0) {
                    html._scrolledAmount.H += ofs;
                    body._offsetStart.H = 0;
                }
            }
            element = node;
            while (element) {
                var parent = element._parent;
                if (!parent) {
                    break;
                }
                //console.debug('element = ' + element.tagName + ', parent = ' + parent.tagName + ', parent offsetSize = ' + parent._offsetSize[testdir] + ' clientSize = ' + parent._clientSize[testdir]);
                if (parent.tagName == "TD") {
                    var table = parent._parent._parent._parent; // point to TABLE
                    if (parent != element._offsetParent && parent._offsetParent != element._offsetParent) {
                        parent = table; // child of TD has the same offsetParent as TABLE, so skip TD, TR, and TBODY (ie. verticalslider)
                    }
                }
                // check if this node and its parent share the same offsetParent
                var relative = element._offsetParent == parent;
                //console.debug('element = ' + element.tagName + ', offsetParent = ' + element._offsetParent.tagName + ', parent = ' + parent.tagName + ', relative = ' + relative);
                for (var dir in element._offsetStart) { // for both x and y directions
                    var otherDir = dir == "H" ? "V" : "H";
                    if (rtl && dir == "H" && (parent != html) && (parent != body) && (dojo.isIE || dojo.isWebKit) && parent._clientSize.H > 0 && parent.scrollWidth > parent._clientSize.H) { // scroll starts on the right
                        var delta = parent.scrollWidth - parent._clientSize.H;
                        //console.debug('rtl scroll delta = ' + delta + ', changing ' + parent.tagName + ' scroll from ' + parent._scrolledAmount.H + ' to ' + (parent._scrolledAmount.H - delta)  + ', parent.scrollWidth = ' + parent.scrollWidth + ', parent._clientSize.H = ' + parent._clientSize.H);
                        if (delta > 0) {
                            parent._scrolledAmount.H -= delta;
                        } // match FF3 which has cool negative scrollLeft values
                    }
                    if (parent._offsetParent.tagName == "TABLE") { // make it consistent
                        if (dojo.isIE) { // make it consistent with Safari and FF3 and exclude the starting TABLE border of TABLE children
                            parent._offsetStart[dir] -= parent._offsetParent._borderStart[dir];
                            parent._borderStart[dir] = parent._borderSize[dir] = 0;
                        }
                        else {
                            parent._offsetStart[dir] += parent._offsetParent._borderStart[dir];
                        }
                    }
                    //if(dir==testdir)console.debug('border start = ' + parent._borderStart[dir] + ',  border size = ' + parent._borderSize[dir]);
                    if (dojo.isIE) {
                        //if(dir==testdir)console.debug('changing parent offsetStart from ' + parent._offsetStart[dir] + ' by adding offsetParent ' + parent._offsetParent.tagName + ' border start = ' + parent._offsetParent._borderStart[dir]);
                        parent._offsetStart[dir] += parent._offsetParent._borderStart[dir];
                    }
                    //if(dir==testdir)console.debug('subtracting border start = ' + parent._borderStart[dir]);
                    // underflow = visible gap between parent and this node taking scrolling into account
                    // if negative, part of the node is obscured by the parent's beginning and should be scrolled to become visible
                    var underflow = element._offsetStart[dir] - parent._scrolledAmount[dir] - (relative ? 0 : parent._offsetStart[dir]) - parent._borderStart[dir];
                    // if overflow is positive, number of pixels obscured by the parent's end
                    var overflow = underflow + element._offsetSize[dir] - parent._offsetSize[dir] + parent._borderSize[dir];
                    //if(dir==testdir)console.debug('element = ' + element.tagName + ', offsetStart = ' + element._offsetStart[dir] + ', relative = ' + relative + ', parent offsetStart = ' + parent._offsetStart[dir] + ', scroll = ' + parent._scrolledAmount[dir] + ', parent border start = ' + parent._borderStart[dir] + ', parent border size = ' + parent._borderSize[dir] + ', underflow = ' + underflow + ', overflow = ' + overflow + ', element offsetSize = ' + element._offsetSize[dir] + ', parent offsetSize = ' + parent._offsetSize[dir]);
                    var scrollAttr = (dir == "H") ? "scrollLeft" : "scrollTop";
                    // see if we should scroll forward or backward
                    var reverse = dir == "H" && rtl; // flip everything
                    var underflowScroll = reverse ? -overflow : underflow;
                    var overflowScroll = reverse ? -underflow : overflow;
                    // don't scroll if the over/underflow signs are opposite since that means that
                    // the node extends beyond parent's boundary in both/neither directions
                    var scrollAmount = (underflowScroll * overflowScroll <= 0) ? 0 : Math[(underflowScroll < 0) ? "max" : "min"](underflowScroll, overflowScroll);
                    //if(dir==testdir)console.debug('element = ' + element.tagName + ' dir = ' + dir + ', scrollAmount = ' + scrollAmount);
                    if (scrollAmount != 0) {
                        var oldScroll = parent[scrollAttr];
                        parent[scrollAttr] += (reverse) ? -scrollAmount : scrollAmount; // actually perform the scroll
                        var scrolledAmount = parent[scrollAttr] - oldScroll; // in case the scroll failed
                        //if(dir==testdir)console.debug('scrolledAmount = ' + scrolledAmount);
                    }
                    if (relative) {
                        element._offsetStart[dir] += parent._offsetStart[dir];
                    }
                    element._offsetStart[dir] -= parent[scrollAttr];
                }
                element._parent = parent._parent;
                element._offsetParent = parent._offsetParent;
            }
            parent = node;
            var next;
            while (parent && parent.removeAttribute) {
                next = parent.parentNode;
                parent.removeAttribute('_offsetParent');
                parent.removeAttribute('_parent');
                parent = next;
            }
        } catch(error) {
            console.error('scrollIntoView: ' + error);
            node.scrollIntoView(false);
        }
    };

}

if (!dojo._hasResource["dijit._base.sniff"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.sniff"] = true;
    // summary:
    //		Applies pre-set CSS classes to the top-level HTML node, based on:
    // 			- browser (ex: dj_ie)
    //			- browser version (ex: dj_ie6)
    //			- box model (ex: dj_contentBox)
    //			- text direction (ex: dijitRtl)
    //
    //		In addition, browser, browser version, and box model are
    //		combined with an RTL flag when browser text is RTL.  ex: dj_ie-rtl.
    //
    //		Simply doing a require on this module will
    //		establish this CSS.  Modified version of Morris' CSS hack.

    dojo.provide("dijit._base.sniff");

    (function() {

        var d = dojo,
                html = d.doc.documentElement,
                ie = d.isIE,
                opera = d.isOpera,
                maj = Math.floor,
                ff = d.isFF,
                boxModel = d.boxModel.replace(/-/, ''),
                classes = {
                    dj_ie: ie,
                    //			dj_ie55: ie == 5.5,
                    dj_ie6: maj(ie) == 6,
                    dj_ie7: maj(ie) == 7,
                    dj_iequirks: ie && d.isQuirks,
                    // NOTE: Opera not supported by dijit
                    dj_opera: opera,
                    dj_opera8: maj(opera) == 8,
                    dj_opera9: maj(opera) == 9,
                    dj_khtml: d.isKhtml,
                    dj_webkit: d.isWebKit,
                    dj_safari: d.isSafari,
                    dj_gecko: d.isMozilla,
                    dj_ff2: maj(ff) == 2,
                    dj_ff3: maj(ff) == 3
                }; // no dojo unsupported browsers

        classes["dj_" + boxModel] = true;

        // apply browser, browser version, and box model class names
        for (var p in classes) {
            if (classes[p]) {
                if (html.className) {
                    html.className += " " + p;
                } else {
                    html.className = p;
                }
            }
        }

        // If RTL mode then add dijitRtl flag plus repeat existing classes
        // with -rtl extension
        // (unshift is to make this code run after <body> node is loaded but before parser runs)
        dojo._loaders.unshift(function() {
            if (!dojo._isBodyLtr()) {
                html.className += " dijitRtl";
                for (var p in classes) {
                    if (classes[p]) {
                        html.className += " " + p + "-rtl";
                    }
                }
            }
        });

    })();

}

if (!dojo._hasResource["dijit._base.typematic"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.typematic"] = true;
    dojo.provide("dijit._base.typematic");

    dijit.typematic = {
        // summary:
        //		These functions are used to repetitively call a user specified callback
        //		method when a specific key or mouse click over a specific DOM node is
        //		held down for a specific amount of time.
        //		Only 1 such event is allowed to occur on the browser page at 1 time.

        _fireEventAndReload: function() {
            this._timer = null;
            this._callback(++this._count, this._node, this._evt);
            this._currentTimeout = (this._currentTimeout < 0) ? this._initialDelay : ((this._subsequentDelay > 1) ? this._subsequentDelay : Math.round(this._currentTimeout * this._subsequentDelay));
            this._timer = setTimeout(dojo.hitch(this, "_fireEventAndReload"), this._currentTimeout);
        },

        trigger: function(/*Event*/ evt, /* Object */ _this, /*DOMNode*/ node, /* Function */ callback, /* Object */ obj, /* Number */ subsequentDelay, /* Number */ initialDelay) {
            // summary:
            //	    Start a timed, repeating callback sequence.
            //	    If already started, the function call is ignored.
            //	    This method is not normally called by the user but can be
            //	    when the normal listener code is insufficient.
            // evt:
            //		key or mouse event object to pass to the user callback
            // _this:
            //		pointer to the user's widget space.
            // node:
            //		the DOM node object to pass the the callback function
            // callback:
            //		function to call until the sequence is stopped called with 3 parameters:
            // count:
            //		integer representing number of repeated calls (0..n) with -1 indicating the iteration has stopped
            // node:
            //		the DOM node object passed in
            // evt:
            //		key or mouse event object
            // obj:
            //		user space object used to uniquely identify each typematic sequence
            // subsequentDelay:
            //		if > 1, the number of milliseconds until the 3->n events occur
            //		or else the fractional time multiplier for the next event's delay, default=0.9
            // initialDelay:
            //		the number of milliseconds until the 2nd event occurs, default=500ms
            if (obj != this._obj) {
                this.stop();
                this._initialDelay = initialDelay || 500;
                this._subsequentDelay = subsequentDelay || 0.90;
                this._obj = obj;
                this._evt = evt;
                this._node = node;
                this._currentTimeout = -1;
                this._count = -1;
                this._callback = dojo.hitch(_this, callback);
                this._fireEventAndReload();
            }
        },

        stop: function() {
            // summary:
            //	  Stop an ongoing timed, repeating callback sequence.
            if (this._timer) {
                clearTimeout(this._timer);
                this._timer = null;
            }
            if (this._obj) {
                this._callback(-1, this._node, this._evt);
                this._obj = null;
            }
        },

        addKeyListener: function(/*DOMNode*/ node, /*Object*/ keyObject, /*Object*/ _this, /*Function*/ callback, /*Number*/ subsequentDelay, /*Number*/ initialDelay) {
            // summary:
            //		Start listening for a specific typematic key.
            //		See also the trigger method for other parameters.
            // keyObject:
            //		an object defining the key to listen for.
            // charOrCode:
            //		the printable character (string) or keyCode (number) to listen for.
            // keyCode:
            //		(deprecated - use charOrCode) the keyCode (number) to listen for (implies charCode = 0).
            // charCode:
            //		(deprecated - use charOrCode) the charCode (number) to listen for.
            // ctrlKey:
            //		desired ctrl key state to initiate the calback sequence:
            //			- pressed (true)
            //			- released (false)
            //			- either (unspecified)
            // altKey:
            //		same as ctrlKey but for the alt key
            // shiftKey:
            //		same as ctrlKey but for the shift key
            // returns:
            //		an array of dojo.connect handles
            if (keyObject.keyCode) {
                keyObject.charOrCode = keyObject.keyCode;
                dojo.deprecated("keyCode attribute parameter for dijit.typematic.addKeyListener is deprecated. Use charOrCode instead.", "", "2.0");
            } else if (keyObject.charCode) {
                keyObject.charOrCode = String.fromCharCode(keyObject.charCode);
                dojo.deprecated("charCode attribute parameter for dijit.typematic.addKeyListener is deprecated. Use charOrCode instead.", "", "2.0");
            }
            return [
                dojo.connect(node, "onkeypress", this, function(evt) {
                    if (evt.charOrCode == keyObject.charOrCode &&
                        (keyObject.ctrlKey === undefined || keyObject.ctrlKey == evt.ctrlKey) &&
                        (keyObject.altKey === undefined || keyObject.altKey == evt.ctrlKey) &&
                        (keyObject.shiftKey === undefined || keyObject.shiftKey == evt.ctrlKey)) {
                        dojo.stopEvent(evt);
                        dijit.typematic.trigger(keyObject, _this, node, callback, keyObject, subsequentDelay, initialDelay);
                    } else if (dijit.typematic._obj == keyObject) {
                        dijit.typematic.stop();
                    }
                }),
                dojo.connect(node, "onkeyup", this, function(evt) {
                    if (dijit.typematic._obj == keyObject) {
                        dijit.typematic.stop();
                    }
                })
            ];
        },

        addMouseListener: function(/*DOMNode*/ node, /*Object*/ _this, /*Function*/ callback, /*Number*/ subsequentDelay, /*Number*/ initialDelay) {
            // summary:
            //		Start listening for a typematic mouse click.
            //		See the trigger method for other parameters.
            // returns:
            //		an array of dojo.connect handles
            var dc = dojo.connect;
            return [
                dc(node, "mousedown", this, function(evt) {
                    dojo.stopEvent(evt);
                    dijit.typematic.trigger(evt, _this, node, callback, node, subsequentDelay, initialDelay);
                }),
                dc(node, "mouseup", this, function(evt) {
                    dojo.stopEvent(evt);
                    dijit.typematic.stop();
                }),
                dc(node, "mouseout", this, function(evt) {
                    dojo.stopEvent(evt);
                    dijit.typematic.stop();
                }),
                dc(node, "mousemove", this, function(evt) {
                    dojo.stopEvent(evt);
                }),
                dc(node, "dblclick", this, function(evt) {
                    dojo.stopEvent(evt);
                    if (dojo.isIE) {
                        dijit.typematic.trigger(evt, _this, node, callback, node, subsequentDelay, initialDelay);
                        setTimeout(dojo.hitch(this, dijit.typematic.stop), 50);
                    }
                })
            ];
        },

        addListener: function(/*Node*/ mouseNode, /*Node*/ keyNode, /*Object*/ keyObject, /*Object*/ _this, /*Function*/ callback, /*Number*/ subsequentDelay, /*Number*/ initialDelay) {
            // summary:
            //		Start listening for a specific typematic key and mouseclick.
            //		This is a thin wrapper to addKeyListener and addMouseListener.
            //		See the addMouseListener and addKeyListener methods for other parameters.
            // mouseNode:
            //		the DOM node object to listen on for mouse events.
            // keyNode:
            //		the DOM node object to listen on for key events.
            // returns:
            //		an array of dojo.connect handles
            return this.addKeyListener(keyNode, keyObject, _this, callback, subsequentDelay, initialDelay).concat(
                    this.addMouseListener(mouseNode, _this, callback, subsequentDelay, initialDelay));
        }
    };

}

if (!dojo._hasResource["dijit._base.wai"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base.wai"] = true;
    dojo.provide("dijit._base.wai");

    dijit.wai = {
        onload: function() {
            // summary:
            //		Detects if we are in high-contrast mode or not

            // This must be a named function and not an anonymous
            // function, so that the widget parsing code can make sure it
            // registers its onload function after this function.
            // DO NOT USE "this" within this function.

            // create div for testing if high contrast mode is on or images are turned off
            var div = dojo.create("div", {
                id: "a11yTestNode",
                style:{
                    cssText:'border: 1px solid;'
                            + 'border-color:red green;'
                            + 'position: absolute;'
                            + 'height: 5px;'
                            + 'top: -999px;'
                            + 'background-image: url("' + (dojo.config.blankGif || dojo.moduleUrl("dojo", "resources/blank.gif")) + '");'
                }
            }, dojo.body());

            // test it
            var cs = dojo.getComputedStyle(div);
            if (cs) {
                var bkImg = cs.backgroundImage;
                var needsA11y = (cs.borderTopColor == cs.borderRightColor) || (bkImg != null && (bkImg == "none" || bkImg == "url(invalid-url:)" ));
                dojo[needsA11y ? "addClass" : "removeClass"](dojo.body(), "dijit_a11y");
                if (dojo.isIE) {
                    div.outerHTML = "";		// prevent mixed-content warning, see http://support.microsoft.com/kb/925014
                } else {
                    dojo.body().removeChild(div);
                }
            }
        }
    };

    // Test if computer is in high contrast mode.
    // Make sure the a11y test runs first, before widgets are instantiated.
    if (dojo.isIE || dojo.isMoz) {    // NOTE: checking in Safari messes things up
        dojo._loaders.unshift(dijit.wai.onload);
    }

    dojo.mixin(dijit,
    {
        _XhtmlRoles: /banner|contentinfo|definition|main|navigation|search|note|secondary|seealso/,

        hasWaiRole: function(/*Element*/ elem, /*String*/ role) {
            // summary:
            //		Determines if an element has a particular non-XHTML role.
            // returns:
            //		True if elem has the specific non-XHTML role attribute and false if not.
            // 		For backwards compatibility if role parameter not provided,
            // 		returns true if has non XHTML role
            var waiRole = this.getWaiRole(elem);
            return role ? (waiRole.indexOf(role) > -1) : (waiRole.length > 0);
        },

        getWaiRole: function(/*Element*/ elem) {
            // summary:
            //		Gets the non-XHTML role for an element (which should be a wai role).
            // returns:
            //		The non-XHTML role of elem or an empty string if elem
            //		does not have a role.
            return dojo.trim((dojo.attr(elem, "role") || "").replace(this._XhtmlRoles, "").replace("wairole:", ""));
        },

        setWaiRole: function(/*Element*/ elem, /*String*/ role) {
            // summary:
            //		Sets the role on an element.
            // description:
            //		In other than FF2 replace existing role attribute with new role.
            //		FF3 supports XHTML and ARIA roles so
            //		if elem already has an XHTML role, append this role to XHTML role
            //		and remove other ARIA roles.
            //		On Firefox 2 and below, "wairole:" is
            //		prepended to the provided role value.

            var curRole = dojo.attr(elem, "role") || "";
            if (dojo.isFF < 3 || !this._XhtmlRoles.test(curRole)) {
                dojo.attr(elem, "role", dojo.isFF < 3 ? "wairole:" + role : role);
            } else {
                if ((" " + curRole + " ").indexOf(" " + role + " ") < 0) {
                    var clearXhtml = dojo.trim(curRole.replace(this._XhtmlRoles, ""));
                    var cleanRole = dojo.trim(curRole.replace(clearXhtml, ""));
                    dojo.attr(elem, "role", cleanRole + (cleanRole ? ' ' : '') + role);
                }
            }
        },

        removeWaiRole: function(/*Element*/ elem, /*String*/ role) {
            // summary:
            //		Removes the specified non-XHTML role from an element.
            // 		Removes role attribute if no specific role provided (for backwards compat.)

            var roleValue = dojo.attr(elem, "role");
            if (!roleValue) {
                return;
            }
            if (role) {
                var searchRole = dojo.isFF < 3 ? "wairole:" + role : role;
                var t = dojo.trim((" " + roleValue + " ").replace(" " + searchRole + " ", " "));
                dojo.attr(elem, "role", t);
            } else {
                elem.removeAttribute("role");
            }
        },

        hasWaiState: function(/*Element*/ elem, /*String*/ state) {
            // summary:
            //		Determines if an element has a given state.
            // description:
            //		On Firefox 2 and below, we check for an attribute in namespace
            //		"http://www.w3.org/2005/07/aaa" with a name of the given state.
            //		On all other browsers, we check for an attribute
            //		called "aria-"+state.
            // returns:
            //		true if elem has a value for the given state and
            //		false if it does not.
            if (dojo.isFF < 3) {
                return elem.hasAttributeNS("http://www.w3.org/2005/07/aaa", state);
            }
            return elem.hasAttribute ? elem.hasAttribute("aria-" + state) : !!elem.getAttribute("aria-" + state);
        },

        getWaiState: function(/*Element*/ elem, /*String*/ state) {
            // summary:
            //		Gets the value of a state on an element.
            // description:
            //		On Firefox 2 and below, we check for an attribute in namespace
            //		"http://www.w3.org/2005/07/aaa" with a name of the given state.
            //		On all other browsers, we check for an attribute called
            //		"aria-"+state.
            // returns:
            //		The value of the requested state on elem
            //		or an empty string if elem has no value for state.
            if (dojo.isFF < 3) {
                return elem.getAttributeNS("http://www.w3.org/2005/07/aaa", state);
            }
            return elem.getAttribute("aria-" + state) || "";
        },

        setWaiState: function(/*Element*/ elem, /*String*/ state, /*String*/ value) {
            // summary:
            //		Sets a state on an element.
            // description:
            //		On Firefox 2 and below, we set an attribute in namespace
            //		"http://www.w3.org/2005/07/aaa" with a name of the given state.
            //		On all other browsers, we set an attribute called
            //		"aria-"+state.
            if (dojo.isFF < 3) {
                elem.setAttributeNS("http://www.w3.org/2005/07/aaa",
                        "aaa:" + state, value);
            } else {
                elem.setAttribute("aria-" + state, value);
            }
        },

        removeWaiState: function(/*Element*/ elem, /*String*/ state) {
            // summary:
            //		Removes a state from an element.
            // description:
            //		On Firefox 2 and below, we remove the attribute in namespace
            //		"http://www.w3.org/2005/07/aaa" with a name of the given state.
            //		On all other browsers, we remove the attribute called
            //		"aria-"+state.
            if (dojo.isFF < 3) {
                elem.removeAttributeNS("http://www.w3.org/2005/07/aaa", state);
            } else {
                elem.removeAttribute("aria-" + state);
            }
        }
    });

}

if (!dojo._hasResource["dijit._base"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._base"] = true;
    dojo.provide("dijit._base");


}

if (!dojo._hasResource["dijit._Widget"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._Widget"] = true;
    dojo.provide("dijit._Widget");

    dojo.require("dijit._base");

    dojo.connect(dojo, "connect",
            function(/*Widget*/ widget, /*String*/ event) {
                if (widget && dojo.isFunction(widget._onConnect)) {
                    widget._onConnect(event);
                }
            });

    dijit._connectOnUseEventHandler = function(/*Event*/ event) {
    };

    (function() {

        var _attrReg = {};
        var getAttrReg = function(dc) {
            if (!_attrReg[dc]) {
                var r = [];
                var attrs;
                var proto = dojo.getObject(dc).prototype;
                for (var fxName in proto) {
                    if (dojo.isFunction(proto[fxName]) && (attrs = fxName.match(/^_set([a-zA-Z]*)Attr$/)) && attrs[1]) {
                        r.push(attrs[1].charAt(0).toLowerCase() + attrs[1].substr(1));
                    }
                }
                _attrReg[dc] = r;
            }
            return _attrReg[dc] || [];
        }

        dojo.declare("dijit._Widget", null, {
            // summary:
            //		Base class for all dijit widgets.

            // id: [const] String
            //		A unique, opaque ID string that can be assigned by users or by the
            //		system. If the developer passes an ID which is known not to be
            //		unique, the specified ID is ignored and the system-generated ID is
            //		used instead.
            id: "",

            // lang: [const] String
            //		Rarely used.  Overrides the default Dojo locale used to render this widget,
            //		as defined by the [HTML LANG](http://www.w3.org/TR/html401/struct/dirlang.html#adef-lang) attribute.
            //		Value must be among the list of locales specified during by the Dojo bootstrap,
            //		formatted according to [RFC 3066](http://www.ietf.org/rfc/rfc3066.txt) (like en-us).
            lang: "",

            // dir: [const] String
            //		Unsupported by Dijit, but here for completeness.  Dijit only supports setting text direction on the
            //		entire document.
            //		Bi-directional support, as defined by the [HTML DIR](http://www.w3.org/TR/html401/struct/dirlang.html#adef-dir)
            //		attribute. Either left-to-right "ltr" or right-to-left "rtl".
            dir: "",

            // class: String
            //		HTML class attribute
            "class": "",

            // style: String||Object
            //		HTML style attributes as cssText string or name/value hash
            style: "",

            // title: String
            //		HTML title attribute, used to specify the title of tabs, accordion panes, etc.
            title: "",

            // srcNodeRef: [readonly] DomNode
            //		pointer to original dom node
            srcNodeRef: null,

            // domNode: [readonly] DomNode
            //		This is our visible representation of the widget! Other DOM
            //		Nodes may by assigned to other properties, usually through the
            //		template system's dojoAttachPoint syntax, but the domNode
            //		property is the canonical "top level" node in widget UI.
            domNode: null,

            // containerNode: [readonly] DomNode
            //		Designates where children of the source dom node will be placed.
            //		"Children" in this case refers to both dom nodes and widgets.
            //		For example, for myWidget:
            //
            //		|	<div dojoType=myWidget>
            //		|		<b> here's a plain dom node
            //		|		<span dojoType=subWidget>and a widget</span>
            //		|		<i> and another plain dom node </i>
            //		|	</div>
            //
            //		containerNode would point to:
            //
            //		|		<b> here's a plain dom node
            //		|		<span dojoType=subWidget>and a widget</span>
            //		|		<i> and another plain dom node </i>
            //
            //		In templated widgets, "containerNode" is set via a
            //		dojoAttachPoint assignment.
            //
            //		containerNode must be defined for any widget that accepts innerHTML
            //		(like ContentPane or BorderContainer or even Button), and conversely
            //		is null for widgets that don't, like TextBox.
            containerNode: null,

            // attributeMap: [protected] Object
            //		attributeMap sets up a "binding" between attributes (aka properties)
            //		of the widget and the widget's DOM.
            //		Changes to widget attributes listed in attributeMap will be
            //		reflected into the DOM.
            //
            //		For example, calling attr('title', 'hello')
            //		on a TitlePane will automatically cause the TitlePane's DOM to update
            //		with the new title.
            //
            //		attributeMap is a hash where the key is an attribute of the widget,
            //		and the value reflects a binding to a:
            //
            //		- DOM node attribute
            // |		focus: {node: "focusNode", type: "attribute"}
            // 		Maps this.focus to this.focusNode.focus
            //
            //		- DOM node innerHTML
            //	|		title: { node: "titleNode", type: "innerHTML" }
            //		Maps this.title to this.titleNode.innerHTML
            //
            //		- DOM node CSS class
            // |		myClass: { node: "domNode", type: "class" }
            //		Maps this.myClass to this.domNode.className
            //
            //		If the value is an array, then each element in the array matches one of the
            //		formats of the above list.
            //
            //		There are also some shorthands for backwards compatibility:
            //		- string --> { node: string, type: "attribute" }, for example:
            //	|	"focusNode" ---> { node: "focusNode", type: "attribute" }
            //		- "" --> { node: "domNode", type: "attribute" }
            attributeMap: {id:"", dir:"", lang:"", "class":"", style:"", title:""},

            // _deferredConnects: [protected] Object
            //		attributeMap addendum for event handlers that should be connected only on first use
            _deferredConnects: {
                onClick: "",
                onDblClick: "",
                onKeyDown: "",
                onKeyPress: "",
                onKeyUp: "",
                onMouseMove: "",
                onMouseDown: "",
                onMouseOut: "",
                onMouseOver: "",
                onMouseLeave: "",
                onMouseEnter: "",
                onMouseUp: ""},

            onClick: dijit._connectOnUseEventHandler,
            /*=====
             onClick: function(event){
             // summary:
             //		Connect to this function to receive notifications of mouse click events.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/
            onDblClick: dijit._connectOnUseEventHandler,
            /*=====
             onDblClick: function(event){
             // summary:
             //		Connect to this function to receive notifications of mouse double click events.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/
            onKeyDown: dijit._connectOnUseEventHandler,
            /*=====
             onKeyDown: function(event){
             // summary:
             //		Connect to this function to receive notifications of keys being pressed down.
             // event:
             //		key Event
             // tags:
             //		callback
             },
             =====*/
            onKeyPress: dijit._connectOnUseEventHandler,
            /*=====
             onKeyPress: function(event){
             // summary:
             //		Connect to this function to receive notifications of printable keys being typed.
             // event:
             //		key Event
             // tags:
             //		callback
             },
             =====*/
            onKeyUp: dijit._connectOnUseEventHandler,
            /*=====
             onKeyUp: function(event){
             // summary:
             //		Connect to this function to receive notifications of keys being released.
             // event:
             //		key Event
             // tags:
             //		callback
             },
             =====*/
            onMouseDown: dijit._connectOnUseEventHandler,
            /*=====
             onMouseDown: function(event){
             // summary:
             //		Connect to this function to receive notifications of when the mouse button is pressed down.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/
            onMouseMove: dijit._connectOnUseEventHandler,
            /*=====
             onMouseMove: function(event){
             // summary:
             //		Connect to this function to receive notifications of when the mouse moves over nodes contained within this widget.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/
            onMouseOut: dijit._connectOnUseEventHandler,
            /*=====
             onMouseOut: function(event){
             // summary:
             //		Connect to this function to receive notifications of when the mouse moves off of nodes contained within this widget.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/
            onMouseOver: dijit._connectOnUseEventHandler,
            /*=====
             onMouseOver: function(event){
             // summary:
             //		Connect to this function to receive notifications of when the mouse moves onto nodes contained within this widget.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/
            onMouseLeave: dijit._connectOnUseEventHandler,
            /*=====
             onMouseLeave: function(event){
             // summary:
             //		Connect to this function to receive notifications of when the mouse moves off of this widget.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/
            onMouseEnter: dijit._connectOnUseEventHandler,
            /*=====
             onMouseEnter: function(event){
             // summary:
             //		Connect to this function to receive notifications of when the mouse moves onto this widget.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/
            onMouseUp: dijit._connectOnUseEventHandler,
            /*=====
             onMouseUp: function(event){
             // summary:
             //		Connect to this function to receive notifications of when the mouse button is released.
             // event:
             //		mouse Event
             // tags:
             //		callback
             },
             =====*/

            // Constants used in templates

            // _blankGif: [protected] URL
            //		Used by <img> nodes in templates that really get there image via CSS background-image
            _blankGif: (dojo.config.blankGif || dojo.moduleUrl("dojo", "resources/blank.gif")),

            //////////// INITIALIZATION METHODS ///////////////////////////////////////

            postscript: function(/*Object?*/params, /*DomNode|String*/srcNodeRef) {
                // summary:
                //		Kicks off widget instantiation.  See create() for details.
                // tags:
                //		private
                this.create(params, srcNodeRef);
            },

            create: function(/*Object?*/params, /*DomNode|String?*/srcNodeRef) {
                // summary:
                //		Kick off the life-cycle of a widget
                // params:
                //		Hash of initialization parameters for widget, including
                //		scalar values (like title, duration etc.) and functions,
                //		typically callbacks like onClick.
                // srcNodeRef:
                //		If a srcNodeRef (dom node) is specified:
                //			- use srcNodeRef.innerHTML as my contents
                //			- if this is a behavioral widget then apply behavior
                //			  to that srcNodeRef
                //			- otherwise, replace srcNodeRef with my generated DOM
                //			  tree
                // description:
                //		To understand the process by which widgets are instantiated, it
                //		is critical to understand what other methods create calls and
                //		which of them you'll want to override. Of course, adventurous
                //		developers could override create entirely, but this should
                //		only be done as a last resort.
                //
                //		Below is a list of the methods that are called, in the order
                //		they are fired, along with notes about what they do and if/when
                //		you should over-ride them in your widget:
                //
                // * postMixInProperties:
                //	|	* a stub function that you can over-ride to modify
                //		variables that may have been naively assigned by
                //		mixInProperties
                // * widget is added to manager object here
                // * buildRendering:
                //	|	* Subclasses use this method to handle all UI initialization
                //		Sets this.domNode.  Templated widgets do this automatically
                //		and otherwise it just uses the source dom node.
                // * postCreate:
                //	|	* a stub function that you can over-ride to modify take
                //		actions once the widget has been placed in the UI
                // tags:
                //		private

                // store pointer to original dom tree
                this.srcNodeRef = dojo.byId(srcNodeRef);

                // For garbage collection.  An array of handles returned by Widget.connect()
                // Each handle returned from Widget.connect() is an array of handles from dojo.connect()
                this._connects = [];

                // To avoid double-connects, remove entries from _deferredConnects
                // that have been setup manually by a subclass (ex, by dojoAttachEvent).
                // If a subclass has redefined a callback (ex: onClick) then assume it's being
                // connected to manually.
                this._deferredConnects = dojo.clone(this._deferredConnects);
                for (var attr in this.attributeMap) {
                    delete this._deferredConnects[attr]; // can't be in both attributeMap and _deferredConnects
                }
                for (attr in this._deferredConnects) {
                    if (this[attr] !== dijit._connectOnUseEventHandler) {
                        delete this._deferredConnects[attr];	// redefined, probably dojoAttachEvent exists
                    }
                }

                //mixin our passed parameters
                if (this.srcNodeRef && (typeof this.srcNodeRef.id == "string")) {
                    this.id = this.srcNodeRef.id;
                }
                if (params) {
                    this.params = params;
                    dojo.mixin(this, params);
                }
                this.postMixInProperties();

                // generate an id for the widget if one wasn't specified
                // (be sure to do this before buildRendering() because that function might
                // expect the id to be there.)
                if (!this.id) {
                    this.id = dijit.getUniqueId(this.declaredClass.replace(/\./g, "_"));
                }
                dijit.registry.add(this);

                this.buildRendering();

                if (this.domNode) {
                    // Copy attributes listed in attributeMap into the [newly created] DOM for the widget.
                    this._applyAttributes();

                    var source = this.srcNodeRef;
                    if (source && source.parentNode) {
                        source.parentNode.replaceChild(this.domNode, source);
                    }

                    // If the developer has specified a handler as a widget parameter
                    // (ex: new Button({onClick: ...})
                    // then naturally need to connect from dom node to that handler immediately,
                    for (attr in this.params) {
                        this._onConnect(attr);
                    }
                }

                if (this.domNode) {
                    this.domNode.setAttribute("widgetId", this.id);
                }
                this.postCreate();

                // If srcNodeRef has been processed and removed from the DOM (e.g. TemplatedWidget) then delete it to allow GC.
                if (this.srcNodeRef && !this.srcNodeRef.parentNode) {
                    delete this.srcNodeRef;
                }

                this._created = true;
            },

            _applyAttributes: function() {
                // summary:
                //		Step during widget creation to copy all widget attributes to the
                //		DOM as per attributeMap and _setXXXAttr functions.
                // description:
                //		Skips over blank/false attribute values, unless they were explicitly specified
                //		as parameters to the widget, since those are the default anyway,
                //		and setting tabIndex="" is different than not setting tabIndex at all.
                //
                //		It processes the attributes in the attribute map first, and then
                //		it goes through and processes the attributes for the _setXXXAttr
                //		functions that have been specified
                // tags:
                //		private
                var condAttrApply = function(attr, scope) {
                    if ((scope.params && attr in scope.params) || scope[attr]) {
                        scope.attr(attr, scope[attr]);
                    }
                };
                for (var attr in this.attributeMap) {
                    condAttrApply(attr, this);
                }
                dojo.forEach(getAttrReg(this.declaredClass), function(a) {
                    if (!(a in this.attributeMap)) {
                        condAttrApply(a, this);
                    }
                }, this);
            },

            postMixInProperties: function() {
                // summary:
                //		Called after the parameters to the widget have been read-in,
                //		but before the widget template is instantiated. Especially
                //		useful to set properties that are referenced in the widget
                //		template.
                // tags:
                //		protected
            },

            buildRendering: function() {
                // summary:
                //		Construct the UI for this widget, setting this.domNode.  Most
                //		widgets will mixin `dijit._Templated`, which implements this
                //		method.
                // tags:
                //		protected
                this.domNode = this.srcNodeRef || dojo.create('div');
            },

            postCreate: function() {
                // summary:
                //		Called after a widget's dom has been setup
                // tags:
                //		protected
            },

            startup: function() {
                // summary:
                //		Called after a widget's children, and other widgets on the page, have been created.
                //		Provides an opportunity to manipulate any children before they are displayed.
                //		This is useful for composite widgets that need to control or layout sub-widgets.
                //		Many layout widgets can use this as a wiring phase.
                this._started = true;
            },

            //////////// DESTROY FUNCTIONS ////////////////////////////////

            destroyRecursive: function(/*Boolean?*/ preserveDom) {
                // summary:
                // 		Destroy this widget and it's descendants. This is the generic
                // 		"destructor" function that all widget users should call to
                // 		cleanly discard with a widget. Once a widget is destroyed, it's
                // 		removed from the manager object.
                // preserveDom:
                //		If true, this method will leave the original Dom structure
                //		alone of descendant Widgets. Note: This will NOT work with
                //		dijit._Templated widgets.

                this.destroyDescendants(preserveDom);
                this.destroy(preserveDom);
            },

            destroy: function(/*Boolean*/ preserveDom) {
                // summary:
                // 		Destroy this widget, but not its descendants.
                //		Will, however, destroy internal widgets such as those used within a template.
                // preserveDom: Boolean
                //		If true, this method will leave the original Dom structure alone.
                //		Note: This will not yet work with _Templated widgets

                this.uninitialize();
                dojo.forEach(this._connects, function(array) {
                    dojo.forEach(array, dojo.disconnect);
                });

                // destroy widgets created as part of template, etc.
                dojo.forEach(this._supportingWidgets || [], function(w) {
                    if (w.destroy) {
                        w.destroy();
                    }
                });

                this.destroyRendering(preserveDom);
                dijit.registry.remove(this.id);
            },

            destroyRendering: function(/*Boolean?*/ preserveDom) {
                // summary:
                //		Destroys the DOM nodes associated with this widget
                // preserveDom:
                //		If true, this method will leave the original Dom structure alone
                //		during tear-down. Note: this will not work with _Templated
                //		widgets yet.
                // tags:
                //		protected

                if (this.bgIframe) {
                    this.bgIframe.destroy(preserveDom);
                    delete this.bgIframe;
                }

                if (this.domNode) {
                    if (preserveDom) {
                        dojo.removeAttr(this.domNode, "widgetId");
                    } else {
                        dojo.destroy(this.domNode);
                    }
                    delete this.domNode;
                }

                if (this.srcNodeRef) {
                    if (!preserveDom) {
                        dojo.destroy(this.srcNodeRef);
                    }
                    delete this.srcNodeRef;
                }
            },

            destroyDescendants: function(/*Boolean?*/ preserveDom) {
                // summary:
                //		Recursively destroy the children of this widget and their
                //		descendants.
                // preserveDom:
                //		If true, the preserveDom attribute is passed to all descendant
                //		widget's .destroy() method. Not for use with _Templated
                //		widgets.

                // get all direct descendants and destroy them recursively
                dojo.forEach(this.getChildren(), function(widget) {
                    if (widget.destroyRecursive) {
                        widget.destroyRecursive(preserveDom);
                    }
                });
            },


            uninitialize: function() {
                // summary:
                //		Stub function. Override to implement custom widget tear-down
                //		behavior.
                // tags:
                //		protected
                return false;
            },

            ////////////////// MISCELLANEOUS METHODS ///////////////////

            onFocus: function() {
                // summary:
                //		Called when the widget becomes "active" because
                //		it or a widget inside of it either has focus, or has recently
                //		been clicked.
                // tags:
                //		callback
            },

            onBlur: function() {
                // summary:
                //		Called when the widget stops being "active" because
                //		focus moved to something outside of it, or the user
                //		clicked somewhere outside of it, or the widget was
                //		hidden.
                // tags:
                //		callback
            },

            _onFocus: function(e) {
                // summary:
                //		This is where widgets do processing for when they are active,
                //		such as changing CSS classes.  See onFocus() for more details.
                // tags:
                //		protected
                this.onFocus();
            },

            _onBlur: function() {
                // summary:
                //		This is where widgets do processing for when they stop being active,
                //		such as changing CSS classes.  See onBlur() for more details.
                // tags:
                //		protected
                this.onBlur();
            },

            _onConnect: function(/*String*/ event) {
                // summary:
                //		Called when someone connects to one of my handlers.
                //		"Turn on" that handler if it isn't active yet.
                //
                //		This is also called for every single initialization parameter
                //		so need to do nothing for parameters like "id".
                // tags:
                //		private
                if (event in this._deferredConnects) {
                    var mapNode = this[this._deferredConnects[event] || 'domNode'];
                    this.connect(mapNode, event.toLowerCase(), event);
                    delete this._deferredConnects[event];
                }
            },

            _setClassAttr: function(/*String*/ value) {
                // summary:
                //		Custom setter for the CSS "class" attribute
                // tags:
                //		protected
                var mapNode = this[this.attributeMap["class"] || 'domNode'];
                dojo.removeClass(mapNode, this["class"])
                this["class"] = value;
                dojo.addClass(mapNode, value);
            },

            _setStyleAttr: function(/*String||Object*/ value) {
                // summary:
                //		Sets the style attribut of the widget according to value,
                //		which is either a hash like {height: "5px", width: "3px"}
                //		or a plain string
                // description:
                //		Determines which node to set the style on based on style setting
                //		in attributeMap.
                // tags:
                //		protected

                var mapNode = this[this.attributeMap["style"] || 'domNode'];

                // Note: technically we should revert any style setting made in a previous call
                // to his method, but that's difficult to keep track of.

                if (dojo.isObject(value)) {
                    dojo.style(mapNode, value);
                } else {
                    if (mapNode.style.cssText) {
                        mapNode.style.cssText += "; " + value;
                    } else {
                        mapNode.style.cssText = value;
                    }
                }

                this["style"] = value;
            },

            setAttribute: function(/*String*/ attr, /*anything*/ value) {
                // summary:
                //		Deprecated.  Use attr() instead.
                // tags:
                //		deprecated
                dojo.deprecated(this.declaredClass + "::setAttribute() is deprecated. Use attr() instead.", "", "2.0");
                this.attr(attr, value);
            },

            _attrToDom: function(/*String*/ attr, /*String*/ value) {
                // summary:
                //		Reflect a widget attribute (title, tabIndex, duration etc.) to
                //		the widget DOM, as specified in attributeMap.
                //
                // description:
                //		Also sets this["attr"] to the new value.
                //		Note some attributes like "type"
                //		cannot be processed this way as they are not mutable.
                //
                // tags:
                //		private

                var commands = this.attributeMap[attr];
                dojo.forEach(dojo.isArray(commands) ? commands : [commands], function(command) {

                    // Get target node and what we are doing to that node
                    var mapNode = this[command.node || command || "domNode"];	// DOM node
                    var type = command.type || "attribute";	// class, innerHTML, or attribute

                    switch (type) {
                        case "attribute":
                            if (dojo.isFunction(value)) { // functions execute in the context of the widget
                                value = dojo.hitch(this, value);
                            }
                            if (/^on[A-Z][a-zA-Z]*$/.test(attr)) { // eg. onSubmit needs to be onsubmit
                                attr = attr.toLowerCase();
                            }
                            dojo.attr(mapNode, attr, value);
                            break;
                        case "innerHTML":
                            mapNode.innerHTML = value;
                            break;
                        case "class":
                            dojo.removeClass(mapNode, this[attr]);
                            dojo.addClass(mapNode, value);
                            break;
                    }
                }, this);
                this[attr] = value;
            },

            attr: function(/*String|Object*/name, /*Object?*/value) {
                //	summary:
                //		Set or get properties on a widget instance.
                //	name:
                //		The property to get or set. If an object is passed here and not
                //		a string, its keys are used as names of attributes to be set
                //		and the value of the object as values to set in the widget.
                //	value:
                //		Optional. If provided, attr() operates as a setter. If omitted,
                //		the current value of the named property is returned.
                //	description:
                //		Get or set named properties on a widget. If no value is
                //		provided, the current value of the attribute is returned,
                //		potentially via a getter method. If a value is provided, then
                //		the method acts as a setter, assigning the value to the name,
                //		potentially calling any explicitly provided setters to handle
                //		the operation. For instance, if the widget has properties "foo"
                //		and "bar" and a method named "_setFooAttr", calling:
                //	|	myWidget.attr("foo", "Howdy!");
                //		would be equivalent to calling:
                //	|	widget._setFooAttr("Howdy!");
                //		while calling:
                //	|	myWidget.attr("bar", "Howdy!");
                //		would be the same as writing:
                //	|	widget.bar = "Howdy!";
                //		It also tries to copy the changes to the widget's DOM according
                //		to settings in attributeMap (see description of `dijit._Widget.attributeMap`
                //		for details)
                //		For example, calling:
                //	|	myTitlePane.attr("title", "Howdy!");
                //		will do
                //	|	myTitlePane.title = "Howdy!";
                //	|	myTitlePane.title.innerHTML = "Howdy!";
                //		It works for dom node attributes too.  Calling
                //	|	widget.attr("disabled", true)
                //		will set the disabled attribute on the widget's focusNode,
                //		among other housekeeping for a change in disabled state.

                //	open questions:
                //		- how to handle build shortcut for attributes which want to map
                //		into DOM attributes?
                //		- what relationship should setAttribute()/attr() have to
                //		layout() calls?
                var args = arguments.length;
                if (args == 1 && !dojo.isString(name)) {
                    for (var x in name) {
                        this.attr(x, name[x]);
                    }
                    return this;
                }
                var names = this._getAttrNames(name);
                if (args == 2) { // setter
                    if (this[names.s]) {
                        // use the explicit setter
                        return this[names.s](value) || this;
                    } else {
                        // if param is specified as DOM node attribute, copy it
                        if (name in this.attributeMap) {
                            this._attrToDom(name, value);
                        }

                        // FIXME: what about function assignments? Any way to connect() here?
                        this[name] = value;
                    }
                    return this;
                } else { // getter
                    if (this[names.g]) {
                        return this[names.g]();
                    } else {
                        return this[name];
                    }
                }
            },

            _attrPairNames: {},        // shared between all widgets
            _getAttrNames: function(name) {
                // summary:
                //		Helper function for Widget.attr().
                //		Caches attribute name values so we don't do the string ops every time.
                // tags:
                //		private

                var apn = this._attrPairNames;
                if (apn[name]) {
                    return apn[name];
                }
                var uc = name.charAt(0).toUpperCase() + name.substr(1);
                return apn[name] = {
                    n: name + "Node",
                    s: "_set" + uc + "Attr",
                    g: "_get" + uc + "Attr"
                };
            },

            toString: function() {
                // summary:
                //		Returns a string that represents the widget. When a widget is
                //		cast to a string, this method will be used to generate the
                //		output. Currently, it does not implement any sort of reversable
                //		serialization.
                return '[Widget ' + this.declaredClass + ', ' + (this.id || 'NO ID') + ']'; // String
            },

            getDescendants: function() {
                // summary:
                //		Returns all the widgets that contained by this, i.e., all widgets underneath this.containerNode.
                //		This method should generally be avoided as it returns widgets declared in templates, which are
                //		supposed to be internal/hidden, but it's left here for back-compat reasons.

                if (this.containerNode) {
                    var list = dojo.query('[widgetId]', this.containerNode);
                    return list.map(dijit.byNode);		// Array
                } else {
                    return [];
                }
            },

            getChildren: function() {
                // summary:
                //		Returns all the widgets contained by this, i.e., all widgets underneath this.containerNode.
                //		Does not return nested widgets, nor widgets that are part of this widget's template.
                if (this.containerNode) {
                    return dijit.findWidgets(this.containerNode);
                } else {
                    return [];
                }
            },

            // nodesWithKeyClick: [private] String[]
            //		List of nodes that correctly handle click events via native browser support,
            //		and don't need dijit's help
            nodesWithKeyClick: ["input", "button"],

            connect: function(
                /*Object|null*/ obj,
                /*String|Function*/ event,
                /*String|Function*/ method) {
                // summary:
                //		Connects specified obj/event to specified method of this object
                //		and registers for disconnect() on widget destroy.
                // description:
                //		Provide widget-specific analog to dojo.connect, except with the
                //		implicit use of this widget as the target object.
                //		This version of connect also provides a special "ondijitclick"
                //		event which triggers on a click or space-up, enter-down in IE
                //		or enter press in FF (since often can't cancel enter onkeydown
                //		in FF)
                // example:
                //	|	var btn = new dijit.form.Button();
                //	|	// when foo.bar() is called, call the listener we're going to
                //	|	// provide in the scope of btn
                //	|	btn.connect(foo, "bar", function(){
                //	|		console.debug(this.toString());
                //	|	});
                // tags:
                //		protected

                var d = dojo;
                var dc = dojo.connect;
                var handles = [];
                if (event == "ondijitclick") {
                    // add key based click activation for unsupported nodes.
                    if (!this.nodesWithKeyClick[obj.nodeName]) {
                        var m = d.hitch(this, method);
                        handles.push(
                                dc(obj, "onkeydown", this, function(e) {
                                    if (!d.isFF && e.keyCode == d.keys.ENTER &&
                                        !e.ctrlKey && !e.shiftKey && !e.altKey && !e.metaKey) {
                                        return m(e);
                                    } else if (e.keyCode == d.keys.SPACE) {
                                        // stop space down as it causes IE to scroll
                                        // the browser window
                                        d.stopEvent(e);
                                    }
                                }),
                                dc(obj, "onkeyup", this, function(e) {
                                    if (e.keyCode == d.keys.SPACE &&
                                        !e.ctrlKey && !e.shiftKey && !e.altKey && !e.metaKey) {
                                        return m(e);
                                    }
                                })
                                );
                        if (d.isFF) {
                            handles.push(
                                    dc(obj, "onkeypress", this, function(e) {
                                        if (e.keyCode == d.keys.ENTER &&
                                            !e.ctrlKey && !e.shiftKey && !e.altKey && !e.metaKey) {
                                            return m(e);
                                        }
                                    })
                                    );
                        }
                    }
                    event = "onclick";
                }
                handles.push(dc(obj, event, this, method));

                // return handles for FormElement and ComboBox
                this._connects.push(handles);
                return handles;
            },

            disconnect: function(/*Object*/ handles) {
                // summary:
                //		Disconnects handle created by this.connect.
                //		Also removes handle from this widget's list of connects
                // tags:
                //		protected
                for (var i = 0; i < this._connects.length; i++) {
                    if (this._connects[i] == handles) {
                        dojo.forEach(handles, dojo.disconnect);
                        this._connects.splice(i, 1);
                        return;
                    }
                }
            },

            isLeftToRight: function() {
                // summary:
                //		Checks the page for text direction
                // tags:
                //		protected
                return dojo._isBodyLtr(); //Boolean
            },

            isFocusable: function() {
                // summary:
                //		Return true if this widget can currently be focused
                //		and false if not
                return this.focus && (dojo.style(this.domNode, "display") != "none");
            },

            placeAt: function(/* String|DomNode|_Widget */reference, /* String?|Int? */position) {
                // summary:
                //		Place this widget's domNode reference somewhere in the DOM based
                //		on standard dojo.place conventions, or passing a Widget reference that
                //		contains and addChild member.
                //
                // description:
                //		A convenience function provided in all _Widgets, providing a simple
                //		shorthand mechanism to put an existing (or newly created) Widget
                //		somewhere in the dom, and allow chaining.
                //
                // reference:
                //		The String id of a domNode, a domNode reference, or a reference to a Widget posessing
                //		an addChild method.
                //
                // position:
                //		If passed a string or domNode reference, the position argument
                //		accepts a string just as dojo.place does, one of: "first", "last",
                //		"before", or "after".
                //
                //		If passed a _Widget reference, and that widget reference has an ".addChild" method,
                //		it will be called passing this widget instance into that method, supplying the optional
                //		position index passed.
                //
                // returns: dijit._Widget
                //		Provides a useful return of the newly created dijit._Widget instance so you
                //		can "chain" this function by instantiating, placing, then saving the return value
                //		to a variable.
                //
                // example:
                // | 	// create a Button with no srcNodeRef, and place it in the body:
                // | 	var button = new dijit.form.Button({ label:"click" }).placeAt(dojo.body());
                // | 	// now, 'button' is still the widget reference to the newly created button
                // | 	dojo.connect(button, "onClick", function(e){ console.log('click'); });
                //
                // example:
                // |	// create a button out of a node with id="src" and append it to id="wrapper":
                // | 	var button = new dijit.form.Button({},"src").placeAt("wrapper");
                //
                // example:
                // |	// place a new button as the first element of some div
                // |	var button = new dijit.form.Button({ label:"click" }).placeAt("wrapper","first");
                //
                // example:
                // |	// create a contentpane and add it to a TabContainer
                // |	var tc = dijit.byId("myTabs");
                // |	new dijit.layout.ContentPane({ href:"foo.html", title:"Wow!" }).placeAt(tc)

                if (reference["declaredClass"] && reference["addChild"]) {
                    reference.addChild(this, position);
                } else {
                    dojo.place(this.domNode, reference, position);
                }
                return this;
            }

        });

    })();

}

if (!dojo._hasResource["dijit._Contained"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._Contained"] = true;
    dojo.provide("dijit._Contained");

    dojo.declare("dijit._Contained",
            null,
    {
        // summary
        //		Mixin for widgets that are children of a container widget
        //
        // example:
        // | 	// make a basic custom widget that knows about it's parents
        // |	dojo.declare("my.customClass",[dijit._Widget,dijit._Contained],{});
        //
        getParent: function() {
            // summary:
            //		Returns the parent widget of this widget, assuming the parent
            //		implements dijit._Container
            for (var p = this.domNode.parentNode; p; p = p.parentNode) {
                var id = p.getAttribute && p.getAttribute("widgetId");
                if (id) {
                    var parent = dijit.byId(id);
                    return parent.isContainer ? parent : null;
                }
            }
            return null;
        },

        _getSibling: function(which) {
            // summary:
            //      Returns next or previous sibling
            // which:
            //      Either "next" or "previous"
            // tags:
            //      private
            var node = this.domNode;
            do{
                node = node[which + "Sibling"];
            } while (node && node.nodeType != 1);
            if (!node) {
                return null;
            } // null
            var id = node.getAttribute("widgetId");
            return dijit.byId(id);
        },

        getPreviousSibling: function() {
            // summary:
            //		Returns null if this is the first child of the parent,
            //		otherwise returns the next element sibling to the "left".

            return this._getSibling("previous"); // Mixed
        },

        getNextSibling: function() {
            // summary:
            //		Returns null if this is the last child of the parent,
            //		otherwise returns the next element sibling to the "right".

            return this._getSibling("next"); // Mixed
        },

        getIndexInParent: function() {
            // summary:
            //		Returns the index of this widget within its container parent.
            //		It returns -1 if the parent does not exist, or if the parent
            //		is not a dijit._Container

            var p = this.getParent();
            if (!p || !p.getIndexOfChild) {
                return -1; // int
            }
            return p.getIndexOfChild(this); // int
        }
    }
            );


}

if (!dojo._hasResource["dijit._Container"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._Container"] = true;
    dojo.provide("dijit._Container");

    dojo.declare("dijit._Container",
            null,
    {
        // summary:
        //		Mixin for widgets that contain a set of widget children.
        // description:
        //		Use this mixin for widgets that needs to know about and
        //		keep track of their widget children. Suitable for widgets like BorderContainer
        //		and TabContainer which contain (only) a set of child widgets.
        //
        //		It's not suitable for widgets like ContentPane
        //		which contains mixed HTML (plain DOM nodes in addition to widgets),
        //		and where contained widgets are not necessarily directly below
        //		this.containerNode.   In that case calls like addChild(node, position)
        //		wouldn't make sense.

        // isContainer: [protected] Boolean
        //		Just a flag indicating that this widget descends from dijit._Container
        isContainer: true,

        buildRendering: function() {
            this.inherited(arguments);
            if (!this.containerNode) {
                // all widgets with descendants must set containerNode
                this.containerNode = this.domNode;
            }
        },

        addChild: function(/*Widget*/ widget, /*int?*/ insertIndex) {
            // summary:
            //		Makes the given widget a child of this widget.
            // description:
            //		Inserts specified child widget's dom node as a child of this widget's
            //		container node, and possibly does other processing (such as layout).

            var refNode = this.containerNode;
            if (insertIndex && typeof insertIndex == "number") {
                var children = this.getChildren();
                if (children && children.length >= insertIndex) {
                    refNode = children[insertIndex - 1].domNode;
                    insertIndex = "after";
                }
            }
            dojo.place(widget.domNode, refNode, insertIndex);

            // If I've been started but the child widget hasn't been started,
            // start it now.  Make sure to do this after widget has been
            // inserted into the DOM tree, so it can see that it's being controlled by me,
            // so it doesn't try to size itself.
            if (this._started && !widget._started) {
                widget.startup();
            }
        },

        removeChild: function(/*Widget or int*/ widget) {
            // summary:
            //		Removes the passed widget instance from this widget but does
            //		not destroy it.  You can also pass in an integer indicating
            //		the index within the container to remove
            if (typeof widget == "number" && widget > 0) {
                widget = this.getChildren()[widget];
            }
            // If we cannot find the widget, just return
            if (!widget || !widget.domNode) {
                return;
            }

            var node = widget.domNode;
            node.parentNode.removeChild(node);	// detach but don't destroy
        },

        _nextElement: function(node) {
            // summary:
            //      Find the next (non-text, non-comment etc) node
            // tags:
            //      private
            do{
                node = node.nextSibling;
            } while (node && node.nodeType != 1);
            return node;
        },

        _firstElement: function(node) {
            // summary:
            //      Find the first (non-text, non-comment etc) node
            // tags:
            //      private
            node = node.firstChild;
            if (node && node.nodeType != 1) {
                node = this._nextElement(node);
            }
            return node;
        },

        getChildren: function() {
            // summary:
            //		Returns array of children widgets.
            // description:
            //		Returns the widgets that are directly under this.containerNode.
            return dojo.query("> [widgetId]", this.containerNode).map(dijit.byNode); // Widget[]
        },

        hasChildren: function() {
            // summary:
            //		Returns true if widget has children, i.e. if this.containerNode contains something.
            return !!this._firstElement(this.containerNode); // Boolean
        },

        destroyDescendants: function(/*Boolean*/ preserveDom) {
            // summary:
            //      Destroys all the widgets inside this.containerNode,
            //      but not this widget itself
            dojo.forEach(this.getChildren(), function(child) {
                child.destroyRecursive(preserveDom);
            });
        },

        _getSiblingOfChild: function(/*Widget*/ child, /*int*/ dir) {
            // summary:
            //		Get the next or previous widget sibling of child
            // dir:
            //		if 1, get the next sibling
            //		if -1, get the previous sibling
            // tags:
            //      private
            var node = child.domNode;
            var which = (dir > 0 ? "nextSibling" : "previousSibling");
            do{
                node = node[which];
            } while (node && (node.nodeType != 1 || !dijit.byNode(node)));
            return node ? dijit.byNode(node) : null;
        },

        getIndexOfChild: function(/*Widget*/ child) {
            // summary:
            //		Gets the index of the child in this container or -1 if not found
            var children = this.getChildren();
            for (var i = 0, c; c = children[i]; i++) {
                if (c == child) {
                    return i; // int
                }
            }
            return -1; // int
        }
    }
            );

}

if (!dojo._hasResource["dijit.layout._LayoutWidget"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.layout._LayoutWidget"] = true;
    dojo.provide("dijit.layout._LayoutWidget");


    dojo.declare("dijit.layout._LayoutWidget",
            [dijit._Widget, dijit._Container, dijit._Contained],
    {
        // summary:
        //		Base class for a _Container widget which is responsible for laying out its children.
        //		Widgets which mixin this code must define layout() to lay out the children.

        // baseClass: [protected extension] String
        //		This class name is applied to the widget's domNode
        //		and also may be used to generate names for sub nodes,
        //		like for example dijitTabContainer-content.
        baseClass: "dijitLayoutContainer",

        // isLayoutContainer: [private deprecated] Boolean
        //		TODO: this is unused, but maybe it *should* be used for a child to
        //		detect whether the parent is going to call resize() on it or not
        //		(see calls to getParent() and resize() in this file)
        isLayoutContainer: true,

        postCreate: function() {
            dojo.addClass(this.domNode, "dijitContainer");
            dojo.addClass(this.domNode, this.baseClass);

            // TODO: this.inherited()
        },

        startup: function() {
            // summary:
            //		Called after all the widgets have been instantiated and their
            //		dom nodes have been inserted somewhere under dojo.doc.body.
            //
            //		Widgets should override this method to do any initialization
            //		dependent on other widgets existing, and then call
            //		this superclass method to finish things off.
            //
            //		startup() in subclasses shouldn't do anything
            //		size related because the size of the widget hasn't been set yet.

            if (this._started) {
                return;
            }

            // TODO: seems like this code should be in _Container.startup().
            // Then things that don't extend LayoutContainer (like GridContainer)
            // would get the behavior for free.
            dojo.forEach(this.getChildren(), function(child) {
                child.startup();
            });

            // If I am a top level widget
            if (!this.getParent || !this.getParent()) {
                // Do recursive sizing and layout of all my descendants
                // (passing in no argument to resize means that it has to glean the size itself)
                this.resize();

                // Since my parent isn't a layout container, and my style is width=height=100% (or something similar),
                // then I need to watch when the window resizes, and size myself accordingly.
                // (Passing in no arguments to resize means that it has to glean the size itself.)
                // TODO: make one global listener to avoid getViewport() per widget.
                this._viewport = dijit.getViewport();
                this.connect(dojo.global, 'onresize', function() {
                    var newViewport = dijit.getViewport();
                    if (newViewport.w != this._viewport.w || newViewport.h != this._viewport.h) {
                        this._viewport = newViewport;
                        this.resize();
                    }
                });
            }

            this.inherited(arguments);
        },

        resize: function(changeSize, resultSize) {
            // summary:
            //		Call this to resize a widget, or after its size has changed.
            // description:
            //		Change size mode:
            //			When changeSize is specified, changes the marginBox of this widget
            //			 and forces it to relayout its contents accordingly.
            //			changeSize may specify height, width, or both.
            //
            //			If resultSize is specified it indicates the size the widget will
            //			become after changeSize has been applied.
            //
            //		Notification mode:
            //			When changeSize is null, indicates that the caller has already changed
            //			the size of the widget, or perhaps it changed because the browser
            //			window was resized.  Tells widget to relayout it's contents accordingly.
            //
            //			If resultSize is also specified it indicates the size the widget has
            //			become.
            //
            //		In either mode, this method also:
            //			1. Sets this._borderBox and this._contentBox to the new size of
            //				the widget.  Queries the current domNode size if necessary.
            //			2. Calls layout() to resize contents (and maybe adjust child widgets).
            //
            // changeSize: Object?
            //		Sets the widget to this margin-box size and position.
            //		May include any/all of the following properties:
            //	|	{w: int, h: int, l: int, t: int}
            //
            // resultSize: Object?
            //		The margin-box size of this widget after applying changeSize (if
            //		changeSize is specified).  If caller knows this size and
            //		passes it in, we don't need to query the browser to get the size.
            //	|	{w: int, h: int}

            var node = this.domNode;

            // set margin box size, unless it wasn't specified, in which case use current size
            if (changeSize) {
                dojo.marginBox(node, changeSize);

                // set offset of the node
                if (changeSize.t) {
                    node.style.top = changeSize.t + "px";
                }
                if (changeSize.l) {
                    node.style.left = changeSize.l + "px";
                }
            }

            // If either height or width wasn't specified by the user, then query node for it.
            // But note that setting the margin box and then immediately querying dimensions may return
            // inaccurate results, so try not to depend on it.
            var mb = resultSize || {};
            dojo.mixin(mb, changeSize || {});	// changeSize overrides resultSize
            if (!("h" in mb) || !("w" in mb)) {
                mb = dojo.mixin(dojo.marginBox(node), mb);	// just use dojo.marginBox() to fill in missing values
            }

            // Compute and save the size of my border box and content box
            // (w/out calling dojo.contentBox() since that may fail if size was recently set)
            var cs = dojo.getComputedStyle(node);
            var me = dojo._getMarginExtents(node, cs);
            var be = dojo._getBorderExtents(node, cs);
            var bb = (this._borderBox = {
                w: mb.w - (me.w + be.w),
                h: mb.h - (me.h + be.h)
            });
            var pe = dojo._getPadExtents(node, cs);
            this._contentBox = {
                l: dojo._toPixelValue(node, cs.paddingLeft),
                t: dojo._toPixelValue(node, cs.paddingTop),
                w: bb.w - pe.w,
                h: bb.h - pe.h
            };

            // Callback for widget to adjust size of it's children
            this.layout();
        },

        layout: function() {
            // summary:
            //		Widgets override this method to size and position their contents/children.
            //		When this is called this._contentBox is guaranteed to be set (see resize()).
            //
            //		This is called after startup(), and also when the widget's size has been
            //		changed.
            // tags:
            //		protected extension
        },

        _setupChild: function(/*Widget*/child) {
            // summary:
            //		Common setup for initial children and children which are added after startup
            // tags:
            //		protected extension

            dojo.addClass(child.domNode, this.baseClass + "-child");
            if (child.baseClass) {
                dojo.addClass(child.domNode, this.baseClass + "-" + child.baseClass);
            }
        },

        addChild: function(/*Widget*/ child, /*Integer?*/ insertIndex) {
            // Overrides _Container.addChild() to call _setupChild()
            this.inherited(arguments);
            if (this._started) {
                this._setupChild(child);
            }
        },

        removeChild: function(/*Widget*/ child) {
            // Overrides _Container.removeChild() to remove class added by _setupChild()
            dojo.removeClass(child.domNode, this.baseClass + "-child");
            if (child.baseClass) {
                dojo.removeClass(child.domNode, this.baseClass + "-" + child.baseClass);
            }
            this.inherited(arguments);
        }
    }
            );

    dijit.layout.marginBox2contentBox = function(/*DomNode*/ node, /*Object*/ mb) {
        // summary:
        //		Given the margin-box size of a node, return its content box size.
        //		Functions like dojo.contentBox() but is more reliable since it doesn't have
        //		to wait for the browser to compute sizes.
        var cs = dojo.getComputedStyle(node);
        var me = dojo._getMarginExtents(node, cs);
        var pb = dojo._getPadBorderExtents(node, cs);
        return {
            l: dojo._toPixelValue(node, cs.paddingLeft),
            t: dojo._toPixelValue(node, cs.paddingTop),
            w: mb.w - (me.w + pb.w),
            h: mb.h - (me.h + pb.h)
        };
    };

    (function() {
        var capitalize = function(word) {
            return word.substring(0, 1).toUpperCase() + word.substring(1);
        };

        var size = function(widget, dim) {
            // size the child
            widget.resize ? widget.resize(dim) : dojo.marginBox(widget.domNode, dim);

            // record child's size, but favor our own numbers when we have them.
            // the browser lies sometimes
            dojo.mixin(widget, dojo.marginBox(widget.domNode));
            dojo.mixin(widget, dim);
        };

        dijit.layout.layoutChildren = function(/*DomNode*/ container, /*Object*/ dim, /*Object[]*/ children) {
            // summary
            //		Layout a bunch of child dom nodes within a parent dom node
            // container:
            //		parent node
            // dim:
            //		{l, t, w, h} object specifying dimensions of container into which to place children
            // children:
            //		an array like [ {domNode: foo, layoutAlign: "bottom" }, {domNode: bar, layoutAlign: "client"} ]

            // copy dim because we are going to modify it
            dim = dojo.mixin({}, dim);

            dojo.addClass(container, "dijitLayoutContainer");

            // Move "client" elements to the end of the array for layout.  a11y dictates that the author
            // needs to be able to put them in the document in tab-order, but this algorithm requires that
            // client be last.
            children = dojo.filter(children, function(item) {
                return item.layoutAlign != "client";
            })
                    .concat(dojo.filter(children, function(item) {
                return item.layoutAlign == "client";
            }));

            // set positions/sizes
            dojo.forEach(children, function(child) {
                var elm = child.domNode,
                        pos = child.layoutAlign;

                // set elem to upper left corner of unused space; may move it later
                var elmStyle = elm.style;
                elmStyle.left = dim.l + "px";
                elmStyle.top = dim.t + "px";
                elmStyle.bottom = elmStyle.right = "auto";

                dojo.addClass(elm, "dijitAlign" + capitalize(pos));

                // set size && adjust record of remaining space.
                // note that setting the width of a <div> may affect it's height.
                if (pos == "top" || pos == "bottom") {
                    size(child, { w: dim.w });
                    dim.h -= child.h;
                    if (pos == "top") {
                        dim.t += child.h;
                    } else {
                        elmStyle.top = dim.t + dim.h + "px";
                    }
                } else if (pos == "left" || pos == "right") {
                    size(child, { h: dim.h });
                    dim.w -= child.w;
                    if (pos == "left") {
                        dim.l += child.w;
                    } else {
                        elmStyle.left = dim.l + dim.w + "px";
                    }
                } else if (pos == "client") {
                    size(child, dim);
                }
            });
        };

    })();

}

if (!dojo._hasResource["dojo.string"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.string"] = true;
    dojo.provide("dojo.string");

    /*=====
     dojo.string = {
     // summary: String utilities for Dojo
     };
     =====*/

    dojo.string.rep = function(/*String*/str, /*Integer*/num) {
        //	summary:
        //		Efficiently replicate a string `n` times.
        //	str:
        //		the string to replicate
        //	num:
        //		number of times to replicate the string

        if (num <= 0 || !str) {
            return "";
        }

        var buf = [];
        for (; ;) {
            if (num & 1) {
                buf.push(str);
            }
            if (!(num >>= 1)) {
                break;
            }
            str += str;
        }
        return buf.join("");	// String
    };

    dojo.string.pad = function(/*String*/text, /*Integer*/size, /*String?*/ch, /*Boolean?*/end) {
        //	summary:
        //		Pad a string to guarantee that it is at least `size` length by
        //		filling with the character `ch` at either the start or end of the
        //		string. Pads at the start, by default.
        //	text:
        //		the string to pad
        //	size:
        //		length to provide padding
        //	ch:
        //		character to pad, defaults to '0'
        //	end:
        //		adds padding at the end if true, otherwise pads at start
        //	example:
        //	|	// Fill the string to length 10 with "+" characters on the right.  Yields "Dojo++++++".
        //	|	dojo.string.pad("Dojo", 10, "+", true);

        if (!ch) {
            ch = '0';
        }
        var out = String(text),
                pad = dojo.string.rep(ch, Math.ceil((size - out.length) / ch.length));
        return end ? out + pad : pad + out;	// String
    };

    dojo.string.substitute = function(/*String*/        template,
        /*Object|Array*/map,
        /*Function?*/    transform,
        /*Object?*/        thisObject) {
        //	summary:
        //		Performs parameterized substitutions on a string. Throws an
        //		exception if any parameter is unmatched.
        //	template:
        //		a string with expressions in the form `${key}` to be replaced or
        //		`${key:format}` which specifies a format function. keys are case-sensitive.
        //	map:
        //		hash to search for substitutions
        //	transform:
        //		a function to process all parameters before substitution takes
        //		place, e.g. dojo.string.encodeXML
        //	thisObject:
        //		where to look for optional format function; default to the global
        //		namespace
        //	example:
        //	|	// returns "File 'foo.html' is not found in directory '/temp'."
        //	|	dojo.string.substitute(
        //	|		"File '${0}' is not found in directory '${1}'.",
        //	|		["foo.html","/temp"]
        //	|	);
        //	|
        //	|	// also returns "File 'foo.html' is not found in directory '/temp'."
        //	|	dojo.string.substitute(
        //	|		"File '${name}' is not found in directory '${info.dir}'.",
        //	|		{ name: "foo.html", info: { dir: "/temp" } }
        //	|	);
        //	example:
        //		use a transform function to modify the values:
        //	|	// returns "file 'foo.html' is not found in directory '/temp'."
        //	|	dojo.string.substitute(
        //	|		"${0} is not found in ${1}.",
        //	|		["foo.html","/temp"],
        //	|		function(str){
        //	|			// try to figure out the type
        //	|			var prefix = (str.charAt(0) == "/") ? "directory": "file";
        //	|			return prefix + " '" + str + "'";
        //	|		}
        //	|	);
        //	example:
        //		use a formatter
        //	|	// returns "thinger -- howdy"
        //	|	dojo.string.substitute(
        //	|		"${0:postfix}", ["thinger"], null, {
        //	|			postfix: function(value, key){
        //	|				return value + " -- howdy";
        //	|			}
        //	|		}
        //	|	);

        thisObject = thisObject || dojo.global;
        transform = (!transform) ?
                    function(v) {
                        return v;
                    } :
                    dojo.hitch(thisObject, transform);

        return template.replace(/\$\{([^\s\:\}]+)(?:\:([^\s\:\}]+))?\}/g, function(match, key, format) {
            var value = dojo.getObject(key, false, map);
            if (format) {
                value = dojo.getObject(format, false, thisObject).call(thisObject, value, key);
            }
            return transform(value, key).toString();
        }); // string
    };

    /*=====
     dojo.string.trim = function(str){
     //	summary:
     //		Trims whitespace from both sides of the string
     //	str: String
     //		String to be trimmed
     //	returns: String
     //		Returns the trimmed string
     //	description:
     //		This version of trim() was taken from [Steven Levithan's blog](http://blog.stevenlevithan.com/archives/faster-trim-javascript).
     //		The short yet performant version of this function is dojo.trim(),
     //		which is part of Dojo base.  Uses String.prototype.trim instead, if available.
     return "";	// String
     }
     =====*/

    dojo.string.trim = String.prototype.trim ?
                       dojo.trim : // aliasing to the native function
                       function(str) {
                           str = str.replace(/^\s+/, '');
                           for (var i = str.length - 1; i >= 0; i--) {
                               if (/\S/.test(str.charAt(i))) {
                                   str = str.substring(0, i + 1);
                                   break;
                               }
                           }
                           return str;
                       };

}

if (!dojo._hasResource["dojo.html"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.html"] = true;
    dojo.provide("dojo.html");

    // the parser might be needed..


    (function() { // private scope, sort of a namespace

        // idCounter is incremented with each instantiation to allow asignment of a unique id for tracking, logging purposes
        var idCounter = 0;

        dojo.html._secureForInnerHtml = function(/*String*/ cont) {
            // summary:
            //		removes !DOCTYPE and title elements from the html string.
            //
            //		khtml is picky about dom faults, you can't attach a style or <title> node as child of body
            //		must go into head, so we need to cut out those tags
            //	cont:
            //		An html string for insertion into the dom
            //
            return cont.replace(/(?:\s*<!DOCTYPE\s[^>]+>|<title[^>]*>[\s\S]*?<\/title>)/ig, ""); // String
        };

        /*====
         dojo.html._emptyNode = function(node){
         // summary:
         //		removes all child nodes from the given node
         //	node: DOMNode
         //		the parent element
         };
         =====*/
        dojo.html._emptyNode = dojo.empty;

        dojo.html._setNodeContent = function(/* DomNode */ node, /* String|DomNode|NodeList */ cont, /* Boolean? */ shouldEmptyFirst) {
            // summary:
            //		inserts the given content into the given node
            //		overlaps similiar functionality in dijit.layout.ContentPane._setContent
            //	node:
            //		the parent element
            //	content:
            //		the content to be set on the parent element.
            //		This can be an html string, a node reference or a NodeList, dojo.NodeList, Array or other enumerable list of nodes
            // shouldEmptyFirst
            //		if shouldEmptyFirst is true, the node will first be emptied of all content before the new content is inserted
            //		defaults to false
            if (shouldEmptyFirst) {
                dojo.html._emptyNode(node);
            }

            if (typeof cont == "string") {
                // there's some hoops to jump through before we can set innerHTML on the would-be parent element.

                // rationale for this block:
                // if node is a table derivate tag, some browsers dont allow innerHTML on those
                // TODO: <select>, <dl>? what other elements will give surprises if you naively set innerHTML?

                var pre = '', post = '', walk = 0, name = node.nodeName.toLowerCase();
                switch (name) {
                    case 'tr':
                        pre = '<tr>'; post = '</tr>';
                        walk += 1;//fallthrough
                    case 'tbody': case 'thead':// children of THEAD is of same type as TBODY
                    pre = '<tbody>' + pre; post += '</tbody>';
                    walk += 1;// falltrough
                    case 'table':
                        pre = '<table>' + pre; post += '</table>';
                        walk += 1;
                        break;
                }
                if (walk) {
                    var n = node.ownerDocument.createElement('div');
                    n.innerHTML = pre + cont + post;
                    do{
                        n = n.firstChild;
                    } while (--walk);
                    // now we can safely add the child nodes...
                    dojo.forEach(n.childNodes, function(n) {
                        node.appendChild(n.cloneNode(true));
                    });
                } else {
                    // innerHTML the content as-is into the node (element)
                    // should we ever support setting content on non-element node types?
                    // e.g. text nodes, comments, etc.?
                    node.innerHTML = cont;
                }

            } else {
                // DomNode or NodeList
                if (cont.nodeType) { // domNode (htmlNode 1 or textNode 3)
                    node.appendChild(cont);
                } else {// nodelist or array such as dojo.Nodelist
                    dojo.forEach(cont, function(n) {
                        node.appendChild(n.cloneNode(true));
                    });
                }
            }
            // return DomNode
            return node;
        };

        // we wrap up the content-setting operation in a object
        dojo.declare("dojo.html._ContentSetter", null,
        {
            // node: DomNode|String
            //		An node which will be the parent element that we set content into
            node: "",

            // content: String|DomNode|DomNode[]
            //		The content to be placed in the node. Can be an HTML string, a node reference, or a enumerable list of nodes
            content: "",

            // id: String?
            //		Usually only used internally, and auto-generated with each instance
            id: "",

            // cleanContent: Boolean
            //		Should the content be treated as a full html document,
            //		and the real content stripped of <html>, <body> wrapper before injection
            cleanContent: false,

            // extractContent: Boolean
            //		Should the content be treated as a full html document, and the real content stripped of <html>, <body> wrapper before injection
            extractContent: false,

            // parseContent: Boolean
            //		Should the node by passed to the parser after the new content is set
            parseContent: false,

            // lifecyle methods
            constructor: function(/* Object */params, /* String|DomNode */node) {
                //	summary:
                //		Provides a configurable, extensible object to wrap the setting on content on a node
                //		call the set() method to actually set the content..

                // the original params are mixed directly into the instance "this"
                dojo.mixin(this, params || {});

                // give precedence to params.node vs. the node argument
                // and ensure its a node, not an id string
                node = this.node = dojo.byId(this.node || node);

                if (!this.id) {
                    this.id = [
                        "Setter",
                        (node) ? node.id || node.tagName : "",
                        idCounter++
                    ].join("_");
                }

                if (! (this.node || node)) {
                    new Error(this.declaredClass + ": no node provided to " + this.id);
                }
            },
            set: function(/* String|DomNode|NodeList? */ cont, /* Object? */ params) {
                // summary:
                //		front-end to the set-content sequence
                //	cont:
                //		An html string, node or enumerable list of nodes for insertion into the dom
                //		If not provided, the object's content property will be used
                if (undefined !== cont) {
                    this.content = cont;
                }
                // in the re-use scenario, set needs to be able to mixin new configuration
                if (params) {
                    this._mixin(params);
                }

                this.onBegin();
                this.setContent();
                this.onEnd();

                return this.node;
            },
            setContent: function() {
                // summary:
                //		sets the content on the node

                var node = this.node;
                if (!node) {
                    console.error("setContent given no node");
                }
                try {
                    node = dojo.html._setNodeContent(node, this.content);
                } catch(e) {
                    // check if a domfault occurs when we are appending this.errorMessage
                    // like for instance if domNode is a UL and we try append a DIV

                    // FIXME: need to allow the user to provide a content error message string
                    var errMess = this.onContentError(e);
                    try {
                        node.innerHTML = errMess;
                    } catch(e) {
                        console.error('Fatal ' + this.declaredClass + '.setContent could not change content due to ' + e.message, e);
                    }
                }
                // always put back the node for the next method
                this.node = node; // DomNode
            },

            empty: function() {
                // summary
                //	cleanly empty out existing content

                // destroy any widgets from a previous run
                // NOTE: if you dont want this you'll need to empty
                // the parseResults array property yourself to avoid bad things happenning
                if (this.parseResults && this.parseResults.length) {
                    dojo.forEach(this.parseResults, function(w) {
                        if (w.destroy) {
                            w.destroy();
                        }
                    });
                    delete this.parseResults;
                }
                // this is fast, but if you know its already empty or safe, you could
                // override empty to skip this step
                dojo.html._emptyNode(this.node);
            },

            onBegin: function() {
                // summary
                //		Called after instantiation, but before set();
                //		It allows modification of any of the object properties
                //		- including the node and content provided - before the set operation actually takes place
                //		This default implementation checks for cleanContent and extractContent flags to
                //		optionally pre-process html string content
                var cont = this.content;

                if (dojo.isString(cont)) {
                    if (this.cleanContent) {
                        cont = dojo.html._secureForInnerHtml(cont);
                    }

                    if (this.extractContent) {
                        var match = cont.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
                        if (match) {
                            cont = match[1];
                        }
                    }
                }

                // clean out the node and any cruft associated with it - like widgets
                this.empty();

                this.content = cont;
                return this.node;
                /* DomNode */
            },

            onEnd: function() {
                // summary
                //		Called after set(), when the new content has been pushed into the node
                //		It provides an opportunity for post-processing before handing back the node to the caller
                //		This default implementation checks a parseContent flag to optionally run the dojo parser over the new content
                if (this.parseContent) {
                    // populates this.parseResults if you need those..
                    this._parse();
                }
                return this.node;
                /* DomNode */
            },

            tearDown: function() {
                // summary
                //		manually reset the Setter instance if its being re-used for example for another set()
                // description
                //		tearDown() is not called automatically.
                //		In normal use, the Setter instance properties are simply allowed to fall out of scope
                //		but the tearDown method can be called to explicitly reset this instance.
                delete this.parseResults;
                delete this.node;
                delete this.content;
            },

            onContentError: function(err) {
                return "Error occured setting content: " + err;
            },

            _mixin: function(params) {
                // mix properties/methods into the instance
                // TODO: the intention with tearDown is to put the Setter's state
                // back to that of the original constructor (vs. deleting/resetting everything regardless of ctor params)
                // so we could do something here to move the original properties aside for later restoration
                var empty = {}, key;
                for (key in params) {
                    if (key in empty) {
                        continue;
                    }
                    // TODO: here's our opportunity to mask the properties we dont consider configurable/overridable
                    // .. but history shows we'll almost always guess wrong
                    this[key] = params[key];
                }
            },
            _parse: function() {
                // summary:
                //		runs the dojo parser over the node contents, storing any results in this.parseResults
                //		Any errors resulting from parsing are passed to _onError for handling

                var rootNode = this.node;
                try {
                    // store the results (widgets, whatever) for potential retrieval
                    this.parseResults = dojo.parser.parse(rootNode, true);
                } catch(e) {
                    this._onError('Content', e, "Error parsing in _ContentSetter#" + this.id);
                }
            },

            _onError: function(type, err, consoleText) {
                // summary:
                //		shows user the string that is returned by on[type]Error
                //		overide/implement on[type]Error and return your own string to customize
                var errText = this['on' + type + 'Error'].call(this, err);
                if (consoleText) {
                    console.error(consoleText, err);
                } else if (errText) { // a empty string won't change current content
                    dojo.html._setNodeContent(this.node, errText, true);
                }
            }
        }); // end dojo.declare()

        dojo.html.set = function(/* DomNode */ node, /* String|DomNode|NodeList */ cont, /* Object? */ params) {
            // summary:
            //		inserts (replaces) the given content into the given node. dojo.place(cont, node, "only")
            //		may be a better choice for simple HTML insertion.
            // description:
            //		Unless you need to use the params capabilities of this method, you should use
            //		dojo.place(cont, node, "only"). dojo.place() has more robust support for injecting
            //		an HTML string into the DOM, but it only handles inserting an HTML string as DOM
            //		elements, or inserting a DOM node. dojo.place does not handle NodeList insertions
            //		or the other capabilities as defined by the params object for this method.
            //	node:
            //		the parent element that will receive the content
            //	cont:
            //		the content to be set on the parent element.
            //		This can be an html string, a node reference or a NodeList, dojo.NodeList, Array or other enumerable list of nodes
            //	params:
            //		Optional flags/properties to configure the content-setting. See dojo.html._ContentSetter
            //	example:
            //		A safe string/node/nodelist content replacement/injection with hooks for extension
            //		Example Usage:
            //		dojo.html.set(node, "some string");
            //		dojo.html.set(node, contentNode, {options});
            //		dojo.html.set(node, myNode.childNodes, {options});
            if (undefined == cont) {
                console.warn("dojo.html.set: no cont argument provided, using empty string");
                cont = "";
            }
            if (!params) {
                // simple and fast
                return dojo.html._setNodeContent(node, cont, true);
            } else {
                // more options but slower
                // note the arguments are reversed in order, to match the convention for instantiation via the parser
                var op = new dojo.html._ContentSetter(dojo.mixin(
                        params,
                { content: cont, node: node }
                        ));
                return op.set();
            }
        };
    })();

}

if (!dojo._hasResource["dojo.i18n"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.i18n"] = true;
    dojo.provide("dojo.i18n");

    /*=====
     dojo.i18n = {
     // summary: Utility classes to enable loading of resources for internationalization (i18n)
     };
     =====*/

    dojo.i18n.getLocalization = function(/*String*/packageName, /*String*/bundleName, /*String?*/locale) {
        //	summary:
        //		Returns an Object containing the localization for a given resource
        //		bundle in a package, matching the specified locale.
        //	description:
        //		Returns a hash containing name/value pairs in its prototypesuch
        //		that values can be easily overridden.  Throws an exception if the
        //		bundle is not found.  Bundle must have already been loaded by
        //		`dojo.requireLocalization()` or by a build optimization step.  NOTE:
        //		try not to call this method as part of an object property
        //		definition (`var foo = { bar: dojo.i18n.getLocalization() }`).  In
        //		some loading situations, the bundle may not be available in time
        //		for the object definition.  Instead, call this method inside a
        //		function that is run after all modules load or the page loads (like
        //		in `dojo.addOnLoad()`), or in a widget lifecycle method.
        //	packageName:
        //		package which is associated with this resource
        //	bundleName:
        //		the base filename of the resource bundle (without the ".js" suffix)
        //	locale:
        //		the variant to load (optional).  By default, the locale defined by
        //		the host environment: dojo.locale

        locale = dojo.i18n.normalizeLocale(locale);

        // look for nearest locale match
        var elements = locale.split('-');
        var module = [packageName,"nls",bundleName].join('.');
        var bundle = dojo._loadedModules[module];
        if (bundle) {
            var localization;
            for (var i = elements.length; i > 0; i--) {
                var loc = elements.slice(0, i).join('_');
                if (bundle[loc]) {
                    localization = bundle[loc];
                    break;
                }
            }
            if (!localization) {
                localization = bundle.ROOT;
            }

            // make a singleton prototype so that the caller won't accidentally change the values globally
            if (localization) {
                var clazz = function() {
                };
                clazz.prototype = localization;
                return new clazz(); // Object
            }
        }

        throw new Error("Bundle not found: " + bundleName + " in " + packageName + " , locale=" + locale);
    };

    dojo.i18n.normalizeLocale = function(/*String?*/locale) {
        //	summary:
        //		Returns canonical form of locale, as used by Dojo.
        //
        //  description:
        //		All variants are case-insensitive and are separated by '-' as specified in [RFC 3066](http://www.ietf.org/rfc/rfc3066.txt).
        //		If no locale is specified, the dojo.locale is returned.  dojo.locale is defined by
        //		the user agent's locale unless overridden by djConfig.

        var result = locale ? locale.toLowerCase() : dojo.locale;
        if (result == "root") {
            result = "ROOT";
        }
        return result; // String
    };

    dojo.i18n._requireLocalization = function(/*String*/moduleName, /*String*/bundleName, /*String?*/locale, /*String?*/availableFlatLocales) {
        //	summary:
        //		See dojo.requireLocalization()
        //	description:
        // 		Called by the bootstrap, but factored out so that it is only
        // 		included in the build when needed.

        var targetLocale = dojo.i18n.normalizeLocale(locale);
        var bundlePackage = [moduleName, "nls", bundleName].join(".");
        // NOTE:
        //		When loading these resources, the packaging does not match what is
        //		on disk.  This is an implementation detail, as this is just a
        //		private data structure to hold the loaded resources.  e.g.
        //		`tests/hello/nls/en-us/salutations.js` is loaded as the object
        //		`tests.hello.nls.salutations.en_us={...}` The structure on disk is
        //		intended to be most convenient for developers and translators, but
        //		in memory it is more logical and efficient to store in a different
        //		order.  Locales cannot use dashes, since the resulting path will
        //		not evaluate as valid JS, so we translate them to underscores.

        //Find the best-match locale to load if we have available flat locales.
        var bestLocale = "";
        if (availableFlatLocales) {
            var flatLocales = availableFlatLocales.split(",");
            for (var i = 0; i < flatLocales.length; i++) {
                //Locale must match from start of string.
                //Using ["indexOf"] so customBase builds do not see
                //this as a dojo._base.array dependency.
                if (targetLocale["indexOf"](flatLocales[i]) == 0) {
                    if (flatLocales[i].length > bestLocale.length) {
                        bestLocale = flatLocales[i];
                    }
                }
            }
            if (!bestLocale) {
                bestLocale = "ROOT";
            }
        }

        //See if the desired locale is already loaded.
        var tempLocale = availableFlatLocales ? bestLocale : targetLocale;
        var bundle = dojo._loadedModules[bundlePackage];
        var localizedBundle = null;
        if (bundle) {
            if (dojo.config.localizationComplete && bundle._built) {
                return;
            }
            var jsLoc = tempLocale.replace(/-/g, '_');
            var translationPackage = bundlePackage + "." + jsLoc;
            localizedBundle = dojo._loadedModules[translationPackage];
        }

        if (!localizedBundle) {
            bundle = dojo["provide"](bundlePackage);
            var syms = dojo._getModuleSymbols(moduleName);
            var modpath = syms.concat("nls").join("/");
            var parent;

            dojo.i18n._searchLocalePath(tempLocale, availableFlatLocales, function(loc) {
                var jsLoc = loc.replace(/-/g, '_');
                var translationPackage = bundlePackage + "." + jsLoc;
                var loaded = false;
                if (!dojo._loadedModules[translationPackage]) {
                    // Mark loaded whether it's found or not, so that further load attempts will not be made
                    dojo["provide"](translationPackage);
                    var module = [modpath];
                    if (loc != "ROOT") {
                        module.push(loc);
                    }
                    module.push(bundleName);
                    var filespec = module.join("/") + '.js';
                    loaded = dojo._loadPath(filespec, null, function(hash) {
                        // Use singleton with prototype to point to parent bundle, then mix-in result from loadPath
                        var clazz = function() {
                        };
                        clazz.prototype = parent;
                        bundle[jsLoc] = new clazz();
                        for (var j in hash) {
                            bundle[jsLoc][j] = hash[j];
                        }
                    });
                } else {
                    loaded = true;
                }
                if (loaded && bundle[jsLoc]) {
                    parent = bundle[jsLoc];
                } else {
                    bundle[jsLoc] = parent;
                }

                if (availableFlatLocales) {
                    //Stop the locale path searching if we know the availableFlatLocales, since
                    //the first call to this function will load the only bundle that is needed.
                    return true;
                }
            });
        }

        //Save the best locale bundle as the target locale bundle when we know the
        //the available bundles.
        if (availableFlatLocales && targetLocale != bestLocale) {
            bundle[targetLocale.replace(/-/g, '_')] = bundle[bestLocale.replace(/-/g, '_')];
        }
    };

    (function() {
        // If other locales are used, dojo.requireLocalization should load them as
        // well, by default.
        //
        // Override dojo.requireLocalization to do load the default bundle, then
        // iterate through the extraLocale list and load those translations as
        // well, unless a particular locale was requested.

        var extra = dojo.config.extraLocale;
        if (extra) {
            if (!extra instanceof Array) {
                extra = [extra];
            }

            var req = dojo.i18n._requireLocalization;
            dojo.i18n._requireLocalization = function(m, b, locale, availableFlatLocales) {
                req(m, b, locale, availableFlatLocales);
                if (locale) {
                    return;
                }
                for (var i = 0; i < extra.length; i++) {
                    req(m, b, extra[i], availableFlatLocales);
                }
            };
        }
    })();

    dojo.i18n._searchLocalePath = function(/*String*/locale, /*Boolean*/down, /*Function*/searchFunc) {
        //	summary:
        //		A helper method to assist in searching for locale-based resources.
        //		Will iterate through the variants of a particular locale, either up
        //		or down, executing a callback function.  For example, "en-us" and
        //		true will try "en-us" followed by "en" and finally "ROOT".

        locale = dojo.i18n.normalizeLocale(locale);

        var elements = locale.split('-');
        var searchlist = [];
        for (var i = elements.length; i > 0; i--) {
            searchlist.push(elements.slice(0, i).join('-'));
        }
        searchlist.push(false);
        if (down) {
            searchlist.reverse();
        }

        for (var j = searchlist.length - 1; j >= 0; j--) {
            var loc = searchlist[j] || "ROOT";
            var stop = searchFunc(loc);
            if (stop) {
                break;
            }
        }
    };

    dojo.i18n._preloadLocalizations = function(/*String*/bundlePrefix, /*Array*/localesGenerated) {
        //	summary:
        //		Load built, flattened resource bundles, if available for all
        //		locales used in the page. Only called by built layer files.

        function preload(locale) {
            locale = dojo.i18n.normalizeLocale(locale);
            dojo.i18n._searchLocalePath(locale, true, function(loc) {
                for (var i = 0; i < localesGenerated.length; i++) {
                    if (localesGenerated[i] == loc) {
                        dojo["require"](bundlePrefix + "_" + loc);
                        return true; // Boolean
                    }
                }
                return false; // Boolean
            });
        }

        preload();
        var extra = dojo.config.extraLocale || [];
        for (var i = 0; i < extra.length; i++) {
            preload(extra[i]);
        }
    };

}

if (!dojo._hasResource["dijit.layout.ContentPane"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.layout.ContentPane"] = true;
    dojo.provide("dijit.layout.ContentPane");


    // for dijit.layout.marginBox2contentBox()


    dojo.declare(
            "dijit.layout.ContentPane", dijit._Widget,
    {
        // summary:
        //		A widget that acts as a container for mixed HTML and widgets, and includes an Ajax interface
        // description:
        //		A widget that can be used as a standalone widget
        //		or as a baseclass for other widgets
        //		Handles replacement of document fragment using either external uri or javascript
        //		generated markup or DOM content, instantiating widgets within that content.
        //		Don't confuse it with an iframe, it only needs/wants document fragments.
        //		It's useful as a child of LayoutContainer, SplitContainer, or TabContainer.
        //		But note that those classes can contain any widget as a child.
        // example:
        //		Some quick samples:
        //		To change the innerHTML use .attr('content', '<b>new content</b>')
        //
        //		Or you can send it a NodeList, .attr('content', dojo.query('div [class=selected]', userSelection))
        //		please note that the nodes in NodeList will copied, not moved
        //
        //		To do a ajax update use .attr('href', url)

        // href: String
        //		The href of the content that displays now.
        //		Set this at construction if you want to load data externally when the
        //		pane is shown.	(Set preload=true to load it immediately.)
        //		Changing href after creation doesn't have any effect; use attr('href', ...);
        href: "",

        /*=====
         // content: String || DomNode || NodeList || dijit._Widget
         //		The innerHTML of the ContentPane.
         //		Note that the initialization parameter / argument to attr("content", ...)
         //		can be a String, DomNode, Nodelist, or _Widget.
         content: "",
         =====*/

        // extractContent: Boolean
        //		Extract visible content from inside of <body> .... </body>.
        //		I.e., strip <html> and <head> (and it's contents) from the href
        extractContent: false,

        // parseOnLoad: Boolean
        //		Parse content and create the widgets, if any.
        parseOnLoad:    true,

        // preventCache: Boolean
        //		Prevent caching of data from href's by appending a timestamp to the href.
        preventCache:    false,

        // preload: Boolean
        //		Force load of data on initialization even if pane is hidden.
        preload: false,

        // refreshOnShow: Boolean
        //		Refresh (re-download) content when pane goes from hidden to shown
        refreshOnShow: false,

        // loadingMessage: String
        //		Message that shows while downloading
        loadingMessage: "<span class='dijitContentPaneLoading'>${loadingState}</span>",

        // errorMessage: String
        //		Message that shows if an error occurs
        errorMessage: "<span class='dijitContentPaneError'>${errorState}</span>",

        // isLoaded: [readonly] Boolean
        //		True if the ContentPane has data in it, either specified
        //		during initialization (via href or inline content), or set
        //		via attr('content', ...) / attr('href', ...)
        //
        //		False if it doesn't have any content, or if ContentPane is
        //		still in the process of downloading href.
        isLoaded: false,

        baseClass: "dijitContentPane",

        // doLayout: Boolean
        //		- false - don't adjust size of children
        //		- true - if there is a single visible child widget, set it's size to
        //				however big the ContentPane is
        doLayout: true,

        // ioArgs: Object
        //		Parameters to pass to xhrGet() request, for example:
        // |	<div dojoType="dijit.layout.ContentPane" href="./bar" ioArgs="{timeout: 500}">
        ioArgs: {},

        // isContainer: [protected] Boolean
        //		Just a flag indicating that this widget will call resize() on
        //		its children.   _LayoutWidget based widgets check for
        //
        //	|		if(!this.getParent || !this.getParent()){
        //
        //		and if getParent() returns false because !parent.isContainer,
        //		then they resize themselves on initialization.
        isContainer: true,

        postMixInProperties: function() {
            this.inherited(arguments);
            var messages = dojo.i18n.getLocalization("dijit", "loading", this.lang);
            this.loadingMessage = dojo.string.substitute(this.loadingMessage, messages);
            this.errorMessage = dojo.string.substitute(this.errorMessage, messages);

            // Detect if we were initialized with data
            if (!this.href && this.srcNodeRef && this.srcNodeRef.innerHTML) {
                this.isLoaded = true;
            }
        },

        buildRendering: function() {
            // Overrides Widget.buildRendering().
            // Since we have no template we need to set this.containerNode ourselves.
            // For subclasses of ContentPane do have a template, does nothing.
            this.inherited(arguments);
            if (!this.containerNode) {
                // make getDescendants() work
                this.containerNode = this.domNode;
            }
        },

        postCreate: function() {
            // remove the title attribute so it doesn't show up when hovering
            // over a node
            this.domNode.title = "";

            if (!dojo.attr(this.domNode, "role")) {
                dijit.setWaiRole(this.domNode, "group");
            }

            dojo.addClass(this.domNode, this.baseClass);
        },

        startup: function() {
            // summary:
            //		See `dijit.layout._LayoutWidget.startup` for description.
            //		Although ContentPane doesn't extend _LayoutWidget, it does implement
            //		the same API.
            if (this._started) {
                return;
            }

            if (this.isLoaded) {
                dojo.forEach(this.getChildren(), function(child) {
                    child.startup();
                });

                // If we have static content in the content pane (specified during
                // initialization) then we need to do layout now... unless we are
                // a child of a TabContainer etc. in which case wait until the TabContainer
                // calls resize() on us.
                if (this.doLayout) {
                    this._checkIfSingleChild();
                }
                if (!this._singleChild || !dijit._Contained.prototype.getParent.call(this)) {
                    this._scheduleLayout();
                }
            }

            // If we have an href then check if we should load it now
            this._loadCheck();

            this.inherited(arguments);
        },

        _checkIfSingleChild: function() {
            // summary:
            //		Test if we have exactly one visible widget as a child,
            //		and if so assume that we are a container for that widget,
            //		and should propogate startup() and resize() calls to it.
            //		Skips over things like data stores since they aren't visible.

            var childNodes = dojo.query(">", this.containerNode),
                    childWidgetNodes = childNodes.filter(function(node) {
                        return dojo.hasAttr(node, "dojoType") || dojo.hasAttr(node, "widgetId");
                    }),
                    candidateWidgets = dojo.filter(childWidgetNodes.map(dijit.byNode), function(widget) {
                        return widget && widget.domNode && widget.resize;
                    });

            if (
            // all child nodes are widgets
                    childNodes.length == childWidgetNodes.length &&

                        // all but one are invisible (like dojo.data)
                    candidateWidgets.length == 1
                    ) {
                this._singleChild = candidateWidgets[0];
            } else {
                delete this._singleChild;
            }
        },

        setHref: function(/*String|Uri*/ href) {
            // summary:
            //		Deprecated.   Use attr('href', ...) instead.
            dojo.deprecated("dijit.layout.ContentPane.setHref() is deprecated. Use attr('href', ...) instead.", "", "2.0");
            return this.attr("href", href);
        },
        _setHrefAttr: function(/*String|Uri*/ href) {
            // summary:
            //		Hook so attr("href", ...) works.
            // description:
            //		Reset the (external defined) content of this pane and replace with new url
            //		Note: It delays the download until widget is shown if preload is false.
            //	href:
            //		url to the page you want to get, must be within the same domain as your mainpage

            // Cancel any in-flight requests (an attr('href') will cancel any in-flight attr('href', ...))
            this.cancel();

            this.href = href;

            // _setHrefAttr() is called during creation and by the user, after creation.
            // only in the second case do we actually load the URL; otherwise it's done in startup()
            if (this._created && (this.preload || this._isShown())) {
                // we return result of refresh() here to avoid code dup. in dojox.layout.ContentPane
                return this.refresh();
            } else {
                // Set flag to indicate that href needs to be loaded the next time the
                // ContentPane is made visible
                this._hrefChanged = true;
            }
        },

        setContent: function(/*String|DomNode|Nodelist*/data) {
            // summary:
            //		Deprecated.   Use attr('content', ...) instead.
            dojo.deprecated("dijit.layout.ContentPane.setContent() is deprecated.  Use attr('content', ...) instead.", "", "2.0");
            this.attr("content", data);
        },
        _setContentAttr: function(/*String|DomNode|Nodelist*/data) {
            // summary:
            //		Hook to make attr("content", ...) work.
            //		Replaces old content with data content, include style classes from old content
            //	data:
            //		the new Content may be String, DomNode or NodeList
            //
            //		if data is a NodeList (or an array of nodes) nodes are copied
            //		so you can import nodes from another document implicitly

            // clear href so we can't run refresh and clear content
            // refresh should only work if we downloaded the content
            this.href = "";

            // Cancel any in-flight requests (an attr('content') will cancel any in-flight attr('href', ...))
            this.cancel();

            this._setContent(data || "");

            this._isDownloaded = false; // mark that content is from a attr('content') not an attr('href')
        },
        _getContentAttr: function() {
            // summary:
            //		Hook to make attr("content") work
            return this.containerNode.innerHTML;
        },

        cancel: function() {
            // summary:
            //		Cancels an in-flight download of content
            if (this._xhrDfd && (this._xhrDfd.fired == -1)) {
                this._xhrDfd.cancel();
            }
            delete this._xhrDfd; // garbage collect
        },

        uninitialize: function() {
            if (this._beingDestroyed) {
                this.cancel();
            }
        },

        destroyRecursive: function(/*Boolean*/ preserveDom) {
            // summary:
            //		Destroy the ContentPane and its contents

            // if we have multiple controllers destroying us, bail after the first
            if (this._beingDestroyed) {
                return;
            }
            this._beingDestroyed = true;
            this.inherited(arguments);
        },

        resize: function(size) {
            // summary:
            //		See `dijit.layout._LayoutWidget.resize` for description.
            //		Although ContentPane doesn't extend _LayoutWidget, it does implement
            //		the same API.

            dojo.marginBox(this.domNode, size);

            // Compute content box size in case we [later] need to size child
            // If either height or width wasn't specified by the user, then query node for it.
            // But note that setting the margin box and then immediately querying dimensions may return
            // inaccurate results, so try not to depend on it.
            var node = this.containerNode,
                    mb = dojo.mixin(dojo.marginBox(node), size || {});

            var cb = (this._contentBox = dijit.layout.marginBox2contentBox(node, mb));

            // If we have a single widget child then size it to fit snugly within my borders
            if (this._singleChild && this._singleChild.resize) {
                // note: if widget has padding this._contentBox will have l and t set,
                // but don't pass them to resize() or it will doubly-offset the child
                this._singleChild.resize({w: cb.w, h: cb.h});
            }
        },

        _isShown: function() {
            // summary:
            //		Returns true if the content is currently shown
            if ("open" in this) {
                return this.open;		// for TitlePane, etc.
            } else {
                var node = this.domNode;
                return (node.style.display != 'none') && (node.style.visibility != 'hidden') && !dojo.hasClass(node, "dijitHidden");
            }
        },

        _onShow: function() {
            // summary:
            //		Called when the ContentPane is made visible
            // description:
            //		For a plain ContentPane, this is called on initialization, from startup().
            //		If the ContentPane is a hidden pane of a TabContainer etc., then it's
            //		called whever the pane is made visible.
            //
            //		Does processing necessary, including href download and layout/resize of
            //		child widget(s)

            if (this._needLayout) {
                // If a layout has been scheduled for when we become visible, do it now
                this._layoutChildren();
            }

            // Do lazy-load of URL
            this._loadCheck();

            // call onShow, if we have one
            if (this.onShow) {
                this.onShow();
            }
        },

        _loadCheck: function() {
            // summary:
            //		Call this to load href contents if necessary.
            // description:
            //		Call when !ContentPane has been made visible [from prior hidden state],
            //		or href has been changed, or on startup, etc.

            if (
                    (this.href && !this._xhrDfd) && // if there's an href that isn't already being loaded
                    (!this.isLoaded || this._hrefChanged || this.refreshOnShow) && // and we need a [re]load
                    (this.preload || this._isShown())    // and now is the time to [re]load
                    ) {
                delete this._hrefChanged;
                this.refresh();
            }
        },

        refresh: function() {
            // summary:
            //		[Re]download contents of href and display
            // description:
            //		1. cancels any currently in-flight requests
            //		2. posts "loading..." message
            //		3. sends XHR to download new data

            // cancel possible prior inflight request
            this.cancel();

            // display loading message
            this._setContent(this.onDownloadStart(), true);

            var self = this;
            var getArgs = {
                preventCache: (this.preventCache || this.refreshOnShow),
                url: this.href,
                handleAs: "text"
            };
            if (dojo.isObject(this.ioArgs)) {
                dojo.mixin(getArgs, this.ioArgs);
            }

            var hand = (this._xhrDfd = (this.ioMethod || dojo.xhrGet)(getArgs));

            hand.addCallback(function(html) {
                try {
                    self._isDownloaded = true;
                    self._setContent(html, false);
                    self.onDownloadEnd();
                } catch(err) {
                    self._onError('Content', err); // onContentError
                }
                delete self._xhrDfd;
                return html;
            });

            hand.addErrback(function(err) {
                if (!hand.canceled) {
                    // show error message in the pane
                    self._onError('Download', err); // onDownloadError
                }
                delete self._xhrDfd;
                return err;
            });
        },

        _onLoadHandler: function(data) {
            // summary:
            //		This is called whenever new content is being loaded
            this.isLoaded = true;
            try {
                this.onLoad(data);
            } catch(e) {
                console.error('Error ' + this.widgetId + ' running custom onLoad code: ' + e.message);
            }
        },

        _onUnloadHandler: function() {
            // summary:
            //		This is called whenever the content is being unloaded
            this.isLoaded = false;
            try {
                this.onUnload();
            } catch(e) {
                console.error('Error ' + this.widgetId + ' running custom onUnload code: ' + e.message);
            }
        },

        destroyDescendants: function() {
            // summary:
            //		Destroy all the widgets inside the ContentPane and empty containerNode

            // Make sure we call onUnload (but only when the ContentPane has real content)
            if (this.isLoaded) {
                this._onUnloadHandler();
            }

            // Even if this.isLoaded == false there might still be a "Loading..." message
            // to erase, so continue...

            // For historical reasons we need to delete all widgets under this.containerNode,
            // even ones that the user has created manually.
            var setter = this._contentSetter;
            dojo.forEach(this.getChildren(), function(widget) {
                if (widget.destroyRecursive) {
                    widget.destroyRecursive();
                }
            });
            if (setter) {
                // Most of the widgets in setter.parseResults have already been destroyed, but
                // things like Menu that have been moved to <body> haven't yet
                dojo.forEach(setter.parseResults, function(widget) {
                    if (widget.destroyRecursive && widget.domNode && widget.domNode.parentNode == dojo.body()) {
                        widget.destroyRecursive();
                    }
                });
                delete setter.parseResults;
            }

            // And then clear away all the DOM nodes
            dojo.html._emptyNode(this.containerNode);
        },

        _setContent: function(cont, isFakeContent) {
            // summary:
            //		Insert the content into the container node

            // first get rid of child widgets
            this.destroyDescendants();

            // Delete any state information we have about current contents
            delete this._singleChild;

            // dojo.html.set will take care of the rest of the details
            // we provide an overide for the error handling to ensure the widget gets the errors
            // configure the setter instance with only the relevant widget instance properties
            // NOTE: unless we hook into attr, or provide property setters for each property,
            // we need to re-configure the ContentSetter with each use
            var setter = this._contentSetter;
            if (! (setter && setter instanceof dojo.html._ContentSetter)) {
                setter = this._contentSetter = new dojo.html._ContentSetter({
                    node: this.containerNode,
                    _onError: dojo.hitch(this, this._onError),
                    onContentError: dojo.hitch(this, function(e) {
                        // fires if a domfault occurs when we are appending this.errorMessage
                        // like for instance if domNode is a UL and we try append a DIV
                        var errMess = this.onContentError(e);
                        try {
                            this.containerNode.innerHTML = errMess;
                        } catch(e) {
                            console.error('Fatal ' + this.id + ' could not change content due to ' + e.message, e);
                        }
                    })/*,
                     _onError */
                });
            }
            ;

            var setterParams = dojo.mixin({
                cleanContent: this.cleanContent,
                extractContent: this.extractContent,
                parseContent: this.parseOnLoad
            }, this._contentSetterParams || {});

            dojo.mixin(setter, setterParams);

            setter.set((dojo.isObject(cont) && cont.domNode) ? cont.domNode : cont);

            // setter params must be pulled afresh from the ContentPane each time
            delete this._contentSetterParams;

            if (!isFakeContent) {
                dojo.forEach(this.getChildren(), function(child) {
                    child.startup();
                });

                if (this.doLayout) {
                    this._checkIfSingleChild();
                }

                // Call resize() on each of my child layout widgets,
                // or resize() on my single child layout widget...
                // either now (if I'm currently visible)
                // or when I become visible
                this._scheduleLayout();

                this._onLoadHandler(cont);
            }
        },

        _onError: function(type, err, consoleText) {
            // shows user the string that is returned by on[type]Error
            // overide on[type]Error and return your own string to customize
            var errText = this['on' + type + 'Error'].call(this, err);
            if (consoleText) {
                console.error(consoleText, err);
            } else if (errText) {// a empty string won't change current content
                this._setContent(errText, true);
            }
        },

        _scheduleLayout: function() {
            // summary:
            //		Call resize() on each of my child layout widgets, either now
            //		(if I'm currently visible) or when I become visible
            if (this._isShown()) {
                this._layoutChildren();
            } else {
                this._needLayout = true;
            }
        },

        _layoutChildren: function() {
            // summary:
            //		Since I am a Container widget, each of my children expects me to
            //		call resize() or layout() on them.
            // description:
            //		Should be called on initialization and also whenever we get new content
            //		(from an href, or from attr('content', ...))... but deferred until
            //		the ContentPane is visible

            if (this._singleChild && this._singleChild.resize) {
                var cb = this._contentBox || dojo.contentBox(this.containerNode);
                this._singleChild.resize({w: cb.w, h: cb.h});
            } else {
                // All my child widgets are independently sized (rather than matching my size),
                // but I still need to call resize() on each child to make it layout.
                dojo.forEach(this.getChildren(), function(widget) {
                    if (widget.resize) {
                        widget.resize();
                    }
                });
            }
            delete this._needLayout;
        },

        // EVENT's, should be overide-able
        onLoad: function(data) {
            // summary:
            //		Event hook, is called after everything is loaded and widgetified
            // tags:
            //		callback
        },

        onUnload: function() {
            // summary:
            //		Event hook, is called before old content is cleared
            // tags:
            //		callback
        },

        onDownloadStart: function() {
            // summary:
            //		Called before download starts.
            // description:
            //		The string returned by this function will be the html
            //		that tells the user we are loading something.
            //		Override with your own function if you want to change text.
            // tags:
            //		extension
            return this.loadingMessage;
        },

        onContentError: function(/*Error*/ error) {
            // summary:
            //		Called on DOM faults, require faults etc. in content.
            //
            //		In order to display an error message in the pane, return
            //		the error message from this method, as an HTML string.
            //
            //		By default (if this method is not overriden), it returns
            //		nothing, so the error message is just printed to the console.
            // tags:
            //		extension
        },

        onDownloadError: function(/*Error*/ error) {
            // summary:
            //		Called when download error occurs.
            //
            //		In order to display an error message in the pane, return
            //		the error message from this method, as an HTML string.
            //
            //		Default behavior (if this method is not overriden) is to display
            //		the error message inside the pane.
            // tags:
            //		extension
            return this.errorMessage;
        },

        onDownloadEnd: function() {
            // summary:
            //		Called when download is finished.
            // tags:
            //		callback
        }
    });

}

if (!dojo._hasResource["dojo.regexp"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.regexp"] = true;
    dojo.provide("dojo.regexp");

    /*=====
     dojo.regexp = {
     // summary: Regular expressions and Builder resources
     };
     =====*/

    dojo.regexp.escapeString = function(/*String*/str, /*String?*/except) {
        //	summary:
        //		Adds escape sequences for special characters in regular expressions
        // except:
        //		a String with special characters to be left unescaped

        return str.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, function(ch) {
            if (except && except.indexOf(ch) != -1) {
                return ch;
            }
            return "\\" + ch;
        }); // String
    }

    dojo.regexp.buildGroupRE = function(/*Object|Array*/arr, /*Function*/re, /*Boolean?*/nonCapture) {
        //	summary:
        //		Builds a regular expression that groups subexpressions
        //	description:
        //		A utility function used by some of the RE generators. The
        //		subexpressions are constructed by the function, re, in the second
        //		parameter.  re builds one subexpression for each elem in the array
        //		a, in the first parameter. Returns a string for a regular
        //		expression that groups all the subexpressions.
        // arr:
        //		A single value or an array of values.
        // re:
        //		A function. Takes one parameter and converts it to a regular
        //		expression.
        // nonCapture:
        //		If true, uses non-capturing match, otherwise matches are retained
        //		by regular expression. Defaults to false

        // case 1: a is a single value.
        if (!(arr instanceof Array)) {
            return re(arr); // String
        }

        // case 2: a is an array
        var b = [];
        for (var i = 0; i < arr.length; i++) {
            // convert each elem to a RE
            b.push(re(arr[i]));
        }

        // join the REs as alternatives in a RE group.
        return dojo.regexp.group(b.join("|"), nonCapture); // String
    }

    dojo.regexp.group = function(/*String*/expression, /*Boolean?*/nonCapture) {
        // summary:
        //		adds group match to expression
        // nonCapture:
        //		If true, uses non-capturing match, otherwise matches are retained
        //		by regular expression.
        return "(" + (nonCapture ? "?:" : "") + expression + ")"; // String
    }

}

if (!dojo._hasResource["dojo.cookie"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.cookie"] = true;
    dojo.provide("dojo.cookie");


    /*=====
     dojo.__cookieProps = function(){
     //	expires: Date|String|Number?
     //		If a number, the number of days from today at which the cookie
     //		will expire. If a date, the date past which the cookie will expire.
     //		If expires is in the past, the cookie will be deleted.
     //		If expires is omitted or is 0, the cookie will expire when the browser closes. << FIXME: 0 seems to disappear right away? FF3.
     //	path: String?
     //		The path to use for the cookie.
     //	domain: String?
     //		The domain to use for the cookie.
     //	secure: Boolean?
     //		Whether to only send the cookie on secure connections
     this.expires = expires;
     this.path = path;
     this.domain = domain;
     this.secure = secure;
     }
     =====*/


    dojo.cookie = function(/*String*/name, /*String?*/value, /*dojo.__cookieProps?*/props) {
        //	summary:
        //		Get or set a cookie.
        //	description:
        // 		If one argument is passed, returns the value of the cookie
        // 		For two or more arguments, acts as a setter.
        //	name:
        //		Name of the cookie
        //	value:
        //		Value for the cookie
        //	props:
        //		Properties for the cookie
        //	example:
        //		set a cookie with the JSON-serialized contents of an object which
        //		will expire 5 days from now:
        //	|	dojo.cookie("configObj", dojo.toJson(config), { expires: 5 });
        //
        //	example:
        //		de-serialize a cookie back into a JavaScript object:
        //	|	var config = dojo.fromJson(dojo.cookie("configObj"));
        //
        //	example:
        //		delete a cookie:
        //	|	dojo.cookie("configObj", null, {expires: -1});
        var c = document.cookie;
        if (arguments.length == 1) {
            var matches = c.match(new RegExp("(?:^|; )" + dojo.regexp.escapeString(name) + "=([^;]*)"));
            return matches ? decodeURIComponent(matches[1]) : undefined; // String or undefined
        } else {
            props = props || {};
            // FIXME: expires=0 seems to disappear right away, not on close? (FF3)  Change docs?
            var exp = props.expires;
            if (typeof exp == "number") {
                var d = new Date();
                d.setTime(d.getTime() + exp * 24 * 60 * 60 * 1000);
                exp = props.expires = d;
            }
            if (exp && exp.toUTCString) {
                props.expires = exp.toUTCString();
            }

            value = encodeURIComponent(value);
            var updatedCookie = name + "=" + value, propName;
            for (propName in props) {
                updatedCookie += "; " + propName;
                var propValue = props[propName];
                if (propValue !== true) {
                    updatedCookie += "=" + propValue;
                }
            }
            document.cookie = updatedCookie;
        }
    };

    dojo.cookie.isSupported = function() {
        //	summary:
        //		Use to determine if the current browser supports cookies or not.
        //
        //		Returns true if user allows cookies.
        //		Returns false if user doesn't allow cookies.

        if (!("cookieEnabled" in navigator)) {
            this("__djCookieTest__", "CookiesAllowed");
            navigator.cookieEnabled = this("__djCookieTest__") == "CookiesAllowed";
            if (navigator.cookieEnabled) {
                this("__djCookieTest__", "", {expires: -1});
            }
        }
        return navigator.cookieEnabled;
    };

}

if (!dojo._hasResource["dijit._Templated"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._Templated"] = true;
    dojo.provide("dijit._Templated");


    dojo.declare("dijit._Templated",
            null,
    {
        //	summary:
        //		Mixin for widgets that are instantiated from a template
        //

        // templateString: [protected] String
        //		A string that represents the widget template. Pre-empts the
        //		templatePath. In builds that have their strings "interned", the
        //		templatePath is converted to an inline templateString, thereby
        //		preventing a synchronous network call.
        templateString: null,

        // templatePath: [protected] String
        //		Path to template (HTML file) for this widget relative to dojo.baseUrl
        templatePath: null,

        // widgetsInTemplate: [protected] Boolean
        //		Should we parse the template to find widgets that might be
        //		declared in markup inside it?  False by default.
        widgetsInTemplate: false,

        // skipNodeCache: [protected] Boolean
        //		If using a cached widget template node poses issues for a
        //		particular widget class, it can set this property to ensure
        //		that its template is always re-built from a string
        _skipNodeCache: false,

        _stringRepl: function(tmpl) {
            // summary:
            //		Does substitution of ${foo} type properties in template string
            // tags:
            //		private
            var className = this.declaredClass, _this = this;
            // Cache contains a string because we need to do property replacement
            // do the property replacement
            return dojo.string.substitute(tmpl, this, function(value, key) {
                if (key.charAt(0) == '!') {
                    value = dojo.getObject(key.substr(1), false, _this);
                }
                if (typeof value == "undefined") {
                    throw new Error(className + " template:" + key);
                } // a debugging aide
                if (value == null) {
                    return "";
                }

                // Substitution keys beginning with ! will skip the transform step,
                // in case a user wishes to insert unescaped markup, e.g. ${!foo}
                return key.charAt(0) == "!" ? value :
                    // Safer substitution, see heading "Attribute values" in
                    // http://www.w3.org/TR/REC-html40/appendix/notes.html#h-B.3.2
                       value.toString().replace(/"/g, "&quot;"); //TODO: add &amp? use encodeXML method?
            }, this);
        },

        // method over-ride
        buildRendering: function() {
            // summary:
            //		Construct the UI for this widget from a template, setting this.domNode.
            // tags:
            //		protected

            // Lookup cached version of template, and download to cache if it
            // isn't there already.  Returns either a DomNode or a string, depending on
            // whether or not the template contains ${foo} replacement parameters.
            var cached = dijit._Templated.getCachedTemplate(this.templatePath, this.templateString, this._skipNodeCache);

            var node;
            if (dojo.isString(cached)) {
                node = dojo._toDom(this._stringRepl(cached));
            } else {
                // if it's a node, all we have to do is clone it
                node = cached.cloneNode(true);
            }

            this.domNode = node;

            // recurse through the node, looking for, and attaching to, our
            // attachment points and events, which should be defined on the template node.
            this._attachTemplateNodes(node);

            if (this.widgetsInTemplate) {
                var cw = (this._supportingWidgets = dojo.parser.parse(node));
                this._attachTemplateNodes(cw, function(n, p) {
                    return n[p];
                });
            }

            this._fillContent(this.srcNodeRef);
        },

        _fillContent: function(/*DomNode*/ source) {
            // summary:
            //		Relocate source contents to templated container node.
            //		this.containerNode must be able to receive children, or exceptions will be thrown.
            // tags:
            //		protected
            var dest = this.containerNode;
            if (source && dest) {
                while (source.hasChildNodes()) {
                    dest.appendChild(source.firstChild);
                }
            }
        },

        _attachTemplateNodes: function(rootNode, getAttrFunc) {
            // summary:
            //		Iterate through the template and attach functions and nodes accordingly.
            // description:
            //		Map widget properties and functions to the handlers specified in
            //		the dom node and it's descendants. This function iterates over all
            //		nodes and looks for these properties:
            //			* dojoAttachPoint
            //			* dojoAttachEvent
            //			* waiRole
            //			* waiState
            // rootNode: DomNode|Array[Widgets]
            //		the node to search for properties. All children will be searched.
            // getAttrFunc: Function?
            //		a function which will be used to obtain property for a given
            //		DomNode/Widget
            // tags:
            //		private

            getAttrFunc = getAttrFunc || function(n, p) {
                return n.getAttribute(p);
            };

            var nodes = dojo.isArray(rootNode) ? rootNode : (rootNode.all || rootNode.getElementsByTagName("*"));
            var x = dojo.isArray(rootNode) ? 0 : -1;
            for (; x < nodes.length; x++) {
                var baseNode = (x == -1) ? rootNode : nodes[x];
                if (this.widgetsInTemplate && getAttrFunc(baseNode, "dojoType")) {
                    continue;
                }
                // Process dojoAttachPoint
                var attachPoint = getAttrFunc(baseNode, "dojoAttachPoint");
                if (attachPoint) {
                    var point, points = attachPoint.split(/\s*,\s*/);
                    while ((point = points.shift())) {
                        if (dojo.isArray(this[point])) {
                            this[point].push(baseNode);
                        } else {
                            this[point] = baseNode;
                        }
                    }
                }

                // Process dojoAttachEvent
                var attachEvent = getAttrFunc(baseNode, "dojoAttachEvent");
                if (attachEvent) {
                    // NOTE: we want to support attributes that have the form
                    // "domEvent: nativeEvent; ..."
                    var event, events = attachEvent.split(/\s*,\s*/);
                    var trim = dojo.trim;
                    while ((event = events.shift())) {
                        if (event) {
                            var thisFunc = null;
                            if (event.indexOf(":") != -1) {
                                // oh, if only JS had tuple assignment
                                var funcNameArr = event.split(":");
                                event = trim(funcNameArr[0]);
                                thisFunc = trim(funcNameArr[1]);
                            } else {
                                event = trim(event);
                            }
                            if (!thisFunc) {
                                thisFunc = event;
                            }
                            this.connect(baseNode, event, thisFunc);
                        }
                    }
                }

                // waiRole, waiState
                var role = getAttrFunc(baseNode, "waiRole");
                if (role) {
                    dijit.setWaiRole(baseNode, role);
                }
                var values = getAttrFunc(baseNode, "waiState");
                if (values) {
                    dojo.forEach(values.split(/\s*,\s*/), function(stateValue) {
                        if (stateValue.indexOf('-') != -1) {
                            var pair = stateValue.split('-');
                            dijit.setWaiState(baseNode, pair[0], pair[1]);
                        }
                    });
                }
            }
        }
    }
            );

    // key is either templatePath or templateString; object is either string or DOM tree
    dijit._Templated._templateCache = {};

    dijit._Templated.getCachedTemplate = function(templatePath, templateString, alwaysUseString) {
        // summary:
        //		Static method to get a template based on the templatePath or
        //		templateString key
        // templatePath: String
        //		The URL to get the template from. dojo.uri.Uri is often passed as well.
        // templateString: String?
        //		a string to use in lieu of fetching the template from a URL. Takes precedence
        //		over templatePath
        // returns: Mixed
        //		Either string (if there are ${} variables that need to be replaced) or just
        //		a DOM tree (if the node can be cloned directly)

        // is it already cached?
        var tmplts = dijit._Templated._templateCache;
        var key = templateString || templatePath;
        var cached = tmplts[key];
        if (cached) {
            if (!cached.ownerDocument || cached.ownerDocument == dojo.doc) {
                // string or node of the same document
                return cached;
            }
            // destroy the old cached node of a different document
            dojo.destroy(cached);
        }

        // If necessary, load template string from template path
        if (!templateString) {
            templateString = dijit._Templated._sanitizeTemplateString(dojo.trim(dojo._getText(templatePath)));
        }

        templateString = dojo.string.trim(templateString);

        if (alwaysUseString || templateString.match(/\$\{([^\}]+)\}/g)) {
            // there are variables in the template so all we can do is cache the string
            return (tmplts[key] = templateString); //String
        } else {
            // there are no variables in the template so we can cache the DOM tree
            return (tmplts[key] = dojo._toDom(templateString)); //Node
        }
    };

    dijit._Templated._sanitizeTemplateString = function(/*String*/tString) {
        // summary:
        //		Strips <?xml ...?> declarations so that external SVG and XML
        // 		documents can be added to a document without worry. Also, if the string
        //		is an HTML document, only the part inside the body tag is returned.
        if (tString) {
            tString = tString.replace(/^\s*<\?xml(\s)+version=[\'\"](\d)*.(\d)*[\'\"](\s)*\?>/im, "");
            var matches = tString.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
            if (matches) {
                tString = matches[1];
            }
        } else {
            tString = "";
        }
        return tString; //String
    };


    if (dojo.isIE) {
        dojo.addOnWindowUnload(function() {
            var cache = dijit._Templated._templateCache;
            for (var key in cache) {
                var value = cache[key];
                if (!isNaN(value.nodeType)) { // isNode equivalent
                    dojo.destroy(value);
                }
                delete cache[key];
            }
        });
    }

    // These arguments can be specified for widgets which are used in templates.
    // Since any widget can be specified as sub widgets in template, mix it
    // into the base widget class.  (This is a hack, but it's effective.)
    dojo.extend(dijit._Widget, {
        dojoAttachEvent: "",
        dojoAttachPoint: "",
        waiRole: "",
        waiState:""
    });

}

if (!dojo._hasResource["dijit.layout.BorderContainer"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.layout.BorderContainer"] = true;
    dojo.provide("dijit.layout.BorderContainer");


    dojo.declare(
            "dijit.layout.BorderContainer",
            dijit.layout._LayoutWidget,
    {
        // summary:
        //		Provides layout in up to 5 regions, a mandatory center with optional borders along its 4 sides.
        //
        // description:
        //		A BorderContainer is a box with a specified size, such as style="width: 500px; height: 500px;",
        //		that contains a child widget marked region="center" and optionally children widgets marked
        //		region equal to "top", "bottom", "leading", "trailing", "left" or "right".
        //		Children along the edges will be laid out according to width or height dimensions and may
        //		include optional splitters (splitter="true") to make them resizable by the user.  The remaining
        //		space is designated for the center region.
        //
        //		NOTE: Splitters must not be more than 50 pixels in width.
        //
        //		The outer size must be specified on the BorderContainer node.  Width must be specified for the sides
        //		and height for the top and bottom, respectively.  No dimensions should be specified on the center;
        //		it will fill the remaining space.  Regions named "leading" and "trailing" may be used just like
        //		"left" and "right" except that they will be reversed in right-to-left environments.
        //
        // example:
        // |	<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false"
        // |            style="width: 400px; height: 300px;">
        // |		<div dojoType="ContentPane" region="top">header text</div>
        // |		<div dojoType="ContentPane" region="right" splitter="true" style="width: 200px;">table of contents</div>
        // |		<div dojoType="ContentPane" region="center">client area</div>
        // |	</div>

        // design: String
        //		Which design is used for the layout:
        //			- "headline" (default) where the top and bottom extend
        //				the full width of the container
        //			- "sidebar" where the left and right sides extend from top to bottom.
        design: "headline",

        // gutters: Boolean
        //		Give each pane a border and margin.
        //		Margin determined by domNode.paddingLeft.
        //		When false, only resizable panes have a gutter (i.e. draggable splitter) for resizing.
        gutters: true,

        // liveSplitters: Boolean
        //		Specifies whether splitters resize as you drag (true) or only upon mouseup (false)
        liveSplitters: true,

        // persist: Boolean
        //		Save splitter positions in a cookie.
        persist: false,

        baseClass: "dijitBorderContainer",

        // _splitterClass: String
        // 		Optional hook to override the default Splitter widget used by BorderContainer
        _splitterClass: "dijit.layout._Splitter",

        postMixInProperties: function() {
            // change class name to indicate that BorderContainer is being used purely for
            // layout (like LayoutContainer) rather than for pretty formatting.
            if (!this.gutters) {
                this.baseClass += "NoGutter";
            }
            this.inherited(arguments);
        },

        postCreate: function() {
            this.inherited(arguments);

            this._splitters = {};
            this._splitterThickness = {};
        },

        startup: function() {
            if (this._started) {
                return;
            }
            dojo.forEach(this.getChildren(), this._setupChild, this);
            this.inherited(arguments);
        },

        _setupChild: function(/*Widget*/child) {
            // Override _LayoutWidget._setupChild().

            var region = child.region;
            if (region) {
                this.inherited(arguments);

                dojo.addClass(child.domNode, this.baseClass + "Pane");

                var ltr = this.isLeftToRight();
                if (region == "leading") {
                    region = ltr ? "left" : "right";
                }
                if (region == "trailing") {
                    region = ltr ? "right" : "left";
                }

                //FIXME: redundant?
                this["_" + region] = child.domNode;
                this["_" + region + "Widget"] = child;

                // Create draggable splitter for resizing pane,
                // or alternately if splitter=false but BorderContainer.gutters=true then
                // insert dummy div just for spacing
                if ((child.splitter || this.gutters) && !this._splitters[region]) {
                    var _Splitter = dojo.getObject(child.splitter ? this._splitterClass : "dijit.layout._Gutter");
                    var flip = {left:'right', right:'left', top:'bottom', bottom:'top', leading:'trailing', trailing:'leading'};
                    var splitter = new _Splitter({
                        container: this,
                        child: child,
                        region: region,
                        //					oppNode: dojo.query('[region=' + flip[child.region] + ']', this.domNode)[0],
                        oppNode: this["_" + flip[child.region]],
                        live: this.liveSplitters
                    });
                    splitter.isSplitter = true;
                    this._splitters[region] = splitter.domNode;
                    dojo.place(this._splitters[region], child.domNode, "after");

                    // Splitters arent added as Contained children, so we need to call startup explicitly
                    splitter.startup();
                }
                child.region = region;
            }
        },

        _computeSplitterThickness: function(region) {
            this._splitterThickness[region] = this._splitterThickness[region] ||
                                              dojo.marginBox(this._splitters[region])[(/top|bottom/.test(region) ? 'h' : 'w')];
        },

        layout: function() {
            // Implement _LayoutWidget.layout() virtual method.
            for (var region in this._splitters) {
                this._computeSplitterThickness(region);
            }
            this._layoutChildren();
        },

        addChild: function(/*Widget*/ child, /*Integer?*/ insertIndex) {
            // Override _LayoutWidget.addChild().
            this.inherited(arguments);
            if (this._started) {
                this._layoutChildren(); //OPT
            }
        },

        removeChild: function(/*Widget*/ child) {
            // Override _LayoutWidget.removeChild().
            var region = child.region;
            var splitter = this._splitters[region];
            if (splitter) {
                dijit.byNode(splitter).destroy();
                delete this._splitters[region];
                delete this._splitterThickness[region];
            }
            this.inherited(arguments);
            delete this["_" + region];
            delete this["_" + region + "Widget"];
            if (this._started) {
                this._layoutChildren(child.region);
            }
            dojo.removeClass(child.domNode, this.baseClass + "Pane");
        },

        getChildren: function() {
            // Override _LayoutWidget.getChildren() to only return real children, not the splitters.
            return dojo.filter(this.inherited(arguments), function(widget) {
                return !widget.isSplitter;
            });
        },

        getSplitter: function(/*String*/region) {
            // summary:
            //		Returns the widget responsible for rendering the splitter associated with region
            var splitter = this._splitters[region];
            return splitter ? dijit.byNode(splitter) : null;
        },

        resize: function(newSize, currentSize) {
            // Overrides _LayoutWidget.resize().

            // resetting potential padding to 0px to provide support for 100% width/height + padding
            // TODO: this hack doesn't respect the box model and is a temporary fix
            if (!this.cs || !this.pe) {
                var node = this.domNode;
                this.cs = dojo.getComputedStyle(node);
                this.pe = dojo._getPadExtents(node, this.cs);
                this.pe.r = dojo._toPixelValue(node, this.cs.paddingRight);
                this.pe.b = dojo._toPixelValue(node, this.cs.paddingBottom);

                dojo.style(node, "padding", "0px");
            }

            this.inherited(arguments);
        },

        _layoutChildren: function(/*String?*/changedRegion) {
            // summary:
            //		This is the main routine for setting size/position of each child

            if (!this._borderBox || !this._borderBox.h) {
                // We are currently hidden, or we haven't been sized by our parent yet.
                // Abort.   Someone will resize us later.
                return;
            }

            var sidebarLayout = (this.design == "sidebar");
            var topHeight = 0, bottomHeight = 0, leftWidth = 0, rightWidth = 0;
            var topStyle = {}, leftStyle = {}, rightStyle = {}, bottomStyle = {},
                    centerStyle = (this._center && this._center.style) || {};

            var changedSide = /left|right/.test(changedRegion);

            var layoutSides = !changedRegion || (!changedSide && !sidebarLayout);
            var layoutTopBottom = !changedRegion || (changedSide && sidebarLayout);

            // Ask browser for width/height of side panes.
            // Would be nice to cache this but height can change according to width
            // (because words wrap around).  I don't think width will ever change though
            // (except when the user drags a splitter).
            if (this._top) {
                topStyle = layoutTopBottom && this._top.style;
                topHeight = dojo.marginBox(this._top).h;
            }
            if (this._left) {
                leftStyle = layoutSides && this._left.style;
                leftWidth = dojo.marginBox(this._left).w;
            }
            if (this._right) {
                rightStyle = layoutSides && this._right.style;
                rightWidth = dojo.marginBox(this._right).w;
            }
            if (this._bottom) {
                bottomStyle = layoutTopBottom && this._bottom.style;
                bottomHeight = dojo.marginBox(this._bottom).h;
            }

            var splitters = this._splitters;
            var topSplitter = splitters.top, bottomSplitter = splitters.bottom,
                    leftSplitter = splitters.left, rightSplitter = splitters.right;
            var splitterThickness = this._splitterThickness;
            var topSplitterThickness = splitterThickness.top || 0,
                    leftSplitterThickness = splitterThickness.left || 0,
                    rightSplitterThickness = splitterThickness.right || 0,
                    bottomSplitterThickness = splitterThickness.bottom || 0;

            // Check for race condition where CSS hasn't finished loading, so
            // the splitter width == the viewport width (#5824)
            if (leftSplitterThickness > 50 || rightSplitterThickness > 50) {
                setTimeout(dojo.hitch(this, function() {
                    // Results are invalid.  Clear them out.
                    this._splitterThickness = {};

                    for (var region in this._splitters) {
                        this._computeSplitterThickness(region);
                    }
                    this._layoutChildren();
                }), 50);
                return false;
            }

            var pe = this.pe;

            var splitterBounds = {
                left: (sidebarLayout ? leftWidth + leftSplitterThickness : 0) + pe.l + "px",
                right: (sidebarLayout ? rightWidth + rightSplitterThickness : 0) + pe.r + "px"
            };

            if (topSplitter) {
                dojo.mixin(topSplitter.style, splitterBounds);
                topSplitter.style.top = topHeight + pe.t + "px";
            }

            if (bottomSplitter) {
                dojo.mixin(bottomSplitter.style, splitterBounds);
                bottomSplitter.style.bottom = bottomHeight + pe.b + "px";
            }

            splitterBounds = {
                top: (sidebarLayout ? 0 : topHeight + topSplitterThickness) + pe.t + "px",
                bottom: (sidebarLayout ? 0 : bottomHeight + bottomSplitterThickness) + pe.b + "px"
            };

            if (leftSplitter) {
                dojo.mixin(leftSplitter.style, splitterBounds);
                leftSplitter.style.left = leftWidth + pe.l + "px";
            }

            if (rightSplitter) {
                dojo.mixin(rightSplitter.style, splitterBounds);
                rightSplitter.style.right = rightWidth + pe.r + "px";
            }

            dojo.mixin(centerStyle, {
                top: pe.t + topHeight + topSplitterThickness + "px",
                left: pe.l + leftWidth + leftSplitterThickness + "px",
                right: pe.r + rightWidth + rightSplitterThickness + "px",
                bottom: pe.b + bottomHeight + bottomSplitterThickness + "px"
            });

            var bounds = {
                top: sidebarLayout ? pe.t + "px" : centerStyle.top,
                bottom: sidebarLayout ? pe.b + "px" : centerStyle.bottom
            };
            dojo.mixin(leftStyle, bounds);
            dojo.mixin(rightStyle, bounds);
            leftStyle.left = pe.l + "px";
            rightStyle.right = pe.r + "px";
            topStyle.top = pe.t + "px";
            bottomStyle.bottom = pe.b + "px";
            if (sidebarLayout) {
                topStyle.left = bottomStyle.left = leftWidth + leftSplitterThickness + pe.l + "px";
                topStyle.right = bottomStyle.right = rightWidth + rightSplitterThickness + pe.r + "px";
            } else {
                topStyle.left = bottomStyle.left = pe.l + "px";
                topStyle.right = bottomStyle.right = pe.r + "px";
            }

            // More calculations about sizes of panes
            var containerHeight = this._borderBox.h - pe.t - pe.b,
                    middleHeight = containerHeight - ( topHeight + topSplitterThickness + bottomHeight + bottomSplitterThickness),
                    sidebarHeight = sidebarLayout ? containerHeight : middleHeight;

            var containerWidth = this._borderBox.w - pe.l - pe.r,
                    middleWidth = containerWidth - (leftWidth + leftSplitterThickness + rightWidth + rightSplitterThickness),
                    sidebarWidth = sidebarLayout ? middleWidth : containerWidth;

            // New margin-box size of each pane
            var dim = {
                top:    { w: sidebarWidth, h: topHeight },
                bottom: { w: sidebarWidth, h: bottomHeight },
                left:    { w: leftWidth, h: sidebarHeight },
                right:    { w: rightWidth, h: sidebarHeight },
                center:    { h: middleHeight, w: middleWidth }
            };

            // Nodes in IE<8 don't respond to t/l/b/r, and TEXTAREA doesn't respond in any browser
            var janky = dojo.isIE < 8 || (dojo.isIE && dojo.isQuirks) || dojo.some(this.getChildren(), function(child) {
                return child.domNode.tagName == "TEXTAREA" || child.domNode.tagName == "INPUT";
            });
            if (janky) {
                // Set the size of the children the old fashioned way, by setting
                // CSS width and height

                var resizeWidget = function(widget, changes, result) {
                    if (widget) {
                        (widget.resize ? widget.resize(changes, result) : dojo.marginBox(widget.domNode, changes));
                    }
                };

                if (leftSplitter) {
                    leftSplitter.style.height = sidebarHeight;
                }
                if (rightSplitter) {
                    rightSplitter.style.height = sidebarHeight;
                }
                resizeWidget(this._leftWidget, {h: sidebarHeight}, dim.left);
                resizeWidget(this._rightWidget, {h: sidebarHeight}, dim.right);

                if (topSplitter) {
                    topSplitter.style.width = sidebarWidth;
                }
                if (bottomSplitter) {
                    bottomSplitter.style.width = sidebarWidth;
                }
                resizeWidget(this._topWidget, {w: sidebarWidth}, dim.top);
                resizeWidget(this._bottomWidget, {w: sidebarWidth}, dim.bottom);

                resizeWidget(this._centerWidget, dim.center);
            } else {
                // We've already sized the children by setting style.top/bottom/left/right...
                // Now just need to call resize() on those children telling them their new size,
                // so they can re-layout themselves

                // Calculate which panes need a notification
                var resizeList = {};
                if (changedRegion) {
                    resizeList[changedRegion] = resizeList.center = true;
                    if (/top|bottom/.test(changedRegion) && this.design != "sidebar") {
                        resizeList.left = resizeList.right = true;
                    } else if (/left|right/.test(changedRegion) && this.design == "sidebar") {
                        resizeList.top = resizeList.bottom = true;
                    }
                }

                dojo.forEach(this.getChildren(), function(child) {
                    if (child.resize && (!changedRegion || child.region in resizeList)) {
                        child.resize(null, dim[child.region]);
                    }
                }, this);
            }
        },

        destroy: function() {
            for (var region in this._splitters) {
                var splitter = this._splitters[region];
                dijit.byNode(splitter).destroy();
                dojo.destroy(splitter);
            }
            delete this._splitters;
            delete this._splitterThickness;
            this.inherited(arguments);
        }
    });

    // This argument can be specified for the children of a BorderContainer.
    // Since any widget can be specified as a LayoutContainer child, mix it
    // into the base widget class.  (This is a hack, but it's effective.)
    dojo.extend(dijit._Widget, {
        // region: String
        //		"top", "bottom", "leading", "trailing", "left", "right", "center".
        //		See the BorderContainer description for details on this parameter.
        region: '',

        // splitter: Boolean
        //		If true, puts a draggable splitter on this widget to resize when used
        //		inside a border container edge region.
        splitter: false,

        // minSize: Number
        //		Specifies a minimum size for this widget when resized by a splitter
        minSize: 0,

        // maxSize: Number
        //		Specifies a maximum size for this widget when resized by a splitter
        maxSize: Infinity
    });


    dojo.declare("dijit.layout._Splitter", [ dijit._Widget, dijit._Templated ],
    {
        // summary:
        //		A draggable spacer between two items in a `dijit.layout.BorderContainer`.
        // description:
        //		This is instantiated by `dijit.layout.BorderContainer`.  Users should not
        //		create it directly.
        // tags:
        //		private

        /*=====
         // container: [const] dijit.layout.BorderContainer
         //		Pointer to the parent BorderContainer
         container: null,

         // child: [const] dijit.layout._LayoutWidget
         //		Pointer to the pane associated with this splitter
         child: null,

         // region: String
         //		Region of pane associated with this splitter.
         //		"top", "bottom", "left", "right".
         region: null,
         =====*/

        // live: [const] Boolean
        //		If true, the child's size changes and the child widget is redrawn as you drag the splitter;
        //		otherwise, the size doesn't change until you drop the splitter (by mouse-up)
        live: true,

        templateString: '<div class="dijitSplitter" dojoAttachEvent="onkeypress:_onKeyPress,onmousedown:_startDrag" tabIndex="0" waiRole="separator"><div class="dijitSplitterThumb"></div></div>',

        postCreate: function() {
            this.inherited(arguments);
            this.horizontal = /top|bottom/.test(this.region);
            dojo.addClass(this.domNode, "dijitSplitter" + (this.horizontal ? "H" : "V"));
            //		dojo.addClass(this.child.domNode, "dijitSplitterPane");
            //		dojo.setSelectable(this.domNode, false); //TODO is this necessary?

            this._factor = /top|left/.test(this.region) ? 1 : -1;
            this._minSize = this.child.minSize;

            // trigger constraints calculations
            this.child.domNode._recalc = true;
            this.connect(this.container, "resize", function() {
                this.child.domNode._recalc = true;
            });

            this._cookieName = this.container.id + "_" + this.region;
            if (this.container.persist) {
                // restore old size
                var persistSize = dojo.cookie(this._cookieName);
                if (persistSize) {
                    this.child.domNode.style[this.horizontal ? "height" : "width"] = persistSize;
                }
            }
        },

        _computeMaxSize: function() {
            var dim = this.horizontal ? 'h' : 'w',
                    thickness = this.container._splitterThickness[this.region];
            var available = dojo.contentBox(this.container.domNode)[dim] -
                            (this.oppNode ? dojo.marginBox(this.oppNode)[dim] : 0) -
                            20 - thickness * 2;
            this._maxSize = Math.min(this.child.maxSize, available);
        },

        _startDrag: function(e) {
            if (this.child.domNode._recalc) {
                this._computeMaxSize();
                this.child.domNode._recalc = false;
            }

            if (!this.cover) {
                this.cover = dojo.doc.createElement('div');
                dojo.addClass(this.cover, "dijitSplitterCover");
                dojo.place(this.cover, this.child.domNode, "after");
            }
            dojo.addClass(this.cover, "dijitSplitterCoverActive");

            // Safeguard in case the stop event was missed.  Shouldn't be necessary if we always get the mouse up.
            if (this.fake) {
                dojo.destroy(this.fake);
            }
            if (!(this._resize = this.live)) { //TODO: disable live for IE6?
                // create fake splitter to display at old position while we drag
                (this.fake = this.domNode.cloneNode(true)).removeAttribute("id");
                dojo.addClass(this.domNode, "dijitSplitterShadow");
                dojo.place(this.fake, this.domNode, "after");
            }
            dojo.addClass(this.domNode, "dijitSplitterActive");

            //Performance: load data info local vars for onmousevent function closure
            var factor = this._factor,
                    max = this._maxSize,
                    min = this._minSize || 20,
                    isHorizontal = this.horizontal,
                    axis = isHorizontal ? "pageY" : "pageX",
                    pageStart = e[axis],
                    splitterStyle = this.domNode.style,
                    dim = isHorizontal ? 'h' : 'w',
                    childStart = dojo.marginBox(this.child.domNode)[dim],
                    region = this.region,
                    splitterStart = parseInt(this.domNode.style[region], 10),
                    resize = this._resize,
                    mb = {},
                    childNode = this.child.domNode,
                    layoutFunc = dojo.hitch(this.container, this.container._layoutChildren),
                    de = dojo.doc.body;

            this._handlers = (this._handlers || []).concat([
                dojo.connect(de, "onmousemove", this._drag = function(e, forceResize) {
                    var delta = e[axis] - pageStart,
                            childSize = factor * delta + childStart,
                            boundChildSize = Math.max(Math.min(childSize, max), min);

                    if (resize || forceResize) {
                        mb[dim] = boundChildSize;
                        // TODO: inefficient; we set the marginBox here and then immediately layoutFunc() needs to query it
                        dojo.marginBox(childNode, mb);
                        layoutFunc(region);
                    }
                    splitterStyle[region] = factor * delta + splitterStart + (boundChildSize - childSize) + "px";
                }),
                dojo.connect(dojo.doc, "ondragstart", dojo.stopEvent),
                dojo.connect(dojo.body(), "onselectstart", dojo.stopEvent),
                dojo.connect(de, "onmouseup", this, "_stopDrag")
            ]);
            dojo.stopEvent(e);
        },

        _stopDrag: function(e) {
            try {
                if (this.cover) {
                    dojo.removeClass(this.cover, "dijitSplitterCoverActive");
                }
                if (this.fake) {
                    dojo.destroy(this.fake);
                }
                dojo.removeClass(this.domNode, "dijitSplitterActive");
                dojo.removeClass(this.domNode, "dijitSplitterShadow");
                this._drag(e); //TODO: redundant with onmousemove?
                this._drag(e, true);
            } finally {
                this._cleanupHandlers();
                if (this.oppNode) {
                    this.oppNode._recalc = true;
                }
                delete this._drag;
            }

            if (this.container.persist) {
                dojo.cookie(this._cookieName, this.child.domNode.style[this.horizontal ? "height" : "width"], {expires:365});
            }
        },

        _cleanupHandlers: function() {
            dojo.forEach(this._handlers, dojo.disconnect);
            delete this._handlers;
        },

        _onKeyPress: function(/*Event*/ e) {
            if (this.child.domNode._recalc) {
                this._computeMaxSize();
                this.child.domNode._recalc = false;
            }

            // should we apply typematic to this?
            this._resize = true;
            var horizontal = this.horizontal;
            var tick = 1;
            var dk = dojo.keys;
            switch (e.charOrCode) {
                case horizontal ? dk.UP_ARROW : dk.LEFT_ARROW:
                    tick *= -1;
                //				break;
                case horizontal ? dk.DOWN_ARROW : dk.RIGHT_ARROW:
                    break;
                default:
                    //				this.inherited(arguments);
                    return;
            }
            var childSize = dojo.marginBox(this.child.domNode)[ horizontal ? 'h' : 'w' ] + this._factor * tick;
            var mb = {};
            mb[ this.horizontal ? "h" : "w"] = Math.max(Math.min(childSize, this._maxSize), this._minSize);
            dojo.marginBox(this.child.domNode, mb);
            if (this.oppNode) {
                this.oppNode._recalc = true;
            }
            this.container._layoutChildren(this.region);
            dojo.stopEvent(e);
        },

        destroy: function() {
            this._cleanupHandlers();
            delete this.child;
            delete this.container;
            delete this.cover;
            delete this.fake;
            this.inherited(arguments);
        }
    });

    dojo.declare("dijit.layout._Gutter", [dijit._Widget, dijit._Templated ],
    {
        // summary:
        // 		Just a spacer div to separate side pane from center pane.
        //		Basically a trick to lookup the gutter/splitter width from the theme.
        // description:
        //		Instantiated by `dijit.layout.BorderContainer`.  Users should not
        //		create directly.
        // tags:
        //		private

        templateString: '<div class="dijitGutter" waiRole="presentation"></div>',

        postCreate: function() {
            this.horizontal = /top|bottom/.test(this.region);
            dojo.addClass(this.domNode, "dijitGutter" + (this.horizontal ? "H" : "V"));
        }
    });

}

if (!dojo._hasResource["dijit.Tooltip"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.Tooltip"] = true;
    dojo.provide("dijit.Tooltip");


    dojo.declare(
            "dijit._MasterTooltip",
            [dijit._Widget, dijit._Templated],
    {
        // summary:
        //		Internal widget that holds the actual tooltip markup,
        //		which occurs once per page.
        //		Called by Tooltip widgets which are just containers to hold
        //		the markup
        // tags:
        //		protected

        // duration: Integer
        //		Milliseconds to fade in/fade out
        duration: dijit.defaultDuration,

        templateString:"<div class=\"dijitTooltip dijitTooltipLeft\" id=\"dojoTooltip\">\r\n\t<div class=\"dijitTooltipContainer dijitTooltipContents\" dojoAttachPoint=\"containerNode\" waiRole='alert'></div>\r\n\t<div class=\"dijitTooltipConnector\"></div>\r\n</div>\r\n",

        postCreate: function() {
            dojo.body().appendChild(this.domNode);

            this.bgIframe = new dijit.BackgroundIframe(this.domNode);

            // Setup fade-in and fade-out functions.
            this.fadeIn = dojo.fadeIn({ node: this.domNode, duration: this.duration, onEnd: dojo.hitch(this, "_onShow") });
            this.fadeOut = dojo.fadeOut({ node: this.domNode, duration: this.duration, onEnd: dojo.hitch(this, "_onHide") });

        },

        show: function(/*String*/ innerHTML, /*DomNode*/ aroundNode, /*String[]?*/ position) {
            // summary:
            //		Display tooltip w/specified contents to right of specified node
            //		(To left if there's no space on the right, or if LTR==right)

            if (this.aroundNode && this.aroundNode === aroundNode) {
                return;
            }

            if (this.fadeOut.status() == "playing") {
                // previous tooltip is being hidden; wait until the hide completes then show new one
                this._onDeck = arguments;
                return;
            }
            this.containerNode.innerHTML = innerHTML;

            // Firefox bug. when innerHTML changes to be shorter than previous
            // one, the node size will not be updated until it moves.
            this.domNode.style.top = (this.domNode.offsetTop + 1) + "px";

            // position the element and change CSS according to position[] (a list of positions to try)
            var align = {};
            var ltr = this.isLeftToRight();
            dojo.forEach((position && position.length) ? position : dijit.Tooltip.defaultPosition, function(pos) {
                switch (pos) {
                    case "after":
                        align[ltr ? "BR" : "BL"] = ltr ? "BL" : "BR";
                        break;
                    case "before":
                        align[ltr ? "BL" : "BR"] = ltr ? "BR" : "BL";
                        break;
                    case "below":
                        // first try to align left borders, next try to align right borders (or reverse for RTL mode)
                        align[ltr ? "BL" : "BR"] = ltr ? "TL" : "TR";
                        align[ltr ? "BR" : "BL"] = ltr ? "TR" : "TL";
                        break;
                    case "above":
                    default:
                        // first try to align left borders, next try to align right borders (or reverse for RTL mode)
                        align[ltr ? "TL" : "TR"] = ltr ? "BL" : "BR";
                        align[ltr ? "TR" : "TL"] = ltr ? "BR" : "BL";
                        break;
                }
            });
            var pos = dijit.placeOnScreenAroundElement(this.domNode, aroundNode, align, dojo.hitch(this, "orient"));

            // show it
            dojo.style(this.domNode, "opacity", 0);
            this.fadeIn.play();
            this.isShowingNow = true;
            this.aroundNode = aroundNode;
        },

        orient: function(/* DomNode */ node, /* String */ aroundCorner, /* String */ tooltipCorner) {
            // summary:
            //		Private function to set CSS for tooltip node based on which position it's in.
            //		This is called by the dijit popup code.
            // tags:
            //		protected

            node.className = "dijitTooltip " +
                             {
                                 "BL-TL": "dijitTooltipBelow dijitTooltipABLeft",
                                 "TL-BL": "dijitTooltipAbove dijitTooltipABLeft",
                                 "BR-TR": "dijitTooltipBelow dijitTooltipABRight",
                                 "TR-BR": "dijitTooltipAbove dijitTooltipABRight",
                                 "BR-BL": "dijitTooltipRight",
                                 "BL-BR": "dijitTooltipLeft"
                             }[aroundCorner + "-" + tooltipCorner];
        },

        _onShow: function() {
            // summary:
            //		Called at end of fade-in operation
            // tags:
            //		protected
            if (dojo.isIE) {
                // the arrow won't show up on a node w/an opacity filter
                this.domNode.style.filter = "";
            }
        },

        hide: function(aroundNode) {
            // summary:
            //		Hide the tooltip
            if (this._onDeck && this._onDeck[1] == aroundNode) {
                // this hide request is for a show() that hasn't even started yet;
                // just cancel the pending show()
                this._onDeck = null;
            } else if (this.aroundNode === aroundNode) {
                // this hide request is for the currently displayed tooltip
                this.fadeIn.stop();
                this.isShowingNow = false;
                this.aroundNode = null;
                this.fadeOut.play();
            } else {
                // just ignore the call, it's for a tooltip that has already been erased
            }
        },

        _onHide: function() {
            // summary:
            //		Called at end of fade-out operation
            // tags:
            //		protected

            this.domNode.style.cssText = "";	// to position offscreen again
            if (this._onDeck) {
                // a show request has been queued up; do it now
                this.show.apply(this, this._onDeck);
                this._onDeck = null;
            }
        }

    }
            );

    dijit.showTooltip = function(/*String*/ innerHTML, /*DomNode*/ aroundNode, /*String[]?*/ position) {
        // summary:
        //		Display tooltip w/specified contents in specified position.
        //		See description of dijit.Tooltip.defaultPosition for details on position parameter.
        //		If position is not specified then dijit.Tooltip.defaultPosition is used.
        if (!dijit._masterTT) {
            dijit._masterTT = new dijit._MasterTooltip();
        }
        return dijit._masterTT.show(innerHTML, aroundNode, position);
    };

    dijit.hideTooltip = function(aroundNode) {
        // summary:
        //		Hide the tooltip
        if (!dijit._masterTT) {
            dijit._masterTT = new dijit._MasterTooltip();
        }
        return dijit._masterTT.hide(aroundNode);
    };

    dojo.declare(
            "dijit.Tooltip",
            dijit._Widget,
    {
        // summary
        //		Pops up a tooltip (a help message) when you hover over a node.

        // label: String
        //		Text to display in the tooltip.
        //		Specified as innerHTML when creating the widget from markup.
        label: "",

        // showDelay: Integer
        //		Number of milliseconds to wait after hovering over/focusing on the object, before
        //		the tooltip is displayed.
        showDelay: 400,

        // connectId: [const] String[]
        //		Id's of domNodes to attach the tooltip to.
        //		When user hovers over any of the specified dom nodes, the tooltip will appear.
        //
        //		Note: Currently connectId can only be specified on initialization, it cannot
        //		be changed via attr('connectId', ...)
        //
        //		Note: in 2.0 this will be renamed to connectIds for less confusion.
        connectId: [],

        // position: String[]
        //		See description of `dijit.Tooltip.defaultPosition` for details on position parameter.
        position: [],

        _setConnectIdAttr: function(ids) {
            // TODO: erase old conections

            this._connectNodes = [];

            // TODO: rename connectId to connectIds for 2.0, and remove this code converting from string to array
            this.connectId = dojo.isArrayLike(ids) ? ids : [ids];

            dojo.forEach(this.connectId, function(id) {
                var node = dojo.byId(id);
                if (node) {
                    this._connectNodes.push(node);
                    dojo.forEach(["onMouseEnter", "onMouseLeave", "onFocus", "onBlur"], function(event) {
                        this.connect(node, event.toLowerCase(), "_" + event);
                    }, this);
                    if (dojo.isIE) {
                        // BiDi workaround
                        node.style.zoom = 1;
                    }
                }
            }, this);
        },

        postCreate: function() {
            dojo.addClass(this.domNode, "dijitTooltipData");
        },

        _onMouseEnter: function(/*Event*/ e) {
            // summary:
            //		Handler for mouseenter event on the target node
            // tags:
            //		private
            this._onHover(e);
        },

        _onMouseLeave: function(/*Event*/ e) {
            // summary:
            //		Handler for mouseleave event on the target node
            // tags:
            //		private
            this._onUnHover(e);
        },

        _onFocus: function(/*Event*/ e) {
            // summary:
            //		Handler for focus event on the target node
            // tags:
            //		private

            // TODO: this is dangerously named, as the dijit focus manager calls
            // _onFocus() on any widget that gets focus (whereas in this class we
            // are connecting onfocus on the *target* DOM node to this method

            this._focus = true;
            this._onHover(e);
            this.inherited(arguments);
        },

        _onBlur: function(/*Event*/ e) {
            // summary:
            //		Handler for blur event on the target node
            // tags:
            //		private

            // TODO: rename; see above comment

            this._focus = false;
            this._onUnHover(e);
            this.inherited(arguments);
        },

        _onHover: function(/*Event*/ e) {
            // summary:
            //		Despite the name of this method, it actually handles both hover and focus
            //		events on the target node, setting a timer to show the tooltip.
            // tags:
            //		private
            if (!this._showTimer) {
                var target = e.target;
                this._showTimer = setTimeout(dojo.hitch(this, function() {
                    this.open(target)
                }), this.showDelay);
            }
        },

        _onUnHover: function(/*Event*/ e) {
            // summary:
            //		Despite the name of this method, it actually handles both mouseleave and blur
            //		events on the target node, hiding the tooltip.
            // tags:
            //		private

            // keep a tooltip open if the associated element still has focus (even though the
            // mouse moved away)
            if (this._focus) {
                return;
            }

            if (this._showTimer) {
                clearTimeout(this._showTimer);
                delete this._showTimer;
            }
            this.close();
        },

        open: function(/*DomNode*/ target) {
            // summary:
            //		Display the tooltip; usually not called directly.
            // tags:
            //		private

            target = target || this._connectNodes[0];
            if (!target) {
                return;
            }

            if (this._showTimer) {
                clearTimeout(this._showTimer);
                delete this._showTimer;
            }
            dijit.showTooltip(this.label || this.domNode.innerHTML, target, this.position);

            this._connectNode = target;
        },

        close: function() {
            // summary:
            //		Hide the tooltip or cancel timer for show of tooltip
            // tags:
            //		private

            if (this._connectNode) {
                // if tooltip is currently shown
                dijit.hideTooltip(this._connectNode);
                delete this._connectNode;
            }
            if (this._showTimer) {
                // if tooltip is scheduled to be shown (after a brief delay)
                clearTimeout(this._showTimer);
                delete this._showTimer;
            }
        },

        uninitialize: function() {
            this.close();
        }
    }
            );

    // dijit.Tooltip.defaultPosition: String[]
    //		This variable controls the position of tooltips, if the position is not specified to
    //		the Tooltip widget or *TextBox widget itself.  It's an array of strings with the following values:
    //
    //			* before: places tooltip to the left of the target node/widget, or to the right in
    //			  the case of RTL scripts like Hebrew and Arabic
    //			* after: places tooltip to the right of the target node/widget, or to the left in
    //			  the case of RTL scripts like Hebrew and Arabic
    //			* above: tooltip goes above target node
    //			* below: tooltip goes below target node
    //
    //		The list is positions is tried, in order, until a position is found where the tooltip fits
    //		within the viewport.
    //
    //		Be careful setting this parameter.  A value of "above" may work fine until the user scrolls
    //		the screen so that there's no room above the target node.   Nodes with drop downs, like
    //		DropDownButton or FilteringSelect, are especially problematic, in that you need to be sure
    //		that the drop down and tooltip don't overlap, even when the viewport is scrolled so that there
    //		is only room below (or above) the target node, but not both.
    dijit.Tooltip.defaultPosition = ["after", "before"];

}

if (!dojo._hasResource["dijit._KeyNavContainer"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit._KeyNavContainer"] = true;
    dojo.provide("dijit._KeyNavContainer");


    dojo.declare("dijit._KeyNavContainer",
            [dijit._Container],
    {

        // summary:
        //		A _Container with keyboard navigation of its children.
        // description:
        //		To use this mixin, call connectKeyNavHandlers() in
        //		postCreate() and call startupKeyNavChildren() in startup().
        //		It provides normalized keyboard and focusing code for Container
        //		widgets.
        /*=====
         // focusedChild: [protected] Widget
         //		The currently focused child widget, or null if there isn't one
         focusedChild: null,
         =====*/

        // tabIndex: Integer
        //		Tab index of the container; same as HTML tabindex attribute.
        //		Note then when user tabs into the container, focus is immediately
        //		moved to the first item in the container.
        tabIndex: "0",


        _keyNavCodes: {},

        connectKeyNavHandlers: function(/*dojo.keys[]*/ prevKeyCodes, /*dojo.keys[]*/ nextKeyCodes) {
            // summary:
            //		Call in postCreate() to attach the keyboard handlers
            //		to the container.
            // preKeyCodes: dojo.keys[]
            //		Key codes for navigating to the previous child.
            // nextKeyCodes: dojo.keys[]
            //		Key codes for navigating to the next child.
            // tags:
            //		protected

            var keyCodes = this._keyNavCodes = {};
            var prev = dojo.hitch(this, this.focusPrev);
            var next = dojo.hitch(this, this.focusNext);
            dojo.forEach(prevKeyCodes, function(code) {
                keyCodes[code] = prev;
            });
            dojo.forEach(nextKeyCodes, function(code) {
                keyCodes[code] = next;
            });
            this.connect(this.domNode, "onkeypress", "_onContainerKeypress");
            this.connect(this.domNode, "onfocus", "_onContainerFocus");
        },

        startupKeyNavChildren: function() {
            // summary:
            //		Call in startup() to set child tabindexes to -1
            // tags:
            //		protected
            dojo.forEach(this.getChildren(), dojo.hitch(this, "_startupChild"));
        },

        addChild: function(/*Widget*/ widget, /*int?*/ insertIndex) {
            // summary:
            //		Add a child to our _Container
            dijit._KeyNavContainer.superclass.addChild.apply(this, arguments);
            this._startupChild(widget);
        },

        focus: function() {
            // summary:
            //		Default focus() implementation: focus the first child.
            this.focusFirstChild();
        },

        focusFirstChild: function() {
            // summary:
            //		Focus the first focusable child in the container.
            // tags:
            //		protected
            this.focusChild(this._getFirstFocusableChild());
        },

        focusNext: function() {
            // summary:
            //		Focus the next widget or focal node (for widgets
            //		with multiple focal nodes) within this container.
            // tags:
            //		protected
            if (this.focusedChild && this.focusedChild.hasNextFocalNode
                    && this.focusedChild.hasNextFocalNode()) {
                this.focusedChild.focusNext();
                return;
            }
            var child = this._getNextFocusableChild(this.focusedChild, 1);
            if (child.getFocalNodes) {
                this.focusChild(child, child.getFocalNodes()[0]);
            } else {
                this.focusChild(child);
            }
        },

        focusPrev: function() {
            // summary:
            //		Focus the previous widget or focal node (for widgets
            //		with multiple focal nodes) within this container.
            // tags:
            //		protected
            if (this.focusedChild && this.focusedChild.hasPrevFocalNode
                    && this.focusedChild.hasPrevFocalNode()) {
                this.focusedChild.focusPrev();
                return;
            }
            var child = this._getNextFocusableChild(this.focusedChild, -1);
            if (child.getFocalNodes) {
                var nodes = child.getFocalNodes();
                this.focusChild(child, nodes[nodes.length - 1]);
            } else {
                this.focusChild(child);
            }
        },

        focusChild: function(/*Widget*/ widget, /*Node?*/ node) {
            // summary:
            //		Focus widget. Optionally focus 'node' within widget.
            // tags:
            //		protected
            if (widget) {
                if (this.focusedChild && widget !== this.focusedChild) {
                    this._onChildBlur(this.focusedChild);
                }
                this.focusedChild = widget;
                if (node && widget.focusFocalNode) {
                    widget.focusFocalNode(node);
                } else {
                    widget.focus();
                }
            }
        },

        _startupChild: function(/*Widget*/ widget) {
            // summary:
            //		Set tabindex="-1" on focusable widgets so that we
            // 		can focus them programmatically and by clicking.
            //		Connect focus and blur handlers.
            // tags:
            //		private
            if (widget.getFocalNodes) {
                dojo.forEach(widget.getFocalNodes(), function(node) {
                    dojo.attr(node, "tabindex", -1);
                    this._connectNode(node);
                }, this);
            } else {
                var node = widget.focusNode || widget.domNode;
                if (widget.isFocusable()) {
                    dojo.attr(node, "tabindex", -1);
                }
                this._connectNode(node);
            }
        },

        _connectNode: function(/*Element*/ node) {
            // summary:
            //		Monitor focus and blur events on the node
            // tags:
            //		private
            this.connect(node, "onfocus", "_onNodeFocus");
            this.connect(node, "onblur", "_onNodeBlur");
        },

        _onContainerFocus: function(evt) {
            // summary:
            //		Handler for when the container gets focus
            // description:
            //		Initially the container itself has a tabIndex, but when it gets
            //		focus, switch focus to first child...
            // tags:
            //		private

            // Note that we can't use _onFocus() because switching focus from the
            // _onFocus() handler confuses the focus.js code
            // (because it causes _onFocusNode() to be called recursively)

            // focus bubbles on Firefox,
            // so just make sure that focus has really gone to the container
            if (evt.target !== this.domNode) {
                return;
            }

            this.focusFirstChild();

            // and then remove the container's tabIndex,
            // so that tab or shift-tab will go to the fields after/before
            // the container, rather than the container itself
            dojo.removeAttr(this.domNode, "tabIndex");
        },

        _onBlur: function(evt) {
            // When focus is moved away the container, and it's descendant (popup) widgets,
            // then restore the container's tabIndex so that user can tab to it again.
            // Note that using _onBlur() so that this doesn't happen when focus is shifted
            // to one of my child widgets (typically a popup)
            if (this.tabIndex) {
                dojo.attr(this.domNode, "tabindex", this.tabIndex);
            }
            // TODO: this.inherited(arguments);
        },

        _onContainerKeypress: function(evt) {
            // summary:
            //		When a key is pressed, if it's an arrow key etc. then
            //		it's handled here.
            // tags:
            //		private
            if (evt.ctrlKey || evt.altKey) {
                return;
            }
            var func = this._keyNavCodes[evt.charOrCode];
            if (func) {
                func();
                dojo.stopEvent(evt);
            }
        },

        _onNodeFocus: function(evt) {
            // summary:
            //		Handler for onfocus event on a child node
            // tags:
            //		private

            // record the child that has been focused
            var widget = dijit.getEnclosingWidget(evt.target);
            if (widget && widget.isFocusable()) {
                this.focusedChild = widget;
            }
            dojo.stopEvent(evt);
        },

        _onNodeBlur: function(evt) {
            // summary:
            //		Handler for onblur event on a child node
            // tags:
            //		private
            dojo.stopEvent(evt);
        },

        _onChildBlur: function(/*Widget*/ widget) {
            // summary:
            //		Called when focus leaves a child widget to go
            //		to a sibling widget.
            // tags:
            //		protected
        },

        _getFirstFocusableChild: function() {
            // summary:
            //		Returns first child that can be focused
            return this._getNextFocusableChild(null, 1);
        },

        _getNextFocusableChild: function(child, dir) {
            // summary:
            //		Returns the next or previous focusable child, compared
            //		to "child"
            // child: Widget
            //		The current widget
            // dir: Integer
            //		* 1 = after
            //		* -1 = before
            if (child) {
                child = this._getSiblingOfChild(child, dir);
            }
            var children = this.getChildren();
            for (var i = 0; i < children.length; i++) {
                if (!child) {
                    child = children[(dir > 0) ? 0 : (children.length - 1)];
                }
                if (child.isFocusable()) {
                    return child;
                }
                child = this._getSiblingOfChild(child, dir);
            }
            // no focusable child found
            return null;
        }
    }
            );

}

if (!dojo._hasResource["dijit.MenuItem"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.MenuItem"] = true;
    dojo.provide("dijit.MenuItem");


    dojo.declare("dijit.MenuItem",
            [dijit._Widget, dijit._Templated, dijit._Contained],
    {
        // summary:
        //		A line item in a Menu Widget

        // Make 3 columns
        // icon, label, and expand arrow (BiDi-dependent) indicating sub-menu
        templateString:"<tr class=\"dijitReset dijitMenuItem\" dojoAttachPoint=\"focusNode\" waiRole=\"menuitem\" tabIndex=\"-1\"\r\n\t\tdojoAttachEvent=\"onmouseenter:_onHover,onmouseleave:_onUnhover,ondijitclick:_onClick\">\r\n\t<td class=\"dijitReset\" waiRole=\"presentation\">\r\n\t\t<img src=\"${_blankGif}\" alt=\"\" class=\"dijitMenuItemIcon\" dojoAttachPoint=\"iconNode\">\r\n\t</td>\r\n\t<td class=\"dijitReset dijitMenuItemLabel\" colspan=\"2\" dojoAttachPoint=\"containerNode\"></td>\r\n\t<td class=\"dijitReset dijitMenuItemAccelKey\" style=\"display: none\" dojoAttachPoint=\"accelKeyNode\"></td>\r\n\t<td class=\"dijitReset dijitMenuArrowCell\" waiRole=\"presentation\">\r\n\t\t<div dojoAttachPoint=\"arrowWrapper\" style=\"visibility: hidden\">\r\n\t\t\t<img src=\"${_blankGif}\" alt=\"\" class=\"dijitMenuExpand\">\r\n\t\t\t<span class=\"dijitMenuExpandA11y\">+</span>\r\n\t\t</div>\r\n\t</td>\r\n</tr>\r\n",

        attributeMap: dojo.delegate(dijit._Widget.prototype.attributeMap, {
            label: { node: "containerNode", type: "innerHTML" },
            iconClass: { node: "iconNode", type: "class" }
        }),

        // label: String
        //		Menu text
        label: '',

        // iconClass: String
        //		Class to apply to DOMNode to make it display an icon.
        iconClass: "",

        // accelKey: String
        //		Text for the accelerator (shortcut) key combination.
        //		Note that although Menu can display accelerator keys there
        //		is no infrastructure to actually catch and execute these
        //		accelerators.
        accelKey: "",

        // disabled: Boolean
        //		If true, the menu item is disabled.
        //		If false, the menu item is enabled.
        disabled: false,

        _fillContent: function(/*DomNode*/ source) {
            // If button label is specified as srcNodeRef.innerHTML rather than
            // this.params.label, handle it here.
            if (source && !("label" in this.params)) {
                this.attr('label', source.innerHTML);
            }
        },

        postCreate: function() {
            dojo.setSelectable(this.domNode, false);
            dojo.attr(this.containerNode, "id", this.id + "_text");
            dijit.setWaiState(this.domNode, "labelledby", this.id + "_text");
        },

        _onHover: function() {
            // summary:
            //		Handler when mouse is moved onto menu item
            // tags:
            //		protected
            dojo.addClass(this.domNode, 'dijitMenuItemHover');
            this.getParent().onItemHover(this);
        },

        _onUnhover: function() {
            // summary:
            //		Handler when mouse is moved off of menu item,
            //		possibly to a child menu, or maybe to a sibling
            //		menuitem or somewhere else entirely.
            // tags:
            //		protected

            // if we are unhovering the currently selected item
            // then unselect it
            dojo.removeClass(this.domNode, 'dijitMenuItemHover');
            this.getParent().onItemUnhover(this);
        },

        _onClick: function(evt) {
            // summary:
            //		Internal handler for click events on MenuItem.
            // tags:
            //		private
            this.getParent().onItemClick(this, evt);
            dojo.stopEvent(evt);
        },

        onClick: function(/*Event*/ evt) {
            // summary:
            //		User defined function to handle clicks
            // tags:
            //		callback
        },

        focus: function() {
            // summary:
            //		Focus on this MenuItem
            try {
                dijit.focus(this.focusNode);
            } catch(e) {
                // this throws on IE (at least) in some scenarios
            }
        },

        _onFocus: function() {
            // summary:
            //		This is called by the focus manager when focus
            //		goes to this MenuItem or a child menu.
            // tags:
            //		protected
            this._setSelected(true);

            // TODO: this.inherited(arguments);
        },

        _setSelected: function(selected) {
            // summary:
            //		Indicate that this node is the currently selected one
            // tags:
            //		private

            /***
             * TODO: remove this method and calls to it, when _onBlur() is working for MenuItem.
             * Currently _onBlur() gets called when focus is moved from the MenuItem to a child menu.
             * That's not supposed to happen, but the problem is:
             * In order to allow dijit.popup's getTopPopup() to work,a sub menu's popupParent
             * points to the parent Menu, bypassing the parent MenuItem... thus the
             * MenuItem is not in the chain of active widgets and gets a premature call to
             * _onBlur()
             */

            dojo.toggleClass(this.domNode, "dijitMenuItemSelected", selected);
        },

        setLabel: function(/*String*/ content) {
            // summary:
            //		Deprecated.   Use attr('label', ...) instead.
            // tags:
            //		deprecated
            dojo.deprecated("dijit.MenuItem.setLabel() is deprecated.  Use attr('label', ...) instead.", "", "2.0");
            this.attr("label", content);
        },

        setDisabled: function(/*Boolean*/ disabled) {
            // summary:
            //		Deprecated.   Use attr('disabled', bool) instead.
            // tags:
            //		deprecated
            dojo.deprecated("dijit.Menu.setDisabled() is deprecated.  Use attr('disabled', bool) instead.", "", "2.0");
            this.attr('disabled', disabled);
        },
        _setDisabledAttr: function(/*Boolean*/ value) {
            // summary:
            //		Hook for attr('disabled', ...) to work.
            //		Enable or disable this menu item.
            this.disabled = value;
            dojo[value ? "addClass" : "removeClass"](this.domNode, 'dijitMenuItemDisabled');
            dijit.setWaiState(this.focusNode, 'disabled', value ? 'true' : 'false');
        },
        _setAccelKeyAttr: function(/*String*/ value) {
            // summary:
            //		Hook for attr('accelKey', ...) to work.
            //		Set accelKey on this menu item.
            this.accelKey = value;

            this.accelKeyNode.style.display = value ? "" : "none";
            this.accelKeyNode.innerHTML = value;
            //have to use colSpan to make it work in IE
            dojo.attr(this.containerNode, 'colSpan', value ? "1" : "2");
        }
    });

}

if (!dojo._hasResource["dijit.PopupMenuItem"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.PopupMenuItem"] = true;
    dojo.provide("dijit.PopupMenuItem");


    dojo.declare("dijit.PopupMenuItem",
            dijit.MenuItem,
    {
        _fillContent: function() {
            // summary:
            //		When Menu is declared in markup, this code gets the menu label and
            //		the popup widget from the srcNodeRef.
            // description:
            //		srcNodeRefinnerHTML contains both the menu item text and a popup widget
            //		The first part holds the menu item text and the second part is the popup
            // example:
            // |	<div dojoType="dijit.PopupMenuItem">
            // |		<span>pick me</span>
            // |		<popup> ... </popup>
            // |	</div>
            // tags:
            //		protected

            if (this.srcNodeRef) {
                var nodes = dojo.query("*", this.srcNodeRef);
                dijit.PopupMenuItem.superclass._fillContent.call(this, nodes[0]);

                // save pointer to srcNode so we can grab the drop down widget after it's instantiated
                this.dropDownContainer = this.srcNodeRef;
            }
        },

        startup: function() {
            if (this._started) {
                return;
            }
            this.inherited(arguments);

            // we didn't copy the dropdown widget from the this.srcNodeRef, so it's in no-man's
            // land now.  move it to dojo.doc.body.
            if (!this.popup) {
                var node = dojo.query("[widgetId]", this.dropDownContainer)[0];
                this.popup = dijit.byNode(node);
            }
            dojo.body().appendChild(this.popup.domNode);

            this.popup.domNode.style.display = "none";
            if (this.arrowWrapper) {
                dojo.style(this.arrowWrapper, "visibility", "");
            }
            dijit.setWaiState(this.focusNode, "haspopup", "true");
        },

        destroyDescendants: function() {
            if (this.popup) {
                this.popup.destroyRecursive();
                delete this.popup;
            }
            this.inherited(arguments);
        }
    });


}

if (!dojo._hasResource["dijit.CheckedMenuItem"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.CheckedMenuItem"] = true;
    dojo.provide("dijit.CheckedMenuItem");


    dojo.declare("dijit.CheckedMenuItem",
            dijit.MenuItem,
    {
        // summary:
        //		A checkbox-like menu item for toggling on and off

        templateString:"<tr class=\"dijitReset dijitMenuItem\" dojoAttachPoint=\"focusNode\" waiRole=\"menuitemcheckbox\" tabIndex=\"-1\"\r\n\t\tdojoAttachEvent=\"onmouseenter:_onHover,onmouseleave:_onUnhover,ondijitclick:_onClick\">\r\n\t<td class=\"dijitReset\" waiRole=\"presentation\">\r\n\t\t<img src=\"${_blankGif}\" alt=\"\" class=\"dijitMenuItemIcon dijitCheckedMenuItemIcon\" dojoAttachPoint=\"iconNode\">\r\n\t\t<span class=\"dijitCheckedMenuItemIconChar\">&#10003;</span>\r\n\t</td>\r\n\t<td class=\"dijitReset dijitMenuItemLabel\" colspan=\"2\" dojoAttachPoint=\"containerNode,labelNode\"></td>\r\n\t<td class=\"dijitReset dijitMenuItemAccelKey\" style=\"display: none\" dojoAttachPoint=\"accelKeyNode\"></td>\r\n\t<td class=\"dijitReset dijitMenuArrowCell\" waiRole=\"presentation\">\r\n\t</td>\r\n</tr>\r\n",

        // checked: Boolean
        //		Our checked state
        checked: false,
        _setCheckedAttr: function(/*Boolean*/ checked) {
            // summary:
            //		Hook so attr('checked', bool) works.
            //		Sets the class and state for the check box.
            dojo.toggleClass(this.domNode, "dijitCheckedMenuItemChecked", checked);
            dijit.setWaiState(this.domNode, "checked", checked);
            this.checked = checked;
        },

        onChange: function(/*Boolean*/ checked) {
            // summary:
            //		User defined function to handle check/uncheck events
            // tags:
            //		callback
        },

        _onClick: function(/*Event*/ e) {
            // summary:
            //		Clicking this item just toggles its state
            // tags:
            //		private
            if (!this.disabled) {
                this.attr("checked", !this.checked);
                this.onChange(this.checked);
            }
            this.inherited(arguments);
        }
    });

}

if (!dojo._hasResource["dijit.MenuSeparator"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.MenuSeparator"] = true;
    dojo.provide("dijit.MenuSeparator");


    dojo.declare("dijit.MenuSeparator",
            [dijit._Widget, dijit._Templated, dijit._Contained],
    {
        // summary:
        //		A line between two menu items

        templateString:"<tr class=\"dijitMenuSeparator\">\r\n\t<td colspan=\"4\">\r\n\t\t<div class=\"dijitMenuSeparatorTop\"></div>\r\n\t\t<div class=\"dijitMenuSeparatorBottom\"></div>\r\n\t</td>\r\n</tr>\r\n",

        postCreate: function() {
            dojo.setSelectable(this.domNode, false);
        },

        isFocusable: function() {
            // summary:
            //		Override to always return false
            // tags:
            //		protected

            return false; // Boolean
        }
    });


}

if (!dojo._hasResource["dijit.Menu"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.Menu"] = true;
    dojo.provide("dijit.Menu");


    dojo.declare("dijit._MenuBase",
            [dijit._Widget, dijit._Templated, dijit._KeyNavContainer],
    {
        // summary:
        //		Base class for Menu and MenuBar

        // parentMenu: [readonly] Widget
        //		pointer to menu that displayed me
        parentMenu: null,

        // popupDelay: Integer
        //		number of milliseconds before hovering (without clicking) causes the popup to automatically open.
        popupDelay: 500,

        startup: function() {
            if (this._started) {
                return;
            }

            dojo.forEach(this.getChildren(), function(child) {
                child.startup();
            });
            this.startupKeyNavChildren();

            this.inherited(arguments);
        },

        onExecute: function() {
            // summary:
            //		Attach point for notification about when a menu item has been executed.
            //		This is an internal mechanism used for Menus to signal to their parent to
            //		close them, because they are about to execute the onClick handler.   In
            //		general developers should not attach to or override this method.
            // tags:
            //		protected
        },

        onCancel: function(/*Boolean*/ closeAll) {
            // summary:
            //		Attach point for notification about when the user cancels the current menu
            //		This is an internal mechanism used for Menus to signal to their parent to
            //		close them.  In general developers should not attach to or override this method.
            // tags:
            //		protected
        },

        _moveToPopup: function(/*Event*/ evt) {
            // summary:
            //		This handles the right arrow key (left arrow key on RTL systems),
            //		which will either open a submenu, or move to the next item in the
            //		ancestor MenuBar
            // tags:
            //		private

            if (this.focusedChild && this.focusedChild.popup && !this.focusedChild.disabled) {
                this.focusedChild._onClick(evt);
            } else {
                var topMenu = this._getTopMenu();
                if (topMenu && topMenu._isMenuBar) {
                    topMenu.focusNext();
                }
            }
        },

        onItemHover: function(/*MenuItem*/ item) {
            // summary:
            //		Called when cursor is over a MenuItem.
            // tags:
            //		protected

            // Don't do anything unless user has "activated" the menu by:
            //		1) clicking it
            //		2) tabbing into it
            //		3) opening it from a parent menu (which automatically focuses it)
            if (this.isActive) {
                this.focusChild(item);

                if (this.focusedChild.popup && !this.focusedChild.disabled && !this.hover_timer) {
                    this.hover_timer = setTimeout(dojo.hitch(this, "_openPopup"), this.popupDelay);
                }
            }
        },

        _onChildBlur: function(item) {
            // summary:
            //		Called when a child MenuItem becomes inactive because focus
            //		has been removed from the MenuItem *and* it's descendant menus.
            // tags:
            //		private

            item._setSelected(false);

            // Close all popups that are open and descendants of this menu
            dijit.popup.close(item.popup);
            this._stopPopupTimer();
        },

        onItemUnhover: function(/*MenuItem*/ item) {
            // summary:
            //		Callback fires when mouse exits a MenuItem
            // tags:
            //		protected
            if (this.isActive) {
                this._stopPopupTimer();
            }
        },

        _stopPopupTimer: function() {
            // summary:
            //		Cancels the popup timer because the user has stop hovering
            //		on the MenuItem, etc.
            // tags:
            //		private
            if (this.hover_timer) {
                clearTimeout(this.hover_timer);
                this.hover_timer = null;
            }
        },

        _getTopMenu: function() {
            // summary:
            //		Returns the top menu in this chain of Menus
            // tags:
            //		private
            for (var top = this; top.parentMenu; top = top.parentMenu);
            return top;
        },

        onItemClick: function(/*Widget*/ item, /*Event*/ evt) {
            // summary:
            //		Handle clicks on an item.
            // tags:
            //		private
            if (item.disabled) {
                return false;
            }

            this.focusChild(item);

            if (item.popup) {
                if (!this.is_open) {
                    this._openPopup();
                }
            } else {
                // before calling user defined handler, close hierarchy of menus
                // and restore focus to place it was when menu was opened
                this.onExecute();

                // user defined handler for click
                item.onClick(evt);
            }
        },

        _openPopup: function() {
            // summary:
            //		Open the popup to the side of/underneath the current menu item
            // tags:
            //		protected

            this._stopPopupTimer();
            var from_item = this.focusedChild;
            var popup = from_item.popup;

            if (popup.isShowingNow) {
                return;
            }
            popup.parentMenu = this;
            var self = this;
            dijit.popup.open({
                parent: this,
                popup: popup,
                around: from_item.domNode,
                orient: this._orient || (this.isLeftToRight() ? {'TR': 'TL', 'TL': 'TR'} : {'TL': 'TR', 'TR': 'TL'}),
                onCancel: function() {
                    // called when the child menu is canceled
                    dijit.popup.close(popup);
                    from_item.focus();	// put focus back on my node
                    self.currentPopup = null;
                },
                onExecute: dojo.hitch(this, "_onDescendantExecute")
            });

            this.currentPopup = popup;

            if (popup.focus) {
                // If user is opening the popup via keyboard (right arrow, or down arrow for MenuBar),
                // if the cursor happens to collide with the popup, it will generate an onmouseover event
                // even though the mouse wasn't moved.   Use a setTimeout() to call popup.focus so that
                // our focus() call overrides the onmouseover event, rather than vice-versa.  (#8742)
                setTimeout(dojo.hitch(popup, "focus"), 0);
            }
        },

        onOpen: function(/*Event*/ e) {
            // summary:
            //		Callback when this menu is opened.
            //		This is called by the popup manager as notification that the menu
            //		was opened.
            // tags:
            //		private

            this.isShowingNow = true;
        },

        onClose: function() {
            // summary:
            //		Callback when this menu is closed.
            //		This is called by the popup manager as notification that the menu
            //		was closed.
            // tags:
            //		private

            this._stopPopupTimer();
            this.parentMenu = null;
            this.isShowingNow = false;
            this.currentPopup = null;
            if (this.focusedChild) {
                this._onChildBlur(this.focusedChild);
                this.focusedChild = null;
            }
        },

        _onFocus: function() {
            // summary:
            //		Called when this Menu gets focus from:
            //			1) clicking it
            //			2) tabbing into it
            //			3) being opened by a parent menu.
            //		This is not called just from mouse hover.
            // tags:
            //		protected
            this.isActive = true;
            dojo.addClass(this.domNode, "dijitMenuActive");
            dojo.removeClass(this.domNode, "dijitMenuPassive");
            this.inherited(arguments);
        },

        _onBlur: function() {
            // summary:
            //		Called when focus is moved away from this Menu and it's submenus.
            // tags:
            //		protected
            this.isActive = false;
            dojo.removeClass(this.domNode, "dijitMenuActive");
            dojo.addClass(this.domNode, "dijitMenuPassive");

            // If user blurs/clicks away from a MenuBar (or always visible Menu), then close all popped up submenus etc.
            this.onClose();

            this.inherited(arguments);
        },

        _onDescendantExecute: function() {
            // summary:
            //		Called when submenu is clicked.  Close hierarchy of menus.
            // tags:
            //		private
            this.onClose();
        }
    });

    dojo.declare("dijit.Menu",
            dijit._MenuBase,
    {
        // summary
        //		A context menu you can assign to multiple elements

        // TODO: most of the code in here is just for context menu (right-click menu)
        // support.  In retrospect that should have been a separate class (dijit.ContextMenu).
        // Split them for 2.0

        constructor: function() {
            this._bindings = [];
        },

        templateString:"<table class=\"dijit dijitMenu dijitMenuPassive dijitReset dijitMenuTable\" waiRole=\"menu\" tabIndex=\"${tabIndex}\" dojoAttachEvent=\"onkeypress:_onKeyPress\">\r\n\t<tbody class=\"dijitReset\" dojoAttachPoint=\"containerNode\"></tbody>\r\n</table>\r\n",

        // targetNodeIds: [const] String[]
        //		Array of dom node ids of nodes to attach to.
        //		Fill this with nodeIds upon widget creation and it becomes context menu for those nodes.
        targetNodeIds: [],

        // contextMenuForWindow: [const] Boolean
        //		If true, right clicking anywhere on the window will cause this context menu to open.
        //		If false, must specify targetNodeIds.
        contextMenuForWindow: false,

        // leftClickToOpen: [const] Boolean
        //		If true, menu will open on left click instead of right click, similiar to a file menu.
        leftClickToOpen: false,

        // _contextMenuWithMouse: [private] Boolean
        //		Used to record mouse and keyboard events to determine if a context
        //		menu is being opened with the keyboard or the mouse.
        _contextMenuWithMouse: false,

        postCreate: function() {
            if (this.contextMenuForWindow) {
                this.bindDomNode(dojo.body());
            } else {
                dojo.forEach(this.targetNodeIds, this.bindDomNode, this);
            }
            var k = dojo.keys, l = this.isLeftToRight();
            this._openSubMenuKey = l ? k.RIGHT_ARROW : k.LEFT_ARROW;
            this._closeSubMenuKey = l ? k.LEFT_ARROW : k.RIGHT_ARROW;
            this.connectKeyNavHandlers([k.UP_ARROW], [k.DOWN_ARROW]);
        },

        _onKeyPress: function(/*Event*/ evt) {
            // summary:
            //		Handle keyboard based menu navigation.
            // tags:
            //		protected

            if (evt.ctrlKey || evt.altKey) {
                return;
            }

            switch (evt.charOrCode) {
                case this._openSubMenuKey:
                    this._moveToPopup(evt);
                    dojo.stopEvent(evt);
                    break;
                case this._closeSubMenuKey:
                    if (this.parentMenu) {
                        if (this.parentMenu._isMenuBar) {
                            this.parentMenu.focusPrev();
                        } else {
                            this.onCancel(false);
                        }
                    } else {
                        dojo.stopEvent(evt);
                    }
                    break;
            }
        },

        // thanks burstlib!
        _iframeContentWindow: function(/* HTMLIFrameElement */iframe_el) {
            // summary:
            //		Returns the window reference of the passed iframe
            // tags:
            //		private
            var win = dijit.getDocumentWindow(dijit.Menu._iframeContentDocument(iframe_el)) ||
                // Moz. TODO: is this available when defaultView isn't?
                      dijit.Menu._iframeContentDocument(iframe_el)['__parent__'] ||
                      (iframe_el.name && dojo.doc.frames[iframe_el.name]) || null;
            return win;	//	Window
        },

        _iframeContentDocument: function(/* HTMLIFrameElement */iframe_el) {
            // summary:
            //		Returns a reference to the document object inside iframe_el
            // tags:
            //		protected
            var doc = iframe_el.contentDocument // W3
                    || (iframe_el.contentWindow && iframe_el.contentWindow.document) // IE
                    || (iframe_el.name && dojo.doc.frames[iframe_el.name] && dojo.doc.frames[iframe_el.name].document)
                    || null;
            return doc;	//	HTMLDocument
        },

        bindDomNode: function(/*String|DomNode*/ node) {
            // summary:
            //		Attach menu to given node
            node = dojo.byId(node);

            //TODO: this is to support context popups in Editor.  Maybe this shouldn't be in dijit.Menu
            var win = dijit.getDocumentWindow(node.ownerDocument);
            if (node.tagName.toLowerCase() == "iframe") {
                win = this._iframeContentWindow(node);
                node = dojo.withGlobal(win, dojo.body);
            }

            // to capture these events at the top level,
            // attach to document, not body
            var cn = (node == dojo.body() ? dojo.doc : node);

            node[this.id] = this._bindings.push([
                dojo.connect(cn, (this.leftClickToOpen) ? "onclick" : "oncontextmenu", this, "_openMyself"),
                dojo.connect(cn, "onkeydown", this, "_contextKey"),
                dojo.connect(cn, "onmousedown", this, "_contextMouse")
            ]);
        },

        unBindDomNode: function(/*String|DomNode*/ nodeName) {
            // summary:
            //		Detach menu from given node
            var node = dojo.byId(nodeName);
            if (node) {
                var bid = node[this.id] - 1, b = this._bindings[bid];
                dojo.forEach(b, dojo.disconnect);
                delete this._bindings[bid];
            }
        },

        _contextKey: function(e) {
            // summary:
            //		Code to handle popping up editor using F10 key rather than mouse
            // tags:
            //		private
            this._contextMenuWithMouse = false;
            if (e.keyCode == dojo.keys.F10) {
                dojo.stopEvent(e);
                if (e.shiftKey && e.type == "keydown") {
                    // FF: copying the wrong property from e will cause the system
                    // context menu to appear in spite of stopEvent. Don't know
                    // exactly which properties cause this effect.
                    var _e = { target: e.target, pageX: e.pageX, pageY: e.pageY };
                    _e.preventDefault = _e.stopPropagation = function() {
                    };
                    // IE: without the delay, focus work in "open" causes the system
                    // context menu to appear in spite of stopEvent.
                    window.setTimeout(dojo.hitch(this, function() {
                        this._openMyself(_e);
                    }), 1);
                }
            }
        },

        _contextMouse: function(e) {
            // summary:
            //		Helper to remember when we opened the context menu with the mouse instead
            //		of with the keyboard
            // tags:
            //		private
            this._contextMenuWithMouse = true;
        },

        _openMyself: function(/*Event*/ e) {
            // summary:
            //		Internal function for opening myself when the user
            //		does a right-click or something similar
            // tags:
            //		private

            if (this.leftClickToOpen && e.button > 0) {
                return;
            }
            dojo.stopEvent(e);

            // Get coordinates.
            // if we are opening the menu with the mouse or on safari open
            // the menu at the mouse cursor
            // (Safari does not have a keyboard command to open the context menu
            // and we don't currently have a reliable way to determine
            // _contextMenuWithMouse on Safari)
            var x,y;
            if (dojo.isSafari || this._contextMenuWithMouse) {
                x = e.pageX;
                y = e.pageY;
            } else {
                // otherwise open near e.target
                var coords = dojo.coords(e.target, true);
                x = coords.x + 10;
                y = coords.y + 10;
            }

            var self = this;
            var savedFocus = dijit.getFocus(this);

            function closeAndRestoreFocus() {
                // user has clicked on a menu or popup
                dijit.focus(savedFocus);
                dijit.popup.close(self);
            }

            dijit.popup.open({
                popup: this,
                x: x,
                y: y,
                onExecute: closeAndRestoreFocus,
                onCancel: closeAndRestoreFocus,
                orient: this.isLeftToRight() ? 'L' : 'R'
            });
            this.focus();

            this._onBlur = function() {
                this.inherited('_onBlur', arguments);
                // Usually the parent closes the child widget but if this is a context
                // menu then there is no parent
                dijit.popup.close(this);
                // don't try to restore focus; user has clicked another part of the screen
                // and set focus there
            };
        },

        uninitialize: function() {
            dojo.forEach(this.targetNodeIds, this.unBindDomNode, this);
            this.inherited(arguments);
        }
    }
            );

    // Back-compat (TODO: remove in 2.0)


}

if (!dojo._hasResource["dijit.form._FormWidget"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.form._FormWidget"] = true;
    dojo.provide("dijit.form._FormWidget");


    dojo.declare("dijit.form._FormWidget", [dijit._Widget, dijit._Templated],
    {
        //
        // summary:
        //		Base class for widgets corresponding to native HTML elements such as <checkbox> or <button>,
        //		which can be children of a <form> node or a `dijit.form.Form` widget.
        //
        // description:
        //		Represents a single HTML element.
        //		All these widgets should have these attributes just like native HTML input elements.
        //		You can set them during widget construction or afterwards, via `dijit._Widget.attr`.
        //
        //	They also share some common methods.

        // baseClass: [protected] String
        //		Root CSS class of the widget (ex: dijitTextBox), used to add CSS classes of widget
        //		(ex: "dijitTextBox dijitTextBoxInvalid dijitTextBoxFocused dijitTextBoxInvalidFocused")
        //		See _setStateClass().
        baseClass: "",

        // name: String
        //		Name used when submitting form; same as "name" attribute or plain HTML elements
        name: "",

        // alt: String
        //		Corresponds to the native HTML <input> element's attribute.
        alt: "",

        // value: String
        //		Corresponds to the native HTML <input> element's attribute.
        value: "",

        // type: String
        //		Corresponds to the native HTML <input> element's attribute.
        type: "text",

        // tabIndex: Integer
        //		Order fields are traversed when user hits the tab key
        tabIndex: "0",

        // disabled: Boolean
        //		Should this widget respond to user input?
        //		In markup, this is specified as "disabled='disabled'", or just "disabled".
        disabled: false,

        // readOnly: Boolean
        //		Should this widget respond to user input?
        //		In markup, this is specified as "readOnly".
        //		Similar to disabled except readOnly form values are submitted.
        readOnly: false,

        // intermediateChanges: Boolean
        //		Fires onChange for each value change or only on demand
        intermediateChanges: false,

        // scrollOnFocus: Boolean
        //		On focus, should this widget scroll into view?
        scrollOnFocus: true,

        // These mixins assume that the focus node is an INPUT, as many but not all _FormWidgets are.
        attributeMap: dojo.delegate(dijit._Widget.prototype.attributeMap, {
            value: "focusNode",
            disabled: "focusNode",
            readOnly: "focusNode",
            id: "focusNode",
            tabIndex: "focusNode",
            alt: "focusNode"
        }),

        postMixInProperties: function() {
            // Setup name=foo string to be referenced from the template (but only if a name has been specified)
            // Unfortunately we can't use attributeMap to set the name due to IE limitations, see #8660
            this.nameAttrSetting = this.name ? ("name='" + this.name + "'") : "";
            this.inherited(arguments);
        },

        _setDisabledAttr: function(/*Boolean*/ value) {
            this.disabled = value;
            dojo.attr(this.focusNode, 'disabled', value);
            dijit.setWaiState(this.focusNode, "disabled", value);

            if (value) {
                //reset those, because after the domNode is disabled, we can no longer receive
                //mouse related events, see #4200
                this._hovering = false;
                this._active = false;
                // remove the tabIndex, especially for FF
                this.focusNode.removeAttribute('tabIndex');
            } else {
                this.focusNode.setAttribute('tabIndex', this.tabIndex);
            }
            this._setStateClass();
        },

        setDisabled: function(/*Boolean*/ disabled) {
            // summary:
            //		Deprecated.   Use attr('disabled', ...) instead.
            dojo.deprecated("setDisabled(" + disabled + ") is deprecated. Use attr('disabled'," + disabled + ") instead.", "", "2.0");
            this.attr('disabled', disabled);
        },

        _onFocus: function(e) {
            if (this.scrollOnFocus) {
                dijit.scrollIntoView(this.domNode);
            }
            this.inherited(arguments);
        },

        _onMouse : function(/*Event*/ event) {
            // summary:
            //	Sets _hovering, _active, and stateModifier properties depending on mouse state,
            //	then calls setStateClass() to set appropriate CSS classes for this.domNode.
            //
            //	To get a different CSS class for hover, send onmouseover and onmouseout events to this method.
            //	To get a different CSS class while mouse button is depressed, send onmousedown to this method.

            var mouseNode = event.currentTarget;
            if (mouseNode && mouseNode.getAttribute) {
                this.stateModifier = mouseNode.getAttribute("stateModifier") || "";
            }

            if (!this.disabled) {
                switch (event.type) {
                    case "mouseenter":
                    case "mouseover":
                        this._hovering = true;
                        this._active = this._mouseDown;
                        break;

                    case "mouseout":
                    case "mouseleave":
                        this._hovering = false;
                        this._active = false;
                        break;

                    case "mousedown" :
                        this._active = true;
                        this._mouseDown = true;
                        // set a global event to handle mouseup, so it fires properly
                        //	even if the cursor leaves the button
                        var mouseUpConnector = this.connect(dojo.body(), "onmouseup", function() {
                            //if user clicks on the button, even if the mouse is released outside of it,
                            //this button should get focus (which mimics native browser buttons)
                            if (this._mouseDown && this.isFocusable()) {
                                this.focus();
                            }
                            this._active = false;
                            this._mouseDown = false;
                            this._setStateClass();
                            this.disconnect(mouseUpConnector);
                        });
                        break;
                }
                this._setStateClass();
            }
        },

        isFocusable: function() {
            // summary:
            //		Tells if this widget is focusable or not.   Used internally by dijit.
            // tags:
            //		protected
            return !this.disabled && !this.readOnly && this.focusNode && (dojo.style(this.domNode, "display") != "none");
        },

        focus: function() {
            // summary:
            //		Put focus on this widget
            dijit.focus(this.focusNode);
        },

        _setStateClass: function() {
            // summary:
            //		Update the visual state of the widget by setting the css classes on this.domNode
            //		(or this.stateNode if defined) by combining this.baseClass with
            //		various suffixes that represent the current widget state(s).
            //
            // description:
            //		In the case where a widget has multiple
            //		states, it sets the class based on all possible
            //	 	combinations.  For example, an invalid form widget that is being hovered
            //		will be "dijitInput dijitInputInvalid dijitInputHover dijitInputInvalidHover".
            //
            //		For complex widgets with multiple regions, there can be various hover/active states,
            //		such as "Hover" or "CloseButtonHover" (for tab buttons).
            //		This is controlled by a stateModifier="CloseButton" attribute on the close button node.
            //
            //		The widget may have one or more of the following states, determined
            //		by this.state, this.checked, this.valid, and this.selected:
            //			- Error - ValidationTextBox sets this.state to "Error" if the current input value is invalid
            //			- Checked - ex: a checkmark or a ToggleButton in a checked state, will have this.checked==true
            //			- Selected - ex: currently selected tab will have this.selected==true
            //
            //		In addition, it may have one or more of the following states,
            //		based on this.disabled and flags set in _onMouse (this._active, this._hovering, this._focused):
            //			- Disabled	- if the widget is disabled
            //			- Active		- if the mouse (or space/enter key?) is being pressed down
            //			- Focused		- if the widget has focus
            //			- Hover		- if the mouse is over the widget

            // Compute new set of classes
            var newStateClasses = this.baseClass.split(" ");

            function multiply(modifier) {
                newStateClasses = newStateClasses.concat(dojo.map(newStateClasses, function(c) {
                    return c + modifier;
                }), "dijit" + modifier);
            }

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
            } else if (this.readOnly) {
                multiply("ReadOnly");
            } else if (this._active) {
                multiply(this.stateModifier + "Active");
            } else {
                if (this._focused) {
                    multiply("Focused");
                }
                if (this._hovering) {
                    multiply(this.stateModifier + "Hover");
                }
            }

            // Remove old state classes and add new ones.
            // For performance concerns we only write into domNode.className once.
            var tn = this.stateNode || this.domNode,
                    classHash = {};	// set of all classes (state and otherwise) for node

            dojo.forEach(tn.className.split(" "), function(c) {
                classHash[c] = true;
            });

            if ("_stateClasses" in this) {
                dojo.forEach(this._stateClasses, function(c) {
                    delete classHash[c];
                });
            }

            dojo.forEach(newStateClasses, function(c) {
                classHash[c] = true;
            });

            var newClasses = [];
            for (var c in classHash) {
                newClasses.push(c);
            }
            tn.className = newClasses.join(" ");

            this._stateClasses = newStateClasses;
        },

        compare: function(/*anything*/val1, /*anything*/val2) {
            // summary:
            //		Compare 2 values (as returned by attr('value') for this widget).
            // tags:
            //		protected
            if ((typeof val1 == "number") && (typeof val2 == "number")) {
                return (isNaN(val1) && isNaN(val2)) ? 0 : (val1 - val2);
            } else if (val1 > val2) {
                return 1;
            }
            else if (val1 < val2) {
                    return -1;
                }
                else {
                    return 0;
                }
        },

        onChange: function(newValue) {
            // summary:
            //		Callback when this widget's value is changed.
            // tags:
            //		callback
        },

        // _onChangeActive: [private] Boolean
        //		Indicates that changes to the value should call onChange() callback.
        //		This is false during widget initialization, to avoid calling onChange()
        //		when the initial value is set.
        _onChangeActive: false,

        _handleOnChange: function(/*anything*/ newValue, /* Boolean? */ priorityChange) {
            // summary:
            //		Called when the value of the widget is set.  Calls onChange() if appropriate
            // newValue:
            //		the new value
            // priorityChange:
            //		For a slider, for example, dragging the slider is priorityChange==false,
            //		but on mouse up, it's priorityChange==true.  If intermediateChanges==true,
            //		onChange is only called form priorityChange=true events.
            // tags:
            //		private
            this._lastValue = newValue;
            if (this._lastValueReported == undefined && (priorityChange === null || !this._onChangeActive)) {
                // this block executes not for a change, but during initialization,
                // and is used to store away the original value (or for ToggleButton, the original checked state)
                this._resetValue = this._lastValueReported = newValue;
            }
            if ((this.intermediateChanges || priorityChange || priorityChange === undefined) &&
                ((typeof newValue != typeof this._lastValueReported) ||
                 this.compare(newValue, this._lastValueReported) != 0)) {
                this._lastValueReported = newValue;
                if (this._onChangeActive) {
                    this.onChange(newValue);
                }
            }
        },

        create: function() {
            // Overrides _Widget.create()
            this.inherited(arguments);
            this._onChangeActive = true;
            this._setStateClass();
        },

        destroy: function() {
            if (this._layoutHackHandle) {
                clearTimeout(this._layoutHackHandle);
            }
            this.inherited(arguments);
        },

        setValue: function(/*String*/ value) {
            // summary:
            //		Deprecated.   Use attr('value', ...) instead.
            dojo.deprecated("dijit.form._FormWidget:setValue(" + value + ") is deprecated.  Use attr('value'," + value + ") instead.", "", "2.0");
            this.attr('value', value);
        },

        getValue: function() {
            // summary:
            //		Deprecated.   Use attr('value') instead.
            dojo.deprecated(this.declaredClass + "::getValue() is deprecated. Use attr('value') instead.", "", "2.0");
            return this.attr('value');
        },

        _layoutHack: function() {
            // summary:
            //		Work around table sizing bugs on FF2 by forcing redraw

            if (dojo.isFF == 2 && !this._layoutHackHandle) {
                var node = this.domNode;
                var old = node.style.opacity;
                node.style.opacity = "0.999";
                this._layoutHackHandle = setTimeout(dojo.hitch(this, function() {
                    this._layoutHackHandle = null;
                    node.style.opacity = old;
                }), 0);
            }
        }
    });

    dojo.declare("dijit.form._FormValueWidget", dijit.form._FormWidget,
    {
        // summary:
        //		Base class for widgets corresponding to native HTML elements such as <input> or <select> that have user changeable values.
        // description:
        //		Each _FormValueWidget represents a single input value, and has a (possibly hidden) <input> element,
        //		to which it serializes it's input value, so that form submission (either normal submission or via FormBind?)
        //		works as expected.

        // Don't attempt to mixin the 'type', 'name' attributes here programatically -- they must be declared
        // directly in the template as read by the parser in order to function. IE is known to specifically
        // require the 'name' attribute at element creation time.   See #8484, #8660.
        // TODO: unclear what that {value: ""} is for; FormWidget.attributeMap copies value to focusNode,
        // so maybe {value: ""} is so the value *doesn't* get copied to focusNode?
        // Seems like we really want value removed from attributeMap altogether
        // (although there's no easy way to do that now)
        attributeMap: dojo.delegate(dijit.form._FormWidget.prototype.attributeMap, { value: "" }),

        postCreate: function() {
            if (dojo.isIE || dojo.isWebKit) { // IE won't stop the event with keypress and Safari won't send an ESCAPE to keypress at all
                this.connect(this.focusNode || this.domNode, "onkeydown", this._onKeyDown);
            }
            // Update our reset value if it hasn't yet been set (because this.attr
            // is only called when there *is* a value
            if (this._resetValue === undefined) {
                this._resetValue = this.value;
            }
        },

        _setValueAttr: function(/*anything*/ newValue, /*Boolean, optional*/ priorityChange) {
            // summary:
            //		Hook so attr('value', value) works.
            // description:
            //		Sets the value of the widget.
            //		If the value has changed, then fire onChange event, unless priorityChange
            //		is specified as null (or false?)
            this.value = newValue;
            this._handleOnChange(newValue, priorityChange);
        },

        _getValueAttr: function(/*String*/ value) {
            // summary:
            //		Hook so attr('value') works.
            return this._lastValue;
        },

        undo: function() {
            // summary:
            //		Restore the value to the last value passed to onChange
            this._setValueAttr(this._lastValueReported, false);
        },

        reset: function() {
            // summary:
            //		Reset the widget's value to what it was at initialization time
            this._hasBeenBlurred = false;
            this._setValueAttr(this._resetValue, true);
        },

        _onKeyDown: function(e) {
            if (e.keyCode == dojo.keys.ESCAPE && !e.ctrlKey && !e.altKey) {
                var te;
                if (dojo.isIE) {
                    e.preventDefault(); // default behavior needs to be stopped here since keypress is too late
                    te = document.createEventObject();
                    te.keyCode = dojo.keys.ESCAPE;
                    te.shiftKey = e.shiftKey;
                    e.srcElement.fireEvent('onkeypress', te);
                } else if (dojo.isWebKit) { // ESCAPE needs help making it into keypress
                    te = document.createEvent('Events');
                    te.initEvent('keypress', true, true);
                    te.keyCode = dojo.keys.ESCAPE;
                    te.shiftKey = e.shiftKey;
                    e.target.dispatchEvent(te);
                }
            }
        }
    });

}

if (!dojo._hasResource["dijit.form.TextBox"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.form.TextBox"] = true;
    dojo.provide("dijit.form.TextBox");


    dojo.declare(
            "dijit.form.TextBox",
            dijit.form._FormValueWidget,
    {
        //	summary:
        //		A base class for textbox form inputs

        //	trim: Boolean
        //		Removes leading and trailing whitespace if true.  Default is false.
        trim: false,

        //	uppercase: Boolean
        //		Converts all characters to uppercase if true.  Default is false.
        uppercase: false,

        //	lowercase: Boolean
        //		Converts all characters to lowercase if true.  Default is false.
        lowercase: false,

        //	propercase: Boolean
        //		Converts the first character of each word to uppercase if true.
        propercase: false,

        //	maxLength: String
        //		HTML INPUT tag maxLength declaration.
        maxLength: "",

        templateString:"<input class=\"dijit dijitReset dijitLeft\" dojoAttachPoint='textbox,focusNode'\r\n\tdojoAttachEvent='onmouseenter:_onMouse,onmouseleave:_onMouse'\r\n\tautocomplete=\"off\" type=\"${type}\" ${nameAttrSetting}\r\n\t/>\r\n",
        baseClass: "dijitTextBox",

        attributeMap: dojo.delegate(dijit.form._FormValueWidget.prototype.attributeMap, {
            maxLength: "focusNode"
        }),

        _getValueAttr: function() {
            // summary:
            //		Hook so attr('value') works as we like.
            // description:
            //		For `dijit.form.TextBox` this basically returns the value of the <input>.
            //
            //		For `dijit.form.MappedTextBox` subclasses, which have both
            //		a "displayed value" and a separate "submit value",
            //		This treats the "displayed value" as the master value, computing the
            //		submit value from it via this.parse().
            return this.parse(this.attr('displayedValue'), this.constraints);
        },

        _setValueAttr: function(value, /*Boolean?*/ priorityChange, /*String?*/ formattedValue) {
            // summary:
            //		Hook so attr('value', ...) works.
            //
            // description:
            //		Sets the value of the widget to "value" which can be of
            //		any type as determined by the widget.
            //
            // value:
            //		The visual element value is also set to a corresponding,
            //		but not necessarily the same, value.
            //
            // formattedValue:
            //		If specified, used to set the visual element value,
            //		otherwise a computed visual value is used.
            //
            // priorityChange:
            //		If true, an onChange event is fired immediately instead of
            //		waiting for the next blur event.

            var filteredValue;
            if (value !== undefined) {
                // TODO: this is calling filter() on both the display value and the actual value.
                // I added a comment to the filter() definition about this, but it should be changed.
                filteredValue = this.filter(value);
                if (typeof formattedValue != "string") {
                    if (filteredValue !== null && ((typeof filteredValue != "number") || !isNaN(filteredValue))) {
                        formattedValue = this.filter(this.format(filteredValue, this.constraints));
                    } else {
                        formattedValue = '';
                    }
                }
            }
            if (formattedValue != null && formattedValue != undefined && ((typeof formattedValue) != "number" || !isNaN(formattedValue)) && this.textbox.value != formattedValue) {
                this.textbox.value = formattedValue;
            }
            this.inherited(arguments, [filteredValue, priorityChange]);
        },

        // displayedValue: String
        //		For subclasses like ComboBox where the displayed value
        //		(ex: Kentucky) and the serialized value (ex: KY) are different,
        //		this represents the displayed value.
        //
        //		Setting 'displayedValue' through attr('displayedValue', ...)
        //		updates 'value', and vice-versa.  Othewise 'value' is updated
        //		from 'displayedValue' periodically, like onBlur etc.
        //
        //		TODO: move declaration to MappedTextBox?
        //		Problem is that ComboBox references displayedValue,
        //		for benefit of FilteringSelect.
        displayedValue: "",

        getDisplayedValue: function() {
            // summary:
            //		Deprecated.   Use attr('displayedValue') instead.
            // tags:
            //		deprecated
            dojo.deprecated(this.declaredClass + "::getDisplayedValue() is deprecated. Use attr('displayedValue') instead.", "", "2.0");
            return this.attr('displayedValue');
        },

        _getDisplayedValueAttr: function() {
            // summary:
            //		Hook so attr('displayedValue') works.
            // description:
            //		Returns the displayed value (what the user sees on the screen),
            // 		after filtering (ie, trimming spaces etc.).
            //
            //		For some subclasses of TextBox (like ComboBox), the displayed value
            //		is different from the serialized value that's actually
            //		sent to the server (see dijit.form.ValidationTextBox.serialize)

            return this.filter(this.textbox.value);
        },

        setDisplayedValue: function(/*String*/value) {
            // summary:
            //		Deprecated.   Use attr('displayedValue', ...) instead.
            // tags:
            //		deprecated
            dojo.deprecated(this.declaredClass + "::setDisplayedValue() is deprecated. Use attr('displayedValue', ...) instead.", "", "2.0");
            this.attr('displayedValue', value);
        },

        _setDisplayedValueAttr: function(/*String*/value) {
            // summary:
            //		Hook so attr('displayedValue', ...) works.
            //	description:
            //		Sets the value of the visual element to the string "value".
            //		The widget value is also set to a corresponding,
            //		but not necessarily the same, value.

            if (value === null || value === undefined) {
                value = ''
            }
            else if (typeof value != "string") {
                value = String(value)
            }
            this.textbox.value = value;
            this._setValueAttr(this.attr('value'), undefined, value);
        },

        format: function(/* String */ value, /* Object */ constraints) {
            // summary:
            //		Replacable function to convert a value to a properly formatted string.
            // tags:
            //		protected extension
            return ((value == null || value == undefined) ? "" : (value.toString ? value.toString() : value));
        },

        parse: function(/* String */ value, /* Object */ constraints) {
            // summary:
            //		Replacable function to convert a formatted string to a value
            // tags:
            //		protected extension

            return value;	// String
        },

        _refreshState: function() {
            // summary:
            //		After the user types some characters, etc., this method is
            //		called to check the field for validity etc.  The base method
            //		in `dijit.form.TextBox` does nothing, but subclasses override.
            // tags:
            //		protected
        },

        _onInput: function(e) {
            if (e && e.type && /key/i.test(e.type) && e.keyCode) {
                switch (e.keyCode) {
                    case dojo.keys.SHIFT:
                    case dojo.keys.ALT:
                    case dojo.keys.CTRL:
                    case dojo.keys.TAB:
                        return;
                }
            }
            if (this.intermediateChanges) {
                var _this = this;
                // the setTimeout allows the key to post to the widget input box
                setTimeout(function() {
                    _this._handleOnChange(_this.attr('value'), false);
                }, 0);
            }
            this._refreshState();
        },

        postCreate: function() {
            // setting the value here is needed since value="" in the template causes "undefined"
            // and setting in the DOM (instead of the JS object) helps with form reset actions
            this.textbox.setAttribute("value", this.textbox.value); // DOM and JS values shuld be the same
            this.inherited(arguments);
            if (dojo.isMoz || dojo.isOpera) {
                this.connect(this.textbox, "oninput", this._onInput);
            } else {
                this.connect(this.textbox, "onkeydown", this._onInput);
                this.connect(this.textbox, "onkeyup", this._onInput);
                this.connect(this.textbox, "onpaste", this._onInput);
                this.connect(this.textbox, "oncut", this._onInput);
            }

            /*#5297:if(this.srcNodeRef){
             dojo.style(this.textbox, "cssText", this.style);
             this.textbox.className += " " + this["class"];
             }*/
            this._layoutHack();
        },

        _blankValue: '', // if the textbox is blank, what value should be reported
        filter: function(val) {
            // summary:
            //		Auto-corrections (such as trimming) that are applied to textbox
            //		value on blur or form submit.
            // description:
            //		For MappedTextBox subclasses, this is called twice
            // 			- once with the display value
            //			- once the value as set/returned by attr('value', ...)
            //		and attr('value'), ex: a Number for NumberTextBox.
            //
            //		In the latter case it does corrections like converting null to NaN.  In
            //		the former case the NumberTextBox.filter() method calls this.inherited()
            //		to execute standard trimming code in TextBox.filter().
            //
            //		TODO: break this into two methods in 2.0
            //
            // tags:
            //		protected extension
            if (val === null) {
                return this._blankValue;
            }
            if (typeof val != "string") {
                return val;
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
        },

        _setBlurValue: function() {
            this._setValueAttr(this.attr('value'), true);
        },

        _onBlur: function(e) {
            if (this.disabled) {
                return;
            }
            this._setBlurValue();
            this.inherited(arguments);
        },

        _onFocus: function(e) {
            if (this.disabled) {
                return;
            }
            this._refreshState();
            this.inherited(arguments);
        },

        reset: function() {
            // Overrides dijit._FormWidget.reset().
            // Additionally resets the displayed textbox value to ''
            this.textbox.value = '';
            this.inherited(arguments);
        }
    }
            );

    dijit.selectInputText = function(/*DomNode*/element, /*Number?*/ start, /*Number?*/ stop) {
        // summary:
        //		Select text in the input element argument, from start (default 0), to stop (default end).

        // TODO: use functions in _editor/selection.js?
        var _window = dojo.global;
        var _document = dojo.doc;
        element = dojo.byId(element);
        if (isNaN(start)) {
            start = 0;
        }
        if (isNaN(stop)) {
            stop = element.value ? element.value.length : 0;
        }
        element.focus();
        if (_document["selection"] && dojo.body()["createTextRange"]) { // IE
            if (element.createTextRange) {
                var range = element.createTextRange();
                with (range) {
                    collapse(true);
                    moveStart("character", start);
                    moveEnd("character", stop);
                    select();
                }
            }
        } else if (_window["getSelection"]) {
            var selection = _window.getSelection();	// TODO: unused, remove
            // FIXME: does this work on Safari?
            if (element.setSelectionRange) {
                element.setSelectionRange(start, stop);
            }
        }
    };

}

if (!dojo._hasResource["dijit.form.ValidationTextBox"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.form.ValidationTextBox"] = true;
    dojo.provide("dijit.form.ValidationTextBox");


    /*=====
     dijit.form.ValidationTextBox.__Constraints = function(){
     // locale: String
     //		locale used for validation, picks up value from this widget's lang attribute
     // _flags_: anything
     //		various flags passed to regExpGen function
     this.locale = "";
     this._flags_ = "";
     }
     =====*/

    dojo.declare(
            "dijit.form.ValidationTextBox",
            dijit.form.TextBox,
    {
        // summary:
        //		Base class for textbox widgets with the ability to validate content of various types and provide user feedback.
        // tags:
        //		protected

        templateString:"<div class=\"dijit dijitReset dijitInlineTable dijitLeft\"\r\n\tid=\"widget_${id}\"\r\n\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\" waiRole=\"presentation\"\r\n\t><div style=\"overflow:hidden;\"\r\n\t\t><div class=\"dijitReset dijitValidationIcon\"><br></div\r\n\t\t><div class=\"dijitReset dijitValidationIconText\">&Chi;</div\r\n\t\t><div class=\"dijitReset dijitInputField\"\r\n\t\t\t><input class=\"dijitReset\" dojoAttachPoint='textbox,focusNode' autocomplete=\"off\"\r\n\t\t\t${nameAttrSetting} type='${type}'\r\n\t\t/></div\r\n\t></div\r\n></div>\r\n",
        baseClass: "dijitTextBox",

        // required: Boolean
        //		User is required to enter data into this field.
        required: false,

        // promptMessage: String
        //		If defined, display this hint string immediately on focus to the textbox, if empty.
        //		Think of this like a tooltip that tells the user what to do, not an error message
        //		that tells the user what they've done wrong.
        //
        //		Message disappears when user starts typing.
        promptMessage: "",

        // invalidMessage: String
        // 		The message to display if value is invalid.
        invalidMessage: "$_unset_$", // read from the message file if not overridden

        // constraints: dijit.form.ValidationTextBox.__Constraints
        //		user-defined object needed to pass parameters to the validator functions
        constraints: {},

        // regExp: [extension protected] String
        //		regular expression string used to validate the input
        //		Do not specify both regExp and regExpGen
        regExp: ".*",

        regExpGen: function(/*dijit.form.ValidationTextBox.__Constraints*/constraints) {
            // summary:
            //		Overridable function used to generate regExp when dependent on constraints.
            //		Do not specify both regExp and regExpGen.
            // tags:
            //		extension protected
            return this.regExp;     // String
        },

        // state: [readonly] String
        //		Shows current state (ie, validation result) of input (Normal, Warning, or Error)
        state: "",

        // tooltipPosition: String[]
        //		See description of `dijit.Tooltip.defaultPosition` for details on this parameter.
        tooltipPosition: [],

        _setValueAttr: function() {
            // summary:
            //		Hook so attr('value', ...) works.
            this.inherited(arguments);
            this.validate(this._focused);
        },

        validator: function(/*anything*/value, /*dijit.form.ValidationTextBox.__Constraints*/constraints) {
            // summary:
            //		Overridable function used to validate the text input against the regular expression.
            // tags:
            //		protected
            return (new RegExp("^(?:" + this.regExpGen(constraints) + ")" + (this.required ? "" : "?") + "$")).test(value) &&
                   (!this.required || !this._isEmpty(value)) &&
                   (this._isEmpty(value) || this.parse(value, constraints) !== undefined); // Boolean
        },

        _isValidSubset: function() {
            // summary:
            //		Returns true if the value is either already valid or could be made valid by appending characters.
            //		This is used for validation while the user [may be] still typing.
            return this.textbox.value.search(this._partialre) == 0;
        },

        isValid: function(/*Boolean*/ isFocused) {
            // summary:
            //		Tests if value is valid.
            //		Can override with your own routine in a subclass.
            // tags:
            //		protected
            return this.validator(this.textbox.value, this.constraints);
        },

        _isEmpty: function(value) {
            // summary:
            //		Checks for whitespace
            return /^\s*$/.test(value); // Boolean
        },

        getErrorMessage: function(/*Boolean*/ isFocused) {
            // summary:
            //		Return an error message to show if appropriate
            // tags:
            //		protected
            return this.invalidMessage; // String
        },

        getPromptMessage: function(/*Boolean*/ isFocused) {
            // summary:
            //		Return a hint message to show when widget is first focused
            // tags:
            //		protected
            return this.promptMessage; // String
        },

        _maskValidSubsetError: true,
        validate: function(/*Boolean*/ isFocused) {
            // summary:
            //		Called by oninit, onblur, and onkeypress.
            // description:
            //		Show missing or invalid messages if appropriate, and highlight textbox field.
            // tags:
            //		protected
            var message = "";
            var isValid = this.disabled || this.isValid(isFocused);
            if (isValid) {
                this._maskValidSubsetError = true;
            }
            var isValidSubset = !isValid && isFocused && this._isValidSubset();
            var isEmpty = this._isEmpty(this.textbox.value);
            this.state = (isValid || (!this._hasBeenBlurred && isEmpty) || isValidSubset) ? "" : "Error";
            if (this.state == "Error") {
                this._maskValidSubsetError = false;
            }
            this._setStateClass();
            dijit.setWaiState(this.focusNode, "invalid", isValid ? "false" : "true");
            if (isFocused) {
                if (isEmpty) {
                    message = this.getPromptMessage(true);
                }
                if (!message && (this.state == "Error" || (isValidSubset && !this._maskValidSubsetError))) {
                    message = this.getErrorMessage(true);
                }
            }
            this.displayMessage(message);
            return isValid;
        },

        // _message: String
        //		Currently displayed message
        _message: "",

        displayMessage: function(/*String*/ message) {
            // summary:
            //		Overridable method to display validation errors/hints.
            //		By default uses a tooltip.
            // tags:
            //		extension
            if (this._message == message) {
                return;
            }
            this._message = message;
            dijit.hideTooltip(this.domNode);
            if (message) {
                dijit.showTooltip(message, this.domNode, this.tooltipPosition);
            }
        },

        _refreshState: function() {
            // Overrides TextBox._refreshState()
            this.validate(this._focused);
            this.inherited(arguments);
        },

        //////////// INITIALIZATION METHODS ///////////////////////////////////////

        constructor: function() {
            this.constraints = {};
        },

        postMixInProperties: function() {
            this.inherited(arguments);
            this.constraints.locale = this.lang;
            this.messages = dojo.i18n.getLocalization("dijit.form", "validate", this.lang);
            if (this.invalidMessage == "$_unset_$") {
                this.invalidMessage = this.messages.invalidMessage;
            }
            var p = this.regExpGen(this.constraints);
            this.regExp = p;
            var partialre = "";
            // parse the regexp and produce a new regexp that matches valid subsets
            // if the regexp is .* then there's no use in matching subsets since everything is valid
            if (p != ".*") {
                this.regExp.replace(/\\.|\[\]|\[.*?[^\\]{1}\]|\{.*?\}|\(\?[=:!]|./g,
                        function (re) {
                            switch (re.charAt(0)) {
                                case '{':
                                case '+':
                                case '?':
                                case '*':
                                case '^':
                                case '$':
                                case '|':
                                case '(': partialre += re; break;
                                case ")": partialre += "|$)"; break;
                                default: partialre += "(?:" + re + "|$)"; break;
                            }
                        }
                        );
            }
            try { // this is needed for now since the above regexp parsing needs more test verification
                "".search(partialre);
            } catch(e) { // should never be here unless the original RE is bad or the parsing is bad
                partialre = this.regExp;
                console.warn('RegExp error in ' + this.declaredClass + ': ' + this.regExp);
            } // should never be here unless the original RE is bad or the parsing is bad
            this._partialre = "^(?:" + partialre + ")$";
        },

        _setDisabledAttr: function(/*Boolean*/ value) {
            this.inherited(arguments);	// call FormValueWidget._setDisabledAttr()
            if (this.valueNode) {
                this.valueNode.disabled = value;
            }
            this._refreshState();
        },

        _setRequiredAttr: function(/*Boolean*/ value) {
            this.required = value;
            dijit.setWaiState(this.focusNode, "required", value);
            this._refreshState();
        },

        postCreate: function() {
            if (dojo.isIE) { // IE INPUT tag fontFamily has to be set directly using STYLE
                var s = dojo.getComputedStyle(this.focusNode);
                if (s) {
                    var ff = s.fontFamily;
                    if (ff) {
                        this.focusNode.style.fontFamily = ff;
                    }
                }
            }
            this.inherited(arguments);
        },

        reset:function() {
            // Overrides dijit.form.TextBox.reset() by also
            // hiding errors about partial matches
            this._maskValidSubsetError = true;
            this.inherited(arguments);
        }
    }
            );

    dojo.declare(
            "dijit.form.MappedTextBox",
            dijit.form.ValidationTextBox,
    {
        // summary:
        //		A dijit.form.ValidationTextBox subclass which provides a base class for widgets that have
        //		a visible formatted display value, and a serializable
        //		value in a hidden input field which is actually sent to the server.
        // description:
        //		The visible display may
        //		be locale-dependent and interactive.  The value sent to the server is stored in a hidden
        //		input field which uses the `name` attribute declared by the original widget.  That value sent
        //		to the server is defined by the dijit.form.MappedTextBox.serialize method and is typically
        //		locale-neutral.
        // tags:
        //		protected

        postMixInProperties: function() {
            this.inherited(arguments);

            // we want the name attribute to go to the hidden <input>, not the displayed <input>,
            // so override _FormWidget.postMixInProperties() setting of nameAttrSetting
            this.nameAttrSetting = "";
        },

        serialize: function(/*anything*/val, /*Object?*/options) {
            // summary:
            //		Overridable function used to convert the attr('value') result to a canonical
            //		(non-localized) string.  For example, will print dates in ISO format, and
            //		numbers the same way as they are represented in javascript.
            // tags:
            //		protected extension
            return val.toString ? val.toString() : ""; // String
        },

        toString: function() {
            // summary:
            //		Returns widget as a printable string using the widget's value
            // tags:
            //		protected
            var val = this.filter(this.attr('value')); // call filter in case value is nonstring and filter has been customized
            return val != null ? (typeof val == "string" ? val : this.serialize(val, this.constraints)) : ""; // String
        },

        validate: function() {
            // Overrides `dijit.form.TextBox.validate`
            this.valueNode.value = this.toString();
            return this.inherited(arguments);
        },

        buildRendering: function() {
            // Overrides `dijit._Templated.buildRendering`

            this.inherited(arguments);

            // Create a hidden <input> node with the serialized value used for submit
            // (as opposed to the displayed value)
            this.valueNode = dojo.create("input", {
                style: { display: "none" },
                type: this.type,
                name: this.name
            }, this.textbox, "after");
        },

        _setDisabledAttr: function(/*Boolean*/ value) {
            this.inherited(arguments);
            dojo.attr(this.valueNode, 'disabled', value);
        },

        reset:function() {
            // Overrides `dijit.form.ValidationTextBox.reset` to
            // reset the hidden textbox value to ''
            this.valueNode.value = '';
            this.inherited(arguments);
        }
    }
            );

    /*=====
     dijit.form.RangeBoundTextBox.__Constraints = function(){
     // min: Number
     //		Minimum signed value.  Default is -Infinity
     // max: Number
     //		Maximum signed value.  Default is +Infinity
     this.min = min;
     this.max = max;
     }
     =====*/

    dojo.declare(
            "dijit.form.RangeBoundTextBox",
            dijit.form.MappedTextBox,
    {
        // summary:
        //		Base class for textbox form widgets which defines a range of valid values.

        // rangeMessage: String
        //		The message to display if value is out-of-range
        rangeMessage: "",

        /*=====
         // constraints: dijit.form.RangeBoundTextBox.__Constraints
         constraints: {},
         ======*/

        rangeCheck: function(/*Number*/ primitive, /*dijit.form.RangeBoundTextBox.__Constraints*/ constraints) {
            // summary:
            //		Overridable function used to validate the range of the numeric input value.
            // tags:
            //		protected
            var isMin = "min" in constraints;
            var isMax = "max" in constraints;
            if (isMin || isMax) {
                return (!isMin || this.compare(primitive, constraints.min) >= 0) &&
                       (!isMax || this.compare(primitive, constraints.max) <= 0);
            }
            return true; // Boolean
        },

        isInRange: function(/*Boolean*/ isFocused) {
            // summary:
            //		Tests if the value is in the min/max range specified in constraints
            // tags:
            //		protected
            return this.rangeCheck(this.attr('value'), this.constraints);
        },

        _isDefinitelyOutOfRange: function() {
            // summary:
            //		Returns true if the value is out of range and will remain
            //		out of range even if the user types more characters
            var val = this.attr('value');
            var isTooLittle = false;
            var isTooMuch = false;
            if ("min" in this.constraints) {
                var min = this.constraints.min;
                val = this.compare(val, ((typeof min == "number") && min >= 0 && val != 0) ? 0 : min);
                isTooLittle = (typeof val == "number") && val < 0;
            }
            if ("max" in this.constraints) {
                var max = this.constraints.max;
                val = this.compare(val, ((typeof max != "number") || max > 0) ? max : 0);
                isTooMuch = (typeof val == "number") && val > 0;
            }
            return isTooLittle || isTooMuch;
        },

        _isValidSubset: function() {
            // summary:
            //		Overrides `dijit.form.ValidationTextBox._isValidSubset`.
            //		Returns true if the input is syntactically valid, and either within
            //		range or could be made in range by more typing.
            return this.inherited(arguments) && !this._isDefinitelyOutOfRange();
        },

        isValid: function(/*Boolean*/ isFocused) {
            // Overrides dijit.form.ValidationTextBox.isValid to check that the value is also in range.
            return this.inherited(arguments) &&
                   ((this._isEmpty(this.textbox.value) && !this.required) || this.isInRange(isFocused)); // Boolean
        },

        getErrorMessage: function(/*Boolean*/ isFocused) {
            // Overrides dijit.form.ValidationTextBox.getErrorMessage to print "out of range" message if appropriate
            if (dijit.form.RangeBoundTextBox.superclass.isValid.call(this, false) && !this.isInRange(isFocused)) {
                return this.rangeMessage;
            } // String
            return this.inherited(arguments);
        },

        postMixInProperties: function() {
            this.inherited(arguments);
            if (!this.rangeMessage) {
                this.messages = dojo.i18n.getLocalization("dijit.form", "validate", this.lang);
                this.rangeMessage = this.messages.rangeMessage;
            }
        },

        postCreate: function() {
            this.inherited(arguments);
            if (this.constraints.min !== undefined) {
                dijit.setWaiState(this.focusNode, "valuemin", this.constraints.min);
            }
            if (this.constraints.max !== undefined) {
                dijit.setWaiState(this.focusNode, "valuemax", this.constraints.max);
            }
        },

        _setValueAttr: function(/*Number*/ value, /*Boolean?*/ priorityChange) {
            // summary:
            //		Hook so attr('value', ...) works.

            dijit.setWaiState(this.focusNode, "valuenow", value);
            this.inherited(arguments);
        }
    }
            );

}

if (!dojo._hasResource["dojo.data.util.sorter"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.data.util.sorter"] = true;
    dojo.provide("dojo.data.util.sorter");

    dojo.data.util.sorter.basicComparator = function(/*anything*/ a,
        /*anything*/ b) {
        //	summary:
        //		Basic comparision function that compares if an item is greater or less than another item
        //	description:
        //		returns 1 if a > b, -1 if a < b, 0 if equal.
        //		'null' values (null, undefined) are treated as larger values so that they're pushed to the end of the list.
        //		And compared to each other, null is equivalent to undefined.

        //null is a problematic compare, so if null, we set to undefined.
        //Makes the check logic simple, compact, and consistent
        //And (null == undefined) === true, so the check later against null
        //works for undefined and is less bytes.
        var r = -1;
        if (a === null) {
            a = undefined;
        }
        if (b === null) {
            b = undefined;
        }
        if (a == b) {
            r = 0;
        } else if (a > b || a == null) {
            r = 1;
        }
        return r; //int {-1,0,1}
    };

    dojo.data.util.sorter.createSortFunction = function(/* attributes array */sortSpec,
        /*dojo.data.core.Read*/ store) {
        //	summary:
        //		Helper function to generate the sorting function based off the list of sort attributes.
        //	description:
        //		The sort function creation will look for a property on the store called 'comparatorMap'.  If it exists
        //		it will look in the mapping for comparisons function for the attributes.  If one is found, it will
        //		use it instead of the basic comparator, which is typically used for strings, ints, booleans, and dates.
        //		Returns the sorting function for this particular list of attributes and sorting directions.
        //
        //	sortSpec: array
        //		A JS object that array that defines out what attribute names to sort on and whether it should be descenting or asending.
        //		The objects should be formatted as follows:
        //		{
        //			attribute: "attributeName-string" || attribute,
        //			descending: true|false;   // Default is false.
        //		}
        //	store: object
        //		The datastore object to look up item values from.
        //
        var sortFunctions = [];

        function createSortFunction(attr, dir) {
            return function(itemA, itemB) {
                var a = store.getValue(itemA, attr);
                var b = store.getValue(itemB, attr);
                //See if we have a override for an attribute comparison.
                var comparator = null;
                if (store.comparatorMap) {
                    if (typeof attr !== "string") {
                        attr = store.getIdentity(attr);
                    }
                    comparator = store.comparatorMap[attr] || dojo.data.util.sorter.basicComparator;
                }
                comparator = comparator || dojo.data.util.sorter.basicComparator;
                return dir * comparator(a, b); //int
            };
        }

        var sortAttribute;
        for (var i = 0; i < sortSpec.length; i++) {
            sortAttribute = sortSpec[i];
            if (sortAttribute.attribute) {
                var direction = (sortAttribute.descending) ? -1 : 1;
                sortFunctions.push(createSortFunction(sortAttribute.attribute, direction));
            }
        }

        return function(rowA, rowB) {
            var i = 0;
            while (i < sortFunctions.length) {
                var ret = sortFunctions[i++](rowA, rowB);
                if (ret !== 0) {
                    return ret;//int
                }
            }
            return 0; //int
        };  //  Function
    };

}

if (!dojo._hasResource["dojo.data.util.simpleFetch"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.data.util.simpleFetch"] = true;
    dojo.provide("dojo.data.util.simpleFetch");


    dojo.data.util.simpleFetch.fetch = function(/* Object? */ request) {
        //	summary:
        //		The simpleFetch mixin is designed to serve as a set of function(s) that can
        //		be mixed into other datastore implementations to accelerate their development.
        //		The simpleFetch mixin should work well for any datastore that can respond to a _fetchItems()
        //		call by returning an array of all the found items that matched the query.  The simpleFetch mixin
        //		is not designed to work for datastores that respond to a fetch() call by incrementally
        //		loading items, or sequentially loading partial batches of the result
        //		set.  For datastores that mixin simpleFetch, simpleFetch
        //		implements a fetch method that automatically handles eight of the fetch()
        //		arguments -- onBegin, onItem, onComplete, onError, start, count, sort and scope
        //		The class mixing in simpleFetch should not implement fetch(),
        //		but should instead implement a _fetchItems() method.  The _fetchItems()
        //		method takes three arguments, the keywordArgs object that was passed
        //		to fetch(), a callback function to be called when the result array is
        //		available, and an error callback to be called if something goes wrong.
        //		The _fetchItems() method should ignore any keywordArgs parameters for
        //		start, count, onBegin, onItem, onComplete, onError, sort, and scope.
        //		The _fetchItems() method needs to correctly handle any other keywordArgs
        //		parameters, including the query parameter and any optional parameters
        //		(such as includeChildren).  The _fetchItems() method should create an array of
        //		result items and pass it to the fetchHandler along with the original request object
        //		-- or, the _fetchItems() method may, if it wants to, create an new request object
        //		with other specifics about the request that are specific to the datastore and pass
        //		that as the request object to the handler.
        //
        //		For more information on this specific function, see dojo.data.api.Read.fetch()
        request = request || {};
        if (!request.store) {
            request.store = this;
        }
        var self = this;

        var _errorHandler = function(errorData, requestObject) {
            if (requestObject.onError) {
                var scope = requestObject.scope || dojo.global;
                requestObject.onError.call(scope, errorData, requestObject);
            }
        };

        var _fetchHandler = function(items, requestObject) {
            var oldAbortFunction = requestObject.abort || null;
            var aborted = false;

            var startIndex = requestObject.start ? requestObject.start : 0;
            var endIndex = (requestObject.count && (requestObject.count !== Infinity)) ? (startIndex + requestObject.count) : items.length;

            requestObject.abort = function() {
                aborted = true;
                if (oldAbortFunction) {
                    oldAbortFunction.call(requestObject);
                }
            };

            var scope = requestObject.scope || dojo.global;
            if (!requestObject.store) {
                requestObject.store = self;
            }
            if (requestObject.onBegin) {
                requestObject.onBegin.call(scope, items.length, requestObject);
            }
            if (requestObject.sort) {
                items.sort(dojo.data.util.sorter.createSortFunction(requestObject.sort, self));
            }
            if (requestObject.onItem) {
                for (var i = startIndex; (i < items.length) && (i < endIndex); ++i) {
                    var item = items[i];
                    if (!aborted) {
                        requestObject.onItem.call(scope, item, requestObject);
                    }
                }
            }
            if (requestObject.onComplete && !aborted) {
                var subset = null;
                if (!requestObject.onItem) {
                    subset = items.slice(startIndex, endIndex);
                }
                requestObject.onComplete.call(scope, subset, requestObject);
            }
        };
        this._fetchItems(request, _fetchHandler, _errorHandler);
        return request;	// Object
    };

}

if (!dojo._hasResource["dojo.data.util.filter"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dojo.data.util.filter"] = true;
    dojo.provide("dojo.data.util.filter");

    dojo.data.util.filter.patternToRegExp = function(/*String*/pattern, /*boolean?*/ ignoreCase) {
        //	summary:
        //		Helper function to convert a simple pattern to a regular expression for matching.
        //	description:
        //		Returns a regular expression object that conforms to the defined conversion rules.
        //		For example:
        //			ca*   -> /^ca.*$/
        //			*ca*  -> /^.*ca.*$/
        //			*c\*a*  -> /^.*c\*a.*$/
        //			*c\*a?*  -> /^.*c\*a..*$/
        //			and so on.
        //
        //	pattern: string
        //		A simple matching pattern to convert that follows basic rules:
        //			* Means match anything, so ca* means match anything starting with ca
        //			? Means match single character.  So, b?b will match to bob and bab, and so on.
        //      	\ is an escape character.  So for example, \* means do not treat * as a match, but literal character *.
        //				To use a \ as a character in the string, it must be escaped.  So in the pattern it should be
        //				represented by \\ to be treated as an ordinary \ character instead of an escape.
        //
        //	ignoreCase:
        //		An optional flag to indicate if the pattern matching should be treated as case-sensitive or not when comparing
        //		By default, it is assumed case sensitive.

        var rxp = "^";
        var c = null;
        for (var i = 0; i < pattern.length; i++) {
            c = pattern.charAt(i);
            switch (c) {
                case '\\':
                    rxp += c;
                    i++;
                    rxp += pattern.charAt(i);
                    break;
                case '*':
                    rxp += ".*"; break;
                case '?':
                    rxp += "."; break;
                case '$':
                case '^':
                case '/':
                case '+':
                case '.':
                case '|':
                case '(':
                case ')':
                case '{':
                case '}':
                case '[':
                case ']':
                    rxp += "\\"; //fallthrough
                default:
                    rxp += c;
            }
        }
        rxp += "$";
        if (ignoreCase) {
            return new RegExp(rxp, "mi"); //RegExp
        } else {
            return new RegExp(rxp, "m"); //RegExp
        }

    };

}

if (!dojo._hasResource["dijit.form.ComboBox"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.form.ComboBox"] = true;
    dojo.provide("dijit.form.ComboBox");


    dojo.declare(
            "dijit.form.ComboBoxMixin",
            null,
    {
        // summary:
        //		Implements the base functionality for ComboBox/FilteringSelect
        // description:
        //		All widgets that mix in dijit.form.ComboBoxMixin must extend dijit.form._FormValueWidget
        // tags:
        //		protected

        // item: Object
        //		This is the item returned by the dojo.data.store implementation that
        //		provides the data for this cobobox, it's the currently selected item.
        item: null,

        // pageSize: Integer
        //		Argument to data provider.
        //		Specifies number of search results per page (before hitting "next" button)
        pageSize: Infinity,

        // store: Object
        //		Reference to data provider object used by this ComboBox
        store: null,

        // fetchProperties: Object
        //		Mixin to the dojo.data store's fetch.
        //		For example, to set the sort order of the ComboBox menu, pass:
        //		{sort:{attribute:"name",descending: true}}
        fetchProperties:{},

        // query: Object
        //		A query that can be passed to 'store' to initially filter the items,
        //		before doing further filtering based on `searchAttr` and the key.
        //		Any reference to the `searchAttr` is ignored.
        query: {},

        // autoComplete: Boolean
        //		If user types in a partial string, and then tab out of the `<input>` box,
        //		automatically copy the first entry displayed in the drop down list to
        //		the `<input>` field
        autoComplete: true,

        // highlightMatch: String
        // 		One of: "first", "all" or "none".
        //
        //		If the ComboBox/FilteringSelect opens with the search results and the searched
        //		string can be found, it will be highlighted.  If set to "all"
        //		then will probably want to change `queryExpr` parameter to '*${0}*'
        //
        //		Highlighting is only performed when `labelType` is "text", so as to not
        //		interfere with any HTML markup an HTML label might contain.
        highlightMatch: "first",

        // searchDelay: Integer
        //		Delay in milliseconds between when user types something and we start
        //		searching based on that value
        searchDelay: 100,

        // searchAttr: String
        //		Search for items in the data store where this attribute (in the item)
        //		matches what the user typed
        searchAttr: "name",

        // labelAttr: String?
        //		The entries in the drop down list come from this attribute in the
        //		dojo.data items.
        //		If not specified, the searchAttr attribute is used instead.
        labelAttr: "",

        // labelType: String
        //		Specifies how to interpret the labelAttr in the data store items.
        //		Can be "html" or "text".
        labelType: "text",

        // queryExpr: String
        //		This specifies what query ComboBox/FilteringSelect sends to the data store,
        //		based on what the user has typed.  Changing this expression will modify
        //		whether the drop down shows only exact matches, a "starting with" match,
        //		etc.   Use it in conjunction with highlightMatch.
        //		dojo.data query expression pattern.
        //		`${0}` will be substituted for the user text.
        //		`*` is used for wildcards.
        //		`${0}*` means "starts with", `*${0}*` means "contains", `${0}` means "is"
        queryExpr: "${0}*",

        // ignoreCase: Boolean
        //		Set true if the ComboBox/FilteringSelect should ignore case when matching possible items
        ignoreCase: true,

        // hasDownArrow: Boolean
        //		Set this textbox to have a down arrow button, to display the drop down list.
        //		Defaults to true.
        hasDownArrow: true,

        templateString:"<div class=\"dijit dijitReset dijitInlineTable dijitLeft\"\r\n\tid=\"widget_${id}\"\r\n\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\" dojoAttachPoint=\"comboNode\" waiRole=\"combobox\" tabIndex=\"-1\"\r\n\t><div style=\"overflow:hidden;\"\r\n\t\t><div class='dijitReset dijitRight dijitButtonNode dijitArrowButton dijitDownArrowButton'\r\n\t\t\tdojoAttachPoint=\"downArrowNode\" waiRole=\"presentation\"\r\n\t\t\tdojoAttachEvent=\"onmousedown:_onArrowMouseDown,onmouseup:_onMouse,onmouseenter:_onMouse,onmouseleave:_onMouse\"\r\n\t\t\t><div class=\"dijitArrowButtonInner\">&thinsp;</div\r\n\t\t\t><div class=\"dijitArrowButtonChar\">&#9660;</div\r\n\t\t></div\r\n\t\t><div class=\"dijitReset dijitValidationIcon\"><br></div\r\n\t\t><div class=\"dijitReset dijitValidationIconText\">&Chi;</div\r\n\t\t><div class=\"dijitReset dijitInputField\"\r\n\t\t\t><input ${nameAttrSetting} type=\"text\" autocomplete=\"off\" class='dijitReset'\r\n\t\t\tdojoAttachEvent=\"onkeypress:_onKeyPress,compositionend\"\r\n\t\t\tdojoAttachPoint=\"textbox,focusNode\" waiRole=\"textbox\" waiState=\"haspopup-true,autocomplete-list\"\r\n\t\t/></div\r\n\t></div\r\n></div>\r\n",

        baseClass:"dijitComboBox",

        _getCaretPos: function(/*DomNode*/ element) {
            // khtml 3.5.2 has selection* methods as does webkit nightlies from 2005-06-22
            var pos = 0;
            if (typeof(element.selectionStart) == "number") {
                // FIXME: this is totally borked on Moz < 1.3. Any recourse?
                pos = element.selectionStart;
            } else if (dojo.isIE) {
                // in the case of a mouse click in a popup being handled,
                // then the dojo.doc.selection is not the textarea, but the popup
                // var r = dojo.doc.selection.createRange();
                // hack to get IE 6 to play nice. What a POS browser.
                var tr = dojo.doc.selection.createRange().duplicate();
                var ntr = element.createTextRange();
                tr.move("character", 0);
                ntr.move("character", 0);
                try {
                    // If control doesnt have focus, you get an exception.
                    // Seems to happen on reverse-tab, but can also happen on tab (seems to be a race condition - only happens sometimes).
                    // There appears to be no workaround for this - googled for quite a while.
                    ntr.setEndPoint("EndToEnd", tr);
                    pos = String(ntr.text).replace(/\r/g, "").length;
                } catch(e) {
                    // If focus has shifted, 0 is fine for caret pos.
                }
            }
            return pos;
        },

        _setCaretPos: function(/*DomNode*/ element, /*Number*/ location) {
            location = parseInt(location);
            dijit.selectInputText(element, location, location);
        },

        _setDisabledAttr: function(/*Boolean*/ value) {
            // Additional code to set disabled state of combobox node.
            // Overrides _FormValueWidget._setDisabledAttr() or ValidationTextBox._setDisabledAttr().
            this.inherited(arguments);
            dijit.setWaiState(this.comboNode, "disabled", value);
        },

        _onKeyPress: function(/*Event*/ evt) {
            // summary:
            //		Handles keyboard events
            var key = evt.charOrCode;
            //except for cutting/pasting case - ctrl + x/v
            if (evt.altKey || (evt.ctrlKey && (key != 'x' && key != 'v')) || evt.key == dojo.keys.SHIFT) {
                return; // throw out weird key combinations and spurious events
            }
            var doSearch = false;
            var pw = this._popupWidget;
            var dk = dojo.keys;
            var highlighted = null;
            if (this._isShowingNow) {
                pw.handleKey(key);
                highlighted = pw.getHighlightedOption();
            }
            switch (key) {
                case dk.PAGE_DOWN:
                case dk.DOWN_ARROW:
                    if (!this._isShowingNow || this._prev_key_esc) {
                        this._arrowPressed();
                        doSearch = true;
                    } else if (highlighted) {
                        this._announceOption(highlighted);
                    }
                    dojo.stopEvent(evt);
                    this._prev_key_backspace = false;
                    this._prev_key_esc = false;
                    break;

                case dk.PAGE_UP:
                case dk.UP_ARROW:
                    if (this._isShowingNow) {
                        this._announceOption(highlighted);
                    }
                    dojo.stopEvent(evt);
                    this._prev_key_backspace = false;
                    this._prev_key_esc = false;
                    break;

                case dk.ENTER:
                    // prevent submitting form if user presses enter. Also
                    // prevent accepting the value if either Next or Previous
                    // are selected
                    if (highlighted) {
                        // only stop event on prev/next
                        if (highlighted == pw.nextButton) {
                            this._nextSearch(1);
                            dojo.stopEvent(evt);
                            break;
                        } else if (highlighted == pw.previousButton) {
                            this._nextSearch(-1);
                            dojo.stopEvent(evt);
                            break;
                        }
                    } else {
                        // Update 'value' (ex: KY) according to currently displayed text
                        this._setDisplayedValueAttr(this.attr('displayedValue'), true);
                    }
                    // default case:
                    // prevent submit, but allow event to bubble
                    evt.preventDefault();
                // fall through

                case dk.TAB:
                    var newvalue = this.attr('displayedValue');
                    // #4617:
                    //		if the user had More Choices selected fall into the
                    //		_onBlur handler
                    if (pw && (
                            newvalue == pw._messages["previousMessage"] ||
                            newvalue == pw._messages["nextMessage"])
                            ) {
                        break;
                    }
                    if (this._isShowingNow) {
                        this._prev_key_backspace = false;
                        this._prev_key_esc = false;
                        if (highlighted) {
                            pw.attr('value', { target: highlighted });
                        }
                        this._lastQuery = null; // in case results come back later
                        this._hideResultList();
                    }
                    break;

                case ' ':
                    this._prev_key_backspace = false;
                    this._prev_key_esc = false;
                    if (highlighted) {
                        dojo.stopEvent(evt);
                        this._selectOption();
                        this._hideResultList();
                    } else {
                        doSearch = true;
                    }
                    break;

                case dk.ESCAPE:
                    this._prev_key_backspace = false;
                    this._prev_key_esc = true;
                    if (this._isShowingNow) {
                        dojo.stopEvent(evt);
                        this._hideResultList();
                    }
                    break;

                case dk.DELETE:
                case dk.BACKSPACE:
                    this._prev_key_esc = false;
                    this._prev_key_backspace = true;
                    doSearch = true;
                    break;

                case dk.RIGHT_ARROW: // fall through
                case dk.LEFT_ARROW:
                    this._prev_key_backspace = false;
                    this._prev_key_esc = false;
                    break;

                default: // non char keys (F1-F12 etc..)  shouldn't open list
                    this._prev_key_backspace = false;
                    this._prev_key_esc = false;
                    doSearch = typeof key == 'string';
            }
            if (this.searchTimer) {
                clearTimeout(this.searchTimer);
            }
            if (doSearch) {
                // need to wait a tad before start search so that the event
                // bubbles through DOM and we have value visible
                setTimeout(dojo.hitch(this, "_startSearchFromInput"), 1);
            }
        },

        _autoCompleteText: function(/*String*/ text) {
            // summary:
            // 		Fill in the textbox with the first item from the drop down
            // 		list, and highlight the characters that were
            // 		auto-completed. For example, if user typed "CA" and the
            // 		drop down list appeared, the textbox would be changed to
            // 		"California" and "ifornia" would be highlighted.

            var fn = this.focusNode;

            // IE7: clear selection so next highlight works all the time
            dijit.selectInputText(fn, fn.value.length);
            // does text autoComplete the value in the textbox?
            var caseFilter = this.ignoreCase ? 'toLowerCase' : 'substr';
            if (text[caseFilter](0).indexOf(this.focusNode.value[caseFilter](0)) == 0) {
                var cpos = this._getCaretPos(fn);
                // only try to extend if we added the last character at the end of the input
                if ((cpos + 1) > fn.value.length) {
                    // only add to input node as we would overwrite Capitalisation of chars
                    // actually, that is ok
                    fn.value = text;//.substr(cpos);
                    // visually highlight the autocompleted characters
                    dijit.selectInputText(fn, cpos);
                }
            } else {
                // text does not autoComplete; replace the whole value and highlight
                fn.value = text;
                dijit.selectInputText(fn);
            }
        },

        _openResultList: function(/*Object*/ results, /*Object*/ dataObject) {
            if (this.disabled ||
                this.readOnly ||
                (dataObject.query[this.searchAttr] != this._lastQuery)
                    ) {
                return;
            }
            this._popupWidget.clearResultList();
            if (!results.length) {
                this._hideResultList();
                return;
            }

            // Fill in the textbox with the first item from the drop down list,
            // and highlight the characters that were auto-completed. For
            // example, if user typed "CA" and the drop down list appeared, the
            // textbox would be changed to "California" and "ifornia" would be
            // highlighted.

            this.item = null;
            var zerothvalue = new String(this.store.getValue(results[0], this.searchAttr));
            if (zerothvalue && this.autoComplete && !this._prev_key_backspace &&
                (dataObject.query[this.searchAttr] != "*")) {
                // when the user clicks the arrow button to show the full list,
                // startSearch looks for "*".
                // it does not make sense to autocomplete
                // if they are just previewing the options available.
                this.item = results[0];
                this._autoCompleteText(zerothvalue);
            }
            dataObject._maxOptions = this._maxOptions;
            this._popupWidget.createOptions(
                    results,
                    dataObject,
                    dojo.hitch(this, "_getMenuLabelFromItem")
                    );

            // show our list (only if we have content, else nothing)
            this._showResultList();

            // #4091:
            //		tell the screen reader that the paging callback finished by
            //		shouting the next choice
            if (dataObject.direction) {
                if (1 == dataObject.direction) {
                    this._popupWidget.highlightFirstOption();
                } else if (-1 == dataObject.direction) {
                    this._popupWidget.highlightLastOption();
                }
                this._announceOption(this._popupWidget.getHighlightedOption());
            }
        },

        _showResultList: function() {
            this._hideResultList();
            var items = this._popupWidget.getItems(),
                    visibleCount = Math.min(items.length, this.maxListLength);   // TODO: unused, remove
            this._arrowPressed();
            // hide the tooltip
            this.displayMessage("");

            // Position the list and if it's too big to fit on the screen then
            // size it to the maximum possible height
            // Our dear friend IE doesnt take max-height so we need to
            // calculate that on our own every time

            // TODO: want to redo this, see
            //		http://trac.dojotoolkit.org/ticket/3272
            //	and
            //		http://trac.dojotoolkit.org/ticket/4108


            // natural size of the list has changed, so erase old
            // width/height settings, which were hardcoded in a previous
            // call to this function (via dojo.marginBox() call)
            dojo.style(this._popupWidget.domNode, {width: "", height: ""});

            var best = this.open();
            // #3212:
            //		only set auto scroll bars if necessary prevents issues with
            //		scroll bars appearing when they shouldn't when node is made
            //		wider (fractional pixels cause this)
            var popupbox = dojo.marginBox(this._popupWidget.domNode);
            this._popupWidget.domNode.style.overflow =
            ((best.h == popupbox.h) && (best.w == popupbox.w)) ? "hidden" : "auto";
            // #4134:
            //		borrow TextArea scrollbar test so content isn't covered by
            //		scrollbar and horizontal scrollbar doesn't appear
            var newwidth = best.w;
            if (best.h < this._popupWidget.domNode.scrollHeight) {
                newwidth += 16;
            }
            dojo.marginBox(this._popupWidget.domNode, {
                h: best.h,
                w: Math.max(newwidth, this.domNode.offsetWidth)
            });
            dijit.setWaiState(this.comboNode, "expanded", "true");
        },

        _hideResultList: function() {
            if (this._isShowingNow) {
                dijit.popup.close(this._popupWidget);
                this._arrowIdle();
                this._isShowingNow = false;
                dijit.setWaiState(this.comboNode, "expanded", "false");
                dijit.removeWaiState(this.focusNode, "activedescendant");
            }
        },

        _setBlurValue: function() {
            // if the user clicks away from the textbox OR tabs away, set the
            // value to the textbox value
            // #4617:
            //		if value is now more choices or previous choices, revert
            //		the value
            var newvalue = this.attr('displayedValue');
            var pw = this._popupWidget;
            if (pw && (
                    newvalue == pw._messages["previousMessage"] ||
                    newvalue == pw._messages["nextMessage"]
                    )
                    ) {
                this._setValueAttr(this._lastValueReported, true);
            } else {
                // Update 'value' (ex: KY) according to currently displayed text
                this.attr('displayedValue', newvalue);
            }
        },

        _onBlur: function() {
            // summary:
            //		Called magically when focus has shifted away from this widget and it's drop down
            this._hideResultList();
            this._arrowIdle();
            this.inherited(arguments);
        },

        _announceOption: function(/*Node*/ node) {
            // summary:
            //		a11y code that puts the highlighted option in the textbox.
            //		This way screen readers will know what is happening in the
            //		menu.

            if (node == null) {
                return;
            }
            // pull the text value from the item attached to the DOM node
            var newValue;
            if (node == this._popupWidget.nextButton ||
                node == this._popupWidget.previousButton) {
                newValue = node.innerHTML;
            } else {
                newValue = this.store.getValue(node.item, this.searchAttr);
            }
            // get the text that the user manually entered (cut off autocompleted text)
            this.focusNode.value = this.focusNode.value.substring(0, this._getCaretPos(this.focusNode));
            //set up ARIA activedescendant
            dijit.setWaiState(this.focusNode, "activedescendant", dojo.attr(node, "id"));
            // autocomplete the rest of the option to announce change
            this._autoCompleteText(newValue);
        },

        _selectOption: function(/*Event*/ evt) {
            var tgt = null;
            if (!evt) {
                evt = { target: this._popupWidget.getHighlightedOption()};
            }
            // what if nothing is highlighted yet?
            if (!evt.target) {
                // handle autocompletion where the the user has hit ENTER or TAB
                this.attr('displayedValue', this.attr('displayedValue'));
                return;
                // otherwise the user has accepted the autocompleted value
            } else {
                tgt = evt.target;
            }
            if (!evt.noHide) {
                this._hideResultList();
                this._setCaretPos(this.focusNode, this.store.getValue(tgt.item, this.searchAttr).length);
            }
            this._doSelect(tgt);
        },

        _doSelect: function(tgt) {
            // summary:
            //		Menu callback function, called when an item in the menu is selected.
            this.item = tgt.item;
            this.attr('value', this.store.getValue(tgt.item, this.searchAttr));
        },

        _onArrowMouseDown: function(evt) {
            // summary:
            //		Callback when arrow is clicked
            if (this.disabled || this.readOnly) {
                return;
            }
            dojo.stopEvent(evt);
            this.focus();
            if (this._isShowingNow) {
                this._hideResultList();
            } else {
                // forces full population of results, if they click
                // on the arrow it means they want to see more options
                this._startSearch("");
            }
        },

        _startSearchFromInput: function() {
            this._startSearch(this.focusNode.value.replace(/([\\\*\?])/g, "\\$1"));
        },

        _getQueryString: function(/*String*/ text) {
            return dojo.string.substitute(this.queryExpr, [text]);
        },

        _startSearch: function(/*String*/ key) {
            if (!this._popupWidget) {
                var popupId = this.id + "_popup";
                this._popupWidget = new dijit.form._ComboBoxMenu({
                    onChange: dojo.hitch(this, this._selectOption),
                    id: popupId
                });
                dijit.removeWaiState(this.focusNode, "activedescendant");
                dijit.setWaiState(this.textbox, "owns", popupId); // associate popup with textbox
            }
            // create a new query to prevent accidentally querying for a hidden
            // value from FilteringSelect's keyField
            this.item = null; // #4872
            var query = dojo.clone(this.query); // #5970
            this._lastInput = key; // Store exactly what was entered by the user.
            this._lastQuery = query[this.searchAttr] = this._getQueryString(key);
            // #5970: set _lastQuery, *then* start the timeout
            // otherwise, if the user types and the last query returns before the timeout,
            // _lastQuery won't be set and their input gets rewritten
            this.searchTimer = setTimeout(dojo.hitch(this, function(query, _this) {
                var fetch = {
                    queryOptions: {
                        ignoreCase: this.ignoreCase,
                        deep: true
                    },
                    query: query,
                    onBegin: dojo.hitch(this, "_setMaxOptions"),
                    onComplete: dojo.hitch(this, "_openResultList"),
                    onError: function(errText) {
                        console.error('dijit.form.ComboBox: ' + errText);
                        dojo.hitch(_this, "_hideResultList")();
                    },
                    start: 0,
                    count: this.pageSize
                };
                dojo.mixin(fetch, _this.fetchProperties);
                var dataObject = _this.store.fetch(fetch);

                var nextSearch = function(dataObject, direction) {
                    dataObject.start += dataObject.count * direction;
                    // #4091:
                    //		tell callback the direction of the paging so the screen
                    //		reader knows which menu option to shout
                    dataObject.direction = direction;
                    this.store.fetch(dataObject);
                };
                this._nextSearch = this._popupWidget.onPage = dojo.hitch(this, nextSearch, dataObject);
            }, query, this), this.searchDelay);
        },

        _setMaxOptions: function(size, request) {
            this._maxOptions = size;
        },

        _getValueField: function() {
            // summmary:
            //		Helper for postMixInProperties() to set this.value based on data inlined into the markup.
            //		Returns the attribute name in the item (in dijit.form._ComboBoxDataStore) to use as the value.
            return this.searchAttr;
        },

        /////////////// Event handlers /////////////////////

        _arrowPressed: function() {
            if (!this.disabled && !this.readOnly && this.hasDownArrow) {
                dojo.addClass(this.downArrowNode, "dijitArrowButtonActive");
            }
        },

        _arrowIdle: function() {
            if (!this.disabled && !this.readOnly && this.hasDownArrow) {
                dojo.removeClass(this.downArrowNode, "dojoArrowButtonPushed");
            }
        },

        // FIXME: For 2.0, rename to "_compositionEnd"
        compositionend: function(/*Event*/ evt) {
            // summary:
            //		When inputting characters using an input method, such as
            //		Asian languages, it will generate this event instead of
            //		onKeyDown event.
            //		Note: this event is only triggered in FF (not in IE)
            // tags:
            //		private
            this._onKeyPress({charCode:-1});
        },

        //////////// INITIALIZATION METHODS ///////////////////////////////////////

        constructor: function() {
            this.query = {};
            this.fetchProperties = {};
        },

        postMixInProperties: function() {
            if (!this.hasDownArrow) {
                this.baseClass = "dijitTextBox";
            }
            if (!this.store) {
                var srcNodeRef = this.srcNodeRef;

                // if user didn't specify store, then assume there are option tags
                this.store = new dijit.form._ComboBoxDataStore(srcNodeRef);

                // if there is no value set and there is an option list, set
                // the value to the first value to be consistent with native
                // Select

                // Firefox and Safari set value
                // IE6 and Opera set selectedIndex, which is automatically set
                // by the selected attribute of an option tag
                // IE6 does not set value, Opera sets value = selectedIndex
                if (!this.value || (
                        (typeof srcNodeRef.selectedIndex == "number") &&
                        srcNodeRef.selectedIndex.toString() === this.value)
                        ) {
                    var item = this.store.fetchSelectedItem();
                    if (item) {
                        this.value = this.store.getValue(item, this._getValueField());
                    }
                }
            }
            this.inherited(arguments);
        },

        postCreate: function() {
            // summary:
            //		Subclasses must call this method from their postCreate() methods
            // tags: protected

            //find any associated label element and add to combobox node.
            var label = dojo.query('label[for="' + this.id + '"]');
            if (label.length) {
                label[0].id = (this.id + "_label");
                var cn = this.comboNode;
                dijit.setWaiState(cn, "labelledby", label[0].id);

            }
            this.inherited(arguments);
        },

        uninitialize: function() {
            if (this._popupWidget) {
                this._hideResultList();
                this._popupWidget.destroy();
            }
        },

        _getMenuLabelFromItem: function(/*Item*/ item) {
            var label = this.store.getValue(item, this.labelAttr || this.searchAttr);
            var labelType = this.labelType;
            // If labelType is not "text" we don't want to screw any markup ot whatever.
            if (this.highlightMatch != "none" && this.labelType == "text" && this._lastInput) {
                label = this.doHighlight(label, this._escapeHtml(this._lastInput));
                labelType = "html";
            }
            return {html: labelType == "html", label: label};
        },

        doHighlight: function(/*String*/label, /*String*/find) {
            // summary:
            //		Highlights the string entered by the user in the menu.  By default this
            //		highlights the first occurence found. Override this method
            //		to implement your custom highlighing.
            // tags:
            //		protected

            // Add greedy when this.highlightMatch=="all"
            var modifiers = "i" + (this.highlightMatch == "all" ? "g" : "");
            var escapedLabel = this._escapeHtml(label);
            find = dojo.regexp.escapeString(find); // escape regexp special chars
            var ret = escapedLabel.replace(new RegExp("(^|\\s)(" + find + ")", modifiers),
                    '$1<span class="dijitComboBoxHighlightMatch">$2</span>');
            return ret;// returns String, (almost) valid HTML (entities encoded)
        },

        _escapeHtml: function(/*string*/str) {
            // TODO Should become dojo.html.entities(), when exists use instead
            // summary:
            //		Adds escape sequences for special characters in XML: &<>"'
            str = String(str).replace(/&/gm, "&amp;").replace(/</gm, "&lt;")
                    .replace(/>/gm, "&gt;").replace(/"/gm, "&quot;");
            return str; // string
        },

        open: function() {
            // summary:
            //		Opens the drop down menu.  TODO: rename to _open.
            // tags:
            //		private
            this._isShowingNow = true;
            return dijit.popup.open({
                popup: this._popupWidget,
                around: this.domNode,
                parent: this
            });
        },

        reset: function() {
            // Overrides the _FormWidget.reset().
            // Additionally reset the .item (to clean up).
            this.item = null;
            this.inherited(arguments);
        }

    }
            );

    dojo.declare(
            "dijit.form._ComboBoxMenu",
            [dijit._Widget, dijit._Templated],
    {
        // summary:
        //		Focus-less menu for internal use in `dijit.form.ComboBox`
        // tags:
        //		private

        templateString: "<ul class='dijitReset dijitMenu' dojoAttachEvent='onmousedown:_onMouseDown,onmouseup:_onMouseUp,onmouseover:_onMouseOver,onmouseout:_onMouseOut' tabIndex='-1' style='overflow: \"auto\"; overflow-x: \"hidden\";'>"
                + "<li class='dijitMenuItem dijitMenuPreviousButton' dojoAttachPoint='previousButton' waiRole='option'></li>"
                + "<li class='dijitMenuItem dijitMenuNextButton' dojoAttachPoint='nextButton' waiRole='option'></li>"
                + "</ul>",

        // _messages: Object
        //		Holds "next" and "previous" text for paging buttons on drop down
        _messages: null,

        postMixInProperties: function() {
            this._messages = dojo.i18n.getLocalization("dijit.form", "ComboBox", this.lang);
            this.inherited(arguments);
        },

        _setValueAttr: function(/*Object*/ value) {
            this.value = value;
            this.onChange(value);
        },

        // stubs
        onChange: function(/*Object*/ value) {
            // summary:
            //		Notifies ComboBox/FilteringSelect that user clicked an option in the drop down menu.
            //		Probably should be called onSelect.
            // tags:
            //		callback
        },
        onPage: function(/*Number*/ direction) {
            // summary:
            //		Notifies ComboBox/FilteringSelect that user clicked to advance to next/previous page.
            // tags:
            //		callback
        },

        postCreate: function() {
            // fill in template with i18n messages
            this.previousButton.innerHTML = this._messages["previousMessage"];
            this.nextButton.innerHTML = this._messages["nextMessage"];
            this.inherited(arguments);
        },

        onClose: function() {
            // summary:
            //		Callback from dijit.popup code to this widget, notifying it that it closed
            // tags:
            //		private
            this._blurOptionNode();
        },

        _createOption: function(/*Object*/ item, labelFunc) {
            // summary:
            //		Creates an option to appear on the popup menu subclassed by
            //		`dijit.form.FilteringSelect`.

            var labelObject = labelFunc(item);
            var menuitem = dojo.doc.createElement("li");
            dijit.setWaiRole(menuitem, "option");
            if (labelObject.html) {
                menuitem.innerHTML = labelObject.label;
            } else {
                menuitem.appendChild(
                        dojo.doc.createTextNode(labelObject.label)
                        );
            }
            // #3250: in blank options, assign a normal height
            if (menuitem.innerHTML == "") {
                menuitem.innerHTML = "&nbsp;";
            }
            menuitem.item = item;
            return menuitem;
        },

        createOptions: function(results, dataObject, labelFunc) {
            // summary:
            //		Fills in the items in the drop down list
            // results:
            //		Array of dojo.data items
            // dataObject:
            //		dojo.data store
            // labelFunc:
            //		Function to produce a label in the drop down list from a dojo.data item

            //this._dataObject=dataObject;
            //this._dataObject.onComplete=dojo.hitch(comboBox, comboBox._openResultList);
            // display "Previous . . ." button
            this.previousButton.style.display = (dataObject.start == 0) ? "none" : "";
            dojo.attr(this.previousButton, "id", this.id + "_prev");
            // create options using _createOption function defined by parent
            // ComboBox (or FilteringSelect) class
            // #2309:
            //		iterate over cache nondestructively
            dojo.forEach(results, function(item, i) {
                var menuitem = this._createOption(item, labelFunc);
                menuitem.className = "dijitReset dijitMenuItem";
                dojo.attr(menuitem, "id", this.id + i);
                this.domNode.insertBefore(menuitem, this.nextButton);
            }, this);
            // display "Next . . ." button
            var displayMore = false;
            //Try to determine if we should show 'more'...
            if (dataObject._maxOptions && dataObject._maxOptions != -1) {
                if ((dataObject.start + dataObject.count) < dataObject._maxOptions) {
                    displayMore = true;
                } else if ((dataObject.start + dataObject.count) > (dataObject._maxOptions - 1)) {
                    //Weird return from a datastore, where a start + count > maxOptions
                    //implies maxOptions isn't really valid and we have to go into faking it.
                    //And more or less assume more if count == results.length
                    if (dataObject.count == results.length) {
                        displayMore = true;
                    }
                }
            } else if (dataObject.count == results.length) {
                //Don't know the size, so we do the best we can based off count alone.
                //So, if we have an exact match to count, assume more.
                displayMore = true;
            }

            this.nextButton.style.display = displayMore ? "" : "none";
            dojo.attr(this.nextButton, "id", this.id + "_next");
        },

        clearResultList: function() {
            // summary:
            //		Clears the entries in the drop down list, but of course keeps the previous and next buttons.
            while (this.domNode.childNodes.length > 2) {
                this.domNode.removeChild(this.domNode.childNodes[this.domNode.childNodes.length - 2]);
            }
        },

        // these functions are called in showResultList
        getItems: function() {
            // summary:
            //		Called from _showResultList().   Returns DOM Nodes representing the items in the drop down list.
            return this.domNode.childNodes;
        },

        getListLength: function() {
            // summary:
            //		Called from _showResultList().   Returns number of  items in the drop down list,
            //		not including next and previous buttons.
            return this.domNode.childNodes.length - 2;
        },

        _onMouseDown: function(/*Event*/ evt) {
            dojo.stopEvent(evt);
        },

        _onMouseUp: function(/*Event*/ evt) {
            if (evt.target === this.domNode) {
                return;
            } else if (evt.target == this.previousButton) {
                this.onPage(-1);
            } else if (evt.target == this.nextButton) {
                this.onPage(1);
            } else {
                var tgt = evt.target;
                // while the clicked node is inside the div
                while (!tgt.item) {
                    // recurse to the top
                    tgt = tgt.parentNode;
                }
                this._setValueAttr({ target: tgt }, true);
            }
        },

        _onMouseOver: function(/*Event*/ evt) {
            if (evt.target === this.domNode) {
                return;
            }
            var tgt = evt.target;
            if (!(tgt == this.previousButton || tgt == this.nextButton)) {
                // while the clicked node is inside the div
                while (!tgt.item) {
                    // recurse to the top
                    tgt = tgt.parentNode;
                }
            }
            this._focusOptionNode(tgt);
        },

        _onMouseOut: function(/*Event*/ evt) {
            if (evt.target === this.domNode) {
                return;
            }
            this._blurOptionNode();
        },

        _focusOptionNode: function(/*DomNode*/ node) {
            // summary:
            //		Does the actual highlight.
            if (this._highlighted_option != node) {
                this._blurOptionNode();
                this._highlighted_option = node;
                dojo.addClass(this._highlighted_option, "dijitMenuItemSelected");
            }
        },

        _blurOptionNode: function() {
            // summary:
            //		Removes highlight on highlighted option.
            if (this._highlighted_option) {
                dojo.removeClass(this._highlighted_option, "dijitMenuItemSelected");
                this._highlighted_option = null;
            }
        },

        _highlightNextOption: function() {
            //	summary:
            // 		Highlight the item just below the current selection.
            // 		If nothing selected, highlight first option.

            // because each press of a button clears the menu,
            // the highlighted option sometimes becomes detached from the menu!
            // test to see if the option has a parent to see if this is the case.
            var fc = this.domNode.firstChild;
            if (!this.getHighlightedOption()) {
                this._focusOptionNode(fc.style.display == "none" ? fc.nextSibling : fc);
            } else {
                var ns = this._highlighted_option.nextSibling;
                if (ns && ns.style.display != "none") {
                    this._focusOptionNode(ns);
                }
            }
            // scrollIntoView is called outside of _focusOptionNode because in IE putting it inside causes the menu to scroll up on mouseover
            dijit.scrollIntoView(this._highlighted_option);
        },

        highlightFirstOption: function() {
            //	summary:
            // 		Highlight the first real item in the list (not Previous Choices).
            this._focusOptionNode(this.domNode.firstChild.nextSibling);
            dijit.scrollIntoView(this._highlighted_option);
        },

        highlightLastOption: function() {
            //	summary:
            // 		Highlight the last real item in the list (not More Choices).
            this._focusOptionNode(this.domNode.lastChild.previousSibling);
            dijit.scrollIntoView(this._highlighted_option);
        },

        _highlightPrevOption: function() {
            //	summary:
            // 		Highlight the item just above the current selection.
            // 		If nothing selected, highlight last option (if
            // 		you select Previous and try to keep scrolling up the list).
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
        },

        _page: function(/*Boolean*/ up) {
            // summary:
            //		Handles page-up and page-down keypresses

            var scrollamount = 0;
            var oldscroll = this.domNode.scrollTop;
            var height = dojo.style(this.domNode, "height");
            // if no item is highlighted, highlight the first option
            if (!this.getHighlightedOption()) {
                this._highlightNextOption();
            }
            while (scrollamount < height) {
                if (up) {
                    // stop at option 1
                    if (!this.getHighlightedOption().previousSibling ||
                        this._highlighted_option.previousSibling.style.display == "none") {
                        break;
                    }
                    this._highlightPrevOption();
                } else {
                    // stop at last option
                    if (!this.getHighlightedOption().nextSibling ||
                        this._highlighted_option.nextSibling.style.display == "none") {
                        break;
                    }
                    this._highlightNextOption();
                }
                // going backwards
                var newscroll = this.domNode.scrollTop;
                scrollamount += (newscroll - oldscroll) * (up ? -1 : 1);
                oldscroll = newscroll;
            }
        },

        pageUp: function() {
            // summary:
            //		Handles pageup keypress.
            //		TODO: just call _page directly from handleKey().
            // tags:
            //		private
            this._page(true);
        },

        pageDown: function() {
            // summary:
            //		Handles pagedown keypress.
            //		TODO: just call _page directly from handleKey().
            // tags:
            //		private
            this._page(false);
        },

        getHighlightedOption: function() {
            //	summary:
            //		Returns the highlighted option.
            var ho = this._highlighted_option;
            return (ho && ho.parentNode) ? ho : null;
        },

        handleKey: function(key) {
            switch (key) {
                case dojo.keys.DOWN_ARROW:
                    this._highlightNextOption();
                    break;
                case dojo.keys.PAGE_DOWN:
                    this.pageDown();
                    break;
                case dojo.keys.UP_ARROW:
                    this._highlightPrevOption();
                    break;
                case dojo.keys.PAGE_UP:
                    this.pageUp();
                    break;
            }
        }
    }
            );

    dojo.declare(
            "dijit.form.ComboBox",
            [dijit.form.ValidationTextBox, dijit.form.ComboBoxMixin],
    {
        //	summary:
        //		Auto-completing text box, and base class for dijit.form.FilteringSelect.
        //
        //	description:
        //		The drop down box's values are populated from an class called
        //		a data provider, which returns a list of values based on the characters
        //		that the user has typed into the input box.
        //		If OPTION tags are used as the data provider via markup,
        //		then the OPTION tag's child text node is used as the widget value
        //		when selected.  The OPTION tag's value attribute is ignored.
        //		To set the default value when using OPTION tags, specify the selected
        //		attribute on 1 of the child OPTION tags.
        //
        //		Some of the options to the ComboBox are actually arguments to the data
        //		provider.

        _setValueAttr: function(/*String*/ value, /*Boolean?*/ priorityChange) {
            // summary:
            //		Hook so attr('value', value) works.
            // description:
            //		Sets the value of the select.
            if (!value) {
                value = '';
            } // null translates to blank
            dijit.form.ValidationTextBox.prototype._setValueAttr.call(this, value, priorityChange);
        }
    }
            );

    dojo.declare("dijit.form._ComboBoxDataStore", null, {
        //	summary:
        //		Inefficient but small data store specialized for inlined `dijit.form.ComboBox` data
        //
        //	description:
        //		Provides a store for inlined data like:
        //
        //	|	<select>
        //	|		<option value="AL">Alabama</option>
        //	|		...
        //
        //		Actually. just implements the subset of dojo.data.Read/Notification
        //		needed for ComboBox and FilteringSelect to work.
        //
        //		Note that an item is just a pointer to the <option> DomNode.

        constructor: function(/*DomNode*/ root) {
            this.root = root;

            dojo.query("> option", root).forEach(function(node) {
                //	TODO: this was added in #3858 but unclear why/if it's needed;  doesn't seem to be.
                //	If it is needed then can we just hide the select itself instead?
                //node.style.display="none";
                node.innerHTML = dojo.trim(node.innerHTML);
            });

        },

        getValue: function(/* item */ item,
            /* attribute-name-string */ attribute,
            /* value? */ defaultValue) {
            return (attribute == "value") ? item.value : (item.innerText || item.textContent || '');
        },

        isItemLoaded: function(/* anything */ something) {
            return true;
        },

        getFeatures: function() {
            return {"dojo.data.api.Read": true, "dojo.data.api.Identity": true};
        },

        _fetchItems: function(/* Object */ args,
            /* Function */ findCallback,
            /* Function */ errorCallback) {
            //	summary:
            //		See dojo.data.util.simpleFetch.fetch()
            if (!args.query) {
                args.query = {};
            }
            if (!args.query.name) {
                args.query.name = "";
            }
            if (!args.queryOptions) {
                args.queryOptions = {};
            }
            var matcher = dojo.data.util.filter.patternToRegExp(args.query.name, args.queryOptions.ignoreCase),
                    items = dojo.query("> option", this.root).filter(function(option) {
                        return (option.innerText || option.textContent || '').match(matcher);
                    });
            if (args.sort) {
                items.sort(dojo.data.util.sorter.createSortFunction(args.sort, this));
            }
            findCallback(items, args);
        },

        close: function(/*dojo.data.api.Request || args || null */ request) {
            return;
        },

        getLabel: function(/* item */ item) {
            return item.innerHTML;
        },

        getIdentity: function(/* item */ item) {
            return dojo.attr(item, "value");
        },

        fetchItemByIdentity: function(/* Object */ args) {
            //	summary:
            //		Given the identity of an item, this method returns the item that has
            //		that identity through the onItem callback.
            //		Refer to dojo.data.api.Identity.fetchItemByIdentity() for more details.
            //
            //	description:
            //		Given arguments like:
            //
            //	|		{identity: "CA", onItem: function(item){...}
            //
            //		Call `onItem()` with the DOM node `<option value="CA">California</option>`
            var item = dojo.query("option[value='" + args.identity + "']", this.root)[0];
            args.onItem(item);
        },

        fetchSelectedItem: function() {
            //	summary:
            //		Get the option marked as selected, like `<option selected>`.
            //		Not part of dojo.data API.
            var root = this.root,
                    si = root.selectedIndex;
            return dojo.query("> option:nth-child(" +
                              (si != -1 ? si + 1 : 1) + ")",
                    root)[0];	// dojo.data.Item
        }
    });
    //Mix in the simple fetch implementation to this class.
    dojo.extend(dijit.form._ComboBoxDataStore, dojo.data.util.simpleFetch);

}

if (!dojo._hasResource["dijit.form.FilteringSelect"]) { //_hasResource checks added by build. Do not use _hasResource directly in your code.
    dojo._hasResource["dijit.form.FilteringSelect"] = true;
    dojo.provide("dijit.form.FilteringSelect");


    dojo.declare(
            "dijit.form.FilteringSelect",
            [dijit.form.MappedTextBox, dijit.form.ComboBoxMixin],
    {
        // summary:
        //		An enhanced version of the HTML SELECT tag, populated dynamically
        //
        // description:
        //		An enhanced version of the HTML SELECT tag, populated dynamically. It works
        //		very nicely with very large data sets because it can load and page data as needed.
        //		It also resembles ComboBox, but does not allow values outside of the provided ones.
        //		If OPTION tags are used as the data provider via markup, then the
        //		OPTION tag's child text node is used as the displayed value when selected
        //		while the OPTION tag's value attribute is used as the widget value on form submit.
        //		To set the default value when using OPTION tags, specify the selected
        //		attribute on 1 of the child OPTION tags.
        //
        //		Similar features:
        //			- There is a drop down list of possible values.
        //			- You can only enter a value from the drop down list.  (You can't
        //				enter an arbitrary value.)
        //			- The value submitted with the form is the hidden value (ex: CA),
        //				not the displayed value a.k.a. label (ex: California)
        //
        //		Enhancements over plain HTML version:
        //			- If you type in some text then it will filter down the list of
        //				possible values in the drop down list.
        //			- List can be specified either as a static list or via a javascript
        //				function (that can get the list from a server)

        _isvalid: true,

        // required: Boolean
        //		True if user is required to enter a value into this field.
        required: true,

        _lastDisplayedValue: "",

        isValid: function() {
            // Overrides ValidationTextBox.isValid()
            return this._isvalid || (!this.required && this.attr('displayedValue') == ""); // #5974
        },

        _callbackSetLabel: function(/*Array*/ result,
            /*Object*/ dataObject,
            /*Boolean?*/ priorityChange) {
            // summary:
            //		Callback function that dynamically sets the label of the
            //		ComboBox

            // setValue does a synchronous lookup,
            // so it calls _callbackSetLabel directly,
            // and so does not pass dataObject
            // still need to test against _lastQuery in case it came too late
            if ((dataObject && dataObject.query[this.searchAttr] != this._lastQuery) || (!dataObject && result.length && this.store.getIdentity(result[0]) != this._lastQuery)) {
                return;
            }
            if (!result.length) {
                //#3268: do nothing on bad input
                //this._setValue("", "");
                //#3285: change CSS to indicate error
                this.valueNode.value = "";
                dijit.form.TextBox.superclass._setValueAttr.call(this, "", priorityChange || (priorityChange === undefined && !this._focused));
                this._isvalid = false;
                this.validate(this._focused);
                this.item = null;
            } else {
                this._setValueFromItem(result[0], priorityChange);
            }
        },

        _openResultList: function(/*Object*/ results, /*Object*/ dataObject) {
            // Overrides ComboBox._openResultList()

            // #3285: tap into search callback to see if user's query resembles a match
            if (dataObject.query[this.searchAttr] != this._lastQuery) {
                return;
            }
            this._isvalid = results.length != 0; // FIXME: should this be greater-than?
            this.validate(true);
            dijit.form.ComboBoxMixin.prototype._openResultList.apply(this, arguments);
        },

        _getValueAttr: function() {
            // summary:
            //		Hook for attr('value') to work.

            // don't get the textbox value but rather the previously set hidden value
            // TODO: seems suspicious that we need this; how is FilteringSelect different
            // than another MappedTextBox widget?
            return this.valueNode.value;
        },

        _getValueField: function() {
            // Overrides ComboBox._getValueField()
            return "value";
        },

        _setValue: function(/*String*/ value,
            /*String*/ displayedValue,
            /*Boolean?*/ priorityChange) {
            // summary:
            //		Internal function for setting the displayed value and hidden value.
            //		Differs from _setValueAttr() in that _setValueAttr() only takes a single
            //		value argument, and has to look up the displayed value from that.
            // tags:
            //		private
            this.valueNode.value = value;
            dijit.form.FilteringSelect.superclass._setValueAttr.call(this, value, priorityChange, displayedValue);
            this._lastDisplayedValue = displayedValue;
        },

        _setValueAttr: function(/*String*/ value, /*Boolean?*/ priorityChange) {
            // summary:
            //		Hook so attr('value', value) works.
            // description:
            //		Sets the value of the select.
            //		Also sets the label to the corresponding value by reverse lookup.
            if (!this._onChangeActive) {
                priorityChange = null;
            }
            this._lastQuery = value;

            if (value === null || value === '') {
                this._setDisplayedValueAttr('', priorityChange);
                return;
            }

            //#3347: fetchItemByIdentity if no keyAttr specified
            var self = this;
            var handleFetchByIdentity = function(item, priorityChange) {
                if (item) {
                    if (self.store.isItemLoaded(item)) {
                        self._callbackSetLabel([item], undefined, priorityChange);
                    } else {
                        self.store.loadItem({
                            item: item,
                            onItem: function(result, dataObject) {
                                self._callbackSetLabel(result, dataObject, priorityChange);
                            }
                        });
                    }
                } else {
                    self._isvalid = false;
                    // prevent errors from Tooltip not being created yet
                    self.validate(false);
                }
            };
            this.store.fetchItemByIdentity({
                identity: value,
                onItem: function(item) {
                    handleFetchByIdentity(item, priorityChange);
                }
            });
        },

        _setValueFromItem: function(/*item*/ item, /*Boolean?*/ priorityChange) {
            //	summary:
            //		Set the displayed valued in the input box, and the hidden value
            //		that gets submitted, based on a dojo.data store item.
            //	description:
            //		Users shouldn't call this function; they should be calling
            //		attr('displayedValue', value) or attr('value', ...) instead
            // tags:
            //		private
            this._isvalid = true;
            this.item = item; // Fix #6381
            this._setValue(this.store.getIdentity(item),
                    this.labelFunc(item, this.store),
                    priorityChange);
        },

        labelFunc: function(/*item*/ item, /*dojo.data.store*/ store) {
            // summary:
            //		Computes the label to display based on the dojo.data store item.
            // returns:
            //		The label that the ComboBox should display
            // tags:
            //		private
            return store.getValue(item, this.searchAttr);
        },

        _doSelect: function(/*Event*/ tgt) {
            // summary:
            //		Overrides ComboBox._doSelect(), the method called when an item in the menu is selected.
            //	description:
            //		FilteringSelect overrides this to set both the visible and
            //		hidden value from the information stored in the menu.
            this._setValueFromItem(tgt.item, true);
        },

        _setDisplayedValueAttr: function(/*String*/ label, /*Boolean?*/ priorityChange) {
            // summary:
            //		Hook so attr('displayedValue', label) works.
            // description:
            //		Sets textbox to display label. Also performs reverse lookup
            //		to set the hidden value.

            // When this is called during initialization it'll ping the datastore
            // for reverse lookup, and when that completes (after an XHR request)
            // will call setValueAttr()... but that shouldn't trigger an onChange()
            // event, even when it happens after creation has finished
            if (!this._created) {
                priorityChange = false;
            }

            if (this.store) {
                var query = dojo.clone(this.query); // #6196: populate query with user-specifics
                // escape meta characters of dojo.data.util.filter.patternToRegExp().
                this._lastQuery = query[this.searchAttr] = label.replace(/([\\\*\?])/g, "\\$1");
                // if the label is not valid, the callback will never set it,
                // so the last valid value will get the warning textbox set the
                // textbox value now so that the impending warning will make
                // sense to the user
                this.textbox.value = label;
                this._lastDisplayedValue = label;
                var _this = this;
                var fetch = {
                    query: query,
                    queryOptions: {
                        ignoreCase: this.ignoreCase,
                        deep: true
                    },
                    onComplete: function(result, dataObject) {
                        dojo.hitch(_this, "_callbackSetLabel")(result, dataObject, priorityChange);
                    },
                    onError: function(errText) {
                        console.error('dijit.form.FilteringSelect: ' + errText);
                        dojo.hitch(_this, "_setValue")("", label, false);
                    }
                };
                dojo.mixin(fetch, this.fetchProperties);
                this.store.fetch(fetch);
            }
        },

        postMixInProperties: function() {
            this.inherited(arguments);
            this._isvalid = !this.required;
        },

        undo: function() {
            this.attr('displayedValue', this._lastDisplayedValue);
        }
    }
            );

}


dojo.i18n._preloadLocalizations("dojo.nls.artifactory-dojo", ["ROOT","en","en-us","xx"]);
