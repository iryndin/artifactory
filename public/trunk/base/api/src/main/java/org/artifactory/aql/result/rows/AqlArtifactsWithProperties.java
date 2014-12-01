package org.artifactory.aql.result.rows;

import java.util.Date;

import static org.artifactory.aql.model.AqlDomainEnum.artifacts;
import static org.artifactory.aql.model.AqlDomainEnum.properties;


/**
 * @author Gidi Shabat
 */
@QueryTypes({artifacts, properties})
public interface AqlArtifactsWithProperties extends AqlRowResult {
    Date getCreated();

    Date getModified();

    Date getUpdated();

    String getCreatedBy();

    String getModifiedBy();

    int getType();

    String getRepo();

    String getPath();

    String getName();

    int getDepth();

    int getNodeId();

    String getOriginalMd5();

    String getActualMd5();

    String getOriginalSha1();

    String getActualSha1();

    String getKey();

    String getValue();
}
