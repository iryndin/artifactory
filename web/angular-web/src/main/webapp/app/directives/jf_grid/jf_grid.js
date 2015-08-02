export function jfGrid($timeout,$compile) {

    let isListTooltip = (cell) => {
        let parent = cell.context.parentElement;
        return (parent.classList.contains('tooltip-show-list'));
    };

    let formatListContent = (content)=>{
        let pipeIndex = content.indexOf('|');
        if (pipeIndex>=0) {
            let listContent = content.substr(pipeIndex+1);
            let list = listContent.split(',');
            let cleanList = _.map(list,(line)=>{
                return line.trim();
            });
            return cleanList.join('\n');
        }
        else return content;
    };

    return {
        scope: {
            gridOptions: '=',
            filterField: '@?',
            filterField2: '@?',
            filterOnChange: '@?',
            noPagination: '@'
        },
        templateUrl: 'directives/jf_grid/jf_grid.html',
        link: ($scope, $element) => {
            $($element).on('mouseenter','.ui-grid-cell-contents',(e)=>{
                let cellItem = $(e.target);

                if (cellItem.hasClass('ui-grid-cell-contents')) {
                    let cellItemContent = cellItem.text().trim();

                    if (cellItemContent.length > 0 && cellItem[0].scrollWidth > cellItem.innerWidth()) {
                        if (!cellItem.hasClass('tooltipstered')) {
                            cellItem.tooltipster({
                                trigger: 'hover',
                                onlyOne: 'true',
                                interactive: 'true',
                                position: 'bottom',
                                content: isListTooltip(cellItem) ? formatListContent(cellItemContent) : cellItemContent
                            });
                            cellItem.tooltipster('show');
                        }
                        else
                            cellItem.tooltipster('enable');
                    }
                    else if (cellItem.hasClass('tooltipstered'))
                        cellItem.tooltipster('disable');
                }
            });
            $scope.$on('$destroy', () => {
                $($element).off('mouseenter');
                $($element).off('mouseleave');
            });


        }
    }
}
