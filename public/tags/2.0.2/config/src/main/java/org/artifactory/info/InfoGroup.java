package org.artifactory.info;

/**
 * The main interface for the information groups of properties
 *
 * @author Noam Tenne
 */
public interface InfoGroup {

    /**
     * Returns all the info objects from the current group
     *
     * @return InfoObject[] - Collection of info objects from current group
     */
    public InfoObject[] getInfo();
}