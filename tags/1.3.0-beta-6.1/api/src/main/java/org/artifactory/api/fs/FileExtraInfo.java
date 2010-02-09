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
import org.artifactory.utils.PathUtils;

/**
 * @author freds
 * @date Oct 12, 2008
 */
@XStreamAlias(FileExtraInfo.ROOT)
public class FileExtraInfo extends ItemExtraInfo {
    public static final String ROOT = "artifactory-file-ext";

    private String sha1;
    private String md5;

    public FileExtraInfo() {
        super();
    }

    public FileExtraInfo(FileExtraInfo extension) {
        super(extension);
        this.sha1 = extension.sha1;
        this.md5 = extension.md5;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String toString() {
        return "FileExtraInfo{" +
                super.toString() +
                ", sha1='" + sha1 + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }

    @Override
    public boolean isIdentical(ItemExtraInfo extraInfo) {
        if (!(extraInfo instanceof FileExtraInfo)) {
            return false;
        }
        FileExtraInfo fileExtraInfo = (FileExtraInfo) extraInfo;
        return PathUtils.safeStringEquals(this.sha1, fileExtraInfo.sha1) &&
                PathUtils.safeStringEquals(this.md5, fileExtraInfo.md5) &&
                super.isIdentical(extraInfo);
    }
}
