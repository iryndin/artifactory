package org.artifactory.aql.api.domain.sensitive;


import com.google.common.collect.Lists;
import org.artifactory.aql.api.internal.AqlApiDynamicFieldsDomains;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.result.rows.AqlArchiveEntryItem;

import java.util.ArrayList;

/**
 * @author Gidi Shabat
 */
public class AqlApiArchive extends AqlBase<AqlApiArchive, AqlArchiveEntryItem> {

    public AqlApiArchive() {
        super(AqlArchiveEntryItem.class);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiArchiveEntryDynamicFieldsDomains<AqlApiArchive> entry() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.archives, AqlDomainEnum.entries);
        return new AqlApiDynamicFieldsDomains.AqlApiArchiveEntryDynamicFieldsDomains(subDomains);
    }

    public static AqlApiDynamicFieldsDomains.AqlApiItemDynamicFieldsDomains<AqlApiArchive> item() {
        ArrayList<AqlDomainEnum> subDomains = Lists.newArrayList(AqlDomainEnum.archives, AqlDomainEnum.items);
        return new AqlApiDynamicFieldsDomains.AqlApiItemDynamicFieldsDomains(subDomains);
    }

    public static AqlApiArchive create() {
        return new AqlApiArchive();
    }
}
