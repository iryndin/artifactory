/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.converters;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.property.ArtifactoryConverter;
import org.artifactory.common.property.FatalConversionException;
import org.artifactory.storage.db.properties.model.DbProperties;
import org.artifactory.storage.db.properties.service.ArtifactoryCommonDbPropertiesService;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author: gidis
 */
public class ServiceConverterHandler implements ArtifactoryConverterAdapter {
    private static final Logger log = LoggerFactory.getLogger(ServiceConverterHandler.class);
    private VersionProvider vm;

    public ServiceConverterHandler(VersionProvider vm) {
        this.vm = vm;
    }

    public void convert(ArtifactoryConverter artifactoryConverter) {
        //Run any necessary conversions on bean to bring the system up to date with the current version
        try {
            artifactoryConverter.convert(vm.getOriginalHomeVersionDetails(), vm.getRunningVersionDetails());
        } catch (FatalConversionException e) {
            //When a fatal conversion happens fail the context loading
            log.error("Conversion failed with fatal status.\n" +
                    "You should analyze the error and retry launching " +
                    "Artifactory. Error is: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            //When conversion fails - report and continue - don't fail
            log.error("Failed to run configuration conversion.", e);
        }
    }


    @Override
    public void conversionEnded() {
        // Update DB properties
        ArtifactoryCommonDbPropertiesService dbPropertiesService = ContextHelper.get().beanForType(
                ArtifactoryCommonDbPropertiesService.class);
        dbPropertiesService.updateDbProperties(createDbPropertiesFromVersion(vm.getRunningVersionDetails()));
    }


    public static DbProperties createDbPropertiesFromVersion(CompoundVersionDetails versionDetails) {
        long installTime = System.currentTimeMillis();
        return new DbProperties(installTime,
                versionDetails.getVersionName(),
                versionDetails.getRevisionInt(),
                versionDetails.getTimestamp()
        );
    }

    @Override
    public boolean isInterested(CompoundVersionDetails source, CompoundVersionDetails target) {
        return source != null && !source.isCurrent();
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        // Not used
    }
}