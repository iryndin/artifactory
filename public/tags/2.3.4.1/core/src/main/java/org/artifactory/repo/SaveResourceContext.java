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

package org.artifactory.repo;

import org.artifactory.api.fs.RepoResource;
import org.artifactory.md.Properties;

import java.io.InputStream;

/**
 * @author Noam Y. Tenne
 */
public class SaveResourceContext {

    private final RepoResource repoResource;
    private final InputStream inputStream;
    private final Properties properties;
    private final long created;
    private final String createBy;
    private final String modifiedBy;

    public SaveResourceContext(RepoResource repoResource, InputStream inputStream, Properties properties,
            long created, String createBy, String modifiedBy) {
        this.repoResource = repoResource;
        this.inputStream = inputStream;
        this.properties = properties;
        this.created = created;
        this.createBy = createBy;
        this.modifiedBy = modifiedBy;
    }

    public RepoResource getRepoResource() {
        return repoResource;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public Properties getProperties() {
        return properties;
    }

    public long getCreated() {
        return created;
    }

    public String getCreateBy() {
        return createBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public static class Builder {

        private RepoResource repoResource;
        private InputStream inputStream;
        private Properties properties;
        private long created;
        private String createBy;
        private String modifiedBy;

        public Builder(RepoResource repoResource, InputStream inputStream) {
            this.repoResource = repoResource;
            this.inputStream = inputStream;
        }

        public Builder repoResource(RepoResource repoResource) {
            this.repoResource = repoResource;
            return this;
        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder properties(Properties properties) {
            this.properties = properties;
            return this;
        }

        public Builder created(long created) {
            this.created = created;
            return this;
        }

        public Builder createdBy(String createBy) {
            this.createBy = createBy;
            return this;
        }

        public Builder modifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public SaveResourceContext build() {
            return new SaveResourceContext(repoResource, inputStream, properties, created, createBy, modifiedBy);
        }
    }
}
