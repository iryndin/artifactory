/**
 * Validates an input to be valid entiyy name
 */
export function jfValidatorReverseProxyPort(ReverseProxiesDao, $q, $timeout) {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function jfValidatorReverseProxyPort(scope, element, attrs, ngModel) {

            let repoKey = attrs.jfValidatorReverseProxyPort;

            let portValidatorDao = ReverseProxiesDao;
            ngModel.$asyncValidators.port = validatePort;

            function validatePort(modelValue, viewValue) {
                var value = modelValue || viewValue;

                if (!value) {
                    return $q.when();
                }

                return ReverseProxiesDao.checkPort(repoKey ? {repoKey: repoKey} : {},{port: value}).$promise
                    .then(function (result) {
                        if (!result.portAvailable) {
                            return $q.reject();
                        }
                        return true;
                    });
            }
        }
    }
}