/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests the MultiStatusHolder.
 *
 * @author Yossi Shaul
 */
@Test
public class MultiStatusHolderTest {
    private static final Logger log = LoggerFactory.getLogger(MultiStatusHolderTest.class);
    private MultiStatusHolder toMerge;
    private MultiStatusHolder target;

    @BeforeMethod
    void initStatusHolders() {
        toMerge = new MultiStatusHolder();
        toMerge.setActivateLogging(false);
        target = new MultiStatusHolder();
        target.setActivateLogging(false);
    }

    public void mergeTwoStatusHolders() {
        toMerge.setWarning("warninig", log);
        toMerge.setStatus("ok", log);

        target.setStatus("target1", log);
        target.setStatus("target2", log);

        assertFalse(target.hasWarnings());
        assertEquals(target.getAllEntries().size(), 2);

        // now merge
        target.merge(toMerge);

        assertTrue(target.hasWarnings());
        assertFalse(target.hasErrors());
        assertEquals(target.getAllEntries().size(), 4);
    }

    public void mergeWithEmptyStatusHolder() {
        target.setActivateLogging(false);
        target.setError("target1", log);
        target.setStatus("target2", log);

        target.merge(toMerge);

        assertFalse(target.hasWarnings());
        assertTrue(target.hasErrors());
        assertEquals(target.getAllEntries().size(), 2);
    }

    public void mergeWithErrorOverride() {
        toMerge.setError("toMergeError", log);
        target.setError("targetError", log);

        target.merge(toMerge);

        assertTrue(target.hasErrors());
        assertEquals(target.getErrors().size(), 2);
        assertEquals(target.getLastError().getMessage(), "toMergeError");
    }

    public void mergeWithSingleStatusHolder() {
        BasicStatusHolder single = new BasicStatusHolder();
        single.setError("error", log);

        target.setStatus("target1", log);
        target.setStatus("target2", log);

        assertFalse(target.hasWarnings());
        assertEquals(target.getAllEntries().size(), 2);

        // now merge
        target.merge(single);

        assertFalse(target.hasWarnings());
        assertTrue(target.hasErrors());
        assertEquals(target.getAllEntries().size(), 3);

        single = new BasicStatusHolder();
        single.setWarning("warning", log);

        // merge again
        target.merge(single);

        assertTrue(target.hasWarnings());
        assertTrue(target.hasErrors());
        assertEquals(target.getAllEntries().size(), 4);
    }

}
