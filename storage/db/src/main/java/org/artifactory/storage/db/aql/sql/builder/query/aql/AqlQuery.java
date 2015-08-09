package org.artifactory.storage.db.aql.sql.builder.query.aql;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.DomainSensitiveField;

import java.util.List;

/**
 * The class contains all the information that has been collected from the AqlApi or the Parser
 *
 * @author Gidi Shabat
 */
public class AqlQuery {
    private List<DomainSensitiveField> resultFields = Lists.newArrayList();
    private SortDetails sort;
    private List<AqlQueryElement> aqlElements = Lists.newArrayList();
    private AqlDomainEnum domain;
    private int limit = Integer.MAX_VALUE;

    public List<DomainSensitiveField> getResultFields() {
        return resultFields;
    }

    public SortDetails getSort() {
        return sort;
    }

    public List<AqlQueryElement> getAqlElements() {
        return aqlElements;
    }

    public AqlDomainEnum getDomain() {
        return domain;
    }

    public int getLimit() {
        return limit;
    }

    public void setSort(SortDetails sort) {
        this.sort = sort;
    }

    public void setDomain(AqlDomainEnum domain) {
        this.domain = domain;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
