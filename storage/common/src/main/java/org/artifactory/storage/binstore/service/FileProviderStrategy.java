package org.artifactory.storage.binstore.service;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * @author Gidi Shabat
 */
public interface FileProviderStrategy {
    @Nonnull
    File createTempFile();

    @Nonnull
    File getFile(String sha1);

    File getBinariesDir();

    boolean isFileExists(String sha1);
}

