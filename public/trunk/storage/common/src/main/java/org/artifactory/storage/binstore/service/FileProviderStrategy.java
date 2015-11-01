package org.artifactory.storage.binstore.service;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author Gidi Shabat
 */
public interface FileProviderStrategy {
    @Nonnull
    File createTempFile();

    @Nonnull
    File getFile(String sha1);

    File getBinariesDir();
}

