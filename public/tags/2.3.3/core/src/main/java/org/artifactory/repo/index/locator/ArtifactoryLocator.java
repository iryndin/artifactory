/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
/*
 * Additional contributors:
 *    JFrog Ltd.
 */

package org.artifactory.repo.index.locator;

import org.apache.maven.index.locator.Locator;
import org.artifactory.repo.jcr.StoringRepo;

/**
 * @author freds
 * @date Oct 24, 2008
 */
public abstract class ArtifactoryLocator implements Locator {
    private final StoringRepo repo;

    public ArtifactoryLocator(StoringRepo repo) {
        this.repo = repo;
    }

    public StoringRepo getRepo() {
        return repo;
    }
}
