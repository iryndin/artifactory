package org.artifactory.aql.api.domain.sensitive;

import com.google.common.collect.Lists;
import org.artifactory.aql.api.internal.AqlApiDynamicFieldsDomains;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.result.rows.AqlArchiveEntryItem;

import java.util.ArrayList;

/**
 * @author Gidi Shabat
 */
public class AqlApiEntry  extends AqlBase<AqlApiEntry, AqlArchiveEntryItem> {

    public AqlApiEntry() {
        super(AqlArchiveEntryItem.class);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiComparator<AqlApiEntry> path() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.entries);
        return new AqlApiDynamicFieldsDomains.AqlApiComparator(AqlFieldEnum.archiveEntryPath, subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiComparator<AqlApiEntry> name() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.entries);
        return new AqlApiDynamicFieldsDomains.AqlApiComparator(AqlFieldEnum.archiveEntryName, subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiArchiveDynamicFieldsDomains<AqlApiEntry> archive() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.entries, AqlDomainEnum.archives);
        return new AqlApiDynamicFieldsDomains.AqlApiArchiveDynamicFieldsDomains(subDomains);
    }



    public static AqlApiEntry create() {
        return new AqlApiEntry();
    }
}

