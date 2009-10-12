package org.artifactory.webapp.wicket.common.component.table.columns;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.RepoAwareItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.links.TitledAjaxLink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Yoav Aharoni
 */
public class ActionsColumn<T extends ActionableItem> extends LinksColumn<T> {
    @SpringBean
    private AuthorizationService authorizationService;

    {
        InjectorHolder.getInjector().inject(this);
    }

    public ActionsColumn(String title) {
        super(title);
    }

    public ActionsColumn(IModel titleModel) {
        super(titleModel);
    }

    protected Collection<ItemAction> getActions(T actionableItem) {
        Set<ItemAction> actions = actionableItem.getActions();
        actionableItem.filterActions(authorizationService);
        return actions;
    }

    protected ItemEvent newEvent(AjaxRequestTarget target, T actionableItem, ItemAction action) {
        if (actionableItem instanceof RepoAwareActionableItem) {
            return new RepoAwareItemEvent((RepoAwareActionableItem) actionableItem, (RepoAwareItemAction) action, target);
        }
        return new ItemEvent(actionableItem, action, target);
    }

    @Override
    protected Collection<? extends AbstractLink> getLinks(final T actionableItem, String linkId) {
        List<TitledAjaxLink> links = new ArrayList<TitledAjaxLink>();
        for (final ItemAction action : getActions(actionableItem)) {
            if (action.isEnabled()) {
                TitledAjaxLink link = new TitledAjaxLink(linkId, new Model(action.getName())) {
                    public void onClick(AjaxRequestTarget target) {
                        action.actionPerformed(newEvent(target, actionableItem, action));
                    }
                };
                link.add(new CssClass("icon-link"));
                link.add(new CssClass(action.getCssClass()));
                links.add(link);
            }
        }
        return links;
    }
}
