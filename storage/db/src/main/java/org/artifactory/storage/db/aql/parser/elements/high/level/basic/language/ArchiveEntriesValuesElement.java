package org.artifactory.storage.db.aql.parser.elements.high.level.basic.language;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalNameElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

import java.util.List;

import static org.artifactory.aql.model.AqlDomainEnum.archives;
import static org.artifactory.aql.model.AqlDomainEnum.entries;
import static org.artifactory.storage.db.aql.parser.AqlParser.archiveValues;
import static org.artifactory.storage.db.aql.parser.AqlParser.dot;

/**
 * @author Gidi Shabat
 */
public class ArchiveEntriesValuesElement extends LazyParserElement implements DomainProviderElement {

    @Override
    protected ParserElement init() {
        List<ParserElement> list = Lists.newArrayList();
        fillWithSubDomains(list);
        return fork(list.toArray(new ParserElement[list.size()]));
    }

    private void fillWithSubDomains(List<ParserElement> list) {
        list.add(forward(forward(new InternalNameElement(archives.signatue), forward(dot, archiveValues))));
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }

    @Override
    public AqlDomainEnum getDomain() {
        return entries;
    }
}