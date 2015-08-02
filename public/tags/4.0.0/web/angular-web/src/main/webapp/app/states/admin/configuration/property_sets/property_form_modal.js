// modal
let $rootScope;
let ArtifactoryModal;
let $q, Property, PropertySet;
class PropertyFormModal {
    constructor(propertySet, property, isNew) {
        this.scope = $rootScope.$new();
        this.scope.PropertyForm = this;
        this.isNew = isNew;
        this.originalProperty = property;
        this.property = angular.copy(this.originalProperty);
        this.propertySet = propertySet;
        this.propertyTypes = Property.propertyTypes;
    }

    launch() {
        this.deferred = $q.defer();
        this.modalInstance = ArtifactoryModal.launchModal('property_form_modal', this.scope)
        return this.deferred.promise;
    }

    save() {
        angular.copy(this.property, this.originalProperty);
        this.deferred.resolve();
        this.closeModal();
    }

    closeModal() {
        this.deferred.reject();
        this.modalInstance.close();
    }

    isPropertyUnique(propertyName) {
        return propertyName === this.originalProperty.name || !this.propertySet.getPropertyByName(propertyName);
    }

    isPredefinedValuesValid() {
        if (this.property.propertyType === 'ANY_VALUE') return true; // Any Value allows no predefined values
        else return this.property.predefinedValues.length; // Other types must have predefined values
    }

    isDefaultValuesValid(propertyType) {
        if (propertyType === 'MULTI_SELECT') return true;
        return this.property.getDefaultValues().length < 2;
    }

    invalidateType() {
        // By changing the property we use in ui-validate-watch, we force the validation on propertyType to run again
        this.propertyTypeWatch = this.propertyTypeWatch || 0;
        this.propertyTypeWatch++;
    }

    getPredefinedValuesStr() {
        // This is for watching the propertyType value
        return JSON.stringify(this.property.predefinedValues);
    }

    removeValue(value) {
        _.remove(this.property.predefinedValues, value);
        this.invalidateType();
    }

    addValue() {
        this.errorMessage = null;

        if (this._isValueEmpty(this.newValue)) {
            this.errorMessage = "Must input value";
        }
        else if (!this._isValueUnique(this.newValue)) {
            this.errorMessage = "Value already exists";
        }
        else {
            this.property.addPredefinedValue(this.newValue);
            this.newValue = null;
            this.invalidateType();
        }
    }

    _isValueEmpty(text) {
        return _.isEmpty(text);
    }
    _isValueUnique(text) {
        return !this.property.getPredefinedValue(text);
    }
}

export function PropertyFormModalFactory(_$rootScope_, _ArtifactoryModal_, _$q_, _Property_, _PropertySet_) {
    Property = _Property_;
    PropertySet = _PropertySet_;
    $rootScope = _$rootScope_;
    ArtifactoryModal = _ArtifactoryModal_;
    $q = _$q_;
    return PropertyFormModal;
}