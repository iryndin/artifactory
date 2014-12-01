package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalNameElement;

/**
 * @author Gidi Shabat
 */
public class DomainElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        AqlDomainEnum[] values = AqlDomainEnum.values();
        InternalNameElement[] internalNameElements = new InternalNameElement[values.length];
        for (int i = 0; i < internalNameElements.length; i++) {
            internalNameElements[i] = new InternalNameElement(values[i].name());
        }
        return fork(internalNameElements);
    }

    @Override
    public boolean isVisibleInResult() {
        return true;
    }
}
