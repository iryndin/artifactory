package org.artifactory.api.common;

import org.artifactory.common.StatusEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * User: gidis
 */
public class ImportExportStatusHolder extends MultiStatusHolder {
    protected static final Logger log = LoggerFactory.getLogger(ImportExportStatusHolder.class);

    protected void logEntry(@Nonnull StatusEntry entry, @Nonnull Logger logger) {
        if (isVerbose()) {
            doLogEntry(entry, log);
        }
        doLogEntry(entry, logger);
    }
}
