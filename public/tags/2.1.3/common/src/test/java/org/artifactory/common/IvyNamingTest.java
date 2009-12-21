package org.artifactory.common;

import org.artifactory.ivy.IvyNaming;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 * Tests the IvyNaming.
 *
 * @author Yossi Shaul
 */
@Test
public class IvyNamingTest {

    public void ivyFileName() {
        assertTrue(IvyNaming.isIvyFileName("ivy.xml"));
        assertTrue(IvyNaming.isIvyFileName("ivy-.xml"));
        assertTrue(IvyNaming.isIvyFileName("ivy-.ivy"));
        assertTrue(IvyNaming.isIvyFileName("organisation-ivy.xml"));
        assertTrue(IvyNaming.isIvyFileName("ivy-1.2.2.3.4.xml"));
        assertFalse(IvyNaming.isIvyFileName("1ivy.xml"));
        assertFalse(IvyNaming.isIvyFileName("ivyy.xml"));
        assertFalse(IvyNaming.isIvyFileName("xxx.ivy.xml"));
    }

}
