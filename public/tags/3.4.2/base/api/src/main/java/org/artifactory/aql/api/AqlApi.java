package org.artifactory.aql.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlSortTypeEnum;
import org.artifactory.aql.result.rows.AqlArchiveArtifact;
import org.artifactory.aql.result.rows.AqlArtifact;
import org.artifactory.aql.result.rows.AqlBuild;
import org.artifactory.aql.result.rows.AqlBuildModule;
import org.artifactory.aql.result.rows.AqlBuildProperty;
import org.artifactory.aql.result.rows.AqlProperty;
import org.artifactory.aql.result.rows.AqlStatistics;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.artifactory.aql.model.AqlField.*;


/**
 * @author Gidi Shabat
 */
public class AqlApi {

    public static AqlArtifactApi findArtifacts() {
        return new AqlArtifactApi(AqlArtifact.class);
    }

    public static AqlPropertyApi findProperties() {
        return new AqlPropertyApi(AqlProperty.class);
    }

    public static AqlStatisticsApi findStatistics() {
        return new AqlStatisticsApi(AqlStatistics.class);
    }

    public static AqlArchiveApi findArchives() {
        return new AqlArchiveApi(AqlArchiveArtifact.class);
    }

    public static AqlBuilApi findBuilds() {
        return new AqlBuilApi(AqlBuild.class);
    }

    public static AqlBuildModuleApi findModules() {
        return new AqlBuildModuleApi(AqlBuildModule.class);
    }

    public static AqlBuildPropertyApi findBuildProperty() {
        return new AqlBuildPropertyApi(AqlBuildProperty.class);
    }

    public static AndClause and(AqlApiElement... elements) {
        return new AndClause(elements);
    }


    public static OrClause or(AqlApiElement... elements) {
        return new OrClause(elements);
    }

    public static FreezeJoin freezeJoin(AqlApiElement... elements) {
        return new FreezeJoin(elements);
    }

    public static CriteriaClause artifactRepo(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactRepo.signature, comparator, variable2);
    }

    public static CriteriaClause artifactPath(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactPath.signature, comparator, variable2);
    }

    public static CriteriaClause artifactName(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactName.signature, comparator, variable2);
    }

    public static CriteriaClause artifactType(AqlComparatorEnum comparator, int variable2) {
        return new CriteriaClause(artifactType.signature, comparator, "" + variable2);
    }

    public static CriteriaClause artifactCreated(AqlComparatorEnum comparator, DateTime variable2) {
        return new CriteriaClause(artifactCreated.signature, comparator, convertDateToString(variable2));
    }

    public static CriteriaClause artifactModified(AqlComparatorEnum comparator, DateTime variable2) {
        return new CriteriaClause(artifactModified.signature, comparator, convertDateToString(variable2));
    }

    public static CriteriaClause artifactUpdated(AqlComparatorEnum comparator, DateTime variable2) {
        return new CriteriaClause(artifactUpdated.signature, comparator, convertDateToString(variable2));
    }

    public static CriteriaClause artifactCreated_by(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactCreatedBy.signature, comparator, variable2);
    }

    public static CriteriaClause artifactModified_by(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactModifiedBy.signature, comparator, variable2);
    }

    public static CriteriaClause artifactDepth(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactDepth.signature, comparator, variable2);
    }

    public static CriteriaClause artifactNodeId(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactNodeId.signature, comparator, variable2);
    }

    public static CriteriaClause artifactOriginalMd5(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactOriginalMd5.signature, comparator, variable2);
    }

    public static CriteriaClause artifactActualMd5(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactActualMd5.signature, comparator, variable2);
    }

    public static CriteriaClause artifactOriginalSha1(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactOriginalSha1.signature, comparator, variable2);
    }

    public static CriteriaClause artifactActualSha1(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(artifactActualSha1.signature, comparator, variable2);
    }

    public static CriteriaClause artifactSize(AqlComparatorEnum comparator, long sizeInBytes) {
        return new CriteriaClause(artifactSize.signature, comparator, "" + sizeInBytes);
    }

    public static CriteriaClause propertyKey(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(AqlField.propertyKey.signature, comparator, variable2);
    }

    /**
     * Creates criteria based on any property that matches the input comparator and value.
     */
    public static CriteriaClause propertyValue(AqlComparatorEnum comparator, String value) {
        return new CriteriaClause(propertyValue.signature, comparator, value);
    }

    public static CriteriaClause archiveEntryName(AqlComparatorEnum comparator, String entryName) {
        return new CriteriaClause(archiveEntryName.signature, comparator, entryName);
    }

    public static CriteriaClause archiveEntryPath(AqlComparatorEnum comparator, String entryPath) {
        return new CriteriaClause(archiveEntryPath.signature, comparator, entryPath);
    }

    public static CriteriaClause buildModuleName(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildModuleName.signature, comparator, variable2);
    }

    public static CriteriaClause buildDependencyName(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildDependencyName.signature, comparator, variable2);
    }

    public static CriteriaClause buildDependencyScope(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildDependencyScope.signature, comparator, variable2);
    }

    public static CriteriaClause buildDependencyType(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildDependencyType.signature, comparator, variable2);
    }

    public static CriteriaClause build_dependencySha1(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildDependencySha1.signature, comparator, variable2);
    }

    public static CriteriaClause buildDependencyMd5(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildDependencyMd5.signature, comparator, variable2);
    }

    public static CriteriaClause buildArtifactName(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildArtifactName.signature, comparator, variable2);
    }

    public static CriteriaClause builArtifactType(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildArtifactType.signature, comparator, variable2);
    }

    public static CriteriaClause buildArtifactSha1(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildArtifactSha1.signature, comparator, variable2);
    }

    public static CriteriaClause buildArtifactMd5(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildArtifactMd5.signature, comparator, variable2);
    }

    public static CriteriaClause buildPropKey(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildPropertyKey.signature, comparator, variable2);
    }

    public static CriteriaClause buildPropValue(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildPropertyValue.signature, comparator, variable2);
    }

    public static CriteriaClause buildUrl(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildUrl.signature, comparator, variable2);
    }

    public static CriteriaClause buildName(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildName.signature, comparator, variable2);
    }

    public static CriteriaClause buildNumber(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildNumber.signature, comparator, variable2);
    }

    public static CriteriaClause buildCreated(AqlComparatorEnum comparator, DateTime variable2) {
        return new CriteriaClause(buildCreated.signature, comparator, convertDateToString(variable2));
    }

    public static CriteriaClause buildCreatedBy(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildCreatedBy.signature, comparator, variable2);
    }

    public static CriteriaClause buildModified(AqlComparatorEnum comparator, DateTime variable2) {
        return new CriteriaClause(buildModified.signature, comparator, convertDateToString(variable2));
    }

    public static CriteriaClause buildModifiedBy(AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(buildModifiedBy.signature, comparator, variable2);
    }

    public static CriteriaClause criteria(AqlField variable1, AqlComparatorEnum comparator, String variable2) {
        return new CriteriaClause(variable1.signature, comparator, variable2);
    }

    public static CriteriaClause criteria(AqlField variable1, AqlComparatorEnum comparator, int variable2) {
        return new CriteriaClause(variable1.signature, comparator, "" + variable2);
    }

    public static CriteriaClause criteria(AqlField variable1, AqlComparatorEnum comparator, long variable2) {
        return new CriteriaClause(variable1.signature, comparator, "" + variable2);
    }

    public static CriteriaClause criteria(AqlField variable1, AqlComparatorEnum comparator, double variable2) {
        return new CriteriaClause(variable1.signature, comparator, "" + variable2);
    }

    public static PropertyCriteriaClause property(String variable1, AqlComparatorEnum comparator, String variable2) {
        return new PropertyCriteriaClause(variable1, comparator, variable2);
    }

    private static String convertDateToString(DateTime date) {
        CentralConfigService centralConfigService = ContextHelper.get().getCentralConfig();
        String defaultPattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(defaultPattern);
        if (centralConfigService != null && centralConfigService.getDateFormatter() != null) {
            dateFormatter = centralConfigService.getDateFormatter();
        }
        return date.toString(dateFormatter);
    }

    public static class FilterApiElement implements AqlApiElement {

        private AqlApiElement filter;
        private Map<AqlField, Object> resultFilter = Maps.newHashMap();


        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList(filter);
        }

        @Override
        public boolean isEmpty() {
            return filter != null;
        }

        public void setFilter(AqlApiElement filter) {
            this.filter = filter;
        }

        public AqlApiElement getFilter() {
            return filter;
        }

        public Map<AqlField, Object> getResultFilter() {
            return resultFilter;
        }
    }

    public static class DomainApiElement implements AqlApiElement {
        private AqlDomainEnum[] domains;

        private AqlField[] extraFields;

        public AqlDomainEnum[] getDomains() {
            return domains;
        }

        public AqlField[] getExtraFields() {
            return extraFields;
        }

        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        public void setExtraFields(AqlField[] extraFields) {
            this.extraFields = extraFields;
        }

        public void setDomains(AqlDomainEnum... domains) {
            this.domains = domains;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    public static class LimitApiElement implements AqlApiElement {
        private int limit = Integer.MAX_VALUE;

        public int getLimit() {
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
            return limit < 0 && limit < Integer.MAX_VALUE;
        }
    }

    public static class SortApiElement implements AqlApiElement {
        private AqlSortTypeEnum sortType = AqlSortTypeEnum.desc;
        private AqlField[] fields;

        public AqlSortTypeEnum getSortType() {
            return sortType;
        }

        public AqlField[] getFields() {
            return fields;
        }

        @Override
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return sortType != null || fields == null || fields.length > 0;
        }

        public void setSortType(AqlSortTypeEnum sortType) {
            this.sortType = sortType;
        }

        public void setFields(AqlField[] fields) {
            this.fields = fields;
        }
    }

    public static class AndClause implements AqlApiElement {

        private final ArrayList<AqlApiElement> andElements;

        public AndClause(AqlApiElement[] elements) {
            this.andElements = Lists.newArrayList(elements);
        }

        @Override
        public List<AqlApiElement> get() {
            return andElements;
        }

        @Override
        public boolean isEmpty() {
            return andElements.isEmpty();
        }

        public void append(AqlApiElement aqlApiElement) {
            andElements.add(aqlApiElement);
        }
    }

    public static class OrClause implements AqlApiElement {

        private final ArrayList<AqlApiElement> orElements;

        public OrClause(AqlApiElement[] elements) {
            this.orElements = Lists.newArrayList(elements);
        }

        @Override
        public List<AqlApiElement> get() {
            return orElements;
        }

        @Override
        public boolean isEmpty() {
            return orElements.isEmpty();
        }

        public void append(AqlApiElement aqlApiElement) {
            orElements.add(aqlApiElement);
        }
    }

    public static class FreezeJoin implements AqlApiElement {

        private final ArrayList<AqlApiElement> elements;

        public FreezeJoin(AqlApiElement[] elements) {
            this.elements = Lists.newArrayList(elements);
        }

        @Override
        public List<AqlApiElement> get() {
            return elements;
        }

        @Override
        public boolean isEmpty() {
            return elements.isEmpty();
        }
    }

    public static class CriteriaClause implements AqlApiElement {
        private String string1;
        private AqlComparatorEnum comparator;
        private String string2;

        public CriteriaClause(String string1, AqlComparatorEnum comparator, String string2) {
            this.string1 = string1;
            this.comparator = comparator;
            this.string2 = string2;
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
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return string1 != null && string2 != null && comparator != null;
        }
    }


    public static class PropertyCriteriaClause implements AqlApiElement {
        private String string1;
        private AqlComparatorEnum comparator;
        private String string2;

        public PropertyCriteriaClause(String string1, AqlComparatorEnum comparator, String string2) {
            this.string1 = string1;
            this.comparator = comparator;
            this.string2 = string2;
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
        public List<AqlApiElement> get() {
            return Lists.newArrayList();
        }

        @Override
        public boolean isEmpty() {
            return string1 != null && string2 != null && comparator != null;
        }
    }
}
