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

package org.artifactory.rest.servlet;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.SpringConfigResourceLoader;
import org.slf4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;

import javax.servlet.ServletConfig;

/**
 * We use our own rest servlet for the initialization using ArtifactoryContext.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryRestServlet extends ServletContainer {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryRestServlet.class);

    @Override
    protected void configure(ServletConfig config, ResourceConfig rc, WebApplication application) {
        super.configure(config, rc, application);
    }

    @Override
    protected void initiate(ResourceConfig rc, WebApplication wa) {
        try {
            ArtifactoryContext artifactoryContext =
                    (ArtifactoryContext) getServletContext().getAttribute(
                            SpringConfigResourceLoader.APPLICATION_CONTEXT_KEY);
            SpringComponentProviderFactory springComponentProviderFactory =
                    new SpringComponentProviderFactory(rc, (ConfigurableApplicationContext) artifactoryContext);
            wa.initiate(rc, springComponentProviderFactory);
        } catch (RuntimeException e) {
            log.error("Exception in initialization of the Rest servlet");
            throw e;
        }
    }
}
