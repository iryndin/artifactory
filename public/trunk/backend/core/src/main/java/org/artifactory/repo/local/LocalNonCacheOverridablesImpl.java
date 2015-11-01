package org.artifactory.repo.local;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.repo.LocalRepo;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.db.DbService;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregates and polls all the overridable components when needing to determine
 * if a path can be deleted without explicit delete permissions
 *
 * @author Shay Yaakov
 */
@Service
@Reloadable(beanClass = LocalNonCacheOverridables.class, initAfter = DbService.class)
public class LocalNonCacheOverridablesImpl implements LocalNonCacheOverridables, BeanNameAware,
        ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(LocalNonCacheOverridablesImpl.class);

    private ArtifactoryApplicationContext context;
    private Set<LocalNonCacheOverridable> overridable = Sets.newHashSet();
    private String beanName;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = ((ArtifactoryApplicationContext) applicationContext);
    }

    @Override
    public void setBeanName(String name) {
        beanName = name;
    }

    @Override
    public void init() {
        Collection<LocalNonCacheOverridable> allOverridableIncludingMe = context.beansForType(
                LocalNonCacheOverridable.class).values();
        Object thisAsBean = context.getBean(beanName);
        overridable.addAll(allOverridableIncludingMe.stream().filter(
                localNonCacheOverridable -> localNonCacheOverridable != thisAsBean).collect(Collectors.toList()));
        log.debug("Loaded overridable: {}", overridable);
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {

    }

    @Override
    public boolean isOverridable(LocalRepo repo, String path) {
        if (StringUtils.isNotBlank(path)) {
            for (LocalNonCacheOverridable localOverridable : overridable) {
                if (localOverridable.isOverridable(repo, path)) {
                    return true;
                }
            }
        }

        return false;
    }
}
