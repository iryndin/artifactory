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

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryConstants {

    static String SYS_PROP_JCR_FORCE_ATOMIC_TX = "artifactory.jcr.forceAtomicTx";
    static String SYS_PROP_JCR_MAX_NODE_LOCK_RETRIES = "artifactory.jcr.maxNodeLockRetries";
    static String SYS_PROP_GLOBAL_OFFLINE = "artifactory.globalOffline";

    public static final boolean forceAtomicTransacions = Boolean.parseBoolean(System.getProperty(
            SYS_PROP_JCR_FORCE_ATOMIC_TX, Boolean.FALSE.toString()));

    public static final int maxNodeLockRetries = Integer.parseInt(System.getProperty(
            SYS_PROP_JCR_MAX_NODE_LOCK_RETRIES, "60"));

    public static final boolean globalOffline = Boolean.parseBoolean(System.getProperty(
            SYS_PROP_GLOBAL_OFFLINE, Boolean.FALSE.toString()));
}