package org.artifactory.mapper;

import org.artifactory.common.property.MinutesToSecondsPropertyMapper;
import org.artifactory.common.property.PropertyMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

/**
 * @author Tomer Cohen
 */
@Test
public class MinutesToSecondsPropertyMapperTest {

    public void minutesToSecondsPropertyMapper() {
        Properties properties = System.getProperties();
        properties.put("artifactory.gc.intervalMins", "1");
        PropertyMapper propertyMapper = new MinutesToSecondsPropertyMapper("artifactory.gc.intervalSecs");
        String result = propertyMapper.map(properties.get("artifactory.gc.intervalMins").toString());

        Assert.assertEquals(result, "60");
    }
}
