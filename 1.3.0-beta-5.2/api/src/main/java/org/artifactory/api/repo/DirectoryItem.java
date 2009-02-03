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

import org.artifactory.api.fs.ItemInfo;

import java.io.Serializable;

/**
 * User: freds Date: Jul 27, 2008 Time: 8:31:30 PM
 */
public class DirectoryItem implements Serializable, Comparable<DirectoryItem> {
    private static final long serialVersionUID = 1L;

    public static final String UP = "..";

    private final String name;
    private final ItemInfo itemInfo;

    public DirectoryItem(ItemInfo itemInfo) {
        this.itemInfo = itemInfo;
        this.name = itemInfo.getName();
    }

    public DirectoryItem(String name, ItemInfo itemInfo) {
        this.itemInfo = itemInfo;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ItemInfo getItemInfo() {
        return itemInfo;
    }

    public String getPath() {
        return getItemInfo().getRelPath();
    }

    public boolean isDirectory() {
        return getItemInfo().isFolder();
    }

    public boolean isFolder() {
        return getItemInfo().isFolder();
    }

    public int compareTo(DirectoryItem o) {
        if (name.equals(o.name)) {
            return 0;
        }
        if (name.equals(UP)) {
            return -1;
        }
        if (o.name.equals(UP)) {
            return 1;
        }
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DirectoryItem item = (DirectoryItem) o;

        return !(name != null ? !name.equals(item.name) : item.name != null);
    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        return result;
    }
}
