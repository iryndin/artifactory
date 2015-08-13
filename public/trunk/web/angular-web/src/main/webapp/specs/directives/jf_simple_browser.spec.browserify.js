'use strict';
import StateParamsMock from '../../mocks/state_params_mock.browserify';
import UserMock from '../../mocks/user_mock.browserify';
import TreeNodeMock from '../../mocks/tree_node_mock.browserify';
import JsTreeObject from '../page_objects/js_tree_object.browserify';
import mockStorage from '../../mocks/artifactory_storage_mock.browserify';
describe('unit test:jf_simple_browser directive', () => {
  let simpleBrowserElement,
    $scope,
    httpBackend,
    RESOURCE,
    TreeBrowserDao,
    repo1,
    repo2,
    child,
    jsTreeObject,
    stateParams = {},
    ArtifactoryEventBus,
    artifactoryState;

  mockStorage();

  function setup(_TreeBrowserDao_, TreeNode, $q, $httpBackend, _RESOURCE_, _ArtifactoryEventBus_, ArtifactoryState) {
      httpBackend = $httpBackend;
      RESOURCE = _RESOURCE_;
      TreeBrowserDao = _TreeBrowserDao_;
      repo1 = new TreeNode(TreeNodeMock.repo('repo1'));
      repo2 = new TreeNode(TreeNodeMock.repo('repo2'));
      child = new TreeNode(TreeNodeMock.file({text: 'file'}));
      ArtifactoryEventBus = _ArtifactoryEventBus_;
      artifactoryState = ArtifactoryState;
      spyOn(ArtifactoryEventBus, 'dispatch').and.callThrough();
      UserMock.mockCurrentUser();
  }

  function compileDirective() {
    $scope = compileHtml('<jf-simple-browser></jf-simple-browser>');
    flush();
    simpleBrowserElement = angular.element(document.body).find('jf-simple-browser')[0];
    jsTreeObject = new JsTreeObject();
  }

  function twoDotsItem() {
    return jsTreeObject.getNodeWithText(/\.\./);
  }
  function repo1Item() {
    return jsTreeObject.getNodeWithText('repo1');
  }
  function repo2Item() {
    return jsTreeObject.getNodeWithText('repo2');
  }
  function fileItem() {
    return jsTreeObject.getNodeWithText('file');
  }

  function flush() {
      httpBackend.flush();
  }

  function drillDownRepo1() {
    repo1.expectGetChildren([child]);
    repo1Item().click();
    flush();
  }

  beforeEach(m('artifactory.templates', 'artifactory.states'));
  beforeEach(() => {
    StateParamsMock(stateParams);
  });

  beforeEach(inject(setup));

  beforeEach(() => {
    TreeNodeMock.expectGetRoots();
  });

  describe('no artifact state', () => {
    beforeEach(compileDirective);

    it('should show tree', () => {
      expect(simpleBrowserElement).toBeDefined();
      expect(repo1Item()).toBeDefined();
      expect(repo2Item()).toBeDefined();
      expect(fileItem()).not.toBeDefined();
      expect(twoDotsItem()).not.toBeDefined();
    });

    it('should allow to drill down to a repo', (done) => {
      drillDownRepo1();
      expect(repo1Item()).toBeDefined();
      expect(fileItem()).toBeDefined();
      expect(repo2Item()).not.toBeDefined();
      TreeBrowserDao.getRoots()
        .then((roots) => {
          expect(ArtifactoryEventBus.dispatch).toHaveBeenCalledWith('tree:node:select', {data: roots[0]});
          done();
        });
      $scope.$digest();
    });

    it('should not drill down to a file', (done) => {
      drillDownRepo1();
      child.expectLoad(TreeNodeMock.data());
      fileItem().click();
      flush();
      expect(repo1Item()).toBeDefined();
      expect(fileItem()).toBeDefined();
      TreeBrowserDao.getRoots()
        .then((roots) => {
          return roots[0].getChildren();
        })
        .then((children) => {
          expect(ArtifactoryEventBus.dispatch).toHaveBeenCalledWith('tree:node:select', {data: children[0]});
          done();
        });
      $scope.$digest();
    });

    it('should allow to go up', () => {
      drillDownRepo1();
      twoDotsItem().click();
      $scope.$digest();
      expect(repo1Item()).toBeDefined();
      expect(repo2Item()).toBeDefined();
    });
  });
  describe('with artifact state, tree untouched', () => {
    beforeEach(() => {
      stateParams.artifact = 'repo1/file';
      repo1.expectGetChildren([child]);
    });
    beforeEach(compileDirective);
    it('should activate repo1', (done) => {
      expect(simpleBrowserElement).toBeDefined();
      expect(repo1Item()).toBeDefined();
      expect(repo2Item()).toBeDefined();
      expect(fileItem()).not.toBeDefined();
      expect(twoDotsItem()).not.toBeDefined();
      TreeBrowserDao.getRoots()
        .then((roots) => {
          expect(ArtifactoryEventBus.dispatch).toHaveBeenCalledWith('tree:node:select', {data: roots[0]});
          done();
        });
      $scope.$digest();
    });
  });
  describe('with artifact state, tree touched', () => {
    beforeEach(() => {
      artifactoryState.setState('tree_touched', true);
      stateParams.artifact = 'repo1/file';
      repo1.expectGetChildren([child]);
    });
    beforeEach(compileDirective);
    it('should activate repo1 & drill down into it', (done) => {
      expect(simpleBrowserElement).toBeDefined();
      expect(repo1Item()).toBeDefined();
      expect(fileItem()).toBeDefined();
      expect(repo2Item()).not.toBeDefined();
      expect(twoDotsItem()).toBeDefined();
      TreeBrowserDao.getRoots()
        .then((roots) => {
          expect(ArtifactoryEventBus.dispatch).toHaveBeenCalledWith('tree:node:select', {data: roots[0]});
          done();
        });
      $scope.$digest();
    });
  });
});
