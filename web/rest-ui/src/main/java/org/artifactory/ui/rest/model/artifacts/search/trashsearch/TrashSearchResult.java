package org.artifactory.ui.rest.model.artifacts.search.trashsearch;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.ItemSearchResult;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.md.Properties;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.ui.rest.model.artifacts.search.BaseSearchResult;
import org.artifactory.util.PathUtils;
import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shay Yaakov
 */
@JsonTypeName("trash")
public class TrashSearchResult extends BaseSearchResult {

    private String originRepository;
    private long deletedTime;
    private String deletedTimeString;
    private String relativeDirPath;

    public TrashSearchResult() {
        // for jackson
    }

    public TrashSearchResult(ArtifactSearchResult artifactSearchResult, Properties properties) {
        super.setRepoKey(artifactSearchResult.getRepoKey());
        super.setName(artifactSearchResult.getName());
        this.repoPath = artifactSearchResult.getItemInfo().getRepoPath();
        String relDirPath = PathUtils.stripFirstPathElement(artifactSearchResult.getRelDirPath());
        if (StringUtils.isBlank(relDirPath)) {
            relDirPath = "[root]";
        }
        this.relativeDirPath = relDirPath;
        this.originRepository = properties.getFirst(TrashService.PROP_ORIGIN_REPO);
        String trashTime = properties.getFirst(TrashService.PROP_TRASH_TIME);
        if (StringUtils.isNotBlank(trashTime)) {
            this.deletedTime = Long.parseLong(trashTime);
            this.deletedTimeString = ContextHelper.get().getCentralConfig().format(deletedTime);
        }
        List<String> actions = new ArrayList<>();
        actions.add("ShowInTree");
        actions.add("Restore");
        actions.add("Delete");
        setActions(actions);
    }

    public String getOriginRepository() {
        return originRepository;
    }

    public void setOriginRepository(String originRepository) {
        this.originRepository = originRepository;
    }

    public long getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(long deletedTime) {
        this.deletedTime = deletedTime;
    }

    public String getDeletedTimeString() {
        return deletedTimeString;
    }

    public void setDeletedTimeString(String deletedTimeString) {
        this.deletedTimeString = deletedTimeString;
    }

    public String getRelativeDirPath() {
        return relativeDirPath;
    }

    public void setRelativeDirPath(String relativeDirPath) {
        this.relativeDirPath = relativeDirPath;
    }

    @Override
    protected void updateActions() {
        List<String> actions = new ArrayList<>();
        actions.add("ShowInTree");
        actions.add("Restore");
        actions.add("Delete");
        setActions(actions);
    }

    @Override
    public ItemSearchResult getSearchResult() {
        return null;
    }
}
