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
package org.artifactory.utils;

import org.apache.log4j.Logger;
import org.artifactory.config.CentralConfig;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class DateUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(DateUtils.class);

    public static String format(Date date) {
        ArtifactoryContext context = ContextHelper.get();
        CentralConfig cc = context.getCentralConfig();
        DateFormat dateFormat = cc.getDateFormatter();
        return dateFormat.format(date);
    }

    public static String format(long date) {
        return format(new Date(date));
    }
}
