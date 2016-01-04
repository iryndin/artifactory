package org.artifactory.addon.docker;

import java.util.Map;

/**
 * @author Dan Feldman
 */
public class DockerLabel {

    private String key;
    private String value;

    public DockerLabel(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public DockerLabel(Map.Entry<String, String> dockerV2InfoLabelEntry) {
        this.key = dockerV2InfoLabelEntry.getKey();
        this.value = dockerV2InfoLabelEntry.getValue();
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
