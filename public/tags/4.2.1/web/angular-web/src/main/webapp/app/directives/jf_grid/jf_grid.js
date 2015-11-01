export function jfGrid($timeout,$compile) {


    let isNoTooltip = (cell) => {
        return (cell.context.classList.contains('no-tooltip'));
    };

    return {
        scope: {
            gridOptions: '=',
            filterField: '@?',
            filterField2: '@?',
            filterOnChange: '@?',
            autoFocus: '@',
            objectName: '@'
        },
        templateUrl: 'directives/jf_grid/jf_grid.html',
        link: ($scope, $element, $attrs) => {

            $scope.gridOptions.gridObjectName = $scope.objectName;

            $scope.noCount = $attrs.hasOwnProperty('noCount');
            $scope.noPagination = $attrs.hasOwnProperty('noPagination');

            $($element).on('mouseenter', '.ui-grid-cell, .ui-grid-cell-contents, .btn-action', (e)=>{
                let cellItem = $(e.target);

                cellItem.parents('.ui-grid-row').addClass('hovered');
                $scope.$apply();

                if (cellItem.hasClass('ui-grid-cell-contents')) {
                    let cellItemContent = cellItem.text().trim();

                    if (cellItemContent.length > 0 && cellItem[0].scrollWidth > cellItem.innerWidth()) {
                        if (!cellItem.hasClass('tooltipstered') && !isNoTooltip(cellItem)) {
                            cellItem.tooltipster({
                                trigger: 'hover',
                                onlyOne: 'true',
                                interactive: 'true',
                                position: 'bottom',
                                content: cellItemContent
                            });
                            cellItem.tooltipster('show');
                        }
                        else if (!isNoTooltip(cellItem)) {
                            cellItem.tooltipster('enable');

                            if (cellItem.tooltipster('content') != cellItemContent)
                                cellItem.tooltipster('content', cellItemContent);
                        }
                    }
                    else if (cellItem.hasClass('tooltipstered'))
                        cellItem.tooltipster('disable');
                }
            }).on('mouseleave', '.ui-grid-draggable-row, .ui-grid-cell, .ui-grid-cell-contents, .btn-action', (e)=>{
                let currentRowElement = $(e.currentTarget).parents('.ui-grid-row'),
                        toRowElement = $(e.relatedTarget).parents('.ui-grid-row');

                if (!toRowElement || !currentRowElement.is(toRowElement)) {
                    currentRowElement.removeClass('hovered');
                    $scope.$apply();
                }
            });
            $scope.$on('$destroy', () => {
                $($element).off('mouseenter');
                $($element).off('mouseleave');
            });


            $scope.getTotalRecords = () => {
                let count;

                if (!$scope.gridOptions.api) return 0;

                let visRows = $scope.gridOptions.api.grid.getVisibleRows();
                let totalRows = $scope.gridOptions.api.grid.rows.length;
                if (_.findWhere(visRows,{entity:{_emptyRow:true}}))
                    count = totalRows - 1;
                else
                    count = totalRows;

                let recordsName;

                if ($scope.objectName) {
                    if ($scope.objectName.indexOf('/')>=0) {
                        let splited = $scope.objectName.split('/');
                        recordsName = count !== 1 ? splited[1] : splited[0];
                    }
                    else
                        recordsName = count !== 1 ? $scope.objectName + 's' : $scope.objectName;
                }
                else
                    recordsName = count !== 1 ? 'records' : 'record';

                return count + ' ' + _.startCase(recordsName);
            };
        }
    }
}
