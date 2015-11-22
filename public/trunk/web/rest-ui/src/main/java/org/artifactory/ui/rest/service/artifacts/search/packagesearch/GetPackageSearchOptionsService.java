package org.artifactory.ui.rest.service.artifacts.search.packagesearch;

import com.google.common.collect.Lists;
import org.apache.http.HttpStatus;
import org.artifactory.aql.AqlService;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchModel;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.search.AqlUISearchPackageTypeModel;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.FieldSearchCriteria;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.criteria.PackageSearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Dan Feldman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetPackageSearchOptionsService implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetPackageSearchOptionsService.class);

    private static final String AVAILABLE_PACKAGES = "availablePackages";

    @Autowired
    AqlService aqlService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        String pkgType = request.getPathParamByKey("type");
        pkgType = pkgType == null ? "" : pkgType;
        //Special call to get package types that are available to search by;
        if (pkgType.equalsIgnoreCase(AVAILABLE_PACKAGES)) {
            response.iModelList(
                    Stream.of(PackageSearchCriteria.PackageSearchType.values())
                            .map(AqlUISearchPackageTypeModel::new)
                            .collect(Collectors.toList()));

            response.responseCode(HttpStatus.SC_OK);
        } else {
            try {
                List<AqlUISearchModel> availableOptions = Lists.newArrayList();
                availableOptions.addAll(PackageSearchCriteria.getCriteriaByPackage(pkgType)
                                .stream()
                                .map(PackageSearchCriteria::getModel)
                                .collect(Collectors.toList())
                );
                // TODO: [by dan] limited to items domain for now - should allow more based on incoming parameter?
                // TODO: [by dan] if pathParam "buildType" == artifact \ dependency -> return relevant field enum values
                availableOptions.addAll(Stream.of(FieldSearchCriteria.values())
                                .map(FieldSearchCriteria::getModel)
                                .collect(Collectors.toList())
                );
                response.iModel(availableOptions);
                response.responseCode(HttpStatus.SC_OK);
            } catch (UnsupportedOperationException uoe) {
                log.debug(uoe.getMessage(), uoe);
                response.error(uoe.getMessage());
            }
        }
    }
}
