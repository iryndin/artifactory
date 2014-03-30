package org.artifactory.info;

import com.google.common.collect.Lists;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ha.HaNodeProperties;

import java.util.ArrayList;
import java.util.Properties;

/**
 * @author Yoav Luft
 */
public class HaPropInfo extends BasePropInfoGroup {
    @Override
    public boolean isInUse() {
        return ArtifactoryHome.get().isHaConfigured();
    }

    @Override
    public InfoObject[] getInfo() {
        if (ArtifactoryHome.get().isHaConfigured()) {
            HaNodeProperties haNodeProperties = ArtifactoryHome.get().getHaNodeProperties();
            if (haNodeProperties != null) {
                Properties nodeProps = haNodeProperties.getProperties();
                ArrayList<InfoObject> infoObjects = Lists.newArrayList();
                for (Object key : nodeProps.keySet()) {
                    Object value = nodeProps.get(key);
                    InfoObject infoObject = new InfoObject(key.toString(), value.toString());
                    infoObjects.add(infoObject);
                }
                return  infoObjects.toArray(new InfoObject[infoObjects.size()]);
            }
        }
        // else
        return new InfoObject[0];
    }
}
