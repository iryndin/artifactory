/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Base class for the checksum policies tests. Mainly to enforce certain tests for all the policies.
 *
 * @author Yossi Shaul
 */
public abstract class ChecksumPolicyBaseTest {
    ChecksumInfo matchedChecksums;
    ChecksumInfo notMatchedChecksums;
    ChecksumInfo noOriginalChecksum;

    abstract ChecksumPolicy getPolicy();

    abstract void checksumsMatch();

    abstract void noOriginalChecksum();

    abstract void checksumsDoesNotMatch();

    abstract void returnedChecksum();

    @BeforeMethod
    void generateTestData() {
        // Match checksum should be the only sha1
        matchedChecksums = new ChecksumInfo(ChecksumType.sha1, "1234567890", "1234567890");
        notMatchedChecksums = new ChecksumInfo(ChecksumType.md5, "thiswontmatch", "1234567890");
        noOriginalChecksum = new ChecksumInfo(ChecksumType.md5, null, "calculatedchecksum123");

    }

    @Test
    public void oneMatchedChecksumAllShouldPass() {
        ChecksumPolicyBase delegatingBasePolicy = new ChecksumPolicyBase() {
            @Override
            boolean verifyChecksum(ChecksumInfo checksumInfo) {
                return ((ChecksumPolicyBase) getPolicy()).verifyChecksum(checksumInfo);
            }

            @Override
            String getChecksum(ChecksumInfo checksumInfo) {
                return ((ChecksumPolicyBase) getPolicy()).getChecksum(checksumInfo);
            }

            @Override
            ChecksumPolicyType getChecksumPolicyType() {
                return null;
            }
        };
        // You can have only 2 checksums in the set (one for each type)
        Assert.assertTrue(delegatingBasePolicy.verify(new HashSet<ChecksumInfo>(
                Arrays.asList(notMatchedChecksums, matchedChecksums))),
                "All policies should pass because there is one matches checksum");
        Assert.assertTrue(delegatingBasePolicy.verify(new HashSet<ChecksumInfo>(
                Arrays.asList(noOriginalChecksum, matchedChecksums))),
                "All policies should pass because there is one matches checksum");
    }
}
