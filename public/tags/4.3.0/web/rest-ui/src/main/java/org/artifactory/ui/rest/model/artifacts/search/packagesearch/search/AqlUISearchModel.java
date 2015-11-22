package org.artifactory.ui.rest.model.artifacts.search.packagesearch.search;

import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Dan Feldman
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AqlUISearchModel implements RestModel {

    //both
    protected String id;

    //out
    protected String displayName;
    protected String fullName;
    protected AqlComparatorEnum[] allowedComparators;

    //in
    protected AqlComparatorEnum comparator;
    protected List<String> values;

    //Used for Serialization
    public AqlUISearchModel(String id, String displayName, String fullName, AqlComparatorEnum[] allowedComparators) {
        this.id = id;
        this.displayName = displayName;
        this.fullName = fullName;
        this.allowedComparators = allowedComparators;
    }

    @JsonCreator //Used for Deserialization
    public AqlUISearchModel(@JsonProperty("id") String id, @JsonProperty("comparator") AqlComparatorEnum comparator,
            @JsonProperty("values") List<String> values) {
        this.id = id;
        this.values = values;
        if (values.stream().filter(value -> value.contains("*") || value.contains("?")).findAny().isPresent()) {
            this.comparator = AqlComparatorEnum.matches;
        } else if (comparator != null) {
            this.comparator = comparator;
        } else {
            this.comparator = AqlComparatorEnum.equals;
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFullName() {
        return fullName;
    }

    public AqlComparatorEnum[] getAllowedComparators() {
        return allowedComparators;
    }

    @JsonIgnore
    public AqlComparatorEnum getComparator() {
        return comparator;
    }

    @JsonIgnore
    public List<String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
