import {EVENTS_NAMES}     from '../constants/artifacts_events.constants';
import {COMMON_EVENTS_NAMES}     from '../constants/common_events.constants';
/**
 * Service for components communication
 */
export class ArtifactoryEventBus {

    /**
     * init an empty map
     */
    constructor() {
        this._listeners = Object.create(null);
    }


    _randomId() {
        return Math.floor((Math.random() * 100000000000) + 1);
    }

     /**
     *
     * push a callback to the event name array is exist.
     * if the event doesn't exist, create this key and
     * init an empty array for it.
     *
     * @param {string / array(string)} eventNames - single event or array of events
     * @param {Function} callback
     * @returns {Deregister} the deregistration function
     */
    register(eventNames, callback) {
         if (_.isArray(eventNames)) {
             return eventNames.map((eventName) => this._registerSingleEvent(eventName, callback));
         }
         else {
             return this._registerSingleEvent(eventNames, callback);
         }
     }

    _registerSingleEvent(eventName, callback) {
        this._verifyEventExists(eventName);
        this._listeners[eventName] = this._listeners[eventName] || [];
        let listener = {
            _callback: callback,
            _id: this._randomId()
        };
        this._listeners[eventName].push(listener);

        return () => {
            this._remove(eventName, listener._id);
        }
    }

    /**
     * Registers a callback and makes sure that it deregisters on scope destroy
     */
    registerOnScope(scope, eventNames, callback) {
        let deregisters = this.register(eventNames, callback);
        if (!_.isArray(deregisters)) deregisters = [deregisters];
        scope.$on('$destroy', () => {
            deregisters.forEach((deregister) => deregister());
        });
    }

    /**
     *
     * invoke all the callbacks in the array under the
     * event key. throw an error if the event key doesn't
     * exist
     *
     * @param {string} eventName
     */
    dispatch(eventName, payload) {
        this._verifyEventExists(eventName);
        if(this._listeners[eventName]) {
            this._listeners[eventName].forEach( (listener) => listener._callback(payload) )
        }
    }

    _verifyEventExists(eventName) {
        if (!EVENTS_NAMES[eventName] && !COMMON_EVENTS_NAMES[eventName]) throw new Error('There are no events registered under the name ' + eventName);
    }

    /**
     *
     * remove the callback from the array under the
     * event name key if exist.
     * throw an error if the event key doesn't exist
     *
     * @param {string} eventName
     * @param {Number} index
     */
    _remove(eventName, id) {
        if(this._listeners[eventName] == 'undefined') {
            throw new Error('This event does not exist');
        }
        if(!_.findWhere(this._listeners[eventName], {_id: id})) {
            throw new Error('This callback is not registered under this event name')
        }

        _.remove(this._listeners[eventName], (listener) => {
            return listener._id == id;
        })
    }
}