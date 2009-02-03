package org.artifactory.cli.test;

import org.artifactory.cli.main.ArtifactoryCli;
import org.artifactory.cli.main.CliOption;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * TODO: documentation
 *
 * @author Noam Tenne
 */
public class TestNewCli {

    @BeforeClass
    public void dontExit() {
        ArtifactoryCli.DO_SYSTEM_EXIT = false;
    }

    @Test
    public void test() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{"help"});
        System.out.println("-----------------------------------------------------");
        cleanOptions();
        ArtifactoryCli.main(new String[]{"help", "import"});
        System.out.println("-----------------------------------------------------");
        cleanOptions();
        ArtifactoryCli.main(new String[]{"import"});
        System.out.println("-----------------------------------------------------");
        cleanOptions();
        ArtifactoryCli.main(new String[]{"help", "dump"});
        System.out.println("-----------------------------------------------------");
    }


    @Test
    public void testError() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "info",
                "--url", TestCli.API_ROOT,
                "--username",
                "--password", "password"
        });
    }

    @Test
    public void testUnknownOption() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "info",
                "--url", TestCli.API_ROOT,
                "--ssss"
        });
    }

    private static void cleanOptions() {
        CliOption[] cliOptions = CliOption.values();
        for (CliOption option : cliOptions) {
            option.setValue(null);
        }
    }
}
