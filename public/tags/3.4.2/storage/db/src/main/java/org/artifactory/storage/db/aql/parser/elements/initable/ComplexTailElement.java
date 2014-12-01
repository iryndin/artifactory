package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author Gidi Shabat
 */
public class ComplexTailElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return fork(empty, forward(comma, filterComplex, filterComplexTail));
    }
}