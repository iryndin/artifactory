package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

import static org.artifactory.storage.db.aql.parser.AqlParser.*;

/**
 * @author Gidi Shabat
 */
public class DomainExtentoinElement extends LazyParserElement {
    @Override
    protected ParserElement init() {
        return forward(fork(domain), fork(empty, forward(openBrackets, fork(empty, domainValues), closeBrackets)));
    }

    @Override
    public boolean isVisibleInResult() {
        return false;
    }
}