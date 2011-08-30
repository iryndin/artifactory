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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteSession;

/**
 * A "safe" local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteRepository RemoteRepository}
 * interface. This class uses an abstract factory method for loading
 * (and reloading) the remote repository instance that is made locally
 * available through the JCR {@link Repository} interface. If the remote
 * reference breaks (a RemoteException is thrown by a remote call), then
 * this adapter attempts to reload the remote reference once before failing.
 *
 * @see javax.jcr.Repository
 * @see org.apache.jackrabbit.rmi.remote.RemoteRepository
 */
public abstract class SafeClientRepository extends ClientObject
        implements Repository {

    /** The adapted remote repository. */
    private RemoteRepository remote;

    /**
     * Creates a client adapter for the given remote repository.
     *
     * @param remote remote repository
     * @param factory local adapter factory
     */
    public SafeClientRepository(LocalAdapterFactory factory) {
        super(factory);
        try {
            remote = getRemoteRepository();
        } catch (RemoteException e) {
            remote = new BrokenRemoteRepository(e);
        }
    }

    /**
     * Abstract factory class for getting the remote repository.
     *
     * @return remote repository
     * @throws RemoteException if the remote repository could not be accessed
     */
    protected abstract RemoteRepository getRemoteRepository()
            throws RemoteException;

    /** {@inheritDoc} */
    public synchronized String getDescriptor(String name) {
        try {
            return remote.getDescriptor(name);
        } catch (RemoteException e1) {
            try {
                remote = getRemoteRepository();
                return remote.getDescriptor(name);
            } catch (RemoteException e2) {
                remote = new BrokenRemoteRepository(e2);
                throw new RemoteRuntimeException(e2);
            }
        }
    }

    /** {@inheritDoc} */
    public synchronized String[] getDescriptorKeys() {
        try {
            return remote.getDescriptorKeys();
        } catch (RemoteException e1) {
            try {
                remote = getRemoteRepository();
                return remote.getDescriptorKeys();
            } catch (RemoteException e2) {
                remote = new BrokenRemoteRepository(e2);
                throw new RemoteRuntimeException(e2);
            }
        }
    }

    private synchronized RemoteSession remoteLogin(
            Credentials credentials, String workspace)
            throws RepositoryException {
        try {
            return remote.login(credentials, workspace);
        } catch (RemoteException e1) {
            try {
                remote = getRemoteRepository();
                return remote.login(credentials, workspace);
            } catch (RemoteException e2) {
                remote = new BrokenRemoteRepository(e2);
                throw new RemoteRepositoryException(e2);
            }
        }
    }

    /** {@inheritDoc} */
    public Session login(Credentials credentials, String workspace)
            throws RepositoryException {
        RemoteSession session = remoteLogin(credentials, workspace);
        return getFactory().getSession(this, session);
    }

    /** {@inheritDoc} */
    public Session login(String workspace) throws RepositoryException {
        return login(null, workspace);
    }

    /** {@inheritDoc} */
    public Session login(Credentials credentials) throws RepositoryException {
        return login(credentials, null);
    }

    /** {@inheritDoc} */
    public Session login() throws RepositoryException {
        return login(null, null);
    }

    public Value getDescriptorValue(String key) {
        throw new RuntimeException("TODO: JCRRMI-26");
    }

    public Value[] getDescriptorValues(String key) {
        throw new RuntimeException("TODO: JCRRMI-26");
    }

    public boolean isSingleValueDescriptor(String key) {
        throw new RuntimeException("TODO: JCRRMI-26");
    }

    public boolean isStandardDescriptor(String key) {
        throw new RuntimeException("TODO: JCRRMI-26");
    }

}
