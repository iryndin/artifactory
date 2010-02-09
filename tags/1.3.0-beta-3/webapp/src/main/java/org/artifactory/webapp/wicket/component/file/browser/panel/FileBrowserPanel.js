var FileBrowser = function(panelId, inputId) {
    FileBrowser.INSTANCE = this;
    this.input = document.getElementById(inputId);
    this.lastSelection = null;
    this.panelId = panelId;
};

FileBrowser.INSTANCE = null;

FileBrowser.get = function() {
    return FileBrowser.INSTANCE;
}

FileBrowser.prototype.ok = function() {
    if (!this.input.value && this.lastSelection) {
        this.input.value = this.lastSelection.innerHTML;
    }

    var eventScript = this.input.getAttribute('onselection');
    eval(eventScript);
}

FileBrowser.prototype.onFileClick = function(element, e) {
    // set file name
    var fileName = element.innerHTML;
    this.input.value = fileName;

    // mark selected
    if (this.lastSelection) {
        this.lastSelection.className = this.lastSelection.prevClass;
    }

    element.prevClass = element.className;
    element.className += ' selected';
    this.lastSelection = element;
}