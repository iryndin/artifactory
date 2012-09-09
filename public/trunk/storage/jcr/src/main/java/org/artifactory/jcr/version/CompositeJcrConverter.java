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

package org.artifactory.jcr.version;

import org.artifactory.version.converter.ConfigurationConverter;

import javax.jcr.Session;

/**
 * Composition of jcr converters.
 *
 * @author Yossi Shaul
 */
public class CompositeJcrConverter implements ConfigurationConverter<Session> {

    private final ConfigurationConverter<Session>[] converters;

    public CompositeJcrConverter(ConfigurationConverter<Session>... converters) {
        this.converters = converters;
    }

    @Override
    public void convert(Session config) {
        for (ConfigurationConverter<Session> converter : converters) {
            converter.convert(config);
        }
    }
}
