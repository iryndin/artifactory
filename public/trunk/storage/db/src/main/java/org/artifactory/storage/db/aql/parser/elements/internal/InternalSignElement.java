package org.artifactory.storage.db.aql.parser.elements.internal;

import org.artifactory.storage.db.aql.parser.AqlParserContext;
import org.artifactory.storage.db.aql.parser.ParserElementResultContainer;

/**
 * @author Gidi Shabat
 */
public class InternalSignElement extends InternalParserElement {
    private String sign;

    public InternalSignElement(String sign) {
        this.sign = sign;
    }

    @Override
    public ParserElementResultContainer[] peelOff(String queryRemainder, AqlParserContext context) {
        if (queryRemainder.startsWith(sign)) {
            String string;
            if ("]".equals(sign) || "[".equals(sign) || ")".equals(sign) || "(".equals(sign)) {
                string = queryRemainder.replaceFirst("\\" + sign, "").trim();
            } else {
                string = queryRemainder.replaceFirst("[" + sign + "]", "").trim();
            }
            context.update(string);
            return new ParserElementResultContainer[]{new ParserElementResultContainer(string, sign)};
        } else {
            return new ParserElementResultContainer[0];
        }
    }
}



