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
package org.artifactory.update.security;

import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author freds
 * @date Nov 9, 2008
 */
public class SimpleUserConverter implements XmlConverter {
    private static final Logger log =
            LoggerFactory.getLogger(SimpleUserConverter.class);

    @SuppressWarnings({"unchecked"})
    public void convert(Document doc) {
        Element usersTag = doc.getRootElement().getChild("users");
        List<Element> users = usersTag.getChildren();
        for (Element user : users) {
            if (user.getName().contains("SimpleUser")) {
                user.setName("user");
                user.removeChild("authorities");
            } else {
                log.warn("A tag " + user + " under users is not SimpleUSer!!");
            }
        }
    }
}
