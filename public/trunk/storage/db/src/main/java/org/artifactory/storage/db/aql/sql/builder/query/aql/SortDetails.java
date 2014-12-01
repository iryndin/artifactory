package org.artifactory.storage.db.aql.sql.builder.query.aql;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlSortTypeEnum;

import java.util.List;

/**
 * Contains the sort info in the AqlQuery
 *
 * @author Gidi Shabat
 */
public class SortDetails {
    private AqlSortTypeEnum sortType;
    private List<AqlField> list = Lists.newArrayList();

    public void addField(AqlField fieldEnum) {
        list.add(fieldEnum);
    }

    public void setSortType(AqlSortTypeEnum sortType) {
        this.sortType = sortType;
    }

    public List<AqlField> getFields() {
        return list;
    }

    public AqlSortTypeEnum getSortType() {
        return sortType;
    }
}
