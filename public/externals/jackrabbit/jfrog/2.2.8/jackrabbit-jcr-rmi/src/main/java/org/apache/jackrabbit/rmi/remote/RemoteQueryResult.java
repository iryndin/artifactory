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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

/**
 * Remote version of the JCR {@link javax.jcr.query.QueryResult QueryResult} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerQueryResult ServerQueryResult}
 * and {@link org.apache.jackrabbit.rmi.client.ClientQueryResult ClientQueryResult}
 * adapter base classes to provide transparent RMI access to remote items.
 * <p>
 * RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.query.QueryResult
 * @see org.apache.jackrabbit.rmi.client.ClientQueryResult
 * @see org.apache.jackrabbit.rmi.server.ServerQueryResult
 */
public interface RemoteQueryResult extends Remote {
    /**
     * @see javax.jcr.query.QueryResult#getColumnNames()
     *
     * @return a <code>PropertyIterator</code>
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String[] getColumnNames() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.QueryResult#getRows()
     *
     * @return a <code>RowIterator</code>
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getRows() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.QueryResult#getNodes()
     *
     * @return a remote node iterator
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getNodes() throws RepositoryException, RemoteException;

}
