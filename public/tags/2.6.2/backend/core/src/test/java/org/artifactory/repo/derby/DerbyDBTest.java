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

package org.artifactory.repo.derby;

import org.apache.jackrabbit.spi.commons.name.NameConstants;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * User: freds Date: Jun 14, 2007 Time: 10:58:02 AM
 */
public class DerbyDBTest {
    enum JcrTable {
        DEFAULT_BINVAL, DEFAULT_NODE, DEFAULT_PROP, DEFAULT_REFS, REP_FSENTRY,
        VER_BINVAL, VER_FS_FSENTRY, VER_NODE, VER_PROP, VER_REFS
    }

    private static final byte[] buff = new byte[12000];

    public static void main(String[] args) {
        Connection dbConn = null;
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            String artHome = System.getProperty("artifactory.home");
            dbConn = DriverManager.getConnection("jdbc:derby:" + artHome + "/data/jcr/db");
            //            printTableColumns(dbConn);
            printTableSizes(dbConn);
            Statement stat = dbConn.createStatement();
            stat.setMaxRows(20);
            //            listRepFSEntry(stat);
            //            listDefaultNode(stat);
            listDefaultProp(stat, false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void listDefaultNode(Statement stat) throws SQLException, IOException {
        ResultSet rs;
        rs = stat.executeQuery("select * from DEFAULT_NODE");
        while (rs.next()) {
            System.out.println("" + rs.getObject("NODE_ID"));
            Blob blob = rs.getBlob("NODE_DATA");
            if (!rs.wasNull() && blob != null) {
                InputStream is = blob.getBinaryStream();
                int n;
                while ((n = is.read(buff)) > 0) {
                    String s = new String(buff, 0, n);
                    System.out.println(s);
                }
            }
        }
    }

    private static void listDefaultProp(Statement stat, boolean deleteLocks)
            throws SQLException, IOException {
        boolean foundLocks = false;
        ResultSet rs;

        String sql = "select * from DEFAULT_PROP where prop_id='" + NameConstants.JCR_LOCKISDEEP +
                "' or prop_id='" + NameConstants.JCR_LOCKOWNER + "'";
        System.out.println("SQL Query " + sql);
        rs = stat.executeQuery(sql);
        while (rs.next()) {
            System.out.println("****************************");
            System.out.println("" + rs.getObject("PROP_ID"));
            Blob blob = rs.getBlob("PROP_DATA");
            if (!rs.wasNull() && blob != null) {
                foundLocks = true;
                InputStream is = blob.getBinaryStream();
                int n;
                while ((n = is.read(buff)) > 0) {
                    String s = new String(buff, 0, n);
                    System.out.println(s);
                }
            }
        }

        if (deleteLocks && foundLocks) {
            // The entries with property named QName.JCR_LOCKOWNER and QName.JCR_LOCKISDEEP
            // Should be set to null to remove all locks
            stat.executeUpdate(
                    "delete default_prop where prop_id='" + NameConstants.JCR_LOCKISDEEP +
                            "' or prop_id='" + NameConstants.JCR_LOCKOWNER + "'");
        }
    }

    private static void listRepFSEntry(Statement stat) throws SQLException, IOException {
        ResultSet rs = stat.executeQuery("select * from rep_fsentry");
        while (rs.next()) {
            System.out.println(
                    "" + rs.getObject("FSENTRY_PATH") + ":" + rs.getObject("FSENTRY_NAME"));
            Blob blob = rs.getBlob("FSENTRY_DATA");
            if (!rs.wasNull() && blob != null) {
                InputStream is = blob.getBinaryStream();
                int n;
                while ((n = is.read(buff)) > 0) {
                    String s = new String(buff, 0, n);
                    System.out.println(s);
                }
            }
        }
        rs.close();
    }

    private static void printTableSizes(Connection dbConn) throws SQLException {
        Statement stat = dbConn.createStatement();
        stat.setMaxRows(1);
        for (JcrTable jcrTable : JcrTable.values()) {
            System.out.println("********************************");
            ResultSet rs = stat.executeQuery("select count(*) from " + jcrTable.name());
            if (rs.next()) {
                System.out.println(jcrTable.name() + " has " + rs.getObject(1) + " rows");
            }
            rs.close();
        }
        System.out.println("********************************");
        System.out.println();
    }

    private static void printTableColumns(Connection dbConn) throws SQLException {
        DatabaseMetaData dbMD = dbConn.getMetaData();
        for (JcrTable jcrTable : JcrTable.values()) {
            System.out.println("********************************");
            System.out.println("Columns of " + jcrTable);
            ResultSet columns = dbMD.getColumns(null, null, jcrTable.name(), null);
            ResultSetMetaData rsMD = columns.getMetaData();
            int nbCols = rsMD.getColumnCount();
            for (int i = 0; i < nbCols; i++) {
                System.out.print(rsMD.getColumnName(i + 1) + ":");
            }
            System.out.println();
            while (columns.next()) {
                for (int i = 0; i < nbCols; i++) {
                    System.out.print(columns.getObject(i + 1) + ":");
                }
                System.out.println("");
            }
        }
        System.out.println("********************************");
        System.out.println();
    }
}
