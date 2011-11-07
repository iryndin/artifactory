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

package org.artifactory.webapp.spring;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.injection.ComponentInjector;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.ISpringContextLocator;
import org.artifactory.webapp.servlet.RequestUtils;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;

/**
 * @author yoavl
 */
public class ArtifactorySpringComponentInjector extends ComponentInjector {

    /**
     * Metadata key used to store application context holder in application's metadata
     */
    private static MetaDataKey CONTEXT_KEY = new MetaDataKey<ApplicationContextHolder>() {
        private static final long serialVersionUID = 1L;
    };

    public ArtifactorySpringComponentInjector(WebApplication webapp) {
        if (webapp == null) {
            throw new IllegalArgumentException("Argument [[webapp]] cannot be null");
        }
        ApplicationContext ctx = get(webapp);
        if (ctx == null) {
            throw new IllegalArgumentException("Argument [[ctx]] cannot be null");
        }
        // store context in application's metadata ...
        webapp.setMetaData(CONTEXT_KEY, new ApplicationContextHolder(ctx));
        //Replace the annotation aware injector with one using a context sensitive bean cache
        try {
            //Don't set the injector more than once, since it's not thread safe
            InjectorHolder.getInjector();
        } catch (IllegalStateException e) {
            //We don't have an injector yet
            InjectorHolder.setInjector(new ArtifactoryAnnotSpringInjector(new NonCachingContextLocator()));
        }
    }

    private static ApplicationContext get(WebApplication webapp) {
        ServletContext sc = webapp.getServletContext();
        ApplicationContext ac = (ApplicationContext) RequestUtils.getArtifactoryContext(sc);
        return ac;
    }

    /**
     * This is a holder for the application context. The reason we need a holder is that metadata only supports storing
     * serializable objects but application context is not. The holder acts as a serializable wrapper for the context.
     * Notice that although holder implements IClusterable it really is not because it has a reference to non
     * serializable context - but this is ok because metadata objects in application are never serialized.
     *
     * @author ivaynberg
     */
    private static class ApplicationContextHolder implements IClusterable {
        private static final long serialVersionUID = 1L;

        private final ApplicationContext context;

        /**
         * Constructor
         *
         * @param context
         */
        public ApplicationContextHolder(ApplicationContext context) {
            this.context = context;
        }

        /**
         * @return the context
         */
        public ApplicationContext getContext() {
            return context;
        }
    }

    private static class NonCachingContextLocator implements ISpringContextLocator {

        private static final long serialVersionUID = 1L;

        public ApplicationContext getSpringContext() {
            return ((ApplicationContextHolder) Application.get().getMetaData(CONTEXT_KEY)).getContext();
        }
    }
}
