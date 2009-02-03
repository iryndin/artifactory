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
package org.artifactory.maven;

import org.artifactory.api.maven.MavenNaming;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for MavenUtils
 *
 * @author Yossi Shaul
 */
public class MavenUtilsTest {

    @Test
    public void testToMaven1Path() {
        String maven1Url = MavenNaming.toMaven1Path(
                "org/apache/commons/commons-email/1.1/commons-email-1.1.jar");
        Assert.assertEquals("org.apache.commons/jars/commons-email-1.1.jar", maven1Url);
    }

    @Test
    public void testToMaven1PathPom() {
        String maven1Url = MavenNaming.toMaven1Path(
                "org/apache/commons/commons-email/1.1/commons-email-1.1.pom");
        Assert.assertEquals("org.apache.commons/poms/commons-email-1.1.pom", maven1Url);
    }

    @Test
    public void testToMaven1PathMD5() {
        String maven1Url = MavenNaming.toMaven1Path(
                "com/sun/commons/logging-api/1.0.4/logging-api-1.0.4.jar.md5");
        Assert.assertEquals("com.sun.commons/jars/logging-api-1.0.4.jar.md5", maven1Url);
    }

    @Test
    public void testToMaven1PathSha1() {
        String maven1Url = MavenNaming.toMaven1Path(
                "com/sun/commons/logging-api/1.0.4/logging-api-1.0.4.pom.sha1");
        Assert.assertEquals("com.sun.commons/poms/logging-api-1.0.4.pom.sha1", maven1Url);
    }
}
