package org.artifactory.ui.rest.model.artifacts.search.packagesearch.search;

import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.descriptor.repo.DockerApiVersion;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Dan Feldman
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AqlUIDockerSearchModel extends AqlUISearchModel {

    private DockerApiVersion version;

    //Used for Serialization
    public AqlUIDockerSearchModel(String id, String displayName, String fullName,
            AqlComparatorEnum[] allowedComparators) {
        super(id, displayName, fullName, allowedComparators);
    }

    @JsonCreator //Used for Deserialization
    public AqlUIDockerSearchModel(@JsonProperty("id") String id,
            @JsonProperty("comparator") AqlComparatorEnum comparator,
            @JsonProperty("values") List<String> values, @JsonProperty("dockerVersion") String version) {
        super(id, comparator, values);
        this.version = DockerApiVersion.valueOf(version);
    }

    @JsonIgnore
    public DockerApiVersion getVersion() {
        return version;
    }
}