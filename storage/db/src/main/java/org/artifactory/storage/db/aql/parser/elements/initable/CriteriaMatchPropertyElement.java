package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author Gidi Shabat
 */
public class CriteriaMatchPropertyElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(quotes, value, quotes, colon, openCurlyBrackets, quotedComperator, colon, valueOrNumber,
                closedCurlyBrackets);
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}
