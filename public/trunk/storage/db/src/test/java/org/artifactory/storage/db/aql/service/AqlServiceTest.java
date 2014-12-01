package org.artifactory.storage.db.aql.service;

import org.apache.commons.lang.StringUtils;
import org.artifactory.aql.result.AqlQueryResultIfc;
import org.artifactory.aql.result.rows.AqlArtifactWithBuildArtifacts;
import org.artifactory.aql.result.rows.AqlArtifactsWithStatistics;
import org.artifactory.aql.result.rows.AqlBaseArtifact;
import org.artifactory.aql.result.rows.AqlProperty;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Gidi Shabat
 */
@Test
public class AqlServiceTest extends DbBaseTest {
    @Autowired
    private AqlServiceImpl aqlService;

    @BeforeClass
    public void setup() {
        importSql("/sql/aql_test.sql");
    }

    @Test
    public void complexQueryWithPropertiesSortJoinAndOr() {
        aqlService.executeQueryEager("artifacts.find(" +
                "{" +
                "   \"$or\":[" +
                "               {\"_artifact_repo\" : \"jcentral\"}," +
                "               {\"_artifact_name\" : {\"$matches\" : \".*test\"}}," +
                "               {\"_artifact_type\" : 1}," +
                "               {\"$and\":[" +
                "                           {\"$join\":[" +
                "                                       {\"version\":\"1,1,1\"}," +
                "                                       {\"_artifact_type\":1}" +
                "                                    ]" +
                "                           }," +
                "                           {" +
                "                               \"version\":\"2.2.2\"}" +
                "                        ]" +
                "                }," +
                "               {\"_artifact_repo\" : \"jcentral\"}" +
                "            ]," +
                "   \"_artifact_repo\" : \"jcentral\"" +
                "}" +
                ").sort(" +
                "{" +
                "    \"$asc\" : [" +
                "                        \"_artifact_repo\",\"_artifact_name\",\"_artifact_path\"" +
                "               ]" +
                "}" +
                ")");
    }

    @Test
    public void findArtifactsFilterByPropertiesUsingDefaultAnd() {
        aqlService.executeQueryEager("artifacts.find(" +
                "{" +
                "       \"license\" : \"GPL\"," +
                "       \"license\" : {\"$matches\" : \"*GPL\"}" +
                "}" +
                ")");
    }

    @Test
    public void findArtifactsFilterByPropertiesUsingOrAndDefaultAnd() {
        aqlService.executeQueryEager("artifacts.find(" +
                "{" +
                "       \"license\" : \"GPL\"," +
                "       \"license\" : {\"$matches\" : \"*GPL\"}," +
                "       \"$or\" : [" +
                "                   {\"group.id\" : \"org.jfrog\"}," +
                "                   {\"classifier\" : {\"$matches\" : \"source\"}}" +
                "                 ]" +
                "}" +
                ")");
    }

    @Test
    public void findArtifactsFilterByPropertiesUsingJoinAndDefaultAnd() {
        aqlService.executeQueryEager("artifacts.find(" +
                "{" +
                "       \"license\" : \"GPL\"," +
                "       \"license\" : {\"$matches\" : \"*GPL\"}," +
                "       \"$join\" : [" +
                "                   {\"group.id\" : \"org.jfrog\"}," +
                "                   {\"classifier\" : {\"$matches\" : \"source\"}}" +
                "                 ]" +
                "}" +
                ")");
    }

    @Test
    public void findPropertiesFilterByPropertiesUsingJoinAndDefaultAnd1() {
        aqlService.executeQueryEager("properties.find(" +
                "{" +
                "       \"license\" : \"GPL\"," +
                "       \"license\" : {\"$matches\" : \"*GPL\"}," +
                "       \"$join\" : [" +
                "                   {\"group.id\" : \"org.jfrog\"}," +
                "                   {\"classifier\" : {\"$matches\" : \"source\"}}" +
                "                 ]" +
                "}" +
                ")");
    }

    @Test
    public void findPropertiesFilterByPropertiesUsingJoinAndDefaultAnd2() {
        aqlService.executeQueryEager("properties.find(" +
                "{" +
                "       \"license\" : \"GPL\"," +
                "       \"license\" : {\"$matches\" : \"*GPL\"}," +
                "       \"$join\" : [" +
                "                   {\"group.id\" : \"org.jfrog\"}," +
                "                   {\"classifier\" : {\"$matches\" : \"source\"}}" +
                "                 ]," +
                "       \"repo\" : \"jcenter\"," +
                "       \"version\" : \"1.1.1\"," +
                "       \"downloads\" : 1" +
                "}" +
                ")");
    }
    /*Archive searches*/

    @Test
    public void findArtifactsByPropertyValueAndMatch() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_property_value\" : {\"$matches\" : \"*is is st*\"}})");
        assertSize(queryResult, 1);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactsByPropertyAndNotMatch() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"repo\" : {\"$not_equals\" : \"repo1    jkkj\"}})");
        assertSize(queryResult, 26);
        assertArtifact(queryResult, "repo1", "ant", "ant", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactsByPropertyValueAndDefaultEquals() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_property_value\" : \"this is string\"})");
        assertSize(queryResult, 1);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactsByRepoAndDefaultEquals() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\"})");
        assertSize(queryResult, 14);
        assertArtifact(queryResult, "repo1", "ant", "ant", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactsByRepoEqualsAndPathMatches() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\", \"_artifact_path\" : {\"$matches\" : \"ant*\"}})");
        assertSize(queryResult, 4);
        assertArtifact(queryResult, "repo1", "ant", "ant", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    public void findArtifactsByArtifactTypeFolder() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager("artifacts.find({\"_artifact_type\" :0})");
        assertSize(queryResult, 15);
        assertArtifact(queryResult, "repo1", ".", ".", 0);
        assertArtifact(queryResult, "repo1", "ant/ant", "1.5", 0);
    }

    public void findArtifactsByRepoEqualsAndArtifactTypeFIle() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\": \"repo1\" , \"_artifact_type\" :1})");
        assertSize(queryResult, 4);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "file2.bin", 1);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactsByPropertyValueAndPropertyKey() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"*\" : {\"$equals\": \"ant\"} , \"build.name\" : {\"$equals\" : \"*\"}})");
        assertSize(queryResult, 2);
        assertArtifact(queryResult, "repo1", ".", ".", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactsUsingRepoFieldPropertyKeyPropertyValueShortcut() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\": \"repo1\" , \"*\" : {\"$equals\" : \"ant\"} , \"build.name\" : {\"$equals\" : \"*\"}})");
        assertSize(queryResult, 2);
        assertArtifact(queryResult, "repo1", ".", ".", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactsUsingRepoFieldAndProperty() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\": \"repo1\" , \"build.name\" : \"ant\"})");
        assertSize(queryResult, 2);
        assertArtifact(queryResult, "repo1", ".", ".", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactsUsingRepoFieldAndDates() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\": [{\"_artifact_repo\": \"repo1\"} , {\"_artifact_type\": 1}]} , {\"_artifact_created\" : {\"$greater\" : \"1970-01-19 00:00:00\"}}]})");
        assertSize(queryResult, 26);
        assertArtifact(queryResult, "repo1", ".", ".", 0);
        assertArtifact(queryResult, "repo1", ".", "ant", 0);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "file2.bin", 1);
        assertArtifact(queryResult, "repo2", ".", "aa", 0);
    }

    @Test
    public void findArtifactsUsingRepoFieldAndDatesAndLess() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\": [{\"repo\": \"none\"} , {\"type\": 1}]} , {\"created\" : {\"$less\" : \"1970-01-19 00:00:00\"}}]})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactsUsingRepoFieldAndDatesAndAndOperator() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\": \"repo1\" , \"$or\" : [ " +
                        "{\"created\" : {\"$less\" : \"1970-01-19 00:00:00\"}}, {\"_artifact_created\" : {\"$greater\" : \"1970-01-19 00:00:00\"}}]})");
        assertSize(queryResult, 14);
        assertArtifact(queryResult, "repo1", "ant/ant", "1.5", 0);
        assertArtifact(queryResult, "repo1", "org/yossis", "tools", 0);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "test.bin", 1);
    }


    @Test
    public void findArtifactsUsingArchives() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_archive_entry_name\": {\"$matches\" : \"a*\"}})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactsUsingArchivesAndMatches() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_archive_entry_name\": {\"$matches\" : \"t*\"}})");
        assertSize(queryResult, 1);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "file2.bin", 1);
    }

    @Test
    public void findArtifactsUsingOrOperatorAndAndOperator() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\" : [{\"repo\": \"repo1\"},{\"gffgfgf\" : \"yossis\"}]},{\"jungle\": {\"$equals\" : \"*\"}}]})");
        assertSize(queryResult, 1);
        assertArtifact(queryResult, "repo1", "org", "yossis", 0);
    }

    @Test
    public void findArtifactsUsingOrOperatorAndAndOperatorAndFieldsAndProperties() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\" : [{\"_artifact_repo\": \"repo1\"},{\"yossis\" : \"pdf\"}]},{\"jungle\":{\"$equals\" : \"*\"}}]})");
        assertSize(queryResult, 2);
        assertArtifact(queryResult, "repo1", "org", "yossis", 0);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "test.bin", 1);
    }

    @Test
    public void artifacts6() {

        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\" : [{\"_artifact_repo\" : \"repo1\"},{\"trance\": {\"$equals\" : \"*\"}}]},{\"hjhj\" : \"yossis\"}]})");
        assertSize(queryResult, 1);
        assertArtifact(queryResult, "repo1", "org", "yossis", 0);
    }

    @Test
    public void findArtifactsWithOrAndAndOperatorsAndArtifactsFieldsAndPropertyKeyMatching() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\" : [{\"_artifact_repo\" : \"repo1\"},{\"$or\" : [{\"build.name\" : \"ant\"},{\"jungle\": {\"$equals\" : \"*\"}}]}]},{\"path\" : {\"$matches\" : \"x*\"}}]})");
        assertSize(queryResult, 3);
        assertArtifact(queryResult, "repo1", ".", ".", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
        assertArtifact(queryResult, "repo1", "org", "yossis", 0);
    }

    @Test
    public void findArtifactsWithCanonicalAndOrOperatorsAndArtifactsFieldsAndPropertyKeyMatching() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\" : [{\"_artifact_repo\" : \"repo1\"},{\"$or\" : [{\"build.name\" : {\"$not_equals\" : \"ant\"}},{\"jungle\": {\"$not_equals\" : \"*\"}}]}]},{\"path\" : {\"$matches\" : \"x*\"}}]})");
        assertSize(queryResult, 14);
        assertArtifact(queryResult, "repo1", ".", ".", 0);
        assertArtifact(queryResult, "repo1", "org/yossis", "empty", 0);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "test.bin", 1);
    }

    @Test
    public void findArtifactsWithCanonicalAndOperatorsAndArtifactsFieldsAndPropertyKeyMatchingAndNorEqual() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\" : [{\"_artifact_repo\" : \"repo1\"},{\"$and\" : [{\"build.name\" : {\"$not_equals\" : \"ant\"}},{\"jungle\": {\"$not_equals\" : \"*\"}}]}]},{\"path\" : {\"$matches\" : \"x*\"}}]})");
        assertSize(queryResult, 13);
        assertArtifact(queryResult, "repo1", ".", "ant", 0);
        assertArtifact(queryResult, "repo1", "ant", "ant", 0);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "file2.bin", 1);
    }

    @Test
    public void findArtifactsWithCanonicalAndOrOperatorsAndArtifactsFieldsUsingNotEquals() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$or\" : [{\"$and\" : [{\"_artifact_repo\" : \"repo1\"},{\"$or\" : [{\"build.name\" : {\"$not_equals\" : \"ant\"}},{\"jungle\": {\"$equals\" : \"*\"}}]}]},{\"path\" : {\"$matches\" : \"x*\"}}]})");
        assertSize(queryResult, 13);
        assertArtifact(queryResult, "repo1", "org", "yossis", 0);
        assertArtifact(queryResult, "repo1", "org/yossis", "tools", 0);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "file3.bin", 1);
    }

    @Test
    public void findArtifactsWithCanonicalAndOrOperatorsAndArtifactsFieldsAndArchiveFields() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\", \"$and\" : [{\"build.name\" : {\"$not_equals\" : \"ant\"}},{\"_archive_entry_name\": {\"$matches\" : \"file*\"}}]})");
        assertSize(queryResult, 1);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "test.bin", 1);
    }

    @Test
    public void findArtifactsWithCanonicalAndOrOperatorsAndArtifactsFieldsAndArchiveFieldsUsingNotEquals() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\", \"$and\" : [{\"build.name\" : {\"$not_equals\" : \"ant\"}},{\"_archive_entry_name\": {\"$matches\" : \"file*\"}},{\"archive+entry_name\": \"lll\"}]})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactsWithBuildFields() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\",\"_build_url\" : \"http://jkgkgkgk\"})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactsWithOrOperatorsAndArtifactsFieldsAndPropertyKeyMatching() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\", \"$or\" : [{\"yo*\" : {\"$matches\" : \"*\"}}, {\"*\" : {\"$matches\" : \"ant*\"}},{\"license\" : \"GPL\"}]})");
        assertSize(queryResult, 4);
        assertArtifact(queryResult, "repo1", "org", "yossis", 0);
        assertArtifact(queryResult, "repo1", ".", ".", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactWithShortEqualsQueriesAndNormalQueries() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\", \"_build_number\" : {\"$matches\" : \"1*\"}})");
        assertSize(queryResult, 1);
        assertArtifact(queryResult, "repo1", "org/yossis/tools", "test.bin", 1);
    }

    @Test
    public void findArtifactWithShortEqualsQueries1() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\", \"_build_dependency_type\" : \"dll\"})");
        assertSize(queryResult, 2);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactWithShortEqualsQueries2() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\", \"_artifact_downloaded_by\" : \"yossis\"})");
        assertSize(queryResult, 3);
        assertArtifact(queryResult, "repo1", "org", "yossis", 0);
        assertArtifact(queryResult, "repo1", ".", "ant-launcher", 0);
    }

    @Test
    public void findArtifactWithOrAndAndOperatorsArtifactFieldsAndProperties() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"_artifact_repo\" : \"repo1\", \"_artifact_downloaded_by\" : \"yossis\", \"$or\" : [{\"yo*\" : {\"$matches\" : \"*\"}} , { \"*\":{\"$matches\" : \"ant*\"}},{\"license\" : \"GPL\"}]})");
        assertSize(queryResult, 2);
        assertArtifact(queryResult, "repo1", "org", "yossis", 0);
        assertArtifact(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
    }

    @Test
    public void findArtifactWithJoin() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"$join\" : [{\"license\"  : {\"$matches\" : \"*GPL\"}}, {\"license\"  : {\"$not_equals\" : \"LGPL\"}}]})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactWithPropertyNotMatches() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"build.name\"  : {\"$not_matches\" : \"*GPL\"}})");
        assertSize(queryResult, 26);
    }

    @Test(enabled = false)
    public void findArtifactWithPropertyValueNotMatches() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"*\"  : {\"$not_matches\" : \"*GPL\"}})");
        assertSize(queryResult, 26);
    }

    @Test
    public void findArtifactWithPropertyKeyNotMatches() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"yossis\"  : {\"$not_matches\" : \"*\"}})");
        assertSize(queryResult, 25);
    }

    @Test
    public void findArtifactWithPropertyKeyNotMatchesAnyThing() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"hghghg\"  : {\"$not_matches\" : \"*\"}})");
        assertSize(queryResult, 26);
    }

    @Test
    public void artifactWithLimit() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"build.name\"  : {\"$not_matches\" : \"*\"}}).limit(2)");
        assertSize(queryResult, 2);
    }

    /*Properties search*/
    @Test
    public void findPropertiesMergingArtifactsFieldJoinAndOrOperatorUsingMatchesAndFilteringByPropertyKey() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"_artifact_repo\" : \"repo1\",\"$or\" : [{\"$join\" : [{\"b*\"  : {\"$matches\" : \"*\"}} , {\"e*\"  : {\"$matches\": \"*\"}},{\"asdasdasdas\" : \"dsdsadasd\"}]}]})");
        assertSize(queryResult, 5);
        assertProperty(queryResult, "empty.val", "");
        assertProperty(queryResult, "build.number", "67");
        assertProperty(queryResult, "build.name", "ant");
    }

    @Test
    public void purePropertiesSearch() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"yossis\"  : {\"$equals\" : \"*\"}})");
        assertSize(queryResult, 4);
        assertProperty(queryResult, "jungle", "value2");
        assertProperty(queryResult, "trance", "me");
    }


    @Test
    public void findPropertiesMergingArtifactsFieldsAndPropertyKeFiltering() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"_artifact_repo\" : \"repo1\",\"yossis\"  : {\"$equals\" : \"*\"}})");
        assertSize(queryResult, 4);
        assertProperty(queryResult, "yossis", "pdf");
    }

    @Test
    public void findPropertiesUsingJoinAndOrOperator() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"$join\" : [ {\"$or\" : [{\"trance\"  : {\"$equals\" : \"*\"}},{\"yossis\"  : {\"$equals\" : \"*\" }}]}]})");
        assertSize(queryResult, 4);
        assertProperty(queryResult, "trance", "me");
        assertProperty(queryResult, "yossis", "pdf");
        assertProperty(queryResult, "yossis", "value1");
    }

    @Test
    public void findPropertiesFilteringByKey() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"yo*\"  : {\"$matches\" : \"*\"}})");
        assertSize(queryResult, 4);
        assertProperty(queryResult, "yossis", "pdf");
        assertProperty(queryResult, "yossis", "value1");
    }

    @Test
    public void findPropertiesUsingJoin() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"$join\" : [{\"$or\" : [{\"yo*\" : {\"$matches\" : \"*\"}}, {\"*\" : {\"$matches\" : \"*o\"}}, {\"_artifact_path\" : {\"$matches\" : \"org*\"}}]}]})");
        assertSize(queryResult, 6);
        assertProperty(queryResult, "empty.val", "");
        assertProperty(queryResult, "trance", "me");
    }

    @Test
    public void findPropertiesMergingFieldsArchiveAndEqualls() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"_archive_entry_name\" : \"llklk\"})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findPropertiesMergingFieldsArchiveAndMaching() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"_archive_entry_name\" : {\"$matches\" :\"*.xml\"}})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findPropertiesMergingFieldsFromStatisticsAndPropertiesAnfArtifactsAndBuildUsingDates() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"_artifact_repo\" :\"repo1\" ,  \"_build_created\" : {\"$greater\" :\"1970-01-19 00:00:00\"}})");
        assertSize(queryResult, 1);
        assertProperty(queryResult, "yossis", "pdf");
    }

    @Test
    public void findPropertiesMergingFieldsFromStatisticsAndPropertiesAnfArtifactsUsingDates() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"_artifact_repo\" :\"repo1\" ,  \"_build_created\" : {\"$less\" :\"1970-01-19 00:00:00\"}})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findPropertiesMergingFieldsFromStatisticsAndProperties() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties.find({\"_artifact_repo\" :\"repo1\" , \"_artifact_downloaded_by\" :\"yossis\", \"$or\" : [ {\"yo*\" : {\"$matches\" : \"*\"} , \"*\" : {\"$matches\" : \"ant*\"}, \"license\" : \"GPL\"}]})");
        assertSize(queryResult, 6);
        assertProperty(queryResult, "yossis", "value1");
        assertProperty(queryResult, "build.name", "ant");
    }

    @Test
    public void findPropertiesUsingBamesExtension() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "properties({\"$names\" : [\"build.name\"]}).find({\"_property_value\" : {\"$matches\" : \"*is is st*\"}})");
        assertSize(queryResult, 1);
        assertProperty(queryResult, "build.name", "ant");
    }

    /*Statistics search*/

    @Test
    public void findStatistics() {
        //AqlQueryResult queryResult = aqlService.executeQueryEager("find statistics filter by repo=repo1");
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "statistics.find({\"_artifact_repo\" :\"repo1\"})");
        assertSize(queryResult, 3);
        assertStatistics(queryResult, "ant-launcher", "repo1", "yossis");
    }

    /*build dependencies*/
    @Test
    public void findBuildDependenciesMergingArtifactFieldsAndBuildFields1() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_dependencies.find({\"_artifact_repo\" :\"repo1\",\"_build_number\" : 1})");
        assertSize(queryResult, 2);
        assertDependencies(queryResult, "ant/ant/1.5", "repo1", "ant-1.5.jar");
    }

    @Test
    public void findBuildDependenciesMergingArtifactFieldsAndBuildFields2() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_dependencies.find({\"_artifact_repo\" :\"repo1\",\"_build_number\" : \"1\"})");
        assertSize(queryResult, 2);
        assertDependencies(queryResult, "ant/ant/1.5", "repo1", "ant-1.5.jar");
    }

    @Test
    public void findBuildDependenciesMergingArtifactFieldsAndBuildFieldsAndBuildPropertiesFields() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_dependencies.find({\"_artifact_repo\" :\"repo1\",\"_build_number\" : 1 , \"_build_property_value\" :\"bad\"})");
        assertSize(queryResult, 1);
        assertDependencies(queryResult, "org/yossis/tools", "repo1", "file3.bin");
    }

    /*build artifacts*/
    @Test
    public void findBuildArtifactsMergingArtifactFieldsAndBuildFieldsAndBuildPropertiesFields() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_artifacts.find({\"_artifact_repo\" :\"repo1\",\"_build_number\" : 1 , \"_build_property_value\" :\"bad\"})");
        assertSize(queryResult, 1);
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "ba1mod1-art1");
    }

    @Test
    public void findBuildArtifactsMergingArtifactFieldsAndBuildFieldsAndBuildFields1() {
        //AqlQueryResult queryResult = aqlService.executeQueryEager("find build_artifacts filter by repo=repo1 and build_name=ba");
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_artifacts.find({\"_artifact_repo\" :\"repo1\",\"_build_name\" : \"ba\"})");
        assertSize(queryResult, 2);
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "ba1mod1-art1");
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "ba1mod1-art1");
    }

    @Test
    public void findBuildArtifactsMergingArtifactFieldsAndBuildFieldsAndBuildFields2() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_artifacts.find({\"_artifact_repo\" :\"repo1\",\"_build_number\" : 1})");
        assertSize(queryResult, 3);
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "ba1mod1-art1");
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "bb1mod2-art1");
    }

    /*sort*/
    @Test
    public void  findBuildArtifactsSortAsc() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_artifacts.find().sort({\"$asc\" : [\"_build_artifact_name\", \"_artifact_repo\"]})");
        assertSize(queryResult, 4);
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "ba1mod1-art1");
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "bb1mod2-art1");
    }

    @Test
    public void  findBuildArtifactsSortDesc() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_artifacts.find().sort({\"$desc\" : [\"_build_artifact_name\", \"_artifact_repo\"]})");
        assertSize(queryResult, 4);
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "ba1mod1-art1");
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "bb1mod2-art1");
    }

    @Test
    public void  findBuildArtifactsWithExtraFieldsAndSorting() {
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_artifacts.find({\"_artifact_repo\" : \"repo1\"}).sort({\"$desc\" : [\"_build_artifact_name\", \"_artifact_repo\"]})");
        assertSize(queryResult, 4);
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "ba1mod1-art1");
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "bb1mod2-art1");
    }

    /*complex string comparator */
    @Test
    public void sortWithcomplexStringComparator() {
        //AqlQueryResult queryResult = aqlService.executeQueryEager("find build_artifacts filter by repo match r*p* sort by build_artifact_name and repo gl");
        AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
                "build_artifacts.find({\"_artifact_repo\" :{\"$matches\" : \"r*p*1\"}}).sort({\"$desc\" : [\"_build_artifact_name\", \"_artifact_repo\"]})");
        assertSize(queryResult, 4);
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "ba1mod1-art1");
        assertBuildArtifacts(queryResult, "org/yossis/tools", "repo1", "test.bin", "bb1mod2-art1");
    }

    @Test
    public void failOnSortThatContainsNoneResultField() {
        try {
            aqlService.executeQueryEager(
                    "artifacts.find({\"_artifact_repo\" : \"repo1\"}).sort({\"$desc\" : [\"_build_number\", \"_artifact_repo\"]})");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(
                    "Only the following result fields are allowed to use in the sort section"));
        }
    }

    @Test
    public void rejectionSortByValue1() {
        try {
            aqlService.executeQueryEager(
                    "artifacts.find({\"_artifact_repo\" : \"repo1\"}).sort({\"$desc\" : [\"dsdsdsd\", \"_artifact_repo\"]})");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(
                    "it looks like there is syntax error near the following sub-query: dsdsdsd"));
        }
    }

    private void assertArtifact(AqlQueryResultIfc queryResult, String repo, String path, String name, int type) {
        boolean found = false;
        for (int j = 0; j < queryResult.getSize(); j++) {
            AqlBaseArtifact row = (AqlBaseArtifact) queryResult.getResult(j);
            if (row.getRepo().equals(repo) && row.getName().equals(name) &&
                    row.getPath().equals(path) && row.getType() == type) {
                found = true;
            }
        }
        Assert.assertTrue(found);
    }

    private void assertProperty(AqlQueryResultIfc queryResult, String key, String value) {
        boolean found = false;
        for (int j = 0; j < queryResult.getSize(); j++) {
            AqlProperty row = (AqlProperty) queryResult.getResult(j);
            if (row.getKey().equals(key) && (StringUtils.isBlank(row.getValue()) && StringUtils.isBlank(value) ||
                    (!StringUtils.isBlank(row.getValue()) && row.getValue().equals(value)))) {
                found = true;
            }
        }
        Assert.assertTrue(found);
    }

    private void assertDependencies(AqlQueryResultIfc queryResult, String path, String repo, String name) {
        boolean found = false;
        for (int j = 0; j < queryResult.getSize(); j++) {
            AqlArtifactWithBuildArtifacts row = (AqlArtifactWithBuildArtifacts) queryResult.getResult(j);
            if (row.getPath().equals(path) &&
                    row.getPath().equals(path) && row.getRepo().equals(repo) &&
                    row.getName().equals(name)) {
                found = true;
            }
        }
        Assert.assertTrue(found);
    }

    private void assertBuildArtifacts(AqlQueryResultIfc queryResult, String path, String repo, String name,
            String buildArtifactName) {
        boolean found = false;
        for (int j = 0; j < queryResult.getSize(); j++) {
            AqlArtifactWithBuildArtifacts row = (AqlArtifactWithBuildArtifacts) queryResult.getResult(j);
            if (row.getPath().equals(path) &&
                    row.getRepo().equals(repo) && row.getName().equals(name) &&
                    row.getBuildArtifactName().equals(buildArtifactName)) {
                found = true;
            }
        }
        Assert.assertTrue(found);
    }

    private void assertStatistics(AqlQueryResultIfc queryResult, String name, String repo, String downloadBy) {
        boolean found = false;
        for (int j = 0; j < queryResult.getSize(); j++) {
            AqlArtifactsWithStatistics row = (AqlArtifactsWithStatistics) queryResult.getResult(j);
            if (row.getDownloadedBy().equals(downloadBy) &&
                    row.getName().equals(name) && row.getRepo().equals(repo)) {
                found = true;
            }
        }
        Assert.assertTrue(found);
    }

    private void assertSize(AqlQueryResultIfc queryResult, int i) {
        Assert.assertEquals(queryResult.getSize(), i);
    }
}
