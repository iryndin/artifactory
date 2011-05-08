/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.logging;

import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.logging.mbean.Error;
import org.artifactory.logging.mbean.ErrorMBean;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.version.CompoundVersionDetails;
import org.springframework.stereotype.Service;

/**
 * Logging service main implementation
 *
 * @author Noam Y. Tenne
 */
@Service
@Reloadable(beanClass = LoggingService.class)
public class LoggingServiceImpl implements LoggingService {

    public void init() {
        InternalContextHelper.get().registerArtifactoryMBean(new Error(), ErrorMBean.class, null);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        /**
         * All conversions are done in org.artifactory.log.logback.LogbackContextHelper#configure
         */
    }
}