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
package org.artifactory.backup;

import org.artifactory.api.repo.BackupService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.ReloadableBean;

import java.util.Date;

/**
 * @author freds
 * @date Oct 27, 2008
 */
public interface InternalBackupService extends ReloadableBean, BackupService {
    /**
     * @param context   The internal artifactory context
     * @param backupIndex   The index of the backup in the backups list
     * @return true if backup was successful
     */
    boolean backupSystem(InternalArtifactoryContext context, int backupIndex);

    /**
     * Iterate (non-recursively) on all folders/files in the backup dir and delete them if they are
     * older than "now" minus the retention period of the beckup.
     *
     * @param now   The base time to use for the cleanup
     * @param backupIndex The index of the backup in the backups list
     */
    void cleanupOldBackups(Date now, int backupIndex);
}
