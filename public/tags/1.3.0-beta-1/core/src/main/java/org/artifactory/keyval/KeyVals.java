package org.artifactory.keyval;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.log4j.Logger;
import org.artifactory.config.ExportableConfig;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.jcr.ocm.OcmStorable;
import org.artifactory.process.StatusHolder;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Persistent key-value configuration manager Created by IntelliJ IDEA. User: yoavl
 */
@Node(extend = OcmStorable.class)
public class KeyVals
        implements InitializingBean, ApplicationContextAware, ExportableConfig, OcmStorable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(KeyVals.class);

    private static final String KEYVALS_KEY = "keyvals";

    @Field
    private String version;
    @Field
    private String revision;
    @Field
    private String prevVersion;
    @Field
    private String prevRevision;

    private ArtifactoryApplicationContext artifactoryContext;

    public static final String KEY_VERSION = "artifactory.version";
    public static final String KEY_REVISION = "artifactory.revision";

    /**
     * Needed by OCM
     */
    public KeyVals() {
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.artifactoryContext = (ArtifactoryApplicationContext) applicationContext;
    }

    /**
     * Needed by OCM - erronousely looks for a getter annotation even if there is no getter
     */
    public ArtifactoryApplicationContext getApplicationContext() {
        return artifactoryContext;
    }

    public void afterPropertiesSet() throws Exception {
        JcrWrapper jcr = artifactoryContext.getJcr();
        ObjectContentManager ocm = jcr.getOcm();
        KeyVals prevKeyVals = (KeyVals) ocm.getObject(getJcrPath());
        if (prevKeyVals == null) {
            //Add the version key/val
            LOGGER.info(
                    "Storing Artifactory version '" + version + "', revision '" + revision + "'.");
            ocm.insert(this);
            ocm.save();
        } else {
            //Update the version key/val
            prevVersion = prevKeyVals.version;
            prevRevision = prevKeyVals.revision;
            if (!revision.startsWith("${") && !revision.equals(prevRevision)) {
                LOGGER.info("Updating Artifactory revision from '" +
                        prevRevision + "' to '" + revision + "'.");
                try {
                    ocm.update(this);
                    ocm.save();
                } catch (Exception e) {
                    LOGGER.warn("Revision not updated!.");
                    throw e;
                }
                LOGGER.info("Revision updated successfully.");
            } else {
                LOGGER.info("Previous Artifactory revision is '" + prevRevision +
                        "'. Current revision is '" + revision + "'.");
            }
        }
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getPrevVersion() {
        return prevVersion;
    }

    public String getPrevRevision() {
        return prevRevision;
    }

    public void setPrevVersion(String prevVersion) {
        this.prevVersion = prevVersion;
    }

    public void setPrevRevision(String prevRevision) {
        this.prevRevision = prevRevision;
    }

    public String getJcrPath() {
        return JcrPath.get().getOcmClassJcrPath(KEYVALS_KEY);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setJcrPath(String path) {
        //noop
    }

    public void exportTo(File exportDir, StatusHolder status) {
        //Store a properties file with the vals at the root folder
        Properties props = new Properties();
        props.put(KEY_VERSION, version);
        props.put(KEY_REVISION, revision);
        File file = new File(exportDir, "artifactory.properties");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            props.store(fos, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export properties.", e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    public void importFrom(String basePath, StatusHolder status) {
        //For now we only have version and revision which should not be imported/exported
    }

    public static class KeyVal {
        private String key;
        private String val;

        public KeyVal(String key, String val) {
            this.key = key;
            this.val = val;
        }

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            KeyVal val1 = (KeyVal) o;
            return key.equals(val1.key) &&
                    !(val != null ? !val.equals(val1.val) : val1.val != null);

        }

        public int hashCode() {
            int result;
            result = key.hashCode();
            result = 31 * result + (val != null ? val.hashCode() : 0);
            return result;
        }
    }
}
