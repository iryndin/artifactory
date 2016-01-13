import EVENTS   from '../../../constants/artifacts_events.constants';
import TOOLTIP  from '../../../constants/artifact_tooltip.constant';

class jfPackageController {
    constructor($state, $stateParams, ArtifactPackageSearchDao, $timeout, $q, ArtifactoryFeatures) {
        this.queryFields = [];

        this.$q = $q;
        this.$state = $state;
        this.$stateParams = $stateParams;
        this.$timeout = $timeout;
        this.artifactPackageSearchDao = ArtifactPackageSearchDao;

        this.isOss = ArtifactoryFeatures.isOss();

        this.init();
    }

    init() {

        if (this.query.selectedPackageType) {
            this.selectedPackageType = this.query.selectedPackageType;

            this.onPackageTypeChange().then(()=>{
                if (this.query.query) {
                    if (this.query.query.search === 'gavc') {
                        for (let key in this.rawQuery) {
                            this.rawQuery[key].values = this.query.query[key];
                        }
                    }
                    else {
                        this.query.query.forEach((queryItem)=>{
                            this.rawQuery[queryItem.id].values = queryItem.values.join(',');
                        })
                    }
                }
            });
        }

        this.artifactPackageSearchDao.availablePackages().$promise.then((data)=> {
            data = _.filter(data, (packageType)=> {
                return !packageType.id.startsWith('docker') && (!this.isOss || packageType.id === "gavc");
            });
            if (!this.isOss) {
                data.unshift({
                    id: 'dockerV2',
                    icon: 'docker',
                    displayName: 'Docker'
                });
            }
            this.availablePackageTypes = data;
        })
    }

    onPackageTypeChange() {

        var defer = this.$q.defer();

        this.rawQuery = {};
        this.$timeout(()=>{
            this.parentController.filterReposLimitByPackageType(this.selectedPackageType.id);
            if (this.selectedPackageType.id === 'gavc') {
                let gavcFields = [
                    {id: 'groupID', displayName: 'Group ID', allowedComparators: ''},
                    {id: 'artifactID', displayName: 'Artifact ID', allowedComparators: ''},
                    {id: 'version', displayName: 'Version', allowedComparators: ''},
                    {id: 'classifier', displayName: 'Classifier', allowedComparators: ''}
                ];
                this.queryFields = gavcFields;
                this.queryFields.forEach((field)=>{
                    this.rawQuery[field.id] = {comparator: field.allowedComparators[0]};
                });
                defer.resolve();
            }
            else {
                this.artifactPackageSearchDao.queryFields({},{packageType:this.selectedPackageType.id}).$promise.then((data)=>{
                    if (this.selectedPackageType.id === 'nuget') {
                        data = _.filter(data,(field)=>{
                            return field.id !== 'nugetTags' && field.id !== 'nugetDigest';
                        })
                    }
                    this.queryFields = data;
                    this.queryFields.forEach((field)=>{
                        this.rawQuery[field.id] = {comparator: field.allowedComparators[0]};
                    });
                    defer.resolve();
                });
            }
        });

        return defer.promise;
    }

    _transformQuery(rawQuery) {
        let transformed;
        if (this.selectedPackageType.id === 'gavc') {
            transformed = {};
            transformed.search = 'gavc';
            for (let key in rawQuery) {
                if (rawQuery[key].values) {
                    transformed[key] = rawQuery[key].values || '';
                }
            }
            transformed.selectedRepositories = this.query.selectedRepositories;
        }
        else {
            transformed = [];
            for (let key in rawQuery) {
                if (rawQuery[key].values) {
                    if (key !== 'repo') transformed.push({
                        id: key,
                        /*
                         comparator: rawQuery[key].comparator,
                         */
                        values: rawQuery[key].values.split(',')
                    })
                }
            }
            transformed.push({
                id: 'repo',
                values: _.pluck(this.query.selectedRepositories, "repoKey")
            })
        }



        return transformed;
    }

    canSearch() {
        let ret = false;
        if (this.rawQuery) {
            for (let key in this.rawQuery) {
                if (this.rawQuery[key].values) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    search() {

        let transformedQuery = this._transformQuery(this.rawQuery);
        let completeQuery = {
            query: transformedQuery,
            selectedPackageType: this.selectedPackageType,
            selectedRepositories: this.query.selectedRepositories,
            columns: this.getColumnsByPackage()
        }
        this.$state.go('.', {
            'searchType': 'package',
            'searchParams': {
                selectedRepos: this.query.selectedRepositories
            },
            'params': btoa(JSON.stringify(completeQuery))
        });
    }
    clear() {
        for (let key in this.rawQuery) {
            let field = this.rawQuery[key];
            if (field.values) delete field.values;
        }
    }
    getColumnsByPackage() {

        switch(this.selectedPackageType.id) {
            case 'gavc':
                return ['artifact','groupID','artifactID','version','classifier','repo','path','modified'];
                break;
            case 'dockerV1':
                return ['dockerV1Image*Image@','dockerV1Tag*Tag@','repo','modified'];
                break;
            case 'dockerV2':
                return ['dockerV2Image*Image@','dockerV2Tag*Tag@','repo','modified'];
                break;
            case 'nuget':
                return ['nugetPackageId*Package ID','nugetVersion*Version@','repo','path','modified'];
                break;
            case 'npm':
                return ['npmName*Package Name','npmVersion*Version@','npmScope*Scope@','repo','path','modified'];
                break;
            case 'bower':
                return ['bowerName*Package Name','bowerVersion*Version@','repo','path','modified'];
                break;
            case 'debian':
                return ['artifact','repo','path','debianDistribution*Distribution@','debianComponent*Component@','debianArchitecture*Architecture@','modified'];
                break;
            case 'pypi':
                return ['pypiName*Name','pypiVersion*Version@','repo','path','modified'];
                break;
            case 'gems':
                return ['gemName*Name','gemVersion*Version@','repo','path','modified'];
                break;
            case 'rpm':
                return ['rpmName*Name','rpmVersion*Version@','rpmArchitecture*Architecture@','repo','path','modified'];
                break;
            case 'vagrant':
                return ['vagrantName*Box Name','vagrantVersion*Box Version@','vagrantProvider*Box Provider@','repo','path','modified'];
                break;
            default:
                return ['artifact','repo','path','modified'];
        }

    }

}

export function jfPackage() {
    return {
        restrict: 'EA',
        scope: {
            query: '=',
            parentController: '='
        },
        controller: jfPackageController,
        controllerAs: 'jfPackage',
        bindToController: true,
        templateUrl: 'directives/jf_search/search_tabs/jf_package.html'
    }
}
