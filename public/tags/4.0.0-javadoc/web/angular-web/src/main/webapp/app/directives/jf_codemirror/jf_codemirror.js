import {search}  from './search/search'

export function jfCodeMirror() {
    return {
        restrict: 'E',
        scope: {
            mimeType: '@',
            model: '=',
            allowEdit: '@',
            height: '@?',
            apiAccess: '='
        },
        controller: jfCodeController,
        controllerAs: 'jfCodeMirror',
        bindToController: true,
        template: '<textarea ui-codemirror="jfCodeMirror.editorOptions" ' +
        'ng-model="jfCodeMirror.model"></textarea>'

    }
}

class jfCodeController {
    constructor($scope, $element) {
        this.$element = $element;
        this.$scope = $scope;

        this.editorOptions = {
            lineNumbers: true,
            readOnly: this.allowEdit ? false : 'nocursor',
            lineWrapping: true,
            mode: 'links',
            viewportMargin: 65,
            mimeType: this.mimeType,
            onLoad: this._codeMirrorLoaded.bind(this)
        };
    }

    // register click handlers on code mirror links
    _codeMirrorLoaded(_editor) {
        if (this.height) {
            let codeMirrorElement = $(this.$element).find('.CodeMirror');
            if (this.height === 'flexible') {
                codeMirrorElement.css('top', 0);
                codeMirrorElement.css('bottom', 0);
                codeMirrorElement.css('left', 0);
                codeMirrorElement.css('right', 0);
                codeMirrorElement.css('position', 'absolute');
            }
            else {
                codeMirrorElement.css('height', this.height);
            }
        }
        $(_editor.display.wrapper).on('click', '.cm-link', function (e) {
            let url = $(this).text();
            if (url) {
                window.open(url, '_blank');
            }
        });
        this.$scope.$on('$destroy', () => $(_editor.display.wrapper).off('click'));
        if (this.apiAccess) {
            this.apiAccess.api = _editor;
            if (this.apiAccess.onLoad) {
                this.apiAccess.onLoad();
            }
        }
    }
}