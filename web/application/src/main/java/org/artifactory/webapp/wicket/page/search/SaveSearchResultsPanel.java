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

package org.artifactory.webapp.wicket.page.search;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.artifactory.addon.wicket.AddonType;
import org.artifactory.addon.wicket.disabledaddon.DisabledAddonBehavior;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.ajax.NoAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.tooltip.TooltipBehavior;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.combobox.ComboBox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.BaseTitledLink;
import org.artifactory.common.wicket.component.panel.fieldset.FieldSetPanel;
import org.artifactory.common.wicket.panel.defaultsubmit.DefaultSubmit;
import org.artifactory.common.wicket.util.SetEnableVisitor;
import org.artifactory.webapp.wicket.application.ArtifactoryWebSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This panel is displayed after search if there are results and allows the user to save the results to a temp "session
 * repository".
 *
 * @author Yossi Shaul
 */
public class SaveSearchResultsPanel extends FieldSetPanel {
    @SpringBean
    protected AuthorizationService authorizationService;

    @WicketProperty
    protected String resultName;

    @WicketProperty
    protected boolean completeVersion = true;

    private Model<String> messageModel;

    public SaveSearchResultsPanel(String id, IModel model) {
        super(id, model);

        setOutputMarkupId(true);
        add(new CssClass(new PropertyModel(this, "cssClass")));

        addSaveResultsForm();
        updateState();
    }

    private void addSaveResultsForm() {
        Form form = new Form("saveResultsForm");
        messageModel = new Model<String>();
        form.add(new TooltipBehavior(messageModel));
        add(form);

        form.add(new HelpBubble("help",
                "The name of the search result to use.\n" +
                        "By saving and assembling named search results, you can perform bulk artifact operations."));

        form.add(newResultCombo("resultName"));

        Component saveResultsLink = createSaveResultsLink("saveResultsLink", "Save");
        form.add(saveResultsLink);

        Component addResultsLink = createAddResultsLink("addResultsLink", "Add");
        form.add(addResultsLink);

        form.add(createSubtractResultsLink("subtractResultsLink", "Subtract"));
        form.add(createIntersectResultsLink("intersectResultsLink", "Intersect"));

        StyledCheckbox completeVersionCheckbox =
                new StyledCheckbox("completeVersion", new PropertyModel<Boolean>(this, "completeVersion"));
        completeVersionCheckbox.setDefaultModelObject(Boolean.FALSE);
        form.add(completeVersionCheckbox);
        form.add(new HelpBubble("completeVersion.help",
                "For every artifact, aggregate all artifacts belonging to the same artifact version (and group) \n" +
                        "under the saved search result, even if not directly found in the current search."));

        form.add(new DefaultSubmit("defaultSubmit", saveResultsLink, addResultsLink));

        postInit();
    }

    protected Component newResultCombo(String id) {
        return new ComboBox(id, new Model<String>(""), Collections.<String>emptyList());
    }

    protected void postInit() {
        setAllEnable(false);
        add(new DisabledAddonBehavior(AddonType.SEARCH));
    }

    public List<String> getSearchNameChoices() {
        Set<String> resultNames = ArtifactoryWebSession.get().getResultNames();
        return new ArrayList<String>(resultNames);
    }

    protected Component createSubtractResultsLink(String id, String title) {
        return createDummyLink(id, title);
    }

    protected Component createAddResultsLink(String id, String title) {
        return createDummyLink(id, title);
    }

    protected Component createSaveResultsLink(String id, String title) {
        return createDummyLink(id, title);
    }

    protected Component createIntersectResultsLink(String id, String title) {
        return createDummyLink(id, title);
    }

    private Component createDummyLink(final String id, final String title) {
        return new BaseTitledLink(id, title).setEnabled(false);
    }

    public void updateState() {
        // nothing here (dummy panel)
    }

    public void setTooltipMessage(String message) {
        messageModel.setObject(message);
    }

    public String getCssClass() {
        return isEnabled() ? "save-results" : "save-results disabled";
    }

    public void setAllEnable(final boolean enabled) {
        setEnabled(enabled);
        visitChildren(new SetEnableVisitor(enabled));
    }

    public class UpdateStateBehavior extends AjaxFormComponentUpdatingBehavior {
        public UpdateStateBehavior(String event) {
            super(event);
            setThrottleDelay(Duration.seconds(0.4));
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            updateState();
            MarkupContainer form = (MarkupContainer) get("saveResultsForm");
            Component saveResultsLink = form.get("saveResultsLink");
            Component addResultsLink = form.get("addResultsLink");
            Component subtractResultsLink = form.get("subtractResultsLink");
            Component intersectResultsLink = form.get("intersectResultsLink");
            target.addComponent(addResultsLink);
            target.addComponent(subtractResultsLink);
            target.addComponent(saveResultsLink);
            target.addComponent(intersectResultsLink);
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new NoAjaxIndicatorDecorator();
        }
    }
}
