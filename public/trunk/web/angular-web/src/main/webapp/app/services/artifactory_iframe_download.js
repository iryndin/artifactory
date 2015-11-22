export function artifactoryIFrameDownload(ArtifactoryNotifications, $timeout) {
    return function(url,defaultErrorMessage) {
        let iframe=$('<iframe style="display: none">');
        iframe.load((event)=>{
            let response,defaultMessage;
            try {
                response = $(event.target).contents().find('pre').text();
            }
            catch(e) { //workaround for ie .contents() ACCESS DENIED error
                defaultMessage = defaultErrorMessage || 'Something went wrong.';
            }
            if (defaultMessage || response) {
                let message = defaultMessage || JSON.parse(JSON.parse(response).errors[0].message).error;
                $timeout(()=>{
                    ArtifactoryNotifications.create({error: message});
                    if (iframe.parent().length) iframe.remove();
                });
            }
        });

        iframe.ready(() => {
            $timeout(()=>{
                if (iframe.parent().length) iframe.remove();
            },15000);
        });

        iframe.attr('src', url).appendTo('body');
    }
}