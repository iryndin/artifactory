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
package org.artifactory.repo.jaxb;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.artifactory.config.CentralConfig;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.repo.JcrRepo;

import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ConfigTest extends TestCase {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ConfigTest.class);

    public void testWriteConfig() throws Exception {
        CentralConfig cc = new CentralConfig();
        cc.setDateFormat("dd-MM-yy HH:mm:ssZ");

        JcrRepo local1 = new JcrRepo();
        local1.setBlackedOut(false);
        local1.setDescription("local repo 1");
        local1.setExcludesPattern("");
        local1.setIncludesPattern("**/*");
        local1.setHandleReleases(true);
        local1.setHandleSnapshots(true);
        local1.setKey("local1");
        local1.init();

        JcrRepo local2 = new JcrRepo();
        local2.setBlackedOut(false);
        local2.setDescription("local repo 2");
        local2.setExcludesPattern("**/*");
        local2.setIncludesPattern("**/*");
        local2.setHandleReleases(true);
        local2.setHandleSnapshots(true);
        local2.setKey("local2");
        local2.init();

        cc.putLocalRepo(local1);
        cc.putLocalRepo(local2);

        JaxbHelper<CentralConfig> helper = new JaxbHelper<CentralConfig>();
        helper.write(System.out, cc);
    }

    public void testReadConfig() {
        InputStream is = getClass().getResourceAsStream(
                "../../../../../resources/org/artifactory/config/artifactory.test.config.xml");
        JaxbHelper<CentralConfig> helper = new JaxbHelper<CentralConfig>();
        CentralConfig config = helper.read(is, CentralConfig.class);
        System.out.println("config = " + config);
    }

    public void testWriteW3cSchema() {
        JaxbHelper<CentralConfig> helper = new JaxbHelper<CentralConfig>();
        helper.generateSchema(System.out, CentralConfig.class, CentralConfig.NS);
    }
}
