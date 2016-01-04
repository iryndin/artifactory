package org.artifactory.aql.result.rows;

import static org.artifactory.aql.model.AqlDomainEnum.entries;
import static org.artifactory.aql.model.AqlFieldEnum.*;

/**
 * @author Gidi Shabat
 */
@QueryTypes(value = entries, fields = {archiveEntryPath, archiveEntryName, archiveEntryPathId, archiveEntryNameId})
public interface AqlArchiveEntryItem extends AqlRowResult {
    String getEntryName();

    String getEntryPath();
}
