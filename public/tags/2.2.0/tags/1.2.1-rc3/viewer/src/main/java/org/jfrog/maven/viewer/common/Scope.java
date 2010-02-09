package org.jfrog.maven.viewer.common;

import org.apache.maven.artifact.Artifact;

/**
 * Created by IntelliJ IDEA.
 * User: Dror Bereznitsky
 * Date: Apr 18, 2007
 * Time: 12:30:09 AM
 */
public enum Scope {
    COMPILE (Artifact.SCOPE_COMPILE),
    TEST(Artifact.SCOPE_TEST),
    RUNTIME(Artifact.SCOPE_RUNTIME),
    PROVIDED(Artifact.SCOPE_PROVIDED),
    SYSTEM(Artifact.SCOPE_SYSTEM);
    
    private final String name;

    Scope(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
