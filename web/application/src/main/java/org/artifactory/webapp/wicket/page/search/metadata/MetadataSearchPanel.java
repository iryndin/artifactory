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

package org.artifactory.webapp.wicket.page.search.metadata;

import com.google.common.collect.Lists;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.api.search.xml.metadata.MetadataSearchResult;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.combobox.HistoryComboBox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.util.ComponentUtils;
import org.artifactory.common.wicket.util.CookieUtils;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.search.BaseSearchPage;
import org.artifactory.webapp.wicket.page.search.BaseSearchPanel;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableArtifactSearchResult;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableMetadataSearchResult;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableSearchResult;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the metadata searcher
 *
 * @author Noam Tenne
 */
public class MetadataSearchPanel<T extends MetadataSearchResult> extends BaseSearchPanel<T> {

    private MetadataSearchControls<MetadataSearchResult> searchControls;
    private StyledCheckbox metaDataSearchCheckBox;
    private List<String> xmlTypes;
    private List<String> metaDataNames;
    private HistoryComboBox typesChoices;

    public MetadataSearchPanel(final Page parent, String id) {
        super(parent, id);
    }

    @Override
    public String getSearchExpression() {
        return searchControls.getValue();
    }

    @Override
    protected void addSearchComponents(Form form) {
        add(new CssClass("metadata-panel"));
        searchControls = new MetadataSearchControls<MetadataSearchResult>();
        xmlTypes = Lists.newArrayList("*.pom", "*ivy*.xml");
        searchControls.setMetadataName(xmlTypes.get(0));
        typesChoices = new HistoryComboBox("metadataName", new
                PropertyModel<String>(searchControls, "metadataName"),
                new PropertyModel<List<String>>(this, "metaDataNames"));
        typesChoices.setPersistent(true);
        typesChoices.setRequired(true);
        typesChoices.setOutputMarkupId(true);
        form.add(typesChoices);
        metaDataSearchCheckBox = new StyledCheckbox("metaDataSearch", new Model<Boolean>());
        metaDataSearchCheckBox.setLabel(Model.of("Metadata Search"));
        metaDataSearchCheckBox.setPersistent(true);
        form.add(metaDataSearchCheckBox);
        metaDataSearchCheckBox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (metaDataSearchCheckBox.isChecked()) {
                    metaDataNames = Lists.newArrayList();
                } else {
                    metaDataNames = xmlTypes;
                }
                target.addComponent(metaDataSearchCheckBox.getParent());
            }
        });

        adjustForMetadataLastSearch();
        form.add(new HelpBubble("xmlName.help", new ResourceModel("xmlName.help")));
        form.add(new HelpBubble("metadataName.help", new ResourceModel("metadataName.help")));

        TextArea xPathTextArea = new TextArea<String>("xPathTextArea",
                new PropertyModel<String>(searchControls, "path"));
        form.add(xPathTextArea);
        xPathTextArea.setRequired(true);
        xPathTextArea.setOutputMarkupId(true);
        xPathTextArea.setPersistent(true);
        form.add(new HelpBubble("xpath.help", new ResourceModel("xpath.help")));

        TextField metadataValueField = new TextField<String>("metadataValueField",
                new PropertyModel<String>(searchControls, "value"));
        form.add(metadataValueField);
        metadataValueField.setOutputMarkupId(true);
        metadataValueField.setPersistent(true);
        form.add(new HelpBubble("metadataValue.help", new ResourceModel("metadataValue.help")));
    }

    @Override
    protected MetadataSearchControls getSearchControls() {
        return searchControls;
    }

    @Override
    protected Class<? extends BaseSearchPage> getMenuPageClass() {
        return MetadataSearchPage.class;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        ComponentUtils.updatePersistentFormComponents(this);
    }

    @Override
    protected void addColumns(List<IColumn<ActionableSearchResult<T>>> columns) {
        columns.add(new ActionsColumn<ActionableSearchResult<T>>(""));
        columns.add(new BaseSearchPanel.ArtifactNameColumn("Metadata Container"));
        columns.add(new PropertyColumn<ActionableSearchResult<T>>(
                Model.of("Path"), "searchResult.relDirPath", "searchResult.relDirPath"));
        columns.add(new PropertyColumn<ActionableSearchResult<T>>(
                Model.of("Repository"), "searchResult.repoKey", "searchResult.repoKey"));
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected SearchResults<T> searchArtifacts() {
        return search(metaDataSearchCheckBox.isChecked(), searchControls);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected SearchResults<T> performLimitlessArtifactSearch() {
        MetadataSearchControls controlsCopy = new MetadataSearchControls<MetadataSearchResult>(searchControls);
        controlsCopy.setLimitSearchResults(false);
        return search(metaDataSearchCheckBox.isChecked(), controlsCopy);
    }

    @Override
    protected void onSearch() {
        super.onSearch();
        typesChoices.addHistory();
        if (metaDataSearchCheckBox.isChecked()) {
            metaDataNames = new ArrayList<String>();
            metaDataNames.add(searchControls.getMetadataName());
        }
    }

    @Override
    protected void onNoResults() {
        warnNoArtifacts(searchControls.getValue());
    }

    @Override
    protected ActionableSearchResult<T> getActionableResult(T searchResult) {
        if (metaDataSearchCheckBox.isChecked()) {
            return new ActionableMetadataSearchResult<T>(searchResult);
        }
        return new ActionableArtifactSearchResult<T>(searchResult);
    }

    @Override
    protected boolean isLimitSearchResults() {
        return searchControls.isLimitSearchResults();
    }

    private void adjustForMetadataLastSearch() {
        metaDataNames = xmlTypes;
        String id = metaDataSearchCheckBox.getId();
        Cookie cookie = CookieUtils.getCookieBycomponentId(id);
        if (cookie != null) {
            String value = cookie.getValue();
            if ("true".equals(value)) {
                metaDataNames = Lists.newArrayList();
            }
        }
    }

    /**
     * Performs the search
     *
     * @param metaDataSearch True if should search for metadata. False if should search for xml type
     * @param controls       Search controls
     * @return List of search results
     */
    private SearchResults search(boolean metaDataSearch, MetadataSearchControls controls) {
        if (metaDataSearch) {
            return searchService.searchMetadata(controls);
        }
        return searchService.searchXmlContent(controls);
    }
}