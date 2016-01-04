package org.artifactory.storage.db.aql.service;

import org.artifactory.aql.result.AqlEagerResult;
import org.testng.annotations.Test;

/**
 * @author Gidi Shabat
 */
public class AqlArchiveTest  extends AqlAbstractServiceTest {

    @Test
    public void archiveWithBuilds() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "archive.entries.find({\"archive.item.repo\" : \"repo1\"," +
                        "\"archive.item.artifact.module.build.name\" : {\"$match\":\"*\"}})");
        assertSize(queryResult, 1);
        assertArchive(queryResult,  "path","file.file");
    }

    @Test
    public void buildsWithArchive() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "builds.find({\"module.artifact.item.archive.entry.name\" :{\"$match\":\"*\"}})");
        assertSize(queryResult, 3);
        assertBuild(queryResult, "ba", "1");
        assertBuild(queryResult, "bb", "1");
        assertBuild(queryResult, "ba", "2");
    }

    @Test
    public void archiveWithBuildsWithInclude() {
        AqlEagerResult queryResult = aqlService.executeQueryEager(
                "archive.entries.find({\"archive.item.repo\" : \"repo1\"," +
                        "\"archive.item.artifact.module.build.name\" : {\"$match\":\"*\"}})" +
                        ".include(\"archive.item\")");
        assertSize(queryResult, 1);
        assertArchive(queryResult,  "path","file.file");
    }

}