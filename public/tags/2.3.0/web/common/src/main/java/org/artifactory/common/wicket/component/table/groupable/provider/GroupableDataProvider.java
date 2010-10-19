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

package org.artifactory.common.wicket.component.table.groupable.provider;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.common.wicket.component.table.groupable.cache.GroupSizeCache;
import org.artifactory.common.wicket.util.ListPropertySorter;

import java.io.Serializable;
import java.util.*;

/**
 * @author Yoav Aharoni
 */
public class GroupableDataProvider<T extends Serializable> extends SortableDataProvider<T> implements IGroupStateLocator {
    private List<T> data;
    private Map<String, IChoiceRenderer<T>> groupRenederMap = new HashMap<String, IChoiceRenderer<T>>();
    private GroupSizeCache groupSizeCache;
    private SortParam groupParam;

    @SuppressWarnings({"unchecked"})
    public GroupableDataProvider() {
        this((List<T>) Collections.EMPTY_LIST);
    }

    public GroupableDataProvider(List<T> data) {
        this.data = data;
    }

    public Iterator<T> iterator(int first, int count) {
        List<T> data = getData();
        ListPropertySorter.sort(data, getGroupParam(), getSort());
        List<T> list = data.subList(first, first + count);
        return list.iterator();
    }

    public int size() {
        return data.size();
    }

    @SuppressWarnings({"unchecked"})
    public IModel model(T object) {
        return new Model<T>(object);
    }

    public SortParam getGroupParam() {
        return groupParam;
    }

    public void setGroupParam(SortParam groupParam) {
        this.groupParam = groupParam;
        clearGroupCache();
    }

    public int getGroupSize(int index) {
        SortParam groupParam = getGroupParam();
        if (groupSizeCache == null) {
            groupSizeCache =
                    GroupSizeCache.getSizeCache(iterator(0, size()), getGroupReneder(groupParam.getProperty()));
        }
        return groupSizeCache.getGroupSize(index);
    }

    public void clearGroupCache() {
        groupSizeCache = null;
    }

    public final IChoiceRenderer<T> getGroupReneder(String property) {
        IChoiceRenderer<T> choiceRenderer = groupRenederMap.get(property);
        if (choiceRenderer == null) {
            choiceRenderer = new ChoiceRenderer<T>(property, property);
            groupRenederMap.put(property, choiceRenderer);
        }
        return choiceRenderer;
    }

    public final void setGroupReneder(String property, IChoiceRenderer<T> choiceRenderer) {
        groupRenederMap.put(property, choiceRenderer);
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
        clearGroupCache();
    }
}
