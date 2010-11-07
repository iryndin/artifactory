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

package org.artifactory.webapp.wicket.page.home.settings;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;

import java.util.Map;

/**
 * A base settings generator panel for Ivy and Gradle since they share similar fields
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseIvySettingsGeneratorPanel extends BaseSettingsGeneratorPanel {

    @SpringBean
    protected SecurityService securityService;

    @SpringBean
    protected UserGroupService userGroupService;

    @WicketProperty
    private String artifactPattern;

    @WicketProperty
    private String ivyPattern;

    @WicketProperty
    private boolean m2Compatible = true;

    /**
     * Main constructor
     *
     * @param id                ID to assign to the panel
     * @param servletContextUrl Running context URL
     * @param virtualRepoKeyMap Virtual repo key association map
     */
    protected BaseIvySettingsGeneratorPanel(String id, String servletContextUrl,
            Map<String, String> virtualRepoKeyMap) {
        super(id, servletContextUrl, virtualRepoKeyMap);

        TitledBorder border = new TitledBorder("settingsBorder");

        final TextField<String> artifactPatternTextField = addTextField("artifactPattern", this,
                "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]", false, true);

        final TextField<String> ivyPatternTextField = addTextField("ivyPattern", this,
                "[organisation]/[module]/[revision]/ivy.xml", false, true);

        StyledCheckbox m2CompatibleCheckBox = new StyledCheckbox("m2Compatible",
                new PropertyModel<Boolean>(this, "m2Compatible"));
        m2CompatibleCheckBox.add(new AjaxFormComponentUpdatingBehavior("onclick") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                artifactPatternTextField.setEnabled(!isM2Compatible());
                ivyPatternTextField.setEnabled(!isM2Compatible());
                target.addComponent(artifactPatternTextField);
                target.addComponent(ivyPatternTextField);
            }
        });
        form.add(m2CompatibleCheckBox);
        form.add(new HelpBubble("m2Compatible.help", new ResourceModel("m2Compatible.help")));

        TitledAjaxSubmitLink generateButton = getGenerateButton(servletContextUrl);

        form.add(new DefaultButtonBehavior(generateButton));
        border.add(form);
        add(border);
        add(generateButton);
    }

    public boolean isM2Compatible() {
        return m2Compatible;
    }

    public void setM2Compatible(boolean m2Compatible) {
        this.m2Compatible = m2Compatible;
    }

    public String getArtifactPattern() {
        return artifactPattern;
    }

    public void setArtifactPattern(String artifactPattern) {
        this.artifactPattern = artifactPattern;
    }

    public String getIvyPattern() {
        return ivyPattern;
    }

    public void setIvyPattern(String ivyPattern) {
        this.ivyPattern = ivyPattern;
    }

    /**
     * Returns the complete URL of the selected repository
     *
     * @param repoKey Target repo key
     * @return Repository URL
     */
    protected String getFullRepositoryUrl(String repoKey) {
        return getFullUrl(servletContextUrl, repoKey);
    }

    /**
     * Gets the full URL of the selected repository including the value of the artifact pattern field
     *
     * @param repoKey Target repo key
     * @return Artifact pattern backed by full repository URL
     */
    protected String getFullArtifactPattern(String repoKey) {
        return getFullUrl(getFullRepositoryUrl(repoKey), getArtifactPattern());
    }

    /**
     * Gets the full URL of the selected repository including the value of the ivy pattern field
     *
     * @param repoKey Target repo key
     * @return Ivy pattern backed by full repository URL
     */
    protected String getFullIvyPattern(String repoKey) {
        return getFullUrl(getFullRepositoryUrl(repoKey), getIvyPattern());
    }

    /**
     * Constructs a URL from the given context and path
     *
     * @param servletContextUrl Base context
     * @param path              Path to append to the context
     * @return Full URL
     */
    private String getFullUrl(String servletContextUrl, String path) {
        return new StringBuilder(servletContextUrl).append("/").append(path).toString();
    }

    /**
     * Creates a required, updatable textfield and adds it to the form
     *
     * @param id                  Textfield ID
     * @param propertyModelObject Host object of the model property to create for the field
     * @param defaultValue        Starting field value
     * @param enabled             True if the field should be initially enabled
     * @param required            True if the field should be required
     * @return Created instance
     */
    protected TextField<String> addTextField(String id, Object propertyModelObject, String defaultValue,
            boolean enabled, boolean required) {
        TextField<String> textField = new TextField<String>(id, new PropertyModel<String>(propertyModelObject, id));
        textField.setDefaultModelObject(defaultValue);
        textField.setRequired(required);
        textField.setOutputMarkupId(true);
        textField.setEnabled(enabled);
        form.add(textField);
        form.add(new HelpBubble(id + ".help", new ResourceModel(id + ".help")));
        return textField;
    }

    /**
     * Creates and adds a virtual repo selection drop down to the form
     *
     * @param id                      DropDown ID
     * @param propertyModelObject     Host object of the model property to create for the field
     * @param defaultSelectionKeyword Keyword to look for when auto-selecting a repo from the list
     */
    protected void addRepoDropDown(String id, Object propertyModelObject, String defaultSelectionKeyword) {
        @SuppressWarnings({"unchecked"})
        DropDownChoice choice = new DropDownChoice(id, new PropertyModel<VirtualRepoEntry>(propertyModelObject, id),
                virtualRepoEntries);

        choice.setDefaultModelObject(getDefaultSelection(defaultSelectionKeyword));
        choice.setRequired(true);
        form.add(choice);
        form.add(new HelpBubble(id + ".help", new ResourceModel(id + ".help")));
    }

    private VirtualRepoEntry getDefaultSelection(String defaultSelectionKeyword) {
        for (VirtualRepoEntry virtualRepoEntry : virtualRepoEntries) {
            if (virtualRepoEntry.getRepoKey().contains(defaultSelectionKeyword)) {
                return virtualRepoEntry;
            }
        }

        return virtualRepoEntries.get(0);
    }
}
