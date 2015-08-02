package org.artifactory.storage.db.aql.parser.elements.high.level.basic.language;

import com.google.common.collect.Lists;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.storage.db.aql.parser.AqlParser;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalNameElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.InternalSignElement;
import org.artifactory.storage.db.aql.parser.elements.low.level.LazyParserElement;

import java.util.List;

import static org.artifactory.aql.model.AqlDomainEnum.buildProperties;
import static org.artifactory.aql.model.AqlDomainEnum.builds;
import static org.artifactory.storage.db.aql.parser.AqlParser.buildStar;
import static org.artifactory.storage.db.aql.parser.AqlParser.dot;

/**
 * @author Gidi Shabat
 */
public class BuildPropertyStarElement extends LazyParserElement implements DomainProviderElement {
    @Override
    protected ParserElement init() {
        List<ParserElement> list = Lists.newArrayList();
        fillWithDomainFields(list);
        fillWithSubDomains(list);
        return fork(list.toArray(new ParserElement[list.size()]));
    }

    private void fillWithDomainFields(List<ParserElement> list) {
        list.add(forward(new InternalSignElement("@"), AqlParser.value));
    }

    private void fillWithSubDomains(List<ParserElement> list) {
        list.add(forward(new InternalNameElement(builds.signatue), dot, buildStar));
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }

    @Override
    public AqlDomainEnum getDomain() {
        return buildProperties;
    }
}