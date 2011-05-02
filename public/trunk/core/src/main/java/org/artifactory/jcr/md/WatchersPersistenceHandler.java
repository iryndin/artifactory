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

package org.artifactory.jcr.md;

import org.artifactory.api.md.watch.Watchers;

/**
 * @author freds
 */
public class WatchersPersistenceHandler extends AbstractXmlContentPersistenceHandler<Watchers> {

    public WatchersPersistenceHandler(WatchersXmlProvider xmlProvider) {
        super(xmlProvider);
    }

    @Override
    protected boolean shouldSaveXmlHierarchy() {
        return false;
    }

    public Watchers copy(Watchers original) {
        return new Watchers(original);
    }
}