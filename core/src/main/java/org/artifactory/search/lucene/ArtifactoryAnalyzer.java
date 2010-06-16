/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.search.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 * An analyzer that breaks tokens on white spaces and lowercases terms
 *
 * @author Noam Y. Tenne
 */
public class ArtifactoryAnalyzer extends Analyzer<Tokenizer> {

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new LowCaseWhitespaceTokenizer(reader);
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        Tokenizer tokenizer = getPreviousTokenStream();
        if (tokenizer == null) {
            tokenizer = new LowCaseWhitespaceTokenizer(reader);
            setPreviousTokenStream(tokenizer);
        } else {
            tokenizer.reset(reader);
        }
        return tokenizer;
    }

    private static class LowCaseWhitespaceTokenizer extends WhitespaceTokenizer {
        public LowCaseWhitespaceTokenizer(Reader reader) {
            super(reader);
        }

        @Override
        protected char normalize(char c) {
            return Character.toLowerCase(c);
        }
    }
}
