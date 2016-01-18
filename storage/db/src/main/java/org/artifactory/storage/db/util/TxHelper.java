package org.artifactory.storage.db.util;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Chen Keinan
 */
public class TxHelper {


    /**
     * check if thread in mid of spring transactional
     *
     * @return if true - it in spring transactional
     */
    public static boolean isInTransaction() {
        return TransactionSynchronizationManager.isSynchronizationActive();
    }

    /**
     * set isolation level to read uncommitted in case of aql queries
     *
     * @param allowDirtyReads - if true set isolation level to  read uncommitted
     * @param con             - sql connection
     * @throws SQLException
     */
    public static void allowDirtyReads(boolean allowDirtyReads, Connection con) throws SQLException {
        if (allowDirtyReads && con != null) {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }
    }

    /**
     * set isolation level to read committed in case of aql queries completed
     *
     * @param allowDirtyReads - if true set isolation level to  read committed
     * @param con             - sql connection
     * @throws SQLException
     */
    public static void disableDirtyReads(boolean allowDirtyReads, Connection con) throws SQLException {
        if (allowDirtyReads && con != null) {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
    }
}
