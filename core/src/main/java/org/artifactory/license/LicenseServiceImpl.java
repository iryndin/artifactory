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

import org.artifactory.api.license.License;
import org.artifactory.api.license.LicenseService;
import org.artifactory.security.CryptoHelper;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * The license service's main implementation
 *
 * @author Noam Tenne
 */
@Service
public class LicenseServiceImpl implements LicenseService {

    public String encrypt(License toEncrypt) {
        if (!toEncrypt.isValid()) {
            throw new IllegalArgumentException("All license-required fields must appear in the license map.");
        }
        SecretKey secretKey = CryptoHelper.generatePbeKey(toEncrypt.getKey());
        String encryptedLicense = CryptoHelper.encryptSymmetric(toEncrypt.toString(), secretKey);

        return encryptedLicense;
    }

    public License decrypt(String key, String toDecript) {
        SecretKey secretKey = CryptoHelper.generatePbeKey(key);
        String decryptedLicense = CryptoHelper.decryptSymmetric(toDecript, secretKey);
        License license = new License(key, new Date(), decryptedLicense);
        if (!license.isValid()) {
            throw new IllegalArgumentException("All license-required fields must appear in the license map.");
        }

        return license;
    }
}
