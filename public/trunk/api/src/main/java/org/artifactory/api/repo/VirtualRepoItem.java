package org.artifactory.api.repo;

import org.artifactory.api.fs.ItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class VirtualRepoItem {
    private final ItemInfo item;
    private List<String> repoKeys = new ArrayList<String>();

    public VirtualRepoItem(ItemInfo item) {
        this.item = item;
    }

    public String getName() {
        return item.getName();
    }

    public String getPath() {
        return item.getRelPath();
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
