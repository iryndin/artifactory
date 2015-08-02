'use strict';
function JsTreeObject(jsTreeElement) {
  this.jstree = jsTreeElement.jstree();
  this.getNode = function(node) {
    return this.jstree.get_node(node);
  };
  
  this.expandFirstItem = function() {
    $('.jstree-node:first-child .jstree-ocl').click();
  };

  this.loadNodeItem = function(text) {
    this.getNodeWithText(text).click();
  };

  this.getNodeWithText = function(text) {
    return _.find($('.jstree-anchor'), function(el) {
      return $(el).text() === text;
    });
  };
  this.getRootItem = function() {
    return this.jstree.get_node('#');
  }
}
module.exports = JsTreeObject;