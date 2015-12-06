import EVENTS from '../constants/artifacts_events.constants';

export function artifactorySpinnerInterceptor($injector, $timeout, $q, ArtifactoryEventBus) {

    let SPINNER_TIMEOUT = 500; //milis
    let serial = 0;
    let timeouts = {};
    let pendings = [];
    let canceled = [];
    let inDelay = [];

    ArtifactoryEventBus.register(EVENTS.CANCEL_SPINNER, () => {
        if (pendings.length) {
            ArtifactoryEventBus.dispatch(EVENTS.HIDE_SPINNER);
            canceled = canceled.concat(pendings);
            pendings = [];
//            console.log('canceled: ', canceled);
        }
    });


    function request(req) {

        req.headers['Request-Agent'] = 'artifactoryUI';

        if ((!req.params || !req.params.$no_spinner) && req.url.startsWith('../ui/') ) {

            let domain = req.params ? req.params.$spinner_domain : undefined;

            req.headers.serial = serial;

            pendings.push(serial);

            inDelay.push(serial);
            timeouts[serial] = $timeout(()=> {
                let canceledIndex = canceled.indexOf(req.headers.serial);

                if (canceledIndex < 0) {
                    ArtifactoryEventBus.dispatch(EVENTS.SHOW_SPINNER, domain);
                }
                else {
                    canceled.splice(canceledIndex,1);
                }

                let inDelayIndex = inDelay.indexOf(req.headers.serial);
                if (inDelayIndex >= 0) inDelay.splice(inDelayIndex,1);

//                console.log('inDelay',inDelay);
            }, SPINNER_TIMEOUT);

            serial++;

        }

        return req;
    }

    function response(res) {
        if (handleResponse(res)) return res;
        else return $q.defer().promise;
    }

    function responseError(res) {
        if (handleResponse(res)) return $q.reject(res);
        else return $q.defer().promise;
    }

    function handleResponse(res) {
        let s = res.config.headers.serial;

        let pendingIndex = pendings.indexOf(s);
        if (pendingIndex >= 0) {
            pendings.splice(pendingIndex,1);
        }

        let inDelayIndex = inDelay.indexOf(s);

        let canceledIndex = canceled.indexOf(s);
        if (canceledIndex >= 0) {
//            console.log('canceled',res);
            if (inDelayIndex < 0) canceled.splice(canceledIndex,1);
            return false;
        }
        else {
            if (timeouts[s]) {
                if (inDelayIndex >= 0) inDelay.splice(inDelayIndex,1);
                else {
                    ArtifactoryEventBus.dispatch(EVENTS.HIDE_SPINNER);
                }
                $timeout.cancel(timeouts[s]);
                delete timeouts[s];
            }

            return  true;
        }

    }

    return {
        response: response,
        request: request,
        responseError: responseError
    };
}
