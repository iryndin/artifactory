package org.artifactory.addon.debian;

import org.artifactory.addon.ha.message.HaMessage;

import java.util.Set;

/**
 * @author Gidi Shabat
 */
public class HaDebianMessage implements HaMessage {
    private Set<DebianCalculationEvent> newEvents;
    private boolean async;

    public HaDebianMessage(Set<DebianCalculationEvent> newEvents, boolean async) {
        this.newEvents = newEvents;
        this.async =async;
    }

    public Set<DebianCalculationEvent> getNewEvents() {
        return newEvents;
    }

    public void setNewEvents(Set<DebianCalculationEvent> newEvents) {
        this.newEvents = newEvents;
    }

    public boolean isAsync() {
        return async;
    }
}
