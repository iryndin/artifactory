package org.artifactory.resource;

import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public enum PackagingType {
    jar, pom;
    public static final List<PackagingType> LIST = Arrays.asList(PackagingType.values());
}
