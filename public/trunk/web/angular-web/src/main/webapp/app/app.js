import Models                   from './models/models.module';



// For debugging:
window._inject = function(injectable) {
    return angular.element(document.body).injector().get(injectable);
}

if (!String.prototype.endsWith) {
    String.prototype.endsWith = function (str) {
        return this.substr(this.length - str.length,str.length)===str;
    }
}
if (!String.prototype.startsWith) {
    String.prototype.startsWith = function (str) {
        return this.substr(0, str.length)===str;
    }
}

/**
 * providers configurations
 * @param $urlRouterProvider
 */
function appConfig($stateProvider, $urlRouterProvider, ngClipProvider, $httpProvider) {
    $urlRouterProvider.otherwise('/home');
    ngClipProvider.setPath("css/ZeroClipboard.swf");
    $httpProvider.interceptors.push('artifactorySpinnerInterceptor');
    $httpProvider.interceptors.push('artifactoryMessageInterceptor');
    $httpProvider.interceptors.push('artifactorySessionInterceptor');
    $httpProvider.interceptors.push('artifactoryServerErrorInterceptor');
    $httpProvider.interceptors.push('artifactoryDebugInterceptor');
}

function appRun($httpBackend, $rootScope, ArtifactoryFeatures, $timeout) {
    $httpBackend.whenPOST(/.*/).passThrough();
    $httpBackend.whenPUT(/.*/).passThrough();
    $httpBackend.whenGET(/.*/).passThrough();
    $httpBackend.whenDELETE(/.*/).passThrough();
    $httpBackend.whenPATCH(/.*/).passThrough();
    defineCodeMirrorMimes();
    defineCodeMirrorLinkOverlay();
    defineCodeMirrorAqlMode();

    $timeout(()=>{
        if (ArtifactoryFeatures.isOss()) {
            installHiringDevsHook();
        }
    },5000)
}

angular.module('artifactory.ui', [

    // Vendor modules
    'ngMessages',
    'ui.utils',
    'ui.select',
    'selectize',
    'ui.codemirror',
    'cfp.hotkeys',
    'artifactory.templates',
    'ui.router',
/* --- MOVED TO artifactory_grid module
    'ui.grid',
    'ui.grid.autoResize',
    'ui.grid.edit',
    'ui.grid.selection',
    'ui.grid.pagination',
    'ui.grid.grouping',
    'ui.grid.resizeColumns',
*/
    'ngCookies',
    'toaster',
    'ngSanitize',
    'ui.layout',
    'ui.bootstrap',
    'ngMockE2E',
    'ngClipboard',
    'ngPasswordStrength',
    'angular-capitalize-filter',
    'angularFileUpload',
    // Application modules
    'artifactory.services',
    'artifactory.directives',
    'artifactory.dao',
    'artifactory.ui_components',
    'artifactory.states',
    'artifactory.filters',
    'ui.grid.draggable-rows',
    'color.picker',
    Models.name
])
    .config(appConfig)
    .run(appRun);

function aliasMime(newMime, existingMime) {
    CodeMirror.defineMIME(newMime, CodeMirror.mimeModes[existingMime]);
}
function defineCodeMirrorMimes() {
    aliasMime('text/x-java-source', 'text/x-java');
    aliasMime('pom', 'text/xml');

    /* Example definition of a simple mode that understands a subset of
     * JavaScript:
     */

}


function defineCodeMirrorAqlMode() {
    CodeMirror.defineMode("aql", function () {
        var urlRegex = /^https?:\/\/[a-zA-Z]+(\.)?(:[0-9]+)?.+?(?=\s|$|"|'|>|<)/;

        let inApiKey = false;
        return {
            token: function (stream, state) {

                if (stream.match(/(?:curl|\-\H|\-\X|\-d|POST)\b/)) {
                    return "external-command";
                }
                else if (stream.match(/(?:X\-Api\-Key)\b/)) {
                    inApiKey=true;
                    return "header-tag";
                }
                else if (stream.match("'")) {
                    inApiKey = false;
                    return null;
                }
                else if (stream.match(/(?:find|include|limit|sort|offset)\b/)) {
                    return "aql-keyword";
                }
                else if (stream.match(/(?:\$and|\$or|\$ne|\$gt|\$gte|\$lt|\$lte|\$rf|\$msp|\$match|\$nmatch|\$eq|\$asc|\$desc)\b/)) {
                    return "aql-operators";
                }
                else if (stream.match(/(?:items|builds|entries)\b/)) {
                    return "aql-domain";
                }
                else if (stream.match(/[\{\}\[\]\(\)]+/)) {
                    return "aql-brackets";
                }
                else if (stream.match(urlRegex)) {
                    return "api-url";
                }
                else {
                    let ret = null;
                    if (inApiKey && !stream.match(':')) {
                        ret = "api-key";
                    }
                    stream.next();
                    return ret;
                }
            }
        };

    });
}

function defineCodeMirrorLinkOverlay() {
    var urlRegex = /^https?:\/\/[a-zA-Z]+(\.)?(:[0-9]+)?.+?(?=\s|$|"|'|>|<)/;
    CodeMirror.defineMode("links", function (config, parserConfig) {
        var linkOverlay = {
            token: function (stream, state) {
                if (stream.match(urlRegex)) {
                    return "link";
                }
                while (stream.next() != null && !stream.match(urlRegex, false)) {
                }
                return null;
            }
        };

        return CodeMirror.overlayMode(CodeMirror.getMode(config, config.mimeType || "text/xml"), linkOverlay);
    });
}




function installHiringDevsHook() {
    window.u = {
        r: {
            reading: function() {
                window.never={
                    mind: function() {
                        window.location.href="https://www.jfrog.com/about/open-positions/";
                    }
                };
                setTimeout(function() {
                    delete window.never;
                },500);
                return false;
            }
        }
    };
    console.log('%cif (u.r.reading(this) && u.can(code) && u.r.looking4.a.job) {\n    u.may(b.come.a(new JFrog("Star Developer")));\n}\nelse {\n    never.mind();\n}\n// Run this code snippet to find out more about CAREERS & OPPORTUNITIES @ JFrog', "font: 12px sans-serif; color: #43a047;");
}
