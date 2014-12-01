package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalNameElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author Gidi Shabat
 */
public class LimitElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(new InternalNameElement("limit"), openBrackets, limitValue, closeBrackets);
    }

    @Override
    public boolean isVisibleInResult() {
        return false;
    }
}