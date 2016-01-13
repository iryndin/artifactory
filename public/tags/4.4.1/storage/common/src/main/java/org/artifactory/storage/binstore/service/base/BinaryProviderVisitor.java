package org.artifactory.storage.binstore.service.base;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public interface BinaryProviderVisitor<T> {
    List<T> visit(BinaryProviderBase binaryProviderBase);
}
