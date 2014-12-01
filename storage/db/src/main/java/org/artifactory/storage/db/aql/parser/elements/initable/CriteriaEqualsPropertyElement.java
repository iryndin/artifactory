package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.colon;
import static org.artifactory.storage.db.aql.parser.AqlParser.valueOrNumber;

/**
 * @author Gidi Shabat
 */
public class CriteriaEqualsPropertyElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(valueOrNumber, colon, valueOrNumber);
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}