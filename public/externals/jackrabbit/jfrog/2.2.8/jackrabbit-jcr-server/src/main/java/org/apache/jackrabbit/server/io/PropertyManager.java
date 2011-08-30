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
package org.apache.jackrabbit.server.io;

import org.apache.jackrabbit.webdav.property.PropEntry;

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * <code>PropertyManager</code>...
 */
public interface PropertyManager {

    /**
     *
     * @param exportContext
     * @param isCollection
     * @return
     * @throws RepositoryException
     */
    public boolean exportProperties(PropertyExportContext exportContext, boolean isCollection) throws RepositoryException;

    /**
     *
     * @param importContext
     * @param isCollection
     * @return
     * @throws RepositoryException
     */
    public Map<? extends PropEntry, ?> alterProperties(PropertyImportContext importContext, boolean isCollection) throws RepositoryException;

    /**
     *
     * @param propertyHandler
     */
    public void addPropertyHandler(PropertyHandler propertyHandler);

    /**
     *
     * @return
     */
    public PropertyHandler[] getPropertyHandlers();
}
