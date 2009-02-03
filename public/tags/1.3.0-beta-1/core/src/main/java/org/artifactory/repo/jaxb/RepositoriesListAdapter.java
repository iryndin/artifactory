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

import org.apache.log4j.Logger;
import org.artifactory.config.CentralConfig;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoBase;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * The sole purpose of this adapter is to make jaxb call the setter:
 * org.artifactory.repo.virtual.VirtualRepo#setRepositories(java.util.Set<org.artifactory.repo.Repo>)
 * (see: com.sun.xml.bind.v2.runtime.reflect.Lister.CollectionLister#endPacking(T, BeanT,
 * com.sun.xml.bind.v2.runtime.reflect.Accessor<BeanT,T>))
 *
 * @author yoavl
 */
public class RepositoriesListAdapter
        extends XmlAdapter<RepositoriesListAdapter.Wrappper, List<Repo>> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(VirtualRepositoriesMapAdapter.class);

    public List<Repo> unmarshal(Wrappper wrappper) throws Exception {
        return wrappper.getList();
    }

    public Wrappper marshal(List<Repo> list) throws Exception {
        return new RepositoriesListAdapter.Wrappper(list);
    }

    @XmlType(name = "RepositoryRefsType")
    public static class Wrappper {
        //TODO: There seems to be a bug of referencing an ID from within an ID - this does not work
        //(list always empty)
        @XmlIDREF
        @XmlElement(name = "repositoryRef", type = RepoBase.class, namespace = CentralConfig.NS)
        private List<Repo> list = new ArrayList<Repo>();

        public Wrappper() {
        }

        public Wrappper(List<Repo> list) {
            this.list = list;
        }

        public List<Repo> getList() {
            return list;
        }
    }
}