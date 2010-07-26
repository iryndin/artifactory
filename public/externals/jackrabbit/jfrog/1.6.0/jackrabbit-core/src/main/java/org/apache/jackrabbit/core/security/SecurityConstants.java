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
package org.apache.jackrabbit.core.security;

/**
 * This interface defines miscellaneous security related constants.
 */
public interface SecurityConstants {

    /**
     * Name of the internal <code>SimpleCredentials</code> attribute where
     * the <code>Subject</code> of the <i>impersonating</i> <code>Session</code>
     * is stored.
     *
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    String IMPERSONATOR_ATTRIBUTE = "org.apache.jackrabbit.core.security.impersonator";

    /**
     * The default principal name of the administrators group
     */
    String ADMINISTRATORS_NAME = "administrators";

    /**
     * The default userID of the administrator.
     */
    String ADMIN_ID = "admin";

    /**
     * The default userID for anonymous login
     */
    String ANONYMOUS_ID = "anonymous";

    /**
     * To be used for the encryption. E.g. for passwords in
     * {@link javax.jcr.SimpleCredentials#getPassword()}  SimpleCredentials} 
     */
    String DEFAULT_DIGEST = "sha1";
}
