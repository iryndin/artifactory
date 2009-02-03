/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.jcr.lock;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.jcr.md.MetadataKey;
import org.artifactory.jcr.md.MetadataValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author freds
 * @date Sep 5, 2008
 */
public class SessionLockManager {
    private static final Logger LOGGER = LogManager.getLogger(SessionLockManager.class);

    private Map<MetadataKey, MetadataValue> lockedMetadata = null;

    public boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    public void debug(Object message) {
        LOGGER.debug(message);
    }

    public void debug(Object message, Throwable t) {
        LOGGER.debug(message, t);
    }

    public void addMetadataObject(MetadataValue value) {
        if (lockedMetadata == null) {
            lockedMetadata = new HashMap<MetadataKey, MetadataValue>(5, 0.75f);
            /*
            new ReferenceMap<MetadataKey, MetadataValue>(
                    ReferenceMap.HARD, ReferenceMap.SOFT, 5, 0.75f);
            */
        }
        lockedMetadata.put(value.getKey(), value);
    }

    public MetadataValue getLockedMetadata(MetadataKey key) {
        if (lockedMetadata == null) {
            return null;
        }
        return lockedMetadata.get(key);
    }

    public boolean isLockedByMe(MetadataKey key) {
        MetadataValue localValue = getLockedMetadata(key);
        if (localValue == null) {
            return false;
        }
        // This sanity check synchronize on lock so active only in debug
        if (LOGGER.isDebugEnabled()) {
            localValue.assertLockOwner();
        }
        return true;
    }

    public void releaseLocks(boolean commit) {
        if (lockedMetadata == null) {
            return;
        }
        try {
            Collection<MetadataValue> lockedMds = lockedMetadata.values();
            MetadataValue[] metadataValues = lockedMds.toArray(new MetadataValue[lockedMds.size()]);
            // Loop through the array since unlock will remove the element from the Map
            for (MetadataValue value : metadataValues) {
                value.unlock(this, commit);
            }
        } finally {
            lockedMetadata = null;
        }
    }

    public boolean removeEntry(MetadataKey key) {
        if (lockedMetadata == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.warn("Unlocking " + key + " but not locked by me!");
            }
            return false;
        }
        MetadataValue localValue = lockedMetadata.remove(key);
        if (LOGGER.isDebugEnabled() && localValue == null) {
            LOGGER.warn("Unlocking " + key + " but not locked by me!");
        }
        return localValue != null;
    }

    public boolean hasLocks() {
        return (lockedMetadata != null && !lockedMetadata.isEmpty());
    }
}
