package org.artifactory.storage.db.aql.parser.elements.high.level.domain.sensitive;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

/**
 * @author Gidi Shabat
 */
public class FilterElement extends DomainSensitiveParserElement {

    @Override
    protected ParserElement init() {
        return fork(provide(CriteriaEqualsElement.class), provide(CriteriaMatchElement.class),
                provide(CriteriaEqualsPropertyElement.class), provide(CriteriaMatchPropertyElement.class),
                provide(FunctionExtensionElement.class));
    }

}
