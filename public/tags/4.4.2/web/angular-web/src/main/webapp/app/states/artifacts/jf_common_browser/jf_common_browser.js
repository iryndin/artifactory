import KEYS       from '../../../constants/keys.constants';
import ACTIONS from '../../../constants/artifacts_actions.constants';

const JSTREE_ROW_HOVER_CLASS = 'jstree-hovered';
const REGEXP = /(pkg|repo)\:(.+)/g;

export default class JFCommonBrowser {
    constructor(ArtifactActions) {
        this.artifactActions = ArtifactActions;
        this.activeFilter = false;

        if (this.browserController) {
            this.activeFilter = this.browserController.activeFilter || false;
            this.searchText = this.browserController.searchText || '';
            if (this.searchText.endsWith('*')) this.searchText = this.searchText.substr(0,this.searchText.length-1);
        }

        this._initJSTreeSorting();
    }

    /********************************************
     * Is the node matching the search criteria
     ********************************************/
    _searchCallback(str, jsTreeNode) {

        if (!jsTreeNode.data) return false;
        let treeNode = jsTreeNode.data;

        // Special filters:
        let filterRegexp = new RegExp(REGEXP);
        let matches = filterRegexp.exec(str);
        if (matches) {
            let filterType = matches[1];
            let filterText = matches[2];
            let rootRepo = this._getRootRepo(jsTreeNode).data;

            switch(filterType) {
                case 'pkg':
                    return (treeNode.isRepo() && treeNode.repoPkgType.toLowerCase().indexOf(filterText.toLowerCase()) != -1) || (!treeNode.isRepo() && this.activeFilter && (rootRepo.isRepo() && rootRepo.repoPkgType.toLowerCase().indexOf(filterText.toLowerCase()) != -1));
                case 'repo':
                    return (treeNode.isRepo() && treeNode.repoType.toLowerCase().indexOf(filterText.toLowerCase()) != -1) || (!treeNode.isRepo() && this.activeFilter && (rootRepo.isRepo() && rootRepo.repoType.toLowerCase().indexOf(filterText.toLowerCase()) != -1));
            }
        }
        // Regular text search:
        else {
            if (!this._isVisible(jsTreeNode)) return false;
            return treeNode.text && treeNode.text.indexOf(str) != -1;
        }
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
                        action._class = 'menu-item-' + action.icon;
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
        if (!text) return;
        this.searchText = text || '';
        $(this.treeElement).unhighlight();
        let showOnlyMatches = text.match(new RegExp(REGEXP));
        this.jstree().search(this.searchText, false, showOnlyMatches);
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
                if (!startFromDom) return true;
                return node.offsetTop > startFromDom.offsetTop;
            });
            // if found - select as first result, if not - select first search result
            this.currentResult = firstNodeBelow ? firstNodeBelow.id : this.searchResults[0];
        }

        this._gotoCurrentSearchResult();
    }

    _isInActiveFilterMode() {
        if (this.searchText.match(new RegExp(REGEXP))) {
            let json = this.jstree().get_json();
            let matchesFound = false;
            for (let node of json) {
                node.data.isRepo = () => {
                    return node.data.type === 'repository' ||
                           node.data.type === 'virtualRemoteRepository' ||
                           node.data.type === 'localRepository' ||
                           node.data.type === 'remoteRepository' ||
                           node.data.type === 'cachedRepository' ||
                           node.data.type === 'virtualRepository';
                };
                if (this._searchCallback(this.searchText,node)) {
                    matchesFound = true;
                    break;
                }
            }
            return matchesFound ? true : 'no results';
        }
        else return false;
    }

    _searchTreeKeyDown(key) {
        let jstree = this.jstree();
        if (key == KEYS.DOWN_ARROW) {
            this._selectNextSearchResult();
        }
        else if (key == KEYS.UP_ARROW) {
            this._selectPreviousSearchResult();
        }
        else if (key == KEYS.ENTER) {
            //manually set the model to the input element's value (because the model is debounced...)
            this.searchText = $('.jf-tree-search').val();

            if (this._isInActiveFilterMode() === true) {
                this.activeFilter = true;
                if (this.browserController) {
                    this.browserController.activeFilter = true;
                    this.browserController.searchText = this.searchText + '*';
                }
                this._searchTree(this.searchText);
                this._focusOnTree();
                if (!this._isVisible(jstree.get_node(this._getSelectedNode()))) {
                    jstree.select_node(this._getFirstVisibleNode());
                }
            }
            else if (this._isInActiveFilterMode() === 'no results') {
                if (this.artifactoryNotifications) this.artifactoryNotifications.create({warn: "No repositories matches the filtered " + (this.searchText.startsWith('pkg:') ? 'package' : 'repository') + " type"});
            }
            else {
                this.activeFilter = false;
                if (this.browserController) this.browserController.activeFilter = false;
                this._selectCurrentSearchResult();
                jstree.open_node(this.currentResult);
                this._clear_search();
                this._focusOnTree();
                this.currentResult = null;
            }
        }
        else if (key == KEYS.ESC) {
            this.activeFilter = false;
            if (this.browserController) this.browserController.activeFilter = false;
            this._clear_search();
            this._focusOnTree();
            this.currentResult = null;
        }
    }

    _clear_search() {
        this.activeFilter = false;
        if (this.browserController) this.browserController.activeFilter = false;
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

    _isVisible(jsTreeNode) {
        // If the node is hidden, the get_node as DOM returns empty result
        return this.jstree().get_node(jsTreeNode, true).length && $('#'+this._getSafeId(jsTreeNode.id)).css('display') !== 'none';
    } 

    _isRootRepoVisible(jsTreeNode) {
        return this.jstree().get_node(this._getRootRepo(jsTreeNode), true).length;
    }

    _getFirstVisibleNode() {
        let json = this.jstree().get_json();
        for (let node of json) {
            if (this._isVisible(node)) {
                return node;
            }
        }
    }

    _getRootRepo(jsTreeNode) {
        if (!jsTreeNode.parents || jsTreeNode.parents.length === 1) return jsTreeNode;
        let rootRepoId = jsTreeNode.parents[jsTreeNode.parents.length-2];
        return this.jstree().get_node(rootRepoId);
    }

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
        let nodeID = this._getSafeId(this.jstree().get_selected()[0]);
        return $('.jstree #' + nodeID + '_anchor');
    }

    _getSafeId(id) {
        return this._escapeChars(id,['/','.','$','{','}','(',')','[',']']);
    }

    _escapeChars(str,chars) {
        let newStr = str;
        chars.forEach((char)=>{
            newStr = newStr ? newStr.split(char).join('\\'+char) : newStr;
        });
        return newStr;
    }

    _getDomElement(node) {
        return this.jstree().get_node(node, true);
    }

    _scrollIntoView(domElement) {
$
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


    _initJSTreeSorting() {
        //str.match(/^\d+|\d+\b|\d+(?=\w)/g);
        //str.search(/\d/)
        let jstree;
        $.jstree.defaults.sort = (a,b) => {
            if (!jstree) jstree = this.jstree();
            let aNode = jstree.get_node(a);
            let bNode = jstree.get_node(b);

            if (!aNode || !bNode) {
                jstree = this.jstree();
                aNode = jstree.get_node(a);
                bNode = jstree.get_node(b);
            }

            let aText = aNode.data ? aNode.data.text.toLowerCase() : '*';
            let bText = bNode.data ? bNode.data.text.toLowerCase() : '*';

            let aType = aNode.data ? aNode.data.type : '*';
            let bType = bNode.data ? bNode.data.type : '*';
            let aRepoType = aNode.data ? aNode.data.repoType : '*';
            let bRepoType = bNode.data ? bNode.data.repoType : '*';

            let aScore=0,bScore=0;

            if (aNode.data && aNode.data.isTrashcan && aNode.data.isTrashcan() && aNode.text !== '..') return 1;
            else if (bNode.data && bNode.data.isTrashcan && bNode.data.isTrashcan() && bNode.text !== '..') return -1;
            else if ((aType === 'repository' || aType === 'virtualRemoteRepository') &&
                (bType === 'repository' || bType === 'virtualRemoteRepository')) {
                //both repos - top level sort

                if (aRepoType==='local') aScore+=10000;
                if (bRepoType==='local') bScore+=10000;

                if (aRepoType==='cached') aScore+=1000;
                if (bRepoType==='cached') bScore+=1000;

                if (aRepoType==='remote') aScore+=100;
                if (bRepoType==='remote') bScore+=100;

                if (aRepoType==='virtual') aScore+=10;
                if (bRepoType==='virtual') bScore+=10;

                if (aText<bText) aScore++;
                if (aText>bText) bScore++;

                return aScore<bScore?1:-1;
            }
            else if ((aType !== 'repository' && aType !== 'virtualRemoteRepository') &&
                     (bType !== 'repository' && bType !== 'virtualRemoteRepository')) {
                //both files or folders

                if (aType==='folder') aScore+=10000;
                if (bType==='folder') bScore+=10000;

                if (aNode.text === '..') aScore+=100000;
                if (bNode.text === '..') aScore+=100000;

                let aHasNumVal = !_.isNaN(parseInt(aText));
                let bHasNumVal = !_.isNaN(parseInt(bText));

                if (aHasNumVal) aScore+=1000;
                if (bHasNumVal) bScore+=1000;

                if (aHasNumVal && bHasNumVal) {

                    let addTo = this._compareVersions(aText,bText);

                    if (addTo==='a') aScore += 100;
                    if (addTo==='b') bScore += 100;
                }
                else {

                    let aDigitIndex = aText.search(/\d/);
                    let bDigitIndex = bText.search(/\d/);

                    if (aDigitIndex === bDigitIndex && aDigitIndex !== -1) {
                        let aBeforeNum = aText.substr(0,aDigitIndex);
                        let bBeforeNum = bText.substr(0,bDigitIndex);
                        if (aBeforeNum === bBeforeNum) {
                            let aFromNum = aText.substr(aDigitIndex);
                            let bFromNum = bText.substr(bDigitIndex);

                            let addTo = this._compareVersions(aFromNum,bFromNum);

                            if (addTo==='a') aScore += 100;
                            if (addTo==='b') bScore += 100;

                        }
                    }

                    if (aText<bText) aScore++;
                    if (aText>bText) bScore++;
                }
                return aScore<bScore?1:-1;
            }
            else {
                if (!aNode.data) return -1; //special node
                else if (!bNode.data) return 1; //special node
                else if ((aType === 'repository' || aType === 'virtualRemoteRepository')) return -1;
                else if ((bType === 'repository' || bType === 'virtualRemoteRepository')) return 1;
                else return aText>bText?1:-1;
            }
        }
    }

    _compareVersions(aText,bText) {
        let aArr = aText.split('.');
        let bArr = bText.split('.');
        let minLength = Math.min(aArr.length,bArr.length);

        let addTo;
        for (let i = 0; i<minLength; i++) {
            let aNum = parseInt(aArr[i]);
            let bNum = parseInt(bArr[i]);
            let aIsNum = !_.isNaN(aNum);
            let bIsNum = !_.isNaN(bNum);
            if (aIsNum && bIsNum && aNum<bNum) {
                addTo = 'a';
                break;
            }
            else if (aIsNum && bIsNum && aNum>bNum) {
                addTo = 'b';
                break;
            }
            else if (!aIsNum || !bIsNum) {
                if (aArr[i]<bArr[i]) {
                    addTo = 'a';
                    break;
                }
                else if (aArr[i]>bArr[i]) {
                    addTo = 'b';
                    break;
                }
            }
        }

        if (!addTo) {
            if (aArr.length > bArr.length) addTo = 'b';
            else if (aArr.length < bArr.length) addTo = 'a';
        }

        return addTo;
    }

}