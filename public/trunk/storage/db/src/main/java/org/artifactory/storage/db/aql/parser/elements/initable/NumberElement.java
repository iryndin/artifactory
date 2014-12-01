package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalNumberElement;

/**
 * @author Gidi Shabat
 */
public class NumberElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return new InternalNumberElement();
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}
