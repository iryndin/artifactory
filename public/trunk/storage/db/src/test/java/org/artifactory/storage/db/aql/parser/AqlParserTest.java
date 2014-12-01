package org.artifactory.storage.db.aql.parser;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Gidi Shabat
 */
@Test
public class AqlParserTest {
    @Test
    public void successParse() throws Exception {
        AqlParser sm = new AqlParser();
        assertValid(sm, "artifacts.find({\"_artifact_repo\":{\"$matches\":\"jc*\"}})");
        assertValid(sm, "artifacts.find({\"license\":{\"$equals\":\"GPL\"}})");
        assertValid(sm, "artifacts.find({\"license\":{\"$equals\":\"GPL\"}})");
        assertValid(sm,
                "artifacts.find({\"license\":{\"$equals\":\"GPL\"},\"license\":{\"$equals\":\"GPL\"},\"$or\":[{\"license\":{\"$equals\":\"GPL\"}}]})");
        assertValid(sm,
                "artifacts.find({\"license\":{\"$equals\":\"GPL\"},\"license\":{\"$less\":\"GPL\"},\"$or\":[{\"license\":{\"$equals\":\"GPL\"},\"license\":{\"$equals\":\"GPL\"}}]})");
        assertValid(sm,
                "artifacts.find({\"license\":{\"$equals\":\"GPL\"},\"$or\":[{\"license\":{\"$equals\":\"GPL\"},\"license\":{\"$equals\":\"GPL\"},\"license\":{\"$equals\":\"GPL\"}}]})");
        assertValid(sm, "artifacts.find({\"$or\":[{\"license\":\"GPL\",\"license\":\"GPL\"}]})");
        assertValid(sm,
                "artifacts.find({\"$or\":[{\"_artifact_repo\":\"jcentral\"},{\"type\":1},{\"$or\":[{\"$or\":[{\"version\":\"1,1,1\"},{\"type\":1}]},{\"_version\":\"2.2.2\"}]}]})");
        assertValid(sm, "artifacts.find({\"$join\":[{\"_artifact_repo\":\"jcentral\"},{\"type\":1}]})");
        assertValid(sm, "artifacts.find({\"$join\":[{\"_artifact_repo\":\"jcentral\",\"type\":1}]})");
        assertValid(sm, "artifacts.find({\"_artifact_repo\":\"jcentral\",\"type\":1})");
        assertValid(sm, "artifacts.find({\"_artifact_repo\":\"jcentral\"},{\"type\":1})");
        assertValid(sm, "artifacts.find({\"$join\":[{\"_artifact_repo\":\"jcentral\"},{\"type\":1}]})");
        assertValid(sm,
                "artifacts.find({\"$join\":[{\"_artifact_repo\":\"jcentral\"},{\"_artifact_type\":1}]}).sort({\"$asc\" : [\"_artifact_name\", \"_artifact_repo\" ]})");
        assertValid(sm, "artifacts.find({\"*\" : {\"$equals\" : \"1.1*\"}})");
        assertValid(sm, "artifacts.find({\"ver*\" : {\"$equals\" : \"*\" }})");
        assertValid(sm, "artifacts({\"$names\" : [\"version\",\"test\" ]}).find({\"ver*\" : {\"$equals\" : \"*\"}})");
        assertValid(sm, "artifacts.find()");
        assertValid(sm, "artifacts.find({\"*\" : {\"$equals\" : \"1.1*\"}}).limit(10)");
    }

    @Test
    public void failureOnParse() throws Exception {
        AqlParser sm = new AqlParser();
        assertInValid(sm, "artifacts({\"$names\" : [}).find({\"ver*\" : \"$equals\"})");
        assertInValid(sm, "artifacts({\"$names\").find({\"ver*\" : \"$equals\"})");
        assertInValid(sm, "artifacts().find({\"ver*\" : \"$equals\"})");
        assertInValid(sm, "artifacts.find({\"ver*\" : \"$equals\"})");
    }

    private void assertValid(AqlParser sm, String script) throws Exception {
        ParserElementResultContainer parse = sm.parse(script);
        System.out.println(parse);
    }

    private void assertInValid(AqlParser sm, String script) throws Exception {
        try {
            sm.parse(script);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }
}
