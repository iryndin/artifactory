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
package org.artifactory.repo.index;

import org.artifactory.worker.FutureWorkMessage;
import org.artifactory.worker.WorkAction;

import java.util.concurrent.Callable;

/**
 * @author freds
 * @date Oct 30, 2008
 */
public class SaveIndexMessage extends FutureWorkMessage<Boolean> {
    static class Action implements Callable<Boolean> {
        private final RepoIndexerData repoIndexerData;

        Action(RepoIndexerData repoIndexerData) {
            this.repoIndexerData = repoIndexerData;
        }

        public Boolean call() throws Exception {
            return repoIndexerData.saveIndexFiles();
        }
    }

    public SaveIndexMessage(RepoIndexerData repoIndexerData) {
        super(WorkAction.SAVE_INDEX_FILE,
                repoIndexerData.getIndexFolderRepoPath(),
                new Action(repoIndexerData));
    }
}
