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
package org.artifactory.config;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Test
public class ConfigTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigTest.class);

    private File testDir;

    @BeforeTest
    public void createTestOutputDir() {
        testDir = new File("target/config-test");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
    }

    public void writeCentralConfig() throws Exception {
        MutableCentralConfigDescriptor desc = new CentralConfigDescriptorImpl();
        desc.setServerName("mymy");
        desc.setDateFormat("dd-MM-yy HH:mm:ssZ");

        LocalRepoDescriptor local1 = new LocalRepoDescriptor();
        local1.setBlackedOut(false);
        local1.setDescription("local repo 1");
        local1.setExcludesPattern("");
        local1.setIncludesPattern("**/*");
        local1.setHandleReleases(true);
        local1.setHandleSnapshots(true);
        local1.setKey("local1");

        LocalRepoDescriptor local2 = new LocalRepoDescriptor();
        local2.setBlackedOut(false);
        local2.setDescription("local repo 2");
        local2.setExcludesPattern("**/*");
        local2.setIncludesPattern("**/*");
        local2.setHandleReleases(true);
        local2.setHandleSnapshots(true);
        local2.setKey("local2");

        OrderedMap<String, LocalRepoDescriptor> localRepositoriesMap =
                new ListOrderedMap<String, LocalRepoDescriptor>();
        localRepositoriesMap.put(local1.getKey(), local1);
        localRepositoriesMap.put(local2.getKey(), local2);
        desc.setLocalRepositoriesMap(localRepositoriesMap);

        // security
        SecurityDescriptor securityDescriptor = new SecurityDescriptor();
        securityDescriptor.setAnonAccessEnabled(true);
        desc.setSecurity(securityDescriptor);

        // ldap settings
        LdapSetting ldap = new LdapSetting();
        ldap.setKey("ldap1");
        ldap.setLdapUrl("ldap://blabla");
        securityDescriptor.setLdapSettings(Arrays.asList(ldap));

        // backups
        BackupDescriptor backup = new BackupDescriptor();
        backup.setKey("backup1");
        backup.setEnabled(false);
        backup.setCronExp("* * * * *");
        desc.setBackups(Arrays.asList(backup));

        JaxbHelper<CentralConfigDescriptor> helper = new JaxbHelper<CentralConfigDescriptor>();
        File outputConfig = new File(testDir, "central.config.test.xml");
        helper.write(new PrintStream(outputConfig), desc);
    }

    @Test(dependsOnMethods = "writeCentralConfig")
    public void readWrittenCentralConfig() {
        File outputConfig = new File(testDir, "central.config.test.xml");
        JaxbHelper<CentralConfigDescriptor> helper = new JaxbHelper<CentralConfigDescriptor>();
        URL sechemaUrl = getSchemaUrl();
        CentralConfigDescriptor cc =
                helper.read(outputConfig, CentralConfigDescriptorImpl.class, sechemaUrl);

        Assert.assertEquals(cc.getServerName(), "mymy");
        Assert.assertTrue(cc.getSecurity().isAnonAccessEnabled());

        Assert.assertEquals(cc.getLocalRepositoriesMap().size(), 2,
                "Expecting 2 local repositories");

        List<BackupDescriptor> backups = cc.getBackups();
        Assert.assertEquals(backups.size(), 1, "Expecting 1 backup");
        Assert.assertEquals(backups.get(0).getKey(), "backup1");
        Assert.assertFalse(backups.get(0).isEnabled());

        log.debug("config = " + cc);
    }

    public void defaultConfigElements() throws FileNotFoundException {
        CentralConfigDescriptorImpl cc = new CentralConfigDescriptorImpl();

        // at least on local repository
        LocalRepoDescriptor localRepo = new LocalRepoDescriptor();
        localRepo.setKey("abc");
        cc.addLocalRepository(localRepo);

        JaxbHelper<CentralConfigDescriptor> helper = new JaxbHelper<CentralConfigDescriptor>();
        File outputConfig = new File(testDir, "config.defaults.test.xml");
        helper.write(new PrintStream(outputConfig), cc);

        CentralConfigDescriptor descriptor =
                helper.read(outputConfig, CentralConfigDescriptorImpl.class, getSchemaUrl());
        Assert.assertNotNull(descriptor.getSecurity(), "Security setting should not be null");
        Assert.assertTrue(descriptor.getSecurity().isAnonAccessEnabled(),
                "Annonymous access should be enabled by default");
    }

    private URL getSchemaUrl() {
        URL sechemaUrl = getClass().getClassLoader().getResource("artifactory.xsd");
        return sechemaUrl;
    }

    public void writeW3cSchema() throws FileNotFoundException {
        JaxbHelper<CentralConfigDescriptorImpl> helper =
                new JaxbHelper<CentralConfigDescriptorImpl>();
        helper.generateSchema(new PrintStream(new File(testDir, "schema.test.xsd")),
                CentralConfigDescriptorImpl.class, Descriptor.NS);
    }
}
