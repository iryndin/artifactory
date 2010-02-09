package org.artifactory.keyval;

import org.apache.log4j.Logger;
import org.artifactory.utils.SqlUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.jdbc.object.SqlUpdate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Persistent key-value configuration manager Created by IntelliJ IDEA. User: yoavl
 */
public class KeyVals implements InitializingBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(KeyVals.class);

    private DataSource dataSource;
    private String version;
    private String revision;

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
            KeyVal keyval = (KeyVal) select.findObject(KEY_VERSION);
            String prevVersion = keyval.getVal();
            LOGGER.info("Updating Artifactory version in the DB from '" + prevVersion + "' to '" +
                    version + "'.");
            update.update(KEY_VERSION, version);
            update.update(KEY_REVISION, revision);
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
