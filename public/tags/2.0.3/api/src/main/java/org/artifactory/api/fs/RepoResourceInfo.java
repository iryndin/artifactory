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
package org.artifactory.api.fs;

import org.artifactory.api.common.Info;

import java.util.Set;

/**
 * Basic information about the file. Internally not stored as XML but as node properties
 *
 * @author yoavl
 */
public interface RepoResourceInfo extends Info {

    String getName();

    long getLastModified();

    void setLastModified(long modified);

    long getSize();

    void setSize(long size);

    String getSha1();

    String getMd5();

    Set<ChecksumInfo> getChecksums();

    void setChecksums(Set<ChecksumInfo> checksums);
}