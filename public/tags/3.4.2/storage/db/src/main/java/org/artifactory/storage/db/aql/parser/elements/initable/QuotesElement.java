package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalSignElement;

/**
 * @author Gidi Shabat
 */
public class QuotesElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return new InternalSignElement("\"");
    }
}