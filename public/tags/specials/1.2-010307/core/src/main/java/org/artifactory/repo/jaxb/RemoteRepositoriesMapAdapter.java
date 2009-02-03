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
            remoteRepositoriesMap.put(repository.getKey(), repository);
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
