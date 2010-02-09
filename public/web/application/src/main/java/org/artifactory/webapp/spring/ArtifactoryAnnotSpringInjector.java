/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.spring;

import org.apache.wicket.injection.ConfigurableInjector;
import org.apache.wicket.injection.IFieldValueFactory;
import org.apache.wicket.spring.ISpringContextLocator;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Yoav Landman
 */
public class ArtifactoryAnnotSpringInjector extends ConfigurableInjector {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryAnnotSpringInjector.class);

    private IFieldValueFactory factory;

    /**
     * Constructor
     *
     * @param locator spring context locator
     */
    public ArtifactoryAnnotSpringInjector(ISpringContextLocator locator) {
        initFactory(locator);
    }

    private void initFactory(ISpringContextLocator locator) {
        factory = new ArtifactoryContextAnnotFieldValueFactory(locator);
    }

    @Override
    protected IFieldValueFactory getFieldValueFactory() {
        return factory;
    }
}
