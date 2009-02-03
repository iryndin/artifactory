package org.artifactory.cli.command;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.cli.common.BaseCommand;
import org.artifactory.cli.common.Command;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.update.VersionsHolder;
import org.artifactory.update.config.ArtifactoryConfigUpdate;
import org.artifactory.update.config.ConfigExporter;
import org.artifactory.update.jcr.JcrExporter;
import org.artifactory.update.utils.UpdateUtils;
import org.artifactory.utils.ExceptionUtils;
import org.artifactory.utils.PathUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The "Dump" command class
 *
 * @author Noam Tenne
 */
public class DumpCommand extends BaseCommand implements Command {
    /**
     * Logger
     */
    private final static Logger log = LoggerFactory.getLogger(DumpCommand.class);
    /**
     * Non-Chached repo option
     */
    public static final String EXPORT_ALL_NON_CACHED_REPOS = "all-non-cached";

    /**
     * Constructor
     */
    public DumpCommand() {
        super(CommandDefinition.dump, CliOption.dest, CliOption.version, CliOption.caches,
                CliOption.security, CliOption.repo, CliOption.norepo,
                CliOption.convert, CliOption.noconvert);
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    public void execute() throws Exception {
        try {
            if (getCommandArgument().isSet()) {
                System.out.println("Executing Artifactory DUMP command on " + getCommandArgument().getValue());
                exportFromDb();
            } else {
                usage();
            }
        } catch (Exception e) {
            System.err.println("Problem during Artifactory migration: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Prints the usage of the command
     */
    public void usage() {
        defaultUsage();
    }

    /**
     * Overrride the command usage specification to insert more details than default
     *
     * @param sb The String Builder used for output
     */
    @Override
    protected void addCommandUsage(StringBuilder sb) {
        super.addCommandUsage(sb);
        sb.append("The Artifactory version will be extracted from ${artifactory.home}/webapps/").
                append(WEBAPPS_ARTIFACTORY_WAR).append(" if present\n");
        sb.append("If the war file is not located there, please do:\n");
        sb.append("1) link or copy it at this location, or pass the version.\n");
        sb.append("2) pass one of the following version as second parameter:\n");
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        int i = 0;
        for (ArtifactoryVersion version : versions) {
            if (version.isCurrent()) {
                //Not valid
                continue;
            }
            sb.append("  ").append(version.getValue());
            if (i % 5 == 4) {
                sb.append("\n");
            }
            i++;
        }
        sb.append("\n");
    }

    /**
     * Exports data from the specified data base
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void exportFromDb()
            throws IOException, ParserConfigurationException, SAXException {
        ArtifactoryHome.setReadOnly(true);
        findAndSetArtifactoryHome();
        // Don't create artifactory home yet, we need to convert the config file

        // Find the version
        final ArtifactoryVersion version = getArtifactoryVersion();

        // If nothing set between norepo and repo set to all repo by default
        if (!CliOption.repo.isSet() && !CliOption.norepo.isSet()) {
            CliOption.repo.setValue(EXPORT_ALL_NON_CACHED_REPOS);
        }

        // Above v130beta1 no need for convert
        if (version.ordinal() >= ArtifactoryVersion.v130beta1.ordinal()) {
            if (!CliOption.convert.isSet() && !CliOption.noconvert.isSet()) {
                CliOption.noconvert.set();
            }
        } else {
            if (!CliOption.convert.isSet() && !CliOption.noconvert.isSet()) {
                CliOption.convert.set();
            }
        }

        // Set the system properties instead of artifactory.properties file
        System.setProperty(ConstantsValue.artifactoryVersion.getPropertyName()
                , version.getValue());
        System.setProperty(ConstantsValue.artifactoryRevision.getPropertyName(),
                "" + version.getRevision());

        VersionsHolder.setOriginalVersion(version);
        VersionsHolder.setFinalVersion(ArtifactoryVersion.getCurrent());
        UpdateUtils.initArtifactoryHome();

        AbstractApplicationContext context = null;
        try {
            context = UpdateUtils.getSpringContext();
            //Main job
            export(context);
        } catch (BeansException e) {
            // Find if the original exception is due to xs:id conversion
            SAXParseException saxException = extractSaxException(e);
            if (saxException != null &&
                    saxException.getMessage().contains("is not a valid value for 'NCName'")) {
                System.err.println(
                        "You got this error because repository keys needs to respect XML ID specification");
                System.err.println(
                        "Please execute the Artifactory Updater with the same JVM parameters.");
                System.err.println("You should have a list of -D" + ConstantsValue
                        .substituteRepoKeys.getPropertyName() + "oldRepoKey=newRepoKey");
                System.err.println(
                        "Please refer to http://wiki.jfrog.org/confluence/display/RTF/Upgrading+Artifactory");
                throw saxException;
            }
            throw e;
        } finally {
            if (context != null) {
                context.destroy();
            }
        }
    }

    private void export(ApplicationContext context)
            throws IOException, SAXException, ParserConfigurationException {
        if (!CliOption.dest.isSet()) {
            CliOption.dest.setValue("tmpExport");
        }
        File exportDir = new File(CliOption.dest.getValue());
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        log.info("Doing an export on dir=[" + exportDir.getAbsolutePath() + "]");
        StatusHolder status = new StatusHolder();
        File result;
        ImportableExportable securityExporter = UpdateUtils.getSecurityExporter(context);
        ExportSettings settings = new ExportSettings(exportDir);
        if (CliOption.security.isSet()) {
            // only export the security settings
            securityExporter.exportTo(settings, status);
            if (status.isError()) {
                throw new RuntimeException(status.getStatusMsg(), status.getException());
            }
            result = status.getCallback();
            log.info("Did a security export into the file [" + result.getAbsolutePath() + "]");
            // Cannot have convert here for sure
            CliOption.noconvert.set();
        } else {
            ConfigExporter configExporter = UpdateUtils.getCentralConfigExporter(context);
            configExporter.exportTo(settings, status);
            securityExporter.exportTo(settings, status);
            if (CliOption.repo.isSet()) {
                JcrExporter jcrExporter = UpdateUtils.getJcrExporter(context);
                if (!CliOption.repo.getValue().equals(EXPORT_ALL_NON_CACHED_REPOS)) {
                    List<String> repos = PathUtils.delimitedListToStringList(
                            CliOption.repo.getValue(), ",", "\r\n\t\f ");
                    jcrExporter.setRepositoriesToExport(repos);
                } else if (CliOption.caches.isSet()) {
                    jcrExporter.setIncludeCaches(true);
                }
                jcrExporter.exportTo(settings, status);
            }
            if (status.isError()) {
                throw new RuntimeException(status.getStatusMsg(), status.getException());
            }
            result = exportDir;
            log.info("Did a full export in [" + result.getAbsolutePath() + "]");
        }

        if (CliOption.noconvert.isSet()) {
            log.info("Param " + CliOption.noconvert.argValue() +
                    " passed! No conversion of local repository to virtual done.");
        } else {
            ArtifactoryConfigUpdate.migrateLocalRepoToVirtual(exportDir);
        }
    }


    /**
     * This method is for the sake of supressing unchecked assignment
     *
     * @param e
     * @return
     */
    @SuppressWarnings({"unchecked"})
    private SAXParseException extractSaxException(BeansException e) {
        return (SAXParseException) ExceptionUtils.getCauseOfTypes(e, SAXParseException.class);
    }
}
