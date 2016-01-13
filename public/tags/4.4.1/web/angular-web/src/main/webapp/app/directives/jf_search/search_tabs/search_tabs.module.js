import {jfQuick}    from './jf_quick'
import {jfClass}    from './jf_class'
import {jfPackage}  from './jf_package'
/*
import {jfGavc}     from './jf_gavc'
*/
import {jfProperty} from './jf_property'
import {jfChecksum} from './jf_checksum'
import {jfRemote}   from './jf_remote'
import {jfTrash}    from './jf_trash'

export default angular.module('searchTabs', [])
        .directive({
            'jfQuick': jfQuick,
            'jfClass': jfClass,
            'jfPackage': jfPackage,
/*
            'jfGavc' : jfGavc,
*/
            'jfProperty':jfProperty,
            'jfChecksum': jfChecksum,
            'jfRemote':jfRemote,
            'jfTrash': jfTrash
        })