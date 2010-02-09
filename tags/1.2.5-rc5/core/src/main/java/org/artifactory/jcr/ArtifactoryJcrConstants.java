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
package org.artifactory.jcr;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ArtifactoryJcrConstants {
    String NT_ARTIFACTORY_FILE = "artifactory:file";
    String NT_ARTIFACTORY_FOLDER = "artifactory:folder";
    String NT_ARTIFACTORY_JAR = "artifactory:jar";
    String NT_ARTIFACTORY_XML_CONTENT = "artifactory:xmlcontent";
    String MIX_ARTIFACTORY_XML_AWARE = "artifactory:xmlAware";
    String MIX_ARTIFACTORY_POM_AWARE = "artifactory:pomAware";
    String MIX_ARTIFACTORY_REPO_AWARE = "artifactory:repoAware";
    String MIX_ARTIFACTORY_CACHEABLE = "artifactory:cacheable";
    String MIX_ARTIFACTORY_AUDITABLE = "artifactory:auditable";
    String MIX_ARTIFACTORY_STATS_AWARE = "artifactory:statsAware";
    String PROP_ARTIFACTORY_NAME = "artifactory:name";
    String PROP_ARTIFACTORY_REPO_KEY = "artifactory:repoKey";
    String PROP_ARTIFACTORY_LAST_UPDATED = "artifactory:lastUpdated";
    String PROP_ARTIFACTORY_MODIFIED_BY = "artifactory:modifiedBy";
    String PROP_ARTIFACTORY_JAR_ENTRY = "artifactory:jarEntry";
    String PROP_ARTIFACTORY_DOWNLOAD_COUNT = "artifactory:downloadCount";
    String ARTIFACTORY_XML = "artifactory:xml";
}
