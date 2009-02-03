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
import org.artifactory.repo.virtual.VirtualRepo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class VirtualRepositoriesMapAdapter
        extends
        XmlAdapter<VirtualRepositoriesMapAdapter.Wrappper, OrderedMap<String, VirtualRepo>> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(VirtualRepositoriesMapAdapter.class);


    public OrderedMap<String, VirtualRepo> unmarshal(Wrappper wrapper)
            throws Exception {
        OrderedMap<String, VirtualRepo> virtualRepositoriesMap =
                new ListOrderedMap<String, VirtualRepo>();
        for (VirtualRepo repository : wrapper.getList()) {
            String key = repository.getKey();
            VirtualRepo repo = virtualRepositoriesMap.put(key, repository);
            //Test for repositories with the same key
            if (repo != null) {
                //Throw an error since jaxb swallows exceptions
                throw new Error(
                        "Duplicate virtual repository key in configuration: " + key + ".");
            }
        }
        return virtualRepositoriesMap;
    }

    public VirtualRepositoriesMapAdapter.Wrappper marshal(
            OrderedMap<String, VirtualRepo> map)
            throws Exception {
        return new VirtualRepositoriesMapAdapter.Wrappper(map);
    }


    @XmlType(name = "VirtualRepositoriesType")
    public static class Wrappper {
        @XmlElement(name = "virtualRepository", required = true, namespace = CentralConfig.NS)
        private List<VirtualRepo> list = new ArrayList<VirtualRepo>();


        public Wrappper() {
        }

        public Wrappper(OrderedMap<String, VirtualRepo> map) {
            for (VirtualRepo repo : map.values()) {
                list.add(repo);
            }
        }

        public List<VirtualRepo> getList() {
            return list;
        }
    }
}