/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.descriptor.repo.jaxb;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
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
public class RemoteRepositoriesMapAdapter extends
        XmlAdapter<RemoteRepositoriesMapAdapter.Wrappper, OrderedMap<String, RemoteRepoDescriptor>> {


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
