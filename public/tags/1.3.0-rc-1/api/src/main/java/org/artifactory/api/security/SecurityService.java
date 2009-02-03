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
package org.artifactory.api.security;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.repo.Lock;
import org.artifactory.descriptor.security.ldap.LdapSetting;

/**
 * User: freds Date: Aug 13, 2008 Time: 5:17:47 PM
 */
public interface SecurityService extends ImportableExportable {
    String FILE_NAME = "security.xml";

    SecurityInfo getSecurityData();

    @Lock(transactional = true)
    void importSecurityData(SecurityInfo descriptor);

    @Lock(transactional = true)
    void removeAllSecurityData();

    /**
     * @see org.artifactory.security.ldap.LdapConnectionTester#testLdapConnection
     */
    StatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password);
}
