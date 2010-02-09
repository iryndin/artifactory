/*
 * This file is part of Artifactory.
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

package org.artifactory.license;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.license.License;
import org.artifactory.api.license.RequiredProperties;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * A test class for the license service implementation
 *
 * @author Noam Tenne
 */
public class LicenseServiceImplTest {

    LicenseServiceImpl service = new LicenseServiceImpl();

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void encryptMissingRequiredProperty() {
        License license = new License("moo@moo.com", new Date());
        license.put("name", "moo");
        license.put("company", "moo");

        service.encrypt(license);
    }

    @Test
    public void encryptNormal() {
        License license = new License("moo@moo.com", new Date());
        license.put("name", "moo");
        license.put("company", "moo");
        license.put(RequiredProperties.END_TIME.getPropertyName(), Long.toString(System.currentTimeMillis()));

        String encrypted = service.encrypt(license);
        Assert.assertFalse(StringUtils.isEmpty(encrypted), "Decrypted license cannot be null or empty.");
        System.out.println(encrypted);
    }

    @Test
    public void encryptDecryptNormal() {
        License license = new License("moo@moo.com", new Date());
        license.put("name", "moo");
        license.put("company", "moo");
        license.put(RequiredProperties.END_TIME.getPropertyName(), Long.toString(System.currentTimeMillis()));

        String encrypted = service.encrypt(license);
        Assert.assertFalse(StringUtils.isEmpty(encrypted), "Decrypted license cannot be null or empty.");

        License decrypted = service.decrypt(license.getKey(), encrypted);
        Assert.assertTrue(license.equals(decrypted));
    }
}
