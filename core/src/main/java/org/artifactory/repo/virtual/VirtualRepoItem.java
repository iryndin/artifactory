package org.artifactory.repo.virtual;

import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrFsItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class VirtualRepoItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(VirtualRepoItem.class);

    private final JcrFsItem item;
    private List<String> repoKeys = new ArrayList<String>();

    public VirtualRepoItem(JcrFsItem item) {
        this.item = item;
    }

    public String getName() {
        return item.getName();
    }

    public String getPath() {
        return item.relPath();
    }

    public boolean isFolder() {
        return item.isFolder();
    }

    public List<String> getRepoKeys() {
        return repoKeys;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VirtualRepoItem item1 = (VirtualRepoItem) o;
        return item.equals(item1.item);

    }

    public int hashCode() {
        return item.hashCode();
    }
}
