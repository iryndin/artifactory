package org.artifactory.repo;

import javax.xml.bind.annotation.XmlType;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XmlType(name = "SnapshotVersionBehaviorType")
public enum SnapshotVersionBehavior {
    unique, nonunique, deployer
}
