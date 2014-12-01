package org.artifactory.storage.db.aql.api;

import org.artifactory.aql.AqlException;
import org.artifactory.aql.api.AqlApi;
import org.artifactory.aql.api.AqlArtifactApi;
import org.artifactory.aql.api.AqlPropertyApi;
import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.result.AqlQueryResultIfc;
import org.artifactory.aql.result.rows.AqlArtifact;
import org.artifactory.aql.result.rows.AqlBaseArtifact;
import org.artifactory.aql.result.rows.AqlProperty;
import org.artifactory.storage.db.aql.service.AqlServiceImpl;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.artifactory.aql.api.AqlApi.*;
import static org.artifactory.aql.api.AqlApi.artifactPath;
import static org.artifactory.aql.api.AqlApi.artifactSize;
import static org.artifactory.aql.api.AqlApi.propertyKey;
import static org.artifactory.aql.api.AqlApi.propertyValue;
import static org.artifactory.aql.model.AqlComparatorEnum.*;
import static org.artifactory.aql.model.AqlField.artifactName;
import static org.artifactory.aql.model.AqlField.artifactRepo;
import static org.artifactory.aql.model.AqlField.propertyKey;
import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

/**
 * @author Gidi Shabat
 */
@Test
public class AqlApiTest extends DbBaseTest {
    @Autowired
    private AqlServiceImpl aqlService;

    @BeforeClass
    public void setup() {
        importSql("/sql/aql_test.sql");
    }

    /*Artifacts search*/

    public void findAllArtifactsTest() throws AqlException {
        AqlArtifactApi aql = findArtifacts();
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 26);
    }

    public void findAllSortedArtifactsTest() throws AqlException {
        AqlArtifactApi aql = findArtifacts().sortBy(artifactRepo);
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 26);
    }

    public void findArtifactsWithSort() throws AqlException {
        AqlArtifactApi aql = findArtifacts().
                filter(
                        artifactPath(matches, "org*")
                ).
                sortBy(artifactName, artifactRepo).
                asc();
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 9);
        assertArtifact(result, "repo-copy", "org/shayy/badmd5", "badmd5.jar", 1);
        assertArtifact(result, "repo1", "org/yossis", "empty", 0);
    }

    @Test
    public void findArtifactsWithOr() throws AqlException {
        AqlArtifactApi aql = findArtifacts().
                filter(
                        or(
                                property("yossia", matches, "*d"),
                                propertyValue(equals, "ant")
                        )
                ).sortBy(artifactName).desc();
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 2);
        assertArtifact(result, "repo1", "ant/ant/1.5", "ant-1.5.jar", 1);
        assertArtifact(result, "repo1", ".", ".", 0);
    }

    @Test
    public void findArtifactsWithAnd() throws AqlException {
        AqlArtifactApi aql = findArtifacts().
                filter(
                        and(
                                property("yossis", matches, "*ue1"),
                                propertyValue(equals, "value2")
                        )
                ).sortBy(artifactName);
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 1);
        assertArtifact(result, "repo1", "org", "yossis", 0);
    }

    @Test
    public void findArtifactsWithAndOr() throws AqlException {
        AqlArtifactApi aql = findArtifacts().
                filter(
                        and(
                                property("yossis", matches, "*ue1"),
                                or(
                                        propertyValue(equals, "value1"),
                                        property("yossis", matches, "*df"))
                        )
                );
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 1);
        assertArtifact(result, "repo1", "org", "yossis", 0);
    }

    @Test
    public void multipleCriteriaOnSamePropertyRow() throws AqlException {
        AqlArtifactApi aql = findArtifacts().
                filter(
                        AqlApi.freezeJoin(
                                propertyKey(matches, "jun*"),
                                propertyKey(matches, "*gle")
                        )
                ).sortBy(artifactName, artifactRepo);
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 1);
        assertArtifact(result, "repo1", "org", "yossis", 0);
    }

    @Test
    public void findAllProperties() throws AqlException {
        AqlPropertyApi aql = findProperties();
        AqlQueryResultIfc<AqlProperty> result = aqlService.executeQueryEager(aql);
        assertSize(result, 9);
    }

    @Test
    public void findPropertiesWithFieldFilter() throws AqlException {
        // return only the properties with the key 'yossis' from repository 'repo1'
        AqlPropertyApi aql = findProperties().
                filter(
                        AqlApi.artifactRepo(equals, "repo1")
                )
                .addResultFilter(AqlField.propertyKey, "yossis")
                .sortBy(propertyKey);
        AqlQueryResultIfc<AqlProperty> result = aqlService.executeQueryEager(aql);
        assertSize(result, 2);
        assertProperty(result, "yossis", "pdf");
        assertProperty(result, "yossis", "value1");
    }

    @Test
    public void singleWildCardMaching() throws AqlException {
        // return only the properties with the key 'yossis' from repository 'repo1'
        AqlPropertyApi aql = findProperties().
                filter(
                        AqlApi.artifactRepo(matches, "rep?1")
                )
                .addResultFilter(AqlField.propertyKey, "yossis")
                .sortBy(propertyKey);
        AqlQueryResultIfc<AqlProperty> result = aqlService.executeQueryEager(aql);
        assertSize(result, 2);
        assertProperty(result, "yossis", "pdf");
        assertProperty(result, "yossis", "value1");
    }

    @Test(enabled = false)
    public void underscoreEscape() throws AqlException {
        // return only the properties with the key 'yossis' from repository 'repo1'
        AqlPropertyApi aql = findProperties().
                filter(
                        AqlApi.artifactRepo(matches, "rep_1")
                )
                .addResultFilter(AqlField.propertyKey, "yossis")
                .sortBy(propertyKey);
        AqlQueryResultIfc<AqlProperty> result = aqlService.executeQueryEager(aql);
        assertSize(result, 0);
    }

    @Test(enabled = false)
    public void percentEscape() throws AqlException {
        // return only the properties with the key 'yossis' from repository 'repo1'
        AqlPropertyApi aql = findProperties().
                filter(
                        AqlApi.artifactRepo(matches, "rep%1")
                )
                .addResultFilter(AqlField.propertyKey, "yossis")
                .sortBy(propertyKey);
        AqlQueryResultIfc<AqlProperty> result = aqlService.executeQueryEager(aql);
        assertSize(result, 0);
    }

    public void findArtifactsBiggerThan() throws AqlException {
        AqlArtifactApi aql = findArtifacts().
                filter(
                        artifactSize(greater, 43434)
                );
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 5);
        for (AqlArtifact artifact : result.getResults()) {
            assertThat(artifact.getSize()).isGreaterThan(43434);
        }
    }

    public void findArtifactsBiggerThanWithLimit() throws AqlException {
        AqlArtifactApi aql = findArtifacts().
                filter(
                        artifactSize(greater, 43434)
                )
                .limit(2);
        AqlQueryResultIfc<AqlArtifact> result = aqlService.executeQueryEager(aql);
        assertSize(result, 2);
        for (AqlArtifact artifact : result.getResults()) {
            assertThat(artifact.getSize()).isGreaterThan(43434);
        }
    }

    private void assertSize(AqlQueryResultIfc queryResult, int i) {
        Assert.assertEquals(queryResult.getSize(), i);
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
        assertTrue(found);
    }

    private void assertProperty(AqlQueryResultIfc queryResult, String key, String value) {
        boolean found = false;
        for (int j = 0; j < queryResult.getSize(); j++) {
            AqlProperty row = (AqlProperty) queryResult.getResult(j);
            if (row.getKey().equals(key) &&
                    row.getValue().equals(value)) {
                found = true;
            }
        }
        assertTrue(found);
    }

}

