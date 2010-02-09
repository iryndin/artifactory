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

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.artifactory.config.CentralConfig;
import org.artifactory.repo.JcrRepo;
import org.artifactory.repo.LocalRepo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class LocalRepositoriesMapAdapter
        extends
        XmlAdapter<LocalRepositoriesMapAdapter.Wrappper, OrderedMap<String, LocalRepo>> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(LocalRepositoriesMapAdapter.class);

    public OrderedMap<String, LocalRepo> unmarshal(Wrappper wrapper)
            throws Exception {
        OrderedMap<String, LocalRepo> localRepositoriesMap =
                new ListOrderedMap<String, LocalRepo>();
        for (LocalRepo repository : wrapper.getList()) {
            String key = repository.getKey();
            LocalRepo repo = localRepositoriesMap.put(key, repository);
            //Test for repositories with the same key
            if (repo != null) {
                //Throw an error since jaxb swallows exceptions
                throw new Error(
                        "Duplicate repository key in configuration: " + key + ".");
            }
        }
        return localRepositoriesMap;
    }

    public Wrappper marshal(OrderedMap<String, LocalRepo> map)
            throws Exception {
        return new Wrappper(map);
    }

    @XmlType(name = "LocalRepositoriesType")
    public static class Wrappper {
        @XmlElement(name = "localRepository", required = true, namespace = CentralConfig.NS)
        private List<JcrRepo> list = new ArrayList<JcrRepo>();

        public Wrappper() {
        }

        public Wrappper(OrderedMap<String, LocalRepo> map) {
            for (LocalRepo repo : map.values()) {
                list.add((JcrRepo) repo);
            }
        }

        public List<JcrRepo> getList() {
            return list;
        }
    }
}
