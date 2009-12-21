/*
 * This file is part of Artifactory.
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

import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.spring.ISpringContextLocator;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Yoav Landman
 */
public class ArtifactoryBeanLocator implements IProxyTargetLocator {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryBeanLocator.class);

    private final Class<?> type;
    private final ISpringContextLocator locator;


    public ArtifactoryBeanLocator(Class<?> type, ISpringContextLocator locator) {
        this.type = type;
        this.locator = locator;
    }

    public Object locateProxyTarget() {
        ArtifactoryContext context = (ArtifactoryContext) locator.getSpringContext();
        Object bean = context.beanForType(type);
        return bean;
    }
}
