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
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.Addon;
import org.artifactory.addon.wicket.SearchAddon;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.util.Pair;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Displays the build's general information
 *
 * @author Noam Y. Tenne
 */
public class BuildGeneralInfoTabPanel extends Panel {

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private AuthorizationService authorizationService;

    /**
     * Main constructor
     *
     * @param id    ID to assign to the panel
     * @param build Build to display
     */
    public BuildGeneralInfoTabPanel(String id, Build build) {
        super(id);

        addInfoBorder(build);
        addSaveSearchResultsPanel(build);
        addPropertiesBorder(build);
    }

    /**
     * Adds the general information border
     *
     * @param build Build object to display
     */
    private void addInfoBorder(Build build) {
        FieldSetBorder infoBorder = new FieldSetBorder("infoBorder");
        add(infoBorder);

        addLabeledValue(infoBorder, "version", "Version", build.getVersion(), true);
        addLabeledValue(infoBorder, "name", "Name", build.getName(), true);
        addLabeledValue(infoBorder, "number", "Number", build.getNumber(), true);

        BuildType buildType = build.getType();
        String buildTypeName = null;
        if (buildType != null) {
            buildTypeName = buildType.getName();
        }
        addLabeledValue(infoBorder, "type", "Type", buildTypeName, true);

        Agent agent = build.getAgent();
        String agentName = null;
        if (agent != null) {
            agentName = agent.toString();
        }
        addLabeledValue(infoBorder, "agent", "Agent", agentName, true);
        BuildAgent buildAgent = build.getBuildAgent();
        String buildAgentName = null;
        if (buildAgent != null) {
            buildAgentName = buildAgent.toString();
        }
        addLabeledValue(infoBorder, "buildAgent", "Build Agent", buildAgentName, true);
        addLabeledValue(infoBorder, "started", "Started", build.getStarted(), true);

        Duration duration = Duration.milliseconds(build.getDurationMillis());
        addLabeledValue(infoBorder, "duration", "Duration", duration.toString(), true);
        addLabeledValue(infoBorder, "principal", "Principal", build.getPrincipal(), true);
        addLabeledValue(infoBorder, "artifactoryPrincipal", "Artifactory Principal", build.getArtifactoryPrincipal(),
                true);
        infoBorder.add(new Label("urlLabel", "URL:"));

        String url = build.getUrl();
        if (url == null) {
            url = "";
        }
        infoBorder.add(new ExternalLink("url", url, url));
        addLabeledValue(infoBorder, "parentBuildId", "Parent Build ID", build.getParentBuildId(),
                StringUtils.isNotBlank(build.getParentBuildId()));
        addLabeledValue(infoBorder, "parentBuildName", "Parent Build Name", build.getParentName(),
                StringUtils.isNotBlank(build.getParentName()));
        addLabeledValue(infoBorder, "parentBuildNumber", "Parent Build Number", build.getParentNumber(),
                StringUtils.isNotBlank(build.getParentNumber()));
    }

    /**
     * Adds a labeled value of the given details
     *
     * @param infoBorder Border to add the label to
     * @param id         ID to assign to the labeled value
     * @param label      Textual label
     * @param labelValue Textual value
     * @param visible
     */
    private void addLabeledValue(FieldSetBorder infoBorder, String id, String label, String labelValue,
            boolean visible) {
        LabeledValue value = new LabeledValue(id, label + ": ", (labelValue != null) ? labelValue : "");
        value.setVisible(visible);
        infoBorder.add(value);
    }

    /**
     * Adds the save search results panel
     *
     * @param build Build to use as file resource
     */
    private void addSaveSearchResultsPanel(Build build) {
        SearchAddon searchAddon = addonsManager.addonByType(SearchAddon.class);
        //Make the search addon the requesting, so if it is disabled, it's because of the search
        add(searchAddon.getBuildSearchResultsPanel(Addon.SEARCH, build));
    }

    /**
     * Adds the build properties border
     *
     * @param build Build to display
     */
    private void addPropertiesBorder(Build build) {
        FieldSetBorder propertiesBorder = new FieldSetBorder("propertiesBorder");
        add(propertiesBorder);

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model("Key"), "first", "first"));
        columns.add(new PropertyColumn(new Model("Value"), "second", "second"));

        List<Pair<String, String>> propertyPairs = getPropertiesAsPairs(build);

        PropertiesDataProvider dataProvider = new PropertiesDataProvider(propertyPairs);
        propertiesBorder.add(new SortableTable("properties", columns, dataProvider, 10));
    }

    /**
     * Converts the properties object to a list of pairs. This is done since we need to use an entry-like object as a
     * display model, and Map.Entry isn't serializable
     *
     * @param build Build to extract properties from
     * @return Pair list of properties
     */
    private List<Pair<String, String>> getPropertiesAsPairs(Build build) {
        List<Pair<String, String>> list = Lists.newArrayList();

        Properties properties = build.getProperties();

        if (properties != null) {
            for (Object key : properties.keySet()) {
                String keyString = String.valueOf(key);
                list.add(new Pair<String, String>(keyString, properties.getProperty(keyString)));
            }
        }

        return list;
    }

    /**
     * The build properties data provider
     */
    private static class PropertiesDataProvider extends SortableDataProvider {

        private List<Pair<String, String>> entries;

        /**
         * Main constructor
         *
         * @param entries Entries to display
         */
        private PropertiesDataProvider(List<Pair<String, String>> entries) {
            setSort("first", true);
            this.entries = entries;
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(entries, getSort());
            List<Pair<String, String>> listToReturn = entries.subList(first, first + count);
            return listToReturn.iterator();
        }

        public int size() {
            return entries.size();
        }

        public IModel model(Object object) {
            return new Model((Pair) object);
        }
    }
}