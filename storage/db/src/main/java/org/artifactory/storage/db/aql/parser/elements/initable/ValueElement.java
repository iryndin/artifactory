package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalValueElement;

/**
 * @author Gidi Shabat
 */
public class ValueElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return new InternalValueElement();
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}
