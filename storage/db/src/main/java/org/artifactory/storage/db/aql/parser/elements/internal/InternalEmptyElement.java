package org.artifactory.storage.db.aql.parser.elements.internal;


import org.artifactory.storage.db.aql.parser.AqlParserContext;
import org.artifactory.storage.db.aql.parser.ParserElementResultContainer;

/**
 * @author Gidi Shabat
 */
public class InternalEmptyElement extends InternalParserElement {

    @Override
    public ParserElementResultContainer[] peelOff(String queryRemainder, AqlParserContext context) {
        context.update(queryRemainder);
        return new ParserElementResultContainer[]{new ParserElementResultContainer(queryRemainder, "")};
    }
}
