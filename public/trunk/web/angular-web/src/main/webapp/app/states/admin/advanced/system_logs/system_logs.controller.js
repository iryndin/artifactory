import API from '../../../../constants/api.constants';

export class AdminAdvancedSystemLogsController {
    constructor($scope, SystemLogsDao, $interval, $window, $timeout) {

        this.logsDao = SystemLogsDao;
        this.$interval = $interval;
        this.$window = $window;
        this.$timeout = $timeout;

        this.intervalPromise = null;

        this._getInitialData();

        $scope.$on('$destroy', ()=> {
            this.stopInterval();
        });
    }

    _getInitialData() {
        this.logsDao.getLogs().$promise.then((data)=> {
            this.refreshRateSecs = data.refreshRateSecs;
            this.logs = _.map(data.logs, (logName)=>{return {logName:logName}});
            this.selectedLog = this.logs[0].logName;
            this.data = {fileSize: 0};
            this._getLogData();
        });
    }

    _getLogData() {
        this.logsDao.getLogData({id: this.selectedLog, fileSize: this.data.fileSize, $no_spinner: true}).$promise.then((data)=> {
            if (this.data.fileSize === 0) {
                this.$timeout(()=> {
                    var textarea = document.getElementById('textarea');
                    textarea.scrollTop = textarea.scrollHeight;
                });
            }

            if (data.fileSize) {
                this.data = data;
            }
            if (!this.intervalPromise) {
                this.startInterval();
            }

        });
    }

    download() {
        this.$window.open(`${API.API_URL}/systemlogs/downloadFile?id=`+this.selectedLog, '_blank');
    }


    onChangeLog() {
        this.stopInterval();
        this.data = {fileSize: 0};
        this._getLogData();
    }

    startInterval() {
        this.intervalPromise = this.$interval(()=> {
//                console.log('poling....');
            this._getLogData();
        }, this.refreshRateSecs * 1000);
    }

    stopInterval() {
        if (this.intervalPromise) {
            this.$interval.cancel(this.intervalPromise);
            this.intervalPromise = null;
        }

    }

}
