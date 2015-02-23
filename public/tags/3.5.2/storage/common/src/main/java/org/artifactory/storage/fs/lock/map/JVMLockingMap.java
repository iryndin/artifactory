package org.artifactory.storage.fs.lock.map;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Shay Yaakov
 */
public class JVMLockingMap implements LockingMap {

    private final Map<String, String> locks = new HashMap<>();

    @Override
    public boolean tryAddAndLock(String path, long timeout, TimeUnit timeUnit) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (true) {
            synchronized (locks) {
                if (locks.containsKey(path)) {
                    long userTimeout = timeUnit.toMillis(timeout);
                    locks.wait(userTimeout);
                    long endTime = System.currentTimeMillis();
                    if (endTime - startTime >= userTimeout) {
                        return false;
                    }
                } else {
                    locks.put(path, path);
                    return true;
                }
            }
        }
    }

    @Override
    public void removeAndUnlock(String path) {
        synchronized (locks) {
            locks.remove(path);
            locks.notifyAll();
        }
    }
}
