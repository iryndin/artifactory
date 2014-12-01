package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author Gidi Shabat
 */
public class DomainValues extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(openCurlyBrackets, quotes, domainValueType, quotes, colon, openParenthesis,
                fork(forward(quotes, value, quotes), forward(quotes, value, quotes, valueTrail)),
                closeParenthesis, closedCurlyBrackets);
    }

    @Override
    public boolean isVisibleInResult() {
        return false;
    }
}
