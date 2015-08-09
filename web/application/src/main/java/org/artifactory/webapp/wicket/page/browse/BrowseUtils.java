package org.artifactory.webapp.wicket.page.browse;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.artifactory.api.repo.BaseBrowsableItem;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author Yoav Luft
 */
public class BrowseUtils {
    /**
     * Creates a new list of items without items with the suffixes "sha1" or "md5"
     *
     * @param items
     * @return
     */
    public static List<BaseBrowsableItem> filterChecksums(Collection<? extends BaseBrowsableItem> items) {
        return Lists.newLinkedList(Iterables.filter(items,
                new Predicate<BaseBrowsableItem>() {
                    @Override
                    public boolean apply(@Nullable BaseBrowsableItem input) {
                        if (input == null || input.getName() == null
                                || input.getName().endsWith(".sha1")
                                || input.getName().endsWith(".md5")) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                }));
    }
}
