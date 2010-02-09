package org.artifactory.api.security;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the GroupInfo class.
 *
 * @author Yossi Shaul
 */
@Test
public class GroupInfoTest {

    public void defaultConstructor() {
        GroupInfo info = new GroupInfo();
        Assert.assertNull(info.getGroupName(), "Group name should be null by default");
        Assert.assertNull(info.getDescription(), "Group description should be null by default");
        Assert.assertFalse(info.isNewUserDefault(), "Group should not be added by default");
    }

    public void copyConstructor() {
        GroupInfo orig = new GroupInfo("name", "bla bla", false);
        GroupInfo copy = new GroupInfo(orig);
        Assert.assertTrue(EqualsBuilder.reflectionEquals(orig, copy), "Orig and copy differ");

        orig.setNewUserDefault(true);
        copy = new GroupInfo(orig);
        Assert.assertTrue(EqualsBuilder.reflectionEquals(orig, copy), "Orig and copy differ");
    }

}
