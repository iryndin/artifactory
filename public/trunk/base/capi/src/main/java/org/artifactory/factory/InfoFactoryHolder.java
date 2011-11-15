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

package org.artifactory.factory;

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

/**
 * Date: 8/1/11
 * Time: 8:59 PM
 *
 * @author Fred Simon
 */
public abstract class InfoFactoryHolder {
    private static final Logger log = LoggerFactory.getLogger(InfoFactoryHolder.class);

    private final static ThreadLocal<InfoFactory> FACTORY_HOLDER = new ThreadLocal<InfoFactory>();
    private final static InfoFactory DEFAULT_FACTORY;

    public static InfoFactory get() {
        return DEFAULT_FACTORY;
        /*
        InfoFactory infoFactory = FACTORY_HOLDER.get();
        if (infoFactory == null) {
        }
        return infoFactory;
        */
    }

    public static void bindFactory(InfoFactory factory) {
        FACTORY_HOLDER.set(factory);
    }

    public static void unbindFactory() {
        FACTORY_HOLDER.remove();
    }

    static {
        DEFAULT_FACTORY = BasicFactory.createInstance(InfoFactory.class,
                "org.artifactory.factory.xstream.XStreamInfoFactory");
    }
}
