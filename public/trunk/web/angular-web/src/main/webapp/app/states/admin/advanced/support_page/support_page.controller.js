import TOOLTIP from '../../../../constants/artifact_tooltip.constant';

export class AdminAdvancedSupportPageController {
    constructor(SupportPageDao, ServerTimeDao, $timeout, $scope, artifactoryIFrameDownload, GeneralConfigDao, RESOURCE, ArtifactoryNotifications, $compile, ArtifactoryModal) {

        this.$scope = $scope;
        this.$timeout = $timeout;
        this.$compile = $compile;
        this.supportPageDao = SupportPageDao;
        this.serverTimeDao = ServerTimeDao;
        this.iFrameDownload = artifactoryIFrameDownload;
        this.RESOURCE = RESOURCE;
        this.TOOLTIP = TOOLTIP.admin.advanced.supportPage;
        this.artifactoryNotifications = ArtifactoryNotifications;
        this.modal = ArtifactoryModal;

        this.ready = false;

        this.timePeriodConfig = {
            maxItems: 1,
            create: false
        };


        GeneralConfigDao.get().$promise.then((data)=> {
            this.dateFormat = this._getDatePartFromFormat(data.dateFormat);
            this._init();
        });

    }


    _init() {


        this.serverTimeDao.get().$promise.then((serverTimeResource)=>{
            let serverTime = "";
            let json = serverTimeResource.toJSON();
            for (let i in json) {
                serverTime += json[i];
            }
            serverTime = parseInt(serverTime);
            let miliDiff = serverTime - (new Date()).getTime();
            let hourDiff = Math.round(miliDiff/(1000*60*60));
            this.timeDiff = hourDiff*1000*60*60;
            this._getOldBundles();
            this.ready = true;
        });

        let localNow = new Date();
        let today = this.today = new Date(localNow.getFullYear(), localNow.getMonth(), localNow.getDate());
        let minDate = this.minDate = new Date(today.getTime()-14*24*60*60*1000);

        this.initAndBindDatepickerElement('from-date','SupportPage.setup.systemLogsConfiguration.startDate',(date)=>{
            if (this.setup.systemLogsConfiguration.startDate.getTime() > this.setup.systemLogsConfiguration.endDate.getTime()) {
                this.setup.systemLogsConfiguration.endDate = new Date(this.setup.systemLogsConfiguration.startDate);
                $('#to-date').datepicker("setDate", this.setup.systemLogsConfiguration.endDate);
            }
        });
        this.initAndBindDatepickerElement('to-date','SupportPage.setup.systemLogsConfiguration.endDate',(date)=>{
            if (this.setup.systemLogsConfiguration.startDate.getTime() > this.setup.systemLogsConfiguration.endDate.getTime()) {
                this.setup.systemLogsConfiguration.startDate = new Date(this.setup.systemLogsConfiguration.endDate);
                $('#from-date').datepicker("setDate", this.setup.systemLogsConfiguration.startDate);
            }
        });

        this.setup = {
            hideUserDetails: true,
            systemLogsConfiguration : {
                enabled: true,
                startDate: new Date(today.getTime()-2*24*60*60*1000),
                endDate: today
            },
            systemInfoConfiguration : {
                enabled : true
            },
            securityInfoConfiguration : {
                enabled : true
            },
            configDescriptorConfiguration : {
                enabled : true
            },
            configFilesConfiguration : {
                enabled : true
            },
            storageSummaryConfiguration : {
                enabled : true
            },
            threadDumpConfiguration : {
                enabled : true,
                count: 1,
                interval: 0
            }
        };

        this.timePeriodOptions = [
            {text: "Last 24 Hours", value: 1},
            {text: "Last 3 Days", value: 3},
            {text: "Last 5 Days", value: 5},
            {text: "Last 7 Days", value: 7},
            {text: "Custom Dates", value: 'CUSTOM'}
        ];

        this.timePeriodSelection = 1;

    }

    _getOldBundles() {
        this.supportPageDao.listBundles().$promise.then((data)=>{
            this.oldBundles = _.filter(data,(obj)=>{
                return typeof obj === 'string';
            });
            this.oldBundles = _.map(this.oldBundles,(filename)=>{
                let filenameNoExt=filename.split('.')[0];
                let time=parseInt(filenameNoExt.split('-')[filenameNoExt.split('-').length-1]) - this.timeDiff;
                return {
                    filename: filename,
                    date: (new Date(time)).toString()
                }
            })
        });
    }

    onChangeTimePeriod() {

        if (this.timePeriodSelection !== "CUSTOM") {
            this.setup.systemLogsConfiguration.endDate = this.today;
            this.setup.systemLogsConfiguration.startDate = new Date(this.today.getTime() - (this.timePeriodSelection-1) *24*60*60*1000);
            $('#from-date').datepicker("setDate", this.setup.systemLogsConfiguration.startDate);
            $('#to-date').datepicker("setDate", this.setup.systemLogsConfiguration.endDate);
        }
    }

    initAndBindDatepickerElement(elemId,model,changeCallback) {
        this.$timeout(()=>{
            $('#'+elemId).datepicker({
                dateFormat: this.dateFormat,//'dd-mm-yy',
                maxDate: this.today,
                showOn: 'none',
                onSelect: (date, dp) => {
                    let selectedDate = new Date(dp.selectedYear,dp.selectedMonth,dp.selectedDay);
                    _.set(this.$scope,model,selectedDate);
                    this.$scope.$apply();
                    changeCallback(selectedDate);
                }
            });
            $('#'+elemId).datepicker("setDate", _.get(this.$scope,model));
            changeCallback(_.get(this.$scope,model));
        });
    }

    openDatePicker(e) {
        if (this.timePeriodSelection === "CUSTOM" && this.setup.systemLogsConfiguration.enabled) {
            $(e.srcElement).datepicker("show");
        }
    }

    download(link) {
        this.iFrameDownload(link);
    }

    downloadOld(filename) {
        let url = this.RESOURCE.API_URL + '/userSupport/downloadBundle/' + filename;
        this.iFrameDownload(url);
    }

    deleteOld(filename) {
        this.modal.confirm(`Are you sure you want to delete this bundle?`)
                .then(() => {
                    this.supportPageDao.deleteBundle({}, {filename: filename}).$promise.then((resp)=> {
                        this._getOldBundles();
                    })
                });
    }

    create() {

        let payload = angular.copy(this.setup);
        if (this.timePeriodSelection != 1) {
            payload.systemLogsConfiguration.startDate = payload.systemLogsConfiguration.startDate.getTime() + this.timeDiff;
            payload.systemLogsConfiguration.endDate = payload.systemLogsConfiguration.endDate.getTime() + this.timeDiff + 24*60*60*1000;
        }
        else {
            payload.systemLogsConfiguration.endDate = (new Date()).getTime() + this.timeDiff;
            payload.systemLogsConfiguration.startDate = payload.systemLogsConfiguration.endDate - 24*60*60*1000;
        }

        payload.configDescriptorConfiguration.hideUserDetails = this.setup.hideUserDetails;
        payload.securityInfoConfiguration.hideUserDetails = this.setup.hideUserDetails;

        delete payload.hideUserDetails;

        this.supportPageDao.generateBundle({},payload).$promise.then((files)=>{
            if (files.length) {
                //this.downloadLinks = files;
                this.artifactoryNotifications.createMessageWithHtml({
                    type: 'success',
                    body: 'Successfully created support information <a href id="link-in-toaster" ng-click="SupportPage.download(\''+files[0]+'\')">bundle</a>',
                    timeout: 10000
                });
                this.$timeout(()=>{
                    let elem = angular.element($('#link-in-toaster'));
                    this.$compile(elem)(this.$scope);

                })
            }
            this._getOldBundles();
        });
    }

    _getDatePartFromFormat(format) {
        let parts = this._breakFormat(format);

        let currContext = 'U';  //U = Unkown D = Date T = Time
        let unknowns = [];

        let gotMonth = false;
        for (let i in parts) {
            let part = parts[i];

            if (_.contains('dy',part.char)) {
                part.context = 'D';
            }
            else if (_.contains('hs',part.char)) {
                part.context = 'T';
            }
            else if (part.char === 'm') {
                if (gotMonth) currContext = 'U';
                part.context = !gotMonth && currContext === 'D' ? 'D' :'U';
                unknowns.push(part);
            }
            if (part.context) currContext = part.context;
            if (currContext !== 'U' && unknowns.length) {
                for (let i in unknowns) {
                    unknowns[i].context = currContext;
                    if (currContext === 'D') gotMonth = true;
                }
                unknowns = [];
            }
        }

        let insideDate=false;
        let justDate = [];
        for (let i in parts) {
            let part = parts[i];
            if (part.context === 'D') {
                insideDate = true;
            }
            else if (part.context === 'T') {
                insideDate = false;
            }
            if (insideDate) justDate.push(part);
        }

        let trim = 0;
        for (let i = justDate.length - 1; i>=0; i--) {
            let part = parts[i];
            if (part.context) {
                break;
            }
            else justDate.pop();
        }

        let finalResult = '';
        for (let i in justDate) {
            let part = justDate[i];
            finalResult += part.precise;
        }

        return finalResult;

    }

    _breakFormat(format) {
        let parts = [];
        while (format.length) {
            let part = this._getNextFormatPart(format);
            parts.push(part);
            format = format.substr(part.count);
        }
        return parts;
    }

    _getNextFormatPart(format) {
        let temp = format.toLowerCase();
        let char = temp.charAt(0);
        let count = 0;
        while (temp.charAt(0) === char) {
            count++;
            temp = temp.substr(1);
        }
        let precise = format.substr(0,count);
        return {
            char: char,
            count: count,
            precise: precise
        }
    }
}