package org.artifactory.webapp.wicket.page.search;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.search.SearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.DownloadAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.ShowInTreeAction;
import org.artifactory.webapp.actionable.action.ViewAction;
import org.artifactory.webapp.wicket.utils.CssClass;

import java.util.List;
import java.util.Set;

/**
 * @author Yossi Shaul
 */
public class ActionableSearchResult extends RepoAwareActionableItemBase {
    private SearchResult searchResult;
    private ViewAction viewAction;

    public ActionableSearchResult(SearchResult searchResult) {
        super(searchResult.getFileInfo());
        this.searchResult = searchResult;
        Set<ItemAction> actions = getActions();
        viewAction = new ViewAction();
        actions.add(viewAction);
        actions.add(new DownloadAction());
        actions.add(new ShowInTreeAction());
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }

    public Panel newItemDetailsPanel(String id) {
        throw new UnsupportedOperationException("method not allowed on search result");
    }

    @Override
    public void addTabs(List<ITab> tabs) {
        throw new UnsupportedOperationException("method not allowed on search result");
    }

    public String getDisplayName() {
        return getItemInfo().getName();
    }

    public String getCssClass() {
        return CssClass.getFileCssClass(getItemInfo().getRelPath()).cssClass();
    }

    public void filterActions(AuthorizationService authService) {
        if (!isPomFile()) {
            viewAction.setEnabled(false);
        }
    }

    private boolean isPomFile() {
        return searchResult.getFileInfo().getName().endsWith(".pom");
    }
}
