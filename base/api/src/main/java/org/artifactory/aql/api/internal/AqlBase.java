package org.artifactory.aql.api.internal;

import com.google.common.collect.Lists;
import org.artifactory.aql.AqlException;
import org.artifactory.aql.api.AqlApiElement;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlOperatorEnum;
import org.artifactory.aql.model.AqlSortTypeEnum;
import org.artifactory.aql.model.AqlVariableTypeEnum;
import org.artifactory.aql.model.DomainSensitiveField;
import org.artifactory.aql.result.rows.AqlRowResult;
import org.artifactory.aql.result.rows.QueryTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gidi Shabat
 */
public class AqlBase<T extends AqlBase, Y extends AqlRowResult> implements AqlApiElement {

    protected SortApiElement sortApiElement = new SortApiElement();
    protected LimitApiElement limit = new LimitApiElement();
    protected OffsetApiElement offset = new OffsetApiElement();
    protected FilterApiElement filter = new FilterApiElement();
    protected DomainApiElement domain = new DomainApiElement();
    protected IncludeApiElement include = new IncludeApiElement();


    public AqlBase(Class<? extends AqlRowResult> domainClass) {
        QueryTypes annotation = domainClass.getAnnotation(QueryTypes.class);
        AqlDomainEnum domain = annotation.value();
        AqlFieldEnum[] resultField = annotation.fields();
        this.domain.setDomain(domain);
        for (AqlFieldEnum field : domain.fields) {
            this.include.getResultFields().add(new DomainSensitiveField(field, Lists.newArrayList(domain)));
        }
        for (AqlFieldEnum field : resultField) {
            this.include.getResultFields().add(new DomainSensitiveField(field, Lists.newArrayList(domain)));
        }
    }

    @SafeVarargs
    public static <T extends AqlBase> AndClause<T> and(AqlApiElement<T>... elements) {
        return new AndClause(elements);
    }

    @SafeVarargs
    public static <T extends AqlBase> PropertyResultFilterClause<T> propertyResultFilter(AqlApiElement<T>... elements) {
        return new PropertyResultFilterClause(elements);
    }

    @SafeVarargs
    public static <T extends AqlBase> OrClause<T> or(AqlApiElement<T>... elements) {
        return new OrClause(elements);
    }

    @SafeVarargs
    public static <T extends AqlBase> FreezeJoin<T> freezeJoin(AqlApiElement<T>... elements) {
        return new FreezeJoin(elements);
    }

    public T filter(AqlApiElement<T> filter) {
        this.filter.setFilter(filter);
        return (T) this;
    }

    @Override
    public List<AqlApiElement> get() {
        ArrayList<AqlApiElement> elements = Lists.newArrayList();
        elements.add(domain);
        elements.add(sortApiElement);
        elements.add(filter);
        elements.add(limit);
        elements.add(offset);
        elements.add(include);
        return elements;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String toNative(int dept) {
        if(domain==null){
            throw new AqlException("Missing primary domain in query");
        }else{
            String ident= getIdent(dept+1);
            StringBuilder builder = new StringBuilder();
            builder.append(domain.toNative(dept)).append("find(\n");
            String identPlus= getIdent(dept+2);
            if(filter != null && ! filter.isEmpty()){
                builder.append(ident).append("{\n");
                builder.append(identPlus).append(filter.toNative(dept + 2)).append("\n");
                builder.append(ident).append("}\n");
            }
            builder.append(")");
            if(include != null && ! include.isEmpty()){
                builder.append(".\n").append(include.toNative(dept));
            }
            if(sortApiElement != null && ! sortApiElement.isEmpty()){
                builder.append(".\n").append(sortApiElement.toNative(dept+1));
            }
            if(offset != null && ! offset.isEmpty()){
                builder.append(".\n").append(offset.toNative(dept+1));
            }
            if(limit != null && ! limit.isEmpty()){
                builder.append(".\n").append(limit.toNative(dept+1));
            }
            return builder.toString();
        }
    }

    public T asc() {
        this.sortApiElement.setSortType(AqlSortTypeEnum.asc);
        return (T) this;
    }

    public T desc() {
        this.sortApiElement.setSortType(AqlSortTypeEnum.desc);
        return (T) this;
    }

    public T addSortElement(AqlApiDynamicFieldsDomains.AqlApiComparator<T> fields) {
        this.sortApiElement.addSortElement(fields);
        return (T) this;
    }

    public T limit(int limit) {
        this.limit.setLimit(limit);
        return (T) this;
    }

    public T offset(int offset) {
        this.offset.setOffset(offset);
        return (T) this;
    }

    public T include(AqlApiDynamicFieldsDomains.AqlApiComparator... comparator) {
        for (AqlApiDynamicFieldsDomains.AqlApiComparator aqlApiComparator : comparator) {
            include.getIncludeFields().add(new DomainSensitiveField(aqlApiComparator.fieldEnum, aqlApiComparator.domains));
        }

        return (T) this;
    }

    public static class FilterApiElement implements AqlApiElement {


        private AqlApiElement filter;

        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList(filter);
        }


        @Override
        public boolean isEmpty() {
            return filter == null  || filter.isEmpty();
        }

        public AqlApiElement getFilter() {
            return filter;
        }

        public void setFilter(AqlApiElement filter) {
            this.filter = filter;
        }

        @Override
        public String toNative(int dept) {
            if(filter==null){
                return "";
            }else {
                return filter.toNative(dept);
            }
        }

    }
    public static class DomainApiElement implements AqlApiElement {

        private AqlDomainEnum domain;
        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        public AqlDomainEnum getDomain() {
            return domain;
        }

        public void setDomain(AqlDomainEnum domain) {
            this.domain = domain;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public String toNative(int dept) {
            StringBuilder builder=new StringBuilder();
            for (String subDomain : domain.subDomains) {
                builder.append(subDomain).append(".");
            }
            return builder.toString();
        }

    }
    public static class IncludeApiElement implements AqlApiElement {

        private List<DomainSensitiveField> includeFields = Lists.newArrayList();
        private List<DomainSensitiveField> resultFields = Lists.newArrayList();
        public List<DomainSensitiveField> getIncludeFields() {
            return includeFields;
        }

        public List<DomainSensitiveField> getResultFields() {
            return resultFields;
        }

        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return includeFields.size()<=0;
        }

        @Override
        public String toNative(int dept) {
            StringBuilder builder=new StringBuilder();
            builder.append("include(");
            Set<DomainSensitiveField> set=new HashSet<>();
            set.addAll(includeFields);
            set.addAll(resultFields);
            ArrayList<DomainSensitiveField> allResultFields = Lists.newArrayList(set);
            for (int i = 0; i < allResultFields.size(); i++) {
                DomainSensitiveField field = allResultFields.get(i);
                builder.append("\"").append(getPath(field.getSubDomains())).append(field.getField().signature).append("\"");
                if(i< allResultFields.size()-1) {
                    builder.append(",");
                }
            }
            builder.append(")");
            return builder.toString();
        }

    }
    public static class LimitApiElement implements AqlApiElement {

        private long limit = Long.MAX_VALUE;
        public long getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return limit < 0 || limit >= Long.MAX_VALUE;
        }

        @Override
        public String toNative(int dept) {
            StringBuilder builder=new StringBuilder();
            builder.append("limit(");
            builder.append(""+limit);
            builder.append(")");
            return builder.toString();
        }

    }
    public static class OffsetApiElement implements AqlApiElement {

        private long offset = 0;
        public long getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return offset <= 0 || offset >= Long.MAX_VALUE;
        }

        @Override
        public String toNative(int dept) {
            StringBuilder builder=new StringBuilder();
            builder.append("offset(");
            builder.append("" + offset);
            builder.append(")");
            return builder.toString();
        }

    }
    public static class SortApiElement implements AqlApiElement {

        private AqlSortTypeEnum sortType = AqlSortTypeEnum.desc;
        private List<DomainSensitiveField> fields=new ArrayList<>();
        public AqlSortTypeEnum getSortType() {
            return sortType;
        }

        public void setSortType(AqlSortTypeEnum sortType) {
            this.sortType = sortType;
        }

        public List<DomainSensitiveField> getFields() {
            return fields;
        }

        public void addSortElement(AqlApiDynamicFieldsDomains.AqlApiComparator field) {
            this.fields.add(new DomainSensitiveField(field.fieldEnum,field.domains));
        }

        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return sortType == null || fields == null || fields.size() == 0;
        }

        @Override
        public String toNative(int dept) {
            StringBuilder builder=new StringBuilder();
            builder.append("sort({");
            builder.append("\"").append(sortType.getAqlName()).append("\"");
            builder.append(":[");
            for (int i = 0; i < fields.size(); i++) {
                DomainSensitiveField field=fields.get(i);
                builder.append("\"").append(getPath(field.getSubDomains())).
                        append(field.getField().signature).append("\"");
                if(i<fields.size()-1) {
                    builder.append(",");
                }
            }
            builder.append("]");
            builder.append("})");
            return builder.toString();
        }

    }
    public static class AndClause<T extends AqlBase> implements AqlApiElement<T> {

        private final ArrayList<AqlApiElement<T>> andElements;

        public AndClause(AqlApiElement<T>[] elements) {
            this.andElements = Lists.newArrayList(elements);
        }

        @Override
        public List<AqlApiElement<T>> get() {
            return andElements;
        }

        @Override
        public boolean isEmpty() {
            return andElements.isEmpty();
        }

        public void append(AqlApiElement<T> aqlApiElement) {
            andElements.add(aqlApiElement);
        }

        @Override
        public String toNative(int dept) {
            String ident = getIdent(dept);
            StringBuilder builder=new StringBuilder();
            builder.append("\"").append(AqlOperatorEnum.and.signature).append("\"").append(":[\n");
            String identPlus = getIdent(dept+1);
            for (int i = 0; i < andElements.size(); i++) {
                AqlApiElement<T> element = andElements.get(i);
                builder.append(identPlus).append("{").append(element.toNative(dept+1)).append("}");
                if(i<andElements.size()-1) {
                    builder.append(",");
                }
                builder.append("\n");
            }
            builder.append(ident).append("]");
            return builder.toString();
        }

    }
    public static class OrClause<T extends AqlBase> implements AqlApiElement<T> {

        private final ArrayList<AqlApiElement<T>> orElements;

        public OrClause(AqlApiElement<T>[] elements) {
            this.orElements = Lists.newArrayList(elements);
        }

        @Override
        public List<AqlApiElement<T>> get() {
            return orElements;
        }

        @Override
        public boolean isEmpty() {
            return orElements.isEmpty();
        }

        public void append(AqlApiElement aqlApiElement) {
            orElements.add(aqlApiElement);
        }

        @Override
        public String toNative(int dept) {
            String ident = getIdent(dept);
            StringBuilder builder=new StringBuilder();
            builder.append("\"").append(AqlOperatorEnum.or.signature).append("\"").append(":[\n");
            String identPlus = getIdent(dept+1);
            for (int i = 0; i < orElements.size(); i++) {
                AqlApiElement<T> element = orElements.get(i);
                builder.append(identPlus).append("{").append(element.toNative(dept+1)).append("}");
                if(i<orElements.size()-1) {
                    builder.append(",");
                }
                builder.append("\n");
            }
            builder.append(ident).append("]");
            return builder.toString();
        }

    }
    public static class PropertyResultFilterClause<T extends AqlBase> implements AqlApiElement<T> {

        private final ArrayList<AqlApiElement<T>> elements;

        public PropertyResultFilterClause(AqlApiElement<T>[] elements) {
            this.elements = Lists.newArrayList(elements);
        }

        @Override
        public List<AqlApiElement<T>> get() {
            return elements;
        }

        @Override
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        public void append(AqlApiElement aqlApiElement) {
            elements.add(aqlApiElement);
        }

        @Override
        public String toNative(int dept) {
            String ident = getIdent(dept);
            StringBuilder builder=new StringBuilder();
            builder.append("\"").append(AqlOperatorEnum.resultFilter.signature).append("\"").append(
                    ":[\n");
            String identPlus = getIdent(dept+1);
            for (int i = 0; i < elements.size(); i++) {
                AqlApiElement<T> element = elements.get(i);
                builder.append(identPlus).append("{").append(element.toNative(dept+1)).append("}");
                if(i<elements.size()-1) {
                    builder.append(",");
                }
                builder.append("\n");
            }
            builder.append(ident).append("]");
            return builder.toString();
        }

    }
    public static class FreezeJoin<T extends AqlBase> implements AqlApiElement<T> {

        private final ArrayList<AqlApiElement<T>> elements;

        public FreezeJoin(AqlApiElement<T>[] elements) {
            this.elements = Lists.newArrayList(elements);
        }

        @Override
        public List<AqlApiElement<T>> get() {
            return elements;
        }

        @Override
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        @Override
        public String toNative(int dept) {
            String ident = getIdent(dept);
            StringBuilder builder=new StringBuilder();
            builder.append("\"").append(AqlOperatorEnum.freezeJoin.signature).append("\"").append(":[\n");
            String identPlus = getIdent(dept+1);
            for (int i = 0; i < elements.size(); i++) {
                AqlApiElement<T> element = elements.get(i);
                builder.append(identPlus).append("{").append(element.toNative(dept+1)).append("}");
                if(i<elements.size()-1) {
                    builder.append(",");
                }
                builder.append("\n");
            }
            builder.append(ident).append("]");
            return builder.toString();
        }

    }
    public static class CriteriaClause<T extends AqlBase> implements AqlApiElement<T> {

        private AqlFieldEnum fieldEnum;
        private List<AqlDomainEnum> subDomains;
        private AqlComparatorEnum comparator;
        private String value;
        public CriteriaClause(AqlFieldEnum fieldEnum, List<AqlDomainEnum> subDomains, AqlComparatorEnum comparator,
                String value) {
            this.fieldEnum = fieldEnum;
            this.subDomains = subDomains;
            this.comparator = comparator;
            this.value = value;
        }

        public List<AqlDomainEnum> getSubDomains() {
            return subDomains;
        }

        public AqlFieldEnum getFieldEnum() {
            return fieldEnum;
        }

        public AqlComparatorEnum getComparator() {
            return comparator;
        }

        public String getValue() {
            return value;
        }

        @Override
        public List<AqlApiElement<T>> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return fieldEnum == null || comparator == null;
        }

        @Override
        public String toNative(int dept) {
            StringBuilder builder=new StringBuilder();
            builder.append("\"").append(getPath(subDomains)).append(fieldEnum.signature).append("\"");
            builder.append(":{");
            builder.append("\"").append(comparator.signature).append("\"");
            builder.append(":");
            if(value==null) {
                builder.append("null");
            }else if(AqlVariableTypeEnum.longInt==fieldEnum.type || AqlVariableTypeEnum.integer==fieldEnum.type) {
                builder.append(value);
            }else {
                builder.append("\"").append(value).append("\"");
            }
            builder.append("}");
            return builder.toString();
        }

    }
    public static class PropertyCriteriaClause<T extends AqlBase> implements AqlApiElement {


        private String string1;
        private AqlComparatorEnum comparator;
        private String string2;
        private List<AqlDomainEnum> subDomains;
        public PropertyCriteriaClause(String key, AqlComparatorEnum comparator, String value,
                List<AqlDomainEnum> subDomains) {
            this.string1 = key;
            this.comparator = comparator;
            this.string2 = value;
            this.subDomains = subDomains;
        }

        public List<AqlDomainEnum> getSubDomains() {
            return subDomains;
        }

        public String getString1() {
            return string1;
        }

        public AqlComparatorEnum getComparator() {
            return comparator;
        }

        public String getString2() {
            return string2;
        }

        @Override
        public List<AqlApiElement<T>> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return string1 != null && string2 != null && comparator != null;
        }

        @Override
        public String toNative(int dept) {
            StringBuilder builder=new StringBuilder();
            builder.append("\"").append(getProprtySensitivePath(subDomains)).append(string1).append("\"");
            builder.append(":{");
            builder.append("\"").append(comparator.signature).append("\"");
            builder.append(":");
            if(string2==null){
                builder.append("null");
            }else {
                builder.append("\"").append(string2).append("\"");
            }
            builder.append("}");
            return builder.toString();
        }

    }


    private static String getPath(List<AqlDomainEnum> subDomains) {
        StringBuilder builder=new StringBuilder();
        for (int i = 1; i < subDomains.size(); i++) {
            AqlDomainEnum domainEnum = subDomains.get(i);
            builder.append(domainEnum.signatue).append(".");
        }
        return builder.toString();
    }

    private static String getProprtySensitivePath(List<AqlDomainEnum> subDomains) {
        StringBuilder builder=new StringBuilder();
        for (int i = 1; i < subDomains.size(); i++) {
            AqlDomainEnum domainEnum = subDomains.get(i);
            if ("property".equals(domainEnum.signatue)) {
                builder.append("@");
            } else {
                builder.append(domainEnum.signatue).append(".");
            }
        }
        return builder.toString();
    }

    private static String getIdent(int dept) {
        String result="";
        for (int i = 0; i < dept; i++) {
             result+="  ";
        }
        return result;
    }
}
