package org.artifactory.aql.api;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public interface AqlApiElement {
    List<AqlApiElement> get();

    public boolean isEmpty();
}
