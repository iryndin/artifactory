package org.artifactory.addon.opkg;

import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.repo.RepoPath;

import java.util.Collection;
import java.util.Set;

/**
 * @author Dan Feldman
 */
public class HaOpkgMessage implements HaMessage {

    public static final String HA_FAILED_MSG = "Failed to send Opkg calculation message to server";

    private Set<OpkgCalculationEvent> newEvents;
    private Collection<RepoPath> propertyWriterEntries;
    private boolean async;

    public HaOpkgMessage(Set<OpkgCalculationEvent> newEvents, Collection<RepoPath> propertyWriterEntries, boolean async) {
        this.newEvents = newEvents;
        this.propertyWriterEntries = propertyWriterEntries;
        this.async = async;
    }

    public Set<OpkgCalculationEvent> getNewEvents() {
        return newEvents;
    }

    public Collection<RepoPath> getPropertyWriterEntries() {
        return propertyWriterEntries;
    }

    public boolean isAsync() {
        return async;
    }
}
