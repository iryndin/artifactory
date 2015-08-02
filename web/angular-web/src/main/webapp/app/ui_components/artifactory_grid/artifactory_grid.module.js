import {ArtifactoryGridFactory} from './artifactory_grid';
import {ARTIFACTORY_GRID} from './artifactory_grid_constants'

export default angular.module('artifactory_grid', ['ui.grid'])
    .service('ArtifactoryGridFactory', ArtifactoryGridFactory)
    .constant('ARTIFACTORY_GRID', ARTIFACTORY_GRID);