import {ArtifactoryGridFactory} from './artifactory_grid';
import {ARTIFACTORY_GRID} from './artifactory_grid_constants'

export default angular.module('artifactory_grid', [
		'ui.grid',
		'ui.grid.autoResize',
	    'ui.grid.edit',
	    'ui.grid.selection',
	    'ui.grid.pagination',
	    'ui.grid.grouping'
	])
    .service('ArtifactoryGridFactory', ArtifactoryGridFactory)
    .constant('ARTIFACTORY_GRID', ARTIFACTORY_GRID);