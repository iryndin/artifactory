package org.artifactory.storage.db.aql.parser.elements.high.level.basic.language;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalNameElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

import java.util.List;

import static org.artifactory.aql.model.AqlDomainEnum.archives;
import static org.artifactory.aql.model.AqlDomainEnum.items;
import static org.artifactory.storage.db.aql.parser.AqlParser.dot;
import static org.artifactory.storage.db.aql.parser.AqlParser.itemDomains;

/**
 * @author Gidi Shabat
 */
public class ArchiveDomainsElement extends LazyParserElement implements DomainProviderElement {

    @Override
    protected ParserElement init() {
        List<ParserElement> list = Lists.newArrayList();
        fillWithDomains(list);
        fillWithSubDomains(list);
        return fork(list.toArray(new ParserElement[list.size()]));
    }

    private void fillWithDomains(List<ParserElement> list) {
        list.add(new IncludeDomainElement(archives));
    }

    private void fillWithSubDomains(List<ParserElement> list) {
        list.add(forward(new InternalNameElement(items.signatue),
                fork(new EmptyIncludeDomainElement(items), forward(dot, itemDomains))));
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }

    @Override
    public AqlDomainEnum getDomain() {
        return archives;
    }
}
