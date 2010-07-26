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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;

import javax.jcr.Value;

/**
 * <code>JackrabbitAccessControlEntry</code> is a Jackrabbit specific extension
 * of the <code>AccessControlEntry</code> interface. It represents an single
 * entry of a {@link JackrabbitAccessControlList}.
 */
public interface JackrabbitAccessControlEntry extends AccessControlEntry {

    /**
     * @return true if this entry adds <code>Privilege</code>s for the principal;
     * false otherwise.
     */
    boolean isAllow();

    /**
     * Return the names of the restrictions present with this access control entry.
     *
     * @return the names of the restrictions
     */
    String[] getRestrictionNames();

    /**
     * Return the value of the restriction with the specified name or
     * <code>null</code> if no such restriction exists.
     *
     * @param restrictionName The of the restriction as obtained through
     * {@link #getRestrictionNames()}.
     * @return value of the restriction with the specified name or
     * <code>null</code> if no such restriction exists
     */
    Value getRestriction(String restrictionName);
}
