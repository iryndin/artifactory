/*
 * This file is part of Artifactory.
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

package org.artifactory.search;

import org.apache.jackrabbit.core.query.lucene.DefaultXMLExcerpt;
import org.apache.lucene.index.TermPositionVector;
import org.artifactory.common.ConstantValues;

import java.io.IOException;

/**
 * Overrides the default excerpt settings by enlarging the amount of fragments allowed to return
 *
 * @author Noam Tenne
 */
public class ArtifactoryXmlExcerpt extends DefaultXMLExcerpt {
    /**
     * {@inheritDoc}
     */
    @Override
    protected String createExcerpt(TermPositionVector tpv,
            String text,
            int maxFragments,
            int maxFragmentSize)
            throws IOException {
        return super.createExcerpt(tpv, text, ConstantValues.searchMaxFragments.getInt(),
                ConstantValues.searchMaxFragmentsSize.getInt());
    }
}
