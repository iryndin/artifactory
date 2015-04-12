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

package org.artifactory.storage.binstore.service;

import javax.annotation.Nonnull;

/**
 * Date: 12/12/12
 * Time: 3:03 PM
 *
 * @author freds
 */
public abstract class BinaryProviderBase implements BinaryProvider {
    private BinaryProviderContext context;

    protected BinaryProviderContext getContext() {
        return context;
    }

    public void setContext(@Nonnull BinaryProviderContext ctx) {
        this.context = ctx;
    }

    public BinaryProviderBase next() {
        return context.next();
    }

    protected void check() {
        if (context == null || context.next() == null) {
            throw new IllegalStateException("Binary Provider " + this + " not initialized!");
        }
    }
}
