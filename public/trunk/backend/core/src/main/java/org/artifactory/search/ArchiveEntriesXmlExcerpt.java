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

package org.artifactory.search;

import com.google.common.collect.Maps;
import org.apache.jackrabbit.core.query.lucene.DefaultXMLExcerpt;
import org.apache.jackrabbit.core.query.lucene.Util;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.search.lucene.ArchiveEntryTermPositionVector;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.SortedMap;

/**
 * Overrides the default excerpt settings: <ul> <li>Enlarges the size and amount of fragments allowed to return.</li>
 * <li>Modifies the default implementation of {@link org.apache.jackrabbit.core.query.lucene.AbstractExcerpt#highlight(java.lang.String)}
 * to utilize a customized term position vector.</li> </ul>
 * <p/>
 * Based on : /org/apache/jackrabbit/jackrabbit-core/1.6.0/jackrabbit-core-1.6.0-sources.jar!/org/apache/jackrabbit/
 * core/query/lucene/AbstractExcerpt.java
 *
 * @author Noam Tenne
 */
public class ArchiveEntriesXmlExcerpt extends DefaultXMLExcerpt {

    /**
     * Indicates whether the query is already rewritten.<P> Implementation taken from {@link
     * org.apache.jackrabbit.core.query.lucene.AbstractExcerpt}
     */
    private boolean rewritten = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String createExcerpt(TermPositionVector tpv, String text, int maxFragments, int maxFragmentSize)
            throws IOException {
        return super.createExcerpt(tpv, text, ConstantValues.searchMaxFragments.getInt(),
                ConstantValues.searchMaxFragmentsSize.getInt());
    }

    /**
     * Implementation taken from {@link org.apache.jackrabbit.core.query.lucene.AbstractExcerpt#highlight(java.lang.String)}
     *
     * @param text
     * @return
     * @throws IOException
     */
    @Override
    public String highlight(String text) throws IOException {
        checkRewritten(null);
        return createExcerpt(createTermPositionVector(text), text, 1, (text.length() + 1) * 2);
    }

    /**
     * Implementation taken from {@link org.apache.jackrabbit.core.query.lucene.AbstractExcerpt#checkRewritten(org.apache.lucene.index.IndexReader)}
     * <P>Makes sure the {@link #query} is rewritten. If the query is already rewritten, this method returns
     * immediately.
     *
     * @param reader an optional index reader, if none is passed this method will retrieve one from the {@link #index}
     *               and close it again after the rewrite operation.
     * @throws IOException if an error occurs while the query is rewritten.
     */
    private void checkRewritten(IndexReader reader) throws IOException {
        if (!rewritten) {
            IndexReader r = reader;
            if (r == null) {
                r = index.getIndexReader();
            }
            try {
                query = query.rewrite(r);
            } finally {
                // only close reader if this method opened one
                if (reader == null) {
                    Util.closeOrRelease(r);
                }
            }
            rewritten = true;
        }
    }

    /**
     * Implementation taken from {@link org.apache.jackrabbit.core.query.lucene.AbstractExcerpt#createTermPositionVector(java.lang.String)}
     * <P>The only change made to this method is the construction of the customized term position vector.
     *
     * @param text the text.
     * @return a <code>TermPositionVector</code> for the given text.
     */
    private TermPositionVector createTermPositionVector(String text) {
        // term -> TermVectorOffsetInfo[]
        final SortedMap<String, TermVectorOffsetInfo[]> termMap = Maps.newTreeMap();
        Reader r = new StringReader(text);
        TokenStream ts = index.getTextAnalyzer().tokenStream("", r);
        Token t = new Token();
        try {
            while ((t = ts.next(t)) != null) {
                String termText = t.term();
                TermVectorOffsetInfo[] info = termMap.get(termText);
                if (info == null) {
                    info = new TermVectorOffsetInfo[1];
                } else {
                    TermVectorOffsetInfo[] tmp = info;
                    info = new TermVectorOffsetInfo[tmp.length + 1];
                    System.arraycopy(tmp, 0, info, 0, tmp.length);
                }
                info[info.length - 1] = new TermVectorOffsetInfo(
                        t.startOffset(), t.endOffset());
                termMap.put(termText, info);
            }
        } catch (IOException e) {
            // should never happen, we are reading from a string
        }

        return new ArchiveEntryTermPositionVector(termMap);
    }
}