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
package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.data.AbstractDataRecord;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;

import java.io.InputStream;

/**
 * Data record that is stored in a database
 *
 * @author freds
 * @date Mar 12, 2009
 */
public class ArtifactoryDbDataRecord extends AbstractDataRecord {

    protected final ArtifactoryDbDataStore store;
    protected final long length;

    /**
     * Creates a data record for the store based on the given identifier and length.
     *
     * @param store
     * @param identifier
     * @param length
     */
    public ArtifactoryDbDataRecord(ArtifactoryDbDataStore store,
            DataIdentifier identifier,
            long length) {
        super(identifier);
        this.store = store;
        this.length = length;
    }

    /**
     * {@inheritDoc}
     */
    public long getLength() throws DataStoreException {
        store.usesIdentifier(getIdentifier());
        return length;
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream() throws DataStoreException {
        store.usesIdentifier(getIdentifier());
        return store.getInputStream(getIdentifier());
    }

}
