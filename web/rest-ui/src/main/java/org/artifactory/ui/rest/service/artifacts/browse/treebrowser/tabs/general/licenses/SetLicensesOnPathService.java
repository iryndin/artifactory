package org.artifactory.ui.rest.service.artifacts.browse.treebrowser.tabs.general.licenses;

import org.apache.http.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.license.LicensesAddon;
import org.artifactory.api.license.LicenseInfo;
import org.artifactory.api.properties.PropertiesService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general.licenses.GeneralTabLicenseModel;
import org.artifactory.ui.utils.RequestUtils;
import org.artifactory.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SetLicensesOnPathService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(SetLicensesOnPathService.class);

    private final Pattern UNKNOWN_PATTERN = Pattern.compile("Unknown\\((.*)\\)", Pattern.CASE_INSENSITIVE);

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    PropertiesService propertiesService;

    @Autowired
    AuthorizationService authService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        RepoPath path = RequestUtils.getPathFromRequest(request);
        if (!authService.canAnnotate(path)) {
            response.error("Insufficient permissions for operation").responseCode(HttpStatus.SC_FORBIDDEN);
            return;
        }
        LicensesAddon licensesAddon = addonsManager.addonByType(LicensesAddon.class);
        List<GeneralTabLicenseModel> newLicensesNames = request.getModels();
        Set<LicenseInfo> newLicenses = newLicensesNames.stream()
                .map(this::trimUnknownFromName)
                .map(licensesAddon::getLicenseByName)
                .collect(Collectors.toSet());
        if (CollectionUtils.isNullOrEmpty(newLicenses)) {
            log.debug("Request sent with empty license set - deleting license properties from path {}", path);
            propertiesService.deleteProperty(path, LicensesAddon.LICENSES_PROP_FULL_NAME);
            propertiesService.deleteProperty(path, LicensesAddon.LICENSES_UNKNOWN_PROP_FULL_NAME);
            response.info("Successfully updated License information");
        } else {
            if (licensesAddon.setLicensePropsOnPath(path, newLicenses)) {
                response.info("Successfully updated License information");
            } else {
                response.error("Failed to update license information - check the log for more information")
                        .responseCode(HttpStatus.SC_BAD_REQUEST);
            }
        }
    }

    /**
     * UI sends unknown licenses as "Unknown(<name>)" (as sent to it by LicenseService) - which will cause duplicate
     * "Unknown" prefixes when setting the license again.
     * This method trims the "Unknown(..)" part so we get only the license name.
     */
    private String trimUnknownFromName(GeneralTabLicenseModel license) {
        Matcher matcher = UNKNOWN_PATTERN.matcher(license.getName());
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return license.getName();
        }
    }
}
