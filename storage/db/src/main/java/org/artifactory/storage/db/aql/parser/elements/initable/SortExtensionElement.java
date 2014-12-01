package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalNameElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author Gidi Shabat
 */
public class SortExtensionElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        ParserElement json = forward(quotes, sortType, quotes, colon, openParenthesis,
                fork(forward(quotes, field, quotes), forward(quotes, field, quotes, fieldTrail)), closeParenthesis);
        return forward(new InternalNameElement("sort"), openBrackets, openCurlyBrackets, json, closedCurlyBrackets,
                closeBrackets);
    }
}
