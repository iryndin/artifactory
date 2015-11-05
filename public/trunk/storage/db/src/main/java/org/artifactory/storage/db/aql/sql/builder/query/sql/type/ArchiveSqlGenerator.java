package org.artifactory.storage.db.aql.sql.builder.query.sql.type;

import com.google.common.collect.Lists;
import org.artifactory.storage.db.aql.sql.builder.links.TableLink;
import org.artifactory.storage.db.aql.sql.builder.links.TableLinkRelation;
import org.artifactory.storage.db.aql.sql.model.SqlTableEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * The class contains tweaking information and optimizations for Archives queries.
 *
 * @author Gidi Shabat
 */
public class ArchiveSqlGenerator extends BasicSqlGenerator {

    @Override
    protected List<TableLink> getExclude() {
        return Lists.newArrayList();
    }

    @Override
    protected List<TableLinkRelation> overrideRoute(List<TableLinkRelation> route) {
        return new ArrayList<>();
    }

    @Override
    protected SqlTableEnum getMainTable() {
        return SqlTableEnum.indexed_archives;
    }

}

