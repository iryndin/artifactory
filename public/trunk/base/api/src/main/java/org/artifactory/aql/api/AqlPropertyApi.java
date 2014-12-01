package org.artifactory.aql.api;

import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.result.rows.AqlProperty;

/**
 * @author Gidi Shabat
 */
public class AqlPropertyApi extends Aql<AqlPropertyApi, AqlProperty> {

    public <T1> AqlPropertyApi(Class<AqlProperty> domainClass) {
        super(domainClass);
    }

    public AqlPropertyApi addResultFilter(AqlField field, String value) {
        this.filter.getResultFilter().put(field, value);
        return this;
    }

    public AqlPropertyApi addResultFilter(AqlField field, int value) {
        this.filter.getResultFilter().put(field, value);
        return this;
    }

    public AqlPropertyApi addResultFilter(AqlField field, long value) {
        this.filter.getResultFilter().put(field, value);
        return this;
    }


}
