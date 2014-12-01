package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.comperator;
import static org.artifactory.storage.db.aql.parser.AqlParser.quotes;

/**
 * @author Gidi Shabat
 */
public class QuotedComperator extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(quotes, comperator, quotes);
    }
}