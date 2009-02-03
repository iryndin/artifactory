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

/**
 * @author freds
 * @date Sep 18, 2008
 */
public enum WorkAction {
    UPDATE_DOWNLOAD_COUNT(true), DOWNLOAD_AND_SAVE(false),
    STORE_CHECKSUMS(true), SAVE_INDEX_FILE(false);//, DELETE, CHECK_VALIDITY, IMPORT, EXPORT

    private final boolean publishAfterCommit;

    WorkAction(boolean publishAfterCommit) {
        this.publishAfterCommit = publishAfterCommit;
    }

    public boolean isPublishAfterCommit() {
        return publishAfterCommit;
    }
}
