package org.artifactory.storage.db.aql.service;

import org.artifactory.aql.result.AqlEagerResult;
import org.testng.annotations.Test;

import static org.artifactory.aql.model.AqlItemTypeEnum.file;
import static org.artifactory.aql.model.AqlItemTypeEnum.folder;


/**
 * @author Gidi Shabat
 */
@Test
public class AqlServiceTest extends AqlAbstractServiceTest {

    @Test
    public void complexQueryWithPropertiesSortJoinAndOr() {
        aqlService.executeQueryEager("items.find(" +
                "{" +
                "   \"$or\":[" +
                "               {\"repo\" : \"jcentral\"}," +
                "               {\"archive.item.repo\" : \"jcentral\"}," +
                        "               {\"name\" : {\"$match\" : \".*test\"}}," +
                        "               {\"type\" : \"file\"}," +
                "               {\"$and\":[" +
                        "                           {\"$msp\":[" +
                        "                                       {\"@version\":\"1.1.1\"}," +
                        "                                       {\"type\":\"file\"}" +
                "                                    ]" +
                "                           }," +
                "                           {" +
                        "                               \"@version\":\"2.2.2\"" +
                "                           }" +
                "                        ]" +
                "                }," +
                "               {\"repo\" : \"jcentral\"}" +
                "            ]," +
                "   \"repo\" : \"jcentral\"" +
                "}" +
                ")" +
                ".sort(" +
                "{" +
                "    \"$asc\" : [" +
                "                        \"name\",\"path\"" +
                "               ]" +
                "}" +
                ")"
        );
    }

    @Test
    public void findPropertiesFilteringByProperties() {
        aqlService.executeQueryEager("properties.find(" +
                "{" +
                "       \"@license\" : \"GPL\"," +
                "       \"@license\" : {\"$match\" : \"*GPL\"}" +
                "}" +
                ")");
    }

    @Test
    public void findPropertiesUsingNotMach() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"property.key\" : {\"$nmatch\" : \"a\"}})." +
                        "include(\"property.key\",\"property.value\",\"node\")");
        assertSize(queryResult, 31);
    }


    @Test
    public void findArtifactsFilterByPropertiesUsingDefaultAnd() {
        aqlService.executeQueryEager("items.find(" +
                "{" +
                "       \"@license\" : \"GPL\"," +
                "       \"@license\" : {\"$match\" : \"*GPL\"}" +
                "}" +
                ")");
    }

    @Test
    public void findArtifactsFilterByPropertiesUsingOrAndDefaultAnd() {
        aqlService.executeQueryEager("items.find(" +
                "{" +
                "       \"@license\" : \"GPL\"," +
                "       \"@license\" : {\"$match\" : \"*GPL\"}," +
                "       \"$or\" : [" +
                "                   {\"@.id\" : \"org.jfrog\"}," +
                "                   {\"@classifier\" : {\"$match\" : \"source\"}}" +
                "                 ]" +
                "}" +
                ")");
    }

    @Test
    public void findArtifactsFilterByPropertiesUsingJoinAndDefaultAnd() {
        aqlService.executeQueryEager("items.find(" +
                "{" +
                "       \"@license\" : \"GPL\"," +
                "       \"@license\" : {\"$match\" : \"*GPL\"}," +
                "       \"$msp\" : [" +
                "                   {\"@group.id\" : \"org.jfrog\"}," +
                "                   {\"@classifier\" : {\"$match\" : \"source\"}}" +
                "                 ]" +
                "}" +
                ")");
    }

    @Test
    public void findPropertiesFilterByPropertiesUsingJoinAndDefaultAnd1() {
        aqlService.executeQueryEager("items.find(" +
                "{" +
                "       \"@license\" : \"GPL\"," +
                "       \"@license\" : {\"$match\" : \"*GPL\"}," +
                "       \"$msp\" : [" +
                "                   {\"@group.id\" : \"org.jfrog\"}," +
                "                   {\"@classifier\" : {\"$match\" : \"source\"}}" +
                "                 ]" +
                "}" +
                ")");
    }

    @Test
    public void findPropertiesFilterByPropertiesUsingJoinAndDefaultAnd2() {
        aqlService.executeQueryEager("properties.find(" +
                "{" +
                "       \"@license\" : \"GPL\"," +
                "       \"@license\" : {\"$match\" : \"*GPL\"}," +
                "       \"$msp\" : [" +
                "                   {\"@group.id\" : \"org.jfrog\"}," +
                "                   {\"@classifier\" : {\"$match\" : \"source\"}}" +
                "                 ]," +
                "       \"item.repo\" : \"jcenter\"," +
                "       \"@version\" : \"1.1.1\"," +
                "       \"item.stat.downloads\" : 1" +
                "}" +
                ")");
    }
    /*Archive searches*/

    @Test
    public void findArtifactsByPropertyValueAndMatch() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"property.value\" : {\"$match\" : \"*is is st*\"}})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsByPropertyAndNotMatch() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"repo\" : {\"$ne\" : \"repo1    jkkj\"}})");
        assertSize(queryResult, 11);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsByPropertyValueAndDefaultEquals() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"property.value\" : \"this is string\"})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsByRepoAndDefaultEquals() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"repo\" : \"repo1\",\"type\" : \"any\"})");
        assertSize(queryResult, 14);
        assertItem(queryResult, "repo1", "ant", "ant", folder);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsByRepoEqualsAndPathMatches() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"repo\" : \"repo1\", \"path\" : {\"$match\" : \"ant*\"}})");
        assertSize(queryResult, 4);
        assertItem(queryResult, "repo1", "ant", "ant", folder);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsByArtifactTypeFolder() {
        AqlEagerResult queryResult = aqlService.executeQueryEager("items.find({\"type\" :\"folder\"})");
        assertSize(queryResult, 15);
        assertItem(queryResult, "repo1", ".", ".", folder);
        assertItem(queryResult, "repo1", "ant/ant", "1.5", folder);
    }

    @Test
    public void findArtifactsByRepoEqualsAndArtifactTypeFile() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"repo\": \"repo1\" , \"type\" :\"file\"})");
        assertSize(queryResult, 4);
        assertItem(queryResult, "repo1", "org/yossis/tools", "file2.bin", file);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsByPropertyValueAndPropertyKey() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"@*\" : {\"$eq\": \"ant\"} , \"@build.name\" : {\"$eq\" : \"*\"}})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsUsingRepoFieldPropertyKeyPropertyValueShortcut() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"repo\": \"repo1\" , \"@*\" : {\"$eq\" : \"ant\"} , \"@build.name\" : {\"$eq\" : \"*\"}})");
        assertSize(queryResult, 2);
        assertItem(queryResult, "repo1", ".", ".", folder);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsUsingRepoFieldAndProperty() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"repo\": \"repo1\" , \"@build.name\" : \"ant\"})");
        assertSize(queryResult, 2);
        assertItem(queryResult, "repo1", ".", ".", folder);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactsUsingRepoFieldAndDates() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"$or\" : [{\"$and\": [{\"repo\": \"repo1\"} , {\"type\": \"file\"}]} , {\"created\" : {\"$gt\" : \"1970-01-19T00:00:00.000Z\"}}]})");
        assertSize(queryResult, 26);
        assertItem(queryResult, "repo1", ".", ".", folder);
        assertItem(queryResult, "repo1", ".", "ant", folder);
        assertItem(queryResult, "repo1", "org/yossis/tools", "file2.bin", file);
        assertItem(queryResult, "repo2", ".", "aa", folder);
    }

    @Test
    public void findArtifactsUsingRepoFieldAndDatesAndLess() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"$or\" : [{\"$and\": [{\"repo\": \"none\"} , {\"type\": \"file\"}]} , {\"created\" : {\"$lt\" : \"1970-01-19T00:00:00.000Z\"}}]})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactsUsingRepoFieldAndDatesAndAndOperator() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"repo\": \"repo1\" , \"$or\" : [ " +
                        "{\"created\" : {\"$lt\" : \"1970-01-19T00:00:00.000Z\"}}, {\"created\" : {\"$gt\" : \"1970-01-19T00:00:00.000Z\"}}]})");
        assertSize(queryResult, 14);
        assertItem(queryResult, "repo1", "ant/ant", "1.5", folder);
        assertItem(queryResult, "repo1", "org/yossis", "tools", folder);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);
    }

    @Test
    public void findArtifactsWithPartialDate() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"repo\": \"repo1\" , \"$or\" : [ " +
                        "{\"created\" : {\"$lt\" : \"1970-01-19T00:00:00.000Z\"}}, {\"created\" : {\"$gt\" : \"1970-01\"}}]})");
        assertSize(queryResult, 14);
        assertItem(queryResult, "repo1", "ant/ant", "1.5", folder);
        assertItem(queryResult, "repo1", "org/yossis", "tools", folder);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);
    }


    @Test
    public void findArtifactsUsingArchives() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"archive.entry_name\": {\"$match\" : \"a*\"}})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactsUsingArchivesAndMatches() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"archive.entry_name\": {\"$match\" : \"t*\"}})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org/yossis/tools", "file2.bin", file);
    }

    @Test
    public void findArtifactsUsingOrOperatorAndAndOperator() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"$or\" : [{\"$and\" : [{\"repo\": \"repo1\"},{\"@gffgfgf\" : \"yossis\"}]},{\"@jungle\": {\"$eq\" : \"*\"}}]})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
    }

    @Test
    public void findArtifactsUsingOrOperatorAndAndOperatorAndFieldsAndProperties() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"$or\" : [{\"$and\" : [{\"repo\": \"repo1\"},{\"@yossis\" : \"pdf\"}]},{\"@jungle\":{\"$eq\" : \"*\"}}]})");
        assertSize(queryResult, 2);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);
    }

    @Test
    public void itemTypeAllAtTheBeginning() {

        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\":\"any\",\"$or\" : [{\"$and\" : [{\"repo\" : \"repo1\"},{\"@trance\": {\"$eq\" : \"*\"}}]},{\"@hjhj\" : \"yossis\"}]})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
    }

    @Test
    public void itemTypeAllAtTheEnd() {

        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"$or\" : [{\"$and\" : [{\"repo\" : \"repo1\"},{\"@trance\": {\"$eq\" : \"*\"}}]},{\"@hjhj\" : \"yossis\"}],\"type\":\"any\"})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
    }

    @Test
    public void itemTypeAllBeforeParenthesis() {

        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"$or\" : [{\"$and\" : [{\"repo\" : \"repo1\"},{\"@trance\": {\"$eq\" : \"*\"}}]},{\"@hjhj\" : \"yossis\"},{\"type\":\"any\"}],\"type\":\"any\"})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
    }

    @Test
    public void itemTypeAllAfterParenthesis() {

        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"$or\" : [{\"$and\" : [{\"type\":\"any\"},{\"repo\" : \"repo1\"},{\"@trance\": {\"$eq\" : \"*\"}}]},{\"@hjhj\" : \"yossis\"}],\"type\":\"any\"})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
    }

    @Test
    public void itemTypeDuplicate() {

        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"$or\" : [{\"$and\" : [{\"type\":\"any\"},{\"type\":\"any\"},{\"type\":\"any\"},{\"repo\" : \"repo1\"},{\"type\":\"any\"},{\"@trance\": {\"$eq\" : \"*\"}}]},{\"@hjhj\" : \"yossis\"}],\"type\":\"any\"})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
    }

    @Test
    public void itemTypeAllDuplicate() {

        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\":\"any\",\"$or\" : [{\"type\":\"any\"},{\"$and\" : [{\"type\":\"any\"},{\"type\":\"any\"},{\"type\":\"any\"},{\"type\":\"any\"},{\"type\":\"any\"},{\"type\":\"any\"}]},{\"type\":\"any\"}],\"type\":\"any\"})");
        assertSize(queryResult, 26);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
    }

    @Test
    public void findArtifactsWithOrAndAndOperatorsAndArtifactsFieldsAndPropertyKeyMatching() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"$or\" : [{\"$and\" : [{\"repo\" : \"repo1\"},{\"$or\" : [{\"@build.name\" : \"ant\"},{\"@jungle\": {\"$eq\" : \"*\"}}]}]},{\"path\" : {\"$match\" : \"x*\"}}]})");
        assertSize(queryResult, 3);
        assertItem(queryResult, "repo1", ".", ".", folder);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
    }

    @Test
    public void findArtifactsWithCanonicalAndOrOperatorsAndArtifactsFieldsAndPropertyKeyMatching() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"$or\" : [{\"$and\" : [{\"repo\" : \"repo1\"},{\"$or\" : [{\"@build.name\" : {\"$ne\" : \"ant\"}},{\"@jungle\": {\"$ne\" : \"*\"}}]}]},{\"path\" : {\"$match\" : \"x*\"}}]})");
        assertSize(queryResult, 14);
        assertItem(queryResult, "repo1", ".", ".", folder);
        assertItem(queryResult, "repo1", "org/yossis", "empty", folder);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);
    }

    @Test
    public void findArtifactsWithCanonicalAndOperatorsAndArtifactsFieldsAndPropertyKeyMatchingAndNorEqual() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"$or\" : [{\"$and\" : [{\"repo\" : \"repo1\"},{\"$and\" : [{\"@build.name\" : {\"$ne\" : \"ant\"}},{\"@jungle\": {\"$ne\" : \"*\"}}]}]},{\"path\" : {\"$match\" : \"x*\"}}]})");
        assertSize(queryResult, 13);
        assertItem(queryResult, "repo1", ".", "ant", folder);
        assertItem(queryResult, "repo1", "ant", "ant", folder);
        assertItem(queryResult, "repo1", "org/yossis/tools", "file2.bin", file);
    }

    @Test
    public void findArtifactsWithCanonicalAndOrOperatorsAndArtifactsFieldsUsingNotEquals() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"$or\" : [{\"$and\" : [{\"repo\" : \"repo1\"},{\"$or\" : [{\"@build.name\" : {\"$ne\" : \"ant\"}},{\"@jungle\": {\"$eq\" : \"*\"}}]}]},{\"path\" : {\"$match\" : \"x*\"}}]})");
        assertSize(queryResult, 13);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
        assertItem(queryResult, "repo1", "org/yossis", "tools", folder);
        assertItem(queryResult, "repo1", "org/yossis/tools", "file3.bin", file);
    }

    @Test
    public void findArtifactsWithCanonicalAndOrOperatorsAndArtifactsFieldsAndArchiveFields() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"repo\" : \"repo1\", \"$and\" : [{\"@build.name\" : {\"$ne\" : \"ant\"}},{\"archive.entry_name\": {\"$match\" : \"file*\"}}]})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);
    }

    @Test
    public void findArtifactsWithCanonicalAndOrOperatorsAndArtifactsFieldsAndArchiveFieldsUsingNotEquals() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"repo\" : \"repo1\", \"$and\" : [{\"@build.name\" : {\"$ne\" : \"ant\"}},{\"archive.entry_name\": {\"$match\" : \"file*\"}},{\"archive.entry_name\": \"lll\"}]})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactsWithBuildFields() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"repo\" : \"repo1\",\"artifact.module.build.url\" : \"http://jkgkgkgk\"})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactsWithOrOperatorsAndArtifactsFieldsAndPropertyKeyMatching() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"repo\" : \"repo1\", \"$or\" : [{\"@yo*\" : {\"$match\" : \"*\"}}, {\"@*\" : {\"$match\" : \"ant*\"}},{\"@license\" : \"GPL\"}]})");
        assertSize(queryResult, 4);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
        assertItem(queryResult, "repo1", ".", ".", folder);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactWithShortEqualsQueriesAndNormalQueries() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"repo\" : \"repo1\", \"artifact.module.build.number\" : {\"$match\" : \"1*\"}})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);
    }

    @Test
    public void findArtifactWithShortEqualsQueries1() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"repo\" : \"repo1\", \"dependency.type\" : \"dll\"})");
        assertSize(queryResult, 2);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactWithShortEqualsQueries2() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"repo\" : \"repo1\", \"stat.downloaded_by\" : \"yossis\"})");
        assertSize(queryResult, 3);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
        assertItem(queryResult, "repo1", ".", "ant-launcher", folder);
    }

    @Test
    public void findArtifactWithOrAndAndOperatorsArtifactFieldsAndProperties() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"type\" : \"any\",\"repo\" : \"repo1\", \"stat.downloaded_by\" : \"yossis\", \"$or\" : [{\"@yo*\" : {\"$match\" : \"*\"}} , { \"@*\":{\"$match\" : \"ant*\"}},{\"@license\" : \"GPL\"}]})");
        assertSize(queryResult, 2);
        assertItem(queryResult, "repo1", "org", "yossis", folder);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
    }

    @Test
    public void findArtifactWithJoin() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"$msp\" : [{\"@license\"  : {\"$match\" : \"*GPL\"}}, {\"@license\"  : {\"$ne\" : \"LGPL\"}}]})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findArtifactWithPropertyNotMatches() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"@build.name\"  : {\"$nmatch\" : \"*GPL\"}})");
        assertSize(queryResult, 11);
    }

    @Test(enabled = false)
    public void findArtifactWithPropertyValueNotMatches() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"@*\"  : {\"$nmatch\" : \"*GPL\"}})");
        assertSize(queryResult, 26);
    }

    @Test
    public void findArtifactWithPropertyKeyNotMatches() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"@yossis\"  : {\"$nmatch\" : \"*\"}})");
        assertSize(queryResult, 10);
    }

    @Test
    public void findArtifactWithPropertyKeyNotMatchesAnyThing() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"@hghghg\"  : {\"$nmatch\" : \"*\"}})");
        assertSize(queryResult, 11);
    }

    @Test
    public void artifactWithLimit() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"@build.name\"  : {\"$nmatch\" : \"*\"}}).limit(2)");
        assertSize(queryResult, 2);
    }

    /*Properties search*/
    @Test
    public void findPropertiesMergingArtifactsFieldJoinAndOrOperatorUsingMatchesAndFilteringByPropertyKey() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"item.repo\" : \"repo1\",\"$or\" : [{\"$msp\" : [{\"b*\"  : {\"$match\" : \"*\"}} , {\"e*\"  : {\"$match\": \"*\"}},{\"asdasdasdas\" : \"dsdsadasd\"}]}]})");
        assertSize(queryResult, 5);
        assertProperty(queryResult, "empty.val", "");
        assertProperty(queryResult, "build.number", "67");
        assertProperty(queryResult, "build.name", "ant");
    }

    @Test
    public void purePropertiesSearch() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"yossis\"  : {\"$eq\" : \"*\"}})");
        assertSize(queryResult, 4);
        assertProperty(queryResult, "jungle", "value2");
        assertProperty(queryResult, "trance", "me");
    }


    @Test
    public void findPropertiesMergingArtifactsFieldsAndPropertyKeFiltering() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"item.repo\" : \"repo1\",\"yossis\"  : {\"$eq\" : \"*\"}})");
        assertSize(queryResult, 4);
        assertProperty(queryResult, "yossis", "pdf");
    }

    @Test
    public void findPropertiesUsingJoinAndOrOperator() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"$msp\" : [ {\"$or\" : [{\"trance\"  : {\"$eq\" : \"*\"}},{\"yossis\"  : {\"$eq\" : \"*\" }}]}]})");
        assertSize(queryResult, 4);
        assertProperty(queryResult, "trance", "me");
        assertProperty(queryResult, "yossis", "pdf");
        assertProperty(queryResult, "yossis", "value1");
    }

    @Test
    public void findPropertiesFilteringByKey() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"yo*\"  : {\"$match\" : \"*\"}})");
        assertSize(queryResult, 4);
        assertProperty(queryResult, "yossis", "pdf");
        assertProperty(queryResult, "yossis", "value1");
    }

    @Test
    public void findPropertiesUsingJoin() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"$msp\" : [{\"$or\" : [{\"@yo*\" : {\"$match\" : \"*\"}}, {\"@*\" : {\"$match\" : \"*o\"}}, {\"item.path\" : {\"$match\" : \"org*\"}}]}]})");
        assertSize(queryResult, 6);
        assertProperty(queryResult, "empty.val", "");
        assertProperty(queryResult, "trance", "me");
    }

    @Test
    public void findPropertiesMergingFieldsArchiveAndEqualls() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"item.archive.entry_name\" : \"llklk\"})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findPropertiesMergingFieldsArchiveAndMaching() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"item.archive.entry_name\" : {\"$match\" :\"*.xml\"}})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findPropertiesMergingFieldsFromStatisticsAndPropertiesAnfArtifactsAndBuildUsingDates() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"item.repo\" :\"repo1\" ,  \"item.artifact.module.build.created\" : {\"$gt\" :\"1970-01-19T00:00:00.000Z\"}})");
        assertSize(queryResult, 1);
        assertProperty(queryResult, "yossis", "pdf");
    }

    @Test
    public void findPropertiesMergingFieldsFromStatisticsAndPropertiesAnfArtifactsUsingDates() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"item.repo\" :\"repo1\" ,  \"item.artifact.module.build.created\" : {\"$lt\" :\"1970-01-19T00:00:00.000Z\"}})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findPropertiesMergingFieldsFromStatisticsAndProperties() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "properties.find({\"item.repo\" :\"repo1\" , \"item.stat.downloaded_by\" :\"yossis\", \"$or\" : [ {\"yo*\" : {\"$match\" : \"*\"} , \"*\" : {\"$match\" : \"ant*\"}, \"license\" : \"GPL\"}]})");
        assertSize(queryResult, 6);
        assertProperty(queryResult, "yossis", "value1");
        assertProperty(queryResult, "build.name", "ant");
    }

    ///*Statistics search*/
    @Test
    public void findStatistics() {
        //AqlQueryResult queryResult = aqlService.executeQueryEager("find statistics filter by repo=repo1");
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "stats.find({\"item.repo\" :\"repo1\"},{\"downloads\":{\"$gte\":0}})");
        assertSize(queryResult, 2);
        assertStatistics(queryResult, 15, "yossis");
    }

    /*build dependencies*/
    @Test
    public void findBuildDependenciesMergingArtifactFieldsAndBuildFields1() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "dependencies.find({\"item.repo\" :\"repo1\",\"module.build.number\" : 1})");
        assertSize(queryResult, 2);
        assertDependencies(queryResult, "ba1mod3-art1", "compile", "dll");
        assertDependencies(queryResult, "bb1mod3-art1", "compile", "dll");
    }

    @Test
    public void findBuildDependenciesMergingArtifactFieldsAndBuildFields2() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "dependencies.find({\"item.repo\" :\"repo1\",\"module.build.number\" : \"1\"})");
        assertSize(queryResult, 2);
        assertDependencies(queryResult, "ba1mod3-art1", "compile", "dll");
        assertDependencies(queryResult, "bb1mod3-art1", "compile", "dll");
    }

    @Test
    public void findArchives() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "archives.find({\"item.repo\" :\"repo1\"})");
        assertSize(queryResult, 8);
        assertArchive(queryResult, "META-INF", "LICENSE.txt");
        assertArchive(queryResult, "META-INF", "MANIFEST.MF");
    }

    @Test
    public void findArchivesFilterByArchiveEntryPath() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "archives.find({\"item.repo\" :\"repo1\",\"entry_path\" :\"META\"})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findBuildDependenciesMergingArtifactFieldsAndBuildFieldsAndBuildPropertiesFields() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "dependencies.find({\"item.repo\" :\"repo1\",\"module.build.number\" : 1 , \"module.build.property.value\" :\"bad\"})");
        assertSize(queryResult, 1);
        assertDependencies(queryResult, "bb1mod3-art1", "compile", "dll");
    }

    @Test
    public void findBuildWithItemProperties() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "builds.find({\"module.artifact.item.repo\" :\"repo1\", \"module.artifact.item.@*\" :{\"$match\":\"*\"}})");
        assertSize(queryResult, 3);
        assertBuild(queryResult, "bb", "1");
    }

    /*build artifacts*/
    @Test
    public void findBuildArtifactsMergingArtifactFieldsAndBuildFieldsAndBuildPropertiesFields() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"item.repo\" :\"repo1\",\"module.build.number\" : 1 , \"module.build.property.value\" :\"bad\"})");
        assertSize(queryResult, 1);
        assertBuildArtifacts(queryResult, "ba1mod1-art1", "dll");
    }

    @Test
    public void findBuildArtifactsMergingArtifactFieldsAndBuildFieldsAndBuildFields1() {
        //AqlQueryResult queryResult = aqlService.executeQueryEager("find build_artifacts filter by repo=repo1 and build_name=ba");
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"item.repo\" :\"repo1\",\"module.build.name\" : \"ba\"})");
        assertSize(queryResult, 2);
        assertBuildArtifacts(queryResult, "ba1mod1-art1", "dll");
        assertBuildArtifacts(queryResult, "ba2mod4-art1", "dll");
    }

    @Test
    public void findBuildArtifactsMergingArtifactFieldsAndBuildFieldsAndBuildFields2() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "artifacts.find({\"item.repo\" :\"repo1\",\"module.build.number\" : 1})");
        assertSize(queryResult, 3);
        assertBuildArtifacts(queryResult, "ba1mod1-art1", "dll");
        assertBuildArtifacts(queryResult, "bb1mod3-art1", "dll");
        assertBuildArtifacts(queryResult, "bb1mod2-art1", "dll");
    }


    @Test
    public void checkOrBehaviour() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"archive.entry_name\" : {\"$match\": \"*txt\"}}).sort({\"$desc\" : [\"name\"]})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);

        queryResult = aqlService.executeQueryEager(
                "items.find({\"archive.entry_name\" : {\"$match\": \"*file\"}}).sort({\"$desc\" : [\"name\"]})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);

        queryResult = aqlService.executeQueryEager(
                "items.find({\"$or\":[{\"archive.entry_name\" : {\"$match\": \"*txt\"}},{\"archive.entry_name\" : {\"$match\": \"*file\"}}]}).sort({\"$desc\" : [\"name\"]})");
        assertSize(queryResult, 2);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);
    }

    @Test
    public void checkAndBehaviour() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"archive.entry_name\" : {\"$match\": \"*txt\"}}).sort({\"$desc\" : [\"name\"]})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "ant/ant/1.5", "ant-1.5.jar", file);

        queryResult = aqlService.executeQueryEager(
                "items.find({\"archive.entry_name\" : {\"$match\": \"*file\"}}).sort({\"$desc\" : [\"name\"]})");
        assertSize(queryResult, 1);
        assertItem(queryResult, "repo1", "org/yossis/tools", "test.bin", file);

        queryResult = aqlService.executeQueryEager(
                "items.find({\"$and\":[{\"archive.entry_name\" : {\"$match\": \"*txt\"}},{\"archive.entry_name\" : {\"$match\": \"*file\"}}]}).sort({\"$desc\" : [\"name\"]})");
        assertSize(queryResult, 0);
    }

    @Test
    public void findItemThatIsAnArtifactAndDependency() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "items.find({\"dependency.name\" : {\"$match\": \"*\"}}).sort({\"$desc\" : [\"name\"]})");
        assertSize(queryResult, 3);
        queryResult = aqlService.executeQueryEager(
                "items.find({\"artifact.name\" : {\"$match\": \"*\"}})");
        assertSize(queryResult, 1);
        queryResult = aqlService.executeQueryEager(
                "items.find({\"$or\":[{\"artifact.name\" : {\"$match\": \"*\"}},{\"dependency.name\" : {\"$match\": \"*\"}}]}).sort({\"$desc\" : [\"name\"]})");
        assertSize(queryResult, 4);
    }

    // Not really supported : We will need to support multi tables join not only in properties
    //@Test
    //public void findBuidThatHaveArtifactAndHaveDependency() {
    //    AqlQueryResultIfc queryResult = aqlService.executeQueryEager(
    //            "builds.find({\"module.dependency.item.name\" : {\"$match\": \"*\"}}).sort({\"$desc\" : [\"name\"]})");
    //    assertSize(queryResult, 1);
    //    queryResult = aqlService.executeQueryEager(
    //            "builds.find({\"module.artifact.item.name\" : {\"$match\": \"*\"}})");
    //    assertSize(queryResult, 1);
    //    queryResult = aqlService.executeQueryEager(
    //            "builds.find({\"$or\":[{\"module.artifact.item.name\" : {\"$match\": \"*\"}},{\"module.dependency.item.name\" : {\"$match\": \"*\"}}]}).sort({\"$desc\" : [\"name\"]})");
    //    assertSize(queryResult, 1);
    //}



}
