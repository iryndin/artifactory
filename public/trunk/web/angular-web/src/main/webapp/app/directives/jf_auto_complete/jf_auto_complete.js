class jfAutoCompleteController {

    constructor($scope, $element, $attrs, $timeout) {

        this.$element = $element;


        $($element).autocomplete({
            source: $scope.uiItems,
            select: (event,ui) => {
                if ($scope.onSelect) {
                    $scope.onSelect({selection:ui.item.value});
                }
            }
        });
        $($element).autocomplete('widget').css('z-index','9999999999');
        $($element).autocomplete('widget').css('max-height','400px');
        $($element).autocomplete('widget').css('overflow-y','auto');
        $($element).autocomplete('widget').css('overflow-x','hidden');


        $scope.$watch('uiItems',(newVal,oldVal)=>{
           this.updateSource(newVal);
        });

    }

    updateSource(list) {
        $(this.$element).autocomplete('option','source',list);
    }

}

export function jfAutoComplete() {

    return {
        restrict: 'A',
        scope: {
            uiItems: '=',
            onSelect: '&'
        },
        controller: jfAutoCompleteController
    };
}
