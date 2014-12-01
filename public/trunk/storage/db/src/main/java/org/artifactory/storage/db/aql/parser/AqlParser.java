package org.artifactory.storage.db.aql.parser;

import org.apache.commons.lang.StringUtils;
import org.artifactory.aql.AqlParserException;
import org.artifactory.storage.db.aql.parser.elements.initable.*;
import org.artifactory.storage.db.aql.parser.elements.internal.InternalEmptyElement;

/**
 * Parse the Aql string query into parser result which is a list of parser elements
 * The parser does not have state therefore a single instance can be used on single JVM
 *
 * @author Gidi Shabat
 */
public class AqlParser {
    public static final String[] DELIMITERS = {"<=", ">=", "!=", " ", "<", ">", "(", ")", "[", "]", "{", "}", "=", "'", ".", ":", "\"", ":"};
    public static final String[] USED_KEYS = {"$mt", "$lt", "$eq", "and", "not", "or", "artifacts"};
    public static final CriteriaEqualsElement criteriaEquals = new CriteriaEqualsElement();
    public static final CriteriaMatchElement criteriaMatch = new CriteriaMatchElement();
    public static final CriteriaEqualsPropertyElement criteriaEqualsProperty = new CriteriaEqualsPropertyElement();
    public static final CriteriaMatchPropertyElement criteriaMatchProperty = new CriteriaMatchPropertyElement();
    public static final ComplexTailElement filterComplexTail = new ComplexTailElement();
    public static final FunctionExtensionElement functionExtentionElement = new FunctionExtensionElement();
    public static final FunctionElement functionElement = new FunctionElement();
    public static final CommaElement comma = new CommaElement();
    public static final FilterComplex filterComplex = new FilterComplex();
    public static final FilterTailElement filterTail = new FilterTailElement();
    public static final FilterElement filter = new FilterElement();
    public static final QuotedComperator quotedComperator = new QuotedComperator();
    public static final ComparatorElement comperator = new ComparatorElement();
    public static final QuotesElement quotes = new QuotesElement();
    public static final ColonElement colon = new ColonElement();
    public static final ValueNumberElement valueOrNumber = new ValueNumberElement();
    public static final ValueElement value = new ValueElement();
    public static final NumberElement number = new NumberElement();
    public static final ValueTrailElement valueTrail = new ValueTrailElement();
    public static final FieldTrailElement fieldTrail = new FieldTrailElement();
    public static final FieldElement field = new FieldElement();
    public static final OpenParenthesisElement openParenthesis = new OpenParenthesisElement();
    public static final CloseParenthesisElement closeParenthesis = new CloseParenthesisElement();
    public static final OpenCurlyBracketsElement openCurlyBrackets = new OpenCurlyBracketsElement();
    public static final CloseCurlyBracketsElement closedCurlyBrackets = new CloseCurlyBracketsElement();
    public static final OpenBracketsElement openBrackets = new OpenBracketsElement();
    public static final CloseBracketsElement closeBrackets = new CloseBracketsElement();
    public static final InternalEmptyElement empty = new InternalEmptyElement();
    public static final DomainValues domainValues = new DomainValues();
    public static final DomainElement domain = new DomainElement();
    public static final DomainExtentoinElement domainExtention = new DomainExtentoinElement();
    public static final DotElement dot = new DotElement();
    public static final SortTypeElement sortType = new SortTypeElement();
    public static final SortExtensionElement sortExtension = new SortExtensionElement();
    public static final LimitValueElement limitValue = new LimitValueElement();
    public static final LimitElement limit = new LimitElement();
    public static final DomainValueTypeElement domainValueType = new DomainValueTypeElement();
    public static final FindElement find = new FindElement();
    public static final RootElement root = new RootElement();

    /**
     * Init once during the class initialisation.
     * All the parser instances will use the same parser elements instances
     */
    static {
        root.initialize();
    }

    /**
     * Initialize the parser process starting from the root element which represent the entire language
     *
     * @param query The AQL query string
     * @return Parsing result
     * @throws AqlParserException If query parsing fails
     */
    public ParserElementResultContainer parse(String query) {
        AqlParserContext parserContext = new AqlParserContext();
        ParserElementResultContainer[] parserElementResultContainers = root.peelOff(query, parserContext);
        for (ParserElementResultContainer parserElementResultContainer : parserElementResultContainers) {
            if (StringUtils.isBlank(parserElementResultContainer.getQueryRemainder())) {
                return parserElementResultContainer;
            }
        }
        String subQuery = parserContext.getQueryRemainder() != null ?
                parserContext.getQueryRemainder().trim() : query.trim();
        throw new AqlParserException(String.format("Fail to parse query: %s, it looks like there is syntax error near" +
                " the following sub-query: %s", query, subQuery));
    }
}
