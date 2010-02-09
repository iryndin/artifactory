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
package org.artifactory;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryHome {
    public static String SYS_PROP = "artifactory.home";
    public static String ENV_VAR = "ARTIFACTORY_HOME";

    public static String path() {
        return System.getProperty(SYS_PROP);
    }

    public static File file() {
        return new File(path());
    }

    public static void create() {
        File logs = new File(file(), "logs");
        if (!logs.exists() && !logs.mkdirs()) {
            throw new RuntimeException(
                    "Failed to create the logs folder at '" + logs.getAbsolutePath() + "'.");
        }
    }
}
