package org.artifactory.info;

import org.artifactory.common.ArtifactoryProperties;
import org.artifactory.common.ConstantsValue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

/**
 * An information group for all the artifactory properties
 *
 * @author Noam Tenne
 */
public class ArtifactoryInfo extends BaseInfoGroup {

    /**
     * Returns all the info objects from the current group
     *
     * @return InfoObject[] - Collection of info objects from current group
     */
    @Override
    public InfoObject[] getInfo() {
        //Make a copy of the artifactory properties
        Properties properties = ArtifactoryProperties.get().getPropertiesCopy();
        ArrayList<InfoObject> infoList = new ArrayList<InfoObject>();
        ConstantsValue[] constants = ConstantsValue.values();

        //Returns all the properties form ConstantsValue
        for (ConstantsValue constantsValue : constants) {
            String value = constantsValue.getString();
            if (value != null) {
                InfoObject infoObject =
                        new InfoObject(constantsValue.getPropertyName(), value);
                infoList.add(infoObject);
                //Remove duplicates from artifactoryProperties copy
                properties.remove(constantsValue.getPropertyName());
            }
        }

        //Iterate over artifactoryProperties copy to get the rest of the properties that were not in ConstantsValue
        Properties propertiesCopy = ArtifactoryProperties.get().getPropertiesCopy();
        Enumeration keys = propertiesCopy.keys();
        while (keys.hasMoreElements()) {
            String propertyName = (String) keys.nextElement();
            InfoObject infoObject =
                    new InfoObject(propertyName, propertiesCopy.getProperty(propertyName));
            infoList.add(infoObject);
        }
        return infoList.toArray(new InfoObject[infoList.size()]);
    }
}
