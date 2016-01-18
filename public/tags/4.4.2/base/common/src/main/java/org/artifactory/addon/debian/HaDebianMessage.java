package org.artifactory.addon.debian;

import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.repo.RepoPath;

import java.util.Collection;
import java.util.Set;

/**
 * @author Gidi Shabat
 */
public class HaDebianMessage implements HaMessage {
    private Set<DebianCalculationEvent> newEvents;
    private Collection<RepoPath> propertyWriterEntries;
    private boolean async;

    public HaDebianMessage(Set<DebianCalculationEvent> newEvents, Collection<RepoPath> propertyWriterEntries, boolean async) {
        this.newEvents = newEvents;
        this.propertyWriterEntries = propertyWriterEntries;
        this.async =async;
    }

    public Set<DebianCalculationEvent> getNewEvents() {
        return newEvents;
    }

    public Collection<RepoPath> getPropertyWriterEntries() {
        return propertyWriterEntries;
    }

    public void setNewEvents(Set<DebianCalculationEvent> newEvents) {
        this.newEvents = newEvents;
    }

    public boolean isAsync() {
        return async;
    }
}
