const JSTREE_ROW_HOVER_CLASS = 'jstree-hovered';
import KEYS       from '../../../constants/keys.constants';
import ACTIONS from '../../../constants/artifacts_actions.constants';

export default class JFCommonBrowser {
    constructor(ArtifactActions) {
        this.artifactActions = ArtifactActions;
    }

    /****************************
     * Context menu items
     ****************************/

    _getContextMenuItems(obj, cb) {
        let actionItems = {};
        if (obj.data) {
            let node = obj.data;
            node.load()
            .then(() => node.refreshWatchActions())
            .then(() => node.getDownloadPath())
            .then(() => {
                if (node.actions) {
                    node.actions.forEach((actionObj) => {
                        let name = actionObj.name;
                        let action = angular.copy(ACTIONS[name]);
                        if (!action) {
                            console.log("Unrecognized action", name);
                            return true;
                        }
                        action.icon = 'action-icon icon ' + action.icon;
                        action.label = action.title;
                        if (actionObj.name === 'Download') {
                            action.link = node.actualDownloadPath;
                        }
                        else {                        
                            action.action = () => {
                                this.artifactActions.perform(actionObj, obj);
                            }
                        }
                        actionItems[name] = action;
                    });

                    cb(actionItems);
                }
                else {
                    cb([]);
                }
            });
        }
        else {
            cb([]);
        }
    }

    /****************************
     * Access methods
     ****************************/
    jstree() {
        return $(this.treeElement).jstree();
    }

    /****************************
     * Searching the tree
     ****************************/
    _searchTree(text) {
        this.searchText = text || '';
        $(this.treeElement).unhighlight();
        this.jstree().search(this.searchText);
    }

    _getSelectedTreeNode() {
        let selectedJsNode = this.jstree().get_node(this._getSelectedNode());
        return selectedJsNode && selectedJsNode.data;
    }

    _getSelectedNode() {
        return this.jstree().get_selected()[0];
    }


    _onSearch(e, data) {
        if (data.length == 0) {
            return;
        }
        this.searchResults = data.res;
        $(this.treeElement).highlight(this.searchText);
        if (!this.currentResult || !_.include(this.searchResults, this.currentResult)) {
            // there is no previous result, or previous result is not included in the search results
            // select first result that's below the node we started the search from
            let startFromDom = this.jstree().get_node(this._getSelectedNode(), /* as_dom = */ true)[0];
            let firstNodeBelow = _.find(data.nodes, (node) => {
                return node.offsetTop > startFromDom.offsetTop;
            });
            // if found - select as first result, if not - select first search result
            this.currentResult = firstNodeBelow ? firstNodeBelow.id : this.searchResults[0];
        }

        this._gotoCurrentSearchResult();
    }

    _searchTreeKeyDown(key) {
        let jstree = this.jstree();
        if (key == KEYS.DOWN_ARROW) {
            this._selectNextSearchResult();
        }
        else if (key == KEYS.UP_ARROW) {
            this._selectPreviousSearchResult();
        }
        else if (key == KEYS.RIGHT_ARROW) {
            jstree.open_node(jstree.get_selected()[0], () => {
                this._searchTree(this.searchText); // Search again, maybe there are children that match the search
            });
        }
        else if (key == KEYS.LEFT_ARROW) {
            jstree.close_node(jstree.get_selected()[0]);
        }
        else if (key == KEYS.ENTER) {
            this._selectCurrentSearchResult();
            this.jstree().open_node(this.currentResult);
            this._clear_search();
            this._focusOnTree();
            this.currentResult = null;
        }
        else if (key == KEYS.ESC) {
            this._clear_search();
            this._focusOnTree();
            this.currentResult = null;
        }
    }

    _clear_search() {
        this._unhoverAll();
        this.jstree().clear_search();
        $(this.treeElement).unhighlight();
    }

    _selectNextSearchResult() {
        let index = this.searchResults.indexOf(this.currentResult);
        index++;
        if (index > this.searchResults.length - 1) {
            index = 0;
        }
        this.currentResult = this.searchResults[index];
        this._gotoCurrentSearchResult();
    }

    _selectPreviousSearchResult() {
        let index = this.searchResults.indexOf(this.currentResult);
        index--;
        if (index < 0) {
            index = this.searchResults.length - 1;
        }
        this.currentResult = this.searchResults[index];
        this._gotoCurrentSearchResult();
    }

    _gotoCurrentSearchResult() {
        this._unhoverAll();
        if (this.currentResult) {
            let domElement = this._getDomElement(this.currentResult);
            this._hover(domElement);
            this._scrollIntoView(domElement);
        }
    }

    _selectCurrentSearchResult() {
        if (this.currentResult) {
            this.jstree().deselect_all();
            this.jstree().select_node(this.currentResult);
        }
    }

    /****************************
     * access the tree
     ****************************/

    _unhoverAll() {
        $('.' + JSTREE_ROW_HOVER_CLASS).removeClass(JSTREE_ROW_HOVER_CLASS);
    }

    _hover(domElement) {
        domElement.find('.jstree-anchor').first().addClass(JSTREE_ROW_HOVER_CLASS);
    }

    _focusOnTree() {
        // Make sure we can continue navigating the tree with the keys
        this._getSelectedJQueryElement().focus();
    }

    _getSelectedJQueryElement() {
        return $('.jstree #' + this.jstree().get_selected()[0] + '_anchor');
    }

    _getDomElement(node) {
        return this.jstree().get_node(node, true);
    }

    _scrollIntoView(domElement) {

        if (!domElement || !domElement[0]) return;

        if (domElement[0].scrollIntoViewIfNeeded) {
            domElement[0].scrollIntoViewIfNeeded(true);
        }
        else {
            this._scrollToViewIfNeededReplacement(domElement[0],true);
        }
    }

    _scrollToViewIfNeededReplacement(elem,centerIfNeeded) {
        centerIfNeeded = arguments.length <= 1 ? true : !!centerIfNeeded;

        var parent = elem ? elem.offsetParent : null;

        if (!parent) return;

        var     parentComputedStyle = window.getComputedStyle(parent, null),
                parentBorderTopWidth = parseInt(parentComputedStyle.getPropertyValue('border-top-width')),
                parentBorderLeftWidth = parseInt(parentComputedStyle.getPropertyValue('border-left-width')),
                overTop = elem.offsetTop - parent.offsetTop < parent.scrollTop,
                overBottom = (elem.offsetTop - parent.offsetTop + elem.clientHeight - parentBorderTopWidth) > (parent.scrollTop + parent.clientHeight),
                overLeft = elem.offsetLeft - parent.offsetLeft < parent.scrollLeft,
                overRight = (elem.offsetLeft - parent.offsetLeft + elem.clientWidth - parentBorderLeftWidth) > (parent.scrollLeft + parent.clientWidth),
                alignWithTop = overTop && !overBottom;

        if ((overTop || overBottom) && centerIfNeeded) {
            parent.scrollTop = elem.offsetTop - parent.offsetTop - parent.clientHeight / 2 - parentBorderTopWidth + elem.clientHeight / 2;
        }

        if ((overLeft || overRight) && centerIfNeeded) {
            parent.scrollLeft = elem.offsetLeft - parent.offsetLeft - parent.clientWidth / 2 - parentBorderLeftWidth + elem.clientWidth / 2;
        }

        if ((overTop || overBottom || overLeft || overRight) && !centerIfNeeded) {
            elem.scrollIntoView(alignWithTop);
        }
    }

}