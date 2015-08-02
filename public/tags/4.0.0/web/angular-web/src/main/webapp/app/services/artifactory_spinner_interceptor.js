import EVENTS from '../constants/artifacts_events.constants';

export function artifactorySpinnerInterceptor($injector, $timeout, $q, ArtifactoryEventBus) {

    let SPINNER_TIMEOUT = 400; //milis
    let serial = 0;
    let timeouts = {};

    function request(req) {

        if (!req.params || !req.params.$no_spinner) {

            req.headers.serial = serial;
            timeouts[serial] = $timeout(()=> {
                ArtifactoryEventBus.dispatch(EVENTS.SHOW_SPINNER);
            }, SPINNER_TIMEOUT);

            serial++;

        }

        return req;
    }

    function response(res) {
        handleResponse(res);
        return res;
    }

    function responseError(res) {
        handleResponse(res);
        return $q.reject(res);
    }

    function handleResponse(res) {
        let s = res.config.headers.serial;
        if (timeouts[s]) {
            ArtifactoryEventBus.dispatch(EVENTS.HIDE_SPINNER);
            $timeout.cancel(timeouts[s]);
            delete timeouts[s];
        }
    }

    return {
        response: response,
        request: request,
        responseError: responseError
    };
}
