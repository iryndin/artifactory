package org.artifactory.io.checksum;

import org.apache.commons.io.IOUtils;
import org.artifactory.checksum.ChecksumType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Chen Keinan
 */
public class ChecksumUtil {
    private static final Logger log = LoggerFactory.getLogger(ChecksumUtil.class);

    public static String getChecksum(ChecksumType checksumType, InputStream inputStream)  {
        StringBuilder checksumBuilder = new StringBuilder();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(checksumType.alg());

        byte[] dataBytes = new byte[1024];
        int nread;
        while ((nread = inputStream.read(dataBytes)) != -1) {
            digest.update(dataBytes, 0, nread);
         }
        byte[] bytes = digest.digest();
        if (bytes.length * 2 != checksumType.length()) {
            int bitLength = bytes.length * 8;
            throw new IllegalArgumentException(
                    "Unrecognised length for binary data: " + bitLength + " bits instead of " + (checksumType.length() * 4));
        }
         for (byte aBinaryData : bytes) {
            String t = Integer.toHexString(aBinaryData & 0xff);
            if (t.length() == 1) {
                checksumBuilder.append("0");
            }
            checksumBuilder.append(t);
        }
        } catch (NoSuchAlgorithmException e) {
            log.debug(e.toString());
        } catch (IOException e) {
          log.debug(e.toString());
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
        return checksumBuilder.toString();
    }
}
