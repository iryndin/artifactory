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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.maven;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.common.wicket.component.label.highlighter.SyntaxHighlighter;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.actionable.tree.ActionableItemsTree;
import org.artifactory.webapp.wicket.page.browse.treebrowser.TreeBrowsePanel;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author Noam Tenne
 */
public class MetadataTabPanel extends Panel {

    private static final Logger log = LoggerFactory.getLogger(MetadataTabPanel.class);

    @SpringBean
    private RepositoryService repoService;

    @SpringBean
    private AuthorizationService authorizationService;

    private SyntaxHighlighter contentPanel = new SyntaxHighlighter("metadataContent").setSyntax(Syntax.XML);

    private RepoPath repoPath;

    @WicketProperty
    private String metadataType;

    public MetadataTabPanel(String id, RepoPath repoPath, List<String> metadataTypeList) {
        super(id);
        this.repoPath = repoPath;
        addComponents(metadataTypeList);
    }

    private void addComponents(List<String> metadataTypeList) {
        setOutputMarkupId(true);
        Form form = new Form("form");

        LabeledValue labeledValue = new LabeledValue("selectLabel", "Metadata Name:");
        form.add(labeledValue);
        final IModel metadataTypeModel = new PropertyModel(this, "metadataType");
        final DropDownChoice metadataTypesDropDown =
                new DropDownChoice("metadataTypes", metadataTypeModel, metadataTypeList);
        metadataTypesDropDown.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                setContent();
                target.addComponent(contentPanel);
            }
        });
        metadataTypesDropDown.setModelObject(metadataTypeList.get(0));
        form.add(metadataTypesDropDown);

        TitledAjaxSubmitLink removeButton = new TitledAjaxSubmitLink("remove", "Remove", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                RepoPath metadataRepoPath =
                        new RepoPath(repoPath.getRepoKey(), repoPath.getPath() + ":" + metadataType);
                repoService.undeploy(metadataRepoPath);
                MarkupContainer browsePanel = findParent(TreeBrowsePanel.class);
                ActionableItemsTree tree = (ActionableItemsTree) browsePanel.get("tree");
                target.addComponent(tree.refreshDisplayPanel());
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                String message = "Are you sure you wish to delete " + metadataType + " ?";
                return new ConfirmationAjaxCallDecorator(message);
            }
        };
        removeButton.setVisible(authorizationService.canAnnotate(repoPath));
        form.add(removeButton);
        add(form);

        FieldSetBorder border = new FieldSetBorder("metadataBorder");
        add(border);
        contentPanel.setOutputMarkupId(true);
        border.add(contentPanel);
        setContent();
    }

    private void setContent() {
        if (metadataType != null) {
            try {
                String xmlContent = repoService.getXmlMetadata(repoPath, metadataType);
                contentPanel.setModel(new Model(xmlContent));
            } catch (RepositoryRuntimeException rre) {
                contentPanel.setModel(new Model("Error while retrieving selected metadata. Please review the log " +
                        "for further details."));
                log.error("Error while retrieving selected metadata '{}': {}", metadataType, rre.getMessage());
                log.debug("Error while retrieving selected metadata '" + metadataType + "'.", rre);
            }
        }
    }
}
