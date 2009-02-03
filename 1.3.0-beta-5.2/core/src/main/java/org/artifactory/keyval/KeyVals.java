package org.artifactory.keyval;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.ocm.OcmStorable;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.PostInitializingBean;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Persistent key-value configuration manager Created by IntelliJ IDEA. User: yoavl
 */
@Node(extend = OcmStorable.class)
public class KeyVals implements ImportableExportable, OcmStorable, PostInitializingBean {
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

    public static final String KEY_VERSION = "artifactory.version";
    public static final String KEY_REVISION = "artifactory.revision";

    /**
     * Needed by OCM
     */
    public KeyVals() {
    }

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addPostInit(getClass());
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends PostInitializingBean>[] initAfter() {
        return new Class[]{JcrService.class};
    }

    @Transactional
    public void init() {
        JcrService jcr = InternalContextHelper.get().getJcrService();
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
                ocm.update(this);
                ocm.save();
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

    public void setJcrPath(String path) {
        //noop
    }

    @Transactional
    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Exporting key values...", LOGGER);
        //Store a properties file with the vals at the root folder
        Properties props = new Properties();
        props.put(KEY_VERSION, version);
        props.put(KEY_REVISION, revision);
        File file = new File(settings.getBaseDir(), "artifactory.properties");
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

    @Transactional
    public void importFrom(ImportSettings settings, StatusHolder status) {
        status.setStatus("Importing key values...", LOGGER);
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

        @Override
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

        @Override
        public int hashCode() {
            int result;
            result = key.hashCode();
            result = 31 * result + (val != null ? val.hashCode() : 0);
            return result;
        }
    }
}
