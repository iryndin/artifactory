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

    @Override
    public Object getObject(String path) throws ObjectContentManagerException {
        return objects.get(path);
    }

    @Override
    public void insert(Object object) throws ObjectContentManagerException {
        OcmStorable ocmObj = (OcmStorable) object;
        objects.put(ocmObj.getJcrPath(), ocmObj);
    }

    @Override
    public void update(Object object) throws ObjectContentManagerException {
        OcmStorable ocmObj = (OcmStorable) object;
        objects.put(ocmObj.getJcrPath(), ocmObj);
    }

    @Override
    public void remove(String path) throws ObjectContentManagerException {
        objects.remove(path);
    }

    @Override
    public void remove(Object object) throws ObjectContentManagerException {
        OcmStorable ocmObj = (OcmStorable) object;
        objects.remove(ocmObj.getJcrPath());
    }

    @Override
    public boolean objectExists(String path) throws ObjectContentManagerException {
        return false;
    }

    @Override
    public boolean isPersistent(Class clazz) {
        return false;
    }

    @Override
    public Object getObjectByUuid(String uuid) throws ObjectContentManagerException {
        return null;
    }

    @Override
    public Object getObject(String path, String versionNumber) throws ObjectContentManagerException {
        return null;
    }

    @Override
    public Object getObject(Class objectClass, String path) throws ObjectContentManagerException {
        return null;
    }

    @Override
    public Object getObject(Class objectClass, String path, String versionNumber)
            throws ObjectContentManagerException {
        return null;
    }

    @Override
    public void retrieveMappedAttribute(Object object, String attributeName) {
    }

    @Override
    public void retrieveAllMappedAttributes(Object object) {
    }

    @Override
    public void remove(Query query) throws ObjectContentManagerException {
    }

    @Override
    public Object getObject(Query query) throws ObjectContentManagerException {
        return null;
    }

    @Override
    public Collection getObjects(Query query) throws ObjectContentManagerException {
        return null;
    }

    @Override
    public Collection getObjects(Class objectClass, String path) throws ObjectContentManagerException {
        return null;
    }

    @Override
    public Collection getObjects(String query, String language) {
        return null;
    }

    @Override
    public Iterator getObjectIterator(Query query) throws ObjectContentManagerException {
        return null;
    }

    @Override
    public Iterator getObjectIterator(String query, String language) {
        return null;
    }

    @Override
    public void checkout(String path) throws VersionException {
    }

    @Override
    public void checkin(String path) throws VersionException {
    }

    @Override
    public void checkin(String path, String[] versionLabels) throws VersionException {
    }

    @Override
    public String[] getVersionLabels(String path, String versionName) throws VersionException {
        return new String[0];
    }

    @Override
    public String[] getAllVersionLabels(String path) throws VersionException {
        return new String[0];
    }

    @Override
    public void addVersionLabel(String path, String versionName, String versionLabel) throws VersionException {
    }

    @Override
    public VersionIterator getAllVersions(String path) throws VersionException {
        return null;
    }

    @Override
    public Version getRootVersion(String path) throws VersionException {
        return null;
    }

    @Override
    public Version getBaseVersion(String path) throws VersionException {
        return null;
    }

    @Override
    public Version getVersion(String path, String versionName) throws VersionException {
        return null;
    }

    @Override
    public void save() throws ObjectContentManagerException {
    }

    @Override
    public void logout() throws ObjectContentManagerException {
    }

    @Override
    public Lock lock(String path, boolean isDeep, boolean isSessionScoped) throws LockedException {
        return null;
    }

    @Override
    public void unlock(String path, String lockToken) throws IllegalUnlockException {
    }

    @Override
    public boolean isLocked(String absPath) {
        return false;
    }

    @Override
    public QueryManager getQueryManager() {
        return null;
    }

    @Override
    public void refresh(boolean keepChanges) {
    }

    @Override
    public void move(String srcPath, String destPath) throws ObjectContentManagerException {
    }

    @Override
    public void copy(String srcPath, String destPath) throws ObjectContentManagerException {
    }

    @Override
    public Session getSession() {
        return null;
    }
}
