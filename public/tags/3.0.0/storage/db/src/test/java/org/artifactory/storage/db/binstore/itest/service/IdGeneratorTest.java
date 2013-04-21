/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.storage.db.binstore.itest.service;

import org.artifactory.common.ConstantValues;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author mamo
 */
@Test(groups = "sequentialIdGenerator")
public class IdGeneratorTest extends IdGeneratorBaseTest {

    @BeforeClass
    public void before() {
        assertEquals(getCurrentInMemoryId(), 1, "Should start with id = 1");
        assertEquals(getCurrentInTableId(), 1, "Table is initialized id = 1");
    }

    @Test
    public void firstId() {
        assertEquals(idGenerator.nextId(), 1, "first id is 1");
        assertEquals(getCurrentInTableId(), 1 + ConstantValues.dbIdGeneratorFetchAmount.getLong(),
                "first nextId should fetch");
    }

    @Test(dependsOnMethods = "firstId")
    public void nextId() {
        assertEquals(idGenerator.nextId(), 2);
        assertEquals(getCurrentInTableId(), 1 + ConstantValues.dbIdGeneratorFetchAmount.getLong(),
                "no fetch if not exhausted ids");
    }

    @Test(dependsOnMethods = "nextId")
    public void fetch() {
        long currentTable = getCurrentInTableId();
        long idBeforeIteration = getCurrentInMemoryId();
        int amount = (int) (currentTable - idBeforeIteration);
        for (int i = 0; i < amount; i++) {
            assertEquals(idGenerator.nextId(), idBeforeIteration + i);
        }
        assertEquals(getCurrentInTableId(), 1 + ConstantValues.dbIdGeneratorFetchAmount.getLong(),
                "no fetch if not exhausted ids");
        assertEquals(idGenerator.nextId(), idBeforeIteration + amount);
        assertEquals(getCurrentInTableId(), 1 + 2 * ConstantValues.dbIdGeneratorFetchAmount.getLong(),
                "no fetch if not exhausted ids");
    }

}
