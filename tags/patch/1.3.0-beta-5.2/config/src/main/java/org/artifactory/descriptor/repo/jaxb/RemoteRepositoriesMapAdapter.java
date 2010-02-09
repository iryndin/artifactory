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
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class RemoteRepositoriesMapAdapter
        extends
        XmlAdapter<RemoteRepositoriesMapAdapter.Wrappper, OrderedMap<String, RemoteRepoDescriptor>> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RemoteRepositoriesMapAdapter.class);


    public OrderedMap<String, RemoteRepoDescriptor> unmarshal(Wrappper wrapper)
            throws Exception {
        OrderedMap<String, RemoteRepoDescriptor> remoteRepositoriesMap =
                new ListOrderedMap<String, RemoteRepoDescriptor>();
        for (RemoteRepoDescriptor repository : wrapper.getList()) {
            String key = repository.getKey();
            RemoteRepoDescriptor repo = remoteRepositoriesMap.put(key, repository);
            //Test for repositories with the same key
            if (repo != null) {
                //Throw an error since jaxb swallows exceptions
                throw new Error(
                        "Duplicate repository key in configuration: " + key + ".");
            }
        }
        return remoteRepositoriesMap;
    }

    public RemoteRepositoriesMapAdapter.Wrappper marshal(
            OrderedMap<String, RemoteRepoDescriptor> map)
            throws Exception {
        return new RemoteRepositoriesMapAdapter.Wrappper(map);
    }


    @XmlType(name = "RemoteRepositoriesType", namespace = Descriptor.NS)
    public static class Wrappper {
        @XmlElement(name = "remoteRepository", required = true, namespace = Descriptor.NS)
        private List<HttpRepoDescriptor> list = new ArrayList<HttpRepoDescriptor>();


        public Wrappper() {
        }

        public Wrappper(OrderedMap<String, RemoteRepoDescriptor> map) {
            for (RemoteRepoDescriptor repo : map.values()) {
                list.add((HttpRepoDescriptor) repo);
            }
        }

        public List<HttpRepoDescriptor> getList() {
            return list;
        }
    }
}
