package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.general.trash;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.md.Properties;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Shay Yaakov
 */
public class TrashItemDetails extends BaseModel {

    private String deletedTime;
    private String deletedBy;
    private String originalRepository;
    private String originalRepositoryType;
    private String originalPath;

    public TrashItemDetails() {
    }

    public TrashItemDetails(Properties properties) {
        String deleted = properties.getFirst(TrashService.PROP_TRASH_TIME);
        if (StringUtils.isNotBlank(deleted)) {
            deletedTime = ContextHelper.get().getCentralConfig().format(Long.parseLong(deleted));
        }
        deletedBy = properties.getFirst(TrashService.PROP_DELETED_BY);
        originalRepository = properties.getFirst(TrashService.PROP_ORIGIN_REPO);
        originalRepositoryType = properties.getFirst(TrashService.PROP_ORIGIN_REPO_TYPE);
        originalPath = properties.getFirst(TrashService.PROP_ORIGIN_PATH);
    }

    public String getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(String deletedTime) {
        this.deletedTime = deletedTime;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public String getOriginalRepository() {
        return originalRepository;
    }

    public void setOriginalRepository(String originalRepository) {
        this.originalRepository = originalRepository;
    }

    public String getOriginalRepositoryType() {
        return originalRepositoryType;
    }

    public void setOriginalRepositoryType(String originalRepositoryType) {
        this.originalRepositoryType = originalRepositoryType;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }
}
