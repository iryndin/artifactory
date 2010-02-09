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

package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
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
    private ChecksumInfo matchedChecksumsCopy;
    private ChecksumInfo notMatchedChecksumsCopy;
    private ChecksumInfo noOriginalChecksumCopy;

    abstract ChecksumPolicy getPolicy();

    abstract void checksumsMatch();

    abstract void noOriginalChecksum();

    abstract void checksumsDoesNotMatch();

    abstract void returnedChecksum();

    @BeforeMethod
    void generateTestData() {
        matchedChecksums = new ChecksumInfo(ChecksumType.sha1, "1234567890", "1234567890");
        matchedChecksumsCopy = createCopy(matchedChecksums);
        notMatchedChecksums = new ChecksumInfo(ChecksumType.sha1, "thiswontmatch", "1234567890");
        notMatchedChecksumsCopy = createCopy(notMatchedChecksums);
        noOriginalChecksum = new ChecksumInfo(ChecksumType.md5, null, "calculatedchecksum123");
        noOriginalChecksumCopy = createCopy(noOriginalChecksum);

    }

    @AfterMethod
    void validateTestData() {
        String message = "Policy should never update the original checksum";
        Assert.assertEquals(matchedChecksumsCopy, matchedChecksums, message);
        Assert.assertEquals(notMatchedChecksums, notMatchedChecksumsCopy, message);
        Assert.assertEquals(noOriginalChecksum, noOriginalChecksumCopy, message);
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
        Assert.assertTrue(delegatingBasePolicy.verify(new HashSet<ChecksumInfo>(
                Arrays.asList(notMatchedChecksums, noOriginalChecksum, matchedChecksums))),
                "All policies should pass because there is one matches chacksum");
    }

    private ChecksumInfo createCopy(ChecksumInfo orig) {
        return new ChecksumInfo(orig.getType(), orig.getOriginal(), orig.getActual());
    }

}
