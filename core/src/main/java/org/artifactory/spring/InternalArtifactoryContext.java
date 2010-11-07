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

package org.artifactory.spring;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.security.SecurityService;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.schedule.TaskService;
import org.springframework.context.ApplicationContext;

import javax.management.MBeanServer;

/**
 * @author freds
 */
public interface InternalArtifactoryContext extends ArtifactoryContext, ReloadableBean, ApplicationContext {
    JcrService getJcrService();

    JcrRepoService getJcrRepoService();

    SecurityService getSecurityService();

    void addReloadableBean(Class<? extends ReloadableBean> interfaceClass);

    boolean isReady();

    TaskService getTaskService();

    /**
     * Registers an object as an mbean.
     *
     * @param mbean      The mbean implementation
     * @param mbeanIfc   The mbean interface
     * @param mbeanProps Optional string to attach to the mbean name
     * @return The registered mbean (might be a proxy of the original instance)
     */
    <T> T registerArtifactoryMBean(T mbean, Class<T> mbeanIfc, String mbeanProps);

    MBeanServer getMBeanServer();
}
