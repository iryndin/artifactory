package org.artifactory.webapp.wicket.utils;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MutableBoolean implements Serializable {
    private boolean value;

    public MutableBoolean() {
    }

    public MutableBoolean(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return isValue();
    }

    public boolean isValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }
}
