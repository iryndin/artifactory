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
package org.artifactory.worker;

import org.jetlang.core.Filter;
import org.springframework.util.Assert;

/**
 * @author freds
 * @date Sep 18, 2008
 */
class RepositoryFiberKey implements Filter<WorkMessage> {
    private final String repoKey;
    private final WorkAction action;

    RepositoryFiberKey(WorkMessage msg) {
        this(msg.getRepoPath().getRepoKey(), msg.getAction());
    }

    RepositoryFiberKey(String repoKey, WorkAction action) {
        Assert.notNull(repoKey, "The repository key cannot be null");
        Assert.notNull(action, "The action cannot be null");
        this.repoKey = repoKey;
        this.action = action;
    }

    public boolean passes(WorkMessage msg) {
        return repoKey.equals(msg.getRepoPath().getRepoKey()) &&
                action == msg.getAction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RepositoryFiberKey key = (RepositoryFiberKey) o;
        return action == key.action && repoKey.equals(key.repoKey);
    }

    @Override
    public int hashCode() {
        int result;
        result = repoKey.hashCode();
        result = 31 * result + action.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RepositoryFiberKey{" +
                "repoKey='" + repoKey + '\'' +
                ", action=" + action +
                '}';
    }
}
