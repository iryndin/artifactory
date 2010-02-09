package org.artifactory.descriptor.repo;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlType;

/**
 * @author Eli Givoni
 */
@XmlType(name = "PomRepositoryReferencesCleanupPolicy", namespace = Descriptor.NS)
public enum PomCleanupPolicy {
    discard_active_reference("Discard Active References"),
    discard_any_reference("Discard Any References"),
    nothing("Nothing");

    private String message;

    PomCleanupPolicy(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
