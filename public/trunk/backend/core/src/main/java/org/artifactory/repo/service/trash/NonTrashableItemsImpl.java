package org.artifactory.repo.service.trash;

import com.google.common.collect.Sets;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
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
 * Aggregates and polls all the items when needing to determine if a path can be sent to trash
 *
 * @author Shay Yaakov
 */
@Service
@Reloadable(beanClass = NonTrashableItems.class, initAfter = DbService.class)
public class NonTrashableItemsImpl implements NonTrashableItems, BeanNameAware, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(NonTrashableItemsImpl.class);

    private ArtifactoryApplicationContext context;
    private Set<NonTrashableItem> trashables = Sets.newHashSet();
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
        Collection<NonTrashableItem> allTrashablesIncludingMe = context.beansForType(NonTrashableItem.class).values();
        Object thisAsBean = context.getBean(beanName);
        trashables.addAll(allTrashablesIncludingMe.stream()
                .filter(nonTrashableItem -> nonTrashableItem != thisAsBean).collect(Collectors.toList()));
        log.debug("Loaded trashables: {}", trashables);
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
    public boolean skipTrash(LocalRepoDescriptor descriptor, String path) {
        for (NonTrashableItem nonTrashableItem : trashables) {
            if (nonTrashableItem.skipTrash(descriptor, path)) {
                return true;
            }
        }

        return false;
    }
}
