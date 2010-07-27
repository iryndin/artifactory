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

package org.artifactory.webapp.wicket.page.build.tabs;

import com.google.common.collect.Lists;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.Addon;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonBehavior;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.NopFormComponentUpdatingBehavior;
import org.artifactory.common.wicket.behavior.tooltip.TooltipBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.combobox.ComboBox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.BaseTitledLink;
import org.artifactory.common.wicket.component.panel.fieldset.FieldSetPanel;
import org.artifactory.common.wicket.model.SelectedItemModel;
import org.artifactory.common.wicket.panel.defaultsubmit.DefaultSubmit;
import org.artifactory.common.wicket.util.SetEnableVisitor;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;
import org.jfrog.build.api.Build;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Displays the default save search result interface for the build
 *
 * @author Noam Y. Tenne
 */
public class BuildSearchResultsPanel extends FieldSetPanel {

    @SpringBean
    protected AddonsManager addonsManager;

    @SpringBean
    protected BuildService buildService;

    @SpringBean
    protected AuthorizationService authorizationService;

    @WicketProperty
    protected boolean artifacts = true;

    @WicketProperty
    protected boolean dependencies = true;

    protected Model messageModel;
    protected Build build;
    private String currentResultName;

    protected Set<String> scopes;
    protected Set<String> selectedScopes;

    /**
     * Main constructor
     *
     * @param requestingAddon The addon that requests the panel
     * @param build           Build to use as file result
     */
    public BuildSearchResultsPanel(Addon requestingAddon, Build build) {
        super("saveSearchResultsPanel");
        this.build = build;
        currentResultName =
                new StringBuilder().append(build.getName()).append("-").append(build.getNumber()).toString();
        messageModel = new Model();
        scopes = buildService.findScopes(build);
        selectedScopes = new HashSet<String>(scopes);

        setOutputMarkupId(true);
        add(new CssClass("build-save-results"));

        addSaveResultsForm(requestingAddon);
        updateState();
    }

    /**
     * Returns a list of the search name choices
     *
     * @return Search name choice list
     */
    public List<String> getSearchNameChoices() {
        Set<String> resultNames = ArtifactoryWebSession.get().getResultNames();

        List<String> resultNamesToReturn = Lists.newArrayList();
        resultNamesToReturn.addAll(resultNames);

        if (!resultNames.contains(currentResultName)) {
            resultNamesToReturn.add(currentResultName);
        }
        Collections.swap(resultNamesToReturn, resultNamesToReturn.indexOf(currentResultName), 0);

        return resultNamesToReturn;
    }

    @Override
    public String getTitle() {
        return "Save to Search Results";
    }

    /**
     * Adds the save search results form the panel
     *
     * @param requestingAddon The addon that requests the panel
     */
    private void addSaveResultsForm(Addon requestingAddon) {
        Form form = new Form("saveResultsForm");
        add(form);

        form.add(new TooltipBehavior(messageModel));
        add(form);

        form.add(new HelpBubble("help", "The name of the search result to use.\nBy saving and assembling named " +
                "search results, you can perform bulk artifact operations."));

        form.add(getResultComboBox("resultName"));

        Component saveResultsLink = getSaveResultsLink("saveResultsLink", "Save");
        form.add(saveResultsLink);

        Component addResultsLink = getAddResultsLink("addResultsLink", "Add");
        form.add(addResultsLink);

        form.add(getSubtractResultsLink("subtractResultsLink", "Subtract"));


        final MarkupContainer scopesContainer = createScopesContainer();
        form.add(scopesContainer);

        final StyledCheckbox artifactsCheckbox = addCheckBox(form, "artifacts",
                "If checked, published module artifacts will be saved as search results.", true);
        artifactsCheckbox.add(new NopFormComponentUpdatingBehavior("onclick"));

        final StyledCheckbox dependenciesCheckbox = addCheckBox(form, "dependencies",
                "If checked, published module dependencies will be saved as search results.\nYou can optionally hand-pick the dependency scopes to include.",
                false);
        if (scopes.isEmpty()) {
            dependenciesCheckbox.add(new NopFormComponentUpdatingBehavior("onclick"));
        } else {
            dependenciesCheckbox.setLabel(new Model("Include Dependencies of the following scopes:"));
            dependenciesCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(scopesContainer);
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new NoAjaxIndicatorDecorator();
                }
            });
        }

        form.add(new DefaultSubmit("defaultSubmit", saveResultsLink, addResultsLink));

        postInit(requestingAddon);
    }

    private MarkupContainer createScopesContainer() {
        WebMarkupContainer scopesContainer = new WebMarkupContainer("scopesContainer");
        scopesContainer.setOutputMarkupId(true);
        if (scopes.isEmpty()) {
            scopesContainer.setVisible(false);
            return scopesContainer;
        }

        RepeatingView scopesView = new RepeatingView("scopeCheckbox");
        scopesContainer.add(scopesView);

        for (String scope : scopes) {
            StyledCheckbox checkbox =
                    new StyledCheckbox(scopesView.newChildId(), new SelectedItemModel<String>(selectedScopes, scope)) {
                        @Override
                        public boolean isEnabled() {
                            return dependencies && super.isEnabled();
                        }
                    };
            checkbox.setLabel(new Model(scope));
            scopesView.add(checkbox);
        }
        return scopesContainer;
    }

    /**
     * Returns the result name combo box
     *
     * @param id the ID to assign to the combo box
     * @return Result name combo box
     */
    protected ComboBox getResultComboBox(String id) {
        return new ComboBox(id, new Model(""), Lists.<String>newArrayList());
    }

    /**
     * Executes any actions needed after initializing the panel components
     *
     * @param requestingAddon The addon that requests the panel
     */
    protected void postInit(Addon requestingAddon) {
        setAllEnable(false);
        add(new DisabledAddonBehavior(requestingAddon));
    }

    /**
     * Returns the save results link
     *
     * @param id    The ID to assign to the link
     * @param title The title to assign to the link
     * @return Save results link
     */
    protected Component getSaveResultsLink(String id, String title) {
        return createDummyLink(id, title);
    }

    /**
     * Returns the add results link
     *
     * @param id    The ID to assign to the link
     * @param title The title to assign to the link
     * @return Add results link
     */
    protected Component getAddResultsLink(String id, String title) {
        return createDummyLink(id, title);
    }

    /**
     * Returns the substract results link
     *
     * @param id    The ID to assign to the link
     * @param title The title to assign to the link
     * @return Substract results link
     */
    protected Component getSubtractResultsLink(String id, String title) {
        return createDummyLink(id, title);
    }

    /**
     * Updates the state of the panel according to different conditions
     */
    protected void updateState() {
    }

    /**
     * Sets whether the panel should be enabled or disabled
     *
     * @param enabled True if the panel should be enabled
     */
    protected void setAllEnable(final boolean enabled) {
        setEnabled(enabled);
        visitChildren(new SetEnableVisitor(enabled));
    }

    /**
     * Adds a checkbox to the given form
     *
     * @param form           Form to add the checkbox to
     * @param id             ID to assign to the checkbox
     * @param helpMessage    Help message to display for the checkbox
     * @param checkByDefault Should the checkbox be checked by default
     */
    private StyledCheckbox addCheckBox(Form form, String id, String helpMessage, boolean checkByDefault) {
        StyledCheckbox checkbox = new StyledCheckbox(id, new PropertyModel(this, id));
        checkbox.setModelObject(checkByDefault);
        checkbox.setOutputMarkupId(true);
        form.add(checkbox);
        form.add(new HelpBubble(id + ".help", helpMessage));
        return checkbox;
    }

    /**
     * Creates a dummy disabled link
     *
     * @param id    The ID to assign to the link
     * @param title The title to assign to the link
     * @return Disabled link
     */
    private Component createDummyLink(final String id, String title) {
        return new BaseTitledLink(id, title).setEnabled(false);
    }

}