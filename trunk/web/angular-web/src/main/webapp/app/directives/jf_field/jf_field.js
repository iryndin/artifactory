import EVENTS     from '../../constants/common_events.constants';

export function jfField($timeout, ArtifactoryEventBus, $rootScope) {
    return {
        restrict: 'E',
        require: '^form',
        scope: {
            animated: '@',
            validations: '@',
            autofocus: '=',
            invalidateOnSubmit: '@',
            alwaysShowErrors: '@',
            dontValidateDisabled: '@'
        },
        transclude: true,
        templateUrl: 'directives/jf_field/jf_field.html',
        link: function jfInputTextLink(scope, element, attrs, form) {

            let inputElement = $(element.find('input')[0]);
            if (!inputElement.length) {
                inputElement = $(element.find('textarea')[0]);
            }
            let inputName = inputElement.attr('name');

            scope.formField = form[inputName];
            if (scope.formField) scope.formField.initialValue = true;
            scope.form = form;
            scope.$watch(()=>scope.autofocus, ()=>focusInput());
            ArtifactoryEventBus.registerOnScope(scope, EVENTS.FORM_SUBMITTED, _onFormSubmitted);
            ArtifactoryEventBus.registerOnScope(scope, EVENTS.FORM_CLEAR_FIELD_VALIDATION, force => {
                _onBlur(force);
                if (scope.formField) scope.formField.preventShowErrors = true;
            });

            scope.$on('$destroy', () => {
                inputElement.off('blur');
                inputElement.off('keyup');
                inputElement.off('keydown');
                inputElement.off('focus');
            });

            inputElement.on('keydown', () => _onKeyDown());

            if (!scope.invalidateOnSubmit && scope.validations) {
                inputElement.on('blur', () => _onBlur());
                inputElement.on('keyup', () => _onKeyUp());
            }
            if (scope.invalidateOnSubmit || scope.validations) {
                inputElement.on('focus', () => _onFocus());
            }

            if (scope.dontValidateDisabled) {
                $rootScope.$watch(() => inputElement[0].disabled, () => _onDisabledChanged(inputElement[0].disabled));
            }
            focusInput();

            function focusInput() {
                if (scope.autofocus && scope.autofocus != 'false') {
                    $timeout(() => {
                        inputElement.focus();
                    });
                }
            }
            function _onDisabledChanged(disabled) {
                if (disabled) {
                    scope.formField.showErrors = false;
                }
                else {
                    scope.formField.showErrors = true;
                }
            }

            function _onFormSubmitted(formName) {
                if (!formName || (scope.form.$name === formName && !scope.formField.$valid)) {
                    inputElement.addClass('invalid');
                    scope.formField.showErrors = true;
                } else {
                    inputElement.removeClass('invalid');
                    scope.formField.showErrors = false;
                }
            }

            function _onBlur(force) {
                $timeout(function () {
                    if (scope.formField) {
                        if (force) {
                            scope.formField.showErrors = false;
                        } else if (!scope.formField.preventShowErrors){
                            scope.formField.showErrors = true;
                        }
                    }
                });

                if (force || (scope.formField && scope.formField.$valid) || (scope.formField && scope.formField.preventShowErrors)) {
                    inputElement.removeClass('invalid');
                } else {
                    inputElement.addClass('invalid');
                }
            }

            function _onFocus() {
                $timeout(function () {
                    if (scope.formField) {
                        scope.formField.showErrors = false;
                        scope.formField.preventShowErrors = false;
                        inputElement.removeClass('invalid');
                    }

                    scope.form.$setPristine();
                });
            }
            function _onKeyDown() {
                scope.formField.initialValue = false;
            }

            function _onKeyUp() {
                let value = inputElement.val();
                if (value !== null && value !== undefined && value !== '') {
                    inputElement.addClass('hascontent');
                } else {
                    inputElement.removeClass('hascontent');
                }
            }
        }
    }
}