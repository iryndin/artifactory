package org.artifactory.keyval;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.config.ExportableConfig;
import org.artifactory.process.StatusHolder;
import org.artifactory.utils.SqlUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.jdbc.object.SqlUpdate;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 * Persistent key-value configuration manager Created by IntelliJ IDEA. User: yoavl
 */
public class KeyVals implements InitializingBean, ExportableConfig {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(KeyVals.class);

    private DataSource dataSource;

    private String version;
    private String revision;
    private String prevVersion;
    private String prevRevision;

    public static final String TABLE_KEYVAL = "keyval";
    public static final String KEY_VERSION = "artifactory.version";

    public static final String KEY_REVISION = "artifactory.revision";
    public static final String KEYVAL_SELECT_STATEMENT =
            "SELECT name, val FROM keyval where name = ?";
    public static final String KEYVAL_INSERT_STATEMENT =
            "INSERT INTO keyval (name, val) VALUES (?, ?)";
    public static final String KEYVAL_UPDATE_STATEMENT = "UPDATE keyval SET val = ? WHERE name = ?";

    public static final String KEYVAL_DELETE_STATEMENT = "DELETE FROM keyval WHERE name = ?";
    private KeyValSelect select;
    private final KeyValInsert insert;
    private final KeyValUpdate update;
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
    private final KeyValDelete delete;


    public KeyVals(DataSource dataSource) {
        this.dataSource = dataSource;
        select = new KeyValSelect(dataSource);
        insert = new KeyValInsert(dataSource);
        update = new KeyValUpdate(dataSource);
        delete = new KeyValDelete(dataSource);
    }

    public void afterPropertiesSet() throws Exception {
        boolean tableExists = SqlUtils.tableExists(TABLE_KEYVAL, dataSource);
        if (!tableExists) {
            //Create the config table if needed
            SqlUtils.executeResourceScript("sql/keyval.sql", dataSource);
            //Add the version key/val
            LOGGER.info("Storing Artifactory version '" + version + "' in the DB.");
            insert.update(KEY_VERSION, version);
            insert.update(KEY_REVISION, revision);
        } else {
            //Update the version key/val
            KeyVal verKeyVal = (KeyVal) select.findObject(KEY_VERSION);
            prevVersion = verKeyVal.getVal();
            KeyVal revKeyVal = (KeyVal) select.findObject(KEY_REVISION);
            prevRevision = revKeyVal.getVal();
            if (!revision.startsWith("${") && !revision.equals(prevRevision)) {
                LOGGER.info("Updating Artifactory revision in the DB from '" +
                        prevRevision + "' to '" + revision + "'.");
                update.update(KEY_VERSION, version);
                update.update(KEY_REVISION, revision);
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

    public void exportTo(String basePath, StatusHolder status) {
        //Store a properties file with the vals
        Properties props = new Properties();
        props.put(KEY_VERSION, version);
        props.put(KEY_REVISION, revision);
        File file = new File(basePath, "artifactory.properties");
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

    protected class KeyValSelect extends MappingSqlQuery {
        protected KeyValSelect(DataSource ds) {
            super(ds, KEYVAL_SELECT_STATEMENT);
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum)
                throws SQLException {
            String key = rs.getString(1);
            String val = rs.getString(2);
            return new KeyVal(key, val);
        }
    }

    protected class KeyValInsert extends SqlUpdate {
        protected KeyValInsert(DataSource ds) {
            super(ds, KEYVAL_INSERT_STATEMENT);
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }
    }

    protected class KeyValUpdate extends SqlUpdate {
        protected KeyValUpdate(DataSource ds) {
            super(ds, KEYVAL_UPDATE_STATEMENT);
            declareParameter(new SqlParameter(Types.VARCHAR));
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }
    }

    protected class KeyValDelete extends SqlUpdate {
        protected KeyValDelete(DataSource ds) {
            super(ds, KEYVAL_DELETE_STATEMENT);
            declareParameter(new SqlParameter(Types.VARCHAR));
            compile();
        }
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
