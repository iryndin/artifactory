export function jfValidatorCron(CronTimeDao, $q, $timeout, ArtifactoryNotifications) {
    return {
        restrict: 'A',
        require: 'ngModel',

        link: function jfCronValidatorLink(scope, element, attrs, ngModel) {
            let cronTimeDao = CronTimeDao.getInstance();

            let cache = {};

            function getFromServer(data) {
                if (!cache[data.cron]) {
                    cache[data.cron] = cronTimeDao.get(data).$promise;
                }
                return cache[data.cron];
            }

            function validateCron(key) {
                return function (modelValue, viewValue) {
                    var value = modelValue || viewValue;

                    if (!value) {

                        return $q.when();
                    }
                    if (value.length < 11) {
                        if (key === 'invalidCron') {
                            return $q.reject();
                        }
                        else {
                            return $q.when();
                        }
                    }

                    let data = {cron: value};
                    if (attrs.jfValidatorCronIsReplication) {
                        data.isReplication = true;
                    }

                    return getFromServer(data)
                            .catch(function (result) {
                                if (result.data.error === key || (result.data.feedbackMsg && result.data.feedbackMsg.error === key)) {
                                    if (key === 'shortCron') {
                                        ArtifactoryNotifications.create({warn: "The current Cron expression will " +
                                        "result in very frequent replications. \nThis will impact system performance."});
                                        return true;
                                    }
                                    return $q.reject();
                                }
                                return true;
                            });
                }
            }

            ngModel.$asyncValidators.invalidCron = validateCron('invalidCron');
            ngModel.$asyncValidators.shortCron = validateCron('shortCron');
            ngModel.$asyncValidators.pastCron = validateCron('pastCron');

        }
    }
}