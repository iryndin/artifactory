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

package org.artifactory.jcr.version.v240;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.checksum.ChecksumStorageHelper;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.Checksums;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.spring.ArtifactoryStorageContext;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.jcr.version.MarkerFileConverterBase;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.Pair;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;
import static org.artifactory.checksum.ChecksumInfo.TRUSTED_FILE_MARKER;
import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_SHA1_ACTUAL;
import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_SHA1_ORIGINAL;

/**
 * Converter Original to Actual depending on state
 *
 * @author Fred Simon
 */
public class ActualChecksumsConverter extends MarkerFileConverterBase {
    private static final Logger log = LoggerFactory.getLogger(ActualChecksumsConverter.class);
    public static final String ACTUAL_CHECKSUMS_MARKER_FILENAME = ".actual.checksums.convert";

    @Override
    public void convert(JcrSession jcrSession) {
        createMarkerFile(ACTUAL_CHECKSUMS_MARKER_FILENAME, "Actual checksums converter");
    }

    @Override
    public void applyConversion() {
        if (getMarkerFile().exists()) {
            convertAllActualChecksumsProperties();
        }
    }

    @Override
    public boolean needConversion() {
        return getMarkerFile().exists();
    }

    protected File getMarkerFile() {
        return new File(ArtifactoryHome.get().getDataDir(), ACTUAL_CHECKSUMS_MARKER_FILENAME);
    }

    enum ConversionType {noOrig, nullActual, notEqual, sleep}

    class ConverterExecutor {
        final ConversionType convType;
        final ChecksumType checksumType;
        int lastRun = -2;

        ConverterExecutor(ConversionType convType, ChecksumType checksumType) {
            this.convType = convType;
            this.checksumType = checksumType;
        }

        boolean isError() {
            return lastRun == -1;
        }

        public int execute() {
            if (lastRun == 0) {
                // already done
                return lastRun;
            }
            lastRun = internalExec();
            return lastRun;
        }

        private int internalExec() {
            switch (convType) {
                case noOrig:
                    return convertNoOrig(checksumType);
                case nullActual:
                    return convertNulls(checksumType);
                case notEqual:
                    return convertNotEqual();
                case sleep:
                    return sleep3secs();
            }
            return -1;
        }
    }

    public boolean convertAllActualChecksumsProperties() {
        long start = System.currentTimeMillis();
        boolean errors = false;
        // All converter method returns an int with:
        // -1 : Error
        // > 0 : Number of nodes transformed
        // 0 : Nothing done
        List<ConverterExecutor> executors = Lists.newArrayList(
                new ConverterExecutor(ConversionType.noOrig, ChecksumType.sha1),
                new ConverterExecutor(ConversionType.nullActual, ChecksumType.sha1),
                new ConverterExecutor(ConversionType.notEqual, ChecksumType.sha1),
                new ConverterExecutor(ConversionType.sleep, ChecksumType.sha1),
                new ConverterExecutor(ConversionType.noOrig, ChecksumType.md5),
                new ConverterExecutor(ConversionType.nullActual, ChecksumType.md5)
        );
        int totalConverted = 0;
        for (ConverterExecutor executor : executors) {
            totalConverted += executor.execute();
            if (executor.isError()) {
                errors = true;
            }
        }
        log.info("Actual Checksums conversion converted " +
                totalConverted + " nodes in " + (System.currentTimeMillis() - start) + "ms");
        if (!errors) {
            FileUtils.deleteQuietly(getMarkerFile());
            log.info("Full conversion of actual checksums finished successfully");
            return true;
        } else {
            log.error("Actual checksums conversion did not complete successfully");
            return false;
        }
    }

    private int sleep3secs() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignore) {
            Thread.interrupted();
            return -1;
        }
        return 0;
    }

    private int convertNoOrig(ChecksumType type) {
        JcrQuerySpec querySpec = JcrQuerySpec.xpath(new StringBuilder().append("/jcr:root").
                append("//. [@").append(ChecksumStorageHelper.getActualPropName(type)).append("='NO_ORIG']").
                toString()).noLimit();

        log.info("Converting Actual Checksums for NO_ORIG");
        return convertNodes(querySpec, "no original " + type);
    }

    private int convertNulls(ChecksumType type) {
        JcrQuerySpec querySpec = JcrQuerySpec.xpath(new StringBuilder().append("/jcr:root").
                append("//. [@").
                append(ChecksumStorageHelper.getOriginalPropName(type)).append(" and not(@").
                append(ChecksumStorageHelper.getActualPropName(type)).append(")]").
                toString()).noLimit();

        log.info("Converting Empty Actual Checksums");
        return convertNodes(querySpec, "no actual " + type);
    }

    private int convertNotEqual() {
        JcrQuerySpec querySpec = JcrQuerySpec.xpath(new StringBuilder().append("/jcr:root").
                append("//. [@").
                append(PROP_ARTIFACTORY_SHA1_ACTUAL).append(" and @").
                append(PROP_ARTIFACTORY_SHA1_ORIGINAL).append(" and @").
                append(PROP_ARTIFACTORY_SHA1_ACTUAL).append("!='NO_ORIG' and @").
                append(PROP_ARTIFACTORY_SHA1_ORIGINAL).append("!='NO_ORIG' and @").
                append(PROP_ARTIFACTORY_SHA1_ORIGINAL).append("!=@").append(PROP_ARTIFACTORY_SHA1_ACTUAL).append("]").
                toString()).noLimit();

        log.info("Converting Empty Actual Checksums");
        return convertNodes(querySpec, "all valid");
    }

    private int convertNodes(JcrQuerySpec querySpec, String passName) {
        int count = 0;
        boolean errors = false;
        long start = System.currentTimeMillis();
        ArtifactoryStorageContext context = StorageContextHelper.get();
        JcrService jcrService = context.getJcrService();
        JcrSession unmanagedSession = jcrService.getUnmanagedSession();
        try {
            log.info("Starting actual checksums pass " + passName + " conversion!");
            if (log.isTraceEnabled()) {
                JcrUtils.preorder(unmanagedSession.getRootNode());
            }
            QueryResult result = jcrService.executeQuery(querySpec, unmanagedSession);
            NodeIterator resultNodes = result.getNodes();

            // Need to save all the nodes in an array list because the iterator will
            // skip some nodes due to the fetch size :)
            List<Node> results = Lists.newArrayList();
            while (resultNodes.hasNext()) {
                results.add(resultNodes.nextNode());
            }
            for (Node node : results) {
                if (verifyChecksumsProperties(node)) {
                    unmanagedSession.save();
                }
                count++;
                if (count % 250 == 0) {
                    log.info("Actual checksums pass " + passName +
                            " converted " + count);
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while converting Checksums properties: {}", e.getMessage());
            log.debug("Error occurred while converting Checksums properties.", e);
            errors = true;
        } finally {
            unmanagedSession.logout();
        }
        String msg = "Actual checksums conversion " + passName +
                " finished in " + (System.currentTimeMillis() - start) + "ms and " +
                "converted " + count + " nodes";
        if (errors) {
            msg = "Some errors occurred in " + msg;
            log.error(msg);
            return -1;
        }
        log.info(msg);
        return count;
    }

    static class ChecksumsValue {
        final ChecksumType checksumType;
        String origVal = null;
        String actualVal = null;

        ChecksumsValue(ChecksumType checksumType) {
            this.checksumType = checksumType;
        }

        boolean actualValid() {
            return checksumType.isValid(actualVal);
        }

        boolean originalValid() {
            return checksumType.isValid(origVal);
        }

        boolean allOK() {
            return actualValid() &&
                    (!originalValid() || actualVal.equals(origVal));
        }
    }

    public boolean verifyChecksumsProperties(Node artifactNode) throws RepositoryException, IOException {
        boolean didConvert = false;
        boolean needFullScan = false;
        ChecksumType[] checksumTypes = ChecksumType.values();
        for (ChecksumType checksumType : checksumTypes) {
            String originalPropName = ChecksumStorageHelper.getOriginalPropName(checksumType);
            String actualPropName = ChecksumStorageHelper.getActualPropName(checksumType);
            ChecksumsValue value = getChecksumsValue(artifactNode, checksumType, originalPropName, actualPropName);
            if (value.allOK()) {
                continue;
            }
            if (!value.originalValid()) {
                // Original checksum invalid (so actual is also invalid or will have stopped at allOK)
                // All checksums invalid => Need full scan
                needFullScan = true;
                break;
            } else {
                if (value.actualValid()) {
                    // All checksums valid, (so not equal or will have stopped at allOK)
                    needFullScan = true;
                    break;
                }
                if (value.actualVal == null || value.actualVal.length() == 0) {
                    log.debug(
                            "Switching null actual with original for " + checksumType + " at " + artifactNode.getPath());
                    // Switch and remove original property
                    didConvert = true;
                    artifactNode.getProperty(originalPropName).remove();
                    artifactNode.setProperty(actualPropName, value.origVal);
                }
                if (TRUSTED_FILE_MARKER.equals(value.actualVal)) {
                    log.debug(
                            "Switching no orig actual with original for " + checksumType + " at " + artifactNode.getPath());
                    // Just switch
                    didConvert = true;
                    artifactNode.setProperty(originalPropName, TRUSTED_FILE_MARKER);
                    artifactNode.setProperty(actualPropName, value.origVal);
                }
            }
        }
        if (needFullScan) {
            InputStream stream = null;
            try {
                log.debug("Doing a full scan at " + artifactNode.getPath());
                stream = artifactNode.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary().getStream();
                Pair<Long, Checksum[]> longPair = Checksums.calculateWithLength(stream, ChecksumType.sha1,
                        ChecksumType.md5);
                Checksum[] second = longPair.getSecond();
                for (Checksum checksum : second) {
                    String actualPropName = ChecksumStorageHelper.getActualPropName(checksum.getType());
                    String originalPropName = ChecksumStorageHelper.getOriginalPropName(checksum.getType());
                    ChecksumsValue value = getChecksumsValue(artifactNode, checksum.getType(), originalPropName,
                            actualPropName);
                    if (checksum.getChecksum().equals(value.origVal)) {
                        // Just switch
                        didConvert = true;
                        artifactNode.setProperty(originalPropName, value.actualVal);
                        artifactNode.setProperty(actualPropName, value.origVal);
                    } else if (checksum.getChecksum().equals(value.actualVal)) {
                        // Actual are OK, leave it
                    } else {
                        // Force new actual
                        didConvert = true;
                        artifactNode.setProperty(actualPropName, checksum.getChecksum());
                    }
                    // force null original if totally invalid
                    if (value.origVal != null && !value.originalValid() && !TRUSTED_FILE_MARKER.equals(value.origVal)) {
                        didConvert = true;
                        artifactNode.getProperty(originalPropName).remove();
                    }
                }
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        return didConvert;
    }

    private ChecksumsValue getChecksumsValue(Node artifactNode, ChecksumType checksumType,
            String originalPropName, String actualPropName) throws RepositoryException {
        ChecksumsValue value = new ChecksumsValue(checksumType);
        if (artifactNode.hasProperty(originalPropName)) {
            value.origVal = artifactNode.getProperty(originalPropName).getString();
        }
        if (artifactNode.hasProperty(actualPropName)) {
            value.actualVal = artifactNode.getProperty(actualPropName).getString();
        }
        return value;
    }
}
