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

package org.artifactory.repo.jcr.cache.expirable;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.jcr.JcrCacheRepo;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.spring.Reloadable;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
@Service
@Reloadable(beanClass = CacheExpiry.class, initAfter = JcrService.class)
public class CacheExpiryImpl implements CacheExpiry, BeanNameAware, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(CacheExpiryImpl.class);

    private ArtifactoryApplicationContext context;
    private Set<CacheExpirable> expirable = Sets.newHashSet();
    private String beanName;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = ((ArtifactoryApplicationContext) applicationContext);
    }

    public void setBeanName(String name) {
        beanName = name;
    }

    public void init() {
        Collection<CacheExpirable> allExpirableIncludingMe = context.beansForType(CacheExpirable.class).values();
        Object thisAsBean = context.getBean(beanName);
        for (CacheExpirable t : allExpirableIncludingMe) {
            if (t != thisAsBean) {
                expirable.add(t);
            }
        }
        log.debug("Loaded expirable: {}", expirable);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public boolean isExpirable(JcrCacheRepo jcrCacheRepo, String path) {
        if (StringUtils.isNotBlank(path)) {
            for (CacheExpirable cacheExpirable : expirable) {
                if (cacheExpirable.isExpirable(jcrCacheRepo, path)) {
                    return true;
                }
            }
        }

        return false;
    }
}
