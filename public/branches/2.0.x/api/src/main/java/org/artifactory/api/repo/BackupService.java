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
package org.artifactory.api.repo;

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.descriptor.repo.RealRepoDescriptor;

import java.io.File;
import java.util.List;

/**
 * User: freds Date: Aug 5, 2008 Time: 9:25:33 PM
 */
public interface BackupService {
    void backupRepos(File backupDir, ExportSettings exportSettings, MultiStatusHolder status);

    void backupRepos(File backupDir, List<RealRepoDescriptor> excludeRepositories,
            StatusHolder status, ExportSettings exportSettings);
}
