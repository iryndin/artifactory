package org.artifactory.storage.db.aql.parser.elements.internal;

import org.artifactory.storage.db.aql.parser.elements.ParserElement;

/**
 * Base class for internal/framework elements
 *
 * @author Yossi Shaul
 */
public abstract class InternalParserElement implements ParserElement {

    /**
     * Internal elements are the base leaf elements and does not require init.
     */
    @Override
    public void initialize() {

    }

    /**
     * @return Internal elements are always not visible
     */
    @Override
    public boolean isVisibleInResult() {
        return false;
    }

}
