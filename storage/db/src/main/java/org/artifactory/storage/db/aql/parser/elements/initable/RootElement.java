package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;


/**
 * Represent the AQL Language structure
 *
 * @author Gidi Shabat
 */
public class RootElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(domainExtention, dot, find,
                fork(empty, forward(dot, sortExtension)),
                fork(empty, forward(dot, limit)));
    }
}
