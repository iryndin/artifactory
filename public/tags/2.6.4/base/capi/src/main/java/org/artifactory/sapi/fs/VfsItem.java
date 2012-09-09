/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.sapi.fs;

import org.artifactory.common.MutableStatusHolder;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.MutableItemInfo;
import org.artifactory.repo.RepoPath;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.net.URI;

/**
 * Date: 8/3/11
 * Time: 10:31 PM
 *
 * @author Fred Simon
 */
public interface VfsItem<T extends ItemInfo, MT extends MutableItemInfo>
        extends Comparable<File>, VfsMetadataAware, Visitable<VfsItem> {
    boolean isMutable();

    boolean isDeleted();

    void setDeleted(boolean deleted);

    void setModifiedInfoFields(long modified, long updated);

    String getName();

    @Override
    String getAbsolutePath();

    String getRelativePath();

    long getCreated();

    @Override
    RepoPath getRepoPath();

    String getRepoKey();

    boolean delete();

    VfsFolder getParentFolder();

    VfsFolder getLockedParentFolder();

    VfsFolder getAncestor(int degree);

    String getParent();

    File getParentFile();

    File getAbsoluteFile();

    boolean canRead();

    boolean canWrite();

    boolean exists();

    @Override
    boolean isFile();

    boolean isHidden();

    boolean renameTo(File dest);

    boolean setReadOnly();

    boolean setExecutable(boolean executable);

    boolean canExecute();

    @Override
    int compareTo(File item);

    String getPath();

    URI toURI();

    long lastModified();

    long length();

    String[] list();

    String[] list(FilenameFilter filter);

    File[] listFiles();

    File[] listFiles(FilenameFilter filter);

    File[] listFiles(FileFilter filter);

    boolean mkdir();

    boolean mkdirs();

    boolean setLastModified(long time);

    @Override
    boolean isDirectory();

    boolean isFolder();

    T getInfo();

    MT getMutableInfo();

    void writeMetadataEntries(MutableStatusHolder status, File metadataFolder, boolean incremental);

    void updateCache();

    VfsItem save(VfsItem originalFsItem);

    void setLastUpdated(long lastUpdated);

    void unexpire();

    boolean isIdentical(VfsItem item);

    <MDT> MDT getMetadata(Class<MDT> mdClass);

    Object getMetadata(String metadataName);

    String getXmlMetadata(String metadataName);

    boolean hasMetadata(String metadataName);

    <MDT> void setMetadata(Class<MDT> mdClass, MDT metadata);

    void setXmlMetadata(String metadataName, String xmlData);

    void removeMetadata(String metadataName);

    void setXmlMetadataLater(String name, String content);

    boolean isDirty();

    void bruteForceDelete();
}
