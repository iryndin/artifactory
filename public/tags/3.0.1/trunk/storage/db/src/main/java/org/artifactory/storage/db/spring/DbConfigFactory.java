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

package org.artifactory.storage.db.spring;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.db.DbType;
import org.artifactory.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.util.StringUtils;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;

/**
 * A Spring {@link org.springframework.context.annotation.Configuration} to initialized database beans.
 *
 * @author Yossi Shaul
 */
@Configuration
public class DbConfigFactory implements BeanFactoryAware {
    private static final Logger log = LoggerFactory.getLogger(DbConfigFactory.class);

    public static final String STORAGE_PROPS_FILE_NAME = "storage.properties";
    public static final String BEAN_PREFIX = "bean:";
    public static final String JNDI_PREFIX = "jndi:";

    private BeanFactory beanFactory;

    @Bean(name = "dataSource")
    public DataSource createDataSource() {
        StorageProperties storageProperties = beanFactory.getBean("storageProperties", StorageProperties.class);
        DataSource result = getDataSourceFromBeanOrJndi(storageProperties, "");
        if (result != null) {
            return result;
        } else {
            return new ArtifactoryDataSource(storageProperties);
        }
    }

    private DataSource getDataSourceFromBeanOrJndi(StorageProperties storageProperties, String suffix) {
        DataSource result = null;
        String connectionUrl = storageProperties.getConnectionUrl();
        if (StringUtils.startsWithIgnoreCase(connectionUrl, BEAN_PREFIX)) {
            result = beanFactory.getBean(connectionUrl.substring(BEAN_PREFIX.length()) + suffix, DataSource.class);
        } else if (StringUtils.startsWithIgnoreCase(connectionUrl, JNDI_PREFIX)) {
            String jndiName = connectionUrl.substring(JNDI_PREFIX.length());
            JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
            jndiObjectFactoryBean.setJndiName(jndiName + suffix);
            try {
                jndiObjectFactoryBean.afterPropertiesSet();
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
            result = (DataSource) jndiObjectFactoryBean.getObject();
        }
        return result;
    }

    /**
     * Returns a separate non-transactional auto-commit datasource. This data source is currently used only by the id
     * generator.
     *
     * @return An auto-commit non-transactional datasource.
     */
    @Bean(name = "uniqueIdsDataSource")
    public DataSource createUniqueIdsDataSource() {
        StorageProperties storageProperties = beanFactory.getBean(StorageProperties.class);
        DataSource result = getDataSourceFromBeanOrJndi(storageProperties, "noTX");
        if (result != null) {
            return result;
        }

        GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
        poolConfig.maxActive = 1;
        poolConfig.maxIdle = 1;
        GenericObjectPool connectionPool = new GenericObjectPool(null, poolConfig);

        String connectionUrl = storageProperties.getConnectionUrl();
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
                connectionUrl,
                storageProperties.getUsername(), storageProperties.getPassword());

        // default auto commit true!
        PoolableConnectionFactory pcf = new ArtifactoryPoolableConnectionFactory(connectionFactory,
                connectionPool, null, null, false, true);
        pcf.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        return new ArtifactoryDataSource(connectionUrl, connectionPool);
    }

    @Bean(name = "storageProperties")
    public StorageProperties getDbProperties() throws IOException {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();

        // TODO: [by YS] read the database used to start artifactory from artifactory.properties.
        // should fail if it used to be non derby and the storage.properties is not found

        File storagePropsFile = new File(artifactoryHome.getEtcDir().getAbsoluteFile(), "/" + STORAGE_PROPS_FILE_NAME);
        if (!storagePropsFile.exists()) {
            copyDefaultDerbyConfig(storagePropsFile);
        }

        log.debug("Loading database properties from: '{}'", storagePropsFile);
        StorageProperties storageProps = new StorageProperties(storagePropsFile);

        // configure embedded derby
        if (isDerbyDbUsed(storageProps.getDbType())) {
            System.setProperty("derby.stream.error.file",
                    new File(artifactoryHome.getLogDir(), "derby.log").getAbsolutePath());
            String url = storageProps.getConnectionUrl();
            String dataDir = FilenameUtils.separatorsToUnix(artifactoryHome.getDataDir().getAbsolutePath());
            url = url.replace("{db.home}", dataDir + "/derby");
            storageProps.setConnectionUrl(url);
        }

        // first for loading of the driver class. automatic registration doesn't work on some Tomcat installations
        String driver = storageProps.getDriverClass();
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load JDBC driver '" + driver + "'", e);
        }

        return storageProps;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private boolean isDerbyDbUsed(DbType dbType) {
        return dbType.equals(DbType.DERBY);
    }

    private void copyDefaultDerbyConfig(File targetStorageFile) throws IOException {
        try (InputStream pis = ResourceUtils.getResource("/META-INF/default/db/derby.properties")) {
            FileUtils.copyInputStreamToFile(pis, targetStorageFile);
        }
    }
}
