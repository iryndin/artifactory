package org.artifactory.storage.binstore.service.providers;

import org.artifactory.binstore.BinaryInfo;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.BinaryNotFoundException;
import org.artifactory.storage.binstore.service.BinaryProviderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 * Support S3 Multi tries
 * Each request is being requested N times N=binary.provider.binaryProvider.max.retry.number property value (default =5)
 *
 * @author Gidi Shabat
 */
public class RetryBinaryProvider extends BinaryProviderBase {
    private static final Logger log = LoggerFactory.getLogger(RetryBinaryProvider.class);
    private int delayBetweenRetry;
    private int maxTrays;

    public RetryBinaryProvider(StorageProperties soStorageProperties) {
        this.maxTrays = soStorageProperties.getMaxRetriesNumber();
        this.delayBetweenRetry = soStorageProperties.getDelayBetweenRetries();
    }

    @Override
    public boolean exists(String sha1, long length) {
        return isExist(sha1, length, 0);
    }

    @Override
    public InputStream getStream(String path) {
        return getStream(path, 0);
    }

    @Override
    public BinaryInfo addStream(InputStream inputStream) throws IOException {
        return addStream(inputStream, 0);
    }

    @Override
    public boolean delete(String path) {
        return delete(path, 0);
    }

    public boolean delete(String path, int trying) {
        try {
            return next().delete(path);
        } catch (Exception e) {
            if (trying < maxTrays) {
                waitDelayTime();
                log.trace("Failed to delete blob from  '{}'  from S3, trying again for the  '{}'  time", path, trying);
                return delete(path, ++trying);
            } else {
                log.error("Failed to delete blob  '{}'  item from S3", path, e);
                return false;
            }
        }
    }

    public boolean isExist(String sha1, long length, int trying) {
        try {
            return next().exists(sha1, length);
        } catch (Exception e) {
            if (trying < maxTrays) {
                waitDelayTime();
                log.trace("Failed to check if blob  '{}'  exist in S3, trying again for the  '{}'  time", sha1, trying);
                return isExist(sha1, length, ++trying);
            } else {
                log.error("Failed to check if blob  '{}'  exist in S3", sha1, e);
                throw e;
            }
        }
    }

    @Nonnull
    public InputStream getStream(String path, int trying) {
        try {
            return next().getStream(path);
        } catch (Exception e) {
            if (e instanceof BinaryNotFoundException) {
                throw e;
            }
            if (trying < maxTrays) {
                waitDelayTime();
                log.trace("Failed to fetch  blob  '{}'  from S3, trying again for the  '{}'  time", path, trying);
                return getStream(path, ++trying);
            } else {
                log.error("Failed to fetch blob  '{}'  from S3", path, e);
                throw e;
            }
        }
    }

    public BinaryInfo addStream(InputStream inputStream, int trying) throws IOException {
        try {
            return next().addStream(inputStream);
        } catch (Exception e) {
            if (trying < maxTrays) {
                log.warn("Failed to add  blob to S3 for the '{}' time, a retry will start in seconds ", trying + 1);
                log.debug("Failed to add  blob to S3 for the '{}' time, a retry will start in seconds ", trying + 1, e);
                waitDelayTime();
                return addStream(inputStream, ++trying);
            } else {
                log.error("Failed to add blob to S3", e);
                throw e;
            }
        }

    }

    private void waitDelayTime() {
        try {
            Thread.sleep(delayBetweenRetry);
        } catch (InterruptedException e) {
            log.debug("waiting {} milli seconds before next retry");
        }
    }
}
