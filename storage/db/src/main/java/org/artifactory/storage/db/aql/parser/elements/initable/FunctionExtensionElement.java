package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author Gidi Shabat
 */
public class FunctionExtensionElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(quotes, functionElement, quotes, colon, openParenthesis,
                fork(filterComplex, forward(filterComplex, filterComplexTail)), closeParenthesis);
    }
}