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
package org.artifactory.cli;

import org.artifactory.utils.PathUtils;

/**
 * @author freds
 * @date Sep 1, 2008
 */
public class OptionInfo implements Option {
    private final String name;
    private final String description;
    private final boolean needExtraParam;
    private final String paramDescription;
    private String value = null;

    public OptionInfo(String name, String description) {
        this(name, description, false, null);
    }

    public OptionInfo(String name, String description, boolean needExtraParam,
            String paramDescription) {
        this.name = name;
        this.description = description;
        this.needExtraParam = needExtraParam;
        this.paramDescription = paramDescription;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isNeedExtraParam() {
        return needExtraParam;
    }

    public String getParamDescription() {
        return paramDescription;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isSet() {
        return PathUtils.hasText(value);
    }

    public void set() {
        setValue("on");
    }

    public String argValue() {
        return Option.OPTION_PREFIX + name;
    }

}
