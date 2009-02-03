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
import org.apache.log4j.Logger;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Test
public class ConfigTest {
    private final static Logger LOGGER = Logger.getLogger(ConfigTest.class);

    private File testDir;

    @BeforeTest
    public void createTestOutputDir() {
        testDir = new File("target/config-test");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
    }

    public void testWriteConfig() throws Exception {
        CentralConfigDescriptor desc = new CentralConfigDescriptor();
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

        JaxbHelper<CentralConfigDescriptor> helper = new JaxbHelper<CentralConfigDescriptor>();
        File outputConfig = new File(testDir, "central.config.test.xml");
        helper.write(new PrintStream(outputConfig), desc);
    }

    public void testReadConfig() {
        InputStream is = getClass().getResourceAsStream(
                "/META-INF/default/artifactory.config.xml");
        JaxbHelper<CentralConfigDescriptor> helper = new JaxbHelper<CentralConfigDescriptor>();
        CentralConfigDescriptor centralConfig = helper.read(is, CentralConfigDescriptor.class,
                CentralConfigDescriptor.class.getClassLoader().getResource("artifactory.xsd"));
        LOGGER.debug("config = " + centralConfig);
    }

    public void testWriteW3cSchema() throws FileNotFoundException {
        JaxbHelper<CentralConfigDescriptor> helper = new JaxbHelper<CentralConfigDescriptor>();
        helper.generateSchema(new PrintStream(new File(testDir, "schema.test.xsd")),
                CentralConfigDescriptor.class, Descriptor.NS);
    }
}
