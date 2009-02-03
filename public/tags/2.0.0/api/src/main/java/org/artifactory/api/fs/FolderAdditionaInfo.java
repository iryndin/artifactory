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

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author freds
 * @date Oct 12, 2008
 */
@XStreamAlias(FolderAdditionaInfo.ROOT)
public class FolderAdditionaInfo extends ItemAdditionalInfo {
    public static final String ROOT = "artifactory-folder-ext";

    public FolderAdditionaInfo() {
        super();
    }

    public FolderAdditionaInfo(FolderAdditionaInfo extension) {
        super(extension);
    }

    @Override
    public String toString() {
        return "FolderExtraInfo{" +
                super.toString() + "}";
    }

    @Override
    public boolean isIdentical(ItemAdditionalInfo additionalInfo) {
        return additionalInfo instanceof FolderAdditionaInfo && super.isIdentical(additionalInfo);
    }
}
