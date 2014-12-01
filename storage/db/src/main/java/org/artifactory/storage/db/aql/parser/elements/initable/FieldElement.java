package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalFieldElement;

/**
 * @author Gidi Shabat
 */
public class FieldElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(new InternalFieldElement());
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}
