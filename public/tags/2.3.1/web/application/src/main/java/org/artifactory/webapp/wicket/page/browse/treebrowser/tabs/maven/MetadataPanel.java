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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepoPathImpl;
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
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.wicket.actionable.tree.ActionableItemsTree;
import org.artifactory.webapp.wicket.page.browse.treebrowser.TreeBrowsePanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/**
 * Displays the metadata that's tagging the currently selected tree item
 *
 * @author Noam Y. Tenne
 */
public class MetadataPanel extends TitledPanel {

    private static final Logger log = LoggerFactory.getLogger(MetadataPanel.class);

    @SpringBean
    private RepositoryService repoService;

    @SpringBean
    private AuthorizationService authorizationService;

    @WicketProperty
    private String metadataType;

    private FieldSetBorder metadataContentBorder;
    private RepoPath repoPath;
    public static final String SELECT_METADATA_PARAM = "selectMetadata";

    /**
     * Main constructor
     *
     * @param id                ID of the panel to construct
     * @param canonicalRepoPath Cannonical repo path of currently selected tree item
     * @param metadataTypeList  Name list of metadata that's tagging the selected item
     */
    public MetadataPanel(String id, final RepoPath canonicalRepoPath, List<String> metadataTypeList) {
        super(id);
        this.repoPath = canonicalRepoPath;
        setOutputMarkupId(true);

        Form metadataSelectorForm = new Form("metadataSelectorForm");

        LabeledValue metadataNameLabeledValue = new LabeledValue("selectLabel", "Metadata Name:");
        metadataSelectorForm.add(metadataNameLabeledValue);
        final IModel<String> metadataTypeModel = new PropertyModel<String>(this, "metadataType");

        final DropDownChoice<String> metadataTypesDropDown =
                new DropDownChoice<String>("metadataTypes", metadataTypeModel, metadataTypeList);
        metadataTypesDropDown.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                setMetadataContent();
                target.addComponent(metadataContentBorder);
            }
        });
        autoSelectMetadataType(metadataTypeList, metadataTypesDropDown);

        metadataSelectorForm.add(metadataTypesDropDown);

        TitledAjaxSubmitLink removeButton = new TitledAjaxSubmitLink("remove", "Remove", metadataSelectorForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                RepoPath metadataRepoPath =
                        new RepoPathImpl(canonicalRepoPath.getRepoKey(),
                                canonicalRepoPath.getPath() + ":" + metadataType);
                repoService.undeploy(metadataRepoPath);
                MarkupContainer browsePanel = findParent(TreeBrowsePanel.class);
                ActionableItemsTree tree = (ActionableItemsTree) browsePanel.get("tree");
                target.addComponent(tree.refreshDisplayPanel());
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new ConfirmationAjaxCallDecorator("Are you sure you wish to delete the selected metadata?");
            }
        };
        removeButton.setVisible(authorizationService.canAnnotate(canonicalRepoPath));
        metadataSelectorForm.add(removeButton);

        metadataContentBorder = new FieldSetBorder("metadataContentBorder");
        metadataContentBorder.setOutputMarkupId(true);
        metadataSelectorForm.add(metadataContentBorder);

        metadataContentBorder.add((Component) new SyntaxHighlighter("metadataContent").setSyntax(Syntax.xml));
        setMetadataContent();

        add(metadataSelectorForm);
    }

    @Override
    public String getTitle() {
        return "XML Metadata";
    }

    private void setMetadataContent() {
        if (metadataType != null) {
            try {
                String xmlContent = repoService.getXmlMetadata(repoPath, metadataType);
                metadataContentBorder.replace(
                        WicketUtils.getSyntaxHighlighter("metadataContent", xmlContent, Syntax.xml));
            } catch (RepositoryRuntimeException rre) {
                metadataContentBorder.replace(WicketUtils.getSyntaxHighlighter("metadataContent",
                        "Error while retrieving selected metadata. Please review the log for further details.",
                        Syntax.plain));
                log.error("Error while retrieving selected metadata '{}': {}", metadataType, rre.getMessage());
                log.debug("Error while retrieving selected metadata '" + metadataType + "'.", rre);
            }
        }
    }

    private void autoSelectMetadataType(List<String> metadataTypeList, DropDownChoice metadataTypesDropDown) {
        if (!metadataTypeList.isEmpty()) {
            WebRequest request = (WebRequest) RequestCycle.get().getRequest();
            String selectMetadata = request.getParameter(SELECT_METADATA_PARAM);

            Object defaultSelection = metadataTypeList.get(0);
            if (StringUtils.isNotBlank(selectMetadata)) {
                try {
                    String decodedSelectedMetadata = URLDecoder.decode(selectMetadata, "utf-8");
                    for (String metadataType : metadataTypeList) {
                        if (decodedSelectedMetadata.equals(metadataType)) {
                            defaultSelection = metadataType;
                            break;
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    log.warn("Unable to decode auto selected metadata name '" + selectMetadata + "'.", e);
                }
            }
            metadataTypesDropDown.setDefaultModelObject(defaultSelection);
        }
    }
}
