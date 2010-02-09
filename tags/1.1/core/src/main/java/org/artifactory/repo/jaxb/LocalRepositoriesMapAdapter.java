package org.artifactory.repo.jaxb;

import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.artifactory.repo.CentralConfig;
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
        XmlAdapter<LocalRepositoriesMapAdapter.Wrappper, ListOrderedMap<String, LocalRepo>> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(LocalRepositoriesMapAdapter.class);


    public ListOrderedMap<String, LocalRepo> unmarshal(Wrappper wrapper)
            throws Exception {
        ListOrderedMap<String, LocalRepo> localRepositoriesMap =
                new ListOrderedMap<String, LocalRepo>();
        for (LocalRepo repository : wrapper.getList()) {
            localRepositoriesMap.put(repository.getKey(), repository);
        }
        return localRepositoriesMap;
    }

    public Wrappper marshal(ListOrderedMap<String, LocalRepo> map)
            throws Exception {
        return new Wrappper(map);
    }


    @XmlType(name = "LocalRepositoriesType")
    public static class Wrappper {
        @XmlElement(name = "localRepository", required = true, namespace = CentralConfig.NS)
        private List<JcrRepo> list = new ArrayList<JcrRepo>();


        public Wrappper() {
        }

        public Wrappper(ListOrderedMap<String, LocalRepo> map) {
            for (LocalRepo repo : map.values()) {
                list.add((JcrRepo) repo);
            }
        }

        public List<JcrRepo> getList() {
            return list;
        }
    }
}
