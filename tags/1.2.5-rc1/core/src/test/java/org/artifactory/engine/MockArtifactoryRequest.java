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
package org.artifactory.engine;

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

import org.artifactory.request.ArtifactoryRequestBase;
import org.artifactory.security.RepoPath;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ben Walding
 */
public class MockArtifactoryRequest extends ArtifactoryRequestBase {
    private final long lastModified;
    private final long ifModifiedSince;
    private final boolean headOnly;

    public MockArtifactoryRequest(
            String path, long lastModified, boolean headOnly, long ifModifiedSince) {
        setRepoPath(new RepoPath("key", path));
        this.lastModified = lastModified;
        this.headOnly = headOnly;
        this.ifModifiedSince = ifModifiedSince;
    }

    public long getLastModified() {
        return round(lastModified);
    }

    public boolean isHeadOnly() {
        return headOnly;
    }

    public String getSourceDescription() {
        return "Mock";
    }

    public long getIfModifiedSince() {
        return ifModifiedSince;
    }

    public boolean isRecursive() {
        return false;
    }

    public boolean isFromAnotherArtifactory() {
        return false;
    }

    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("not implemented.");
    }
}