package org.artifactory.cli.command;

import org.apache.commons.io.IOUtils;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * The "Configuration" command class
 *
 * @author Noam Tenne
 */
public class ConfigurationCommand extends UrlBasedCommand implements Command {

    /**
     * Default constructor
     */
    public ConfigurationCommand() {
        super(CommandDefinition.configuration, CliOption.destFile, CliOption.update, CliOption.overwrite);
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public int execute() throws Exception {
        String configurationUri = getURL() + "system/configuration";
        boolean isDestSet = CliOption.destFile.isSet();
        boolean isUpdateSet = CliOption.update.isSet();
        if (isDestSet && isUpdateSet) {
            System.err.println("Cannot execute command with both '--destFile' and '--update' options set." +
                    "Please execute them seperately.");
            return 3;
        }
        if (isDestSet) {
            String value = CliOption.destFile.getValue();
            File saveDestination = new File(value);
            if ((!saveDestination.exists()) ||
                    (CliOption.overwrite.isSet())) {
                saveDestination.createNewFile();
                FileOutputStream fos = new FileOutputStream(saveDestination.getPath());
                byte[] content = get(configurationUri, 200, null, false);
                try {
                    fos.write(content);
                }
                finally {
                    IOUtils.closeQuietly(fos);
                }

                System.out.println("Configuration was successfuly saved to: " + value);
            } else {
                System.err.println(
                        "Destination file already exists. If would like to overwrite it, run the command again" +
                                " with the '--overwrite' option.");
                return 3;
            }
        } else if (isUpdateSet) {
            String value = CliOption.update.getValue();
            File saveDestination = new File(value);
            if (saveDestination.exists() && saveDestination.isFile() && saveDestination.canRead()) {
                byte[] bytes = IOUtils.toByteArray(new FileInputStream(saveDestination));
                byte[] returnedBytes = post(configurationUri, bytes, "application/xml", 200, null, false);
                IOUtils.write(returnedBytes, System.out);
                IOUtils.write("\n", System.out);
            } else {
                throw new IllegalStateException(
                        "The specified path must be exist an existing file which is read permmited. " +
                                "Please make sure these conditions are met");
            }
        } else {
            get(configurationUri, 200, null, true);
        }
        return 0;
    }

    /**
     * Prints the usage of the command
     */
    public void usage() {
        defaultUsage();
    }
}
