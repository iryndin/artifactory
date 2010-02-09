/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.utils;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SqlUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SqlUtils.class);

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static boolean tableExists(String tableName, DataSource dataSource) {
        boolean exits;
        try {
            DatabaseMetaData databaseMetaData = dataSource.getConnection().getMetaData();
            exits = databaseMetaData.getTables("", null, tableName.toUpperCase(), null).next();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check table existence.", e);
        }
        return exits;
    }

    public static void executeResourceScript(String resourcePath, DataSource dataSource) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(resourcePath);
        if (is ==null) {
            throw new RuntimeException("Failed to locate sql resource at '" + resourcePath + "'.");
        }
        List<String> sqlsList = new ArrayList<String>();
        try {
            String sql = IOUtils.toString(is);
            //Break the script into individual statemnts
            StringTokenizer tokenizer = new StringTokenizer(sql, ";");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken().trim();
                token = token.replace('\n', ' ');
                //Ignore comments
                if (!token.startsWith("--") && token.length() > 0) {
                    sqlsList.add(token);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute sql script.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String[] sqls = sqlsList.toArray(new String[]{});
        jdbcTemplate.batchUpdate(sqls);
    }
}
