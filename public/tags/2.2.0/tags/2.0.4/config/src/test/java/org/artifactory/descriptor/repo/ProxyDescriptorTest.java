package org.artifactory.descriptor.repo;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the ProxyDescriptor.
 *
 * @author Yossi Shaul
 */
@Test
public class ProxyDescriptorTest {

    public void defaultConstructor() {
        ProxyDescriptor proxy = new ProxyDescriptor();
        Assert.assertNull(proxy.getKey());
        Assert.assertNull(proxy.getHost());
        Assert.assertEquals(proxy.getPort(), 0);
        Assert.assertNull(proxy.getUsername());
        Assert.assertNull(proxy.getPassword());
        Assert.assertNull(proxy.getDomain());
    }

}
