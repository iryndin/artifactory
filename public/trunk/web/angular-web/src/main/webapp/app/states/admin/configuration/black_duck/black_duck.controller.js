import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminConfigurationBlack_duckController {

    constructor(BlackDuckDao, ProxiesDao, ArtifactoryModelSaver) {
        this.blackDuckDao = BlackDuckDao.getInstance();
        this.artifactoryModelSaver = ArtifactoryModelSaver.createInstance(this,['blackDuck']);
        this.proxiesDao = ProxiesDao;
        this.TOOLTIP = TOOLTIP.admin.configuration.blackDuck;
        this._initBlackDuck();
    }

    _initBlackDuck() {
        this.proxiesDao.get().$promise.then((proxies)=> {
            this.proxies = [''];
            this.proxies = this.proxies.concat(proxies);
            this.getBlackduckData();
        })
    }

    getBlackduckData() {
        this.blackDuckDao.get().$promise.then((data)=> {
//            console.log(data);
            this.blackDuck = data;
            this.artifactoryModelSaver.save();
        });
    }

    save(duck) {
        if (duck.proxyRef==='') delete duck.proxyRef;
        this.blackDuckDao.update(duck).$promise.then(()=>{
            this.artifactoryModelSaver.save();
        });
    }

    testBlackDuck(duck) {
        this.blackDuckDao.save(duck).$promise.then(function (data) {
//            console.log(data);
        });
    }

    reset() {
        this.artifactoryModelSaver.ask().then(()=>{
            this.getBlackduckData();
        });
    }
}