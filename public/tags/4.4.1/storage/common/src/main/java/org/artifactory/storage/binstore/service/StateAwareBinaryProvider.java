package org.artifactory.storage.binstore.service;

/**
 * @author Fred Simon
 */
public interface StateAwareBinaryProvider {

    boolean tryToActivate();

    boolean isActive();

}
