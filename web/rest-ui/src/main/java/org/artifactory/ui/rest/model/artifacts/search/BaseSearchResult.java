package org.artifactory.ui.rest.model.artifacts.search;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.ItemSearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.ItemInfo;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.ui.rest.model.artifacts.search.classsearch.ClassSearchResult;
import org.artifactory.ui.rest.model.artifacts.search.gavcsearch.GavcResult;
import org.artifactory.ui.rest.model.artifacts.search.packagesearch.result.PackageSearchResult;
import org.artifactory.ui.rest.model.artifacts.search.propertysearch.PropertyResult;
import org.artifactory.ui.rest.model.artifacts.search.quicksearch.QuickSearchResult;
import org.artifactory.util.PathUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen Keinan
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = ClassSearchResult.class, name = "class"),
        @JsonSubTypes.Type(value = GavcResult.class, name = "gavc"),
        @JsonSubTypes.Type(value = PropertyResult.class, name = "property"),
        @JsonSubTypes.Type(value = StashResult.class, name = "stash"),
        @JsonSubTypes.Type(value = QuickSearchResult.class, name = "quick"),
        @JsonSubTypes.Type(value = PackageSearchResult.class, name = "package")})
@JsonIgnoreProperties("searchResult")
public abstract class BaseSearchResult extends BaseModel {

    private String repoKey;
    private String name;
    protected RepoPath repoPath;
    private long modifiedDate;
    private String modifiedString;
    private List<String> actions;
    private String downloadLink;

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getModifiedString() {
        return modifiedString;
    }

    public void setModifiedString(String modifiedString) {
        this.modifiedString = modifiedString;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    protected void updateActions() {
        AuthorizationService authorizationService = ContextHelper.get().getAuthorizationService();
        List<String> actions = new ArrayList<>();
        if (NamingUtils.isViewable(getName()) || "class".equals(PathUtils.getExtension(getName()))) {
            actions.add("View");
        }
        if(StringUtils.isNotEmpty(downloadLink)) {
            actions.add("Download");
        }
        actions.add("ShowInTree");
        if (authorizationService.canDelete(repoPath)) {
            actions.add("Delete");
        }
        setActions(actions);
    }

    public abstract ItemSearchResult getSearchResult();

    /**
     * return item info by repo key and path
     *
     * @return
     */
    protected ItemInfo getItemInfo(RepoPath repoPath) {
        ItemInfo itemInfo;
        if (repoPath.isFile()) {
            itemInfo = InfoFactoryHolder.get().createFileInfo(repoPath);
        } else {
            itemInfo = InfoFactoryHolder.get().createFolderInfo(repoPath);
        }
        return itemInfo;
    }
}
