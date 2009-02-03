package org.artifactory.schedule;


import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class CachedThreadPoolTaskExecutor extends ConcurrentTaskExecutor {
    public CachedThreadPoolTaskExecutor() {
        super(Executors.newCachedThreadPool());
    }
}
