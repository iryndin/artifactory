package org.artifactory.cli.command;

import org.artifactory.api.common.StatusEntry;
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
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
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
import java.util.concurrent.atomic.AtomicInteger;

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
        super(
                CommandDefinition.dump,
                CliOption.dest,
                CliOption.version,
                CliOption.verbose,
                CliOption.repos,
                CliOption.caches,
                CliOption.security,
                CliOption.noconvert);
    }

    /**
     * Executes the command
     *
     * @throws Exception
     */
    public int execute() throws Exception {
        try {
            if (getCommandArgument().isSet()) {
                System.out.println("Executing Artifactory DUMP command on " + getCommandArgument().getValue());
                exportFromDb();
                return 0;
            } else {
                usage();
                return 3;
            }
        } catch (Exception e) {
            System.err.println("Problem during Artifactory migration: " + e);
            e.printStackTrace();
            return 15;
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
        sb.append(
                "Artifactory will try to automcatically determine the previous version from $ARTIFACTORY_HOME/webapps/")
                .append(WEBAPPS_ARTIFACTORY_WAR).append(" file if present.\n");
        sb.append("If this file does not exist, please specify one of the following as a --version parameter:\n");
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
    private void exportFromDb() throws IOException, ParserConfigurationException, SAXException {
        ArtifactoryHome.setReadOnly(true);
        findAndSetArtifactoryHome();
        // Don't create artifactory home yet, we need to convert the config file

        // Find the version
        final ArtifactoryVersion version = getArtifactoryVersion();
        if (version == null) {
            log.error("Artifactory version could not be determined.");
            return;
        }

        // If nothing set for repos, set to all repo by default
        if (!CliOption.repos.isSet()) {
            CliOption.repos.setValue(EXPORT_ALL_NON_CACHED_REPOS);
        }

        // Set the system properties instead of artifactory.properties file
        System.setProperty(ConstantsValue.artifactoryVersion.getPropertyName(), version.getValue());
        System.setProperty(ConstantsValue.artifactoryRevision.getPropertyName(), "" + version.getRevision());

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

    private static class DumpStatusHolder extends StatusHolder {
        private static final Logger log = LoggerFactory.getLogger("DumpStatusHolder");
        private AtomicInteger doingDots = new AtomicInteger(0);
        private AtomicInteger warnings = new AtomicInteger(0);
        private AtomicInteger errors = new AtomicInteger(0);

        @Override
        protected void logEntry(StatusEntry entry, Logger logger) {
            if (!isVerbose()) {
                // Activate dots only if not verbose
                if (entry.isDebug()) {
                    int dots = doingDots.incrementAndGet();
                    if (dots == 80) {
                        doingDots.getAndAdd(-80);
                        System.out.println(".");
                    } else {
                        System.out.print(".");
                    }
                } else {
                    int dots = doingDots.getAndSet(0);
                    if (dots > 0) {
                        System.out.println("");
                    }
                }
            }

            // Ignore logger parameter, use only mine
            String statusMessage = entry.getStatusMessage();
            if (entry.isWarning()) {
                warnings.incrementAndGet();
                log.warn(statusMessage);
            } else if (entry.isError()) {
                errors.incrementAndGet();
                Throwable throwable = entry.getException();
                if (isVerbose()) {
                    log.error(statusMessage, throwable);
                } else {
                    log.error(statusMessage);
                }
            } else if (entry.isDebug()) {
                if (isVerbose()) {
                    log.debug(statusMessage);
                }
            } else {
                log.info(statusMessage);
            }
        }

        public int getWarnings() {
            return warnings.get();
        }

        public int getErrors() {
            return errors.get();
        }
    }

    private void export(ApplicationContext context)
            throws IOException, SAXException, ParserConfigurationException {
        if (!CliOption.dest.isSet()) {
            CliOption.dest.setValue("dumpExport");
        }
        File exportDir = new File(CliOption.dest.getValue());
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        log.info("Dumping into destination folder=[" + exportDir.getAbsolutePath() + "]");
        DumpStatusHolder status = new DumpStatusHolder();
        File result;
        ImportableExportable securityExporter = UpdateUtils.getSecurityExporter(context);
        ExportSettings settings = new ExportSettings(exportDir);
        settings.setVerbose(CliOption.verbose.isSet());
        settings.setFailFast(CliOption.failOnError.isSet());
        settings.setFailIfEmpty(CliOption.failIfEmpty.isSet());
        // First sync status and settings
        status.setFailFast(settings.isFailFast());
        status.setVerbose(settings.isVerbose());
        if (CliOption.security.isSet()) {
            // only export the security settings
            securityExporter.exportTo(settings, status);
            if (status.isError()) {
                throw new RuntimeException(status.getStatusMsg(), status.getException());
            }
            result = status.getCallback();
            int nbWarnings = status.getWarnings();
            int nbErrors = status.getErrors();
            if (status.isError()) {
                log.error("Security export into [{}] failed with {} errors and {} warnings.\n" +
                        "Last error was {}\n" +
                        "Check [cli.log] file for details!",
                        new Object[]{exportDir.getAbsolutePath(), nbErrors, nbWarnings, status.getLastError()});
            } else if (nbWarnings > 0) {
                log.warn("Security export into the file [{}] succeeded with {} warnings.\n" +
                        "Check [cli.log] file for details!", result.getAbsolutePath(), nbWarnings);
            } else {
                log.info("Security export into the file [{}] succeeded with no warnings.",
                        result.getAbsolutePath());
            }
            // Cannot have convert here for sure
            CliOption.noconvert.set();
        } else {
            ConfigExporter configExporter = UpdateUtils.getCentralConfigExporter(context);
            configExporter.exportTo(settings, status);
            securityExporter.exportTo(settings, status);
            if (CliOption.repos.isSet()) {
                JcrExporter jcrExporter = UpdateUtils.getJcrExporter(context);
                if (!CliOption.repos.getValue().equals(EXPORT_ALL_NON_CACHED_REPOS)) {
                    List<String> repos = PathUtils.delimitedListToStringList(CliOption.repos.getValue(), ",");
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
            int nbWarnings = status.getWarnings();
            int nbErrors = status.getErrors();
            if (status.isError()) {
                log.error("Full export into [{}] failed with {} errors and {} warnings.\n" +
                        "Last error was {}\n" +
                        "Check [cli.log] file for details!",
                        new Object[]{exportDir.getAbsolutePath(), nbErrors, nbWarnings, status.getLastError()});
            } else if (nbWarnings > 0) {
                log.warn("Full export into [{}] succeeded with {} warnings.\n" +
                        "Check [cli.log] file for details!", result.getAbsolutePath(), nbWarnings);
            } else {
                log.info("Full export into [{}] succeeded with no warnings.",
                        result.getAbsolutePath());
            }
        }

        // Above v130beta1 no need for convert, and don't convert if failed
        if (!status.isError() && VersionsHolder.getOriginalVersion().before(ArtifactoryVersion.v130beta1)) {
            if (CliOption.noconvert.isSet()) {
                log.info(CliOption.noconvert.argValue() +
                        " has been specified! No conversion of local repository to virtual done.");
            } else {
                ArtifactoryConfigUpdate.migrateLocalRepoToVirtual(exportDir);
            }
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
