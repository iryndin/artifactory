/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.schedule.quartz;

import org.apache.commons.logging.Log;
import org.quartz.Calendar;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A delegate for quartz with a derby db persistent store. Uses ps.setBinaryStream instead of setBytes(PreparedStatement
 * ps, int index, ByteArrayOutputStream baos) Please keep for future use if we ever use db-based jobs again.
 */
public class DerbyDelegate extends StdJDBCDelegate {

    public DerbyDelegate(Log logger, String tablePrefix, String instanceId) {
        super(logger, tablePrefix, instanceId);
    }

    public DerbyDelegate(Log logger, String tablePrefix, String instanceId,
            Boolean useProperties) {
        super(logger, tablePrefix, instanceId, useProperties);
    }

    @Override
    public int insertJobDetail(Connection conn, JobDetail job)
            throws IOException, SQLException {
        //log.debug( "Inserting JobDetail " + job );
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        int len = baos.toByteArray().length;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        PreparedStatement ps = null;

        int insertResult = 0;

        try {
            ps = conn.prepareStatement(rtp(INSERT_JOB_DETAIL));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());
            ps.setString(3, job.getDescription());
            ps.setString(4, job.getJobClass().getName());
            setBoolean(ps, 5, job.isDurable());
            setBoolean(ps, 6, job.isVolatile());
            setBoolean(ps, 7, job.isStateful());
            setBoolean(ps, 8, job.requestsRecovery());
            ps.setBinaryStream(9, bais, len);

            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }

        if (insertResult > 0) {
            String[] jobListeners = job.getJobListenerNames();
            for (int i = 0; jobListeners != null && i < jobListeners.length; i++) {
                insertJobListener(conn, job, jobListeners[i]);
            }
        }

        return insertResult;
    }

    /**
     * <p/> Update the job detail record. </p>
     *
     * @param conn the DB Connection
     * @param job  the job to update
     * @return number of rows updated
     * @throws java.io.IOException if there were problems serializing the JobDataMap
     */
    @Override
    public int updateJobDetail(Connection conn, JobDetail job)
            throws IOException, SQLException {
        //log.debug( "Updating job detail " + job );
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        int len = baos.toByteArray().length;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        PreparedStatement ps = null;

        int insertResult = 0;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_JOB_DETAIL));
            ps.setString(1, job.getDescription());
            ps.setString(2, job.getJobClass().getName());
            setBoolean(ps, 3, job.isDurable());
            setBoolean(ps, 4, job.isVolatile());
            setBoolean(ps, 5, job.isStateful());
            setBoolean(ps, 6, job.requestsRecovery());
            ps.setBinaryStream(7, bais, len);
            ps.setString(8, job.getName());
            ps.setString(9, job.getGroup());

            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }

        if (insertResult > 0) {
            deleteJobListeners(conn, job.getName(), job.getGroup());

            String[] jobListeners = job.getJobListenerNames();
            for (int i = 0; jobListeners != null && i < jobListeners.length; i++) {
                insertJobListener(conn, job, jobListeners[i]);
            }
        }

        return insertResult;
    }

    public int insertTrigger(Connection conn, Trigger trigger, String state,
            JobDetail jobDetail) throws SQLException, IOException {

        ByteArrayOutputStream baos = serializeJobData(trigger.getJobDataMap());
        int len = baos.toByteArray().length;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        PreparedStatement ps = null;

        int insertResult = 0;

        try {
            ps = conn.prepareStatement(rtp(INSERT_TRIGGER));
            ps.setString(1, trigger.getName());
            ps.setString(2, trigger.getGroup());
            ps.setString(3, trigger.getJobName());
            ps.setString(4, trigger.getJobGroup());
            setBoolean(ps, 5, trigger.isVolatile());
            ps.setString(6, trigger.getDescription());
            ps.setBigDecimal(7, new BigDecimal(String.valueOf(trigger
                    .getNextFireTime().getTime())));
            long prevFireTime = -1;
            if (trigger.getPreviousFireTime() != null) {
                prevFireTime = trigger.getPreviousFireTime().getTime();
            }
            ps.setBigDecimal(8, new BigDecimal(String.valueOf(prevFireTime)));
            ps.setString(9, state);
            if (trigger.getClass() == SimpleTrigger.class) {
                ps.setString(10, TTYPE_SIMPLE);
            } else if (trigger.getClass() == CronTrigger.class) {
                ps.setString(10, TTYPE_CRON);
            } else {
                ps.setString(10, TTYPE_BLOB);
            }
            ps.setBigDecimal(11, new BigDecimal(String.valueOf(trigger
                    .getStartTime().getTime())));
            long endTime = 0;
            if (trigger.getEndTime() != null) {
                endTime = trigger.getEndTime().getTime();
            }
            ps.setBigDecimal(12, new BigDecimal(String.valueOf(endTime)));
            ps.setString(13, trigger.getCalendarName());
            ps.setInt(14, trigger.getMisfireInstruction());
            ps.setBinaryStream(15, bais, len);
            ps.setInt(16, trigger.getPriority());

            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }

        if (insertResult > 0) {
            String[] trigListeners = trigger.getTriggerListenerNames();
            for (int i = 0; trigListeners != null && i < trigListeners.length; i++) {
                insertTriggerListener(conn, trigger, trigListeners[i]);
            }
        }

        return insertResult;
    }

    public int updateTrigger(Connection conn, Trigger trigger, String state,
            JobDetail jobDetail) throws SQLException, IOException {

        ByteArrayOutputStream baos = serializeJobData(trigger.getJobDataMap());
        int len = baos.toByteArray().length;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        PreparedStatement ps = null;

        int insertResult = 0;


        try {
            ps = conn.prepareStatement(rtp(UPDATE_TRIGGER));

            ps.setString(1, trigger.getJobName());
            ps.setString(2, trigger.getJobGroup());
            setBoolean(ps, 3, trigger.isVolatile());
            ps.setString(4, trigger.getDescription());
            long nextFireTime = -1;
            if (trigger.getNextFireTime() != null) {
                nextFireTime = trigger.getNextFireTime().getTime();
            }
            ps.setBigDecimal(5, new BigDecimal(String.valueOf(nextFireTime)));
            long prevFireTime = -1;
            if (trigger.getPreviousFireTime() != null) {
                prevFireTime = trigger.getPreviousFireTime().getTime();
            }
            ps.setBigDecimal(6, new BigDecimal(String.valueOf(prevFireTime)));
            ps.setString(7, state);
            if (trigger.getClass() == SimpleTrigger.class) {
                //                updateSimpleTrigger(conn, (SimpleTrigger)trigger);
                ps.setString(8, TTYPE_SIMPLE);
            } else if (trigger.getClass() == CronTrigger.class) {
                //                updateCronTrigger(conn, (CronTrigger)trigger);
                ps.setString(8, TTYPE_CRON);
            } else {
                //                updateBlobTrigger(conn, trigger);
                ps.setString(8, TTYPE_BLOB);
            }
            ps.setBigDecimal(9, new BigDecimal(String.valueOf(trigger
                    .getStartTime().getTime())));
            long endTime = 0;
            if (trigger.getEndTime() != null) {
                endTime = trigger.getEndTime().getTime();
            }
            ps.setBigDecimal(10, new BigDecimal(String.valueOf(endTime)));
            ps.setString(11, trigger.getCalendarName());
            ps.setInt(12, trigger.getMisfireInstruction());

            ps.setInt(13, trigger.getPriority());
            ps.setBinaryStream(14, bais, len);
            ps.setString(15, trigger.getName());
            ps.setString(16, trigger.getGroup());

            insertResult = ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }

        if (insertResult > 0) {
            deleteTriggerListeners(conn, trigger.getName(), trigger.getGroup());

            String[] trigListeners = trigger.getTriggerListenerNames();
            for (int i = 0; trigListeners != null && i < trigListeners.length; i++) {
                insertTriggerListener(conn, trigger, trigListeners[i]);
            }
        }

        return insertResult;
    }

    public int updateJobData(Connection conn, JobDetail job)
            throws IOException, SQLException {
        //log.debug( "Updating Job Data for Job " + job );
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        int len = baos.toByteArray().length;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_JOB_DATA));
            ps.setBinaryStream(1, bais, len);
            ps.setString(2, job.getName());
            ps.setString(3, job.getGroup());

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    public int insertCalendar(Connection conn, String calendarName,
            Calendar calendar) throws IOException, SQLException {
        //log.debug( "Inserting Calendar " + calendarName + " : " + calendar
        // );
        ByteArrayOutputStream baos = serializeObject(calendar);
        byte buf[] = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_CALENDAR));
            ps.setString(1, calendarName);
            ps.setBinaryStream(2, bais, buf.length);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    public int updateCalendar(Connection conn, String calendarName,
            Calendar calendar) throws IOException, SQLException {
        //log.debug( "Updating calendar " + calendarName + " : " + calendar );
        ByteArrayOutputStream baos = serializeObject(calendar);
        byte buf[] = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);

        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_CALENDAR));
            ps.setBinaryStream(1, bais, buf.length);
            ps.setString(2, calendarName);

            return ps.executeUpdate();
        } finally {
            closeStatement(ps);
        }
    }

    protected Object getObjectFromBlob(ResultSet rs, String colName)
            throws ClassNotFoundException, IOException, SQLException {
        //log.debug( "Getting blob from column: " + colName );
        Object obj = null;

        byte binaryData[] = rs.getBytes(colName);

        InputStream binaryInput = new ByteArrayInputStream(binaryData);

        ObjectInputStream in = new ObjectInputStream(binaryInput);
        try {
            obj = in.readObject();
        } finally {
            in.close();
        }

        return obj;
    }

    protected Object getJobDetailFromBlob(ResultSet rs, String colName)
            throws ClassNotFoundException, IOException, SQLException {
        if (canUseProperties()) {
            byte data[] = rs.getBytes(colName);
            if (data == null) {
                return null;
            }
            InputStream binaryInput = new ByteArrayInputStream(data);
            return binaryInput;
        }
        return getObjectFromBlob(rs, colName);
    }
}