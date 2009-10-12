package org.artifactory.cli.common;

import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.ArtifactoryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The main base class for CLI commands
 *
 * @author Noam Tenne
 */
public abstract class BaseCommand implements Command {
    private final static Logger log = LoggerFactory.getLogger(BaseCommand.class);
    /**
     * The Command definition of the command
     */
    private final CommandDefinition commandDefinition;
    /**
     * Array of the parameters which can be used with this command
     */
    private final CliOption[] optionalParameters;
    /**
     * Core path
     */
    public static final String LIB_ARTIFACTORY_CORE = "WEB-INF/lib/artifactory-core";
    /**
     * War path
     */
    public static final String WEBAPPS_ARTIFACTORY_WAR = "artifactory.war";

    /**
     * Default constructor
     */
    public BaseCommand(CommandDefinition commandDefinition, CliOption... optionalParameters) {
        Assert.notNull(commandDefinition, "Command object cannot be null");
        this.commandDefinition = commandDefinition;
        this.optionalParameters = optionalParameters;
    }

    /**
     * Prints the command usage
     */
    protected void defaultUsage() {
        StringBuilder sb = new StringBuilder();
        addCommandDesc(sb);
        addCommandUsage(sb);
        addCommandOptions(sb);
        System.out.println(sb.toString());
    }

    /**
     * Prints the command description
     *
     * @param sb
     */
    protected void addCommandDesc(StringBuilder sb) {
        sb.append(commandDefinition.getCommandParam().getName()).append(": ")
                .append(commandDefinition.getCommandParam().getDescription()).append("\n");
    }

    /**
     * Prints the command usage
     */
    protected void addCommandUsage(StringBuilder sb) {
        sb.append("usage: ").append(commandDefinition.getCommandParam().getName());
        if (commandDefinition.getCommandParam().isNeedExtraParam()) {
            sb.append(" [")
                    .append(commandDefinition.getCommandParam().getParamDescription()).append("]");
        }
        sb.append(" ...\n");
    }

    /**
     * Prints the command options
     */
    protected void addCommandOptions(StringBuilder sb) {
        if ((optionalParameters != null) && (optionalParameters.length > 0)) {
            sb.append("Valid options:\n");
            for (CliOption cliOption : optionalParameters) {
                sb.append("  --").append(cliOption.getName());
                if (cliOption.getParamDescription() != null) {
                    sb.append(" [").append(cliOption.getParamDescription()).append("]");
                }
                sb.append(": ").append(cliOption.getDescription());
                sb.append("\n");
            }
        }
    }

    /**
     * Returns the command's name
     *
     * @return Command's name
     */
    public String getName() {
        return getCommandArgument().getName();
    }

    /**
     * Returns the command's argument
     *
     * @return Command's argument
     */
    protected Param getCommandArgument() {
        return commandDefinition.getCommandParam();
    }

    /**
     * Returns one the command's associated CLI options via name. Returns null if there is no option found with the
     * given name.
     *
     * @param optionName The name of the option
     * @return The requested CLI option
     */
    public CliOption getOption(String optionName) {
        for (CliOption optionalParameter : optionalParameters) {
            if (optionalParameter.getName().equalsIgnoreCase(optionName)) {
                return optionalParameter;
            }
        }
        return null;
    }

    /**
     * Returns all the parameters that the command supports
     *
     * @return All optional parameters
     */
    public CliOption[] getOptionalParams() {
        return optionalParameters;
    }

    /**
     * Analyzes the given arguments and extracts the commands, options and arguments
     *
     * @param args Arguments given in the CLI
     * @return Boolean value representing the validity of the given arguments
     */
    public boolean analyzeParameters(String[] args) {
        int i = 0;
        Param argument = getCommandArgument();
        i = checkExtraParam("command", argument, args, i);
        if (i == -1) {
            return false;
        }

        // After this point we have a commandDefinition usage and getOption go to commandDefinition
        for (; i < args.length; i++) {
            String arg = getCleanArgument(args[i]);
            if (!arg.startsWith(OptionInfo.OPTION_PREFIX)) {
                System.out.println("Error parsing parameter " + arg);
                usage();
                return false;
            }
            final String optionName = arg.substring(OptionInfo.OPTION_PREFIX.length());
            Option option = getOption(optionName);
            if (option == null) {
                System.out.println("Unknown argument: " + arg);
                usage();
                return false;
            }

            i = checkExtraParam("option", option, args, i);
            if (i == -1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks one parameter ahead (to extract the values)
     *
     * @param paramType Param type (<command> or [option])
     * @param param     The class of the certain option
     * @param args      The arguments given in the CLI
     * @param i         The current location of the iterator
     * @return
     */
    private int checkExtraParam(String paramType, Param param, String[] args, int i) {
        if (param.isNeedExtraParam()) {
            i++;
            if (i >= args.length) {
                System.out.println("The " + paramType + " " + param + " needs a parameter: " +
                        param.getParamDescription());
                usage();
                return -1;
            }
            String arg = getCleanArgument(args[i]);
            param.setValue(arg);
            if (param.getValue().startsWith(OptionInfo.OPTION_PREFIX)) {
                // Unset on error
                param.setValue(null);
                System.out.println("The " + paramType + " " + param + " needs a parameter: " +
                        param.getParamDescription());
                usage();
                return -1;
            }
        } else {
            param.set();
        }
        if ("command".equals(paramType)) {
            i++;
        }
        return i;
    }

    /**
     * Returns the version class of the specified artifactory home
     *
     * @return ArtifactoryVersion Version class of chosen Artifactory
     * @throws java.io.IOException
     */
    @SuppressWarnings({"OverlyComplexMethod"})
    public ArtifactoryVersion getArtifactoryVersion() throws IOException {
        String versionName = null;
        if (CliOption.version.isSet()) {
            versionName = CliOption.version.getValue();
        } else {
            log.info("Finding version...");
            // First look for artifactory.properties in the data folder (works since 1.3.0-rc1)
            ArtifactoryHome.setDataAndJcrDir();
            //File propsFile = new File(ArtifactoryHome.getDataDir(), ArtifactoryHome.ARTIFACTORY_PROPERTIES_FILE);
            File webappsDir = ArtifactoryHome.getOrCreateSubDir("webapps");
            File warFile = new File(webappsDir, WEBAPPS_ARTIFACTORY_WAR);
            if (!warFile.exists()) {
                log.error("War file {} does not exists please put it there or give a version param", warFile);
                usage();
            }
            ZipFile zipFile = new ZipFile(warFile);
            Enumeration<? extends ZipEntry> warEntries = zipFile.entries();
            while (warEntries.hasMoreElements()) {
                ZipEntry zipEntry = warEntries.nextElement();
                String zipEntryName = zipEntry.getName();
                if (zipEntryName.startsWith(LIB_ARTIFACTORY_CORE) && zipEntryName.endsWith(".jar")) {
                    versionName = zipEntryName.substring(LIB_ARTIFACTORY_CORE.length() + 1, zipEntryName.length() - 4);
                    break;
                }
            }
            if (versionName == null) {
                log.error("Did not find the version in {} looking for {}.", warFile, LIB_ARTIFACTORY_CORE);
                usage();
            }
            log.info("Found version name " + versionName);
        }

        ArtifactoryVersion[] artifactoryVersions = ArtifactoryVersion.values();
        ArtifactoryVersion version = null;
        for (ArtifactoryVersion artifactoryVersion : artifactoryVersions) {
            if (artifactoryVersion.getValue().equals(versionName)) {
                version = artifactoryVersion;
                break;
            }
        }
        if (version == null) {
            log.error("Version {} is wrong or is not supported by this updater", versionName);
            log.error("If you know a good close version, please give a version param");
            usage();
            // Too avoid the may have NPE below
            return null;
        }
        if (version.isCurrent()) {
            log.error("Version " + versionName + " is the latest version, no update needed");
            log.error("If you know it's an old version, please give a version param");
            usage();
        }
        log.info("Found supported version " + version.getValue() + " revision " +
                version.getRevision());
        // If the version is before the xs:id usage print a warning message
        return version;
    }

    /**
     * Returns a File representation of the selected Artfiactory home
     *
     * @return File Location of Artifactory home
     */
    public void findAndSetArtifactoryHome() {
        String artifactoryHome = getCommandArgument().getValue();
        File artifactoryHomeDir = new File(artifactoryHome);
        if (!artifactoryHomeDir.exists() || !artifactoryHomeDir.isDirectory()) {
            log.error("Artifactory home " + artifactoryHomeDir.getAbsolutePath() +
                    " does not exists or is not a directory.");
            usage();
        }
        ArtifactoryHome.setHomeDir(artifactoryHomeDir);
        log.info("Artifactory Home dir=[" + artifactoryHomeDir.getAbsolutePath() + "]");
    }

    /**
     * Returns the given argument after being trimmed of the leading and trailing whitespaces
     *
     * @param argument Argument to trim
     * @return String - Trimmed argument
     */
    private String getCleanArgument(String argument) {
        return argument.trim();
    }
}
