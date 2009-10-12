/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.descriptor.repo;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
@XmlEnum(String.class)
public enum SnapshotVersionBehavior {
    @XmlEnumValue("unique")UNIQUE("Unique"),
    @XmlEnumValue("non-unique")NONUNIQUE("Non-unique"),
    @XmlEnumValue("deployer")DEPLOYER("Deployer");

    //The name to display when used in different components
    private String displayName;

    /**
     * Sets the display name of the element
     *
     * @param displayName The display name
     */
    SnapshotVersionBehavior(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name of the element
     *
     * @return String - Element display name
     */
    public String getDisplayName() {
        return displayName;
    }
}