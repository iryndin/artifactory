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

import org.artifactory.storage.db.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author mamo
 */
public class ConcurrentIdGeneratorTest extends IdGeneratorBaseTest {

    @Autowired
    private IdGenerator idGenerator;

    private static final int COUNT = 10000;
    private static final int POOL_SIZE = 200;

    private long startId;

    @BeforeClass
    public void before() {
        startId = getCurrentInMemoryId();
    }

    //@Test(invocationCount = COUNT, threadPoolSize = POOL_SIZE) //avoid testing testng executor...
    @Test
    public void concurrentIdGenerator() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
        List<Future<Long>> futures = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            Future<Long> future = executorService.submit(new Callable<Long>() {
                @Override
                public Long call() throws Exception {
                    long nextId = idGenerator.nextId();
                    return nextId;
                }
            });
            futures.add(future);
        }

        final Map<Long, Long> indices = new ConcurrentHashMap<>();

        for (int i = 0; i < COUNT; i++) {
            Long nextId = futures.get(i).get();
            assertTrue(indices.put(nextId, nextId) == null, "Index should be unique");
        }
    }

    @AfterClass
    public void after() {
        assertEquals(getCurrentInMemoryId(), startId + COUNT, "Current index should have promoted by COUNT");

        assertEquals(getCurrentInTableId(), getMaxReservedIndex(),
                "Max index should be consistent in memory and in table");

        assertTrue(getCurrentInMemoryId() <= getMaxReservedIndex(), "Illegal state");
    }
}
