package org.artifactory.aql.api;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlSortTypeEnum;
import org.artifactory.aql.result.rows.AqlRowResult;
import org.artifactory.aql.result.rows.QueryTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gidi Shabat
 */
public class Aql<Y extends Aql<Y, T>, T extends AqlRowResult> implements AqlApiElement {

    protected AqlApi.DomainApiElement domain = new AqlApi.DomainApiElement();
    protected AqlApi.FilterApiElement filter = new AqlApi.FilterApiElement();
    protected AqlApi.SortApiElement sort = new AqlApi.SortApiElement();
    protected AqlApi.LimitApiElement limit = new AqlApi.LimitApiElement();

    public Aql(Class<T> domainClass) {
        QueryTypes annotation = domainClass.getAnnotation(QueryTypes.class);
        AqlDomainEnum[] domains = annotation.value();
        AqlField[] extraFields = annotation.fields();
        domain.setDomains(domains);
        domain.setExtraFields(extraFields);
    }

    public Y filter(AqlApiElement filter) {
        this.filter.setFilter(filter);
        return (Y) this;
    }

    public Y asc() {
        sort.setSortType(AqlSortTypeEnum.asc);
        return (Y) this;
    }

    public Y desc() {
        sort.setSortType(AqlSortTypeEnum.desc);
        return (Y) this;
    }

    public Y sortBy(AqlField... fields) {
        sort.setFields(fields);
        return (Y) this;
    }

    public Y limit(int limit) {
        this.limit.setLimit(limit);
        return (Y) this;
    }

    @Override
    public List<AqlApiElement> get() {
        ArrayList<AqlApiElement> elements = Lists.newArrayList();
        elements.add(domain);
        elements.add(sort);
        elements.add(filter);
        elements.add(limit);
        return elements;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}