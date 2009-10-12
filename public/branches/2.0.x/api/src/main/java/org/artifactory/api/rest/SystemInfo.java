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
package org.artifactory.api.rest;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.config.VersionInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * User: freds Date: Aug 12, 2008 Time: 6:15:10 PM
 */
@XStreamAlias("system")
public class SystemInfo {
    public VersionInfo artifactoryVersion;
    public String configFilePath;
    public Map<String, String> jvm = new HashMap<String, String>();
    public SystemActionInfo actionExample;

    /*
    @Override
    public String toString() {
        return "version=" + artifactoryVersion + "\n" +
                "configFilePath=" + configFilePath + "\n" +
                "jvm=" + jvm;
    }
    */

    public String toString() {
        return "SystemInfo{" +
                "artifactoryVersion=" + artifactoryVersion +
                ", configFilePath='" + configFilePath + '\'' +
                ", jvm=" + jvm +
                ", actionExample=" + actionExample +
                '}';
    }
}
