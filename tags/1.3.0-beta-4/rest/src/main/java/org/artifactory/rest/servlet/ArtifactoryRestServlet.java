/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.rest.servlet;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.sun.jersey.spi.service.ComponentContext;
import com.sun.jersey.spi.service.ComponentProvider;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.rest.common.SpringAutowired;

import javax.servlet.ServletConfig;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * User: freds Date: Aug 13, 2008 Time: 3:14:57 PM
 */
public class ArtifactoryRestServlet extends ServletContainer {
    @Override
    protected void configure(ServletConfig config, ResourceConfig rc,
            WebApplication application) {
        super.configure(config, rc, application);

        Set<Object> pi = rc.getProviderInstances();
        pi.add(getSpringBeanInjector(AuthorizationService.class));
        pi.add(getSpringBeanInjector(CentralConfigService.class));
        pi.add(getSpringBeanInjector(UserGroupService.class));
        pi.add(getSpringBeanInjector(AclService.class));
        pi.add(getSpringBeanInjector(RepositoryService.class));
        pi.add(getSpringBeanInjector(ArtifactoryContext.class));
        pi.add(getSpringBeanInjector(SecurityService.class));
    }

    public static <T> SpringBeanInjector<T> getSpringBeanInjector(Class<T> beanInterface) {
        return new SpringBeanInjector<T>(beanInterface);
    }

    public static class SpringBeanInjector<T>
            implements InjectableProvider<SpringAutowired, Type>, Injectable<T> {
        private Class<T> type;

        public SpringBeanInjector(Class<T> t) {
            this.type = t;
        }

        public ComponentProvider.Scope getScope() {
            return ComponentProvider.Scope.PerRequest;
        }

        public Injectable getInjectable(ComponentContext ic,
                SpringAutowired autowired, Type type) {
            if (type.equals(this.type)) {
                return this;
            } else {
                return null;
            }
        }

        public T getValue(HttpContext context) {
            /*
            (ArtifactoryContext) context
                    .getAttribute("org.springframework.web.context.ROOT");
            */
            ArtifactoryContext springContext = ContextHelper.get();
            if (springContext == null) {
                return null;
            }
            return springContext.beanForType(type);
        }
    }
}
