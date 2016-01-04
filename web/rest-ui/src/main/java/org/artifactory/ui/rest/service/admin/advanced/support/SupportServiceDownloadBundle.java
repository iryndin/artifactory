package org.artifactory.ui.rest.service.admin.advanced.support;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.support.SupportAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SupportServiceDownloadBundle<String> implements RestService<String> {

    @Override
    public void execute(ArtifactoryRestRequest<String> request, RestResponse response) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SupportAddon supportAddon = addonsManager.addonByType(SupportAddon.class);

        if (supportAddon.isSupportAddonEnabled()) {
            String bundle = request.getImodel();

            InputStream is = null;
            try {
                is = supportAddon.download(bundle.toString());
                if (is != null) {
                    response.iModel(is);
                } else {
                    response.error("No content could be found, see log for more details");
                }
            } catch (FileNotFoundException e) {
                response.error("Support bundle \"" + bundle + "\" does not exist");
                return;
            }
        }
    }
}
