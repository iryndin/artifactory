/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.util.SerializablePair;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.jfrog.build.api.Build;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Displays all the environment variables that were attached to the given build info
 *
 * @author Noam Y. Tenne
 */
public class BuildEnvironmentTabPanel extends Panel {

    public BuildEnvironmentTabPanel(String id, Build build) {
        super(id);

        FieldSetBorder propertiesBorder = new FieldSetBorder("propertiesBorder");
        add(propertiesBorder);

        List<IColumn<SerializablePair<String, String>>> columns = Lists.newArrayList();
        columns.add(new PropertyColumn<SerializablePair<String, String>>(Model.of("Key"), "first", "first"));
        columns.add(new PropertyColumn<SerializablePair<String, String>>(Model.of("Value"), "second", "second"));

        List<SerializablePair<String, String>> propertyPairs = getPropertiesAsPairs(build);

        PropertiesDataProvider dataProvider = new PropertiesDataProvider(propertyPairs);
        propertiesBorder.add(new SortableTable<SerializablePair<String, String>>(
                "properties", columns, dataProvider, Integer.MAX_VALUE));
    }

    /**
     * Converts the properties object to a list of pairs. This is done since we need to use an entry-like object as a
     * display model, and Map.Entry isn't serializable
     *
     * @param build Build to extract properties from
     * @return Pair list of properties
     */
    private List<SerializablePair<String, String>> getPropertiesAsPairs(Build build) {
        List<SerializablePair<String, String>> list = Lists.newArrayList();

        Properties properties = build.getProperties();

        if (properties != null) {
            for (Object key : properties.keySet()) {
                String keyString = String.valueOf(key);
                list.add(new SerializablePair<String, String>(keyString, properties.getProperty(keyString)));
            }
        }

        return list;
    }

    /**
     * The build properties data provider
     */
    private static class PropertiesDataProvider extends SortableDataProvider<SerializablePair<String, String>> {

        private List<SerializablePair<String, String>> entries;

        /**
         * Main constructor
         *
         * @param entries Entries to display
         */
        private PropertiesDataProvider(List<SerializablePair<String, String>> entries) {
            setSort("first", true);
            this.entries = entries;
        }

        public Iterator<SerializablePair<String, String>> iterator(int first, int count) {
            ListPropertySorter.sort(entries, getSort());
            List<SerializablePair<String, String>> listToReturn = entries.subList(first, first + count);
            return listToReturn.iterator();
        }

        public int size() {
            return entries.size();
        }

        public IModel<SerializablePair<String, String>> model(SerializablePair<String, String> object) {
            return new Model<SerializablePair<String, String>>(object);
        }
    }
}
