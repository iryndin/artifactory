/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.browse.treebrowser;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.persistence.IValuePersister;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.persister.EscapeCookieValuePersister;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ItemActionListener;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;
import org.artifactory.webapp.actionable.model.HierarchicActionableItem;
import org.artifactory.webapp.wicket.actionable.tree.ActionableItemsProvider;
import org.artifactory.webapp.wicket.actionable.tree.ActionableItemsTree;
import org.artifactory.webapp.wicket.actionable.tree.TreeKeyEventHandler;

import java.util.List;

/**
 * @author Yoav Landman
 * @author Yossi Shaul
 */
public abstract class TreeBrowsePanel extends TitledPanel implements ActionableItemsProvider, ItemActionListener {

    /**
     * Wicket container for the tabs panel
     */
    private final WebMarkupContainer nodePanelContainer;

    /**
     * Selected node information will be displayed in this panel (the tabs)
     */
    private Panel nodeDisplayPanel;

    /**
     * A modal window for displaying text  content
     */
    private final ModalHandler textContentViewer;

    /**
     * The tree...
     */
    private final ActionableItemsTree tree;

    @SpringBean
    private AuthorizationService authService;
    private static final IValuePersister COMPACT_PERSISTER = new CompactPersister();

    public TreeBrowsePanel(String id) {
        this(id, null);
    }

    public TreeBrowsePanel(String id, ActionableItem initialItem) {
        super(id);

        Component menuPlaceHolder = new WebMarkupContainer("contextMenu");
        menuPlaceHolder.setOutputMarkupId(true);
        add(menuPlaceHolder);

        nodePanelContainer = new WebMarkupContainer("nodePanelContainer");
        nodePanelContainer.setOutputMarkupId(true);
        add(nodePanelContainer);

        nodeDisplayPanel = new EmptyPanel("nodePanel");
        nodeDisplayPanel.setOutputMarkupId(true);
        nodePanelContainer.add(nodeDisplayPanel);

        textContentViewer = new ModalHandler("contentDialog");
        add(textContentViewer);

        RepoPath repoPath = null;
        if (initialItem instanceof RepoAwareActionableItem) {
            repoPath = ((RepoAwareActionableItem) initialItem).getRepoPath();
        }

        final CompactFoldersCheckbox compactCheckbox = new CompactFoldersCheckbox("compactCheckbox");
        add(compactCheckbox);

        tree = new ActionableItemsTree("tree", this, repoPath, compactCheckbox.isCompactAllowed());
        add(tree);
        add(new TreeKeyEventHandler("keyEventHandler", tree));
    }

    public abstract HierarchicActionableItem getRoot();

    public List<? extends ActionableItem> getChildren(HierarchicActionableItem parent) {
        List<? extends ActionableItem> children = parent.getChildren(authService);
        for (ActionableItem item : children) {
            // Add the tree as a listener (required for remove)
            // Must delegate to tree, because tree might be null at this point
            item.addActionListener(this);

            // Add the event targets
            ItemEventTargetComponents targetComponents =
                    new ItemEventTargetComponents() {
                        @Override
                        public Component getRefreshableComponent() {
                            return tree;
                        }

                        @Override
                        public WebMarkupContainer getNodePanelContainer() {
                            return TreeBrowsePanel.this.nodePanelContainer;
                        }

                        @Override
                        public ModalWindow getModalWindow() {
                            return textContentViewer;
                        }
                    };
            item.setEventTargetComponents(targetComponents);

            // Filter out candidates that are not clearing up
            item.filterActions(authService);
        }
        return children;
    }

    public boolean hasChildren(HierarchicActionableItem parent) {
        return parent.hasChildren(authService);
    }

    public Panel getItemDisplayPanel() {
        return nodeDisplayPanel;
    }

    public void setItemDisplayPanel(Panel panel) {
        nodeDisplayPanel = panel;
    }

    public void actionPerformed(ItemEvent e) {
        tree.actionPerformed(e);
    }

    public ActionableItemsTree getTree() {
        return tree;
    }

    /**
     * Replaces the node display panel with an empty panel.
     *
     * @param target The ajax target to use for refreshing the component
     */
    public void removeNodePanel(AjaxRequestTarget target) {
        Panel dummyPanel = new EmptyPanel("nodePanel");
        dummyPanel.setOutputMarkupId(true);
        setItemDisplayPanel(dummyPanel);
        nodePanelContainer.replace(dummyPanel);
        target.addComponent(dummyPanel);
    }

    /**
     * StyledCheckbox with persist load on c'tor.
     */
    private class CompactFoldersCheckbox extends StyledCheckbox {
        private CompactFoldersCheckbox(String id) {
            super(id, new Model(false));

            setPersistent(true);
            COMPACT_PERSISTER.load(this);
            add(new AjaxFormComponentUpdatingBehavior("onclick") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    COMPACT_PERSISTER.save(CompactFoldersCheckbox.this);
                    tree.setCompactAllowed(isCompactAllowed());
                    target.addComponent(tree);
                    target.addComponent(tree.refreshDisplayPanel());
                    tree.adjustLayout(target);
                }
            });

        }

        public Boolean isCompactAllowed() {
            return (Boolean) getModelObject();
        }
    }

    private static class CompactPersister extends EscapeCookieValuePersister {
        @Override
        protected String getName(FormComponent component) {
            return "browseRepoPanel.compactCheckbox";
        }
    }
}