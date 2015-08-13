class jfSwitchController {
    constructor() {
        if (!this.options) throw 'Must supply options';
        this.optionObjects = this.options.map((option) => {
            if (typeof(option) === 'string') {
                return {value: option, text: option};
            }
            else {
                return option;
            }
        });
        if (_.isEmpty(this.ngModel)) this.ngModel = this.optionObjects[0].value;
    }
    getOptionObjects() {

    }
    selectOption(option) {
        this.ngModelCtrl.$setViewValue(option.value);
    }
    isSelected(option) {
        return this.ngModel === option.value;
    }
}

export function jfSwitch() {
    return {
        restrict: 'E',
        require: 'ngModel',
        scope: {
            jfSwitchTitle: '@',
            options: '=',
            ngModel: '='
        },
        link: ($scope, attrs, $element, ngModelCtrl) => {
            $scope.jfSwitch.ngModelCtrl = ngModelCtrl;
        },
        controller: jfSwitchController,
        controllerAs: 'jfSwitch',
        bindToController: true,
        templateUrl: 'directives/jf_switch/jf_switch.html'
    }
}