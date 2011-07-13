/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
