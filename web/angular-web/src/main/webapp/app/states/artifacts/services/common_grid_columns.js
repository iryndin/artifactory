export function commonGridColumns() {
    let nextId = 0;
    return {
        repoPathColumn: function(specialClass) {
            return '<div ng-if="row.entity.repoKey" class="ui-grid-cell-contents '+specialClass+'">{{row.entity.repoKey}}/{{row.entity.path}}</div>' +
                    '<div ng-if="!row.entity.repoKey" class="ui-grid-cell-contents '+specialClass+'">{{row.entity.path}}</div>';
        },

        downloadableColumn: function(specialClass) {
            return '<div ng-if="row.entity.downloadLink"" class="ui-grid-cell-contents '+specialClass+'">{{row.entity.name}}</div>' +
                    '<div ng-if="!row.entity.downloadLink" class="ui-grid-cell-contents '+specialClass+'">{{row.entity.name}}</div>';
        },

        booleanColumn: function(model) {
            return '<div class="grid-checkbox"><input ng-model="' +
                    model + '" type="checkbox" disabled/><span class="icon icon-v"></span></div>';
        },
        checkboxColumn: function(model, click, disabled) {
            return '<div ng-if="!row.entity._emptyRow" class="grid-cell-checkbox"><jf-checkbox><input ng-model="' + model + '"' +
                    (click && click.length ? ' ng-click="' + click + '"' : '') +
                    (disabled && disabled.length ? ' ng-disabled="' + disabled + '"' : '') +
                    ' type="checkbox"/></jf-checkbox></div>';
        },
        listableColumn: function(listModel,rowNameModel,displayModel,alwaysShow,testIdPrefix=null) {

            testIdPrefix = testIdPrefix ? testIdPrefix + '-' : '';

            displayModel = displayModel ? `{{${listModel}.length}} | {{${displayModel}}}` : `{{${listModel}.length}} | {{${listModel}.join(\', \')}}`;

            let id = `${testIdPrefix}{{row.uid}}_${nextId}`;

            let alwaysShowClass = alwaysShow ? 'always-show' : '';

            let template =  `<div ng-if="${listModel}.length" class="ui-grid-cell-contents no-tooltip ${alwaysShowClass}" id="${id}"><span class="gridcell-content-text">${displayModel}</span><a class="gridcell-showall" ng-if="grid.options.isOverflowing('${testIdPrefix}'+row.uid+'_'+${nextId}) || ${alwaysShow}" href ng-click="grid.options.showAll(${listModel},${rowNameModel},col)"> (See All)</a></div>
                             <div ng-if="!${listModel}.length" class="ui-grid-cell-contents no-tooltip" id="${id}">-</div>`;

            nextId++;
            return template;
        }
    }
}