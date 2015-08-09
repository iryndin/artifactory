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

package org.artifactory.storage.db.build.dao;

import com.google.common.collect.Lists;
import org.apache.commons.compress.utils.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.storage.db.build.entity.BuildEntity;
import org.artifactory.storage.db.build.entity.BuildPromotionStatus;
import org.artifactory.storage.db.build.entity.BuildProperty;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.storage.db.util.blob.BlobWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Date: 10/30/12
 * Time: 12:44 PM
 *
 * @author freds
 */
@Repository
public class BuildsDao extends BaseDao {
    private static final Logger log = LoggerFactory.getLogger(BuildsDao.class);

    @Autowired
    public BuildsDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    public int createBuild(BuildEntity b, BlobWrapper jsonBlob) throws SQLException {
        int res = jdbcHelper.executeUpdate("INSERT INTO builds VALUES(" +
                "?, " +
                "?, ?, ?, " +
                "?, ?, ?," +
                "?, ?)",
                b.getBuildId(),
                b.getBuildName(), b.getBuildNumber(), b.getBuildDate(),
                b.getCiUrl(), b.getCreated(), b.getCreatedBy(),
                nullIfZero(b.getModified()), b.getModifiedBy());
        res += jdbcHelper.executeUpdate("INSERT INTO build_jsons VALUES(?,?)", b.getBuildId(), jsonBlob);
        int nbProps = b.getProperties().size();
        if (nbProps != 0) {
            for (BuildProperty bp : b.getProperties()) {
                String propValue = bp.getPropValue();
                if (propValue.length() > 2048) {
                    log.info("Trimming property value to 2048 characters {}", bp.getPropKey());
                    log.debug("Trimming property value to 2048 characters {}: {}", bp.getPropKey(), bp.getPropValue());
                    propValue = StringUtils.substring(propValue, 0, 2048);
                }
                res += jdbcHelper.executeUpdate("INSERT INTO build_props VALUES (?,?,?,?)",
                        bp.getPropId(), bp.getBuildId(), bp.getPropKey(), propValue);
            }
        }
        int nbPromotions = b.getPromotions().size();
        if (nbPromotions != 0) {
            for (BuildPromotionStatus bp : b.getPromotions()) {
                res += jdbcHelper.executeUpdate("INSERT INTO build_promotions VALUES (?,?,?,?,?,?,?)",
                        bp.getBuildId(), bp.getCreated(), bp.getCreatedBy(),
                        bp.getStatus(), bp.getRepository(), bp.getComment(), bp.getCiUser());
            }
        }
        return res;
    }

    public int rename(long buildId, String newName, BlobWrapper jsonBlob, String currentUser, long currentTime)
            throws SQLException {
        int res = jdbcHelper.executeUpdate("UPDATE builds SET" +
                " build_name = ?, modified = ?, modified_by = ?" +
                " WHERE build_id = ?", newName, currentTime, currentUser, buildId);
        res += jdbcHelper.executeUpdate("DELETE FROM build_jsons WHERE build_id=?", buildId);
        res += jdbcHelper.executeUpdate("INSERT INTO build_jsons VALUES(?,?)", buildId, jsonBlob);
        return res;
    }

    public int addPromotionStatus(long buildId, BuildPromotionStatus promotionStatus,
            BlobWrapper jsonBlob, String currentUser, long currentTime)
            throws SQLException {
        int res = jdbcHelper.executeUpdate("UPDATE builds SET" +
                " modified = ?, modified_by = ?" +
                " WHERE build_id = ?", currentTime, currentUser, buildId);
        res += jdbcHelper.executeUpdate("DELETE FROM build_jsons WHERE build_id=?", buildId);
        res += jdbcHelper.executeUpdate("INSERT INTO build_jsons VALUES(?,?)", buildId, jsonBlob);
        res += jdbcHelper.executeUpdate("INSERT INTO build_promotions VALUES (?,?,?,?,?,?,?)",
                promotionStatus.getBuildId(),
                promotionStatus.getCreated(),
                promotionStatus.getCreatedBy(),
                promotionStatus.getStatus(),
                promotionStatus.getRepository(),
                promotionStatus.getComment(),
                promotionStatus.getCiUser());
        return res;
    }

    public int deleteAllBuilds() throws SQLException {
        int res = jdbcHelper.executeUpdate("DELETE FROM build_jsons");
        res += jdbcHelper.executeUpdate("DELETE FROM build_props");
        res += jdbcHelper.executeUpdate("DELETE FROM build_promotions");
        res += jdbcHelper.executeUpdate("DELETE FROM builds");
        return res;
    }

    public int deleteBuild(long buildId) throws SQLException {
        int res = jdbcHelper.executeUpdate("DELETE FROM build_jsons WHERE build_id=?", buildId);
        res += jdbcHelper.executeUpdate("DELETE FROM build_props WHERE build_id=?", buildId);
        res += jdbcHelper.executeUpdate("DELETE FROM build_promotions WHERE build_id=?", buildId);
        res += jdbcHelper.executeUpdate("DELETE FROM builds WHERE build_id=?", buildId);
        return res;
    }

    public <T> T getJsonBuild(long buildId, Class<T> clazz) throws SQLException {
        ResultSet rs = null;
        InputStream jsonStream = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT build_info_json FROM build_jsons WHERE" +
                    " build_id = ?",
                    buildId);
            if (rs.next()) {
                jsonStream = rs.getBinaryStream(1);
                if (CharSequence.class.isAssignableFrom(clazz)) {
                    //noinspection unchecked
                    return (T) IOUtils.toString(jsonStream, Charsets.UTF_8.name());
                }
                return JacksonReader.streamAsClass(jsonStream, clazz);
            }
        } catch (IOException e) {
            throw new SQLException("Failed to read JSON data for build '" + buildId + "' due to: " + e.getMessage(), e);
        } finally {
            DbUtils.close(rs);
            IOUtils.closeQuietly(jsonStream);
        }
        return null;
    }

    public BuildEntity getBuild(long buildId) throws SQLException {
        ResultSet rs = null;
        BuildEntity build = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT * FROM builds WHERE" +
                    " build_id = ?",
                    buildId);
            if (rs.next()) {
                build = resultSetToBuild(rs);
            }
        } finally {
            DbUtils.close(rs);
        }
        if (build != null) {
            build.setProperties(findBuildProperties(build.getBuildId()));
            build.setPromotions(findBuildPromotions(build.getBuildId()));
        }
        return build;
    }

    public long findBuildId(String name, String number, long startDate) throws SQLException {
        ResultSet rs = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT build_id FROM builds WHERE" +
                    " build_name = ? AND build_number = ? AND build_date = ?",
                    name, number, startDate);
            if (rs.next()) {
                return rs.getLong(1);
            }
        } finally {
            DbUtils.close(rs);
        }
        return 0L;
    }

    public BuildEntity findBuild(String name, String number, long startDate) throws SQLException {
        long buildId = findBuildId(name, number, startDate);
        if (buildId > 0L) {
            return getBuild(buildId);
        }
        return null;
    }

    public BuildEntity getLatestBuild(String buildName) throws SQLException {
        long latestBuildDate = 0L;
        ResultSet rs = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT max(build_date) FROM builds WHERE build_name = ?", buildName);
            if (rs.next()) {
                latestBuildDate = rs.getLong(1);
            }
        } finally {
            DbUtils.close(rs);
            rs = null;
        }
        BuildEntity buildEntity = null;
        if (latestBuildDate > 0L) {
            try {
                rs = jdbcHelper.executeSelect("SELECT * FROM builds " +
                        "WHERE build_name = ? AND build_date = ?", buildName, latestBuildDate);
                if (rs.next()) {
                    buildEntity = resultSetToBuild(rs);
                }
            } finally {
                DbUtils.close(rs);
            }
        }
        if (buildEntity != null) {
            buildEntity.setProperties(findBuildProperties(buildEntity.getBuildId()));
            buildEntity.setPromotions(findBuildPromotions(buildEntity.getBuildId()));
        }
        return buildEntity;
    }

    public long findLatestBuildDate(String buildName, String buildNumber) throws SQLException {
        long latestBuildDate = 0L;
        ResultSet rs = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT max(build_date) FROM builds WHERE" +
                    " build_name = ? AND build_number = ?",
                    buildName, buildNumber);
            if (rs.next()) {
                latestBuildDate = rs.getLong(1);
            }
        } finally {
            DbUtils.close(rs);
        }
        return latestBuildDate;
    }

    public List<Long> findBuildIds(String buildName) throws SQLException {
        ResultSet rs = null;
        List<Long> buildIds = new ArrayList<>();
        try {
            rs = jdbcHelper.executeSelect("SELECT build_id FROM builds WHERE" +
                    " build_name = ? ORDER BY build_date DESC",
                    buildName);
            while (rs.next()) {
                buildIds.add(rs.getLong(1));
            }
        } finally {
            DbUtils.close(rs);
        }
        return buildIds;
    }

    public List<Long> findBuildIds(String buildName, String buildNumber) throws SQLException {
        ResultSet rs = null;
        List<Long> buildIds = new ArrayList<>();
        try {
            rs = jdbcHelper.executeSelect("SELECT build_id FROM builds WHERE" +
                    " build_name = ? AND build_number = ? ORDER BY build_date DESC",
                    buildName, buildNumber);
            while (rs.next()) {
                buildIds.add(rs.getLong(1));
            }
        } finally {
            DbUtils.close(rs);
        }
        return buildIds;
    }

    public List<String> getAllBuildNames() throws SQLException {
        ResultSet rs = null;
        List<String> buildNames = new ArrayList<>();
        try {
            rs = jdbcHelper.executeSelect(
                    "SELECT build_name, max(build_date) d FROM builds GROUP BY build_name ORDER BY d");
            while (rs.next()) {
                buildNames.add(rs.getString(1));
            }
        } finally {
            DbUtils.close(rs);
        }
        return buildNames;
    }

    public Collection<BuildEntity> findBuildsForArtifactChecksum(ChecksumType type, String checksum) throws SQLException {
        Collection<BuildEntity> results = Lists.newArrayList();
        ResultSet rs = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT DISTINCT b.* FROM builds b, build_artifacts ba, build_modules bm" +
                    " WHERE b.build_id = bm.build_id" +
                    " AND bm.module_id = ba.module_id" +
                    " AND ba." + type.name() + " = ?" +
                    " AND ba.module_id = bm.module_id", checksum);
            while (rs.next()) {
                results.add(resultSetToBuild(rs));
            }
        } finally {
            DbUtils.close(rs);
        }
        for (BuildEntity buildEntity : results) {
            buildEntity.setProperties(findBuildProperties(buildEntity.getBuildId()));
            buildEntity.setPromotions(findBuildPromotions(buildEntity.getBuildId()));
        }
        return results;
    }

    public Collection<BuildEntity> findBuildsForDependencyChecksum(ChecksumType type, String checksum) throws SQLException {
        Collection<BuildEntity> results = Lists.newArrayList();
        ResultSet rs = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT DISTINCT b.* FROM builds b, build_dependencies bd, build_modules bm" +
                    " WHERE b.build_id = bm.build_id" +
                    " AND bm.module_id = bd.module_id" +
                    " AND bd." + type.name() + " = ?" +
                    " AND bd.module_id = bm.module_id", checksum);
            while (rs.next()) {
                results.add(resultSetToBuild(rs));
            }
        } finally {
            DbUtils.close(rs);
        }
        for (BuildEntity buildEntity : results) {
            buildEntity.setProperties(findBuildProperties(buildEntity.getBuildId()));
            buildEntity.setPromotions(findBuildPromotions(buildEntity.getBuildId()));
        }
        return results;
    }

    private Set<BuildProperty> findBuildProperties(long buildId) throws SQLException {
        ResultSet rs = null;
        Set<BuildProperty> buildProperties = new HashSet<>();
        try {
            rs = jdbcHelper.executeSelect("SELECT * FROM build_props WHERE" +
                    " build_id = ?",
                    buildId);
            while (rs.next()) {
                buildProperties.add(resultSetToBuildProperty(rs));
            }
        } finally {
            DbUtils.close(rs);
        }
        return buildProperties;
    }

    private SortedSet<BuildPromotionStatus> findBuildPromotions(long buildId) throws SQLException {
        ResultSet rs = null;
        SortedSet<BuildPromotionStatus> buildPromotions = new TreeSet<>();
        try {
            rs = jdbcHelper.executeSelect("SELECT * FROM build_promotions WHERE" +
                    " build_id = ?",
                    buildId);
            while (rs.next()) {
                buildPromotions.add(resultSetToBuildPromotion(rs));
            }
        } finally {
            DbUtils.close(rs);
        }
        return buildPromotions;
    }

    private BuildProperty resultSetToBuildProperty(ResultSet rs) throws SQLException {
        return new BuildProperty(rs.getLong(1), rs.getLong(2),
                rs.getString(3), rs.getString(4));
    }

    private BuildPromotionStatus resultSetToBuildPromotion(ResultSet rs) throws SQLException {
        return new BuildPromotionStatus(rs.getLong(1), rs.getLong(2),
                rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                rs.getString(7));
    }

    private BuildEntity resultSetToBuild(ResultSet rs) throws SQLException {
        return new BuildEntity(rs.getLong(1),
                rs.getString(2), rs.getString(3), rs.getLong(4),
                rs.getString(5), rs.getLong(6), rs.getString(7),
                zeroIfNull(rs.getLong(8)), rs.getString(9)
        );
    }
}
