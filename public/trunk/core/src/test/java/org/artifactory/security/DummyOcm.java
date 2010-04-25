/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.security;

import org.apache.jackrabbit.ocm.exception.IllegalUnlockException;
import org.apache.jackrabbit.ocm.exception.LockedException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.lock.Lock;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.version.Version;
import org.apache.jackrabbit.ocm.version.VersionIterator;
import org.artifactory.jcr.ocm.OcmStorable;

import javax.jcr.Session;
import javax.jcr.version.VersionException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author freds
 * @date Jan 15, 2009
 */
class DummyOcm implements ObjectContentManager {
    private Map<String, ? super OcmStorable> objects = new ConcurrentHashMap<String, OcmStorable>();

    DummyOcm() {
    }

    DummyOcm(Map<String, ? super OcmStorable> objects) {
        this.objects = objects;
    }

    public Map<String, ? super OcmStorable> getObjects() {
        return objects;
    }

    public Object getObject(String path) throws ObjectContentManagerException {
        return objects.get(path);
    }

    public void insert(Object object) throws ObjectContentManagerException {
        OcmStorable ocmObj = (OcmStorable) object;
        objects.put(ocmObj.getJcrPath(), ocmObj);
    }

    public void update(Object object) throws ObjectContentManagerException {
        OcmStorable ocmObj = (OcmStorable) object;
        objects.put(ocmObj.getJcrPath(), ocmObj);
    }

    public void remove(String path) throws ObjectContentManagerException {
        objects.remove(path);
    }

    public void remove(Object object) throws ObjectContentManagerException {
        OcmStorable ocmObj = (OcmStorable) object;
        objects.remove(ocmObj.getJcrPath());
    }

    public boolean objectExists(String path) throws ObjectContentManagerException {
        return false;
    }

    public boolean isPersistent(Class clazz) {
        return false;
    }

    public Object getObjectByUuid(String uuid) throws ObjectContentManagerException {
        return null;
    }

    public Object getObject(String path, String versionNumber) throws ObjectContentManagerException {
        return null;
    }

    public Object getObject(Class objectClass, String path) throws ObjectContentManagerException {
        return null;
    }

    public Object getObject(Class objectClass, String path, String versionNumber)
            throws ObjectContentManagerException {
        return null;
    }

    public void retrieveMappedAttribute(Object object, String attributeName) {
    }

    public void retrieveAllMappedAttributes(Object object) {
    }

    public void remove(Query query) throws ObjectContentManagerException {
    }

    public Object getObject(Query query) throws ObjectContentManagerException {
        return null;
    }

    public Collection getObjects(Query query) throws ObjectContentManagerException {
        return null;
    }

    public Collection getObjects(Class objectClass, String path) throws ObjectContentManagerException {
        return null;
    }

    public Collection getObjects(String query, String language) {
        return null;
    }

    public Iterator getObjectIterator(Query query) throws ObjectContentManagerException {
        return null;
    }

    public Iterator getObjectIterator(String query, String language) {
        return null;
    }

    public void checkout(String path) throws VersionException {
    }

    public void checkin(String path) throws VersionException {
    }

    public void checkin(String path, String[] versionLabels) throws VersionException {
    }

    public String[] getVersionLabels(String path, String versionName) throws VersionException {
        return new String[0];
    }

    public String[] getAllVersionLabels(String path) throws VersionException {
        return new String[0];
    }

    public void addVersionLabel(String path, String versionName, String versionLabel) throws VersionException {
    }

    public VersionIterator getAllVersions(String path) throws VersionException {
        return null;
    }

    public Version getRootVersion(String path) throws VersionException {
        return null;
    }

    public Version getBaseVersion(String path) throws VersionException {
        return null;
    }

    public Version getVersion(String path, String versionName) throws VersionException {
        return null;
    }

    public void save() throws ObjectContentManagerException {
    }

    public void logout() throws ObjectContentManagerException {
    }

    public Lock lock(String path, boolean isDeep, boolean isSessionScoped) throws LockedException {
        return null;
    }

    public void unlock(String path, String lockToken) throws IllegalUnlockException {
    }

    public boolean isLocked(String absPath) {
        return false;
    }

    public QueryManager getQueryManager() {
        return null;
    }

    public void refresh(boolean keepChanges) {
    }

    public void move(String srcPath, String destPath) throws ObjectContentManagerException {
    }

    public void copy(String srcPath, String destPath) throws ObjectContentManagerException {
    }

    public Session getSession() {
        return null;
    }
}
