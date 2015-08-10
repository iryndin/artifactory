import Grid from './artifactory_grid/artifactory_grid.module';
import Modal from './artifactory_modal/artifactory_modal.module';

angular.module('artifactory.ui_components', [
    Grid.name,
    Modal.name
]);