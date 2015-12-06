/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.features.matrix;

import org.artifactory.features.VersionFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * SmartRepo Version features descriptor
 *
 * @author Michael Pasternak
 */
@Lazy(true)
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Component()
public class SmartRepoVersionFeatures extends VersionFeatures {

    transient private static final Logger log = LoggerFactory.getLogger(SmartRepoVersionFeatures.class);

    transient public static final String SYNC_PROPERTIES = "SYNC_PROPERTIES";
    transient public static final String LIST_CONTENT = "LIST_CONTENT";
    transient public static final String SYNC_STATISTICS = "SYNC_STATISTICS";

    public SmartRepoVersionFeatures() {
        super("smartrepo-features.xml");
    }
}
