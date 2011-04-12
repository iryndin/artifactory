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
package org.apache.jackrabbit.core;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.RepositoryManager;

/**
 * The repository manager implementation.
 */
public class RepositoryManagerImpl implements RepositoryManager {

    private final TransientRepository tr;

    RepositoryManagerImpl(TransientRepository tr) {
        this.tr = tr;
    }

    public DataStoreGarbageCollector createDataStoreGarbageCollector() throws RepositoryException {
        RepositoryImpl rep = tr.getRepository();
        if (rep != null) {
            return rep.createDataStoreGarbageCollector();
        } else {
            throw new RepositoryException("Repository is stopped");
        }
    }

    public void stop() {
        tr.shutdown();
    }

}
