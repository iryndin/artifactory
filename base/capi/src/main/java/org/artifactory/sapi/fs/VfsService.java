package org.artifactory.sapi.fs;

import org.artifactory.sapi.common.ArtifactorySession;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.RequiresTransaction;
import org.artifactory.sapi.common.TxPropagation;

import java.io.File;
import java.io.InputStream;

/**
 * Date: 8/5/11
 * Time: 11:35 AM
 *
 * @author Fred Simon
 */
@RequiresTransaction(TxPropagation.REQUIRED)
public interface VfsService {

    boolean nodeExists(String absolutePath);

    boolean delete(String absolutePath);

    InputStream getStream(String absolutePath);

    String getContentAsString(String absolutePath);

    MetadataReader fillBestMatchMetadataReader(ImportSettings importSettings, File metadataFolder);

    void createIfNeeded(String absolutePath);

    @RequiresTransaction(TxPropagation.NEVER)
    ArtifactorySession getUnmanagedSession();
}
