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
package org.artifactory.spring;

import org.artifactory.config.CentralConfig;
import org.artifactory.config.ExportableConfig;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.security.ArtifactorySecurityManager;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public interface ArtifactoryContext extends ExportableConfig {
    CentralConfig getCentralConfig();

    <T> T beanForType(Class<T> type);

    ArtifactorySecurityManager getSecurity();

    JcrWrapper getJcr();

    void exportTo(File exportDir, boolean createArchive, Date time, StatusHolder status);

    void exportTo(File basePath, List<LocalRepo> reposToExport, boolean createArchive,
                  Date time, StatusHolder status);
}
