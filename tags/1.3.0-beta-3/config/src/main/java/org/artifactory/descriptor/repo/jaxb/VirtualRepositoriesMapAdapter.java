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
package org.artifactory.descriptor.repo.jaxb;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;

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
        XmlAdapter<VirtualRepositoriesMapAdapter.Wrappper, OrderedMap<String, VirtualRepoDescriptor>> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(VirtualRepositoriesMapAdapter.class);


    public OrderedMap<String, VirtualRepoDescriptor> unmarshal(Wrappper wrapper)
            throws Exception {
        OrderedMap<String, VirtualRepoDescriptor> virtualRepositoriesMap =
                new ListOrderedMap<String, VirtualRepoDescriptor>();
        for (VirtualRepoDescriptor repository : wrapper.getList()) {
            String key = repository.getKey();
            VirtualRepoDescriptor repo = virtualRepositoriesMap.put(key, repository);
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
            OrderedMap<String, VirtualRepoDescriptor> map)
            throws Exception {
        return new VirtualRepositoriesMapAdapter.Wrappper(map);
    }


    @XmlType(name = "VirtualRepositoriesType", namespace = Descriptor.NS)
    public static class Wrappper {
        @XmlElement(name = "virtualRepository", required = true, namespace = Descriptor.NS)
        private List<VirtualRepoDescriptor> list = new ArrayList<VirtualRepoDescriptor>();


        public Wrappper() {
        }

        public Wrappper(OrderedMap<String, VirtualRepoDescriptor> map) {
            for (VirtualRepoDescriptor repo : map.values()) {
                list.add(repo);
            }
        }

        public List<VirtualRepoDescriptor> getList() {
            return list;
        }
    }
}