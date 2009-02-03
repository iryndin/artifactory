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

import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.HttpRepo;
import org.artifactory.repo.RemoteRepo;

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
        XmlAdapter<RemoteRepositoriesMapAdapter.Wrappper, ListOrderedMap<String, RemoteRepo>> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RemoteRepositoriesMapAdapter.class);


    public ListOrderedMap<String, RemoteRepo> unmarshal(Wrappper wrapper)
            throws Exception {
        ListOrderedMap<String, RemoteRepo> remoteRepositoriesMap =
                new ListOrderedMap<String, RemoteRepo>();
        for (RemoteRepo repository : wrapper.getList()) {
            String key = repository.getKey();
            RemoteRepo repo = remoteRepositoriesMap.put(key, repository);
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
            ListOrderedMap<String, RemoteRepo> map)
            throws Exception {
        return new RemoteRepositoriesMapAdapter.Wrappper(map);
    }


    @XmlType(name = "RemoteRepositoriesType")
    public static class Wrappper {
        @XmlElement(name = "remoteRepository", required = true, namespace = CentralConfig.NS)
        private List<HttpRepo> list = new ArrayList<HttpRepo>();


        public Wrappper() {
        }

        public Wrappper(ListOrderedMap<String, RemoteRepo> map) {
            for (RemoteRepo repo : map.values()) {
                list.add((HttpRepo) repo);
            }
        }

        public List<HttpRepo> getList() {
            return list;
        }
    }
}
