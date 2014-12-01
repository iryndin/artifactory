package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.aql.model.AqlDomainValueTypeEnum;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalNameElement;

/**
 * @author Gidi Shabat
 */
public class DomainValueTypeElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        AqlDomainValueTypeEnum[] values = AqlDomainValueTypeEnum.values();
        ParserElement[] parserElements = new ParserElement[values.length];
        for (int i = 0; i < parserElements.length; i++) {
            parserElements[i] = forward(new InternalNameElement(values[i].signature, true));
        }
        return fork(parserElements);
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}