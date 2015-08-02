class jfUiSelectController {
    constructor() {
        this.displayLabel = (item) => {
            if (!item) return null;
            if (item[this.jfSelectDisabled]) return null;
            if (this.jfSelectDisplayAttr) {
                return item[this.jfSelectDisplayAttr];
            } else if (this.jfSelectDisplayFunc) {
                return this.jfSelectDisplayFunc({$item: item});
            } else {
                return item;
            }
        };
    }
}

export function jfUiSelect() {
    return {
        controller: jfUiSelectController,
        controllerAs: 'jfUiSelect',
        bindToController: true,
        scope: {
            jfSelectModel: '=',
            jfSelectOptions: '=',
            jfSelectDisabled: '=',
            jfSelectChange: '&',
            jfSelectDisplayAttr: '@',
            jfSelectDisplayFunc: '&?',
            jfSelectPlaceholder: '@'
        },
        templateUrl: 'directives/jf_ui_select/jf_ui_select.html'
    }
}