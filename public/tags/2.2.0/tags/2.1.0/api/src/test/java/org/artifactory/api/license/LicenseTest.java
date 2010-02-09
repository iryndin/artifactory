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

package org.artifactory.api.license;

import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * A test class for the license object
 *
 * @author Noam Tenne
 */
public class LicenseTest {

    @Test
    public void simpleConstructor() {
        String key = "key";
        Date date = new Date();
        License license = new License(key, date);
        assertTrue(license.getKey().equals(key), "Keys should be equal.");
        assertTrue(license.getStartDate().equals(date), "Start dates should be equal.");
        assertTrue(license.isEmpty(), "The property map should be empty by default.");
        assertTrue(StringUtils.isEmpty(license.toString()), "The property map should be empty by default.");
    }

    @Test
    public void badDecryptedConstructor() {
        String key = "key";
        Date date = new Date();
        License license = new License(key, date, "");
        assertTrue(license.isEmpty(), "The property map should be empty.");
        license = new License(key, date, "asdasdad");
        assertTrue(license.isEmpty(), "The property map should be empty.");
    }

    @Test
    public void goodDecryptedConstructor() {
        String key = "key";
        Date date = new Date();
        License license = new License(key, date, "moo|moo1||moo2|moo3");
        Assert.assertFalse(license.isEmpty(), "The property map should not be empty.");
        assertEquals(license.get("moo"), "moo1", "Value 'moo1' should have been mapped under key 'moo'.");
        assertEquals(license.get("moo2"), "moo3", "Value 'moo3' should have been mapped under key 'moo2'.");

        License duplicateLicense = new License(license.getKey(), license.getStartDate(), license.toString());
        assertTrue(duplicateLicense.equals(license), "Duplicated licenses should be equal.");
    }
}
