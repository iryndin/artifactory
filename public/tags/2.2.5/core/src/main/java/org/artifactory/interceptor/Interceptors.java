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

package org.artifactory.interceptor;

import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author yoav
 */
public abstract class Interceptors<T extends Interceptor> implements Iterable<T>, Serializable, ReloadableBean,
        BeanNameAware, ApplicationContextAware {
    private final static Logger log = LoggerFactory.getLogger(Interceptors.class);

    private final List<T> interceptors = new LinkedList<T>();
    private Class<T> interceptorInterface;

    protected ArtifactoryApplicationContext context;
    private String beanName;

    @SuppressWarnings({"unchecked"})
    protected Interceptors() {
        //Get all the interceptors that match the parametrized type of this chain
        this.interceptorInterface =
                (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public Iterator<T> iterator() {
        return interceptors.iterator();
    }

    public void init() {
        Collection<T> allInterceptorsIncludingMe = context.beansForType(interceptorInterface).values();
        Object thisAsBean = context.getBean(beanName);
        for (T t : allInterceptorsIncludingMe) {
            if (t != thisAsBean) {
                interceptors.add(t);
            }
        }
        log.debug("Loaded interceptors of type {}: {}", interceptorInterface.getSimpleName(), interceptors);
    }

    public void setApplicationContext(ApplicationContext context) {
        this.context = (ArtifactoryApplicationContext) context;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // nothing here
    }

    public void destroy() {
        // nothing here
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        // nothing here
    }
}