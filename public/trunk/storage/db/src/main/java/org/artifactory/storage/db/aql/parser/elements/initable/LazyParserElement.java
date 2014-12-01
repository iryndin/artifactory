package org.artifactory.storage.db.aql.parser.elements.initable;

import org.artifactory.storage.db.aql.parser.AqlParserContext;
import org.artifactory.storage.db.aql.parser.ParserElementResultContainer;
import org.artifactory.storage.db.aql.parser.elements.ParserElement;
import org.artifactory.storage.db.aql.parser.elements.dynamic.ForkParserElement;
import org.artifactory.storage.db.aql.parser.elements.dynamic.ForwardElement;

/**
 * @author Gidi Shabat
 */
public abstract class LazyParserElement implements ParserElement {
    private ParserElement element;

    @Override
    public ParserElementResultContainer[] peelOff(String queryRemainder, AqlParserContext context) {
        ParserElementResultContainer[] possiblePaths = element.peelOff(queryRemainder, context);
        if (isVisibleInResult()) {
            for (ParserElementResultContainer path : possiblePaths) {
                path.add(this, path.getElement());
            }
        }
        return possiblePaths;
    }

    @Override
    public void initialize() {
        if (element == null) {
            element = init();
            element.initialize();
        }
    }

    protected abstract ParserElement init();

    @Override
    public boolean isVisibleInResult() {
        return false;
    }

    ForkParserElement fork(ParserElement... elements) {
        return new ForkParserElement(elements);
    }

    ParserElement forward(ParserElement... elements) {
        return new ForwardElement(elements);
    }
}
