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
package org.artifactory.api.repo.exception;

import org.artifactory.api.repo.RepoPath;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class RepoAccessException extends Exception {
    private final String username;
    private final RepoPath repoPath;
    private final String action;

    public RepoAccessException(String message, RepoPath repoPath, String action, String username) {
        super(message);
        this.username = username;
        this.repoPath = repoPath;
        this.action = action;
    }

    public RepoAccessException(String message, RepoPath repoPath, String action, String username, Throwable cause) {
        super(message, cause);
        this.username = username;
        this.repoPath = repoPath;
        this.action = action;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " Action '" + action + "' is unauthorized for user '" + username + "' on '" +
                repoPath + "'.";
    }
}