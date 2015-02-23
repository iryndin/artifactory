package org.artifactory.storage.db.aql.parser.elements.low.level;

import org.apache.commons.lang.StringUtils;
import org.artifactory.aql.AqlFieldResolver;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlOperatorEnum;
import org.artifactory.storage.db.aql.parser.AqlParserContext;
import org.artifactory.storage.db.aql.parser.ParserElementResultContainer;

/**
 * @author Gidi Shabat
 */
public class InternalFieldElement extends InternalParserElement {

    @Override
    public ParserElementResultContainer[] peelOff(String queryRemainder, AqlParserContext context) {
        String string;
        int index = StringUtils.indexOf(queryRemainder, "\"");
        if (index >= 0) {
            string = queryRemainder.substring(0, index);
            if (AqlComparatorEnum.value(string) != null) {
                return new ParserElementResultContainer[0];
            }
            if (AqlOperatorEnum.value(string) != null) {
                return new ParserElementResultContainer[0];
            }
            if (!(AqlFieldResolver.resolve(string.toLowerCase()) instanceof AqlField)) {
                return new ParserElementResultContainer[0];
            }
            String trim = StringUtils.replaceOnce(queryRemainder, string, "").trim();
            context.update(trim);
            return new ParserElementResultContainer[]{new ParserElementResultContainer(trim, string)};
        } else {
            return new ParserElementResultContainer[0];
        }
    }
}
